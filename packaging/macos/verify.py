#!/usr/bin/env python3
"""Verify the built Mac app/installer with owned fixtures; never authorize BPF or change the OS."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import plistlib
import shlex
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET


REQUIRED = {"java.base", "java.desktop", "java.net.http", "java.xml", "jdk.httpserver"}


def run(command, **kwargs):
    return subprocess.run([str(value) for value in command], check=True, **kwargs)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != "Darwin":
        parser.error("Run the binary verification on a native macOS runner.")
    directory = args.directory.resolve()
    report = json.loads((directory / "macos-build-report.json").read_text(encoding="utf-8"))
    architecture = report["architecture"]
    if architecture != platform.machine():
        parser.error("Verify the installer on a matching native CPU runner.")
    app = directory / "MultiRoute Resume.app"
    contents = app / "Contents"
    with (contents / "Info.plist").open("rb") as stream:
        metadata = plistlib.load(stream)
    assert metadata["CFBundleIdentifier"] == "local.multiroute.resume"
    assert metadata["LSMinimumSystemVersion"] == report["minimum_macos"]
    assert set(metadata["CFBundleLocalizations"]) == {"en", "ru", "de", "es", "fr"}
    assert metadata["MultiRouteProjectOwnerURL"] == report["project_owner_url"]
    executable = contents / "MacOS" / metadata["CFBundleExecutable"]
    assert executable.is_file() and not executable.read_bytes().startswith(b"#!"), "The Finder launcher must be native code."
    assert architecture in run(["/usr/bin/lipo", "-archs", executable], capture_output=True, text=True).stdout.split()
    runtime = contents / "runtime" / "Contents" / "Home"
    if not (runtime / "bin/java").is_file():
        runtime = contents / "runtime"
    java = runtime / "bin/java"
    modules = run([java, "--list-modules"], capture_output=True, text=True).stdout
    assert REQUIRED.issubset({line.split("@", 1)[0] for line in modules.splitlines()})
    run([java, "--validate-modules"])
    app_dir = contents / "app"
    license_bytes = (app_dir / "LICENSE.txt").read_bytes()
    assert hashlib.sha256(license_bytes).hexdigest() == report["license_sha256"]
    assert ("Project owner: " + report["project_owner_url"]).encode("utf-8") in license_bytes
    assert (app_dir / "THIRD-PARTY-NOTICES.txt").is_file()
    jars = [app_dir / "MultiRoute-Resume.jar", *sorted((app_dir / "lib").glob("*.jar"))]
    classpath = os.pathsep.join(str(path) for path in jars)
    jdk = Path(os.environ["JAVA_HOME"])
    checks = ["native Finder launcher", "bundle metadata and five language prompts", "bundled Java modules"]
    with tempfile.TemporaryDirectory(prefix="multiroute-macos-verify-") as temporary:
        owned = Path(temporary)
        probe = owned / "NativeProbe.java"
        probe.write_text('''
public final class NativeProbe {
    public static void main(String[] args) throws Exception {
        Object value = Class.forName("com.frostwire.jlibtorrent.LibTorrent").getMethod("version").invoke(null);
        System.out.println("NATIVE_TORRENT_OK " + value);
    }
}
''', encoding="utf-8")
        run([jdk / "bin/javac", "--release", "17", "-d", owned, probe])
        native = run([java, "-cp", str(owned) + os.pathsep + classpath, "NativeProbe"], capture_output=True, text=True)
        assert "NATIVE_TORRENT_OK" in native.stdout, "The bundled native torrent library must load on its advertised OS/CPU."
        checks.append("bundled torrent JNI loaded without any peers or downloads")
        fixture = owned / "MacAuthorizationFixture.java"
        fixture.write_text('''
package multiroute;
import java.util.*;
public final class MacAuthorizationFixture {
    public static void main(String[] args) throws Exception {
        String special = "own fixture é'$(printf ignored)&" + (java.io.File.separatorChar == '/' ? "\\\"" : "");
        String ready = java.nio.file.Path.of(args.length == 0 ? "/tmp" : args[0], special, "network", "ready-12345678-1234-1234-1234-123456789abc.json").toString();
        List<String> values = List.of(MacGatewayLauncher.expectedHelper().toString(),
            "--virtual-ip", "192.168.1.70", "--gateways", "192.168.1.1,192.168.1.2",
            "--ready-file", ready, "--parent-pid", "123", "--drop-uid", "501", "--drop-gid", "20", "--stop-file", ready + ".stop");
        List<String> command = MacGatewayLauncher.authorizationArguments(values);
        int rejected = 0;
        for (int position : new int[]{0, 1, 2, 4, 8, 10, 14}) {
            List<String> invalid = new ArrayList<>(values);
            invalid.set(position, position == 0 ? "/tmp/untrusted" : position == 1 ? "--other" : "bad");
            try { MacGatewayLauncher.authorizationArguments(invalid); throw new AssertionError("Unsafe argument accepted"); }
            catch (java.io.IOException expected) { rejected++; }
        }
        System.out.println(Json.stringify(Map.of("arguments", values, "authorization", command, "rejected", rejected)));
    }
}
''', encoding="utf-8")
        run([jdk / "bin/javac", "--release", "17", "-cp", classpath, "-d", owned, fixture])
        validation = json.loads(run([java, "-cp", str(owned) + os.pathsep + classpath, "multiroute.MacAuthorizationFixture"], capture_output=True, text=True).stdout)
        launcher_path = str(Path(validation["arguments"][0]).with_name("gateway-launch"))
        assert shlex.split(validation["authorization"][3]) == ["exec", launcher_path, *validation["arguments"]]
        assert validation["authorization"][0] == "/usr/bin/osascript" and validation["rejected"] == 7
        apple_script = owned / "authorize.applescript"
        apple_script.write_text(validation["authorization"][2], encoding="utf-8")
        run(["/usr/bin/osacompile", "-o", owned / "authorize.scpt", apple_script])
        checks.append("AppleScript compiled; quoting round-trip and seven invalid requests rejected without authorization")
        with socket.socket() as socket_fixture:
            socket_fixture.bind(("127.0.0.1", 0))
            port = socket_fixture.getsockname()[1]
        data = owned / "owned-api-state"
        log_path = owned / "owned-api.log"
        child = None
        with log_path.open("wb") as log:
            try:
                child = subprocess.Popen([str(executable), "--headless", "--port", str(port), "--data-dir", str(data)], stdout=log, stderr=subprocess.STDOUT)
                deadline = time.monotonic() + 35
                while time.monotonic() < deadline:
                    if child.poll() is not None:
                        raise AssertionError("Native app startup failed:\n" + log_path.read_text(encoding="utf-8", errors="replace"))
                    try:
                        with urllib.request.urlopen(f"http://127.0.0.1:{port}/api/v1/health", timeout=1) as response:
                            health = json.load(response)
                        assert health.get("ok") is True and health.get("version") == report["version"]
                        checks.append("native app launcher reached its local API using fresh owned state")
                        break
                    except (urllib.error.URLError, TimeoutError):
                        time.sleep(0.15)
                else:
                    raise AssertionError("Native app API did not become ready.")
            finally:
                if child is not None and child.poll() is None:
                    child.terminate()
                    try:
                        child.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        child.kill()
                        child.wait(timeout=5)
        expanded = owned / "expanded-package"
        run(["/usr/sbin/pkgutil", "--expand-full", directory / report["package"], expanded])
        candidates = [path for path in expanded.rglob("gateway-net") if "/Library/PrivilegedHelperTools/" in path.as_posix()]
        assert len(candidates) == 1
        helper = candidates[0]
        marker = helper.with_name("gateway-net.sha256")
        assert marker.read_text(encoding="ascii").strip() == report["helper_sha256"]
        selftest = run([helper, "--self-test"], capture_output=True, text=True)
        assert "SELF_TEST_OK no network handles, no routing changes" in selftest.stdout
        assert architecture in run(["/usr/bin/lipo", "-archs", helper], capture_output=True, text=True).stdout.split()
        launcher = helper.with_name("gateway-launch")
        assert launcher.with_name("gateway-launch.sha256").read_text(encoding="ascii").strip() == report["gateway_launcher_sha256"]
        monitor = run([launcher, "--self-test"], capture_output=True, text=True)
        assert "1MiB fully drained, 64KiB retained, exit codes preserved, closed consumer tolerated" in monitor.stdout
        # Controlled CI fixture only: no capture handles, installer execution, routes or permissions.
        uid, gid = os.getuid(), os.getgid()
        assert uid > 0 and gid > 0
        dropped = run(["/usr/bin/sudo", "-n", launcher, "--self-test-drop", str(uid), str(gid), str(os.getpid())], capture_output=True, text=True)
        assert f"GATEWAY_LAUNCH_DROP_OK uid={uid} gid={gid} cannot regain root" in dropped.stdout
        checks.append("capture monitor drained 1MiB with a 64KiB bound, preserved exit codes and tolerated a closed consumer")
        checks.append("controlled root monitor fixture permanently dropped UID/GID and could not regain root")
        distribution = ET.parse(expanded / "Distribution").getroot()
        license_element = distribution.find("license")
        assert license_element is not None and license_element.get("file") == "LICENSE.txt" and license_element.get("mime-type") == "text/plain"
        installer_license = expanded / "Resources" / license_element.get("file")
        assert hashlib.sha256(installer_license.read_bytes()).hexdigest() == report["license_sha256"]
        checks.append("Installer license resource and installed app license exactly match the saved license and owner profile")
        assert distribution.find("options").get("hostArchitectures") == architecture
        assert distribution.find("volume-check/allowed-os-versions/os-version").get("min") == report["minimum_macos"]
        checks.append("expanded installer contains the matching helper/checksum and passes native offline self-test")
    result = {
        "ok": True, "architecture": architecture, "version": report["version"], "checks": checks,
        "not_verified": ["physical gateway throughput", "BPF authorization UI", "end-user Finder/Gatekeeper interaction"],
        "installed_packages": False, "administrator_prompt_executed": False, "routing_or_BPF_permissions_changed": False,
        "controlled_uid_drop_fixture": True,
    }
    (directory / "macos-verification-report.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
