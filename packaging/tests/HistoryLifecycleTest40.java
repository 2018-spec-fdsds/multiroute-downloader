package multiroute;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLSession;

/** Owned deterministic range bodies, Unicode paths and isolated profiles; no network or user data. */
public final class HistoryLifecycleTest40 {
    private static int checks;
    private static final List<String> cases = new ArrayList<>();
    private static final byte[] DATA = new byte[3 * 65536 + 97];
    private static Path own;
    private static void check(boolean value, String name) {
        checks++; if (!value) throw new AssertionError(name); cases.add(name);
    }
    private static String hash(String algorithm) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(DATA)); }
    private static Map<String,Object> state(String id, String name, Path directory) throws Exception {
        Files.createDirectories(directory);
        Map<String,Object> state = new LinkedHashMap<>();
        state.put("id", id); state.put("filename", name); state.put("directory", directory.toString());
        state.put("url", "https://owned-history.invalid/file.bin?token=OWN-SECRET-DO-NOT-LOG");
        state.put("part_path", directory.resolve("." + name + "." + id + ".part").toString());
        state.put("size", 0L); state.put("downloaded", 0L); state.put("chunk_size", 65536L);
        state.put("chunks", new LinkedHashMap<>()); state.put("status", "queued"); state.put("error", "");
        state.put("created_at", Instant.now().toString()); state.put("etag", ""); state.put("modified", "");
        state.put("routes", List.of(new LinkedHashMap<>(Map.of("type", "direct", "enabled", true, "name", "Owned route"))));
        Checksums.initialize(state, Map.of("sha256", hash("SHA-256"))); DownloadHistory.initialize(state);
        return state;
    }
    private static final class Response implements HttpResponse<InputStream> {
        final HttpHeaders headers; final InputStream body; final URI uri;
        Response(String url, long start, long end, InputStream body, String publishedSha) throws Exception {
            this.body = body; this.uri = URI.create(url);
            String repr = "md5=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hash("MD5"))) + ":, " +
                    "sha-256=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(publishedSha)) + ":, " +
                    "sha-512=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hash("SHA-512"))) + ":";
            headers = HttpHeaders.of(Map.of("Content-Range", List.of("bytes " + start + "-" + end + "/" + DATA.length),
                    "Content-Length", List.of(Long.toString(end - start + 1)), "ETag", List.of("\"owned-history-v1\""),
                    "Content-Type", List.of("application/octet-stream"), "Repr-Digest", List.of(repr)), (a,b) -> true);
        }
        public int statusCode() { return 206; }
        public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return headers; }
        public InputStream body() { return body; }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public URI uri() { return uri; }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
    private static final class Opener implements Main.Job.HttpOpener {
        final CountDownLatch blocked = new CountDownLatch(1);
        final AtomicBoolean blockOnce = new AtomicBoolean(true);
        final boolean block;
        String publishedSha;
        int requests;
        Opener(boolean block) throws Exception { this.block = block; publishedSha = hash("SHA-256"); }
        public HttpResponse<InputStream> open(Map<String,Object> route, String url, long start, long end, String etag, String modified) throws Exception {
            requests++; InputStream body = new ByteArrayInputStream(Arrays.copyOfRange(DATA, (int)start, (int)end + 1));
            if (block && start == 65536 && blockOnce.getAndSet(false)) body = new InputStream() {
                volatile boolean closed;
                public int read() throws IOException { byte[] one = new byte[1]; return read(one, 0, 1) < 0 ? -1 : one[0] & 255; }
                public synchronized int read(byte[] b, int off, int len) throws IOException {
                    blocked.countDown(); while (!closed) try { wait(20); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Owned cancelled range", interrupted); }
                    throw new IOException("Owned closed range");
                }
                public synchronized void close() { closed = true; notifyAll(); }
            };
            return new Response(url, start, end, body, publishedSha);
        }
    }
    private static void stopped(Main.Job job) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (job.thread != null && System.nanoTime() < deadline) Thread.sleep(10);
        check(job.thread == null, "owned job worker stopped");
    }
    private static List<Map<String,Object>> events(Main.Job job) { return DownloadHistory.events(job.snapshot()); }
    private static List<String> kinds(Main.Job job) { return events(job).stream().map(e -> Main.str(e.get("kind"))).toList(); }
    private static List<Map<String,Object>> journal(Path profile) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        for (String line : Files.readAllLines(profile.resolve("logs/downloads-history.jsonl"), StandardCharsets.UTF_8)) {
            if (!line.isBlank()) result.add(Main.obj(Json.parse(line)));
        }
        return result;
    }
    private static void stableTimes(Main.Job job) {
        Instant previous = Instant.EPOCH;
        for (Map<String,Object> event : events(job)) {
            Instant time = Instant.parse(Main.str(event.get("time")));
            check(!time.isBefore(previous), "lifecycle dates follow real chronological transitions"); previous = time;
        }
    }
    private static void consoleEncoding() throws Exception {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        Path out = own.resolve("owned-console.stdout.bin"), err = own.resolve("owned-console.stderr.bin");
        List<String> command = List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dfile.encoding=windows-1252", "-Dstdout.encoding=windows-1252", "-Dstderr.encoding=windows-1252",
                "-Duser.home=" + own.resolve("console-home"), "-Djava.io.tmpdir=" + own.resolve("console-temp"),
                "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"),
                HistoryLifecycleTest40.class.getName(), "--owned-console");
        Files.createDirectories(own.resolve("console-home")); Files.createDirectories(own.resolve("console-temp"));
        ProcessBuilder builder = new ProcessBuilder(command).redirectOutput(out.toFile()).redirectError(err.toFile());
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS")) builder.environment().remove(variable);
        Process process = builder.start();
        try {
            boolean ended = process.waitFor(10, TimeUnit.SECONDS);
            byte[] expected = "Привет_日本語_é_🚀\n".getBytes(StandardCharsets.UTF_8);
            check(ended && process.exitValue() == 0 && Arrays.equals(Files.readAllBytes(out), expected) &&
                    Arrays.equals(Files.readAllBytes(err), expected),
                    "owned legacy-windows-1252 child produces exact UTF-8 stdout and stderr after idempotent console setup");
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(3, TimeUnit.SECONDS); }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--owned-console")) {
            Main.configureUtf8Console(); Main.configureUtf8Console();
            System.out.print("Привет_日本語_é_🚀\n"); System.err.print("Привет_日本語_é_🚀\n");
            System.out.flush(); System.err.flush(); return;
        }
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        own = Path.of(args[0]).toAbsolutePath().normalize(); Files.createDirectories(own);
        consoleEncoding();
        for (int i = 0; i < DATA.length; i++) DATA[i] = (byte)(i * 19 + i / 71);
        Path profile = own.resolve("profile"), directory = own.resolve("Загрузки-ä-📦");
        Files.createDirectories(profile); AppLog.initialize(profile); I18n.setLanguage("en");
        try {
            String name = "Архив-ä-📦.bin";
            Map<String,Object> first = state("aaaaaaaaaaaa", name, directory);
            Opener opener = new Opener(true); Main.Job job = new Main.Job(first, profile, opener, Main.HTTP_IDLE_TIMEOUT_NANOS);
            job.persist();
            check(events(job).isEmpty() && !job.state.containsKey("first_started_at"), "queued job has no fabricated start time");
            check(!Files.exists(profile.resolve("logs/downloads-history.jsonl")), "queued checkpoint does not invent download journal event");
            job.launch(); check(opener.blocked.await(5, TimeUnit.SECONDS), "real second HTTP range waits after first verified segment");
            job.pause(); stopped(job);
            check(kinds(job).equals(List.of("started", "paused")), "actual run and pause recorded exactly once");
            check(Main.num(job.state.get("downloaded"), 0) >= 65536 && !Main.obj(job.state.get("chunks")).isEmpty(), "first actual HTTP segment checkpoint exists before pause");
            job.pause(); check(kinds(job).equals(List.of("started", "paused")), "repeated pause and UI refresh do not add lifecycle entries");
            Object firstTime = job.state.get("first_started_at"); job.launch(); stopped(job);
            check("complete".equals(job.snapshot().get("status")), "real resumed segmented job reaches checksum-verified complete");
            check(kinds(job).equals(List.of("started", "paused", "resumed", "completed")), "start pause resume complete recorded in order");
            check(firstTime.equals(job.state.get("first_started_at")), "resume keeps original observed first start timestamp");
            check(job.state.containsKey("last_resumed_at") && job.state.containsKey("completed_at"), "resume and successful completion each expose exact date");
            check(Arrays.equals(Files.readAllBytes(directory.resolve(name)), DATA), "own Unicode output contains complete actual range bytes");
            check(hash("SHA-256").equals(job.snapshot().get("sha256")), "assembled output verified with independent SHA-256");
            stableTimes(job);
            AppLog.flush(); String canonical = Files.readString(profile.resolve("logs/downloads-history.jsonl"), StandardCharsets.UTF_8);
            check(canonical.contains(name) && canonical.contains("📦") && !canonical.contains("\ufffd"), "canonical journal round-trips Cyrillic emoji and German characters");
            check(!canonical.contains("OWN-SECRET") && !canonical.contains("owned-history.invalid"), "durable journal excludes source URLs query tokens and credentials");
            List<Map<String,Object>> persisted = journal(profile);
            check(persisted.size() == 4 && persisted.stream().map(e -> e.get("event_id")).distinct().count() == 4, "durable event IDs are unique and one entry per transition");
            check(job.snapshot().get("lifecycle_history") instanceof List<?> && !job.snapshot().containsKey("lifecycle_run_open") &&
                    !Json.stringify(job.snapshot().get("lifecycle_history")).contains("OWN-SECRET"), "API snapshot exposes safe lifecycle fields without internal flags or source credentials");
            for (Map<String,Object> event : persisted) {
                check(name.equals(event.get("filename")) && directory.resolve(name).toString().equals(event.get("path")), "each event stores exact full file name and absolute destination path");
                check("download-history".equals(event.get("category")) && event.get("source") instanceof String && event.get("args") instanceof List<?>, "JSONL retains canonical translatable template and arguments");
            }
            byte[] journalBefore = Files.readAllBytes(profile.resolve("logs/downloads-history.jsonl"));
            byte[] humanBefore = Files.readAllBytes(profile.resolve("logs/downloads-history.log"));
            for (String language : List.of("ru", "en", "de", "es", "fr")) {
                I18n.setLanguage(language); String view = DownloadHistory.snapshotText(profile, language);
                check(view.contains(name) && view.contains(directory.resolve(name).toString()) && !view.contains("\ufffd"), "history UI language " + language + " preserves full Unicode filename path and dates");
                check(view.matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} (?:Z|[+-]\\d{2}:\\d{2}).*"), "history UI " + language + " contains date time and zone offset");
            }
            AppLog.flush(); check(Arrays.equals(journalBefore, Files.readAllBytes(profile.resolve("logs/downloads-history.jsonl"))) && Arrays.equals(humanBefore, Files.readAllBytes(profile.resolve("logs/downloads-history.log"))), "language changes never rewrite durable canonical or human history");
            for (int i = 0; i < 2304; i++) { AppLog.info("Owned bounded log eviction %d", i); if (i % 32 == 0) AppLog.flush(); }
            AppLog.flush(); check(!AppLog.snapshotText().contains(name), "bounded application log can evict old download messages");
            check(DownloadHistory.snapshotText(profile, "en").contains(name), "durable history survives diagnostic log eviction");
            AppLog.close(); AppLog.initialize(profile);
            Map<String,Object> restored = Main.obj(Json.parse(Files.readString(job.manifest, StandardCharsets.UTF_8)));
            Main.Job reloaded = new Main.Job(restored, profile, new Opener(false), Main.HTTP_IDLE_TIMEOUT_NANOS); reloaded.persist(); reloaded.persist();
            check(events(reloaded).equals(events(job)), "job lifecycle survives restart with exact event IDs and times");
            check(journal(profile).size() == 4, "restarting and repeated checkpoints do not duplicate durable events");
            check(DownloadHistory.snapshotText(profile, "ru").contains(name), "restarted UI can display durable history after application log eviction");
            Main.Manager manager = new Main.Manager(profile, (u,r) -> Map.of(), p -> Files.getFileStore(p).getUsableSpace(), (r,t) -> {});
            manager.remove("aaaaaaaaaaaa", false); manager.shutdown();
            check(Files.exists(directory.resolve(name)) && journal(profile).size() == 4, "removing a completed job keeps its audit history and default saved file");

            Path failedProfile = own.resolve("bad-checksum"); Files.createDirectories(failedProfile);
            Map<String,Object> wrong = state("bbbbbbbbbbbb", "Ошибка-📦.bin", directory);
            wrong.put("expected_sha256", "0".repeat(64)); Opener bad = new Opener(false); bad.publishedSha = "0".repeat(64);
            Main.Job failed = new Main.Job(wrong, failedProfile, bad, Main.HTTP_IDLE_TIMEOUT_NANOS); failed.launch(); stopped(failed);
            check("failed".equals(failed.snapshot().get("status")) && bad.requests >= 5, "all actual range bytes downloaded before final checksum mismatch");
            check(!failed.state.containsKey("completed_at") && !kinds(failed).contains("completed") && kinds(failed).contains("failed"), "checksum failure never records successful completion time or event");
            check(!Files.exists(directory.resolve("Ошибка-📦.bin")), "checksum mismatch never publishes a final file");

            Map<String,Object> legacy = state("cccccccccccc", "Старый-файл.bin", directory); legacy.remove("lifecycle_version"); legacy.remove("lifecycle_history");
            legacy.put("created_at", "2020-01-01T00:00:00Z"); DownloadHistory.begin(legacy);
            check(!legacy.containsKey("first_started_at") && DownloadHistory.events(legacy).get(0).get("kind").equals("resumed"), "legacy resume keeps historical first start unknown instead of guessing created_at or now");
            DownloadHistory.begin(legacy); check(DownloadHistory.events(legacy).size() == 1, "internal retry within one run does not fabricate another resume");

            Path recoveryProfile = own.resolve("published-recovery"); Files.createDirectories(recoveryProfile);
            Map<String,Object> published = state("dddddddddddd", name, directory); published.remove("lifecycle_version");
            published.put("size", DATA.length); published.put("sha256", hash("SHA-256"));
            Main.Job recovered = new Main.Job(published, recoveryProfile, new Opener(false), Main.HTTP_IDLE_TIMEOUT_NANOS);
            recovered.launch(); stopped(recovered);
            check("complete".equals(recovered.snapshot().get("status")) && kinds(recovered).equals(List.of("recovered")), "already published matching file is verified without a fake new download");
            check(!recovered.state.containsKey("first_started_at") && !recovered.state.containsKey("completed_at"), "published-file recovery does not invent prior start or completion dates");

            Path readOnlyProfile = own.resolve("history-unavailable"); Files.createDirectories(readOnlyProfile);
            Files.writeString(readOnlyProfile.resolve("logs"), "Owned file blocks history directory", StandardCharsets.UTF_8);
            Main.Job resilient = new Main.Job(state("eeeeeeeeeeee", "Без-журнала.bin", directory), readOnlyProfile, new Opener(false), Main.HTTP_IDLE_TIMEOUT_NANOS);
            resilient.launch(); stopped(resilient);
            check("complete".equals(resilient.snapshot().get("status")) && Files.exists(directory.resolve("Без-журнала.bin")), "history I/O failure cannot fail an otherwise verified download");

            Map<String,Object> torrent = state("ffffffffffff", "Раздача-📦", directory); torrent.put("kind", "torrent");
            torrent.put("selected_files", List.of("Файл-ä.bin", "Папка/二.bin")); DownloadHistory.begin(torrent);
            Main.Job torrentJob = new Main.Job(torrent, own.resolve("torrent-observer"), new Opener(false), Main.HTTP_IDLE_TIMEOUT_NANOS);
            torrentJob.torrentUpdate(Map.of("status", "seeding", "verified", true, "pieces_verified", true));
            torrentJob.torrentUpdate(Map.of("status", "complete", "verified", true, "pieces_verified", true));
            check(kinds(torrentJob).stream().filter("completed"::equals).count() == 1, "verified torrent seeding and final callback record one download completion");
            check(Main.arr(events(torrentJob).get(0).get("paths")).size() == 2, "torrent history stores full selected file paths");
            Main.arr(Main.obj(Main.arr(torrentJob.snapshot().get("lifecycle_history")).get(0)).get("paths")).clear();
            check(Main.arr(events(torrentJob).get(0).get("paths")).size() == 2, "API history path lists cannot mutate persistent job history");

            Path boundedProfile = own.resolve("bounded-history-view"); Files.createDirectories(boundedProfile);
            Map<String,Object> many = state("111111111111", "История-ä-📦.bin", directory); DownloadHistory writer = new DownloadHistory(boundedProfile, many);
            for (int i = 0; i < 510; i++) { DownloadHistory.begin(many); DownloadHistory.pause(many, "pause"); }
            writer.checkpoint(many);
            String bounded = DownloadHistory.snapshotText(boundedProfile, "en");
            check(bounded.lines().count() <= 1001 && journal(boundedProfile).size() == 1020, "UI tail is at most 1000 entries while full on-disk audit retains 1020 transitions");
            check(bounded.contains("📦") && !bounded.contains("\ufffd"), "bounded history tail preserves UTF-8 character boundaries");
            Path journalPath = boundedProfile.resolve("logs/downloads-history.jsonl");
            byte[] completeLines = Files.readAllBytes(journalPath);
            try (OutputStream output = Files.newOutputStream(journalPath, StandardOpenOption.TRUNCATE_EXISTING)) {
                output.write(("Ж📦".repeat(400000) + "\n").getBytes(StandardCharsets.UTF_8)); output.write(completeLines);
            }
            check(Files.size(journalPath) > 2L * 1024 * 1024 && DownloadHistory.snapshotText(boundedProfile, "en").contains("История-ä-📦.bin") &&
                    !DownloadHistory.snapshotText(boundedProfile, "en").contains("\ufffd"), "history viewer safely reads a bounded two-MiB tail after a Unicode boundary");
            Files.write(journalPath, new byte[]{(byte)0xe2,(byte)0x82}, StandardOpenOption.APPEND);
            check(DownloadHistory.snapshotText(boundedProfile, "en").contains("📦"), "torn trailing UTF-8 append does not hide previous complete history entries");
            DownloadHistory afterCrash = new DownloadHistory(boundedProfile, many); many.put("filename", "После-обрыва-📦.bin");
            DownloadHistory.begin(many); afterCrash.checkpoint(many);
            check(DownloadHistory.snapshotText(boundedProfile, "en").contains("После-обрыва-📦.bin"), "restarted history writer can continue after an interrupted UTF-8 journal suffix");
            AppLog.flush();
            Map<String,Object> report = new LinkedHashMap<>();
            report.put("status", "PASS"); report.put("version", Main.VERSION); report.put("checks", checks); report.put("cases", cases);
            report.put("user_logs_read", false); report.put("user_media_read", false); report.put("clipboard_read", false); report.put("network_calls", 0);
            report.put("actual_segmented_bytes", DATA.length); report.put("unicode_fixture", name); report.put("utc_instants_and_local_offsets", true);
            Files.writeString(own.resolve("HISTORY-LIFECYCLE-TESTS.json"), Json.stringify(report) + "\n", StandardCharsets.UTF_8);
            System.out.println("PASS " + checks + " history lifecycle checks");
        } finally { AppLog.close(); }
    }
}
