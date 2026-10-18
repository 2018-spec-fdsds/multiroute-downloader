package multiroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Observed download lifecycle, independent of the bounded application diagnostics. */
final class DownloadHistory {
    private static final Object JOURNAL_LOCK = new Object();
    private static final int MAX_VIEW_BYTES = 2 * 1024 * 1024, MAX_VIEW_EVENTS = 1000;
    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
    private final Path canonical, human;
    private final Set<String> written = new HashSet<>(), humanWritten = new HashSet<>();
    private boolean scanned, warned;

    DownloadHistory(Path dataDirectory, Map<String,Object> state) {
        canonical = dataDirectory.resolve("logs").resolve("downloads-history.jsonl");
        human = canonical.resolveSibling("downloads-history.log");
        // A restored process is no longer active. Its historical start date is never guessed.
        state.put("lifecycle_run_open", false);
        if (!(state.get("lifecycle_history") instanceof List<?>)) state.put("lifecycle_history", new ArrayList<>());
        scanned = events(state).isEmpty();
    }

    static void initialize(Map<String,Object> state) {
        state.put("lifecycle_version", 1);
        state.put("lifecycle_history", new ArrayList<>());
    }

    static List<Map<String,Object>> events(Map<String,Object> state) {
        List<Map<String,Object>> result = new ArrayList<>();
        if (state.get("lifecycle_history") instanceof List<?> entries) for (Object raw : entries)
            if (raw instanceof Map<?,?> item && valid(item)) {
                Map<String,Object> event = new LinkedHashMap<>();
                for (String key : List.of("event_id", "job_id", "kind", "time", "filename", "path", "paths", "reason"))
                    if (item.containsKey(key)) event.put(key, key.equals("paths") && item.get(key) instanceof List<?> paths
                            ? new ArrayList<>(paths) : item.get(key));
                result.add(event);
            }
        return result;
    }

    private static boolean valid(Map<?,?> event) {
        try {
            return event.get("event_id") instanceof String id && id.matches("[a-f0-9-]{36}") &&
                    event.get("job_id") instanceof String job && job.matches("[a-f0-9]{12}") &&
                    Set.of("started", "resumed", "paused", "completed", "recovered", "failed").contains(event.get("kind")) &&
                    Instant.parse(String.valueOf(event.get("time"))) != null;
        } catch (RuntimeException malformed) { return false; }
    }

    static void begin(Map<String,Object> state) {
        if (Boolean.TRUE.equals(state.get("lifecycle_run_open"))) return;
        boolean observedBefore = events(state).stream().anyMatch(event -> Set.of("started", "resumed").contains(event.get("kind")));
        String kind = !observedBefore && Long.valueOf(1).equals(number(state.get("lifecycle_version"))) ? "started" : "resumed";
        record(state, kind, "");
        state.put("lifecycle_run_open", true);
    }

    private static Long number(Object value) { return value instanceof Number n ? n.longValue() : null; }

    static void pause(Map<String,Object> state, String reason) {
        if (!Boolean.TRUE.equals(state.get("lifecycle_run_open"))) return;
        if (!state.containsKey("completed_at")) record(state, "paused", reason);
        state.put("lifecycle_run_open", false);
    }

    static void complete(Map<String,Object> state) {
        if (!state.containsKey("completed_at")) record(state, "completed", "");
        state.put("lifecycle_run_open", false);
    }

    static void recovered(Map<String,Object> state) {
        // Finding a previously published file proves integrity now, not its historical completion time.
        if (!state.containsKey("completed_at") && events(state).stream().noneMatch(event -> "recovered".equals(event.get("kind"))))
            record(state, "recovered", "");
        state.put("lifecycle_run_open", false);
    }

    static void failed(Map<String,Object> state) {
        if (Boolean.TRUE.equals(state.get("lifecycle_run_open"))) record(state, "failed", "");
        state.put("lifecycle_run_open", false);
    }

