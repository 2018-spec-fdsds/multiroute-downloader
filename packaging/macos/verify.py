#!/usr/bin/env python3
"""Verify the built Mac app/installer with owned fixtures; never authorize BPF or change the OS."""
from __future__ import annotations

import argparse
import base64
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
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


REQUIRED = {"java.base", "java.desktop", "java.net.http", "java.xml", "jdk.httpserver"}
ROOT = Path(__file__).resolve().parents[2]
LANGUAGES = ("en", "ru", "de", "es", "fr", "zh", "ko", "ar", "it", "ja", "hi", "vi")
SCREENSHOTS = ("desktop-downloads-en.png", "desktop-network-en.png", "desktop-bulk-import-en.png", "tray-speeds-3.3.png",
               "desktop-4.0-light-en.png", "desktop-4.0-dark-en.png")


def verify_documentation(app_dir: Path, report: dict) -> None:
    hashes = report["bundled_documentation_sha256"]
    required = {"README.md", "README.ru.md", "docs/COMPATIBILITY.json", "CHECKS.ru.md",
                "TEST-REPORT.json", "DEPLOYMENT-TEST-REPORT.json", "native/source/BUILD-README.txt"}
    required.update("docs/" + language + ".md" for language in LANGUAGES)
    required.update("docs/images/" + name for name in SCREENSHOTS)
    assert required.issubset(hashes), "The app must contain twelve local guides, READMEs, compatibility information and screenshots."
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


def verify_browser_extension(app_dir: Path, report: dict) -> int:
    expected = report["browser_extension_sha256"]
    actual = {path.relative_to(app_dir).as_posix() for path in (app_dir / "browser-extension").rglob("*") if path.is_file()}
    assert actual == set(expected) and len(actual) >= 20
    for name, digest in expected.items():
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest
        assert hashlib.sha256((app_dir / name).read_bytes()).hexdigest() == digest
    manifest = json.loads((app_dir / "browser-extension/manifest.json").read_text(encoding="utf-8"))
    assert manifest["manifest_version"] == 3 and manifest["version"] == report["version"] + ".0"
    assert set(manifest["permissions"]) == {"contextMenus", "activeTab", "storage"}
    assert manifest["host_permissions"] == ["http://127.0.0.1/*"]
    key = hashlib.sha256(base64.b64decode(manifest["key"], validate=True)).digest()[:16].hex()
    extension_id = "".join(chr(ord("a") + int(character, 16)) for character in key)
    assert extension_id == "ljdklefabmaecjalfhjoicmgedmjjkbl"
    assert {path.name for path in (app_dir / "browser-extension/_locales").iterdir() if path.is_dir()} == set(LANGUAGES) - {"zh"} | {"zh_CN"}
    return len(actual)


def run(command, **kwargs):
    if kwargs.get("text") or kwargs.get("universal_newlines"):
        kwargs.setdefault("encoding", "utf-8")
    try:
        return subprocess.run([str(value) for value in command], check=True, **kwargs)
    except subprocess.CalledProcessError as failed:
        # These are owned package fixtures. Preserve the actual assertion in CI
        # instead of losing captured output when the temporary folder is removed.
        for label, value in (("stdout", failed.stdout), ("stderr", failed.stderr)):
            if value:
                decoded = value.decode("utf-8", errors="replace") if isinstance(value, bytes) else value
                print("Owned command " + label + ":\n" + decoded, file=sys.stderr, flush=True)
        raise


def verify_native_source_pin(report: dict) -> int:
    inputs = [path for path in (ROOT / "native/source").rglob("*")
              if path.is_file() and path.suffix in {".c", ".h"}]
    inputs += [ROOT / "native/source/build_macos.sh", ROOT / "packaging/macos/gateway_launch.c"]
    actual = {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in sorted(inputs)}
    assert report["native_source_sha256"] == actual, "Native input set/bytes differ from the build snapshot."
    for name in ("gateway_diagnostics", "gateway_topology"):
        result = report["native_pure_fixtures"][name]
        assert result["ok"] and not result["capture_executed"] and not result["os_network_changed"]
        assert result["source_sha256"] == actual["native/source/tests/" + name + "_test.c"]
        for stream in ("stdout", "stderr"):
            assert hashlib.sha256(result[stream].encode("utf-8")).hexdigest() == result[stream + "_sha256"]
    return len(actual)


