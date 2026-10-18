#!/usr/bin/env python3
"""Build a self-contained macOS .app and one graphical .pkg on a native Mac runner."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import plistlib
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
APP_NAME = "MultiRoute Resume"
BUNDLE_ID = "local.multiroute.resume"
REQUIRED_MODULES = {"java.base", "java.desktop", "java.net.http", "java.xml", "jdk.httpserver"}


def run(command: list[str | Path], **kwargs) -> subprocess.CompletedProcess:
    if kwargs.get("text") or kwargs.get("universal_newlines"):
        kwargs.setdefault("encoding", "utf-8")
    return subprocess.run([str(part) for part in command], check=True, **kwargs)


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def manifest_attribute(name: str, value: str) -> str:
    line = name + ": " + value
    chunks = [line[:70]]
    line = line[70:]
    while line:
        chunks.append(" " + line[:69])
        line = line[69:]
    return "\r\n".join(chunks) + "\r\n"


def write_platform_jar(source: Path, target: Path, jars: list[Path], version: str) -> None:
    manifest = "Manifest-Version: 1.0\r\nMain-Class: multiroute.DesktopLauncher\r\n"
    manifest += manifest_attribute("Implementation-Version", version)
    manifest += manifest_attribute("Class-Path", " ".join("lib/" + path.name for path in jars)) + "\r\n"
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as new:
        if "multiroute/DesktopLauncher.class" not in old.namelist() or "multiroute/MacGatewayLauncher.class" not in old.namelist():
            raise SystemExit("The native desktop entry point and graphical macOS authorizer must be compiled first.")
        new.writestr("META-INF/MANIFEST.MF", manifest)
        for item in old.infolist():
            if item.filename != "META-INF/MANIFEST.MF":
                new.writestr(item, old.read(item))


def plist_write(path: Path, value) -> None:
    with path.open("wb") as stream:
        plistlib.dump(value, stream, fmt=plistlib.FMT_XML, sort_keys=False)


def script_templates(destination: Path, version: str, digest: str, launcher_digest: str) -> None:
    destination.mkdir()
    for name in ("preinstall", "postinstall"):
        text = (HERE / (name + ".sh.in")).read_text(encoding="utf-8")
        text = text.replace("@VERSION@", version).replace("@HELPER_SHA256@", digest)
        text = text.replace("@LAUNCHER_SHA256@", launcher_digest)
        if re.search(r"@[A-Z_]+@", text):
            raise SystemExit("Unresolved installer template placeholder: " + name)
        path = destination / name
        path.write_text(text, encoding="utf-8", newline="\n")
        path.chmod(0o755)
        run(["/bin/bash", "-n", path])


def localize_network_prompt(contents: Path) -> None:
    values = {
        "en": "MultiRoute Resume connects to your selected local gateways and downloads through them without changing system routes.",
        "ru": "MultiRoute Resume подключается к выбранным шлюзам локальной сети и скачивает через них, не меняя системные маршруты.",
        "de": "MultiRoute Resume verbindet sich mit Ihren gewählten lokalen Gateways und lädt über sie herunter, ohne Systemrouten zu ändern.",
        "es": "MultiRoute Resume se conecta a las puertas de enlace locales seleccionadas y descarga a través de ellas sin cambiar las rutas del sistema.",
        "fr": "MultiRoute Resume se connecte aux passerelles locales sélectionnées et télécharge via celles-ci sans modifier les routes du système.",
        "zh-Hans": "MultiRoute Resume 连接到您选择的本地网关，通过这些网关下载文件，不会更改系统路由。",
        "ko": "MultiRoute Resume는 선택한 로컬 게이트웨이에 연결하여 시스템 경로를 변경하지 않고 파일을 다운로드합니다.",
        "ar": "يتصل MultiRoute Resume ببوابات الشبكة المحلية التي تختارها وينزّل الملفات عبرها دون تغيير مسارات النظام.",
        "it": "MultiRoute Resume si collega ai gateway locali selezionati e scarica tramite essi senza modificare le route di sistema.",
    }
    for language, text in values.items():
        directory = contents / "Resources" / (language + ".lproj")
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "InfoPlist.strings").write_text(
            '"NSLocalNetworkUsageDescription" = "' + text + '";\n', encoding="utf-8", newline="\n"
        )


def distribution_xml(package_name: str, version: str, architecture: str, minimum: str) -> bytes:
    root = ET.Element("installer-gui-script", {"minSpecVersion": "2"})
    ET.SubElement(root, "title").text = APP_NAME
    ET.SubElement(root, "license", {"file": "LICENSE.txt", "mime-type": "text/plain"})
    ET.SubElement(root, "options", {
        "customize": "never", "require-scripts": "false", "rootVolumeOnly": "true",
        "hostArchitectures": architecture,
    })
    ET.SubElement(root, "domains", {"enable_anywhere": "false", "enable_currentUserHome": "false", "enable_localSystem": "true"})
    volume = ET.SubElement(root, "volume-check")
    versions = ET.SubElement(volume, "allowed-os-versions")
    ET.SubElement(versions, "os-version", {"min": minimum})
    outline = ET.SubElement(root, "choices-outline")
    ET.SubElement(outline, "line", {"choice": "application"})
    choice = ET.SubElement(root, "choice", {"id": "application", "visible": "false", "title": APP_NAME})
    ET.SubElement(choice, "pkg-ref", {"id": BUNDLE_ID})
    reference = ET.SubElement(root, "pkg-ref", {"id": BUNDLE_ID, "version": version, "onConclusion": "none"})
    reference.text = package_name
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "dist" / "macos")
    parser.add_argument("--architecture", choices=("arm64", "x86_64"))
    parser.add_argument("--signing-user", default=os.environ.get("MULTIROUTE_MAC_SIGNING_USER", ""),
                        help="Team/name portion present in both Developer ID Application and Installer certificates")
    parser.add_argument("--notarize-profile", default=os.environ.get("MULTIROUTE_MAC_NOTARIZE_PROFILE", ""),
                        help="Existing notarytool keychain credential profile; never pass passwords in arguments")
    args = parser.parse_args()
    if platform.system() != "Darwin":
        parser.error("Build this installer on macOS; jpackage and Apple package tools do not cross-compile packages.")
    architecture = args.architecture or platform.machine()
    if architecture not in ("arm64", "x86_64") or architecture != platform.machine():
        parser.error("Use a native arm64 or x86_64 runner matching the requested package architecture.")
    if args.notarize_profile and not args.signing_user:
        parser.error("Notarization requires installed Developer ID signing certificates.")
    license_file = ROOT / "LICENSE.txt"
    if not license_file.is_file():
        parser.error("The saved application LICENSE.txt must be included in the installer.")
    license_text = license_file.read_text(encoding="utf-8")
    owner_match = re.search(r"^Project owner: (https://github\.com/[A-Za-z0-9-]+)\s*$", license_text, re.MULTILINE)
    if not owner_match:
        parser.error("LICENSE.txt must identify the project owner's GitHub profile URL.")
    owner_url = owner_match.group(1)
    jdk = Path(os.environ.get("JAVA_HOME", ""))
    if not jdk.is_absolute() or not all((jdk / "bin" / name).is_file() for name in ("java", "javac", "jlink", "jpackage")):
        parser.error("Set JAVA_HOME to a native full JDK 21 or newer with jpackage and jlink.")
    properties = run([jdk / "bin/java", "-XshowSettings:properties", "-version"], capture_output=True, text=True)
    text = properties.stdout + properties.stderr
    found = re.search(r"java\.specification\.version\s*=\s*(\d+)", text)
    cpu = re.search(r"os\.arch\s*=\s*(\S+)", text)
    if not found or int(found.group(1)) < 21 or not cpu or cpu.group(1) not in ({"aarch64", "arm64"} if architecture == "arm64" else {"x86_64", "amd64"}):
        parser.error("The packaging JDK must be version 21+ and use the native package CPU architecture.")
    source = (ROOT / "src/multiroute/Main.java").read_text(encoding="utf-8")
    version_match = re.search(r'static final String VERSION\s*=\s*"([0-9]+(?:\.[0-9]+){1,2})"', source)
    if not version_match:
        parser.error("Cannot read the saved application version.")
    version = version_match.group(1)
    if "MacGatewayLauncher.startHelper" not in (ROOT / "src/multiroute/GatewayNetwork.java").read_text(encoding="utf-8"):
        parser.error("GatewayNetwork must integrate graphical MacGatewayLauncher before a native package can be built.")
    minimum = "26.0" if architecture == "arm64" else "15.0"
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=True)
    output = args.output / f"MultiRoute-Resume-{version}-macOS-{architecture}.pkg"
    if output.exists() or (args.output / (APP_NAME + ".app")).exists():
        parser.error("An installer already exists at the output path; use a fresh output directory.")
    run([sys.executable, ROOT / "scripts/build-java.py", "--javac", jdk / "bin/javac", "--platform", "macos"])
    dependencies = json.loads((ROOT / "scripts/dependencies.sha256.json").read_text(encoding="utf-8"))
    jars = sorted(path for path in (ROOT / "lib").glob("*.jar")
                  if not path.name.startswith(("jlibtorrent-linux-", "jlibtorrent-windows-", "jlibtorrent-macosx-"))
                  or path.name.startswith("jlibtorrent-macosx-" + architecture + "-"))
    for path in jars:
        if dependencies.get(path.name) != sha256(path):
            raise SystemExit("Dependency checksum mismatch: " + path.name)
    with tempfile.TemporaryDirectory(prefix="multiroute-macos-package-") as temporary:
        stage = Path(temporary)
        input_dir = stage / "input"
        (input_dir / "lib").mkdir(parents=True)
        for path in jars:
            shutil.copy2(path, input_dir / "lib" / path.name)
        write_platform_jar(ROOT / "MultiRoute-Resume.jar", input_dir / "MultiRoute-Resume.jar", jars, version)
        documentation_names = ("README.md", "README.ru.md", "CHECKS.ru.md", "TEST-REPORT.json",
                               "DEPLOYMENT-TEST-REPORT.json", "native/source/BUILD-README.txt")
        for name in ("LICENSE.txt", "LICENSING.md", "THIRD-PARTY-NOTICES.txt", "API.ru.txt", "openapi.yaml", *documentation_names):
            if (ROOT / name).is_file():
                (input_dir / name).parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(ROOT / name, input_dir / name)
        shutil.copytree(ROOT / "docs", input_dir / "docs")
        documentation_files = [ROOT / name for name in documentation_names]
        documentation_files.extend(path for path in sorted((ROOT / "docs").rglob("*")) if path.is_file())
        documentation_hashes = {path.relative_to(ROOT).as_posix(): sha256(path) for path in documentation_files}
        shutil.copytree(ROOT / "lib/licenses", input_dir / "lib/licenses")
        runtime = stage / "runtime"
        run([jdk / "bin/jlink", "--add-modules", "java.se,jdk.httpserver,jdk.crypto.ec,jdk.unsupported,jdk.charsets,jdk.localedata",
             "--bind-services", "--strip-debug", "--no-header-files", "--no-man-pages", "--output", runtime])
        modules = run([runtime / "bin/java", "--list-modules"], capture_output=True, text=True)
        present = {line.split("@", 1)[0] for line in modules.stdout.splitlines()}
        if not REQUIRED_MODULES.issubset(present):
            raise SystemExit("The bundled runtime is missing required application modules.")
        run([runtime / "bin/java", "--validate-modules"])
        images = stage / "images"
        run([jdk / "bin/jpackage", "--type", "app-image", "--name", APP_NAME,
             "--input", input_dir, "--main-jar", "MultiRoute-Resume.jar", "--main-class", "multiroute.DesktopLauncher",
             "--runtime-image", runtime, "--dest", images, "--app-version", version,
             "--vendor", "2018-spec-fdsds", "--description", "Parallel resumable downloads through your selected LAN gateways",
             "--mac-package-identifier", BUNDLE_ID, "--mac-app-category", "public.app-category.utilities",
             "--java-options", "-Dmultiroute.native.desktop=true"])
        app = images / (APP_NAME + ".app")
        contents = app / "Contents"
        plist_path = contents / "Info.plist"
        with plist_path.open("rb") as stream:
            metadata = plistlib.load(stream)
        metadata["LSMinimumSystemVersion"] = minimum
        metadata["NSLocalNetworkUsageDescription"] = "MultiRoute Resume uses your selected local Internet gateways without changing system routes."
        metadata["CFBundleLocalizations"] = ["en", "ru", "de", "es", "fr", "zh-Hans", "ko", "ar", "it"]
        metadata["MultiRouteProjectOwnerURL"] = owner_url
        plist_write(plist_path, metadata)
        localize_network_prompt(contents)
        run(["/usr/bin/plutil", "-lint", plist_path])
        helper = stage / "gateway-net"
        environment = dict(os.environ, ARCH=architecture, MULTIROUTE_BUILD_OUTPUT=str(helper))
        run(["/bin/bash", ROOT / "native/source/build_macos.sh"], env=environment)
        launcher = stage / "gateway-launch"
        clang = run(["/usr/bin/xcrun", "--find", "clang"], capture_output=True, text=True).stdout.strip()
        sdk = run(["/usr/bin/xcrun", "--sdk", "macosx", "--show-sdk-path"], capture_output=True, text=True).stdout.strip()
        run([clang, "-std=c11", "-O2", "-fstack-protector-strong", "-Wall", "-Wextra", "-Werror=format",
             "-Werror=implicit-function-declaration", "-isysroot", sdk, "-arch", architecture, "-mmacosx-version-min=11.0",
             '-DMULTIROUTE_VERSION="' + version + '"', HERE / "gateway_launch.c", "-o", launcher])
        run(["/usr/bin/codesign", "--force", "--sign", "-", launcher])
        run([launcher, "--self-test"])
        if args.signing_user:
            for binary in (helper, launcher):
                run(["/usr/bin/codesign", "--force", "--options", "runtime", "--timestamp", "--sign",
                     "Developer ID Application: " + args.signing_user, binary])
            run([jdk / "bin/jpackage", "--type", "app-image", "--app-image", app, "--mac-sign",
                 "--mac-signing-key-user-name", args.signing_user, "--mac-entitlements", HERE / "entitlements.plist"])
        else:
            run(["/usr/bin/codesign", "--force", "--deep", "--sign", "-", app])
        run(["/usr/bin/codesign", "--verify", "--deep", "--strict", app])
        for binary in (helper, launcher):
            run(["/usr/bin/codesign", "--verify", "--strict", binary])
        helper_digest, launcher_digest = sha256(helper), sha256(launcher)
        payload = stage / "payload"
        (payload / "Applications").mkdir(parents=True)
        shutil.copytree(app, payload / "Applications" / app.name, symlinks=True)
        helper_directory = payload / "Library/PrivilegedHelperTools/MultiRouteResume" / version
        helper_directory.mkdir(parents=True)
        for binary, digest in ((helper, helper_digest), (launcher, launcher_digest)):
            target = helper_directory / binary.name
            shutil.copy2(binary, target)
            target.chmod(0o755)
            marker = target.with_name(target.name + ".sha256")
            marker.write_text(digest + "\n", encoding="ascii")
            marker.chmod(0o644)
        installer_scripts = stage / "installer-scripts"
        script_templates(installer_scripts, version, helper_digest, launcher_digest)
        components = stage / "components.plist"
        run(["/usr/bin/pkgbuild", "--analyze", "--root", payload, components])
        with components.open("rb") as stream:
            component_data = plistlib.load(stream)
        for component in component_data:
            component["BundleIsRelocatable"] = False
            component["BundleIsVersionChecked"] = True
            component["BundleHasStrictIdentifier"] = True
            component["BundleOverwriteAction"] = "upgrade"
        plist_write(components, component_data)
        package_dir = stage / "packages"
        package_dir.mkdir()
        component_package = package_dir / "MultiRoute-Resume-component.pkg"
        run(["/usr/bin/pkgbuild", "--root", payload, "--identifier", BUNDLE_ID, "--version", version,
             "--ownership", "recommended", "--install-location", "/", "--component-plist", components,
             "--scripts", installer_scripts, component_package])
        distribution = stage / "Distribution.xml"
        distribution.write_bytes(distribution_xml(component_package.name, version, architecture, minimum))
        installer_resources = stage / "installer-resources"
        installer_resources.mkdir()
        shutil.copy2(license_file, installer_resources / "LICENSE.txt")
        command = ["/usr/bin/productbuild", "--distribution", distribution, "--package-path", package_dir,
                   "--resources", installer_resources]
        if args.signing_user:
            command += ["--sign", "Developer ID Installer: " + args.signing_user]
        command += [output]
        run(command)
        if args.notarize_profile:
            run(["/usr/bin/xcrun", "notarytool", "submit", output, "--keychain-profile", args.notarize_profile, "--wait"])
            run(["/usr/bin/xcrun", "stapler", "staple", output])
            run(["/usr/bin/xcrun", "stapler", "validate", output])
        app_output = args.output / app.name
        if app_output.exists():
            raise SystemExit("Refusing to overwrite a previous app image: " + str(app_output))
        shutil.copytree(app, app_output, symlinks=True)
        build_report = {
            "version": version, "architecture": architecture, "minimum_macos": minimum,
            "package": output.name, "sha256": sha256(output), "helper_sha256": helper_digest,
            "gateway_launcher_sha256": launcher_digest, "helper_output_limit_bytes": 65536,
            "license_sha256": sha256(license_file), "project_owner_url": owner_url,
            "installer_displays_license": True,
            "bundled_documentation_sha256": documentation_hashes,
            "bundled_java": int(found.group(1)), "modules": sorted(present),
            "dependencies": {path.name: sha256(path) for path in jars},
            "developer_id_signed": bool(args.signing_user), "notarized": bool(args.notarize_profile),
            "terminal_required_for_installed_application": False,
            "setup_language": "en",
            "desktop_shortcut": {
                "name": "MultiRoute Resume.app", "target": "/Applications/MultiRoute Resume.app",
                "installer_creation_context": "validated console user via sudo, never a root home write",
                "existing_desktop_items_replaced": False,
                "failure_policy": "best effort during setup; normal-user first GUI launch retries",
            },
            "privileged_java": False, "system_routes_changed": False,
            "verification": "Compiled and packaged on this runner; local API/native self-tests run separately.",
            "not_verified": ["physical LAN gateways", "BPF access authorization dialog", "Finder/Gatekeeper on an end-user Mac"],
        }
        (args.output / "macos-build-report.json").write_text(json.dumps(build_report, indent=2) + "\n", encoding="utf-8")
        print("Built " + str(output))


if __name__ == "__main__":
    main()
