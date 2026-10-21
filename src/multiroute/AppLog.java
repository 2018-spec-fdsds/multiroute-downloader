package multiroute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/** Bounded asynchronous logs. Source templates survive language changes and application restarts. */
public final class AppLog {
    private static final int MAX_EVENTS = 2048;
    private static final int MAX_TEXT = 2 * 1024 * 1024;
    private static final int MAX_FIELD = 4096;
    private static final int MAX_ARGUMENTS = 16;
    private static final int MAX_EVENT_TEXT = 16 * 1024;
    private static final int QUEUE_SIZE = 1024;
    private static final int MAX_CANONICAL_FILE = 8 * 1024 * 1024;
    private static final int MAX_ERROR_JOURNAL = 4 * 1024 * 1024;
    private static final int ERROR_JOURNAL_BACKUPS = 3;
    private static final Pattern MAC = Pattern.compile("(?i)(?<![0-9a-f])(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}(?![0-9a-f])|(?i)(?<![0-9a-f])[0-9a-f]{4}(?:\\.[0-9a-f]{4}){2}(?![0-9a-f])");
    private static final Pattern URL_SECRET = Pattern.compile("(?i)(https?://)[^\\s/?#]+@|([?&](?:token|access_token|auth|authorization|signature|sig|x-amz-[^=&#\\s]+|x-goog-[^=&#\\s]+)=)[^&#\\s]*");
    private static volatile Session session;
    private static final AtomicLong REVISION = new AtomicLong();

    static {
        I18n.addLanguageListener(() -> {
            Session current = session;
            if (current != null) current.rewrite.set(true);
            REVISION.incrementAndGet();
        });
    }

    private AppLog() { }

    /** Call only after acquiring the application's single-instance guard. */
    public static void initialize(Path dataDirectory) {
        close();
        try {
            Session created = new Session(dataDirectory.toAbsolutePath().normalize());
            session = created;
            created.writer.start();
        } catch (Exception unavailable) {
            // Logging is optional: a read-only disk must never stop a download.
            System.err.println(sanitize(unavailable.getMessage()));
        }
    }

    public static boolean enabled() { return session != null; }
    public static long snapshotRevision() { return REVISION.get(); }
    public static Path logsDirectory() {
        Session current = session;
        return current == null ? null : current.directory;
    }
    public static void info(String source, Object... args) { record("INFO", "application", source, args); }
    public static void error(String source, Object... args) { record("ERROR", "application", source, args); }
    /** Network errors have their own durable journal, independent of the bounded UI history. */
    public static void networkError(String source, Object... args) { record("ERROR", "network", source, args); }
    public static void nativeLine(String raw) {
        if (raw != null && raw.startsWith("NETWORK_TOPOLOGY_ERROR ")) {
            networkError("Network topology check failed: %s", DiagnosticText.arg(
                    NetworkDiagnostics.safe(raw.substring("NETWORK_TOPOLOGY_ERROR ".length()))));
        } else if (raw != null && raw.startsWith("TUNNEL_ERROR ")) {
            try {
                if (raw.length() > 4096) throw new IllegalArgumentException();
                Map<String,Object> fields = Main.obj(Json.parse(raw.substring("TUNNEL_ERROR ".length())));
                String gateway = Main.str(fields.get("gateway")), host = Main.str(fields.get("host"));
                String destination = Main.str(fields.get("destination_ipv4")), stage = Main.str(fields.get("stage"));
                String reason = Main.str(fields.get("reason"));
                long port = Main.num(fields.get("remote_port"), 0), elapsed = Main.num(fields.get("elapsed_ms"), 0);
                if (!GatewayNetwork.validIpv4(gateway) || !host.matches("[A-Za-z0-9.-]{0,253}") ||
                        (!destination.isEmpty() && !GatewayNetwork.validIpv4(destination)) ||
                        !java.util.Set.of("parse", "arp", "dns", "tcp_open", "tcp_bind", "tcp_bind_interface", "tcp_connect").contains(stage) ||
                        !reason.matches("[A-Za-z0-9 .()_/-]{1,160}") || port < 0 || port > 65535 || elapsed < 0)
                    throw new IllegalArgumentException();
                Object nativeError = fields.get("lwip_error");
                String errorCode = nativeError instanceof Number number ? Long.toString(number.longValue()) : "—";
                networkError("Gateway tunnel failed: gateway=%s server=%s:%d destination=%s stage=%s elapsed_ms=%d lwip_error=%s reason=%s",
                        gateway, host.isEmpty() ? "—" : host, port, destination.isEmpty() ? "—" : destination,
                        DiagnosticText.arg(stage), elapsed, errorCode, DiagnosticText.arg(reason));
            } catch (RuntimeException malformed) {
                // Never retain an unparsed request or URL from a malformed diagnostic.
                networkError("Malformed gateway tunnel diagnostic");
            }
        } else record("INFO", "network", raw, new Object[0]);
    }
    /** Lifecycle timestamps describe the observed transition, not a later refresh of the log. */
    static void downloadEvent(Map<String,Object> entry) {
        try {
            Event event = new Event(String.valueOf(entry.get("time")), "INFO", "application",
                    sanitizeTemplate(DownloadHistory.template(String.valueOf(entry.get("kind")))),
                    List.copyOf(encodeArguments(DownloadHistory.arguments(entry).toArray(), 0, new int[]{MAX_EVENT_TEXT})));
            Session current = session;
            if (current != null && current.accepting && !current.offer(event)) current.dropped.incrementAndGet();
        } catch (RuntimeException ignored) { /* the durable history remains independent of UI diagnostics */ }
    }
    /** Nested presentation text retains its template; ordinary strings remain user data. */
    public record Message(String source, Object[] args) {
        public Message { args = args.clone(); }
        @Override public Object[] args() { return args.clone(); }
    }
    public static Message arg(String source, Object... args) { return new Message(source, args); }

