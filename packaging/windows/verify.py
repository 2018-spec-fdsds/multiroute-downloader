#!/usr/bin/env python3
"""Verify native Windows payload and inspect the embedded MSI without installing."""
from pathlib import Path
import argparse
import collections
import hashlib
import json
import os
import struct
import subprocess


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024*1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def pe(data):
    if data[:2] != b"MZ":
        raise ValueError("Expected a PE executable")
    header = struct.unpack_from("<I", data, 60)[0]
    if data[header:header+4] != b"PE\x00\x00":
        raise ValueError("Missing PE signature")
    machine, sections = struct.unpack_from("<HH", data, header+4)
    optional_size = struct.unpack_from("<H", data, header+20)[0]
    optional = header+24
    magic = struct.unpack_from("<H", data, optional)[0]
    subsystem = struct.unpack_from("<H", data, optional+68)[0]
    table = optional+optional_size
    ranges = []
    for index in range(sections):
        at = table+index*40
        virtual_size, rva, raw_size, offset = struct.unpack_from("<IIII", data, at+8)
        ranges.append((rva, max(virtual_size, raw_size), offset))
    def location(rva):
        for start, length, offset in ranges:
            if start <= rva < start+length:
                return offset+rva-start
        raise ValueError("PE resource RVA is outside a section")
    directory = optional+(112 if magic == 0x20b else 96)
    resource_rva, resource_size = struct.unpack_from("<II", data, directory+16)
    entries = []
    if resource_rva:
        base = location(resource_rva)
        def walk(relative, path, depth=0):
            if depth > 4:
                raise ValueError("Unexpected PE resource depth")
            at = base+relative
            named, numbered = struct.unpack_from("<HH", data, at+12)
            for index in range(named+numbered):
                name, value = struct.unpack_from("<II", data, at+16+index*8)
                if name & 0x80000000:
                    start = base+(name & 0x7fffffff)
                    count = struct.unpack_from("<H", data, start)[0]
                    label = data[start+2:start+2+count*2].decode("utf-16-le")
                else:
                    label = str(name)
                if value & 0x80000000:
                    walk(value & 0x7fffffff, path+[label], depth+1)
                else:
                    rva, size = struct.unpack_from("<II", data, base+value)
                    offset = location(rva)
                    entries.append((path+[label], data[offset:offset+size]))
        walk(0, [])
    return machine, subsystem, entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-dir", required=True, type=Path)
    args = parser.parse_args()
    build = args.build_dir.resolve()
    original = json.loads((build/"BUILD-REPORT.json").read_text(encoding="utf-8"))
    image = build/"image/MultiRoute Resume"
    checks = []
    def check(name, condition):
        checks.append({"name": name, "passed": bool(condition)})
        if not condition:
            raise ValueError("Verification failed: "+name)
    for name, expected in original["app_image_files"].items():
        check("image SHA-256 "+name, sha256(image/name) == expected)
    filenames = {p.relative_to(image).as_posix() for p in image.rglob("*") if p.is_file()}
    check("image inventory exact", filenames == set(original["app_image_files"]))
    check("no profile or download state bundled", not any(p.startswith("app/data/") or p.endswith((".config", ".part", ".log", ".instance")) for p in filenames))
    machine, subsystem, launcher_resources = pe((image/"MultiRoute Resume.exe").read_bytes())
    check("native launcher is x64 PE", machine == 0x8664)
    check("native launcher is GUI without console", subsystem == 2)
    manifests = [data.decode("utf-8", errors="replace") for names, data in launcher_resources if names[0] == "24"]
    check("native launcher requests ordinary user privileges", len(manifests) == 1 and 'level="asInvoker"' in manifests[0])
    config = (image/"app/MultiRoute Resume.cfg").read_text(encoding="utf-8")
    check("entry point matches build", "app.mainclass="+original["entry_point"] in config)
    check("native package Java flag set", "-Dmultiroute.native.desktop=true" in config or "-Dmultiroute.native.package=true" in config)
    check("private full licensed runtime bundled", (image/"runtime/bin/java.exe").is_file() and (image/"runtime/legal/java.base/LICENSE").is_file())
    check("only Windows JNI dependency bundled", (image/"app/lib/jlibtorrent-windows-2.0.12.9.jar").is_file() and not any(p.name.startswith(("jlibtorrent-linux-", "jlibtorrent-macosx-")) for p in (image/"app/lib").glob("*.jar")))
    installer_info = original["installer"]
    installer = build/"installer"/installer_info["name"]
    check("installer SHA-256 matches", sha256(installer) == installer_info["sha256"])
    check("installer size matches", installer.stat().st_size == installer_info["size"])
    machine, subsystem, resources = pe(installer.read_bytes())
    check("installer is GUI PE", subsystem == 2)
    msi_resources = [(path, data) for path, data in resources if data.startswith(bytes.fromhex("d0cf11e0a1b11ae1"))]
    check("one embedded MSI resource", len(msi_resources) == 1)
    verify = build/"verification"
    verify.mkdir(exist_ok=True)
    embedded = verify/"installer-payload.msi"
    embedded.write_bytes(msi_resources[0][1])
    msi_report = verify/"MSI-INSPECTION.json"
    ps = Path(os.environ["SystemRoot"])/"System32/WindowsPowerShell/v1.0/powershell.exe"
    subprocess.run([str(ps), "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                    str(Path(__file__).with_name("inspect-msi.ps1")), "-Path", str(embedded), "-Report", str(msi_report)], check=True)
    msi = json.loads(msi_report.read_text(encoding="utf-8-sig"))
    properties = {item["Property"]: item["Value"] for item in msi["properties"]}
    check("installer version matches", properties.get("ProductVersion") == original["application_version"])
    # An absent/empty ALLUSERS installs per user, per Windows Installer semantics.
    check("installer is per-user", not properties.get("ALLUSERS"))
    directory_table = {item["Directory"]: item for item in msi["directories"]}
    cursor = "INSTALLDIR"
    install_chain = []
    while cursor and cursor in directory_table:
        row = directory_table[cursor]
        install_chain.append(row)
        cursor = row["Parent"]
    check("installer defaults to user LocalAppData", any(row["Directory"] == "LocalAppDataFolder" for row in install_chain))
    if original["entry_point"] == "multiroute.DesktopLauncher":
        check("installer separates app files from user profile", directory_table["INSTALLDIR"]["DefaultDir"].split("|")[-1] == "MultiRoute Resume App")
        check("original project license included in installed files", (image/"app/LICENSE.txt").is_file())
        check("installer license confirmation dialog included", any(row["Dialog"] == "LicenseAgreementDlg" for row in msi["dialogs"]))
        check("installer displays the project's license text", any(row["Type"] == "ScrollableText" and "MULTIROUTE RESUME LICENSE" in row["Text"] for row in msi["license_controls"]))
    check("Windows uninstall entry enabled", properties.get("ARPNOREMOVE") != "1")
    shortcut_paths = {item["Directory"] for item in msi["shortcuts"]}
    check("desktop shortcut declared", "DesktopFolder" in shortcut_paths)
    check("Start menu shortcut declared", len(shortcut_paths-{"DesktopFolder"}) > 0)
    msi_files = {item["File"]: item["FileName"].split("|")[-1] for item in msi["files"]}
    check("all shortcuts launch native EXE", all(item["Target"].startswith("[#") and msi_files.get(item["Target"][2:-1]) == "MultiRoute Resume.exe" for item in msi["shortcuts"]))
    # jpackage replaces its build-image marker .jpackage.xml with .package in MSI.
    file_lengths = sorted(int(item["FileSize"]) for item in msi["files"] if item["FileName"].split("|")[-1] != ".package")
    payload_names = {name for name in filenames if name != "app/.jpackage.xml"}
    check("MSI file count matches image", len(file_lengths) == len(payload_names))
    check("MSI file sizes match image", file_lengths == sorted((image/name).stat().st_size for name in payload_names))
    # Extract the embedded cabinets with Microsoft's archive tool, not msiexec.
    # Cabinet file names are the generated File-table identifiers in this owned MSI.
    cabinet_output = verify/"cabinet-files"
    cabinet_output.mkdir(exist_ok=True)
    expand = Path(os.environ["SystemRoot"])/"System32/expand.exe"
    cabinets = msi["cabinets"] if isinstance(msi["cabinets"], list) else [msi["cabinets"]]
    for cabinet in cabinets:
        subprocess.run([str(expand), "-F:*", str(verify/cabinet), str(cabinet_output)], check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    actual_files = {p.name: p for p in cabinet_output.iterdir() if p.is_file()}
    check("all cabinet files declared in MSI", set(actual_files) == set(msi_files))
    image_hashes = collections.Counter(sha256(image/name) for name in payload_names)
    installed_hashes = collections.Counter(sha256(actual_files[item["File"]]) for item in msi["files"] if item["FileName"].split("|")[-1] != ".package")
    check("all embedded application and Java runtime file SHA-256 values match image", image_hashes == installed_hashes)
    output = {"status": "PASS", "checks": len(checks), "results": checks,
              "installer": installer_info, "MSI_properties": properties,
              "MSI_directories": msi["directories"], "MSI_shortcuts": msi["shortcuts"],
              "embedded_msi_sha256": sha256(embedded), "installer_executed": False,
              "Npcap_installer_executed": False, "user_downloads_accessed": False}
    (build/"VERIFICATION-REPORT.json").write_text(json.dumps(output, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")
    print(json.dumps({key: output[key] for key in ("status", "checks", "installer", "MSI_shortcuts")}, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
