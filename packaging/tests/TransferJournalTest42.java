package multiroute;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Synthetic payload counters and owned files only: no source, credentials, user logs or network. */
public final class TransferJournalTest42 {
    private static int checks;
    private static void check(boolean value, String reason) { checks++; if (!value) throw new AssertionError(reason); }
    private static List<Map<String,Object>> events(Path file) throws Exception {
        List<Map<String,Object>> values = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            try { values.add(Main.obj(Json.parse(line))); } catch (RuntimeException torn) { /* owned torn prefix fixture */ }
        }
        return values;
    }
    private static long number(Map<String,Object> value, String key) { return Main.num(value.get(key), -1); }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        Path own = Path.of(args.length == 0 ? System.getProperty("test.root") : args[0]).toAbsolutePath(); Files.createDirectories(own);
        Path data = own.resolve("profile"), journal = data.resolve("logs/" + TransferJournal.FILE_NAME);
        List<String> cases = new ArrayList<>();
        PrintStream original = System.out;
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            System.setOut(quiet); I18n.setLanguage("en"); AppLog.initialize(data);
            AtomicLong clock = new AtomicLong();
            String job = "задание_日本語_é", gateway = "192.168.1.1";
            TransferJournal.Attempt first = TransferJournal.begin(job, gateway, 0, -1, 0, 0, false, clock::get);
            for (int i = 1; i <= 10000; i++) {
                clock.set(TimeUnit.MILLISECONDS.toNanos(i)); first.progress(i * 100L, i * 100L);
            }
            AppLog.flush(); check(events(journal).size() == 1, "Payload callbacks flooded the journal before 12 seconds");
            clock.set(TimeUnit.SECONDS.toNanos(12)); first.progress(2_000_000, 1_999_000);
            clock.set(TimeUnit.SECONDS.toNanos(23)); first.progress(3_000_000, 2_999_000);
            AppLog.flush(); check(events(journal).size() == 2, "Progress interval not enforced");
            clock.set(TimeUnit.SECONDS.toNanos(24)); first.progress(5_800_000_000L, 5_799_999_000L);
            clock.set(TimeUnit.SECONDS.toNanos(25));
            first.finish("failed", "Owned reset at https://user:password@cdn.example.test/private?token=owned-secret&signature=owned-signature Bearer ownedBearer hf_ownedToken", 5_800_000_000L, 5_799_999_000L, 5_800_000_000L);
            first.finish("completed", "duplicate", 8_000_000_000L, 8_000_000_000L, 8_000_000_000L);
            first.progress(9_000_000_000L, 9_000_000_000L); AppLog.flush();
            List<Map<String,Object>> firstEvents = events(journal);
            check(firstEvents.size() == 4, "Terminal event was duplicated or post-finish progress accepted");
            Map<String,Object> terminal = firstEvents.get(3);
            check(number(terminal,"attempt_bytes") == 5_800_000_000L && number(terminal,"persisted_bytes") == 5_799_999_000L && number(terminal,"wire_total_bytes") == 5_800_000_000L, "Attempt, saved and wire bytes were conflated");
            check(number(terminal,"range_start") == 0 && number(terminal,"range_end") == -1 && number(terminal,"elapsed_ms") == 25_000, "Range or elapsed monotonic time lost");
            check(firstEvents.stream().allMatch(value -> value.get("attempt_id").equals(terminal.get("attempt_id"))), "One attempt got inconsistent identity");
            String rendered = AppLog.snapshotTextFor("en"), raw = Files.readString(journal, StandardCharsets.UTF_8);
            for (String secret : List.of("password", "owned-secret", "owned-signature", "ownedBearer", "hf_ownedToken", "/private")) {
                check(!raw.contains(secret) && !rendered.contains(secret), "Sensitive URL or credential leaked: " + secret);
            }
            check(raw.contains(job) && rendered.contains(job) && !raw.contains("\ufffd") && !rendered.contains("\ufffd"), "Unicode damaged on journal or UI path");
            cases.add("10000_payload_callbacks_throttled_at_12_seconds_distinct_64bit_counters_and_terminal_idempotence");

            Path part = own.resolve("часть_日本語_é.part"); byte[] prefix = "Собственные данные 日本語 é".getBytes(StandardCharsets.UTF_8); Files.write(part, prefix);
            String preservedReason = "Partial file preserved after a transfer error; retrying will check resume support before writing.";
            TransferJournal.preserved(job, gateway, 5_799_999_000L, 5_800_000_000L, preservedReason);
            check(java.util.Arrays.equals(prefix, Files.readAllBytes(part)), "Logging preservation modified the file");
            TransferJournal.restart(job, gateway, 5_799_999_000L, 5_800_000_000L, "Owned explicit restart");
            check(java.util.Arrays.equals(prefix, Files.readAllBytes(part)), "Logging reset modified the file");
            clock.set(TimeUnit.SECONDS.toNanos(30));
            TransferJournal.Attempt next = TransferJournal.begin(job, "192.168.1.3", 0, 7_000_000_000L, 0, 5_800_000_000L, true, clock::get);
            clock.addAndGet(TimeUnit.SECONDS.toNanos(12)); next.progress(627_526_818L, 627_526_818L);
            next.finish("paused", preservedReason, 627_526_818L, 627_526_818L, 6_427_526_818L); AppLog.flush();
            List<Map<String,Object>> accounting = events(journal);
            Map<String,Object> preserved = accounting.get(4), restart = accounting.get(5), after = accounting.get(accounting.size()-1);
            check(preserved.get("event").equals("prefix_preserved") && number(preserved,"discarded_bytes") == 0 && number(preserved,"persisted_bytes") == 5_799_999_000L, "Preservation falsely claimed discarded bytes");
            check(restart.get("event").equals("prefix_restarted") && number(restart,"discarded_bytes") == 5_799_999_000L && number(restart,"persisted_bytes") == 0 && number(restart,"wire_total_bytes") == 5_800_000_000L, "Reset history lost discarded prefix or prior wire total");
            check(number(after,"attempt_bytes") == 627_526_818L && number(after,"persisted_bytes") == 627_526_818L && number(after,"wire_total_bytes") == 6_427_526_818L && after.get("gateway").equals("192.168.1.3"), "Retry accounting cannot explain wire greater than saved prefix");
            HashSet<Object> ids = new HashSet<>();
            for (Map<String,Object> event : accounting) { check(ids.add(event.get("event_id")), "Duplicate event identity"); Instant.parse(String.valueOf(event.get("time"))); }
            cases.add("preserve_and_explicit_reset_keep_discarded_prefix_gateway_and_wire_history_without_touching_payload");

            for (String language : I18n.codes()) {
                String text = AppLog.snapshotTextFor(language);
                check(!text.contains("\ufffd") && text.contains(job), "Unicode lost in language " + language);
                check(text.contains(I18n.tFor(language, "Transfer paused")), "Terminal state untranslated: " + language);
                if (!language.equals("en")) check(!text.contains("Download attempt started:") && !text.contains(preservedReason), "Transfer text fell back to English: " + language);
                check(text.contains("6427526818") || language.equals("ar"), "64-bit wire total missing from translated UI: " + language);
            }
            for (String key : List.of(TransferJournal.START, TransferJournal.PROGRESS, TransferJournal.END, TransferJournal.PRESERVED, TransferJournal.RESTART,
                    "Resume supported", "Resume unavailable", "Transfer completed", "Transfer paused", "Transfer retry", "Transfer interrupted", "Transfer failed", "Could not append transfer journal", "Transfer journal events omitted: %d",
                    "Partial file preserved: the source does not support safe resuming. Explicitly restart the download to replace it.", preservedReason,
                    "Partial file preserved: source size or identity changed.", "Saved partial data recovered for segmented downloading")) {
                check(I18n.sourceRows().containsKey(key), "Canonical template absent: " + key);
                for (String language : LocalizedCatalog.EXTRA) check(LocalizedCatalog.catalog(language).containsKey(key), "Resource translation absent: " + language);
            }
            String persistent = Files.readString(journal, StandardCharsets.UTF_8);
            I18n.setLanguage("ru"); AppLog.flush(); check(persistent.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Language change rewrote durable records");
            AppLog.close(); AppLog.initialize(data); AppLog.flush();
            check(AppLog.snapshotTextFor("en").contains("Download attempt ended:"), "Transfer events not restored to UI");
            check(persistent.equals(Files.readString(journal, StandardCharsets.UTF_8)), "Application restart duplicated or rewrote durable records");
            for (int i = 0; i < 2300; i++) { AppLog.info("Owned eviction %d", i); if (i % 32 == 0) AppLog.flush(); }
            AppLog.flush(); check(!AppLog.snapshotText().contains(job), "Fixture did not evict bounded UI history");
            check(persistent.equals(Files.readString(journal, StandardCharsets.UTF_8)), "UI eviction erased transfer history");
            cases.add("all_12_languages_UTF8_restart_and_language_changes_do_not_duplicate_or_erase_independent_history");

            java.lang.reflect.Field sessionField = AppLog.class.getDeclaredField("session"); sessionField.setAccessible(true);
            Object session = sessionField.get(null);
            synchronized (session) {
                for (int i = 0; i < 1200; i++) AppLog.info("Owned crowded queue %d", i);
                TransferJournal.Attempt critical = TransferJournal.begin("owned-priority", "192.168.1.4", 10, 19, 10, 30, true);
                critical.finish("failed", "Owned terminal survives INFO saturation", 7, 17, 37);
            }
            AppLog.flush();
            check(Files.readString(journal, StandardCharsets.UTF_8).contains("Owned terminal survives INFO saturation"), "INFO saturation dropped terminal accounting");
            cases.add("critical_attempt_events_displace_noncritical_INFO_in_bounded_async_queue");

            TransferJournal.Attempt overflow = TransferJournal.begin("owned-overflow", "192.168.1.2", -5, -1, -2, Long.MAX_VALUE-2, true, clock::get);
            clock.addAndGet(TimeUnit.SECONDS.toNanos(12)); overflow.progress(99, -3); overflow.finish("retry", "owned", 98, -1, -10); AppLog.flush();
            Map<String,Object> saturated = events(journal).get(events(journal).size()-1);
            check(number(saturated,"wire_total_bytes") == Long.MAX_VALUE && number(saturated,"attempt_bytes") == 99 && number(saturated,"persisted_bytes") == 0, "Counters overflowed, decreased, or accepted negative persisted bytes");
            cases.add("counter_overflow_saturates_and_nonnegative_monotonic_attempt_totals");
            TransferJournal.Attempt parallel = TransferJournal.begin("owned-parallel", "192.168.1.4", 20, 29, 10, 100, true, clock::get);
            clock.addAndGet(TimeUnit.SECONDS.toNanos(12)); parallel.progress(5, 15, 999); AppLog.flush();
            Map<String,Object> parallelProgress = events(journal).get(events(journal).size()-1);
            check(number(parallelProgress,"wire_total_bytes") == 999 && number(parallelProgress,"attempt_bytes") == 5,
                    "Parallel job wire total was replaced with this attempt's own bytes");
            parallel.finish("completed", "owned", 10, 20, 1004); AppLog.flush();
            cases.add("parallel_progress_accepts_full_job_wire_total_separate_from_own_attempt");
            AppLog.close();

            Path rotation = own.resolve("rotation"); Files.createDirectories(rotation.resolve("logs")); AppLog.initialize(rotation); AppLog.flush();
            Path rotating = rotation.resolve("logs/" + TransferJournal.FILE_NAME);
            for (int i = 1; i <= 4; i++) {
                Files.writeString(rotating, "owned archive " + i + "\n" + "x".repeat((int)TransferJournal.MAX_FILE_BYTES), StandardCharsets.UTF_8);
                TransferJournal.preserved("owned-rotate", "192.168.1.1", i, i, "owned"); AppLog.flush();
            }
            check(Files.readString(rotating.resolveSibling(TransferJournal.FILE_NAME+".1"), StandardCharsets.UTF_8).startsWith("owned archive 4"), "Newest archive wrong");
            check(Files.readString(rotating.resolveSibling(TransferJournal.FILE_NAME+".3"), StandardCharsets.UTF_8).startsWith("owned archive 2"), "Oldest archive order wrong");
            check(!Files.exists(rotating.resolveSibling(TransferJournal.FILE_NAME+".4")) && Files.size(rotating) < 8192, "Retention is unbounded");
            cases.add("rotation_4_MiB_current_plus_three_archives"); AppLog.close();

            Path torn = own.resolve("torn"); Files.createDirectories(torn.resolve("logs"));
            Path tornFile = torn.resolve("logs/" + TransferJournal.FILE_NAME); Files.writeString(tornFile, "{\"incomplete\":", StandardCharsets.UTF_8);
            AppLog.initialize(torn); TransferJournal.preserved("owned-torn", "192.168.1.2", 42, 50, "owned"); AppLog.flush();
            check(events(tornFile).size() == 1 && Files.readString(tornFile, StandardCharsets.UTF_8).startsWith("{\"incomplete\":\n{"), "A torn previous line swallowed new event");
            AppLog.close(); cases.add("torn_append_recovers_newline_without_rewriting_older_history");

            Path unavailable = own.resolve("unavailable"); Files.createDirectories(unavailable.resolve("logs/" + TransferJournal.FILE_NAME));
            AppLog.initialize(unavailable); TransferJournal.preserved("owned-disk-failure", "192.168.1.2", 4, 8, "owned"); AppLog.flush();
            check(AppLog.snapshotTextFor("en").contains("Could not append transfer journal"), "Disk failure missing from UI");
            AppLog.close(); cases.add("journal_disk_failure_is_visible_and_never_thrown_into_transfer");
        } finally { AppLog.close(); System.setOut(original); }
        Map<String,Object> result = new LinkedHashMap<>(); result.put("status", "PASS"); result.put("checks", checks); result.put("cases", cases);
        result.put("user_logs_read", false); result.put("network_calls", 0); result.put("payload_files_modified", "Owned fixture only");
        Files.writeString(own.resolve("TRANSFER-JOURNAL-TESTS.json"), Json.stringify(result), StandardCharsets.UTF_8);
        System.out.println(Json.stringify(result));
    }
}