    private static void record(String level, String kind, String source, Object[] args) {
        try {
            Event event = event(level, kind, source, args);
            Session current = session;
            if (current == null && !kind.equals("network")) {
                String text = render(event, I18n.language());
                if (level.equals("ERROR")) System.err.println(text); else System.out.println(text);
            }
            if (current != null && current.accepting && !current.offer(event)) current.dropped.incrementAndGet();
        } catch (RuntimeException ignored) { /* diagnostics cannot break the caller */ }
    }

    public static String snapshotText() { return snapshotTextFor(I18n.language()); }
    public static String snapshotTextFor(String language) {
        Session current = session;
        if (current == null) return "";
        StringBuilder result = new StringBuilder();
        for (Event event : current.snapshot()) result.append(line(event, language));
        return result.toString();
    }

    /** A bounded test/shutdown barrier; normal recording never waits for disk I/O. */
    public static void flush() {
        Session current = session;
        if (current != null) current.barrier(false);
    }
    public static void close() {
        Session current = session;
        if (current == null) return;
        current.accepting = false;
        current.barrier(true);
        if (session == current) session = null;
    }

    private static Event event(String level, String kind, String source, Object[] args) {
        List<Object> values = encodeArguments(args, 0, new int[]{MAX_EVENT_TEXT});
        return new Event(Instant.now().toString(), level, kind, sanitizeTemplate(source), List.copyOf(values));
    }

