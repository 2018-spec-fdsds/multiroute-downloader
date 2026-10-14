#!/usr/bin/env python3
"""Build a per-user Windows EXE installer and a native application image.

All downloads and build outputs stay in the selected build directory. No driver,
system Java, registry setting or live application instance is changed.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import urllib.request
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[2]
TOOLS = {
    "jdk": {
        "file": "temurin21-jdk.zip",
        "url": "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip",
        "sha256": "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e",
        "limit": 230_000_000,
        "root": "jdk-21.0.12.1+1",
    },
    "jre": {
        "file": "temurin21-jre.zip",
        "url": "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip",
        "sha256": "d35f31e712f0fcf6ac5a093edc90204fbff22f720ba3950bd09d331d5e621636",
        "limit": 65_000_000,
        "root": "jdk-21.0.12.1+1-jre",
    },
    "wix": {
        "file": "wix314-binaries.zip",
        "url": "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip",
        "sha256": "6ac824e1642d6f7277d0ed7ea09411a508f6116ba6fae0aa5f2c7daa2ff43d31",
        "limit": 60_000_000,
        "root": "",
    },
}
APP_NAME = "MultiRoute Resume"
UPGRADE_UUID = "fa8e2b12-2c41-4662-9c07-9c9fafbc3e1f"


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def run(command, log=None, env=None):
    result = subprocess.run([str(p) for p in command], env=env,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    output = result.stdout + result.stderr
    if log:
        Path(log).write_text(output, encoding="utf-8")
    if result.returncode:
        raise RuntimeError(output[-12000:])
    return output


def extract_zip(archive, destination):
    """Extract new owned paths only, rejecting traversal, links and duplicates."""
    destination.mkdir(parents=True, exist_ok=False)
    resolved = destination.resolve()
    seen = set()
    total = 0
    with zipfile.ZipFile(archive) as source:
        if not 0 < len(source.infolist()) < 15000:
            raise ValueError("Unexpected tool archive entry count")
        for entry in source.infolist():
            name = entry.filename
            parts = name.rstrip("/").split("/")
            if not name or "\\" in name or ":" in name or name.startswith("/"):
                raise ValueError("Unsafe archive path")
            if any(p in ("", ".", "..") or p.endswith((" ", ".")) or
                   p.split(".")[0].upper() in {"CON", "PRN", "AUX", "NUL", *["COM"+str(n) for n in range(1, 10)], *["LPT"+str(n) for n in range(1, 10)]}
                   for p in parts):
                raise ValueError("Unsafe Windows archive component")
            mode = (entry.external_attr >> 16) & 0o170000
            if mode == 0o120000 or entry.external_attr & 0x400:
                raise ValueError("Archive links are forbidden")
            target = (destination / name).resolve()
            if not target.is_relative_to(resolved) or str(target).casefold() in seen:
                raise ValueError("Duplicate or outside archive path")
            seen.add(str(target).casefold())
            total += entry.file_size
            if total > 1_500_000_000:
                raise ValueError("Tool archive exceeds extraction limit")
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with source.open(entry) as incoming, target.open("xb") as outgoing:
                    shutil.copyfileobj(incoming, outgoing)
                if target.stat().st_size != entry.file_size:
                    raise ValueError("Incomplete archive entry")


def fetch_tool(tool, folder):
    metadata = TOOLS[tool]
    archive = folder / metadata["file"]
    folder.mkdir(parents=True, exist_ok=True)
    if not archive.exists():
        temporary = archive.with_suffix(".download")
        request = urllib.request.Request(metadata["url"], headers={"User-Agent": "MultiRoute-Resume-build/3.2"})
        with urllib.request.urlopen(request, timeout=60) as incoming, temporary.open("xb") as outgoing:
            if not incoming.url.startswith("https://"):
                raise ValueError("Build tool redirect must use HTTPS")
            received = 0
            while chunk := incoming.read(1024 * 1024):
                received += len(chunk)
                if received > metadata["limit"]:
                    raise ValueError("Build tool download exceeds limit")
                outgoing.write(chunk)
        if sha256(temporary) != metadata["sha256"]:
            raise ValueError("Official build tool SHA-256 mismatch: " + tool)
        temporary.replace(archive)
    if sha256(archive) != metadata["sha256"]:
        raise ValueError("Cached build tool SHA-256 mismatch: " + tool)
    extracted = folder / tool
    if not extracted.exists():
        extract_zip(archive, extracted)
    return extracted / metadata["root"] if metadata["root"] else extracted


def icon(path):
    """An owned geometric icon; no external art or image dependencies."""
    size = 64
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            pixel = (19, 74, 110, 255)
            for cx, cy in ((12, 14), (25, 14), (38, 14), (51, 14)):
                if (x-cx)**2 + (y-cy)**2 <= 25:
                    pixel = (44, 200, 169, 255)
            if (28 <= x <= 35 and 25 <= y <= 42) or (38 <= y <= 50 and abs(x-31.5) <= 50-y):
                pixel = (255, 255, 255, 255)
            if 18 <= x <= 46 and 53 <= y <= 57:
                pixel = (255, 255, 255, 255)
            raw.extend(pixel)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind+data) & 0xffffffff)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw))) + chunk(b"IEND", b"")
    path.write_bytes(struct.pack("<HHH", 0, 1, 1) + struct.pack("<BBBBHHII", size, size, 0, 0, 1, 32, len(png), 22) + png)


def copy_file(source, target):
    if not source.is_file() or source.is_symlink():
        raise ValueError("Missing ordinary payload file: " + str(source))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="3.2")
    parser.add_argument("--jar", type=Path, default=ROOT / "MultiRoute-Resume.jar")
    parser.add_argument("--native-helper", type=Path, default=ROOT / "native/windows-x64/gateway-net.exe")
    parser.add_argument("--build-dir", type=Path, default=ROOT / "build/windows-package")
    parser.add_argument("--tools-dir", type=Path, default=ROOT / "build/windows-tools")
    parser.add_argument("--fetch-tools", action="store_true", help="Download pinned official portable build tools and the licensed JRE")
    parser.add_argument("--tools-only", action="store_true", help="Fetch/locate tools before compiling the current source JAR")
    parser.add_argument("--jpackage", type=Path)
    parser.add_argument("--wix", type=Path)
    parser.add_argument("--runtime", type=Path)
    parser.add_argument("--main-class", default="multiroute.DesktopLauncher")
    parser.add_argument("--image-only", action="store_true", help="Build the native application image without invoking the Windows installer compiler")
    args = parser.parse_args()
    if os.name != "nt":
        parser.error("Windows packages must be built on Windows, as required by jpackage.")
    if not args.version.replace(".", "").isdigit():
        parser.error("Use a numeric application version, for example 3.2.")
    if args.fetch_tools:
        jdk = fetch_tool("jdk", args.tools_dir.resolve())
        jre = fetch_tool("jre", args.tools_dir.resolve())
        wix = fetch_tool("wix", args.tools_dir.resolve())
        args.jpackage = args.jpackage or jdk / "bin/jpackage.exe"
        args.runtime = args.runtime or jre
        args.wix = args.wix or wix
    if args.tools_only:
        if not args.fetch_tools:
            parser.error("Use --fetch-tools with --tools-only.")
        print(json.dumps({"jpackage": str(args.jpackage), "javac": str(jdk/"bin/javac.exe"),
                          "runtime": str(args.runtime), "wix": str(args.wix)}, indent=2))
        return
    with zipfile.ZipFile(args.jar) as archive:
        if args.main_class.replace(".", "/") + ".class" not in archive.namelist():
            parser.error("Build the current Java sources first; the requested native entry point is absent.")
    if not args.jpackage or not args.runtime or (not args.image_only and not args.wix):
        parser.error("Pass --fetch-tools or provide --jpackage, --runtime and --wix explicitly.")
    build = args.build_dir.resolve()
    # Never delete an existing build directory: earlier packages remain reviewable.
    build.mkdir(parents=True, exist_ok=False)
    inputs = build / "input"
    inputs.mkdir()
    properties = run([args.runtime / "bin/java.exe", "-XshowSettings:properties", "-version"])
    runtime_properties = dict(re.findall(r"(?m)^\s*([a-z.]+)\s*=\s*([^\r\n]*)$", properties))
    if runtime_properties.get("java.version") != "21.0.12.1" or runtime_properties.get("os.arch") != "amd64" or runtime_properties.get("java.vendor") != "Eclipse Adoptium":
        raise ValueError("The bundled runtime must be the pinned full x64 Temurin 21.0.12.1+1 runtime")
    modules = run([args.runtime / "bin/java.exe", "--list-modules"])
    for module in ("java.desktop", "java.net.http", "jdk.httpserver", "java.xml"):
        if not any(line.split("@", 1)[0] == module for line in modules.splitlines()):
            raise ValueError("Bundled Java runtime is missing " + module)
    run([args.runtime / "bin/java.exe", "--validate-modules"])
    copy_file(args.jar, inputs / "MultiRoute-Resume.jar")
    dependencies = json.loads((ROOT / "scripts/dependencies.sha256.json").read_text(encoding="utf-8"))
    for name in ("jlibtorrent-2.0.12.9.jar", "jlibtorrent-windows-2.0.12.9.jar", "jsch-2.28.8.jar"):
        source = ROOT / "lib" / name
        if sha256(source) != dependencies[name]:
            raise ValueError("Dependency SHA-256 mismatch: " + name)
        copy_file(source, inputs / "lib" / name)
    shutil.copytree(ROOT / "lib/licenses", inputs / "lib/licenses")
    copy_file(ROOT / "native/source/lwip/COPYING", inputs / "native/LICENSE-lwIP.txt")
    copy_file(ROOT / "native/source/BUILD-README.txt", inputs / "native/source/BUILD-README.txt")
    copy_file(ROOT / "THIRD-PARTY-NOTICES.txt", inputs / "THIRD-PARTY-NOTICES.txt")
    copy_file(args.native_helper, inputs / "native/windows-x64/gateway-net.exe")
    for name in ("bootstrap.ps1", "bootstrap-common.psm1", "NativePrerequisites.ps1"):
        copy_file(ROOT / "windows" / name, inputs / "windows" / name)
    for source in sorted((ROOT / "docs").rglob("*")):
        if source.is_file() and source.suffix.lower() in {".md", ".png", ".svg", ".jpg", ".jpeg", ".json"}:
            copy_file(source, inputs / "docs" / source.relative_to(ROOT / "docs"))
    for source in (ROOT / "README.md", ROOT / "README.ru.md", ROOT / "LICENSE.txt", ROOT / "LICENSING.md", ROOT / "API.ru.txt", ROOT / "openapi.yaml", ROOT / "CHECKS.ru.md", ROOT / "TEST-REPORT.json", ROOT / "DEPLOYMENT-TEST-REPORT.json"):
        if source.is_file():
            copy_file(source, inputs / source.name)
    app_icon = build / "MultiRoute-Resume.ico"
    icon(app_icon)
    image_folder = build / "image"
    image_folder.mkdir()
    image_args = [args.jpackage, "-J-Duser.language=en", "-J-Duser.country=US", "--type", "app-image", "--input", inputs, "--dest", image_folder,
                  "--name", APP_NAME, "--app-version", args.version,
                  "--vendor", "2018-spec-fdsds", "--description", "Downloads over multiple LAN Internet gateways",
                  "--main-jar", "MultiRoute-Resume.jar", "--main-class", args.main_class,
                  "--runtime-image", args.runtime, "--icon", app_icon,
                  "--java-options", "-Dmultiroute.native.desktop=true"]
    run(image_args, build / "jpackage-image.log")
    image = image_folder / APP_NAME
    executable = image / (APP_NAME + ".exe")
    if not executable.is_file():
        raise ValueError("jpackage did not create the native application EXE")
    report = {
        "application_version": args.version,
        "format": "Windows x64 native EXE application and per-user EXE installer",
        "entry_point": args.main_class,
        "bundled_runtime": "Eclipse Temurin 21.0.12.1+1 x64 JRE (full modules, licenses retained)",
        "java_runtime_version": runtime_properties.get("java.runtime.version"),
        "jdk_sha256": TOOLS["jdk"]["sha256"],
        "jre_archive_sha256": TOOLS["jre"]["sha256"],
        "wix_archive_sha256": TOOLS["wix"]["sha256"],
        "java_jar_sha256": sha256(args.jar),
        "native_helper_sha256": sha256(args.native_helper),
        "launcher_sha256": sha256(executable),
        "driver_bundled": False,
        "installation_scope": "Current user, Start menu and desktop shortcuts by default; Windows uninstall entry",
        "installer_language": "English (en-US, Windows Installer language 1033)",
        "desktop_shortcut_default": True,
        "app_image_files": {p.relative_to(image).as_posix(): sha256(p) for p in sorted(image.rglob("*")) if p.is_file()},
        "limitations": ["Installer and launcher are unsigned; Windows may show an unrecognized-app confirmation.",
                        "Npcap is downloaded from its official source at first launch if missing, after checksum and publisher checks, using its normal interactive UAC installer.",
                        "No Npcap installation, OS installer registration or capture traffic was exercised by this build."],
    }
    if not args.image_only:
        installer_folder = build / "installer"
        installer_folder.mkdir()
        env = os.environ.copy()
        env["PATH"] = str(args.wix.resolve()) + os.pathsep + env.get("PATH", "")
        package_args = [args.jpackage, "-J-Duser.language=en", "-J-Duser.country=US", "--verbose", "--type", "exe", "--app-image", image,
                        "--dest", installer_folder, "--name", APP_NAME, "--app-version", args.version,
                        "--vendor", "2018-spec-fdsds", "--description", "Downloads over multiple LAN Internet gateways",
                        "--win-per-user-install", "--install-dir", "MultiRoute Resume App", "--win-dir-chooser", "--win-menu", "--win-menu-group", APP_NAME,
                        "--win-shortcut", "--win-upgrade-uuid", UPGRADE_UUID,
                        "--license-file", ROOT / "LICENSE.txt",
                        "--about-url", "https://github.com/2018-spec-fdsds/multiroute-resume",
                        "--win-help-url", "https://github.com/2018-spec-fdsds/multiroute-resume/blob/main/docs/en.md"]
        run(package_args, build / "jpackage-installer.log", env=env)
        packages = sorted(installer_folder.glob("*.exe"))
        if len(packages) != 1:
            raise ValueError("Expected one native EXE installer")
        installer = packages[0]
        public_name = installer.with_name("MultiRoute-Resume-" + args.version + "-Windows-x64-Setup.exe")
        installer.rename(public_name)
        report["installer"] = {"name": public_name.name, "size": public_name.stat().st_size, "sha256": sha256(public_name)}
    (build / "BUILD-REPORT.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "app_image_files"}, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
