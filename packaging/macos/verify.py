#!/usr/bin/env python3
"""Verify the built Mac app/installer with owned fixtures; never authorize BPF or change the OS."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import platform
import plistlib
import re
import shlex
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


REQUIRED = {"java.base", "java.desktop", "java.net.http", "java.xml", "jdk.httpserver"}
ROOT = Path(__file__).resolve().parents[2]
LANGUAGES = ("en", "ru", "de", "es", "fr")
SCREENSHOTS = ("desktop-downloads-en.png", "desktop-network-en.png", "desktop-bulk-import-en.png", "tray-speeds-3.3.png")


def verify_documentation(app_dir: Path, report: dict) -> None:
    hashes = report["bundled_documentation_sha256"]
    required = {"README.md", "README.ru.md", "docs/COMPATIBILITY.json", "CHECKS.ru.md",
                "TEST-REPORT.json", "DEPLOYMENT-TEST-REPORT.json", "native/source/BUILD-README.txt"}
    required.update("docs/" + language + ".md" for language in LANGUAGES)
    required.update("docs/images/" + name for name in SCREENSHOTS)
    assert required.issubset(hashes), "The app must contain five local guides, READMEs, compatibility information and screenshots."
    for name, expected in hashes.items():
        relative = PurePosixPath(name)
        assert not relative.is_absolute() and ".." not in relative.parts, "Invalid documentation manifest path."
        source = ROOT.joinpath(*relative.parts)
        bundled = app_dir.joinpath(*relative.parts)
        assert hashlib.sha256(source.read_bytes()).hexdigest() == expected, "Documentation differs from the saved source: " + name
        assert hashlib.sha256(bundled.read_bytes()).hexdigest() == expected, "Bundled documentation mismatch: " + name
    for language in LANGUAGES:
        guide = (app_dir / "docs" / (language + ".md")).read_text(encoding="utf-8")
        assert report["project_owner_url"] in guide, "Missing owner link in " + language + " guide."
        assert "../LICENSE.txt" in guide, "Missing local licence link in " + language + " guide."
    for name in SCREENSHOTS:
        assert (app_dir / "docs/images" / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n"), "Invalid bundled screenshot: " + name


def verify_relative_markdown_links(app_dir: Path) -> int:
    app_root = app_dir.resolve()
    count = 0
    for document in sorted(app_root.rglob("*.md")):
        content = document.read_text(encoding="utf-8")
        for raw_target in re.findall(r"!?\[[^\]]*\]\(([^\)]+)\)", content):
            parsed = urllib.parse.urlsplit(raw_target.strip().strip("<>"))
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            local = (document.parent / urllib.parse.unquote(parsed.path)).resolve()
            assert local.is_relative_to(app_root) and local.exists(), (
                "Offline documentation link is missing or escapes the app: "
                + document.relative_to(app_root).as_posix() + " -> " + parsed.path
            )
            count += 1
    assert count > 0, "The installed guides must retain their local links."
    return count


def run(command, **kwargs):
    return subprocess.run([str(value) for value in command], check=True, **kwargs)


def desktop_shortcut_fragment(script: str) -> str:
    return script.split("# BEGIN_DESKTOP_SHORTCUT_USER\n", 1)[1].split("# END_DESKTOP_SHORTCUT_USER", 1)[0]


def verify_desktop_shortcut(script: str, owned: Path) -> int:
    fragment = desktop_shortcut_fragment(script)
    source = (ROOT / "packaging/macos/postinstall.sh.in").read_text(encoding="utf-8")
    assert fragment == desktop_shortcut_fragment(source), "Installed shortcut logic differs from its saved source."
    assert '/usr/bin/sudo -n -H -u "$console_user" -- /bin/bash -c' in script
    assert '/usr/bin/id -u "$console_user"' in script
    uid = os.getuid()
    assert uid > 0, "Desktop shortcut fixtures must run as a normal user."
    fixtures = owned / "shortcut-fixtures"
    fixtures.mkdir()
    application = fixtures / "Owned Native Application.app"
    application.mkdir()
    command = fragment + '\ncreate_user_desktop_shortcut "$@"\n'

    def invoke(home: Path, expected_uid: int = uid, target: Path = application):
        return subprocess.run(["/bin/bash", "-c", command, "owned-shortcut-fixture", str(home),
                               str(expected_uid), str(target)], capture_output=True, text=True)

    def home(name: str, desktop: bool = True):
        result = fixtures / name
        result.mkdir()
        if desktop:
            (result / "Desktop").mkdir()
        return result

    fresh = home("fresh normal user")
    shortcut = fresh / "Desktop/MultiRoute Resume.app"
    assert invoke(fresh).returncode == 0 and shortcut.is_symlink()
    assert os.readlink(shortcut) == str(application) and shortcut.lstat().st_uid == uid
    original = shortcut.lstat().st_ino
    assert invoke(fresh).returncode == 0 and shortcut.lstat().st_ino == original
    regular = home("existing regular item")
    existing = regular / "Desktop/MultiRoute Resume.app"
    existing.write_text("keep this owned item", encoding="utf-8")
    assert invoke(regular).returncode == 0 and existing.read_text(encoding="utf-8") == "keep this owned item"
    linked = home("existing broken link")
    existing_link = linked / "Desktop/MultiRoute Resume.app"
    existing_link.symlink_to(fixtures / "not-present.app")
    original_target = os.readlink(existing_link)
    assert invoke(linked).returncode == 0 and os.readlink(existing_link) == original_target
    absent = home("missing Desktop", desktop=False)
    assert invoke(absent).returncode != 0 and not (absent / "Desktop").exists()
    redirected = home("redirected Desktop", desktop=False)
    other = fixtures / "other owned directory"
    other.mkdir()
    (redirected / "Desktop").symlink_to(other)
    assert invoke(redirected).returncode != 0 and not (other / "MultiRoute Resume.app").exists()
    wrong_uid = home("wrong uid")
    assert invoke(wrong_uid, expected_uid=0).returncode != 0
    assert not (wrong_uid / "Desktop/MultiRoute Resume.app").exists()
    absent_app = home("missing app")
    assert invoke(absent_app, target=fixtures / "missing app target").returncode != 0
    assert not (absent_app / "Desktop/MultiRoute Resume.app").exists()
    return 8


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
    assert report["setup_language"] == "en"
    assert report["desktop_shortcut"]["target"] == "/Applications/MultiRoute Resume.app"
    assert report["desktop_shortcut"]["existing_desktop_items_replaced"] is False
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
    verify_documentation(app_dir, report)
    app_markdown_links = verify_relative_markdown_links(app_dir)
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
        desktop_fixture = owned / "NativeDesktopFixture.java"
        desktop_fixture.write_text('''
package multiroute;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
public final class NativeDesktopFixture {
    static int checks;
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createDirectories(Path.of(args[0]));
        Path app = Files.createDirectory(root.resolve("Owned Native.app"));
        Path desktop = Files.createDirectory(root.resolve("fresh Desktop"));
        Path link = desktop.resolve("MultiRoute Resume.app");
        DesktopLauncher.createDesktopShortcutIfAbsent(desktop, app);
        check(Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(app.toAbsolutePath()), "Native Mac shortcut creation");
        DesktopLauncher.createDesktopShortcutIfAbsent(desktop, app);
        check(Files.readSymbolicLink(link).equals(app.toAbsolutePath()), "Retry preserves the existing shortcut");
        Path regularDesktop = Files.createDirectory(root.resolve("existing regular Desktop"));
        Path regular = regularDesktop.resolve("MultiRoute Resume.app");
        Files.writeString(regular, "keep this owned item");
        DesktopLauncher.createDesktopShortcutIfAbsent(regularDesktop, app);
        check(Files.readString(regular).equals("keep this owned item"), "Existing file retained");
        Path linkedDesktop = Files.createDirectory(root.resolve("existing broken shortcut"));
        Path broken = linkedDesktop.resolve("MultiRoute Resume.app");
        Path absent = root.resolve("not-present.app");
        Files.createSymbolicLink(broken, absent);
        DesktopLauncher.createDesktopShortcutIfAbsent(linkedDesktop, app);
        check(Files.readSymbolicLink(broken).equals(absent), "Existing broken link retained");
        Path other = Files.createDirectory(root.resolve("other owned folder"));
        Path redirected = root.resolve("redirected Desktop");
        Files.createSymbolicLink(redirected, other);
        DesktopLauncher.createDesktopShortcutIfAbsent(redirected, app);
        check(!Files.exists(other.resolve("MultiRoute Resume.app")), "Redirected Desktop rejected");
        Path noAppDesktop = Files.createDirectory(root.resolve("missing app Desktop"));
        DesktopLauncher.createDesktopShortcutIfAbsent(noAppDesktop, absent);
        check(!Files.exists(noAppDesktop.resolve("MultiRoute Resume.app")), "Missing application rejected");
        Path missingDesktop = root.resolve("missing Desktop");
        DesktopLauncher.createDesktopShortcutIfAbsent(missingDesktop, app);
        check(!Files.exists(missingDesktop), "Desktop is never created implicitly");
        int shortcuts = checks;
        check(DesktopTray.totalBytesPerSecond(List.of(Map.of("status", "running", "speed_bps", 125000L),
                Map.of("status", "retrying", "speed_bps", 250000L), Map.of("status", "paused", "speed_bps", 9000000L))) == 375000L,
                "Tray sums active downloads and excludes stale paused counters");
        check(DesktopTray.totalBytesPerSecond(List.of(Map.of("status", "running", "speed_bps", -1L))) == 0L, "Negative speeds clamped");
        check(DesktopTray.totalBytesPerSecond(List.of(Map.of("status", "running", "speed_bps", Long.MAX_VALUE),
                Map.of("status", "running", "speed_bps", 1L))) == Long.MAX_VALUE, "Sum saturates on overflow");
        check(DesktopTray.compactRate(-1).equals("0.0"), "Negative compact speed clamped");
        check(DesktopTray.compactRate(1250000).equals("10.0"), "Byte speed converted to megabits");
        check(DesktopTray.iconUnits(124999999L).equals("Mb/s"), "Below one gigabit uses megabit units");
        check(DesktopTray.compactRate(125000000L).equals("1.0") && DesktopTray.iconUnits(125000000L).equals("Gb/s"),
                "One gigabit boundary uses compact gigabit units");
        check(DesktopTray.compactRate(1250000000L).equals("10.0") && DesktopTray.iconUnits(1250000000L).equals("Gb/s"),
                "Ten gigabits is displayed without a counter limit");
        long tenGigabits = DesktopTray.totalBytesPerSecond(List.of(Map.of("status", "running", "speed_bps", 625000000L),
                Map.of("status", "running", "speed_bps", 625000000L)));
        check(tenGigabits == 1250000000L && DesktopTray.compactRate(tenGigabits).equals("10.0"), "Two five-gigabit downloads sum to ten gigabits");
        check(DesktopTray.tooltip(tenGigabits).contains("1250000000") && DesktopTray.tooltip(tenGigabits).matches(".*10[.,]00.*"),
                "Tooltip retains exact bytes and a precise ten-gigabit rate");
        BufferedImage tiny = DesktopTray.drawIcon(0, new Dimension(1,1));
        check(tiny.getWidth() == 16 && tiny.getHeight() == 16, "Minimum tray icon dimensions");
        BufferedImage idle = DesktopTray.drawIcon(0, new Dimension(64,64));
        BufferedImage active = DesktopTray.drawIcon(1250000, new Dimension(64,64));
        boolean changed = false;
        for (int y=0;y<64;y++) for (int x=0;x<64;x++) changed |= idle.getRGB(x,y) != active.getRGB(x,y);
        check(active.getWidth() == 64 && active.getHeight() == 64 && changed, "Icon visibly reflects live speed");
        BufferedImage megabit = DesktopTray.drawIcon(125000L, new Dimension(64,64));
        BufferedImage gigabit = DesktopTray.drawIcon(125000000L, new Dimension(64,64));
        boolean unitsChanged = false;
        for (int y=40;y<64;y++) for (int x=0;x<64;x++) unitsChanged |= megabit.getRGB(x,y) != gigabit.getRGB(x,y);
        check(unitsChanged, "Equal compact numbers render different Mb/s and Gb/s unit labels");
        boolean transparent = true, black = true, bothRows = true;
        for (int dimension : new int[]{16, 20, 24, 32}) for (long rate : new long[]{0L, 12500000L, 125000000L, 1250000000L}) {
            BufferedImage rendered = DesktopTray.drawIcon(rate, new Dimension(dimension, dimension));
            int empty = 0, numberInk = 0, unitInk = 0;
            for (int y=0; y<dimension; y++) for (int x=0; x<dimension; x++) {
                int pixel = rendered.getRGB(x,y);
                if ((pixel >>> 24) == 0) empty++;
                else {
                    black &= (pixel & 0x00ffffff) == 0;
                    if (y < Math.round(dimension * .62f)) numberInk++;
                    if (y >= Math.round(dimension * .62f)) unitInk++;
                }
            }
            transparent &= empty > dimension * dimension / 3 && (rendered.getRGB(0,0) >>> 24) == 0;
            bothRows &= numberInk > 0 && unitInk > 0;
        }
        check(transparent, "Tray background remains transparent at native icon sizes through ten gigabits");
        check(black, "All visible tray digits and units are black, including antialiased edges");
        check(bothRows, "Both number and unit rows contain visible glyphs at every native icon size");
        System.out.println("OWNED_NATIVE_DESKTOP_FIXTURE_OK shortcuts=" + shortcuts + " tray=" + (checks-shortcuts));
    }
}
''', encoding="utf-8")
        run([jdk / "bin/javac", "--release", "17", "-cp", classpath, "-d", owned, desktop_fixture])
        desktop_test = run([java, "-Djava.awt.headless=true", "-cp", str(owned) + os.pathsep + classpath,
                            "multiroute.NativeDesktopFixture", owned / "owned-desktop-state"], capture_output=True, text=True)
        assert "OWNED_NATIVE_DESKTOP_FIXTURE_OK shortcuts=7 tray=16" in desktop_test.stdout
        checks.append("seven normal-user Java shortcut fixtures create real native Mac symlinks and preserve existing items without touching the user Desktop")
        checks.append("sixteen headless tray checks include transparent background, black glyphs, native-size digit and unit rows through ten gigabits, aggregate rates and exact byte counters without installing a tray icon")
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
        postinstall_scripts = list(expanded.rglob("postinstall"))
        assert len(postinstall_scripts) == 1
        run(["/bin/bash", "-n", postinstall_scripts[0]])
        shortcut_checks = verify_desktop_shortcut(postinstall_scripts[0].read_text(encoding="utf-8"), owned)
        checks.append("eight owned normal-user Desktop shortcut fixtures pass; installer switches to a validated console user and preserves existing items")
        installed_apps = list(expanded.rglob("MultiRoute Resume.app"))
        assert len(installed_apps) == 1, "The installer must contain exactly one application bundle."
        installed_app_dir = installed_apps[0] / "Contents" / "app"
        verify_documentation(installed_app_dir, report)
        installed_markdown_links = verify_relative_markdown_links(installed_app_dir)
        assert installed_markdown_links == app_markdown_links
        assert (installed_app_dir / "LICENSE.txt").read_bytes() == license_bytes
        assert (installed_app_dir / "THIRD-PARTY-NOTICES.txt").read_bytes() == (ROOT / "THIRD-PARTY-NOTICES.txt").read_bytes()
        checks.append("five local language guides, both READMEs, compatibility information and four screenshots match saved sources in the app and expanded installer")
        checks.append("all relative Markdown links resolve within the app image and expanded installer, including baseline reports and native build instructions")
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
        assert distribution.find("title").text == "MultiRoute Resume"
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
        "not_verified": ["physical gateway throughput", "BPF authorization UI", "end-user Finder/Gatekeeper interaction", "real macOS menu-bar tray interaction"],
        "installed_packages": False, "administrator_prompt_executed": False, "routing_or_BPF_permissions_changed": False,
        "controlled_uid_drop_fixture": True,
        "offline_markdown_link_counts": {"app_image": app_markdown_links, "expanded_installer": installed_markdown_links},
        "setup_language": "en", "desktop_shortcut_owned_fixture_checks": shortcut_checks,
        "java_desktop_shortcut_owned_fixture_checks": 7, "headless_tray_fixture_checks": 13,
        "real_user_desktop_modified": False,
    }
    (directory / "macos-verification-report.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