    private static List<Object> encodeArguments(Object[] args, int depth, int[] budget) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < Math.min(args.length, MAX_ARGUMENTS); i++) {
            Object arg = args[i];
            if (budget[0] <= 0) values.add("");
            else if (arg instanceof DiagnosticText.Arg diagnostic)
                values.add(Map.of("diagnostic", budgetedTemplate(diagnostic.raw(), budget)));
            else if (arg instanceof Message message && depth < 4) {
                budget[0] -= 80;
                values.add(Map.of("source", budgetedTemplate(message.source(), budget), "args", encodeArguments(message.args(), depth + 1, budget)));
            } else if (arg instanceof Number || arg instanceof Boolean) { values.add(arg); budget[0] -= 32; }
            else values.add(budgeted(String.valueOf(arg), budget));
        }
        return values;
    }
    private static String budgeted(String value, int[] budget) {
        String text = sanitize(value);
        int length = Math.min(text.length(), Math.max(0, budget[0]));
        budget[0] -= length + 32;
        return text.substring(0, length);
    }
    private static String budgetedTemplate(String value, int[] budget) {
        String text = sanitizeTemplate(value);
        int length = Math.min(text.length(), Math.max(0, budget[0]));
        budget[0] -= length + 32;
        return text.substring(0, length);
    }

    private static String sanitize(String value) {
        return sanitize(value, false);
    }
    private static String sanitizeTemplate(String value) {
        return sanitize(value, true);
    }
    private static String sanitize(String value, boolean template) {
        String source = value == null ? "" : value;
        // Legacy/native messages can contain hardware addresses, even when the UI no longer does.
        source = source.replaceAll("(?i)\\s*[.;]?\\s*MAC\\s+(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\\.?", "")
                .replaceAll("(?i)\\s+mac=(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}", "");
        source = MAC.matcher(source).replaceAll("[hidden]");
        java.util.regex.Matcher secrets = URL_SECRET.matcher(source);
        StringBuffer safe = new StringBuffer();
        while (secrets.find()) secrets.appendReplacement(safe, java.util.regex.Matcher.quoteReplacement(
                secrets.group(1) != null ? secrets.group(1) + "[hidden]@" : secrets.group(2) + "[hidden]"));
        secrets.appendTail(safe);
        source = safe.toString().replace('\u0000', ' ');
        if (!template) source = source.replace('\r', ' ').replace('\n', ' ');
        return source.length() <= MAX_FIELD ? source : source.substring(0, MAX_FIELD) + "…";
    }

    private static String render(Event event, String language) {
        try {
            if (event.args.isEmpty()) return sanitize(DiagnosticText.renderFor(language, event.source));
            Object[] args = decodeArguments(event.args, language, 0);
            return sanitize(I18n.tFor(language, event.source, args));
        } catch (RuntimeException invalidTemplate) {
            return sanitize(I18n.tFor(language, "Не удалось обработать сообщение журнала"));
        }
    }
    private static Object[] decodeArguments(List<?> values, String language, int depth) {
        Object[] result = new Object[values.size()];
        for (int i = 0; i < result.length; i++) {
            Object value = values.get(i);
            if (value instanceof Map<?,?> map && map.get("diagnostic") instanceof String text)
                result[i] = DiagnosticText.renderFor(language, text);
            else if (depth < 4 && value instanceof Map<?,?> map && map.get("source") instanceof String source && map.get("args") instanceof List<?> args)
                result[i] = args.isEmpty() ? DiagnosticText.renderFor(language, source)
                        : I18n.tFor(language, source, decodeArguments(args, language, depth + 1));
            else result[i] = value;
        }
        return result;
    }
    private static String line(Event event, String language) {
        return event.time + " [" + event.level + "] " + render(event, language) + System.lineSeparator();
    }

    private record Event(String time, String level, String kind, String source, List<Object> args) {
        int size() { return source.length() + args.toString().length() + 80; }
        Map<String,Object> map() {
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("time", time); result.put("level", level); result.put("kind", kind);
            result.put("source", source); result.put("args", args); return result;
        }
    }
    private record Barrier(CountDownLatch done, boolean close) { }

    private static final class Session {
        final Path directory, canonical, application, network, networkErrors;
        final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        final Deque<Event> history = new ArrayDeque<>();
        final AtomicLong dropped = new AtomicLong();
        final AtomicLong droppedNetworkErrors = new AtomicLong();
        final AtomicBoolean rewrite = new AtomicBoolean(true);
        final Thread writer;
        volatile boolean accepting = true;
        volatile boolean closeRequested;
        int historySize;
        boolean errorJournalUnavailable;

        Session(Path data) throws IOException {
            directory = data.resolve("logs"); canonical = directory.resolve("events.json");
            application = directory.resolve("application.log"); network = data.resolve("network").resolve("gateway-network.log");
            networkErrors = directory.resolve("network-errors.log");
            Files.createDirectories(directory); Files.createDirectories(network.getParent());
            restore();
            writer = new Thread(this::run, "application-log-writer"); writer.setDaemon(true);
        }
        void restore() {
            try {
                if (!Files.isRegularFile(canonical) || Files.size(canonical) > MAX_CANONICAL_FILE) return;
                Object parsed = Json.parse(Files.readString(canonical, StandardCharsets.UTF_8));
                if (!(parsed instanceof Map<?,?> root) || !Long.valueOf(1).equals(root.get("format")) ||
                        !(root.get("events") instanceof List<?> events)) return;
                for (Object raw : events) {
                    if (!(raw instanceof Map<?,?> item) || !(item.get("args") instanceof List<?> args)) continue;
                    String kind = String.valueOf(item.get("kind")), level = String.valueOf(item.get("level"));
                    if (!(kind.equals("application") || kind.equals("network")) || !(level.equals("INFO") || level.equals("ERROR"))) continue;
                    List<Object> values = restoreArguments(args, 0);
                    String time = String.valueOf(item.get("time")); Instant.parse(time);
                    append(new Event(time, level, kind, sanitizeTemplate(String.valueOf(item.get("source"))), List.copyOf(values)));
                }
            } catch (Exception ignored) { /* unrelated/old human logs are never read or interpreted */ }
        }
        List<Object> restoreArguments(List<?> args, int depth) {
            List<Object> values = new ArrayList<>();
            for (Object value : args.subList(0, Math.min(args.size(), MAX_ARGUMENTS))) {
                if (value instanceof Map<?,?> nested && nested.get("diagnostic") instanceof String text)
                    values.add(Map.of("diagnostic", sanitizeTemplate(text)));
                else if (depth < 4 && value instanceof Map<?,?> nested && nested.get("source") instanceof String source && nested.get("args") instanceof List<?> children)
                    values.add(Map.of("source", sanitizeTemplate(source), "args", restoreArguments(children, depth + 1)));
                else if (value instanceof Number || value instanceof Boolean) values.add(value);
                else values.add(sanitize(String.valueOf(value)));
            }
            return values;
        }
        synchronized void append(Event event) {
            history.addLast(event); historySize += event.size();
            while (history.size() > MAX_EVENTS || historySize > MAX_TEXT) historySize -= history.removeFirst().size();
            REVISION.incrementAndGet();
        }
        synchronized List<Event> snapshot() { return new ArrayList<>(history); }
        boolean offer(Event event) {
            if (queue.offer(event)) return true;
            if (event.kind.equals("network") && event.level.equals("ERROR")) {
                // A connection/debug burst must not discard the more useful failure record.
                for (Object queued : queue) {
                    if (queued instanceof Event prior && prior.level.equals("INFO") && queue.remove(queued)) {
                        dropped.incrementAndGet();
                        if (queue.offer(event)) return true;
                    }
                }
                droppedNetworkErrors.incrementAndGet();
            }
            return false;
        }
        void barrier(boolean close) {
            if (Thread.currentThread() == writer) return;
            boolean interrupted = Thread.interrupted();
            CountDownLatch done = new CountDownLatch(1);
            try {
                if (queue.offer(new Barrier(done, close), 3, TimeUnit.SECONDS)) done.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException cancelled) { interrupted = true; }
            finally {
                if (close) closeRequested = true;
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
        void run() {
            long nextSave = 0;
            boolean dirty = true;
            try {
                for (;;) {
                    Object command = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (command instanceof Event event) {
                        append(event); dirty = true;
                        if (event.kind.equals("network") && event.level.equals("ERROR")) appendNetworkError(event);
                        if (event.kind.equals("application")) {
                            String text = render(event, I18n.language());
                            if (event.level.equals("ERROR")) System.err.println(text); else System.out.println(text);
                        }
                    }
                    long lost = dropped.getAndSet(0);
                    if (lost > 0) { append(event("ERROR", "application", "Часть сообщений журнала пропущена: %d", new Object[]{lost})); dirty = true; }
                    long lostErrors = droppedNetworkErrors.getAndSet(0);
                    if (lostErrors > 0) {
                        Event notice = event("ERROR", "network", "Часть сообщений журнала пропущена: %d", new Object[]{lostErrors});
                        append(notice); appendNetworkError(notice); dirty = true;
                    }
                    boolean requested = rewrite.getAndSet(false);
                    if (command instanceof Barrier barrier) {
                        save(); dirty = false; nextSave = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                        barrier.done.countDown(); if (barrier.close) break;
                    } else if (requested || (dirty && System.nanoTime() >= nextSave)) {
                        save(); dirty = false; nextSave = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                    }
                    if (closeRequested && queue.isEmpty()) { if (dirty) save(); break; }
                }
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        void save() {
            try {
                List<Event> events = snapshot(); List<Object> values = new ArrayList<>();
                String language = I18n.language(); StringBuilder app = new StringBuilder(), net = new StringBuilder();
                for (Event event : events) {
                    values.add(event.map()); (event.kind.equals("network") ? net : app).append(line(event, language));
                }
                atomic(canonical, Json.stringify(Map.of("format", 1, "events", values)));
                atomic(application, app.toString()); atomic(network, net.toString());
            } catch (Exception ignored) { /* never recurse into the logger after disk errors */ }
        }
        /** Append once at observation time; restoring UI events or changing language never duplicates errors. */
        void appendNetworkError(Event event) {
            try {
                byte[] bytes = line(event, I18n.language()).getBytes(StandardCharsets.UTF_8);
                if (Files.isRegularFile(networkErrors) && Files.size(networkErrors) + bytes.length > MAX_ERROR_JOURNAL) {
                    for (int index = ERROR_JOURNAL_BACKUPS; index >= 1; index--) {
                        Path from = index == 1 ? networkErrors : networkErrors.resolveSibling("network-errors.log." + (index - 1));
                        Path to = networkErrors.resolveSibling("network-errors.log." + index);
                        if (Files.exists(from)) Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                Files.write(networkErrors, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                errorJournalUnavailable = false;
            } catch (IOException | SecurityException unavailable) {
                if (!errorJournalUnavailable) System.err.println(DiagnosticText.render("Could not append network error journal") + ": " + sanitize(unavailable.getMessage()));
                errorJournalUnavailable = true;
            }
        }
        void atomic(Path path, String value) throws IOException {
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, value, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        }
    }
}