def verify_network42_fixtures(java: Path, jdk: Path, classpath: str, owned: Path, version: str) -> dict:
    """Use the real app JAR with fresh owned state and no installed profiles/helpers."""
    root = owned / "network42"
    classes = root / "classes"
    classes.mkdir(parents=True)
    fixtures = {
        "NetworkDiagnosticsTest42": ("NETWORK-DIAGNOSTICS-TESTS.json", True),
        "NetworkErrorJournalTest42": ("NETWORK-ERROR-JOURNAL-TESTS.json", True),
        "NetworkTopologyBridgeTest42": ("NETWORK-TOPOLOGY-BRIDGE-TESTS.json", True),
        "NetworkTopologyDesktopTest42": ("NETWORK-TOPOLOGY-DESKTOP-TESTS.json", False),
    }
    run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath,
         "-d", classes, *[ROOT / "packaging/tests" / (name + ".java") for name in fixtures]], timeout=120)
    results = {}
    for name, (filename, headless) in fixtures.items():
        case = root / name
        for directory in ("home", "tmp", "state"):
            (case / directory).mkdir(parents=True)
        completed = run([java, "-Djava.awt.headless=" + str(headless).lower(), "-Dfile.encoding=UTF-8",
                         "-Duser.home=" + str(case / "home"), "-Djava.io.tmpdir=" + str(case / "tmp"),
                         "-cp", classpath + os.pathsep + str(classes), "multiroute." + name, case / "state"],
                        capture_output=True, text=True, timeout=120)
        result = json.loads((case / "state" / filename).read_text(encoding="utf-8"))
        assert result["status"] == "PASS" and result["checks"] > 0
        if "version" in result:
            assert result["version"] == version
        assert "\ufffd" not in completed.stdout
        if name == "NetworkDiagnosticsTest42":
            assert not result["real_gateways_used"] and not result["user_data_read"]
            assert result["network"] == "owned loopback proxy only"
        elif name == "NetworkErrorJournalTest42":
            assert not result["user_logs_read"] and not result["native_network_started"] and result["network_calls"] == 0
        elif name == "NetworkTopologyBridgeTest42":
            assert result["native_processes_started"] == result["network_calls"] == 0
            assert not result["os_routes_changed"] and not result["user_data_read"]
        else:
            assert result["real_network_calls"] == 0 and not result["os_network_changed"]
            assert set(result["languages"]) == set(LANGUAGES)
        results[name] = {"checks": result["checks"], "headless": headless,
                         "fixture_source_sha256": hashlib.sha256((ROOT / "packaging/tests" / (name + ".java")).read_bytes()).hexdigest()}
    return results


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
    shortcut = fresh / "Desktop/MultiRoute Downloader.app"
    assert invoke(fresh).returncode == 0 and shortcut.is_symlink()
    assert os.readlink(shortcut) == str(application) and shortcut.lstat().st_uid == uid
    original = shortcut.lstat().st_ino
    assert invoke(fresh).returncode == 0 and shortcut.lstat().st_ino == original
    regular = home("existing regular item")
    existing = regular / "Desktop/MultiRoute Downloader.app"
    existing.write_text("keep this owned item", encoding="utf-8")
    assert invoke(regular).returncode == 0 and existing.read_text(encoding="utf-8") == "keep this owned item"
    linked = home("existing broken link")
    existing_link = linked / "Desktop/MultiRoute Downloader.app"
    existing_link.symlink_to(fixtures / "not-present.app")
    original_target = os.readlink(existing_link)
    assert invoke(linked).returncode == 0 and os.readlink(existing_link) == original_target
    absent = home("missing Desktop", desktop=False)
    assert invoke(absent).returncode != 0 and not (absent / "Desktop").exists()
    redirected = home("redirected Desktop", desktop=False)
    other = fixtures / "other owned directory"
    other.mkdir()
    (redirected / "Desktop").symlink_to(other)
    assert invoke(redirected).returncode != 0 and not (other / "MultiRoute Downloader.app").exists()
    wrong_uid = home("wrong uid")
    assert invoke(wrong_uid, expected_uid=0).returncode != 0
    assert not (wrong_uid / "Desktop/MultiRoute Downloader.app").exists()
    absent_app = home("missing app")
    assert invoke(absent_app, target=fixtures / "missing app target").returncode != 0
    assert not (absent_app / "Desktop/MultiRoute Downloader.app").exists()
    return 8