    private static void record(Map<String,Object> state, String kind, String reason) {
        Map<String,Object> event = new LinkedHashMap<>();
        String time = Instant.now().toString();
        event.put("event_id", UUID.randomUUID().toString()); event.put("job_id", Main.str(state.get("id")));
        event.put("kind", kind); event.put("time", time); event.put("filename", Main.str(state.get("filename")));
        Path directory = Path.of(Main.str(state.get("directory"))).toAbsolutePath().normalize();
        event.put("path", directory.resolve(Main.str(state.get("filename"))).normalize().toString());
        if ("torrent".equals(state.get("kind")) && state.get("selected_files") instanceof List<?> files) {
            List<String> paths = new ArrayList<>();
            for (Object file : files) {
                Path path = directory.resolve(Main.str(file)).normalize();
                if (path.startsWith(directory)) paths.add(path.toString());
            }
            event.put("paths", paths);
        }
        if (!reason.isEmpty()) event.put("reason", reason);
        List<Map<String,Object>> history = events(state); history.add(event); state.put("lifecycle_history", history);
        switch (kind) {
            case "started" -> state.putIfAbsent("first_started_at", time);
            case "resumed" -> state.put("last_resumed_at", time);
            case "completed" -> state.put("completed_at", time);
            default -> { }
        }
    }

    /** Called after an atomic job checkpoint. Failures must never fail the transfer itself. */
    void checkpoint(Map<String,Object> state) {
        boolean interrupted = Thread.interrupted();
        try {
            List<Map<String,Object>> entries = events(state);
            if (entries.isEmpty()) return;
            if (scanned && entries.stream().allMatch(event -> written.contains(Main.str(event.get("event_id"))) &&
                    humanWritten.contains(Main.str(event.get("event_id"))))) return;
            synchronized (JOURNAL_LOCK) {
                Main.safePath(canonical); Main.safePath(human); Files.createDirectories(canonical.getParent());
                if (!scanned) { scan(entries); scanned = true; }
                for (Map<String,Object> event : entries) {
                    String id = Main.str(event.get("event_id"));
                    if (!written.contains(id)) {
                        Map<String,Object> value = new LinkedHashMap<>(event);
                        value.put("format", 1); value.put("category", "download-history");
                        value.put("source", template(Main.str(event.get("kind")))); value.put("args", arguments(event));
                        append(canonical, Json.stringify(value) + "\n"); written.add(id);
                        AppLog.downloadEvent(event);
                    }
                    if (!humanWritten.contains(id)) {
                        String local = LOCAL_TIME.format(Instant.parse(Main.str(event.get("time"))).atZone(ZoneId.systemDefault()));
                        append(human, local + " [" + id + "] " + render(event, I18n.language()) + "\n"); humanWritten.add(id);
                    }
                }
                warned = false;
            }
        } catch (Exception unavailable) {
            if (!warned) { warned = true; AppLog.error("Не удалось записать историю загрузок: %s", DiagnosticText.arg(unavailable.getMessage())); }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }

    private void scan(List<Map<String,Object>> entries) throws IOException {
        Set<String> expected = new HashSet<>(); for (Map<String,Object> event : entries) expected.add(Main.str(event.get("event_id")));
        if (Files.isRegularFile(canonical, LinkOption.NOFOLLOW_LINKS)) try (BufferedReader input = journalReader(canonical)) {
            String line; while ((line = input.readLine()) != null) try {
                if (line.indexOf('\ufffd') >= 0) continue;
                Object parsed = Json.parse(line);
                if (parsed instanceof Map<?,?> value && expected.contains(value.get("event_id"))) written.add(Main.str(value.get("event_id")));
            } catch (RuntimeException tornLine) { /* keep older complete entries after an interrupted append */ }
        }
        if (Files.isRegularFile(human, LinkOption.NOFOLLOW_LINKS)) try (BufferedReader input = journalReader(human)) {
            String line; while ((line = input.readLine()) != null) {
                if (line.indexOf('\ufffd') >= 0) continue;
                int start = line.indexOf('['), end = start < 0 ? -1 : line.indexOf(']', start);
                if (end > start && expected.contains(line.substring(start + 1, end))) humanWritten.add(line.substring(start + 1, end));
            }
        }
    }

    private static BufferedReader journalReader(Path path) throws IOException {
        // A crash can leave an incomplete UTF-8 suffix. Skip that line without blocking future appends.
        return new BufferedReader(new InputStreamReader(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS), StandardCharsets.UTF_8));
    }

