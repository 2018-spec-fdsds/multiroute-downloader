#!/usr/bin/env python3
"""Owned fixtures for archive validation used by the native Windows builder."""
from pathlib import Path
import argparse
import importlib.util
import json
import uuid
import zipfile

spec = importlib.util.spec_from_file_location("native_windows_build", Path(__file__).with_name("build.py"))
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--fixture-dir", required=True, type=Path)
args = parser.parse_args()
fixture = args.fixture_dir.resolve()
fixture.mkdir(parents=True, exist_ok=False)
results = []


def check(name, entries, allowed=False):
    archive = fixture / (uuid.uuid4().hex + ".zip")
    destination = fixture / uuid.uuid4().hex
    with zipfile.ZipFile(archive, "w") as output:
        for entry, content in entries:
            output.writestr(entry, content)
    error = None
    try:
        builder.extract_zip(archive, destination)
    except ValueError as failure:
        error = failure
    passed = error is None if allowed else error is not None
    results.append({"name": name, "passed": passed})
    if not passed:
        raise AssertionError(name)
    return destination


normal = check("normal pinned tool-style archive", [("jdk/bin/java.exe", b"owned test data"), ("jdk/legal/LICENSE", b"license")], allowed=True)
assert (normal / "jdk/bin/java.exe").read_bytes() == b"owned test data"
check("parent traversal rejected", [("../outside-owned-sentinel", b"bad")])
assert not (fixture / "outside-owned-sentinel").exists()
check("absolute path rejected", [("/outside", b"bad")])
check("Windows drive path rejected", [("C:/outside", b"bad")])
check("Windows separators rejected", [("foo\\..\\outside", b"bad")])
check("reserved Windows device rejected", [("jdk/NUL.txt", b"bad")])
check("trailing dot rejected", [("jdk/bin./java.exe", b"bad")])
check("case-fold duplicate rejected", [("jdk/A", b"one"), ("jdk/a", b"two")])
link = zipfile.ZipInfo("jdk/bin/link")
link.create_system = 3
link.external_attr = 0o120777 << 16
check("symbolic link rejected", [(link, b"../../outside")])
report = {"status": "PASS", "checks": len(results), "results": results, "installer_executed": False, "user_data_accessed": False}
(fixture / "REPORT.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
