package multiroute;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Owned errors and a synthetic child output pipe only. Never opens a capture device or user profile. */
public final class NetworkErrorJournalTest42 {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static String error() {
        Map<String,Object> fields = new LinkedHashMap<>();
        fields.put("gateway", "192.168.1.2"); fields.put("host", "cdn.example.test");
        fields.put("destination_ipv4", "203.0.113.42"); fields.put("remote_port", 443);
        fields.put("stage", "tcp_connect"); fields.put("reason", "Selected gateway TCP connection timed out");
        fields.put("elapsed_ms", 5012); fields.put("lwip_error", -3);
        return "TUNNEL_ERROR " + Json.stringify(fields);
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        if (args.length > 0 && args[0].equals("emit")) { System.err.println(error()); return; }
        Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        PrintStream original = System.out;
        List<String> cases = new ArrayList<>();
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            System.setOut(quiet);
            I18n.setLanguage("en"); AppLog.initialize(own.resolve("profile"));
            Path journal = own.resolve("profile/logs/network-errors.log");
            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                    "-Dfile.encoding=UTF-8", "-cp", System.getProperty("java.class.path"), NetworkErrorJournalTest42.class.getName(), "emit")
                    .redirectErrorStream(true).start();
            Thread pump = GatewayNetwork.drainOutput(child);
            check(child.waitFor(10, TimeUnit.SECONDS) && child.exitValue() == 0, "Owned child output did not finish");
            pump.join(3000); check(!pump.isAlive(), "Owned output pump did not drain"); AppLog.flush();
            String first = Files.readString(journal, StandardCharsets.UTF_8);
            check(first.contains("[ERROR]") && first.contains("192.168.1.2") && first.contains("cdn.example.test:443") &&
                    first.contains("203.0.113.42") && first.contains("5012") && first.contains("-3"), "Native failure fields or severity were lost");
            Instant.parse(first.substring(0, first.indexOf(' ')));
            check(AppLog.snapshotText().contains("Gateway tunnel failed"), "Native error is missing from UI snapshot");
            cases.add("native_output_pipe_to_timestamped_error_journal_and_UI");
            AppLog.nativeLine("CONNECTED gateway=192.168.1.1 source=192.168.1.70:40001 host=example.test:443");
            AppLog.flush(); check(first.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Normal connection became a durable error");
            AppLog.nativeLine("NETWORK_TOPOLOGY_ERROR Gateway component is not running"); AppLog.flush();
            check(Files.readString(journal, StandardCharsets.UTF_8).contains("Network topology check failed: Gateway component is not running"), "Native topology failure missing from durable error journal");
            AppLog.nativeLine("TUNNEL_ERROR {broken owned request /private?token=owned-secret}");
            AppLog.nativeLine(error().replace("cdn.example.test", "evil.test/secret?token=owned-secret"));
            AppLog.flush();
            String malformed = Files.readString(journal, StandardCharsets.UTF_8);
            check(!malformed.contains("owned-secret") && !AppLog.snapshotText().contains("owned-secret"), "Malformed diagnostic disclosed untrusted payload");
            check(malformed.lines().count() == 4, "Malformed native errors were lost or split across lines");
            cases.add("normal_lines_not_errors_and_malformed_native_payload_never_retained");
            java.lang.reflect.Field sessionField = AppLog.class.getDeclaredField("session"); sessionField.setAccessible(true);
            Object session = sessionField.get(null);
            synchronized (session) {
                // Hold the history consumer while producers fill the bounded queue through the real API.
                for (int i = 0; i < 1200; i++) AppLog.info("Owned full-queue normal event %d", i);
                AppLog.networkError("Owned priority network error %s", "Ω");
            }
            AppLog.flush();
            check(Files.readString(journal, StandardCharsets.UTF_8).contains("Owned priority network error Ω"), "Normal-event queue saturation dropped network error");
            cases.add("network_error_displaces_INFO_when_normal_queue_is_full");
            AppLog.networkError("Owned network failure %s", "Кириллица 日本語 العربية é"); AppLog.flush();
            String unicode = Files.readString(journal, StandardCharsets.UTF_8);
            check(unicode.contains("Кириллица 日本語 العربية é") && !unicode.contains("\ufffd"), "UTF-8 error data damaged");
            for (int i = 0; i < 2300; i++) { AppLog.info("Owned normal event %d", i); if (i % 32 == 0) AppLog.flush(); }
            AppLog.flush();
            check(!AppLog.snapshotText().contains("cdn.example.test"), "Fixture failed to evict UI event");
            check(unicode.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Error journal depended on UI history");
            AppLog.close(); AppLog.initialize(own.resolve("profile")); AppLog.flush();
            check(unicode.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Restoring events duplicated or removed error records");
            I18n.setLanguage("ru"); AppLog.flush();
            check(unicode.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Language change rewrote observation history");
            cases.add("Unicode_and_independent_retention_across_UI_eviction_restart_and_language_change");
            AppLog.close();
            Path rotation = own.resolve("rotation"); Files.createDirectories(rotation.resolve("logs"));
            Path rotating = rotation.resolve("logs/network-errors.log");
            AppLog.initialize(rotation); AppLog.flush();
            for (int i = 1; i <= 4; i++) {
                Files.writeString(rotating, "owned archive " + i + "\n" + "x".repeat(4 * 1024 * 1024), StandardCharsets.UTF_8);
                AppLog.nativeLine(error()); AppLog.flush();
            }
            check(Files.readString(rotating.resolveSibling("network-errors.log.1"), StandardCharsets.UTF_8).startsWith("owned archive 4"), "Newest archive not retained");
            check(Files.readString(rotating.resolveSibling("network-errors.log.3"), StandardCharsets.UTF_8).startsWith("owned archive 2"), "Archive rotation order incorrect");
            check(!Files.exists(rotating.resolveSibling("network-errors.log.4")), "Archive count unbounded");
            check(Files.size(rotating) < 8192, "Rotated current file unbounded");
            cases.add("bounded_4_MiB_rotation_with_three_archives");
        } finally { AppLog.close(); System.setOut(original); }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("status", "PASS"); result.put("checks", checks); result.put("cases", cases);
        result.put("user_logs_read", false); result.put("native_network_started", false); result.put("network_calls", 0);
        Files.writeString(own.resolve("NETWORK-ERROR-JOURNAL-TESTS.json"), Json.stringify(result), StandardCharsets.UTF_8);
        System.out.println(Json.stringify(result));
    }
}