def verify_checksum_fixture(java: Path, jdk: Path, classpath: str, owned: Path) -> int:
    """Exercise the packaged Java checksum and publication code without network or a Manager."""
    fixture = owned / "NativeChecksumFixture.java"
    fixture.write_text(r'''
package multiroute;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class NativeChecksumFixture {
    static int checks;
    static final byte[] ABC = "abc".getBytes(StandardCharsets.US_ASCII);
    static final Map<String,String> KNOWN = Map.of(
        "md5", "900150983cd24fb0d6963f7d28e17f72",
        "sha256", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        "sha512", "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
    static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    static void rejects(String algorithm, Object value) {
        try {
            Checksums.normalize(algorithm, value);
            throw new AssertionError("Invalid " + algorithm + " accepted");
        } catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createDirectories(Path.of(args[0]).toAbsolutePath());
        Path input = root.resolve("owned-abc.bin");
        Files.write(input, ABC);
        require(Checksums.ALGORITHMS.equals(List.of("md5", "sha256", "sha512")), "Three standard whole-file algorithms");
        require(Checksums.file(input).equals(KNOWN), "Bundled JRE fixed abc vectors for all three hashes");
        Checksums.verify(KNOWN, KNOWN);
        checks++;

        Map<String,Object> supplied = new LinkedHashMap<>();
        for (String algorithm : List.of("md5", "sha512")) {
            supplied.put("expected_" + algorithm, " " + KNOWN.get(algorithm).toUpperCase(Locale.ROOT) + " ");
            rejects(algorithm, "0".repeat(Checksums.hexLength(algorithm)-1));
            rejects(algorithm, "z".repeat(Checksums.hexLength(algorithm)));
            rejects(algorithm, 123);
        }
        require(Checksums.expected(supplied).equals(Map.of("md5", KNOWN.get("md5"), "sha512", KNOWN.get("sha512"))),
                "Manual MD5 and SHA512 references normalize without inventing another reference");
        checks++;

        for (String algorithm : List.of("md5", "sha512")) {
            Path output = Files.createDirectory(root.resolve("mismatch-" + algorithm));
            Path part = output.resolve("owned.bin.part");
            Files.write(part, ABC);
            Map<String,Object> state = new LinkedHashMap<>();
            state.put("id", "owned-" + algorithm);
            state.put("directory", output.toString()); state.put("filename", "owned.bin");
            state.put("part_path", part.toString()); state.put("size", 3L);
            state.put("status", "verifying"); state.put("routes", List.of());
            for (String hash : Checksums.ALGORITHMS) state.put("expected_" + hash,
                    hash.equals(algorithm) ? "0".repeat(Checksums.hexLength(hash)) : KNOWN.get(hash));
            Main.Job job = new Main.Job(state, root.resolve("owned-checkpoints"));
            boolean rejected = false;
            try { job.finish(); } catch (Checksum.ConflictException expected) { rejected = true; }
            require(rejected, "Real finalization must reject wrong " + algorithm);
            require(Files.isRegularFile(part, LinkOption.NOFOLLOW_LINKS) && Arrays.equals(Files.readAllBytes(part), ABC),
                    "Mismatched partial data preserved");
            require(!Files.exists(output.resolve("owned.bin"), LinkOption.NOFOLLOW_LINKS), "Wrong file is never published");
            require(Checksums.values(state).equals(KNOWN), "All actual hashes retained before rejecting publication");
        }
        checks++;

        require(!Checksums.strong(Map.of("md5", KNOWN.get("md5"))) && Checksums.strong(Map.of("sha512", KNOWN.get("sha512"))),
                "MD5 does not relax the strong identity guard while SHA512 is a strong reference");
        checks++;

        Checksum.Discovery discovery = new Checksum.Discovery(KNOWN.get("sha256"), "owned legacy source");
        require(discovery.sha256().equals(KNOWN.get("sha256")) && discovery.source().equals("owned legacy source")
                && discovery.checksums().equals(Map.of("sha256", KNOWN.get("sha256")))
                && discovery.md5().isEmpty() && discovery.sha512().isEmpty(), "Legacy two-argument discovery remains compatible");
        Map<String,Object> legacy = new LinkedHashMap<>();
        legacy.put("expected_sha256", KNOWN.get("sha256").toUpperCase(Locale.ROOT));
        legacy.put("sha256", KNOWN.get("sha256")); legacy.put("checksum_source", "owned legacy source");
        Checksums.normalizeState(legacy);
        require(Checksums.expected(legacy).equals(Map.of("sha256", KNOWN.get("sha256")))
                && Checksums.values(legacy).equals(Map.of("sha256", KNOWN.get("sha256")))
                && Checksums.sources(legacy).equals(Map.of("sha256", "owned legacy source"))
                && legacy.get("expected_md5").equals("") && legacy.get("expected_sha512").equals(""),
                "Old SHA256-only state keeps its source and does not fabricate publisher MD5 or SHA512");
        checks++;
        System.out.println("OWNED_NATIVE_CHECKSUM_FIXTURE_OK checks=" + checks);
    }
}
''', encoding="utf-8")
    run([jdk / "bin/javac", "--release", "17", "-cp", classpath, "-d", owned, fixture])
    result = run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "checksum-home"),
                  "-Djava.io.tmpdir=" + str(owned), "-cp", str(owned) + os.pathsep + classpath,
                  "multiroute.NativeChecksumFixture", owned / "owned-checksum-state"], capture_output=True, text=True)
    counted = re.search(r"^OWNED_NATIVE_CHECKSUM_FIXTURE_OK checks=(\d+)$", result.stdout, re.MULTILINE)
    assert counted is not None, "The packaged checksum fixture must finish all offline gates."
    check_count = int(counted.group(1))
    assert check_count == 5, "All five packaged checksum gates must pass."
    return check_count