    private static void append(Path path, String value) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            long length = channel.size();
            if (length > 0) {
                ByteBuffer last = ByteBuffer.allocate(1); channel.read(last, length - 1);
                if (last.array()[0] != '\n') { channel.position(length); channel.write(ByteBuffer.wrap(new byte[]{'\n'})); length++; }
            }
            channel.position(length); ByteBuffer bytes = ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(false);
        }
    }

    /** Bounded tail for the log window; the append-only journal itself retains all entries. */
    static String snapshotText(Path dataDirectory, String language) {
        Path path = dataDirectory.resolve("logs").resolve("downloads-history.jsonl");
        try {
            Main.safePath(path);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                return I18n.tFor(language, "История загрузок появится после начала первой загрузки.");
            byte[] tail; long start;
            // Append-only reads do not hold the writer lock; an in-flight final line is skipped below.
            try (FileChannel input = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                long length = input.size(); start = Math.max(0, length - MAX_VIEW_BYTES);
                ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(MAX_VIEW_BYTES, length));
                long offset = start;
                while (buffer.hasRemaining()) { int read = input.read(buffer, offset); if (read <= 0) break; offset += read; }
                tail = java.util.Arrays.copyOf(buffer.array(), buffer.position());
            }
            Deque<String> lines = new ArrayDeque<>(); int beginning = 0;
            if (start > 0) { while (beginning < tail.length && tail[beginning] != '\n') beginning++; beginning++; }
            for (int end = beginning; end < tail.length; end++) if (tail[end] == '\n') {
                try {
                    String raw = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(tail, beginning, end - beginning)).toString();
                    Object parsed = Json.parse(raw);
                    if (parsed instanceof Map<?,?> item && valid(item)) {
                        Map<String,Object> event = new LinkedHashMap<>(); item.forEach((key, value) -> event.put(String.valueOf(key), value));
                        String local = LOCAL_TIME.format(Instant.parse(Main.str(event.get("time"))).atZone(ZoneId.systemDefault()));
                        lines.addLast(local + " " + render(event, language));
                        if (lines.size() > MAX_VIEW_EVENTS) lines.removeFirst();
                    }
                } catch (Exception malformed) { /* incomplete append or malformed imported line is not displayed */ }
                beginning = end + 1;
            }
            StringBuilder view = new StringBuilder(I18n.tFor(language, "Последние записи истории: %d. Полная история: %s", lines.size(), path.toAbsolutePath().normalize())).append('\n');
            for (String line : lines) view.append(line).append('\n');
            return view.toString();
        } catch (Exception unavailable) { return I18n.tFor(language, "Не удалось прочитать историю загрузок."); }
    }

    static String template(String kind) {
        return switch (kind) {
            case "started" -> "Загрузка начата: %s; полный путь: %s; задание: %s";
            case "resumed" -> "Загрузка продолжена: %s; полный путь: %s; задание: %s";
            case "paused" -> "Загрузка приостановлена: %s; полный путь: %s; задание: %s";
            case "completed" -> "Загрузка завершена и проверена: %s; полный путь: %s; задание: %s";
            case "recovered" -> "Ранее сохранённый файл проверен: %s; полный путь: %s; задание: %s";
            default -> "Загрузка завершилась ошибкой: %s; полный путь: %s; задание: %s";
        };
    }
    static List<String> arguments(Map<String,Object> event) {
        return List.of(singleLine(Main.str(event.get("filename"))), singleLine(Main.str(event.get("path"))), Main.str(event.get("job_id")));
    }
    static String render(Map<String,Object> event, String language) {
        return singleLine(I18n.tFor(language, template(Main.str(event.get("kind"))), arguments(event).toArray()));
    }
    private static String singleLine(String value) { return value.replace('\r', ' ').replace('\n', ' ').replace('\u0000', ' '); }
}