def verify_download_progress_fixture(java: Path, jdk: Path, classpath: str, owned: Path) -> int:
    """Check the actual packaged row DTO/renderer on the EDT without windows or downloads."""
    fixture = owned / "NativeDownloadProgressFixture.java"
    fixture.write_text(r'''
package multiroute;
import java.awt.Component;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
public final class NativeDownloadProgressFixture {
    static int groups;
    static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    static JProgressBar bar(Component renderer) {
        require(renderer instanceof JPanel, "A reusable panel wraps the progress bar");
        JPanel panel = (JPanel) renderer;
        require(panel.getComponentCount() == 1 && panel.getComponent(0) instanceof JProgressBar,
                "Exactly one progress bar is reused for every row");
        return (JProgressBar) panel.getComponent(0);
    }
    static void exercise() {
        Path providedHome = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
                .resolve("owned fallback profile");
        for (String name : List.of("owned Windows profile A", "owned Windows profile B")) {
            Path profile = providedHome.resolveSibling(name);
            require(DesktopUI.defaultDownloadDirectory("Windows 11", providedHome.toString(),
                    Map.of("USERPROFILE", " " + profile + " ")).equals(profile.resolve("Downloads")),
                    "Each provided Windows profile gets its own Downloads without reading user files");
        }
        for (String invalid : List.of("", "   ", "relative-profile"))
            require(DesktopUI.defaultDownloadDirectory("Windows 11", providedHome.toString(),
                    Map.of("USERPROFILE", invalid)).equals(providedHome.resolve("Downloads")),
                    "Blank or relative Windows profiles safely fall back to the provided home");
        for (String operatingSystem : List.of("Linux", "Mac OS X", "Darwin"))
            require(DesktopUI.defaultDownloadDirectory(operatingSystem, providedHome.toString(),
                    Map.of("USERPROFILE", providedHome.resolveSibling("ignored Windows profile").toString()))
                    .equals(providedHome.resolve("Downloads")),
                    "Linux and macOS use the provided home and ignore Windows environment input");
        DownloadProgress known = DownloadProgress.from(Map.of("size", 200L, "downloaded", 175L,
                "progress_bytes", 25L, "status", "running"));
        require(known.received() == 25 && known.perMille() == 125 && known.determinate(),
                "Each row uses its own progress bytes instead of received retry traffic");
        DownloadProgress fallback = DownloadProgress.from(Map.of("size", 200L, "downloaded", 100L, "status", "paused"));
        require(fallback.perMille() == 500, "Older rows fall back to downloaded bytes");
        DownloadProgress unknown = new DownloadProgress(-1, 64, "running", "");
        require(!unknown.determinate() && unknown.perMille() == 0 && !unknown.text().contains("%"),
                "Unknown totals show received bytes without an invented percentage");
        DownloadProgress zero = new DownloadProgress(0, 0, "queued", "");
        require(!zero.determinate() && zero.text().equals("—"), "A pending zero-sized row has no percentage");
        DownloadProgress emptyComplete = new DownloadProgress(0, 0, "complete", "");
        DownloadProgress emptySeeding = new DownloadProgress(0, 0, "seeding", "");
        require(emptyComplete.determinate() && emptyComplete.perMille() == 1000 && emptySeeding.perMille() == 1000,
                "Completed zero-byte and seeding rows have completed progress");
        DownloadProgress failed = new DownloadProgress(100, 40, "failed", "owned transfer issue");
        require(failed.perMille() == 400 && failed.tooltip().contains("owned transfer issue"),
                "A failed row retains its own partial percentage and error");
        require(new DownloadProgress(100, -5, null, null).perMille() == 0
                && new DownloadProgress(100, 150, "running", "").perMille() == 1000,
                "Negative bytes and bytes beyond the total remain bounded");
        DownloadProgress huge = new DownloadProgress(Long.MAX_VALUE, Long.MAX_VALUE - 1, "running", "");
        require(huge.perMille() == 999 && new DownloadProgress(Long.MAX_VALUE, Long.MAX_VALUE, "complete", "").perMille() == 1000,
                "Long-sized values never overflow or show an incomplete file as 100 percent");
        groups++;

        DefaultTableModel mixed = new DefaultTableModel(new Object[][]{
            {"owned-half", fallback}, {"owned-unknown", unknown}, {"owned-failed", failed}
        }, new Object[]{"File", "Progress"});
        JTable mixedTable = new JTable(mixed);
        mixedTable.setRowSelectionInterval(0, 0);
        DownloadProgress.Renderer renderer = new DownloadProgress.Renderer();
        Component first = renderer.getTableCellRendererComponent(mixedTable, fallback, false, false, 0, 1);
        JProgressBar reused = bar(first);
        require(reused.getValue() == 500 && reused.getString().equals(fallback.text()), "Known progress renders its own row");
        Component next = renderer.getTableCellRendererComponent(mixedTable, unknown, true, false, 1, 1);
        require(next == first && bar(next) == reused && reused.getValue() == 0 && !reused.isIndeterminate()
                && reused.getString().equals(unknown.text()) && !reused.getString().contains("%"),
                "Rendering an unknown row resets the reused bar instead of retaining another row's percentage");
        require(next.getBackground().equals(mixedTable.getSelectionBackground()), "Selection background belongs to the rendered row");
        mixedTable.moveColumn(0, 1);
        next = renderer.getTableCellRendererComponent(mixedTable, failed, false, false, 2, 0);
        require(next == first && bar(next) == reused && reused.getValue() == 400
                && reused.getToolTipText().contains("owned transfer issue")
                && reused.getAccessibleContext().getAccessibleName().startsWith("owned-failed"),
                "A failed row replaces tooltip and accessible filename correctly after column reordering");
        groups++;

        DefaultTableModel sortedModel = new DefaultTableModel(new Object[][]{
            {"A", new DownloadProgress(100, 75, "running", "")},
            {"B", new DownloadProgress(0, 128, "running", "")},
            {"C", new DownloadProgress(100, 25, "failed", "owned sorted issue")},
            {"D", new DownloadProgress(-1, 64, "retrying", "")},
            {"E", emptyComplete}
        }, new Object[]{"File", "Progress"}) {
            public Class<?> getColumnClass(int column) { return column == 1 ? DownloadProgress.class : String.class; }
        };
        JTable sorted = new JTable(sortedModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(sortedModel);
        sorted.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));
        require(sorted.convertRowIndexToModel(0) == 3 && sorted.convertRowIndexToModel(1) == 1
                && sorted.convertRowIndexToModel(2) == 2 && sorted.convertRowIndexToModel(3) == 0
                && sorted.convertRowIndexToModel(4) == 4, "JTable sorts actual progress DTOs by known state and numeric transfer progress");
        sorter.setRowFilter(RowFilter.regexFilter("^(A|C|E)$", 0));
        require(sorted.getRowCount() == 3 && sorted.convertRowIndexToModel(0) == 2
                && sorted.convertRowIndexToModel(1) == 0 && sorted.convertRowIndexToModel(2) == 4,
                "Filtering preserves the sorted visible-to-model row mapping");
        sorted.setRowSelectionInterval(1, 1);
        DownloadProgress visible = (DownloadProgress) sorted.getValueAt(0, 1);
        Component rendered = renderer.getTableCellRendererComponent(sorted, visible, false, false, 0, 1);
        require(rendered == first && bar(rendered) == reused && reused.getValue() == 250
                && reused.getToolTipText().contains("owned sorted issue")
                && reused.getAccessibleContext().getAccessibleName().startsWith("C"),
                "A sorted and filtered row renders itself even when a different row is selected");
        groups++;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(NativeDownloadProgressFixture::exercise);
        System.out.println("OWNED_NATIVE_PROGRESS_FIXTURE_OK groups=" + groups);
    }
}
''', encoding="utf-8")
    run([jdk / "bin/javac", "--release", "17", "-cp", classpath, "-d", owned, fixture])
    result = run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "progress-home"),
                  "-Djava.io.tmpdir=" + str(owned), "-cp", str(owned) + os.pathsep + classpath,
                  "multiroute.NativeDownloadProgressFixture"], capture_output=True, text=True)
    counted = re.search(r"^OWNED_NATIVE_PROGRESS_FIXTURE_OK groups=(\d+)$", result.stdout, re.MULTILINE)
    assert counted is not None, "The packaged row progress fixture must finish all headless groups."
    progress_group_count = int(counted.group(1))
    assert progress_group_count == 3, "All three packaged row progress groups must pass."
    return progress_group_count


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
    native_input_count = verify_native_source_pin(report)
    package = directory / report["package"]
    assert hashlib.sha256(package.read_bytes()).hexdigest() == report["sha256"], "Installer bytes differ from the build report."
    app = directory / "MultiRoute Downloader.app"
    contents = app / "Contents"
    with (contents / "Info.plist").open("rb") as stream:
        metadata = plistlib.load(stream)
    assert metadata["CFBundleIdentifier"] == "local.multiroute.resume"
    assert metadata["LSMinimumSystemVersion"] == report["minimum_macos"]
    assert set(metadata["CFBundleLocalizations"]) == {"en", "ru", "de", "es", "fr", "zh-Hans", "ko", "ar", "it", "ja", "hi", "vi"}
    assert metadata["MultiRouteProjectOwnerURL"] == report["project_owner_url"]
    assert report["setup_language"] == "en"
    assert report["desktop_shortcut"]["target"] == "/Applications/MultiRoute Downloader.app"
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
    assert hashlib.sha256((app_dir / "MultiRoute-Downloader.jar").read_bytes()).hexdigest() == report["application_jar_sha256"]
    license_bytes = (app_dir / "LICENSE.txt").read_bytes()
    assert hashlib.sha256(license_bytes).hexdigest() == report["license_sha256"]
    assert ("Project owner: " + report["project_owner_url"]).encode("utf-8") in license_bytes
    assert (app_dir / "THIRD-PARTY-NOTICES.txt").is_file()
    verify_documentation(app_dir, report)
    browser_extension_count = verify_browser_extension(app_dir, report)
    app_markdown_links = verify_relative_markdown_links(app_dir)
    jars = [app_dir / "MultiRoute-Downloader.jar", *sorted((app_dir / "lib").glob("*.jar"))]
    classpath = os.pathsep.join(str(path) for path in jars)
    jdk = Path(os.environ["JAVA_HOME"])
    checks = ["native Finder launcher", "bundle metadata and twelve language prompts", "bundled Java modules"]
    checks.append("complete fixed-ID twelve-language Manifest V3 browser extension payload matches saved source hashes in the app image and expanded package, with only explicit link delivery permissions and loopback host access")
    with tempfile.TemporaryDirectory(prefix="multiroute-macos-verify-") as temporary:
        # macOS /var is a system symlink to /private/var. Resolve only our newly
        # created fixture root; retain production checks against symlink paths.
        owned = Path(temporary).resolve(strict=True)
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
        appearance = app_dir / "lib/flatlaf-3.7.2.jar"
        pinned = json.loads((ROOT / "scripts/dependencies.sha256.json").read_text(encoding="utf-8"))
        assert hashlib.sha256(appearance.read_bytes()).hexdigest() == pinned[appearance.name]
        assert (app_dir / "lib/licenses/FLATLAF-3.7.2-LICENSE").is_file()
        checks.append("pinned FlatLaf desktop appearance dependency and its full Apache license are bundled")
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
        Path link = desktop.resolve("MultiRoute Downloader.app");
        DesktopLauncher.createDesktopShortcutIfAbsent(desktop, app);
        check(Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(app.toAbsolutePath()), "Native Mac shortcut creation");
        DesktopLauncher.createDesktopShortcutIfAbsent(desktop, app);
        check(Files.readSymbolicLink(link).equals(app.toAbsolutePath()), "Retry preserves the existing shortcut");
        Path regularDesktop = Files.createDirectory(root.resolve("existing regular Desktop"));
        Path regular = regularDesktop.resolve("MultiRoute Downloader.app");
        Files.writeString(regular, "keep this owned item");
        DesktopLauncher.createDesktopShortcutIfAbsent(regularDesktop, app);
        check(Files.readString(regular).equals("keep this owned item"), "Existing file retained");
        Path linkedDesktop = Files.createDirectory(root.resolve("existing broken shortcut"));
        Path broken = linkedDesktop.resolve("MultiRoute Downloader.app");
        Path absent = root.resolve("not-present.app");
        Files.createSymbolicLink(broken, absent);
        DesktopLauncher.createDesktopShortcutIfAbsent(linkedDesktop, app);
        check(Files.readSymbolicLink(broken).equals(absent), "Existing broken link retained");
        Path other = Files.createDirectory(root.resolve("other owned folder"));
        Path redirected = root.resolve("redirected Desktop");
        Files.createSymbolicLink(redirected, other);
        DesktopLauncher.createDesktopShortcutIfAbsent(redirected, app);
        check(!Files.exists(other.resolve("MultiRoute Downloader.app")), "Redirected Desktop rejected");
        Path noAppDesktop = Files.createDirectory(root.resolve("missing app Desktop"));
        DesktopLauncher.createDesktopShortcutIfAbsent(noAppDesktop, absent);
        check(!Files.exists(noAppDesktop.resolve("MultiRoute Downloader.app")), "Missing application rejected");
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
        checksum_fixture_checks = verify_checksum_fixture(java, jdk, classpath, owned)
        checks.append("bundled Java computes fixed abc MD5, SHA-256 and SHA-512 vectors through the whole-file helper without any network")
        checks.append("manual MD5 and SHA-512 references normalize uppercase text and reject invalid lengths, hexadecimal text and numeric input")
        checks.append("real owned Job.finish rejects wrong MD5 or SHA-512, preserves each partial file, retains all three actual hashes and never publishes a final file")
        checks.append("MD5 alone cannot satisfy strong source identity while a SHA-512 reference can")
        checks.append("legacy SHA-256-only state and two-argument discovery retain their source without fabricating MD5 or SHA-512 references")
        download_progress_fixture_checks = verify_download_progress_fixture(java, jdk, classpath, owned)

        log_fixture = ROOT / "packaging/tests/LogScrollTest36.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, log_fixture])
        run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "log-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.LogScrollTest36", owned / "log-scroll", "headless"], capture_output=True, text=True)
        log_scroll = json.loads((owned / "log-scroll/LOG-SCROLL-TESTS.json").read_text(encoding="utf-8"))
        assert log_scroll["status"] == "PASS" and log_scroll["checks"] == 25 and log_scroll["headless_only"]
        assert log_scroll["version"] == report["version"] and not log_scroll["user_logs_read"]
        checks.append("twenty-five packaged log viewport checks preserve tail, browsing position, selection, later scrolling, bounded history and five-language rendering using owned text without reading user logs")

        details_fixture = ROOT / "packaging/tests/DetailsScrollTest37.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, details_fixture])
        run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "details-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.DetailsScrollTest37", owned / "details-scroll", "headless"], capture_output=True, text=True)
        details_scroll = json.loads((owned / "details-scroll/DETAILS-SCROLL-TESTS.json").read_text(encoding="utf-8"))
        assert details_scroll["status"] == "PASS" and details_scroll["checks"] == 10 and details_scroll["headless_only"]
        assert details_scroll["version"] == report["version"] and not details_scroll["user_profile_or_logs_read"]
        assert details_scroll["network_calls"] == 0
        checks.append("ten packaged download-details viewport checks preserve the report tail, selection, reading position and manual scrolling while resetting a newly selected report, using only owned text")

        documentation_fixture = ROOT / "packaging/tests/DocumentationLinksTest38.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, documentation_fixture])
        run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "documentation-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.DocumentationLinksTest38",
             owned / "documentation-links", "headless", ROOT], capture_output=True, text=True)
        documentation_links = json.loads((owned / "documentation-links/DOCUMENTATION-LINKS-TESTS.json").read_text(encoding="utf-8"))
        assert documentation_links["status"] == "PASS" and documentation_links["checks"] == 211 and documentation_links["headless_only"]
        assert documentation_links["version"] == report["version"] and documentation_links["local_guide_anchors_checked"]
        assert documentation_links["network_calls"] == documentation_links["browser_launches"] == 0
        checks.append("two hundred eleven packaged documentation checks validate five languages, pinned HTTPS guide links, API anchors, English fallback and bundled guide sections without launching a browser")

        startup_fixture = ROOT / "packaging/tests/StartupCommandTest39.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, startup_fixture])
        run([java, "-Djava.awt.headless=true", "-Duser.home=" + str(owned / "startup-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.StartupCommandTest39",
             owned / "startup-commands", "headless"], capture_output=True, text=True)
        startup_commands = json.loads((owned / "startup-commands/STARTUP-COMMAND-TESTS.json").read_text(encoding="utf-8"))
        assert startup_commands["status"] == "PASS" and startup_commands["checks"] == 32
        assert startup_commands["version"] == report["version"] and startup_commands["headless_only"]
        assert startup_commands["helper_cleanup"] and startup_commands["collector_threads_alive"] == 0
        assert startup_commands["network_calls"] == 0 and not startup_commands["user_profile_or_logs_read"]
        checks.append("thirty-two packaged startup command checks validate bounded pipe output, byte preservation, delayed final output, timeout, interruption, helper cleanup and independence from temporary output files")

        history_fixture = ROOT / "packaging/tests/HistoryLifecycleTest40.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, history_fixture])
        run([java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Duser.home=" + str(owned / "history-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.HistoryLifecycleTest40", owned / "download-history"], capture_output=True, text=True)
        download_history = json.loads((owned / "download-history/HISTORY-LIFECYCLE-TESTS.json").read_text(encoding="utf-8"))
        assert download_history["status"] == "PASS" and download_history["checks"] == 67
        assert download_history["version"] == report["version"]
        assert download_history["network_calls"] == 0
        checks.append("sixty-seven packaged lifecycle checks verify assembled Unicode files, real start and resume transitions, verified completion, checksum failure, restart and removal retention, bounded history viewing, explicit UTF-8 console bytes and independent durable logging")

        modern_fixture = ROOT / "packaging/tests/ModernDesktopTest40.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, modern_fixture])
        run([java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Duser.home=" + str(owned / "modern-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.ModernDesktopTest40", owned / "modern-desktop", "headless"], capture_output=True, text=True)
        modern_desktop = json.loads((owned / "modern-desktop/MODERN-DESKTOP-TESTS.json").read_text(encoding="utf-8"))
        assert modern_desktop["status"] == "PASS" and modern_desktop["checks"] == 17
        assert modern_desktop["version"] == report["version"] and modern_desktop["mode"] == "headless"
        assert modern_desktop["network_calls"] == modern_desktop["clipboard_actions_executed"] == 0
        checks.append("seventeen packaged appearance checks load FlatLaf light and dark themes, validate contrast, Unicode full paths, escaped and wrapped long tooltips and local dated timeline formatting without claiming graphical Mac interaction")

        locales_fixture = ROOT / "packaging/tests/LocalesTest40.java"
        run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, locales_fixture])
        run([java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Duser.home=" + str(owned / "locales-home"),
             "-cp", classpath + os.pathsep + str(owned), "multiroute.LocalesTest40", owned / "locales"], capture_output=True, text=True)
        locales = json.loads((owned / "locales/LOCALES-TESTS.json").read_text(encoding="utf-8"))
        assert locales["status"] == "PASS" and locales["checks"] >= 39523
        assert locales["version"] == report["version"] and locales["catalog_entries"] >= 1407
        assert set(locales["languages"]) == set(LANGUAGES) and locales["arabic_rtl_and_ltr_data_fields"]
        assert locales["network_calls"] == 0 and not locales["user_data_read"]
        checks.append("twelve-language catalog checks cover the complete current message catalog including Chinese, Korean, Arabic, Italian, Japanese, Hindi and Vietnamese, exact formatting placeholders, Unicode history output, real Swing Arabic direction and left-to-right URL fields")

        network42 = verify_network42_fixtures(java, jdk, classpath, owned, report["version"])
        checks.append("packaged JAR network diagnostics, durable error journal and authoritative topology bridge pass in fresh owned profiles with no physical gateways or OS network changes")
        checks.append("owned non-headless Swing topology label, translated tooltips and status changes pass using injected snapshots and no real gateway requests")

        for fixture_name in ("SiteProfilesTest40", "ArchivePreviewTest40", "TransfersApiTest40", "ProfileRoutesTest40", "ScheduleQueueTest40", "BrowserBridgeTest40"):
            fixture_source = ROOT / "packaging/tests" / (fixture_name + ".java")
            run([jdk / "bin/javac", "--release", "17", "-encoding", "UTF-8", "-cp", classpath, "-d", owned, fixture_source])
            run([java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Duser.home=" + str(owned / (fixture_name + "-home")),
                 "-Djava.io.tmpdir=" + str(owned), "-cp", classpath + os.pathsep + str(owned),
                 "multiroute." + fixture_name, owned / fixture_name], capture_output=True, text=True)
        site_profiles = json.loads((owned / "SiteProfilesTest40/SITE-PROFILES-TESTS.json").read_text(encoding="utf-8"))
        assert site_profiles["status"] == "PASS" and site_profiles["checks"] == 94
        assert site_profiles["network_calls"] == 0 and not site_profiles["user_data_read"] and not site_profiles["remote_files_modified"]
        assert site_profiles["same_origin_accounts_isolated"] and site_profiles["encrypted_atomic_vault_and_nonce_rotation"]
        checks.append("ninety-four packaged site profile checks verify independent same-origin accounts, explicit profile route binding, AES-GCM nonce rotation and tamper rejection, atomic owner-only storage, Unicode credentials and secret-free public metadata")
        archive_preview = json.loads((owned / "ArchivePreviewTest40/ARCHIVE-PREVIEW-TESTS.json").read_text(encoding="utf-8"))
        assert archive_preview["status"] == "PASS" and archive_preview["checks"] >= 68
        assert archive_preview["real_proxy_requests"] > 0 and not archive_preview["external_network"] and not archive_preview["user_files_read"]
        checks.append("packaged generated archive checks list ZIP, TAR, 7Z, RAR4 and RAR5 through an owned loopback proxy, preserve Unicode, verify stable range identity, metadata and cancellation limits and reject unsupported encrypted RAR headers without extraction")
        transfers_api = json.loads((owned / "TransfersApiTest40/TRANSFERS-API-TESTS.json").read_text(encoding="utf-8"))
        assert transfers_api["status"] == "PASS" and transfers_api["checks"] >= 81
        assert transfers_api["actual_controller_handler"] and not transfers_api["external_network"] and not transfers_api["user_profile_read"]
        checks.append("packaged transfer API checks exercise actual HTTP handlers for profile isolation and normalization, upload creation and pause/resume/removal, origin rejection, Unicode JSON and compatible reuse of saved legacy profile data")
        profile_routes = json.loads((owned / "ProfileRoutesTest40/PROFILE-ROUTES-TESTS.json").read_text(encoding="utf-8"))
        assert profile_routes["status"] == "PASS" and profile_routes["checks"] == 53
        assert profile_routes["network_calls"] == 0 and not profile_routes["user_data_read"] and not profile_routes["os_routes_changed"]
        checks.append("fifty-three packaged profile route checks verify canonical saved profile identity, no credential fallback across accounts, preservation through source preparation and runtime gateway changes, rejection of conflicting route profiles and compatibility of prior saved jobs")
        schedule_queue = json.loads((owned / "ScheduleQueueTest40/SCHEDULE-QUEUE-TESTS.json").read_text(encoding="utf-8"))
        assert schedule_queue["status"] == "PASS" and schedule_queue["checks"] == 61
        assert schedule_queue["network_calls"] == 0 and not schedule_queue["user_data_read"]
        assert schedule_queue["actual_verified_files"] == 6 and schedule_queue["owned_range_opens"] > 0
        checks.append("sixty-one packaged schedule and priority checks exercise actual queue ordering, daily and overnight windows, one-time delay, cancellation and continuation, manual pauses, persisted Unicode settings and six checksum-verified owned files without networking")
        browser_bridge = json.loads((owned / "BrowserBridgeTest40/BROWSER-BRIDGE-TESTS.json").read_text(encoding="utf-8"))
        assert browser_bridge["status"] == "PASS" and browser_bridge["checks"] == 107
        assert browser_bridge["external_network_calls"] == browser_bridge["source_network_calls"] == browser_bridge["download_jobs_created"] == 0
        assert browser_bridge["actual_loopback_http"] and not browser_bridge["user_data_read"]
        checks.append("one-hundred-seven packaged browser bridge checks use actual loopback HTTP handlers to verify default-disabled and persisted opt-in, exact fixed extension origin, scoped preflight, unchanged ordinary API guards, strict URL-only bodies and Unicode desktop callback delivery")

        checks.append("actual packaged row progress DTO handles known, unknown, zero, completed, failed and Long-sized values without an invented percentage or overflow; provided current-user profiles resolve Downloads safely on Windows, Linux and macOS")
        checks.append("actual packaged renderer reuses one progress bar, resets mixed row state and preserves row-specific accessibility after column reordering without opening a window")
        checks.append("JTable sorts and filters actual progress DTOs numerically and renders the correct visible row independently of the selected download")
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
        run(["/usr/sbin/pkgutil", "--expand-full", package, expanded])
        postinstall_scripts = list(expanded.rglob("postinstall"))
        assert len(postinstall_scripts) == 1
        run(["/bin/bash", "-n", postinstall_scripts[0]])
        shortcut_checks = verify_desktop_shortcut(postinstall_scripts[0].read_text(encoding="utf-8"), owned)
        checks.append("eight owned normal-user Desktop shortcut fixtures pass; installer switches to a validated console user and preserves existing items")
        installed_apps = list(expanded.rglob("MultiRoute Downloader.app"))
        assert len(installed_apps) == 1, "The installer must contain exactly one application bundle."
        installed_app_dir = installed_apps[0] / "Contents" / "app"
        assert hashlib.sha256((installed_app_dir / "MultiRoute-Downloader.jar").read_bytes()).hexdigest() == report["application_jar_sha256"]
        verify_documentation(installed_app_dir, report)
        assert verify_browser_extension(installed_app_dir, report) == browser_extension_count
        installed_markdown_links = verify_relative_markdown_links(installed_app_dir)
        assert installed_markdown_links == app_markdown_links
        assert (installed_app_dir / "LICENSE.txt").read_bytes() == license_bytes
        assert (installed_app_dir / "THIRD-PARTY-NOTICES.txt").read_bytes() == (ROOT / "THIRD-PARTY-NOTICES.txt").read_bytes()
        checks.append("nine local language guides, both READMEs, compatibility information and historical plus modern theme screenshots match saved sources in the app and expanded installer")
        checks.append("all relative Markdown links resolve within the app image and expanded installer, including baseline reports and native build instructions")
        candidates = [path for path in expanded.rglob("gateway-net") if "/Library/PrivilegedHelperTools/" in path.as_posix()]
        assert len(candidates) == 1
        helper = candidates[0]
        marker = helper.with_name("gateway-net.sha256")
        assert marker.read_text(encoding="ascii").strip() == report["helper_sha256"]
        assert hashlib.sha256(helper.read_bytes()).hexdigest() == report["helper_sha256"], "Expanded helper bytes differ from the build report."
        selftest = run([helper, "--self-test"], capture_output=True, text=True, timeout=30)
        assert "SELF_TEST_OK no network handles, no routing changes" in selftest.stdout
        initial = next(line for line in selftest.stdout.splitlines() if line.startswith("INITIAL_PROFILE_READY "))
        assert json.loads(initial.removeprefix("INITIAL_PROFILE_READY "))["configured_gateways"] == ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
        assert "ordered profiles, rejected topology leaves old profiles" in selftest.stdout
        assert architecture in run(["/usr/bin/lipo", "-archs", helper], capture_output=True, text=True).stdout.split()
        launcher = helper.with_name("gateway-launch")
        assert launcher.with_name("gateway-launch.sha256").read_text(encoding="ascii").strip() == report["gateway_launcher_sha256"]
        assert hashlib.sha256(launcher.read_bytes()).hexdigest() == report["gateway_launcher_sha256"], "Expanded launcher bytes differ from the build report."
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
        assert distribution.find("title").text == "MultiRoute Downloader"
        license_element = distribution.find("license")
        assert license_element is not None and license_element.get("file") == "LICENSE.txt" and license_element.get("mime-type") == "text/plain"
        installer_license = expanded / "Resources" / license_element.get("file")
        assert hashlib.sha256(installer_license.read_bytes()).hexdigest() == report["license_sha256"]
        checks.append("Installer license resource and installed app license exactly match the saved license and owner profile")
        assert distribution.find("options").get("hostArchitectures") == architecture
        assert distribution.find("volume-check/allowed-os-versions/os-version").get("min") == report["minimum_macos"]
        checks.append("expanded installer contains the matching helper/checksum and passes native offline self-test")
        checks.append("native C/header input set is pinned and both pure native fixtures passed on this Mac CPU without capture")
    result = {
        "ok": True, "architecture": architecture, "version": report["version"], "checks": checks,
        "not_verified": ["physical gateway throughput", "BPF authorization UI", "end-user Finder/Gatekeeper interaction", "real macOS menu-bar tray interaction", "graphical macOS downloads table layout, scrolling and viewport restoration"],
        "installed_packages": False, "administrator_prompt_executed": False, "routing_or_BPF_permissions_changed": False,
        "controlled_uid_drop_fixture": True,
        "offline_markdown_link_counts": {"app_image": app_markdown_links, "expanded_installer": installed_markdown_links},
        "setup_language": "en", "desktop_shortcut_owned_fixture_checks": shortcut_checks,
        "java_desktop_shortcut_owned_fixture_checks": 7, "headless_tray_fixture_checks": 16,
        "checksum34_fixture_checks": checksum_fixture_checks, "whole_file_algorithms": ["md5", "sha256", "sha512"],
        "download_progress35_fixture_checks": download_progress_fixture_checks,
        "log_scroll36_fixture_checks": log_scroll["checks"],
        "details_scroll37_fixture_checks": details_scroll["checks"],
        "documentation_links38_fixture_checks": documentation_links["checks"],
        "startup_command39_fixture_checks": startup_commands["checks"],
        "download_history40_fixture_checks": download_history["checks"],
        "modern_desktop40_fixture_checks": modern_desktop["checks"],
        "locales40_fixture_checks": locales["checks"],
        "site_profiles40_fixture_checks": site_profiles["checks"],
        "archive_preview40_fixture_checks": archive_preview["checks"],
        "transfers_api40_fixture_checks": transfers_api["checks"],
        "profile_routes40_fixture_checks": profile_routes["checks"],
        "schedule_queue40_fixture_checks": schedule_queue["checks"],
        "browser_extension_files_verified": browser_extension_count,
        "browser_bridge40_fixture_checks": browser_bridge["checks"],
        "network42_fixtures": network42, "native_source_files_verified": native_input_count,
        "native_pure_fixtures": report["native_pure_fixtures"],
        "graphical_macos_downloads_table_layout_verified": False,
        "real_user_desktop_modified": False,
    }
    (directory / "macos-verification-report.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
