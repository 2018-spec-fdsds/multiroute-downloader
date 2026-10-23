package multiroute;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Native desktop controller, versioned local API and durable segmented downloads. */
public final class Main {
    static final String VERSION = "4.1.3";
    static final int MAX_CHUNK_MB = 64;
    static final Pattern SHA256 = Pattern.compile("[a-fA-F0-9]{64}");
    static final Pattern JOB_ACTION = Pattern.compile("/jobs/([a-f0-9]{12})/(pause|resume)");
    static final Pattern JOB_PATH = Pattern.compile("/jobs/([a-f0-9]{12})");
    static final Pattern JOB_OPTIONS = Pattern.compile("/jobs/([a-f0-9]{12})/options");
    static final Pattern JOB_ROUTES = Pattern.compile("/jobs/([a-f0-9]{12})/routes");
    static final long HTTP_IDLE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60);
    static final Pattern JOB_DIRECTORY = Pattern.compile("/jobs/([a-f0-9]{12})/directory");
    static final Pattern RESERVED = Pattern.compile("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?");
    static final Set<Lease> ALL_STREAMS = ConcurrentHashMap.newKeySet();
    static final Set<Job> METRIC_JOBS = ConcurrentHashMap.newKeySet();
    static final ScheduledExecutorService METRICS = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "download-metrics-checkpoint"); thread.setDaemon(true); return thread;
    });
    static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "range-idle-watchdog"); thread.setDaemon(true); return thread;
    });
    static {
        METRICS.scheduleAtFixedRate(() -> {
            for (Job job : METRIC_JOBS) try { job.checkpointMetrics(); }
            catch (RuntimeException error) { AppLog.error("Download metrics checkpoint: %s", DiagnosticText.arg(error.getMessage())); }
        }, 1, 1, TimeUnit.SECONDS);
        WATCHDOG.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            for (Job job : METRIC_JOBS) job.watchAttempts(now);
            for (Lease lease : ALL_STREAMS)
                if (now - lease.lastProgress.get() > TimeUnit.SECONDS.toNanos(60)) lease.close();
        }, 1, 1, TimeUnit.SECONDS);
    }

    static final class Lease implements AutoCloseable {
        final InputStream stream;
        final Set<Lease> owner;
        final AtomicLong lastProgress = new AtomicLong(System.nanoTime());
        Lease(InputStream stream, Set<Lease> owner) {
            this.stream = stream; this.owner = owner; owner.add(this); ALL_STREAMS.add(this);
        }
        void touch() { lastProgress.set(System.nanoTime()); }
        @Override public void close() {
            owner.remove(this); ALL_STREAMS.remove(this);
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    static String str(Object value) { return value == null ? "" : String.valueOf(value); }
    static long num(Object value, long fallback) { return value instanceof Number ? ((Number) value).longValue() : fallback; }
    static boolean yes(Object value) { return Boolean.TRUE.equals(value); }
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object value) { return (Map<String,Object>) value; }
    @SuppressWarnings("unchecked") static List<Object> arr(Object value) { return (List<Object>) value; }
    static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }
    static MessageDigest sha() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    static Path normalized(Path path) { return path.toAbsolutePath().normalize(); }
    static void safePath(Path path) throws IOException {
        for (Path item = normalized(path); item != null; item = item.getParent()) {
            if (Files.exists(item, LinkOption.NOFOLLOW_LINKS)) {
                BasicFileAttributes attributes = Files.readAttributes(item, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attributes.isSymbolicLink() || attributes.isOther())
                    throw new IOException("Путь является ссылкой или особым объектом: " + item);
            }
            if (Files.exists(item, LinkOption.NOFOLLOW_LINKS) && !item.equals(normalized(path)) &&
                    !Files.isDirectory(item, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Путь папки занят файлом: " + item);
        }
    }
    static Path existingFolder(Path directory) throws IOException {
        Path folder = normalized(directory); safePath(folder);
        while (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS) && folder.getParent() != null) folder = folder.getParent();
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Папка сохранения занята файлом: " + directory);
        return folder;
    }
    static String spaceError(Path directory, long required, long free) {
        return String.format(Locale.ROOT, "Недостаточно свободного места: нужно %.2f ГиБ (%d байт), доступно %.2f ГиБ (%d байт). Папка: %s. Выберите другую папку сохранения.",
                required / 1073741824.0, required, free / 1073741824.0, free, normalized(directory));
    }
    static void requireSpace(Path directory, long required) throws IOException {
        long free = Files.getFileStore(existingFolder(directory)).getUsableSpace();
        if (required > free) throw new IOException(spaceError(directory, required, free));
    }

    static String filename(String value) {
        String name = value.trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\") ||
            name.matches(".*[<>:\"|?*\\x00-\\x1f].*") || name.endsWith(" ") || name.endsWith(".") || RESERVED.matcher(name).matches())
            throw new IllegalArgumentException("Некорректное имя файла");
        return name;
    }

    static String httpUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Нужен URL http:// или https:// без встроенного пароля");
            return value;
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Нужен корректный URL http:// или https:// без встроенного пароля");
        }
    }

    static void save(Path path, Map<String,Object> value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        byte[] bytes = Json.stringify(value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException error) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    /** In a selected-gateway job the four explicit paths own the traffic; an anonymous OS path is redundant. */
    static List<Object> normalizeRoutes(List<Object> source) {
        Set<String> explicit = new LinkedHashSet<>();
        for (Object raw : source) {
            Map<String,Object> route = obj(raw);
            if ("gateway".equals(route.get("type")) && yes(route.get("enabled"))) explicit.add(str(route.get("gateway")));
        }
        List<Object> result = new ArrayList<>(); Map<String,Map<String,Object>> gateways = new LinkedHashMap<>();
        for (Object raw : source) {
            Map<String,Object> route = new LinkedHashMap<>(obj(raw));
            String type = str(route.get("type")), gateway = str(route.get("gateway"));
            boolean system = type.equals("direct") || type.equals("default");
            if (!explicit.isEmpty() && system && gateway.isBlank()) continue;
            if (!explicit.isEmpty() && system && explicit.contains(gateway)) {
                // Select the named gateway transport without attributing old OS-path counters to it.
                continue;
            }
            if ("gateway".equals(type)) {
                Map<String,Object> previous = gateways.get(gateway);
                if (previous != null) {
                    long received = num(previous.get("received_bytes"), num(previous.get("bytes"), 0)) +
                            num(route.get("received_bytes"), num(route.get("bytes"), 0));
                    long bytes = num(previous.get("bytes"), 0) + num(route.get("bytes"), 0);
                    long chunks = num(previous.get("chunks"), 0) + num(route.get("chunks"), 0);
                    if (yes(previous.get("enabled")) && yes(route.get("enabled")) &&
                            (!str(previous.get("virtual_ip")).equals(str(route.get("virtual_ip"))) || !str(previous.get("site_profile_id")).equals(str(route.get("site_profile_id")))))
                        throw new IllegalArgumentException("Повторный шлюз имеет разные настройки адреса приложения: " + gateway);
                    if (!yes(previous.get("enabled")) && yes(route.get("enabled"))) previous.putAll(route);
                    previous.put("enabled", yes(previous.get("enabled")) || yes(route.get("enabled")));
                    previous.put("bytes", bytes);
                    previous.put("chunks", chunks);
                    previous.put("received_bytes", received);
                    continue;
                }
                gateways.put(gateway, route);
            }
            result.add(route);
        }
        if (!explicit.isEmpty() && result.stream().filter(item -> yes(obj(item).get("enabled"))).count() > 32)
            throw new IllegalArgumentException("Choose at most 32 independent gateways");
        return result;
    }

    static final class Job {
        final Map<String,Object> state;
        final Path manifest;
        volatile boolean stop;
        volatile boolean restart;
        volatile Thread thread;
        volatile boolean shuttingDown;
        volatile boolean maintenance;
        volatile TransferLimits transferLimits;
        volatile APIEvents events;
        final List<Thread> routeThreads = new CopyOnWriteArrayList<>();
        final Set<Lease> openStreams = ConcurrentHashMap.newKeySet();
        final DownloadMetrics metrics;
        final DownloadHistory history;
        @FunctionalInterface interface HttpOpener {
            java.net.http.HttpResponse<InputStream> open(Map<String,Object> route, String url, long start,
                    long end, String etag, String modified) throws Exception;
        }
        final HttpOpener httpOpener;
        final long idleTimeoutNanos;
        final Set<HttpAttempt> activeAttempts = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String,Long> disabledEpochs = new ConcurrentHashMap<>();
        final Map<String,HttpWorker> httpWorkers = new ConcurrentHashMap<>();
        volatile long routesRevision;
        volatile HttpQueue httpQueue;
        static String routeKey(Map<String,Object> route) {
            return "gateway".equals(route.get("type")) ? "gateway\n" + str(route.get("gateway")) : DownloadMetrics.key(route);
        }
        synchronized List<Map<String,Object>> routeReferences() {
            List<Map<String,Object>> result = new ArrayList<>();
            for (Object raw : arr(state.get("routes"))) if (yes(obj(raw).get("enabled"))) result.add(obj(raw));
            return result;
        }
        synchronized List<Map<String,Object>> enabledRoutesSnapshot() {
            List<Map<String,Object>> result = new ArrayList<>();
            for (Map<String,Object> route : routeReferences()) result.add(obj(Json.parse(Json.stringify(route))));
            return result;
        }
        synchronized boolean selected(Map<String,Object> route, long epoch) {
            return yes(route.get("enabled")) && disabledEpochs.getOrDefault(routeKey(route), 0L) == epoch;
        }
        void watchAttempts(long now) {
            for (HttpAttempt attempt : activeAttempts) {
                if (!attempt.finished && !attempt.cancelled && now - attempt.lastProgress >= idleTimeoutNanos) {
                    attempt.stalled = true; attempt.cancel();
                }
            }
        }
        void selectionChanged() {
            for (HttpWorker worker : httpWorkers.values()) if (!worker.selected()) worker.retire();
            for (HttpAttempt attempt : activeAttempts)
                if (!selected(attempt.route, attempt.disabledEpoch)) attempt.cancel();
            HttpQueue queue = httpQueue;
            if (queue != null) synchronized (queue) { queue.notifyAll(); }
        }
        private String loggedStatus = "";
        private String loggedError = "";

        Job(Map<String,Object> state, Path dataDir) {
            this(state, dataDir, SourceTransfer::open, HTTP_IDLE_TIMEOUT_NANOS);
        }
        /** Explicit owned-transport and accelerated-clock seams; production always uses sixty seconds. */
        Job(Map<String,Object> state, Path dataDir, HttpOpener opener, long idleTimeoutNanos) {
            if (idleTimeoutNanos <= 0 || idleTimeoutNanos > HTTP_IDLE_TIMEOUT_NANOS)
                throw new IllegalArgumentException("Invalid owned-test idle timeout");
            this.httpOpener = Objects.requireNonNull(opener); this.idleTimeoutNanos = idleTimeoutNanos;
            this.state = state;
            Checksums.normalizeState(state);
            long historical = 0;
            for (Object raw : arr(state.get("routes"))) {
                Map<String,Object> route = obj(raw);
                historical += Math.max(0, num(route.get("received_bytes"), num(route.get("bytes"), 0)));
            }
            state.put("received_bytes", Math.max(historical, num(state.get("received_bytes"), 0)));
            state.put("routes", normalizeRoutes(arr(state.get("routes"))));
            this.metrics = new DownloadMetrics(state);
            this.manifest = dataDir.resolve(str(state.get("id")) + ".json");
            this.history = new DownloadHistory(dataDir, state);
        }
        synchronized void persist() {
            metrics.checkpoint(state);
            boolean interrupted = Thread.interrupted();
            try {
                for (int attempt = 0; ; attempt++) try { save(manifest, state); break; }
                catch (java.nio.channels.ClosedByInterruptException cancelled) {
                    // Cancellation may arrive during the channel write. Reopen the atomic temporary file.
                    interrupted |= Thread.interrupted();
                    if (attempt >= 4) throw cancelled;
                }
            }
            catch (IOException error) { throw new UncheckedIOException(error); }
            finally { if (interrupted) Thread.currentThread().interrupt(); }
            history.checkpoint(state);
            String status = str(state.get("status"));
            String error = str(state.get("error"));
            if (!status.equals(loggedStatus)) {
                loggedStatus = status;
                AppLog.info("Состояние загрузки %s: %s", str(state.get("filename")), DiagnosticText.arg(status));
            }
            if (!error.isBlank() && !error.equals(loggedError))
                AppLog.error("Ошибка загрузки %s: %s", str(state.get("filename")), DiagnosticText.arg(error));
            loggedError = error;
            if(events != null) events.publish("job",snapshot());
        }
        synchronized void checkpointMetrics() { if (metrics.active()) persist(); }
        synchronized Map<String,Object> snapshot() {
            Map<String,Object> copy = new LinkedHashMap<>(state);
            copy.remove("chunks"); copy.remove("etag"); copy.remove("modified"); copy.remove("part_path");
            copy.remove("lifecycle_run_open");
            copy.remove("torrent_metadata"); copy.remove("target_paths"); copy.remove("idempotency_fingerprint"); copy.remove("idempotency_key");
            List<Object> routes = new ArrayList<>();
            for (Object item : arr(state.get("routes"))) {
                Map<String,Object> route = new LinkedHashMap<>(obj(item));
                route.remove("token"); route.remove("socks_endpoint");
                route.keySet().removeIf(GatewayNetwork::hardwareField);
                for (String field : List.of("status", "message", "last_error"))
                    if (route.get(field) instanceof String text) route.put(field, DiagnosticText.redact(text));
                routes.add(route);
            }
            copy.put("routes", routes);
            copy.put("lifecycle_history", DownloadHistory.events(state));
            copy.keySet().removeIf(key -> key.startsWith("probe_"));
            metrics.decorate(copy);
            return copy;
        }
        synchronized void launch() {
            if (shuttingDown || maintenance || "complete".equals(state.get("status"))) return;
            if (thread != null && thread.isAlive()) {
                if (stop) {
                    restart = true;
                    state.put("status", "queued"); persist();
                }
                return;
            }
            stop = false; restart = false;
            metrics.start(); METRIC_JOBS.add(this);
            state.put("status", "running"); state.put("error", "");
            try { persist(); }
            catch (RuntimeException failure) {
                stop = true; metrics.stop(); METRIC_JOBS.remove(this);
                state.put("status", "failed"); state.put("error", "Не удалось сохранить состояние запуска: " + str(failure.getMessage()));
                try { persist(); } catch (RuntimeException retry) { failure.addSuppressed(retry); }
                throw failure;
            }
            thread = new Thread(this::run, "job-" + state.get("id"));
            thread.setDaemon(true); thread.start();
        }
        void pause() {
            synchronized (this) {
                if ("complete".equals(state.get("status"))) return;
                restart = false; stop = true;
                metrics.stop(); METRIC_JOBS.remove(this);
                DownloadHistory.pause(state, "pause");
                state.put("status", "paused"); persist();
            }
            for (HttpAttempt attempt : activeAttempts) attempt.cancel();
            for (Lease lease : openStreams) lease.close();
            for (Thread route : routeThreads) route.interrupt();
        }

        void stopForShutdown() {
            Thread running;
            synchronized (this) {
                shuttingDown = true; restart = false; stop = true;
                metrics.stop(); METRIC_JOBS.remove(this);
                DownloadHistory.pause(state, "shutdown");
                running = thread;
            }
            for (HttpAttempt attempt : activeAttempts) attempt.cancel();
            for (Lease lease : openStreams) lease.close();
            for (Thread route : routeThreads) route.interrupt();
            if (running != null) running.interrupt();
            synchronized (this) { persist(); }
        }

        /** Do not hold the job monitor while torrent callbacks or route threads finish. */
        void stopAndAwait() throws IOException, InterruptedException {
            Thread running;
            synchronized (this) {
                maintenance = true; restart = false; stop = true; running = thread;
                metrics.stop(); METRIC_JOBS.remove(this);
                DownloadHistory.pause(state, "maintenance");
                if (!"complete".equals(state.get("status"))) state.put("status", "paused");
                try { persist(); } catch (RuntimeException error) { maintenance = false; throw error; }
            }
            for (HttpAttempt attempt : activeAttempts) attempt.cancel();
            for (Lease lease : openStreams) lease.close();
            for (Thread route : routeThreads) route.interrupt();
            if (running != null) running.interrupt();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            try {
                if (running != null) awaitThread(running, deadline);
                for (Thread route : routeThreads) awaitThread(route, deadline);
                if ((thread != null && thread.isAlive()) || routeThreads.stream().anyMatch(Thread::isAlive))
                    throw new IOException("Загрузка ещё останавливается; повторите действие после остановки");
                synchronized (this) {
                    if (!"complete".equals(state.get("status"))) state.put("status", "paused");
                    persist();
                }
            } catch (IOException | InterruptedException | RuntimeException error) { maintenance = false; throw error; }
        }
        private static void awaitThread(Thread thread, long deadline) throws IOException, InterruptedException {
            while (thread.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("Загрузка ещё останавливается; повторите действие после остановки");
                thread.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
            }
        }

        @SuppressWarnings("unchecked") Map<String,Object> chunks() { return (Map<String,Object>) state.get("chunks"); }
        Path part() { return Path.of(str(state.get("part_path"))); }
        long size() { return num(state.get("size"), 0); }
        long chunkSize() { return num(state.get("chunk_size"), 0); }

        static final class PartialPreservedException extends IOException {
            PartialPreservedException(String message) { super(message); }
        }

        /** A ranged source can ignore an individual request; retry without consuming its full body. */
        static final class IgnoredRangeException extends IOException {
            IgnoredRangeException() { super("Source temporarily ignored a byte range; the saved segment was preserved"); }
        }

        /** A zero-byte range is ignored by some download CDNs even when ordinary ranges work. */
        record MetadataResponse(java.net.http.HttpResponse<InputStream> response, long offset) { }

        MetadataResponse metadataResponse(Map<String,Object> route, String url, HttpAttempt attempt) throws Exception {
            var first = httpOpener.open(route, url, 0, 0, null, null);
            String scheme = URI.create(url).getScheme().toLowerCase(Locale.ROOT);
            if (first.statusCode() != 200 || !Set.of("http", "https", "webdavs").contains(scheme))
                return new MetadataResponse(first, 0);
            long total;
            try { total = wholeLength(first, null, null, null); }
            catch (Exception failure) { first.body().close(); throw failure; }
            if (total == 0 || total == 1) return new MetadataResponse(first, 0);
            String etag = first.headers().firstValue("ETag").orElse("");
            if (etag.startsWith("W/")) etag = "";
            String modified = first.headers().firstValue("Last-Modified").orElse("");
            first.body().close(); attempt.check();
            var second = httpOpener.open(route, url, 1, 1, null, null);
            try {
                if (second.statusCode() == 206) {
                    Transport.validate(second, 1, 1, total<0?null:total, etag, modified);
                    return new MetadataResponse(second, 1);
                }
                wholeLength(second, total<0?null:total, etag, modified);
                return new MetadataResponse(second, 0);
            } catch (Exception failure) { second.body().close(); throw failure; }
        }

        /** A server ignoring Range can supply one bounded whole file, never a segment. */
        static long wholeLength(java.net.http.HttpResponse<InputStream> response, Long expectedTotal,
                String etag, String modified) throws IOException {
            if (response.statusCode() != 200) throw new IOException("Expected HTTP 200 for whole file");
            if (!response.headers().firstValue("Content-Encoding").orElse("identity").equalsIgnoreCase("identity"))
                throw new IOException("Unexpected Content-Encoding");
            String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
            if (type.startsWith("multipart/") || type.startsWith("text/html"))
                throw new IOException("Источник вернул страницу HTML или multipart вместо файла; нужна ссылка на скачивание");
            long length;
            try { length = Long.parseLong(response.headers().firstValue("Content-Length").orElse("-1")); }
            catch (NumberFormatException error) { throw new IOException("Invalid Content-Length",error); }
            if (length < -1 || (length == -1 && expectedTotal != null))
                throw new IOException("The source did not provide the file size");
            if (expectedTotal != null && length != expectedTotal) throw new IOException("Remote file size changed");
            if (etag != null && !etag.isBlank() && !etag.equals(response.headers().firstValue("ETag").orElse(null)))
                throw new IOException("Remote ETag changed or is missing");
            if ((etag == null || etag.isBlank()) && modified != null && !modified.isBlank() &&
                    !modified.equals(response.headers().firstValue("Last-Modified").orElse(null)))
                throw new IOException("Remote Last-Modified changed or is missing");
            return length;
        }

        static boolean terminalHttpFailure(Exception error) {
            if (error instanceof Transport.NonRetryableException || error instanceof Checksum.ConflictException ||
                    error instanceof IllegalArgumentException) return true;
            String message = str(error.getMessage());
            return message.contains("Expected HTTP 206") || message.contains("Content-Range") ||
                    message.contains("Content-Encoding") || message.contains("страницу HTML");
        }

        long[] metadata() throws Exception {
            String last = "";
            Exception terminal = null;
            boolean temporaryFailure = false;
            Set<String> tried = new HashSet<>();
            while (!stop) {
                Map<String,Object> route = null;String metadataUrl=null;
                List<?> sourceUrls=state.get("mirrors") instanceof List<?> mirrors&&!mirrors.isEmpty()?mirrors:List.of(str(state.get("url")));
                outer: for (Map<String,Object> candidate : routeReferences())for(Object raw:sourceUrls)
                    if (tried.add(routeKey(candidate)+"\n"+str(raw))) { route = candidate; metadataUrl=str(raw);break outer; }
                if (route == null) break;
                HttpAttempt attempt = new HttpAttempt(-1, -1, 0, route);
                NetworkDiagnostics.Attempt diagnostic = NetworkDiagnostics.attempt(str(state.get("id")), route,
                        metadataUrl, "Metadata", -1, 0, 0, () -> !stop && !attempt.cancelled);
                try (diagnostic) {
                    attempt.check();
                    MetadataResponse probe = metadataResponse(route, metadataUrl, attempt);
                    var response = probe.response();
                    try (Lease lease = new Lease(response.body(), openStreams)) {
                        attempt.attach(lease);
                        InputStream stream = lease.stream;
                        boolean whole = response.statusCode() == 200;
                        long total = whole ? wholeLength(response, null, null, null) :
                                Transport.validate(response, probe.offset(), probe.offset(), null, null, null);
                        if (state.containsKey("source_size") && total >= 0 && total != num(state.get("source_size"), -1))
                            throw new Checksum.ConflictException("Размер файла не совпадает с метаданными Hugging Face");
                        if (!whole) {
                            if (stream.read() < 0) throw new IOException("Неверный проверочный сегмент");
                            attempt.progress(1); lease.touch();
                            if (stream.read() >= 0) throw new IOException("Неверный проверочный сегмент");
                        }
                        attempt.check();
                        synchronized (this) {
                            String foundEtag = response.headers().firstValue("ETag").orElse("");
                            state.put("probe_etag", foundEtag.startsWith("W/") ? "" : foundEtag);
                            state.put("probe_modified", response.headers().firstValue("Last-Modified").orElse(""));
                            state.put("probe_whole", whole);
                        }
                        Checksum.Discovery discovered = Checksum.discover(route,metadataUrl, response, new Checksum.TransferObserver() {
                                    Lease current;
                                    public void opened(InputStream body) throws IOException {
                                        attempt.check(); current = new Lease(body, openStreams); attempt.attach(current);
                                    }
                                    public void received(int bytes) { attempt.progress(bytes); if (bytes > 0) current.touch(); }
                                    public void closed(InputStream body) { if (current != null) { current.close(); current = null; } }
                                }, Checksums.expected(state));
                        attempt.check();
                        Checksums.checkDiscovery(state, discovered.checksums());
                        synchronized (this) {
                            attempt.check();
                            state.put("probe_source_sha256", discovered.sha256());
                            state.put("probe_checksum_source", discovered.source());
                            state.put("probe_checksums", discovered.checksums());
                            state.put("probe_checksum_sources", discovered.sources());
                            state.put("checksum_warnings", discovered.warnings());
                            if(Set.of("ftp","ftps","sftp").contains(URI.create(str(state.get("url"))).getScheme().toLowerCase(Locale.ROOT))&&!Checksums.strong(discovered.checksums())&&!Checksums.strong(Checksums.expected(state)))state.put("probe_whole",true);
                        }
                        return new long[]{total};
                    }
                } catch (Exception error) {
                    if (stop) throw error;
                    if (attempt.cancelled) {
                        Thread.interrupted(); temporaryFailure = true;
                        last = str(route.get("name")) + ": " + (attempt.stalled ?
                                "Шлюз не передавал данные в течение 60 секунд" : "Попытка сегмента отменена");
                        if (attempt.stalled) synchronized (this) { route.put("last_error", "Шлюз не передавал данные в течение 60 секунд"); }
                        continue;
                    }
                    diagnostic.failure(error);
                    if (error instanceof InterruptedException || (error instanceof Checksum.ConflictException && sourceUrls.size()==1)) throw error;
                    last = str(route.get("name")) + ": " + error.getMessage();
                    synchronized (this) { route.put("last_error", error.getMessage()); }
                    if (terminalHttpFailure(error)) terminal = error;
                    else temporaryFailure = true;
                } finally { attempt.released(); }
            }
            String message = "Не удалось подготовить загрузку: " + last;
            if (!temporaryFailure && terminal != null) throw new Transport.NonRetryableException(message, terminal);
            throw new IOException(message);
        }

        void verifyResume() throws Exception {
            Map<String,Object> old = new LinkedHashMap<>(chunks());
            Map<String,Object> valid = new LinkedHashMap<>();
            long downloaded = 0;
            try (FileChannel channel = FileChannel.open(part(), StandardOpenOption.READ)) {
                byte[] buffer = new byte[1024 * 1024];
                for (var entry : old.entrySet()) {
                    int index;
                    try { index = Integer.parseInt(entry.getKey()); } catch (NumberFormatException error) { continue; }
                    long start = (long) index * chunkSize();
                    if (index < 0 || start >= size()) continue;
                    long length = Math.min(chunkSize(), size() - start);
                    long left = length, offset = start;
                    MessageDigest digest = sha();
                    while (left > 0) {
                        ByteBuffer slice = ByteBuffer.wrap(buffer, 0, (int) Math.min(left, buffer.length));
                        int read = channel.read(slice, offset);
                        if (read <= 0) break;
                        digest.update(buffer, 0, read);
                        offset += read; left -= read;
                    }
                    if (left == 0 && hex(digest.digest()).equals(str(entry.getValue()))) {
                        valid.put(entry.getKey(), entry.getValue()); downloaded += length;
                    }
                }
            }
            synchronized (this) { state.put("chunks", valid); state.put("downloaded", downloaded); persist(); }
        }

        /** A range has one writer until that attempt has closed both its stream and file channel. */
        final class HttpAttempt {
            final int index, worker;
            final long generation, started = System.nanoTime();
            final Thread owner = Thread.currentThread();
            volatile long received, lastProgress = started;
            volatile boolean cancelled, finished, stalled;
            final Map<String,Object> route;
            final long disabledEpoch;
            volatile Lease lease;
            volatile TransferJournal.Attempt journal;
            int rescueWorker = -1;
            boolean complete;
            HttpAttempt(int index, int worker, long generation, Map<String,Object> route) {
                this.index = index; this.worker = worker; this.generation = generation; this.route = route;
                disabledEpoch = disabledEpochs.getOrDefault(routeKey(route), 0L);
                activeAttempts.add(this);
            }
            void check() throws IOException {
                if (stop || cancelled || !selected(route, disabledEpoch)) throw new IOException("Попытка сегмента отменена");
            }
            void attach(Lease body) throws IOException {
                synchronized (this) { lease = body; }
                if (cancelled || stop) { body.close(); check(); }
            }
            void progress(int bytes) { if (bytes > 0) { received += bytes; lastProgress = System.nanoTime(); } }
            void cancel() {
                Lease body;
                synchronized (this) {
                    // A delayed cancellation must not interrupt this thread's next assignment.
                    if (finished) return;
                    cancelled = true; owner.interrupt(); body = lease;
                }
                if (body != null) body.close();
            }
            synchronized void released() { finished = true; lease = null; activeAttempts.remove(this); }
        }

        /** Runtime ownership never changes the persisted chunk size or verified chunk identities. */
        final class HttpQueue {
            final Deque<Integer> pending = new ArrayDeque<>();
            final Map<Integer,HttpAttempt> attempts = new LinkedHashMap<>();
            final Map<Integer,Set<String>> handoffOwners = new HashMap<>();
            final long[] completedBytes, completedNanos;
            final Set<Integer> activeWorkers = new HashSet<>();
            final Map<Integer,String> avoidWorkers = new HashMap<>();
            final Map<Integer,Map<String,Object>> workerRoutes = new HashMap<>();
            long generation;
            HttpQueue(int count, int workers) {
                if(workers<1||workers>32)throw new IllegalArgumentException("Invalid HTTP worker capacity");
                completedBytes = new long[32]; completedNanos = new long[32];
                for (int index = 0; index < count; index++)
                    if (!chunks().containsKey(Integer.toString(index))) pending.addLast(index);
            }
            private HttpAttempt assign(int index, int worker) {
                HttpAttempt attempt = new HttpAttempt(index, worker, ++generation, workerRoutes.get(worker));
                attempts.put(index, attempt);
                Set<String> owners = handoffOwners.get(index);
                if (owners != null) owners.add(routeKey(workerRoutes.get(worker)));
                return attempt;
            }
            synchronized void register(int worker, Map<String,Object> route) {
                Map<String,Object> previous = workerRoutes.get(worker);
                if (previous != null && !routeKey(previous).equals(routeKey(route))) {
                    completedBytes[worker] = 0; completedNanos[worker] = 0;
                }
                activeWorkers.add(worker); workerRoutes.put(worker, route); notifyAll();
            }
            synchronized void unregister(int worker, Map<String,Object> route) {
                if (workerRoutes.get(worker) == route) activeWorkers.remove(worker);
                notifyAll();
            }
            synchronized boolean finished() { return pending.isEmpty() && attempts.isEmpty(); }
            synchronized HttpAttempt next(int worker) throws InterruptedException {
                while (!stop && activeWorkers.contains(worker)) {
                    for (Iterator<Integer> it = pending.iterator(); it.hasNext();) {
                        int index = it.next();
                        String avoid = avoidWorkers.get(index);
                        if (avoid != null && avoid.equals(routeKey(workerRoutes.get(worker))) && activeWorkers.size() > 1) continue;
                        it.remove(); avoidWorkers.remove(index); return assign(index, worker);
                    }
                    if (attempts.isEmpty() && pending.isEmpty()) return null;
                    long now = System.nanoTime();
                    double ownRate = completedNanos[worker] == 0 ? 0 :
                            completedBytes[worker] * 1_000_000_000.0 / completedNanos[worker];
                    HttpAttempt candidate = null;
                    double worstSeconds = 0;
                    for (HttpAttempt attempt : attempts.values()) {
                        if (attempt.cancelled || attempt.finished || attempt.worker == worker) continue;
                        Set<String> owners = handoffOwners.get(attempt.index);
                        if (owners != null && owners.contains(routeKey(workerRoutes.get(worker)))) continue;
                        long age = now - attempt.started;
                        if (age < TimeUnit.SECONDS.toNanos(5)) continue;
                        long length = Math.min(chunkSize(), size() - (long)attempt.index * chunkSize());
                        long remaining = Math.max(0, length - attempt.received);
                        // Silence is reclaimed only by the independent sixty-second watchdog.
                        boolean progressing = attempt.received > 0 && now - attempt.lastProgress < TimeUnit.SECONDS.toNanos(5);
                        double rate = attempt.received * 1_000_000_000.0 / Math.max(1, age);
                        double seconds = rate <= 0 ? Double.POSITIVE_INFINITY : remaining / rate;
                        // Only a worker with real successful measurements judges a still-active slow range.
                        boolean muchSlower = ownRate > 0 && remaining > 0 && ownRate > rate * 2 &&
                                seconds > Math.max(8, length / ownRate + 4);
                        if (progressing && muchSlower && (candidate == null || seconds > worstSeconds)) {
                            candidate = attempt; worstSeconds = seconds;
                        }
                    }
                    if (candidate != null) {
                        Set<String> owners = handoffOwners.computeIfAbsent(candidate.index, ignored -> new HashSet<>());
                        owners.add(routeKey(candidate.route)); owners.add(routeKey(workerRoutes.get(worker)));
                        candidate.cancelled = true; candidate.rescueWorker = worker;
                        return candidate;
                    }
                    wait(200);
                }
                return null;
            }
            synchronized HttpAttempt takeOver(HttpAttempt previous, int worker) throws InterruptedException {
                while (!stop && activeWorkers.contains(worker) && !previous.finished) wait(200);
                if (stop || !activeWorkers.contains(worker) || previous.complete || !previous.finished) return null;
                // A range can be assigned only after the old writer released its stream and disk channel.
                // A different worker may already have taken it from the shared pool.
                if (!pending.remove(previous.index)) return null;
                avoidWorkers.remove(previous.index); return assign(previous.index, worker);
            }
            synchronized void release(HttpAttempt attempt, boolean complete) {
                if (attempts.get(attempt.index) != attempt)
                    throw new IllegalStateException("Stale segment attempt cannot release another writer");
                attempt.complete = complete;
                attempt.released();
                if (complete) {
                    completedBytes[attempt.worker] += Math.min(chunkSize(), size() - (long)attempt.index * chunkSize());
                    completedNanos[attempt.worker] += Math.max(1, System.nanoTime() - attempt.started);
                    attempts.remove(attempt.index); handoffOwners.remove(attempt.index);
                } else {
                    attempts.remove(attempt.index);
                    if (attempt.cancelled) avoidWorkers.put(attempt.index, routeKey(attempt.route));
                    pending.addFirst(attempt.index);
                }
                notifyAll();
            }
        }

        String downloadChunk(Map<String,Object> route, HttpAttempt attempt) throws Exception {
            int index = attempt.index;
            attempt.check();
            long start = (long) index * chunkSize();
            long end = Math.min(size() - 1, start + chunkSize() - 1);
            String etag = str(state.get("etag"));
            String modified = str(state.get("modified"));
            var response = openRange(route,attempt,start,end,etag,modified);
            MessageDigest digest = sha();
            long left = end - start + 1, offset = start;
            byte[] buffer = new byte[1024 * 1024];
            try (Lease lease = new Lease(response.body(), openStreams); FileChannel output = FileChannel.open(part(), StandardOpenOption.WRITE)) {
                attempt.attach(lease);
                InputStream input = lease.stream;
                boolean wholePrefix = response.statusCode()==200 && start==0;
                if (wholePrefix) {
                    wholeLength(response, size(), etag, modified);
                } else Transport.validate(response, start, end, size(), etag, modified);
                while (left > 0) {
                    attempt.check();
                    int permitted = (int)Math.min(buffer.length,left);
                    if(transferLimits != null) permitted=transferLimits.acquire(str(route.get("gateway")),permitted,()->stop||attempt.cancelled,()->{lease.touch();attempt.lastProgress=System.nanoTime();});
                    attempt.lastProgress=System.nanoTime();
                    int got = input.read(buffer, 0, permitted);
                    if (got < 0) throw new EOFException("Соединение оборвалось до конца сегмента");
                    attempt.progress(got);
                    synchronized (this) { metrics.receive(route, index, got); }
                    if (got > 0) lease.touch();
                    attempt.check();
                    ByteBuffer slice = ByteBuffer.wrap(buffer, 0, got);
                    while (slice.hasRemaining()) offset += output.write(slice, offset);
                    digest.update(buffer, 0, got);
                    left -= got;
                    if(attempt.journal!=null)attempt.journal.progress(attempt.received,savedBytes()+attempt.received,wireBytes());
                }
                // Some CDNs ignore every zero-start Range. Its validated full body may supply only
                // the bounded first segment; closing it does not make another range overlap.
                if (!wholePrefix && input.read() >= 0) throw new IOException("Сервер передал лишние байты сегмента");
                attempt.check();
                output.force(true);
            }
            attempt.check();
            return hex(digest.digest());
        }

        java.net.http.HttpResponse<InputStream> openRange(Map<String,Object> route,HttpAttempt attempt,long start,long end,String etag,String modified)throws Exception {
            List<?> urls=state.get("mirrors") instanceof List<?> mirrors&&!mirrors.isEmpty()?mirrors:List.of(str(state.get("url")));
            int first=(int)Math.floorMod(attempt.worker+attempt.generation,urls.size());Exception last=null;
            for(int index=0;index<urls.size();index++) {
                attempt.check();String url=str(urls.get((first+index)%urls.size()));java.net.http.HttpResponse<InputStream> response=null;
                NetworkDiagnostics.Attempt diagnostic=NetworkDiagnostics.attempt(str(state.get("id")),route,url,
                        "Segment",attempt.index,start,end,()->!stop&&!attempt.cancelled);
                try(diagnostic){response=httpOpener.open(route,url,start,end,etag,modified);
                    if(start==0&&response.statusCode()==200)wholeLength(response,size(),etag,modified);
                    else if(start>0&&response.statusCode()==200&&yes(state.get("distribution_supported"))) {
                        wholeLength(response,size(),etag,modified);
                        throw new IgnoredRangeException();
                    }
                    else Transport.validate(response,start,end,size(),etag,modified);
                    synchronized(this){route.put("source_url_active",url);}return response;}
                catch(Exception error){if(response!=null)try{response.body().close();}catch(IOException close){error.addSuppressed(close);}diagnostic.failure(error);last=error;if(stop||attempt.cancelled||error instanceof InterruptedException)throw error;}
            }
            throw Objects.requireNonNull(last);
        }

        final class HttpWorker {
            final Map<String,Object> route;
            final HttpQueue queue;
            final int workerId;
            final long epoch, revision;
            final AtomicReference<String> permanentFailure;
            volatile boolean retired, failed;
            final Thread worker;
            HttpWorker(Map<String,Object> route, HttpQueue queue, int workerId, AtomicReference<String> permanentFailure) {
                this.route = route; this.queue = queue; this.workerId = workerId; this.permanentFailure = permanentFailure;
                synchronized (Job.this) { epoch = disabledEpochs.getOrDefault(routeKey(route), 0L); revision = routesRevision; }
                worker = new Thread(this::run, "route-" + route.get("name")); worker.setDaemon(true);
            }
            boolean selected() { return !retired && Job.this.selected(route, epoch); }
            void retire() {
                if (retired) return;
                retired = true; queue.unregister(workerId, route); worker.interrupt();
                for (HttpAttempt attempt : activeAttempts) if (attempt.owner == worker) attempt.cancel();
            }
            void run() {
                int errors = 0;
                try {
                    while (!stop && selected()) {
                        HttpAttempt attempt;
                        try {
                            attempt = queue.next(workerId);
                            if (attempt != null && attempt.owner != Thread.currentThread()) {
                                attempt.cancel(); attempt = queue.takeOver(attempt, workerId);
                                if (attempt == null) continue;
                            }
                        } catch (InterruptedException cancelled) { return; }
                        if (attempt == null) return;
                        boolean complete = false; Exception failure = null;
                        int index = attempt.index;
                        long start=(long)index*chunkSize(),end=Math.min(size()-1,start+chunkSize()-1);
                        attempt.journal=TransferJournal.begin(str(state.get("id")),str(route.get("gateway")),start,end,savedBytes(),wireBytes(),true);
                        HttpAttempt diagnosticAttempt=attempt;
                        NetworkDiagnostics.Attempt diagnostic=NetworkDiagnostics.attempt(str(state.get("id")),route,
                                str(state.get("url")),"Segment",index,start,end,()->!stop&&!diagnosticAttempt.cancelled&&!retired);
                        try (diagnostic) {
                            String digest = downloadChunk(route, attempt); attempt.check();
                            long length = Math.min(chunkSize(), size() - (long) index * chunkSize());
                            synchronized (Job.this) {
                                attempt.check();
                                if (!chunks().containsKey(Integer.toString(index))) {
                                    chunks().put(Integer.toString(index), digest);
                                    state.put("downloaded", num(state.get("downloaded"), 0) + length);
                                    route.put("bytes", num(route.get("bytes"), 0) + length);
                                    route.put("chunks", num(route.get("chunks"), 0) + 1);
                                }
                                complete = true; route.put("last_error", "");
                                state.put("error", ""); state.put("status", "running");
                            }
                            errors = 0;
                        } catch (Exception error) { failure = error; }
                        finally {
                            // downloadChunk's try-with-resources has closed its stream and FileChannel.
                            if (!complete && failure != null && System.nanoTime() - attempt.lastProgress >= idleTimeoutNanos) {
                                // A request-header timeout can race the periodic watchdog by a few milliseconds.
                                attempt.stalled = true; attempt.cancelled = true;
                            }
                            synchronized (Job.this) { metrics.endChunk(index); }
                            queue.release(attempt, complete);
                            attempt.journal.finish(complete?"completed":stop?"paused":attempt.cancelled?"retry":"failed",
                                    failure==null?"":str(failure.getMessage()),attempt.received,savedBytes(),wireBytes());
                            if (attempt.cancelled && !stop) Thread.interrupted();
                        }
                        if (attempt.stalled) {
                            synchronized (Job.this) { route.put("last_error", "Шлюз не передавал данные в течение 60 секунд"); }
                            AppLog.info("Сегмент %s возвращён в общий пул после 60 секунд без данных через шлюз %s", index, str(route.get("gateway")));
                        }
                        if (!selected()) return;
                        if (failure != null && !attempt.cancelled && !stop) {
                            diagnostic.failure(NetworkDiagnostics.uri(str(route.getOrDefault("source_url_active",state.get("url")))),"Segment",failure);
                            synchronized (Job.this) {
                                if (stop || !selected()) return;
                                route.put("last_error", failure.getMessage());
                                state.put("error", str(route.get("name")) + ": " + str(failure.getMessage()));
                                state.put("status", "retrying"); persist();
                            }
                            if (terminalHttpFailure(failure)) {
                                failed = true; permanentFailure.set(str(route.get("name")) + ": " + str(failure.getMessage())); return;
                            }
                            errors++;
                            try { Thread.sleep(Math.min(30000, (1L << Math.min(errors, 5)) * 1000)); }
                            catch (InterruptedException cancelled) { return; }
                        } else if (attempt.cancelled && !stop) {
                            try { Thread.sleep(200); } catch (InterruptedException cancelled) { return; }
                        }
                    }
                } finally { queue.unregister(workerId, route); }
            }
        }

        void download() throws Exception {
            int count = Math.toIntExact((size() + chunkSize() - 1) / chunkSize());
            HttpQueue queue = new HttpQueue(count, 32); httpQueue = queue;
            AtomicReference<String> permanentFailure = new AtomicReference<>("");
            List<Thread> launched = new ArrayList<>();
            try {
                while (!stop) {
                    for (HttpWorker worker : httpWorkers.values()) if (!worker.selected()) worker.retire();
                    synchronized (this) {
                        for (Map<String,Object> route : routeReferences()) {
                            String key = routeKey(route); HttpWorker prior = httpWorkers.get(key);
                            if (prior != null && (prior.worker.isAlive() || (prior.failed && prior.revision == routesRevision))) continue;
                            if (queue.finished()) break;
                            BitSet used = new BitSet(4);
                            for (HttpWorker existing : httpWorkers.values())
                                if (existing.worker.isAlive()) used.set(existing.workerId);
                            int workerId = used.nextClearBit(0);
                            if (workerId >= 32) continue; // A retired writer must release its slot before it is reused.
                            HttpWorker worker = new HttpWorker(route, queue, workerId, permanentFailure);
                            queue.register(workerId, route); httpWorkers.put(key, worker);
                            routeThreads.add(worker.worker); launched.add(worker.worker); worker.worker.start();
                        }
                    }
                    if (queue.finished()) break;
                    if (httpWorkers.values().stream().noneMatch(worker -> worker.worker.isAlive()))
                        throw new IOException(permanentFailure.get().isEmpty() ? "Не все сегменты загружены" :
                                "Не удалось загрузить сегменты: " + permanentFailure.get());
                    synchronized (queue) { queue.wait(100); }
                }
            } finally {
                for (HttpWorker worker : httpWorkers.values()) worker.retire();
                for (Thread worker : launched) {
                    boolean interrupted = false;
                    while (worker.isAlive()) try { worker.join(200); } catch (InterruptedException cancelled) { interrupted = true; }
                    if (interrupted) Thread.currentThread().interrupt();
                }
                routeThreads.removeIf(worker -> !worker.isAlive()); httpWorkers.clear(); httpQueue = null;
            }
            if (stop) return;
            if (chunks().size() != count) throw new IOException("Не все сегменты загружены");
        }

        void finish() throws Exception {
            Metalink.verifyPieces(part(),state);
            if (Files.size(part()) != size()) throw new IOException("Неверный итоговый размер");
            Map<String,String> actual = Checksums.file(part());
            synchronized (this) { state.putAll(actual); persist(); }
            Checksums.verify(actual, Checksums.expected(state));
            Path finalPath = Path.of(str(state.get("directory"))).resolve(str(state.get("filename")));
            safePath(finalPath);
            synchronized (this) {
                if (stop) return;
                if (Files.exists(finalPath)) {
                    if (Files.size(finalPath) != size() || !Checksums.file(finalPath).equals(actual))
                        throw new FileAlreadyExistsException("Целевой файл уже существует и отличается; .part сохранён");
                } else {
                    try { Files.createLink(finalPath, part()); }
                    catch (UnsupportedOperationException | FileSystemException error) {
                        if (Files.exists(finalPath)) throw new FileAlreadyExistsException("Целевой файл уже существует; .part сохранён");
                        try { Files.move(part(), finalPath, StandardCopyOption.ATOMIC_MOVE); }
                        catch (AtomicMoveNotSupportedException unavailable) { Files.move(part(), finalPath); }
                    }
                }
                state.put("status", "complete"); state.put("error", ""); DownloadHistory.complete(state); persist();
            }
            try { Files.deleteIfExists(part()); } catch (IOException ignored) { }
        }

        /** Whole-file transfers preserve their prefix on every error, pause and application restart. */
        void streamWhole(long declared) throws Exception {
            Path directory=Path.of(str(state.get("directory")));safePath(directory);safePath(part());Files.createDirectories(directory);
            if(Files.exists(part(),LinkOption.NOFOLLOW_LINKS)&&!Files.isRegularFile(part(),LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid partial file type");
            long preserved=Files.exists(part())?Files.size(part()):0;
            if(preserved>0) {
                if(declared>=0&&preserved==declared&&savedBytes()==preserved&&
                        (!state.containsKey("whole_prefix_bytes")||num(state.get("whole_prefix_bytes"),-1)==declared)) {
                    synchronized(this){state.put("size",declared);state.put("downloaded",preserved);state.put("chunks",new LinkedHashMap<>(Map.of("0",hashFile(part()))));persist();}
                    return;
                }
                String reason="Partial file preserved: the source does not support safe resuming. Explicitly restart the download to replace it.";
                TransferJournal.preserved(str(state.get("id")),"",preserved,wireBytes(),reason);
                throw new PartialPreservedException(reason);
            }
            while(!stop) {
                for(Map<String,Object> route:routeReferences()) {
                    HttpAttempt attempt=new HttpAttempt(0,0,System.nanoTime(),route);
                    long received=0;
                    TransferJournal.Attempt journal=TransferJournal.begin(str(state.get("id")),str(route.get("gateway")),0,declared<0?-1:declared-1,0,wireBytes(),false);
                    String outcome="interrupted",reason="";
                    try {
                        attempt.check();var response=SourceTransfer.openWhole(route,str(state.get("url")));
                        try(Lease lease=new Lease(response.body(),openStreams)) {
                            attempt.attach(lease);
                            // Headers and source identity must be accepted before opening the saved file for writing.
                            long actualLength=wholeLength(response,declared<0?null:declared,str(state.get("etag")),str(state.get("modified")));
                            if(declared>=0)requireSpace(directory,declared);
                            if(Files.exists(part())&&Files.size(part())!=0)throw new PartialPreservedException("Partial file preserved: the source does not support safe resuming. Explicitly restart the download to replace it.");
                            try(FileChannel output=FileChannel.open(part(),StandardOpenOption.CREATE,StandardOpenOption.WRITE)) {
                            long spaceCheck=0;byte[] buffer=new byte[32768];MessageDigest digest=sha();
                            synchronized(this){state.put("downloaded",0L);state.put("status","running");state.put("error","");persist();}
                            while(!stop) {
                                attempt.check();int allowed=buffer.length;
                                if(transferLimits!=null)allowed=transferLimits.acquire(str(route.get("gateway")),allowed,()->stop||attempt.cancelled,()->{lease.touch();attempt.lastProgress=System.nanoTime();});
                                attempt.lastProgress=System.nanoTime();int read=lease.stream.read(buffer,0,allowed);if(read<0)break;
                                attempt.progress(read);lease.touch();if(read==0)continue;
                                synchronized(this){metrics.receive(route,0,read);metrics.endChunk(0);}
                                if(actualLength>=0&&received+read>actualLength)throw new IOException("Source sent more bytes than declared");
                                if(received>=spaceCheck){requireSpace(directory,Math.max(8L*1024*1024,read));spaceCheck=received+8L*1024*1024;}
                                ByteBuffer bytes=ByteBuffer.wrap(buffer,0,read);while(bytes.hasRemaining())output.write(bytes);digest.update(buffer,0,read);received+=read;
                                synchronized(this){state.put("downloaded",received);}
                                journal.progress(attempt.received,received,wireBytes());
                            }
                            attempt.check();if(actualLength>=0&&received!=actualLength)throw new EOFException("Incomplete whole-file transfer");
                            if(state.containsKey("source_size")&&received!=num(state.get("source_size"),-1))throw new Checksum.ConflictException("File size differs from source metadata");
                            output.force(true);synchronized(this){state.put("size",received);state.put("chunk_size",Math.max(1,received));state.put("chunks",received==0?new LinkedHashMap<>():new LinkedHashMap<>(Map.of("0",hex(digest.digest()))));state.put("downloaded",received);route.put("bytes",num(route.get("bytes"),0)+received);route.put("chunks",num(route.get("chunks"),0)+1);metrics.endChunk(0);persist();}outcome="completed";return;
                            }
                        }
                    }catch(Exception failure){reason=str(failure.getMessage());outcome=stop?"paused":"failed";
                        synchronized(this){route.put("last_error",reason);state.put("downloaded",Math.max(received,Files.exists(part())?Files.size(part()):0));persist();}
                        if(stop)throw failure;
                        if(received>0||(Files.exists(part())&&Files.size(part())>0)) {
                            TransferJournal.preserved(str(state.get("id")),str(route.get("gateway")),Files.size(part()),wireBytes(),reason);
                            throw new PartialPreservedException("Partial file preserved after a transfer error; retrying will check resume support before writing.");
                        }
                        if(attempt.cancelled){Thread.interrupted();continue;}if(terminalHttpFailure(failure))throw failure;
                        synchronized(this){state.put("status","retrying");state.put("error",reason);persist();}
                    }
                    finally{attempt.released();journal.finish(outcome,reason,attempt.received,savedBytes(),wireBytes());}
                }
                for(int delay=0;delay<50&&!stop;delay++)Thread.sleep(100);
            }
        }

        synchronized long wireBytes() { metrics.checkpoint(state); return num(state.get("received_bytes"),0); }
        synchronized long savedBytes() { return num(state.get("downloaded"),0); }

        /** Reuse only complete saved prefix ranges; final publisher checksums still verify the assembled file. */
        void recoverWholePrefix(long total) throws Exception {
            if(!Files.exists(part(),LinkOption.NOFOLLOW_LINKS))return;
            safePath(part());if(!Files.isRegularFile(part(),LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid partial file type");
            long actual=Files.size(part()),prefix=num(state.get("whole_prefix_bytes"),actual);
            if(prefix<0||prefix>actual||prefix>total||actual>total)
                throw new PartialPreservedException("Partial file preserved: source size or identity changed.");
            long segment=num(state.get("segment_size"),20L*1024*1024);
            if(segment<1||segment>MAX_CHUNK_MB*1024L*1024)segment=20L*1024*1024;
            synchronized(this){state.put("whole_prefix_bytes",prefix);persist();}
            Map<String,Object> recovered=new LinkedHashMap<>();long completeBytes=0;
            try(FileChannel input=FileChannel.open(part(),StandardOpenOption.READ)) {
                byte[] buffer=new byte[1024*1024];
                for(long start=0;start<total;start+=segment) {
                    long length=Math.min(segment,total-start);if(start+length>prefix)break;
                    MessageDigest digest=sha();long offset=start,left=length;
                    while(left>0){if(stop)throw new InterruptedException();int read=input.read(ByteBuffer.wrap(buffer,0,(int)Math.min(buffer.length,left)),offset);if(read<=0)throw new EOFException("Saved partial file changed during recovery");digest.update(buffer,0,read);offset+=read;left-=read;}
                    recovered.put(Long.toString(start/segment),hex(digest.digest()));completeBytes+=length;
                }
            }
            if(actual<total){requireSpace(part().getParent(),total-actual);try(FileChannel output=FileChannel.open(part(),StandardOpenOption.WRITE)){if(total>0)output.write(ByteBuffer.wrap(new byte[]{0}),total-1);output.force(true);}}
            synchronized(this){state.put("chunk_size",segment);state.put("segment_size",segment);state.put("chunks",recovered);state.put("downloaded",completeBytes);persist();}
            TransferJournal.preserved(str(state.get("id")),"",prefix,wireBytes(),"Saved partial data recovered for segmented downloading");
        }

        String hashFile(Path path) throws Exception {
            return Checksums.file(path).get("sha256");
        }

        boolean recoverPublished() throws Exception {
            Path finalPath = Path.of(str(state.get("directory"))).resolve(str(state.get("filename")));
            safePath(finalPath);
            String savedHash = str(state.get("sha256"));
            if (savedHash.isEmpty() || size() < 0 || !Files.exists(finalPath)) return false;
            Map<String,String> actual = Checksums.file(finalPath);
            if (Files.size(finalPath) != size() || !actual.get("sha256").equals(savedHash))
                throw new IOException("Итоговый файл существует, но не совпадает с сохранённым SHA-256");
            Checksums.verify(actual, Checksums.values(state));
            Checksums.verify(actual, Checksums.expected(state));
            synchronized (this) {
                if (stop) return true;
                state.putAll(actual); state.put("status", "complete"); state.put("error", ""); DownloadHistory.recovered(state); persist();
            }
            try { Files.deleteIfExists(part()); } catch (IOException ignored) { }
            return true;
        }

        void run() {
            try {
                if ("torrent".equals(state.get("kind"))) {
                    Map<String,Object> input;
                    synchronized (this) {
                        if (stop) return;
                        DownloadHistory.begin(state); persist();
                        input = obj(Json.parse(Json.stringify(state)));
                    }
                    Map<String,Object> result = TorrentEngine.runControlled(input,
                            manifest.getParent().resolve("torrents").resolve(str(state.get("id"))), () -> stop,
                            this::torrentUpdate, this::enabledRoutesSnapshot, this::optionsSnapshot, transferLimits);
                    torrentUpdate(result);
                    return;
                }
                if (recoverPublished()) return;
                synchronized (this) {
                    if (stop) return;
                    DownloadHistory.begin(state); persist();
                }
                long total = 0;
                while (!stop) {
                    try { total = metadata()[0]; break; }
                    catch (Exception error) {
                        if (terminalHttpFailure(error) || error instanceof InterruptedException) throw error;
                        synchronized (this) {
                            if (stop) return;
                            state.put("status", "retrying"); state.put("error", error.getMessage()); persist();
                        }
                        for (int i = 0; i < 150 && !stop; i++) Thread.sleep(100);
                    }
                }
                if (stop) return;
                String etag = str(state.remove("probe_etag"));
                String modified = str(state.remove("probe_modified"));
                String discoveredSha = str(state.remove("probe_source_sha256"));
                String discoveredSource = str(state.remove("probe_checksum_source"));
                Object rawDiscovered = state.remove("probe_checksums");
                Map<String,String> discovered = Checksums.values(rawDiscovered instanceof Map<?,?> map ? map : Map.of());
                Map<String,String> sources = new LinkedHashMap<>();
                Object rawSources = state.remove("probe_checksum_sources");
                if(rawSources instanceof Map<?,?> map)for(var entry:map.entrySet())sources.put(str(entry.getKey()),str(entry.getValue()));
                if(discovered.isEmpty()&&!discoveredSha.isEmpty()){discovered=Map.of("sha256",discoveredSha);sources.put("sha256",discoveredSource);}
                boolean whole = yes(state.remove("probe_whole"));
                boolean previousWhole = yes(state.get("whole_file"));
                String expectedSha = str(state.get("expected_sha256"));
                if(state.get("mirrors") instanceof List<?> mirrors&&mirrors.size()>1&&!expectedSha.isEmpty()){etag="";modified="";}
                Checksums.checkDiscovery(state, discovered);
                Map<String,String> expectedHashes = new LinkedHashMap<>(Checksums.expected(state)); expectedHashes.putAll(discovered);
                expectedSha = expectedHashes.getOrDefault("sha256", "");
                if (!whole && etag.isEmpty() && !Checksums.strong(expectedHashes))
                    throw new IOException("Источник не дал сильный ETag; для безопасной докачки укажите ожидаемую SHA-256 или SHA-512");
                if(whole&&!previousWhole&&Files.exists(part(),LinkOption.NOFOLLOW_LINKS)&&Files.size(part())>0)
                    throw new PartialPreservedException("Partial file preserved: the source does not support safe resuming. Explicitly restart the download to replace it.");
                synchronized(this) {
                    if(size()!=0 && (size()!=total || (!Checksums.strong(expectedHashes) &&
                            (!str(state.get("etag")).equals(etag)||!str(state.get("modified")).equals(modified))))) {
                        if(previousWhole)throw new PartialPreservedException("Partial file preserved: source size or identity changed.");
                        throw new IOException("Исходный файл изменился; прежние сегменты сохранены");
                    }
                }
                if(!whole && previousWhole)recoverWholePrefix(total);
                synchronized (this) {
                    if (size() != 0 && (size() != total || (!Checksums.strong(expectedHashes) &&
                            (!str(state.get("etag")).equals(etag) || !str(state.get("modified")).equals(modified)))))
                        throw new IOException("Исходный файл изменился; прежние сегменты сохранены");
                    state.put("size", total); state.put("etag", etag); state.put("modified", modified);
                    state.put("whole_file", whole); state.put("resumable", !whole);
                    state.put("distribution_supported", !whole);
                    if(whole && !previousWhole && num(state.get("downloaded"),0)==0) {
                        state.put("segment_size",chunkSize());state.put("chunks",new LinkedHashMap<>());
                    }
                    if (whole) state.put("chunk_size", Math.max(1, total));
                    else state.remove("whole_prefix_bytes");
                    state.remove("restart_required");
                    Checksums.acceptDiscovery(state, discovered, sources);
                    persist();
                }
                Path directory = Path.of(str(state.get("directory")));
                if(whole) {
                    streamWhole(total);if(stop)return;
                    synchronized(this){metrics.stop();METRIC_JOBS.remove(this);state.put("status","verifying");persist();}
                    finish();return;
                }
                safePath(directory); safePath(part());
                Files.createDirectories(directory);
                if (!Files.exists(part(), LinkOption.NOFOLLOW_LINKS)) {
                    requireSpace(directory, total);
                    try (FileChannel channel = FileChannel.open(part(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        if (total > 0) channel.write(ByteBuffer.wrap(new byte[]{0}), total - 1);
                        channel.force(true);
                    }
                } else if (!Files.isRegularFile(part(), LinkOption.NOFOLLOW_LINKS) || Files.size(part()) != total)
                    throw new IOException("Размер или тип существующего .part не совпадает с источником");
                verifyResume();
                download();
                if (stop) return;
                synchronized (this) {
                    if (stop) return;
                    metrics.stop(); METRIC_JOBS.remove(this); state.put("status", "verifying"); persist();
                }
                finish();
            } catch (Exception error) {
                synchronized (this) {
                    if (!stop) {
                        metrics.stop(); METRIC_JOBS.remove(this);
                        if(error instanceof PartialPreservedException) {
                            DownloadHistory.pause(state,"partial_preserved");state.put("status","paused");state.put("restart_required",true);
                        } else { DownloadHistory.failed(state);state.put("status", "failed"); }
                        state.put("error", error.getMessage()); persist();
                    }
                }
            } finally {
                synchronized (this) {
                    metrics.stop(); METRIC_JOBS.remove(this);
                    try { persist(); }
                    catch (RuntimeException saveError) {
                        state.put("error", "Не удалось сохранить остановленное задание: " + str(saveError.getMessage()));
                        AppLog.error("Ошибка загрузки %s: %s", str(state.get("filename")), DiagnosticText.arg(str(state.get("error"))));
                    }
                    thread = null;
                    if (restart && !"complete".equals(state.get("status"))) try { launch(); }
                    catch (RuntimeException launchError) { AppLog.error("Не удалось перезапустить задание: %s", DiagnosticText.arg(launchError.getMessage())); }
                }
            }
        }
        synchronized Map<String,Object> optionsSnapshot() {
            Map<String,Object> value=new LinkedHashMap<>();
            for(String field:List.of("file_priorities","seeding_enabled","seeding_minutes","seeding_ratio"))if(state.containsKey(field))value.put(field,state.get(field));
            return obj(Json.parse(Json.stringify(value)));
        }
        synchronized void torrentUpdate(Map<String,Object> delta) {
            Map<String,Object> update = new LinkedHashMap<>(delta);
            if (delta.get("routes") instanceof List<?>) {
                Map<String,Map<String,Object>> current = new LinkedHashMap<>();
                for (Object raw : arr(state.get("routes"))) current.put(routeKey(obj(raw)), obj(raw));
                for (Object raw : arr(delta.get("routes"))) {
                    Map<String,Object> reported = obj(raw), target = current.get(routeKey(reported));
                    if (target == null) continue; // An old callback cannot re-add a deselected or unknown gateway.
                    for (String field : List.of("bytes", "chunks", "received_bytes", "stall_releases", "last_error", "active", "stalled", "peers", "seeds", "tracker_status", "uploaded_bytes", "upload_speed_bps"))
                        if (reported.containsKey(field)) {
                            Object value = reported.get(field);
                            if (List.of("bytes", "chunks", "received_bytes", "stall_releases", "uploaded_bytes").contains(field))
                                value = Math.max(num(target.get(field), 0), num(value, 0));
                            target.put(field, value);
                        }
                    if (!yes(target.get("enabled"))) { target.put("active", false); target.put("stalled", false); }
                }
                metrics.observeRoutes(new ArrayList<Object>(current.values()));
                update.remove("routes");
            }
            if (stop) { persist(); return; }
            state.putAll(update);
            if (List.of("seeding", "complete").contains(str(delta.get("status")))) DownloadHistory.complete(state);
            else if ("failed".equals(delta.get("status"))) DownloadHistory.failed(state);
            if (List.of("paused", "verifying", "complete", "failed").contains(str(delta.get("status")))) {
                metrics.stop(); METRIC_JOBS.remove(this);
            }
            persist();
        }
    }

    static final class Manager {
        @FunctionalInterface interface SourceInspector {
            Map<String,Object> inspect(String url, Map<String,Object> route) throws Exception;
        }
        final Path dataDir;
        final SourceInspector sourceInspector;
        @FunctionalInterface interface RouteChecker { void check(Map<String,Object> route, boolean torrent) throws Exception; }
        final RouteChecker routeChecker;
        @FunctionalInterface interface SpaceProbe { long usable(Path existingDirectory) throws IOException; }
        final SpaceProbe spaceProbe;
        volatile boolean closing;
        final ConcurrentMap<String,Job> jobs = new ConcurrentHashMap<>();
        final QueueController queue;
        final UploadManager uploads;
        final BrowserIntegration browser;
        volatile java.util.function.Consumer<String> browserDownload;
        final APIEvents events = new APIEvents();
        final ScheduledExecutorService queueTimer=Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"job-queue");t.setDaemon(true);return t;});
        final Map<String,Object> idempotency = new LinkedHashMap<>();
        final Path idempotencyFile;
        Manager(Path dataDir) throws IOException {
            this(dataDir, HubSource::inspect);
        }
        /** Package-only seam for tests with owned repository metadata and HTTP data. */
        Manager(Path dataDir, SourceInspector sourceInspector) throws IOException {
            this(dataDir, sourceInspector, path -> Files.getFileStore(path).getUsableSpace());
        }
        Manager(Path dataDir, SourceInspector sourceInspector, SpaceProbe spaceProbe) throws IOException {
            this(dataDir, sourceInspector, spaceProbe, (route, torrent) -> {
                if (torrent) GatewayNetwork.socksEndpoint(route); else GatewayNetwork.endpoint(route);
            });
        }
        /** Package-only route validation seam for local synthetic download tests. */
        Manager(Path dataDir, SourceInspector sourceInspector, SpaceProbe spaceProbe, RouteChecker checker) throws IOException {
            this.routeChecker = Objects.requireNonNull(checker);
            this.sourceInspector = Objects.requireNonNull(sourceInspector);
            this.spaceProbe = Objects.requireNonNull(spaceProbe);
            this.dataDir = normalized(dataDir); Files.createDirectories(this.dataDir);
            SiteProfiles.configure(this.dataDir);
            uploads=new UploadManager(this.dataDir);
            browser=new BrowserIntegration(this.dataDir);
            queue=new QueueController(this.dataDir);idempotencyFile=this.dataDir.resolve("idempotency.config");
            if(Files.isRegularFile(idempotencyFile,LinkOption.NOFOLLOW_LINKS)){safePath(idempotencyFile);if(Files.size(idempotencyFile)>2*1024*1024)throw new IOException("Idempotency registry is too large");idempotency.putAll(obj(Json.parse(Files.readString(idempotencyFile, StandardCharsets.UTF_8))));}
            recoverInterruptedRemovals();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dataDir, "*.json")) {
                for (Path path : files) try {
                    Map<String,Object> state = obj(Json.parse(Files.readString(path, StandardCharsets.UTF_8)));
                    String id = str(state.get("id"));
                    if (!id.matches("[a-f0-9]{12}") || !path.getFileName().toString().equals(id + ".json") || Files.isSymbolicLink(path))
                        throw new IOException("Некорректный файл задания");
                    Job job = ownedJob(state);
                    jobs.put(str(state.get("id")), job);
                    if (List.of("running", "retrying", "verifying", "seeding").contains(str(state.get("status")))) {state.put("status","paused");job.persist();}
                } catch (Exception error) { AppLog.error("Invalid manifest %s: %s", path, DiagnosticText.arg(error.toString())); }
            }
            queueTimer.scheduleWithFixedDelay(()->{try{queueTick();}catch(Exception error){AppLog.error("Queue controller: %s",DiagnosticText.arg(error.getMessage()));}},250,250,TimeUnit.MILLISECONDS);
        }
        private void recoverInterruptedRemovals() throws IOException {
            try (DirectoryStream<Path> pending = Files.newDirectoryStream(dataDir, "*.removing")) {
                for (Path tombstone : pending) try {
                    safePath(tombstone);
                    if (!Files.isRegularFile(tombstone, LinkOption.NOFOLLOW_LINKS) || Files.size(tombstone) > 64L*1024*1024)
                        throw new IOException("Некорректная запись незавершённого удаления");
                    Map<String,Object> saved = obj(Json.parse(Files.readString(tombstone, StandardCharsets.UTF_8)));
                    String id = str(saved.get("id"));
                    if (!id.matches("[a-f0-9]{12}") || !tombstone.getFileName().toString().equals(id + ".removing"))
                        throw new IOException("Идентификатор записи удаления не совпадает");
                    Path manifest = dataDir.resolve(id + ".json"); safePath(manifest);
                    if (Files.exists(manifest, LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Одновременно существуют запись задания и запись удаления");
                    saved.put("status", "paused");
                    saved.put("error", "Удаление было прервано. Проверьте сохранённые файлы и повторите удаление при необходимости");
                    save(manifest, saved); Files.delete(tombstone);
                } catch (Exception error) {
                    AppLog.error("Не удалось восстановить запись незавершённого удаления %s: %s", tombstone, DiagnosticText.arg(error.getMessage()));
                }
            }
        }
        private void availableSpace(Path directory, long required) throws IOException {
            long free = spaceProbe.usable(existingFolder(directory));
            if (required > free) throw new IOException(spaceError(directory, required, free));
        }
        synchronized Map<String,Object> create(Map<String,Object> input) throws Exception {
            input=SiteProfiles.apply(input);
            String key=str(input.get("idempotency_key"));
            if(key.isEmpty())return createOnce(input);
            if(!key.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IllegalArgumentException("Invalid idempotency key");
            long now=System.currentTimeMillis();idempotency.entrySet().removeIf(e->num(obj(e.getValue()).get("time"),0)<now-TimeUnit.DAYS.toMillis(7));
            String fingerprint=inputFingerprint(input);
            if(!idempotency.containsKey(key)){List<Job> recovered=new ArrayList<>();for(Job job:jobs.values())synchronized(job){if(key.equals(job.state.get("idempotency_key"))){if(!fingerprint.equals(job.state.get("idempotency_fingerprint")))throw new IOException("Idempotency key was used for a different request");recovered.add(job);}}if(!recovered.isEmpty()){recovered.sort(Comparator.comparing(job->str(job.state.get("created_at"))));Map<String,Object> result=recovered.size()==1?recovered.get(0).snapshot():Map.of("jobs",recovered.stream().map(Job::snapshot).toList(),"recovered_idempotency",true);idempotency.put(key,Map.of("time",now,"fingerprint",fingerprint,"result",result));save(idempotencyFile,idempotency);}}
            if(idempotency.containsKey(key)){Map<String,Object> prior=obj(idempotency.get(key));if(!fingerprint.equals(prior.get("fingerprint")))throw new IOException("Idempotency key was used for a different request");Map<String,Object> result=obj(Json.parse(Json.stringify(prior.get("result"))));if(result.containsKey("id")&&!jobs.containsKey(str(result.get("id"))))throw new IOException("The idempotent job was removed; use a new key");result.put("idempotent_replay",true);return result;}
            if(idempotency.size()>=1024)throw new IOException("Idempotency registry limit reached");
            Map<String,Object> tagged=new LinkedHashMap<>(input);tagged.put("_idempotency_fingerprint",fingerprint);Map<String,Object> result=createOnce(tagged);idempotency.put(key,Map.of("time",now,"fingerprint",fingerprint,"result",result));save(idempotencyFile,idempotency);return result;
        }
        private Map<String,Object> createOnce(Map<String,Object> input) throws Exception {
            Checksums.expected(input);
            if (isTorrentInput(input)) return createTorrent(input);
            if (HubSource.isHubUrl(str(input.get("url"))) || Metalink.isMetalink(str(input.get("url"))) || WebDav.isWebDav(str(input.get("url"))) || Set.of("metalink","webdav").contains(str(input.get("source_type")))) {
                Map<String,Object> result = createFromSource(input);
                List<Object> created = arr(result.get("jobs"));
                return created.size() == 1 ? obj(created.get(0)) : result;
            }
            return createPrepared(input, null);
        }

        /** Resolve a repository explicitly; server metadata supplies all sizes/checksums. */
        synchronized Map<String,Object> createFromSource(Map<String,Object> input) throws Exception {
            input=SiteProfiles.apply(input);
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            Map<String,String> manualHashes = Checksums.expected(input);
            if (isTorrentInput(input)) {
                Map<String,Object> job = createTorrent(input);
                return Map.of("jobs", List.of(job), "total_size", job.get("size"), "source",
                        Map.of("kind", "torrent", "name", job.get("filename"),
                                "info_hash_v1", job.get("info_hash_v1"), "info_hash_v2", job.get("info_hash_v2")));
            }
            String sourceUrl = SourceTransfer.validate(str(input.get("url"))).toString();
            boolean listed=HubSource.isHubUrl(sourceUrl)||Metalink.isMetalink(sourceUrl)||WebDav.isWebDav(sourceUrl)||Set.of("metalink","webdav").contains(str(input.get("source_type")));
            if (!listed)
                return Map.of("jobs", List.of(createPrepared(input, null)), "source", Map.of("kind", "file"));
            String revision = str(input.get("revision"));
            String inspectedUrl = revision.isEmpty() ? sourceUrl : HubSource.atRevision(sourceUrl, revision);
            Map<String,Object> inspected = Metalink.isMetalink(sourceUrl)||"metalink".equals(input.get("source_type"))?Metalink.inspect(firstSourceRoute(input),sourceUrl):
                    WebDav.isWebDav(sourceUrl)||"webdav".equals(input.get("source_type"))?WebDav.inspect(firstSourceRoute(input),sourceUrl):sourceInspector.inspect(inspectedUrl,firstSourceRoute(input));
            Map<String,Map<String,Object>> available = new LinkedHashMap<>();
            for (Object value : arr(inspected.get("files"))) {
                Map<String,Object> file = obj(value);
                available.put(str(file.get("path")), file);
            }
            List<String> selected = new ArrayList<>();
            if (!"file".equals(inspected.get("kind"))) {
                if (!(input.get("selected_files") instanceof List<?> list) || list.isEmpty())
                    throw new IllegalArgumentException("Это список файлов Hugging Face. Выберите файлы перед началом загрузки");
                for (Object path : list) {
                    if (!(path instanceof String text)) throw new IllegalArgumentException("selected_files должен содержать пути файлов");
                    if (selected.contains(text)) throw new IllegalArgumentException("Файл выбран повторно: " + text);
                    selected.add(text);
                }
            } else selected.addAll(available.keySet());
            if (selected.isEmpty() || selected.size() > 1000) throw new IllegalArgumentException("Выберите от 1 до 1000 файлов");
            if (selected.size() > 1 && (!str(input.get("filename")).isBlank() || !manualHashes.isEmpty()))
                throw new IllegalArgumentException("A global filename or checksum is allowed only for one selected file");
            Path base = Path.of(str(input.get("directory")).isEmpty() ? "downloads" : str(input.get("directory"))).toAbsolutePath().normalize();
            long chunkMb = num(input.get("chunk_mb"), 20);
            if (chunkMb < 1 || chunkMb > MAX_CHUNK_MB) throw new IllegalArgumentException("Размер сегмента: 1–64 МиБ");
            List<PreparedHttp> prepared = new ArrayList<>();
            Set<String> targets = new HashSet<>();
            long total = 0, requiredSpace = 0;
            for (String path : selected) {
                Map<String,Object> file = available.get(path);
                if (file == null) throw new IllegalArgumentException("Файл отсутствует в выбранной версии репозитория: " + path);
                String[] parts = path.split("/", -1);
                Path targetDirectory = base;
                if (!"file".equals(inspected.get("kind")))
                    for (int i=0; i<parts.length-1; i++) targetDirectory = targetDirectory.resolve(filename(parts[i]));
                String name = str(input.get("filename")).isBlank() ? filename(parts[parts.length-1]) : filename(str(input.get("filename")));
                Path target = targetDirectory.resolve(name).normalize();
                String targetKey = target.toString().toLowerCase(Locale.ROOT);
                String separator = FileSystems.getDefault().getSeparator();
                if (!target.startsWith(base) || targets.stream().anyMatch(previous -> previous.equals(targetKey) ||
                        previous.startsWith(targetKey + separator) || targetKey.startsWith(previous + separator)))
                    throw new IllegalArgumentException("Пути выбранных файлов пересекаются");
                targets.add(targetKey);
                for (Path ancestor = targetDirectory; ancestor != null; ancestor = ancestor.getParent()) {
                    if (Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(ancestor))
                        throw new IllegalArgumentException("Путь папки занят файлом: " + ancestor.getFileName());
                    if (ancestor.startsWith(base) && !ancestor.equals(base) && Files.isSymbolicLink(ancestor))
                        throw new IllegalArgumentException("Подпапка репозитория является ссылкой: " + ancestor.getFileName());
                }
                Map<String,String> published = Checksums.values(file);
                for(var entry:manualHashes.entrySet())if(published.containsKey(entry.getKey())&&!published.get(entry.getKey()).equals(entry.getValue()))
                    throw new Checksum.ConflictException("Manual " + Checksums.label(entry.getKey()) + " differs from the publisher's checksum");
                Map<String,Object> task = new LinkedHashMap<>(input);
                task.put("url", file.get("url")); task.put("directory", targetDirectory.toString()); task.put("filename", name);
                Map<String,String> references=new LinkedHashMap<>(manualHashes);references.putAll(published);
                for(String algorithm:Checksums.ALGORITHMS)task.put("expected_"+algorithm,references.getOrDefault(algorithm,""));
                Map<String,Object> origin = new LinkedHashMap<>(file);
                origin.put("source_url", sourceUrl); origin.put("repo_id", inspected.get("repo_id"));
                origin.put("revision", inspected.get("revision")); origin.put("repo_type", inspected.get("repo_type"));
                PreparedHttp preparation = prepareHttp(task, origin, false);
                prepared.add(preparation);
                total = Math.addExact(total, num(file.get("size"),0));
                if (!preparation.reused()) requiredSpace = Math.addExact(requiredSpace, num(file.get("size"),0));
            }
            availableSpace(base, requiredSpace);
            List<Object> created = commitHttp(prepared);
            int reused = (int) prepared.stream().filter(PreparedHttp::reused).count();
            return Map.of("jobs", created, "total_size", total, "reused_count", reused, "source", Map.of("kind", inspected.get("kind"),
                    "repo_id", str(inspected.get("repo_id")), "revision", str(inspected.get("revision"))));
        }

        static Map<String,Object> firstSourceRoute(Map<String,Object> input) {
            if (input.get("route") instanceof Map<?,?> value) return obj(value);
            if (input.get("routes") instanceof List<?> list)
                for (Object value : list) if (value instanceof Map<?,?> && yes(obj(value).get("enabled"))) return obj(value);
            throw new IllegalArgumentException("Выберите шлюз или основное подключение для чтения источника");
        }

        static boolean isTorrentInput(Map<String,Object> input) {
            return "torrent".equals(str(input.get("source_type"))) || TorrentSource.isTorrentSource(str(input.get("url")));
        }

        Map<String,Object> inspectSource(Map<String,Object> input) throws Exception {
            input=SiteProfiles.apply(input);
            if (isTorrentInput(input)) {
                Map<String,Object> route = firstSourceRoute(input);
                if (!"gateway".equals(route.get("type"))) throw new IOException("Для torrent и magnet выберите шлюз");
                Map<String,Object> summary = new LinkedHashMap<>(TorrentSource.inspect(str(input.get("url")), route));
                summary.remove("torrent_metadata");
                return summary;
            }
            String url = SourceTransfer.validate(str(input.get("url"))).toString();
            Map<String,Object> route=firstSourceRoute(input);
            if(Metalink.isMetalink(url)||"metalink".equals(input.get("source_type")))return Metalink.inspect(route,url);
            if(WebDav.isWebDav(url)||"webdav".equals(input.get("source_type")))return WebDav.inspect(route,url);
            if (!HubSource.isHubUrl(url)) return SourceTransfer.inspect(route,url);
            String revision = str(input.get("revision"));
            return sourceInspector.inspect(revision.isEmpty() ? url : HubSource.atRevision(url, revision), firstSourceRoute(input));
        }

        private void checkJobTarget(Path requested) { checkJobTarget(requested, null); }
        private void checkJobTarget(Path requested, String excludedId) {
            String value = requested.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
            String separator = FileSystems.getDefault().getSeparator();
            for (Job job : jobs.values()) {
                if (str(job.state.get("id")).equals(excludedId)) continue;
                List<String> used = new ArrayList<>();
                synchronized (job) {
                    if (job.state.get("target_paths") instanceof List<?> list)
                        for (Object path : list) used.add(str(path));
                    else used.add(Path.of(str(job.state.get("directory"))).resolve(str(job.state.get("filename"))).toString());
                }
                for (String path : used) {
                    String other = Path.of(path).toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
                    if (value.equals(other) || value.startsWith(other + separator) || other.startsWith(value + separator))
                        throw new IllegalArgumentException("Для этого файла или папки уже есть задание");
                }
            }
        }

        private Job reusableTarget(Path requested, Map<String,Object> input, Map<String,Object> origin) {
            Path target = normalized(requested);
            for (Job job : jobs.values()) synchronized (job) {
                if ("torrent".equals(job.state.get("kind"))) continue;
                if(!str(job.state.get("site_profile_id")).equals(str(input.get("site_profile_id"))))continue;
                Path other = normalized(Path.of(str(job.state.get("directory"))).resolve(str(job.state.get("filename"))));
                if (!target.toString().equalsIgnoreCase(other.toString())) continue;
                String priorSha = str(job.state.get("source_sha256"));
                if (priorSha.isEmpty()) priorSha = str(job.state.get("expected_sha256"));
                Map<String,String> requestedHashes=Checksums.expected(input),priorHashes=Checksums.expected(job.state);
                boolean sameReferences=true;
                for(var entry:requestedHashes.entrySet())if(!entry.getValue().equals(priorHashes.get(entry.getKey())))sameReferences=false;
                if(!sameReferences)continue;
                if (origin != null) {
                    try {
                        HubSource.Source previous = HubSource.parse(str(job.state.get("url")));
                        if (!previous.file() || !previous.repo().equals(str(origin.get("repo_id"))) ||
                                !previous.repoType().equals(str(origin.get("repo_type"))) || !previous.path().equals(str(origin.get("path")))) continue;
                        long previousSize = num(job.state.get("source_size"), num(job.state.get("size"), -1));
                        if (previousSize != num(origin.get("size"), -2)) continue;
                        String currentSha = str(origin.get("sha256"));
                        if (!currentSha.isEmpty() && !priorSha.isEmpty() && currentSha.equalsIgnoreCase(priorSha)) return job;
                        String commit = str(origin.get("revision"));
                        if (currentSha.isEmpty() && commit.matches("[a-fA-F0-9]{40}") && commit.equalsIgnoreCase(previous.revision())) return job;
                    } catch (Exception ignored) { }
                } else {
                    String currentSha = str(input.get("expected_sha256"));
                    if (str(job.state.get("url")).equals(str(input.get("url"))) && !priorSha.isEmpty() && priorSha.equalsIgnoreCase(currentSha)) return job;
                    String prior512=priorHashes.getOrDefault("sha512",str(job.state.get("source_sha512"))),current512=requestedHashes.getOrDefault("sha512","");
                    if(str(job.state.get("url")).equals(str(input.get("url")))&&!prior512.isEmpty()&&prior512.equals(current512))return job;
                }
            }
            return null;
        }

        private static void copyProfile(Map<String,Object> input,Map<String,Object> source,Map<String,Object> route)throws IOException {
            String profile=str(input.get("site_profile_id")),from=str(source.get("site_profile_id"));
            if(!profile.isEmpty()&&!from.isEmpty()&&!profile.equals(from))throw new IOException("Invalid site profile identifier");
            if(profile.isEmpty())profile=from;
            if(!profile.isEmpty()){try{if(!UUID.fromString(profile).toString().equalsIgnoreCase(profile))throw new IllegalArgumentException();}catch(RuntimeException invalid){throw new IOException("Invalid site profile identifier");}route.put("site_profile_id",profile);}
        }
        private record PreparedHttp(Job job, boolean reused) { }
        private List<Object> commitHttp(List<PreparedHttp> prepared) {
            List<Job> saved = new ArrayList<>();
            try {
                for (PreparedHttp task : prepared) if (!task.reused()) { task.job().persist(); saved.add(task.job()); }
            } catch (RuntimeException failure) {
                for (Job job : saved) try { Files.deleteIfExists(job.manifest); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
            for (PreparedHttp task : prepared) if (!task.reused()) jobs.put(str(task.job().state.get("id")), task.job());
            List<Object> result = new ArrayList<>();
            for (PreparedHttp task : prepared) {
                if (!task.reused()) enqueue(task.job());
                Map<String,Object> snapshot = task.job().snapshot();
                if (task.reused()) snapshot.put("reused", true);
                result.add(snapshot);
            }
            return result;
        }

        private Map<String,Object> createTorrent(Map<String,Object> input) throws Exception {
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            if (!(input.get("routes") instanceof List<?> list) || list.isEmpty())
                throw new IOException("Для torrent и magnet выберите шлюз");
            List<Object> routes = new ArrayList<>(); Set<String> gateways = new HashSet<>();
            for (Object value : normalizeRoutes(arr(list))) {
                if (!(value instanceof Map<?,?>)) throw new IOException("Некорректное описание шлюза");
                Map<String,Object> from = obj(value);
                if (!yes(from.get("enabled"))) continue;
                String gateway = str(from.get("gateway"));
                if (!"gateway".equals(from.get("type"))) throw new IOException("Для torrent и magnet используйте выбранные шлюзы");
                if (!gateways.add(gateway)) throw new IOException("Шлюз выбран повторно");
                Map<String,Object> route = new LinkedHashMap<>();
                route.put("type", "gateway"); route.put("gateway", gateway); route.put("enabled", true);
                route.put("name", str(from.get("name")).isBlank() ? "Шлюз " + gateway : str(from.get("name")));
                route.put("virtual_ip", str(from.get("virtual_ip")).isBlank() ? GatewayNetwork.settings().get("virtual_ip") : from.get("virtual_ip"));
                copyProfile(input,from,route);
                route.put("bytes", 0); route.put("chunks", 0); route.put("last_error", "");
                routeChecker.check(route,true); routes.add(route);
            }
            if (routes.isEmpty() || routes.size() > 32) throw new IOException("Choose between 1 and 32 gateways");
            Map<String,Object> metadata = TorrentSource.inspect(str(input.get("url")), obj(routes.get(0)));
            for (String key : List.of("info_hash_v1", "info_hash_v2")) {
                String wanted = str(input.get(key));
                if (!wanted.isEmpty() && !wanted.equalsIgnoreCase(str(metadata.get(key))))
                    throw new IOException("Torrent изменился после выбора файлов; откройте список повторно");
            }
            Map<String,Map<String,Object>> available = new LinkedHashMap<>();
            for (Object value : arr(metadata.get("files"))) {
                Map<String,Object> file = obj(value);
                if (!yes(file.get("pad"))) available.put(str(file.get("path")), file);
            }
            List<String> selected = new ArrayList<>();
            if (input.get("selected_files") instanceof List<?> selection) {
                for (Object path : selection) {
                    if (!(path instanceof String text) || !available.containsKey(text) || selected.contains(text))
                        throw new IOException("Некорректный или повторный выбор файла torrent");
                    selected.add(text);
                }
            } else if (available.size() == 1) selected.addAll(available.keySet());
            else throw new IOException("Выберите файлы многосоставного torrent перед загрузкой");
            if (selected.isEmpty() || selected.size() > 1000) throw new IOException("Выберите от 1 до 1000 файлов");
            if (!str(input.get("filename")).isBlank()) throw new IOException("Для torrent используются имена из раздачи; очистите поле имени файла");
            Map<String,String> expectedHashes = Checksums.expected(input);
            if (!expectedHashes.isEmpty() && selected.size() != 1)
                throw new IOException("A global checksum is allowed only for one selected torrent file");
            Path base = Path.of(str(input.get("directory")).isBlank() ? "downloads" : str(input.get("directory"))).toAbsolutePath().normalize();
            List<String> targets = new ArrayList<>(); long size = 0;
            for (String path : selected) {
                String normalized = TorrentSource.normalize(path);
                Path target = base.resolve(normalized).normalize();
                if (!target.startsWith(base)) throw new IOException("Путь torrent выходит за папку сохранения");
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Файл уже существует: " + path);
                for (Path folder = target.getParent(); folder != null; folder = folder.getParent()) {
                    if (Files.exists(folder, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Путь папки занят файлом или ссылкой: " + folder.getFileName());
                    if (folder.equals(base)) break;
                }
                checkJobTarget(target); targets.add(target.toString()); size = Math.addExact(size, num(available.get(path).get("size"), 0));
            }
            availableSpace(base, size);
            String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Map<String,Object> state = new LinkedHashMap<>();
            state.put("id", id); state.put("url", str(input.get("url"))); state.put("kind", "torrent");
            if(input.containsKey("site_profile_id"))state.put("site_profile_id",input.get("site_profile_id"));
            state.put("filename", filename(str(metadata.get("name")))); state.put("directory", base.toString());
            state.put("size", size); state.put("downloaded", 0); Checksums.initialize(state,expectedHashes);
            state.put("checksum_source", "bittorrent-piece-hashes"); state.put("torrent_metadata", metadata.get("torrent_metadata"));
            state.put("info_hash_v1", metadata.get("info_hash_v1")); state.put("info_hash_v2", metadata.get("info_hash_v2"));
            state.put("selected_files", selected); state.put("target_paths", targets); state.put("files", metadata.get("files"));
            state.put("routes", routes); state.put("status", "queued"); state.put("error", ""); state.put("chunks", new LinkedHashMap<>());
            state.put("created_at", Instant.now().toString()); DownloadHistory.initialize(state); stampIdempotency(state,input);
            Job job = ownedJob(state); applyJobOptions(job,input,false); jobs.put(id, job); job.persist(); enqueue(job); return job.snapshot();
        }

        private Map<String,Object> createPrepared(Map<String,Object> input, Map<String,Object> origin) throws Exception {
            return obj(commitHttp(List.of(prepareHttp(input, origin, true))).get(0));
        }
        private PreparedHttp prepareHttp(Map<String,Object> input, Map<String,Object> origin, boolean checkSpace) throws Exception {
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            String url = SourceTransfer.validate(str(input.get("url"))).toString();
            String requested = str(input.get("filename"));
            String path = URI.create(url).getPath();
            String inferred = path == null || path.isEmpty() || path.endsWith("/") ? "download.bin" : Path.of(path).getFileName().toString();
            String name = requested.isEmpty() ? inferred : requested;
            if (name.isEmpty()) name = "download.bin";
            name = filename(name);
            Path directory = Path.of(str(input.get("directory")).isEmpty() ? "downloads" : str(input.get("directory"))).toAbsolutePath().normalize();
            Map<String,String> expectedHashes = Checksums.expected(input);
            String expected = expectedHashes.getOrDefault("sha256", "");
            long chunkMb = num(input.get("chunk_mb"), 20);
            if (chunkMb < 1 || chunkMb > MAX_CHUNK_MB) throw new IllegalArgumentException("Размер сегмента: 1–64 МиБ");
            List<Object> routes = new ArrayList<>();
            for (Object item : normalizeRoutes(arr(input.get("routes")))) {
                Map<String,Object> source = obj(item);
                String type = str(source.get("type"));
                if (type.equals("default")) type = "direct";
                if (!type.equals("direct") && !type.equals("worker") && !type.equals("gateway")) throw new IllegalArgumentException("Неизвестный тип канала");
                boolean enabled = yes(source.get("enabled"));
                String endpoint = str(source.get("endpoint")).trim(), token = str(source.get("token"));
                if (type.equals("worker") && enabled) {
                    httpUrl(endpoint);
                    if (!token.isEmpty() && token.length() < 16)
                        throw new IllegalArgumentException("Токен агента должен содержать от 16 символов, если он задан");
                }
                Map<String,Object> route = new LinkedHashMap<>();
                route.put("name", str(source.get("name")).isEmpty() ? type : str(source.get("name")));
                route.put("type", type); route.put("gateway", str(source.get("gateway"))); route.put("endpoint", endpoint); route.put("token", token); route.put("enabled", enabled);
                copyProfile(input,source,route);
                if(type.equals("worker")&&!str(route.get("site_profile_id")).isEmpty())throw new IOException("Source authentication requires an application gateway");
                if (type.equals("gateway")) {
                    route.put("virtual_ip", str(source.get("virtual_ip")).isBlank()
                            ? GatewayNetwork.settings().get("virtual_ip") : str(source.get("virtual_ip")));
                    route.put("endpoint", "");
                    route.put("token", "");
                    if (enabled) routeChecker.check(route,false);
                }
                route.put("bytes", 0); route.put("chunks", 0); route.put("last_error", "");
                if (enabled && type.equals("worker") && !str(route.get("gateway")).isEmpty()) {
                    Map<String,Object> probe = Transport.probe(route);
                    if (!yes(probe.get("ok")) || !str(route.get("gateway")).equals(str(probe.get("expected_gateway"))) || !yes(probe.get("gateway_match")))
                        throw new IllegalArgumentException("Не подтверждён маршрут агента через " + route.get("gateway"));
                }
                routes.add(route);
            }
            if (routes.stream().noneMatch(item -> yes(obj(item).get("enabled")))) throw new IllegalArgumentException("Включите хотя бы один канал");
            Path finalPath = directory.resolve(name);
            safePath(finalPath); existingFolder(directory);
            Job reusable = reusableTarget(finalPath, input, origin);
            if (reusable != null) return new PreparedHttp(reusable, true);
            if (Files.exists(finalPath, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Целевой файл уже существует");
            checkJobTarget(finalPath);
            if (checkSpace && origin != null) availableSpace(directory, num(origin.get("size"), 0));
            String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Map<String,Object> state = new LinkedHashMap<>();
            state.put("id", id); state.put("url", url); state.put("filename", name); state.put("directory", directory.toString());
            if(input.containsKey("site_profile_id"))state.put("site_profile_id",input.get("site_profile_id"));
            Checksums.initialize(state,expectedHashes);
            if (origin != null) {
                state.put("source_url", origin.get("source_url")); state.put("source_path", origin.get("path"));
                state.put("repo_id", origin.get("repo_id")); state.put("revision", origin.get("revision"));
                state.put("storage", origin.get("storage")); state.put("source_size", origin.get("size"));
                Map<String,String> published=Checksums.values(origin),sources=new LinkedHashMap<>();
                if(origin.get("checksum_sources") instanceof Map<?,?> map)for(var entry:map.entrySet())sources.put(str(entry.getKey()),str(entry.getValue()));
                if(published.containsKey("sha256")&&!sources.containsKey("sha256"))sources.put("sha256",str(origin.get("checksum_source")));
                Checksums.acceptDiscovery(state,published,sources);
            }
            Object mirrors=origin!=null?origin.get("mirrors"):input.get("mirrors");
            if(mirrors instanceof List<?> list){if(list.isEmpty()||list.size()>32||expected.isEmpty())throw new IOException("Trusted mirrors require a SHA-256 and 1 to 32 URLs");List<String> urls=new ArrayList<>();for(Object item:list)urls.add(SourceTransfer.validate(str(item)).toString());state.put("mirrors",urls);}
            if(origin!=null)for(String field:List.of("piece_length","piece_hashes","metalink_url"))if(origin.containsKey(field))state.put(field,origin.get(field));
            state.put("sha256", ""); state.put("chunk_size", chunkMb * 1024 * 1024);
            state.put("size", 0); state.put("downloaded", 0); state.put("status", "queued"); state.put("error", "");
            state.put("created_at", Instant.now().toString()); state.put("routes", routes); state.put("etag", ""); state.put("modified", "");
            DownloadHistory.initialize(state);
            state.put("chunks", new LinkedHashMap<>()); state.put("part_path", directory.resolve("." + name + "." + id + ".part").toString());
            stampIdempotency(state,input);Job job=ownedJob(state);applyJobOptions(job,input,false);return new PreparedHttp(job,false);
        }
        private static Object canonicalValue(Object value) {
            if(value instanceof Map<?,?> map){Map<String,Object> copy=new TreeMap<>();for(var item:map.entrySet())copy.put(str(item.getKey()),canonicalValue(item.getValue()));return copy;}
            if(value instanceof List<?> list){List<Object> copy=new ArrayList<>();for(Object item:list)copy.add(canonicalValue(item));return copy;}return value;
        }
        private static String inputFingerprint(Map<String,Object> input){Map<String,Object> stable=new LinkedHashMap<>(input);stable.remove("idempotency_key");stable.remove("_idempotency_fingerprint");return hex(sha().digest(Json.stringify(canonicalValue(stable)).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        private static void stampIdempotency(Map<String,Object> state,Map<String,Object> input){String key=str(input.get("idempotency_key"));if(!key.isEmpty()){state.put("idempotency_key",key);state.put("idempotency_fingerprint",input.containsKey("_idempotency_fingerprint")?input.get("_idempotency_fingerprint"):inputFingerprint(input));}}
        private Job ownedJob(Map<String,Object> state) {
            Job job=new Job(state,dataDir);job.transferLimits=queue.limits;job.events=events;
            state.putIfAbsent("priority",0);state.putIfAbsent("start_after_ms",0L);
            state.putIfAbsent("seeding_enabled",false);state.putIfAbsent("seeding_minutes",30);state.putIfAbsent("seeding_ratio",1.0);state.putIfAbsent("file_priorities",Map.of());
            return job;
        }
        private synchronized void enqueue(Job job) {
            synchronized(job) {
                if(closing||job.shuttingDown||"complete".equals(job.state.get("status")))return;
                job.restart=false;job.state.put("status","queued");job.state.put("error","");job.persist();
            }
            queueTick();
        }
        private synchronized void queueTick() {
            if(closing)return;
            boolean allowed=queue.allowed(Instant.now());
            List<Job> active=new ArrayList<>(),pending=new ArrayList<>();
            for(Job job:jobs.values())synchronized(job) {
                String status=str(job.state.get("status"));
                if(job.thread!=null&&job.thread.isAlive())active.add(job);
                if("queued".equals(status)&&!job.maintenance&&(job.thread==null||!job.thread.isAlive())&&num(job.state.get("start_after_ms"),0)<=System.currentTimeMillis())pending.add(job);
            }
            if(!allowed) {
                for(Job job:active)synchronized(job) {
                    if(!List.of("running","retrying","seeding").contains(str(job.state.get("status"))))continue;
                    job.pause();job.state.put("status","queued");job.state.put("queue_reason","schedule");job.persist();
                }
                return;
            }
            pending.sort(Comparator.<Job>comparingLong(job->-num(job.snapshot().get("priority"),0)).thenComparing(job->str(job.snapshot().get("created_at"))));
            int available=queue.maximum()-active.size();
            for(Job job:pending){if(available--<=0)break;job.state.remove("queue_reason");job.launch();}
        }
        synchronized Map<String,Object> queueSettings(){Map<String,Object> value=queue.snapshot();value.put("schedule_timezone",java.time.ZoneId.systemDefault().getId());value.put("active_jobs",jobs.values().stream().filter(job->job.thread!=null&&job.thread.isAlive()).count());return value;}
        synchronized Map<String,Object> updateQueueSettings(Map<String,Object> value)throws IOException {
            if(closing)throw new IOException("Приложение завершает работу");queue.update(value);queueTick();events.publish("queue",queueSettings());return queueSettings();
        }
        synchronized Map<String,Object> batchCreate(List<Map<String,Object>> inputs) {
            if(inputs==null||inputs.isEmpty()||inputs.size()>128)throw new IllegalArgumentException("Batch requires between 1 and 128 links");
            List<Object> results=new ArrayList<>();int successes=0;
            for(int index=0;index<inputs.size();index++)try{Map<String,Object> job=create(inputs.get(index));results.add(Map.of("index",index,"ok",true,"job",job));successes++;}
            catch(Exception failure){results.add(Map.of("index",index,"ok",false,"error",str(failure.getMessage())));}
            return Map.of("results",results,"success_count",successes);
        }
        synchronized Map<String,Object> updateJobOptions(String id,Map<String,Object> options)throws IOException {
            if(closing)throw new IOException("Приложение завершает работу");Job job=job(id);
            synchronized(job) {
                Map<String,Object> previous=new LinkedHashMap<>(job.state);
                Map<String,Object> beforeFiles=previous.get("file_priorities") instanceof Map<?,?> raw?obj(raw):Map.of();
                try{applyJobOptions(job,options,true);job.persist();}catch(RuntimeException failure){job.state.clear();job.state.putAll(previous);throw failure;}
                Map<String,Object> afterFiles=job.state.get("file_priorities") instanceof Map<?,?> raw?obj(raw):Map.of();
                boolean changedFiles=false;if(job.state.get("selected_files") instanceof List<?> selected)for(Object path:selected)if(num(beforeFiles.get(str(path)),4)!=num(afterFiles.get(str(path)),4)){changedFiles=true;break;}
                if(changedFiles&&"torrent".equals(job.state.get("kind"))&&job.thread!=null&&job.thread.isAlive()){job.pause();enqueue(job);}
            }
            queueTick();return job.snapshot();
        }
        private static void applyJobOptions(Job job,Map<String,Object> options,boolean strict) {
            Set<String> allowed=Set.of("priority","start_after_ms","file_priorities","seeding_enabled","seeding_minutes","seeding_ratio");
            if(strict)for(String field:options.keySet())if(!allowed.contains(field))throw new IllegalArgumentException("Unknown job option: "+field);
            Map<String,Object> accepted=new LinkedHashMap<>();
            for(String field:List.of("priority","start_after_ms","seeding_minutes"))if(options.containsKey(field)) {
                long min=field.equals("priority")?-100:field.equals("seeding_minutes")?1:0;
                long max=field.equals("priority")?100:field.equals("seeding_minutes")?1440:253402300799000L;
                accepted.put(field,QueueController.integer(options.get(field),min,max,field));
            }
            if(options.containsKey("seeding_enabled")){if(!(options.get("seeding_enabled") instanceof Boolean))throw new IllegalArgumentException("Invalid seeding_enabled");accepted.put("seeding_enabled",options.get("seeding_enabled"));}
            if(options.containsKey("seeding_ratio")){if(!(options.get("seeding_ratio") instanceof Number number)||!Double.isFinite(number.doubleValue())||number.doubleValue()<=0||number.doubleValue()>100)throw new IllegalArgumentException("Invalid seeding_ratio");accepted.put("seeding_ratio",number.doubleValue());}
            if(options.containsKey("file_priorities")) {
                if(!"torrent".equals(job.state.get("kind")))throw new IllegalArgumentException("File priorities require a torrent job");
                if(!(options.get("file_priorities") instanceof Map<?,?> raw)||raw.size()>1000)throw new IllegalArgumentException("Invalid file priorities");
                Set<String> selected=new HashSet<>();for(Object path:arr(job.state.get("selected_files")))selected.add(str(path));
                Map<String,Object> priorities=new LinkedHashMap<>();for(var entry:raw.entrySet()){String path=str(entry.getKey());if(!selected.contains(path))throw new IllegalArgumentException("File is not selected in the torrent: "+path);priorities.put(path,QueueController.integer(entry.getValue(),0,7,"file priority"));}
                if(selected.stream().allMatch(path->num(priorities.get(path),4)==0))throw new IllegalArgumentException("Select at least one torrent file");
                accepted.put("file_priorities",priorities);
            }
            job.state.putAll(accepted);
        }
        synchronized void addGatewayProfile(String gateway)throws IOException {
            Map<String,Object> settings=GatewayNetwork.settings();List<String> gateways=new ArrayList<>();for(Object raw:arr(settings.get("gateways")))gateways.add(str(raw));
            if(gateways.contains(gateway))throw new IOException("Gateway profile already exists");gateways.add(gateway);configureNetwork(str(settings.get("virtual_ip")),gateways);
        }
        synchronized void removeGatewayProfile(String gateway)throws IOException {
            Map<String,Object> settings=GatewayNetwork.settings();List<String> gateways=new ArrayList<>();for(Object raw:arr(settings.get("gateways")))if(!gateway.equals(str(raw)))gateways.add(str(raw));
            if(gateways.size()==arr(settings.get("gateways")).size())throw new IOException("Gateway profile was not found");configureNetwork(str(settings.get("virtual_ip")),gateways);
        }
        synchronized void configureNetwork(String ip, List<String> gateways) throws IOException {
            if(closing)throw new IOException("Приложение завершает работу");
            GatewayNetwork.validateSettings(ip,gateways);
            Map<String,Object> current=GatewayNetwork.settings();
            boolean changed=!ip.equals(current.get("virtual_ip"))||!gateways.equals(current.get("gateways"));
            if(!changed){try{GatewayNetwork.checkAddress();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException(interrupted);}return;}
            Set<String> desired=new HashSet<>(gateways);Map<Job,List<Object>> previous=new LinkedHashMap<>();Map<Job,Map<String,Long>> epochs=new LinkedHashMap<>();
            for(Job job:jobs.values())synchronized(job) {
                if("complete".equals(job.state.get("status")))continue;
                if(!ip.equals(current.get("virtual_ip"))&&job.thread!=null&&job.thread.isAlive())throw new IOException("Pause downloads before changing the virtual IP");
                List<Object> routes=arr(job.state.get("routes"));boolean affected=routes.stream().anyMatch(raw->"gateway".equals(obj(raw).get("type"))&&yes(obj(raw).get("enabled"))&&!desired.contains(str(obj(raw).get("gateway"))));
                if(affected&&!routes.stream().anyMatch(raw->yes(obj(raw).get("enabled"))&&(!"gateway".equals(obj(raw).get("type"))||desired.contains(str(obj(raw).get("gateway"))))))throw new IOException("Keep at least one gateway for every unfinished download");
                previous.put(job,arr(Json.parse(Json.stringify(routes))));epochs.put(job,new HashMap<>(job.disabledEpochs));
            }
            Path settingsPath=dataDir.resolve("desktop-settings.config");safePath(settingsPath);
            boolean settingsExisted=Files.isRegularFile(settingsPath,LinkOption.NOFOLLOW_LINKS);
            Map<String,Object> oldSettings=settingsExisted?new LinkedHashMap<>(obj(Json.parse(Files.readString(settingsPath, StandardCharsets.UTF_8)))):new LinkedHashMap<>();
            Map<String,Object> nextSettings=new LinkedHashMap<>(oldSettings);nextSettings.put("virtual_ip",ip);nextSettings.put("gateways",gateways);
            try {
                for(Job job:previous.keySet())synchronized(job){
                    for(Object raw:arr(job.state.get("routes"))){Map<String,Object> route=obj(raw);if(!"gateway".equals(route.get("type")))continue;route.put("virtual_ip",ip);
                        if(!desired.contains(str(route.get("gateway")))){if(yes(route.get("enabled")))job.disabledEpochs.merge(Job.routeKey(route),1L,Long::sum);route.put("enabled",false);route.put("active",false);route.put("stalled",false);}}
                    job.persist();job.routesRevision++;
                }
                for(Job job:previous.keySet())job.selectionChanged();
                GatewayNetwork.configure(ip,gateways);
                if(!ip.equals(current.get("virtual_ip")))GatewayNetwork.checkAddress();
                save(settingsPath,nextSettings);
            }catch(Exception failure){
                for(Job job:previous.keySet())synchronized(job){Map<String,Map<String,Object>> savedRoutes=new HashMap<>();for(Object raw:previous.get(job))savedRoutes.put(Job.routeKey(obj(raw)),obj(raw));for(Object raw:arr(job.state.get("routes"))){Map<String,Object> route=obj(raw),saved=savedRoutes.get(Job.routeKey(route));if(saved==null)continue;for(String field:List.of("enabled","active","stalled","virtual_ip")){route.remove(field);if(saved.containsKey(field))route.put(field,saved.get(field));}}job.disabledEpochs.clear();job.disabledEpochs.putAll(epochs.get(job));job.routesRevision++;try{job.persist();}catch(RuntimeException restore){failure.addSuppressed(restore);}}
                for(Job job:previous.keySet())job.selectionChanged();
                try{List<String> old=new ArrayList<>();for(Object raw:arr(current.get("gateways")))old.add(str(raw));GatewayNetwork.configure(str(current.get("virtual_ip")),old);}catch(Exception restore){failure.addSuppressed(restore);}
                try{if(settingsExisted)save(settingsPath,oldSettings);else Files.deleteIfExists(settingsPath);}catch(Exception restore){failure.addSuppressed(restore);}
                if(failure instanceof InterruptedException)Thread.currentThread().interrupt();throw new IOException(failure.getMessage(),failure);
            }
            events.publish("network",GatewayNetwork.settings());
        }
        synchronized Map<String,Object> updateJobRoutes(String id, List<Object> requested) throws Exception {
            if (closing) throw new IOException("Приложение завершает работу");
            Job job = job(id);
            Map<String,Object> network = GatewayNetwork.settings();
            String ip = str(network.get("virtual_ip"));
            Set<String> configured = new LinkedHashSet<>();
            for (Object raw : arr(network.get("gateways"))) configured.add(str(raw));
            if (requested == null || requested.isEmpty() || requested.size() > 32)
                throw new IOException("Choose between 1 and 32 gateways");
            LinkedHashMap<String,Map<String,Object>> desired = new LinkedHashMap<>();
            boolean torrent;
            Map<String,Object> profileBinding;
            synchronized (job) {
                requireRouteChangeAllowed(job); torrent = "torrent".equals(job.state.get("kind"));
                profileBinding=SiteProfiles.apply(job.state);
                for (Object raw : arr(job.state.get("routes"))) {
                    Map<String,Object> existing = obj(raw);
                    if ("gateway".equals(existing.get("type")) && !str(existing.get("virtual_ip")).isBlank() &&
                            !ip.equals(str(existing.get("virtual_ip"))))
                        throw new IOException("Нельзя менять сеть приложения во время изменения шлюзов загрузки");
                }
            }
            Set<String> seen = new HashSet<>();
            for (Object raw : requested) {
                if (!(raw instanceof Map<?,?>)) throw new IOException("Некорректный список шлюзов загрузки");
                Map<String,Object> from = obj(raw);
                String gateway = str(from.get("gateway"));
                if (!"gateway".equals(str(from.get("type"))) || !configured.contains(gateway) || !seen.add(gateway))
                    throw new IOException("Выберите только настроенные в приложении шлюзы без повторов");
                if (!yes(from.get("enabled"))) continue;
                if (!str(from.get("virtual_ip")).isBlank() && !ip.equals(str(from.get("virtual_ip"))))
                    throw new IOException("Нельзя менять сеть приложения во время изменения шлюзов загрузки");
                Map<String,Object> candidate = new LinkedHashMap<>();
                candidate.put("type", "gateway"); candidate.put("gateway", gateway); candidate.put("enabled", true);
                candidate.put("virtual_ip", ip); candidate.put("endpoint", ""); candidate.put("token", "");
                candidate.put("name", str(from.get("name")).isBlank() ? "Шлюз " + gateway : str(from.get("name")));
                candidate.put("bytes", 0L); candidate.put("received_bytes", 0L); candidate.put("chunks", 0L);
                candidate.put("last_error", "");
                if(from.containsKey("site_profile_id")&&!str(from.get("site_profile_id")).equals(str(profileBinding.get("site_profile_id"))))throw new IOException("Invalid site profile identifier");
                copyProfile(profileBinding,Map.of(),candidate);
                routeChecker.check(candidate, torrent); desired.put(gateway, candidate);
            }
            if (desired.isEmpty()) throw new IOException("Choose between 1 and 32 gateways");
            synchronized (job) {
                requireRouteChangeAllowed(job);
                Map<String,Object> latest = GatewayNetwork.settings();
                if (!ip.equals(latest.get("virtual_ip")) || !network.get("gateways").equals(latest.get("gateways")))
                    throw new IOException("Нельзя менять сеть приложения во время изменения шлюзов загрузки");
                List<Object> old = arr(job.state.get("routes"));
                List<Object> routes = new ArrayList<>(old);
                Map<String,Map<String,Object>> flags = new LinkedHashMap<>();
                Map<String,Long> epochs = new HashMap<>(job.disabledEpochs);
                for (Object raw : old) {
                    Map<String,Object> route = obj(raw); String key = Job.routeKey(route);
                    Map<String,Object> savedFlags = new LinkedHashMap<>();
                    for (String field : List.of("enabled", "active", "stalled"))
                        if (route.containsKey(field)) savedFlags.put(field, route.get(field));
                    flags.put(key, savedFlags);
                    boolean enabled = "gateway".equals(route.get("type")) && desired.containsKey(str(route.get("gateway")));
                    if (yes(route.get("enabled")) && !enabled)
                        job.disabledEpochs.put(key, job.disabledEpochs.getOrDefault(key, 0L) + 1);
                    route.put("enabled", enabled);
                    if (!enabled) { route.put("active", false); route.put("stalled", false); }
                    if (enabled) desired.remove(str(route.get("gateway")));
                }
                boolean hadProfile=job.state.containsKey("site_profile_id");Object previousProfile=job.state.get("site_profile_id");
                if(!str(profileBinding.get("site_profile_id")).isEmpty())job.state.put("site_profile_id",profileBinding.get("site_profile_id"));
                routes.addAll(desired.values()); job.state.put("routes", routes);
                try { job.persist(); }
                catch (RuntimeException failure) {
                    for (Object raw : old) {
                        Map<String,Object> route = obj(raw);
                        for (String field : List.of("enabled", "active", "stalled")) route.remove(field);
                        route.putAll(flags.get(Job.routeKey(route)));
                    }
                    job.state.put("routes", old);if(hadProfile)job.state.put("site_profile_id",previousProfile);else job.state.remove("site_profile_id");
                    job.disabledEpochs.clear(); job.disabledEpochs.putAll(epochs); throw failure;
                }
                job.routesRevision++;
            }
            job.selectionChanged();
            AppLog.info("Шлюзы загрузки %s изменены", str(job.state.get("filename")));
            return job.snapshot();
        }
        private static void requireRouteChangeAllowed(Job job) throws IOException {
            if (job.shuttingDown || job.maintenance || (job.stop && job.thread != null && job.thread.isAlive()))
                throw new IOException("Загрузка ещё останавливается; повторите действие после остановки");
            if (List.of("complete", "verifying").contains(str(job.state.get("status"))))
                throw new IOException("Нельзя менять шлюзы завершённой загрузки или во время проверки файла");
        }
        Job job(String id) { Job job = jobs.get(id); if (job == null) throw new IllegalArgumentException("Задание не найдено"); return job; }
        private Path ownedPart(Job job) throws IOException {
            String id = str(job.state.get("id"));
            if (!id.matches("[a-f0-9]{12}")) throw new IOException("Некорректный идентификатор загрузки");
            String name = filename(str(job.state.get("filename")));
            Path expected = normalized(Path.of(str(job.state.get("directory"))).resolve("." + name + "." + id + ".part"));
            Path actual = normalized(job.part());
            if (!actual.equals(expected)) throw new IOException("Промежуточный файл не принадлежит этому заданию");
            safePath(actual);
            if (Files.exists(actual, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(actual, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Некорректный промежуточный файл");
            return actual;
        }
        private Path torrentCache(Job job) throws IOException {
            String id = str(job.state.get("id"));
            if (!id.matches("[a-f0-9]{12}")) throw new IOException("Некорректный идентификатор загрузки");
            Path path = dataDir.resolve("torrents").resolve(id); safePath(path); return path;
        }
        private static List<Path> ownedTree(Path root) throws IOException {
            safePath(root); List<Path> entries = new ArrayList<>();
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return entries;
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                    safePath(directory);
                    if (!attributes.isDirectory() || attributes.isOther() || attributes.isSymbolicLink())
                        throw new IOException("Небезопасная папка временных данных: " + directory);
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    safePath(file);
                    if (!attributes.isRegularFile() || attributes.isOther() || attributes.isSymbolicLink())
                        throw new IOException("Небезопасный файл временных данных: " + file);
                    entries.add(file); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                    if (failure != null) throw failure; entries.add(directory); return FileVisitResult.CONTINUE;
                }
            });
            return entries;
        }
        private List<Path> ownedPublishedFiles(Job selected) throws Exception {
            Path directory = normalized(Path.of(str(selected.state.get("directory")))); safePath(directory);
            List<Path> result = new ArrayList<>();
            if (!"torrent".equals(selected.state.get("kind"))) {
                Path target = directory.resolve(filename(str(selected.state.get("filename")))); safePath(target);
                if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return result;
                String proof = str(selected.state.get("sha256"));
                if (!SHA256.matcher(proof).matches() || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) ||
                        Files.size(target) != num(selected.state.get("size"), -1) || !selected.hashFile(target).equalsIgnoreCase(proof))
                    throw new IOException("Файл по пути задания не подтверждён как его результат и сохранён: " + target);
                result.add(target); return result;
            }
            Map<String,Object> proofs = new LinkedHashMap<>();
            Path checkpoint = torrentCache(selected).resolve("checkpoint.json"); safePath(checkpoint);
            if (Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(checkpoint, LinkOption.NOFOLLOW_LINKS) || Files.size(checkpoint) > 64L*1024*1024)
                    throw new IOException("Некорректный checkpoint torrent");
                Map<String,Object> saved = obj(Json.parse(Files.readString(checkpoint, StandardCharsets.UTF_8)));
                if (!directory.toString().equals(str(saved.get("root")))) throw new IOException("Папка checkpoint torrent не совпадает с заданием");
                if (saved.get("published") instanceof Map<?,?>) proofs.putAll(obj(saved.get("published")));
            }
            if (selected.state.get("files") instanceof List<?>) for (Object raw : arr(selected.state.get("files"))) {
                Map<String,Object> file = obj(raw);
                if (SHA256.matcher(str(file.get("sha256"))).matches()) proofs.put(str(file.get("path")), file);
            }
            Set<Path> seen = new HashSet<>();
            for (Object raw : arr(selected.state.get("selected_files"))) {
                String path = TorrentSource.normalize(str(raw)); Path target = directory.resolve(path).normalize();
                if (!target.startsWith(directory)) throw new IOException("Путь torrent выходит за папку сохранения");
                safePath(target); if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) continue;
                Map<String,Object> proof = proofs.get(path) instanceof Map<?,?> ? obj(proofs.get(path)) : Map.of();
                if (!SHA256.matcher(str(proof.get("sha256"))).matches() || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) ||
                        Files.size(target) != num(proof.get("size"), -1) || !selected.hashFile(target).equalsIgnoreCase(str(proof.get("sha256"))))
                    throw new IOException("Файл torrent не подтверждён как результат этого задания и сохранён: " + target);
                if (seen.add(target)) result.add(target);
            }
            return result;
        }
        synchronized Map<String,Object> remove(String id) throws Exception { return remove(id, false); }
        synchronized Map<String,Object> remove(String id, boolean deleteFiles) throws Exception {
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            Job selected = job(id); selected.stopAndAwait();
            Path tombstone = dataDir.resolve(id + ".removing"); boolean renamed = false;
            List<String> deletedFiles = new ArrayList<>(), keptPaths = new ArrayList<>(); Path deleting = null;
            try {
                safePath(selected.manifest); safePath(tombstone);
                if (!selected.manifest.equals(dataDir.resolve(id + ".json")) || !Files.isRegularFile(selected.manifest, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Некорректный файл задания");
                List<Path> temporary = new ArrayList<>();
                if (!"torrent".equals(selected.state.get("kind"))) {
                    Path part = ownedPart(selected);
                    if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)) {
                        if (deleteFiles) temporary.add(part); else keptPaths.add(part.toString());
                    }
                }
                Path cache = torrentCache(selected);
                if (deleteFiles) temporary.addAll(ownedTree(cache));
                else if (Files.exists(cache, LinkOption.NOFOLLOW_LINKS)) keptPaths.add(cache.toString());
                List<Path> published = deleteFiles ? ownedPublishedFiles(selected) : List.of();
                if (Files.exists(tombstone, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Удаление этого задания уже выполнялось; временная запись занята");
                Files.move(selected.manifest, tombstone); renamed = true;
                for (Path path : published) { deleting = path; safePath(path); Files.delete(path); deletedFiles.add(path.toString()); }
                for (Path path : temporary) {
                    deleting = path; safePath(path); boolean file = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
                    Files.delete(path); if (file) deletedFiles.add(path.toString());
                }
                deleting = tombstone; Files.delete(tombstone); renamed = false; jobs.remove(id, selected);
                return Map.of("ok", true, "id", id, "removed", true, "completed_files_kept", !deleteFiles,
                        "unfinished_parts_kept", !deleteFiles, "delete_files", deleteFiles, "deleted_files", deletedFiles, "kept_paths", keptPaths);
            } catch (Exception failure) {
                boolean restored = !renamed;
                if (renamed) try { Files.move(tombstone, selected.manifest); restored = true; }
                catch (IOException restoration) { failure.addSuppressed(restoration); }
                if (deleting != null) throw new IOException("Удаление выполнено частично; удалено объектов: " + deletedFiles.size() +
                        ". Не удалось удалить: " + deleting + ". Причина: " + str(failure.getMessage()) +
                        (restored ? ". Задание сохранено для повторной попытки." : ". Запись удаления осталась: " + tombstone + "; восстановление записи не удалось."), failure);
                throw failure;
            } finally { if (jobs.containsKey(id)) selected.maintenance = false; }
        }
        synchronized Map<String,Object> changeDirectory(String id, String value) throws Exception {
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            Job selected = job(id); Map<String,Object> before = selected.snapshot();
            if (!List.of("paused", "failed").contains(str(before.get("status"))))
                throw new IOException("Приостановите загрузку перед сменой папки сохранения");
            if (value == null || value.isBlank()) throw new IOException("Выберите папку сохранения");
            Path destination = normalized(Path.of(value)); existingFolder(destination);
            boolean torrent = "torrent".equals(selected.state.get("kind"));
            Path oldDirectory = normalized(Path.of(str(selected.state.get("directory"))));
            if (destination.equals(oldDirectory)) {
                Map<String,Object> same = selected.snapshot(); same.put("directory_updated", false); same.put("resume_preserved", true); return same;
            }
            List<String> targets = new ArrayList<>();
            if (torrent) for (Object path : arr(selected.state.get("selected_files"))) {
                Path target = destination.resolve(TorrentSource.normalize(str(path))).normalize();
                if (!target.startsWith(destination)) throw new IOException("Путь torrent выходит за папку сохранения");
                safePath(target);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Целевой файл уже существует: " + target);
                checkJobTarget(target, id); targets.add(target.toString());
            } else {
                Path target = destination.resolve(filename(str(selected.state.get("filename")))); safePath(target);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Целевой файл уже существует: " + target);
                checkJobTarget(target, id);
            }
            selected.stopAndAwait();
            Map<String,Object> previous;
            synchronized (selected) { previous = obj(Json.parse(Json.stringify(selected.state))); }
            Path oldPart = null, newPart = null, checkpoint = null; Map<String,Object> oldCheckpoint = null;
            boolean copied = false, checkpointChanged = false, manifestChanged = false;
            Map<String,Object> committed = null;
            try {
                long required = Math.max(num(selected.state.get("size"), 0), num(selected.state.get("source_size"), 0));
                if (!torrent) {
                    oldPart = ownedPart(selected);
                    newPart = destination.resolve("." + filename(str(selected.state.get("filename"))) + "." + id + ".part");
                    safePath(newPart);
                    if (Files.exists(newPart, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException("Промежуточный файл в новой папке уже существует");
                    if (Files.exists(oldPart, LinkOption.NOFOLLOW_LINKS)) required = Math.max(required, Files.size(oldPart));
                } else {
                    checkpoint = torrentCache(selected).resolve("checkpoint.json"); safePath(checkpoint);
                    if (Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
                        if (!Files.isRegularFile(checkpoint, LinkOption.NOFOLLOW_LINKS) || Files.size(checkpoint) > 64L*1024*1024)
                            throw new IOException("Некорректный checkpoint torrent");
                        oldCheckpoint = obj(Json.parse(Files.readString(checkpoint, StandardCharsets.UTF_8)));
                        if (oldCheckpoint.containsKey("root") && !oldDirectory.toString().equals(str(oldCheckpoint.get("root"))))
                            throw new IOException("Сохранённая папка torrent не совпадает с заданием");
                    }
                }
                availableSpace(destination, required); Files.createDirectories(destination); safePath(destination);
                if (oldPart != null && Files.exists(oldPart, LinkOption.NOFOLLOW_LINKS)) {
                    try (FileChannel input = FileChannel.open(oldPart, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
                         FileChannel output = FileChannel.open(newPart, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                        copied = true; long position = 0, length = input.size();
                        while (position < length) {
                            long transferred = input.transferTo(position, Math.min(16L*1024*1024, length-position), output);
                            if (transferred <= 0) throw new IOException("Не удалось перенести промежуточный файл; исходные части сохранены");
                            position += transferred;
                        }
                        output.force(true);
                    }
                    if (Files.size(oldPart) != Files.size(newPart) || !selected.hashFile(oldPart).equals(selected.hashFile(newPart)))
                        throw new IOException("Ошибка проверки перенесённых частей; исходные части сохранены");
                    try (FileChannel channel = FileChannel.open(newPart, StandardOpenOption.WRITE)) { channel.force(true); }
                }
                if (oldCheckpoint != null) {
                    Map<String,Object> updated = new LinkedHashMap<>(oldCheckpoint);
                    updated.put("root", destination.toString()); updated.put("published", new LinkedHashMap<>());
                    save(checkpoint, updated); checkpointChanged = true;
                }
                synchronized (selected) {
                    selected.state.put("directory", destination.toString());
                    if (torrent) selected.state.put("target_paths", targets);
                    else selected.state.put("part_path", newPart.toString());
                    selected.state.put("status", "paused"); selected.state.put("error", ""); selected.persist(); manifestChanged = true;
                    committed = obj(Json.parse(Json.stringify(selected.state)));
                }
                if (copied) Files.delete(oldPart);
                Map<String,Object> result = selected.snapshot(); result.put("directory_updated", true); result.put("resume_preserved", true); return result;
            } catch (Exception failure) {
                boolean restored = true;
                if (manifestChanged || !selected.state.equals(previous)) {
                    synchronized (selected) {
                        selected.state.clear(); selected.state.putAll(previous);
                        try { selected.persist(); }
                        catch (Exception restoration) {
                            restored = false; failure.addSuppressed(restoration);
                            if (manifestChanged && committed != null) { selected.state.clear(); selected.state.putAll(committed); }
                        }
                    }
                }
                if (restored && checkpointChanged) try { save(checkpoint, oldCheckpoint); } catch (Exception restoration) { failure.addSuppressed(restoration); }
                // If the old manifest could not be restored, retain the new verified copy it may still reference.
                if (restored && copied) try { safePath(newPart); Files.deleteIfExists(newPart); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            } finally { selected.maintenance = false; }
        }
        synchronized Map<String,Object> action(String id, String action) throws IOException {
            if (closing) throw new IllegalStateException("Приложение завершает работу");
            Job selected = job(id);
            if ("pause".equals(action)) selected.pause();
            else if ("resume".equals(action)) {
                if (!"complete".equals(selected.state.get("status"))) {
                    Path directory = Path.of(str(selected.state.get("directory")));
                    long total = Math.max(num(selected.state.get("size"), 0), num(selected.state.get("source_size"), 0));
                    if ("torrent".equals(selected.state.get("kind"))) availableSpace(directory, total);
                    else if (!Files.exists(ownedPart(selected), LinkOption.NOFOLLOW_LINKS)) availableSpace(directory, total);
                }
                enqueue(selected);
            }
            else throw new IllegalArgumentException("Неизвестное действие с загрузкой");
            return selected.snapshot();
        }
        Map<String,Object> state() {
            List<Object> result = new ArrayList<>();
            jobs.values().stream().sorted(Comparator.comparing(job -> str(job.state.get("created_at")))).forEach(job -> result.add(job.snapshot()));
            return Map.of("jobs", result);
        }

        void shutdown() {
            List<Job> saved;
            synchronized (this) { closing = true; saved = new ArrayList<>(jobs.values()); }
            queueTimer.shutdownNow();events.close();uploads.shutdown();browserDownload=null;
            for (Job job : saved) try { job.stopForShutdown(); }
            catch (Exception error) { AppLog.error("Не удалось сохранить задание: %s", DiagnosticText.arg(error.getMessage())); }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            for (Job job : saved) {
                List<Thread> threads = new ArrayList<>(job.routeThreads);
                Thread running = job.thread;
                if (running != null) threads.add(running);
                for (Thread thread : threads) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    try { thread.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining))); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); return; }
                }
                try { synchronized (job) { job.persist(); } }
                catch (Exception error) { AppLog.error("Не удалось сохранить задание при остановке: %s", DiagnosticText.arg(error.getMessage())); }
            }
        }
    }

    static boolean deleteFilesRequested(String query) {
        boolean deleteFiles = false, found = false;
        if (query != null) for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            String key = URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
            if (!"delete_files".equals(key)) continue;
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8) : "";
            if (found || (!value.equals("true") && !value.equals("false")))
                throw new IllegalArgumentException("Параметр delete_files должен быть указан один раз: true или false");
            deleteFiles = value.equals("true"); found = true;
        }
        return deleteFiles;
    }
    static void json(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) { stream.write(bytes); }
    }
    static Map<String,Object> jobRequest(HttpExchange exchange)throws IOException {
        Map<String,Object> value=request(exchange);String key=exchange.getRequestHeaders().getFirst("Idempotency-Key");
        if(key!=null){if(value.containsKey("idempotency_key")&&!key.equals(value.get("idempotency_key")))throw new IllegalArgumentException("Conflicting idempotency keys");value.put("idempotency_key",key);}return value;
    }
    static Map<String,Object> request(HttpExchange exchange) throws IOException {
        String header=exchange.getRequestHeaders().getFirst("Content-Length");
        long declared=header==null?-1:Long.parseLong(header);if(declared>1048576||declared< -1)throw new IllegalArgumentException("Слишком большой запрос");
        byte[] bytes=exchange.getRequestBody().readNBytes(1048577);if(bytes.length>1048576||(declared>=0&&bytes.length!=declared))throw new IllegalArgumentException("Invalid request size");
        Object parsed=Json.parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));if(!(parsed instanceof Map<?,?>))throw new IllegalArgumentException("Expected a JSON object");return obj(parsed);
    }
    static Map<String,Object> chooseDirectory(String initial) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("На этом компьютере нет графического рабочего стола; введите путь вручную");
        AtomicReference<String> chosen = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JFileChooser picker = directoryPicker(initial);
            if (picker.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                chosen.set(picker.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        });
        return chosen.get() == null ? Map.of("cancelled", true) : Map.of("path", chosen.get());
    }
    /** The API uses this same configured chooser; tests can inspect it without opening a modal dialog. */
    static JFileChooser directoryPicker(String initial) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { }
        I18n.installSwingDefaults();
        JFileChooser picker = new JFileChooser();
        picker.setLocale(I18n.locale());
        picker.setDialogTitle(I18n.t("Выберите папку сохранения"));
        picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        picker.setAcceptAllFileFilterUsed(false);
        ClipboardSupport.install(picker);
        if (!initial.isBlank()) {
            Path path = Path.of(initial).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) picker.setCurrentDirectory(path.toFile());
        }
        return picker;
    }
    static void handle(HttpExchange exchange, Manager manager, SingleInstance.Guard instance, Runnable stopApplication) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            String host = str(exchange.getRequestHeaders().getFirst("Host")).toLowerCase(Locale.ROOT);
            int localPort = exchange.getLocalAddress().getPort();
            Set<String> localHosts = Set.of("127.0.0.1:" + localPort, "localhost:" + localPort, "[::1]:" + localPort);
            if (!localHosts.contains(host)) { json(exchange, 403, Map.of("error", "Недопустимый Host")); return; }
            // Only this narrow opt-in endpoint accepts our fixed extension origin.
            if(path.startsWith("/api/v1/browser/")){manager.browser.handle(exchange,manager);return;}
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null && !Set.of("http://127.0.0.1:" + localPort,
                                        "http://localhost:" + localPort, "http://[::1]:" + localPort).contains(origin.toLowerCase(Locale.ROOT))) {
                json(exchange, 403, Map.of("error", "Недопустимый Origin")); return;
            }
            if ((path.equals("/") || path.equals("/index.html")) && method.equals("GET")) {
                json(exchange, 200, Map.of("application", Brand.NAME, "interface", "desktop", "version", VERSION, "api", "/api/v1"));
                return;
            }
            if (path.equals("/api/v1/instance") && method.equals("GET")) {
                json(exchange, 200, instance.instanceInfo()); return;
            }
            if (path.equals("/api/v1/instance/stop") && method.equals("POST")) {
                String supplied = exchange.getRequestHeaders().getFirst("X-Instance-Key");
                if (supplied == null || !MessageDigest.isEqual(instance.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    json(exchange, 403, Map.of("error", "Недопустимый запрос остановки")); return;
                }
                json(exchange, 202, Map.of("ok", true)); stopApplication.run(); return;
            }
            if (path.equals("/api/state") || path.equals("/api/v1/jobs")) {
                if (method.equals("GET")) { json(exchange, 200, manager.state()); return; }
                if (method.equals("POST") && path.equals("/api/v1/jobs")) { json(exchange, 201, manager.create(jobRequest(exchange))); return; }
            }
            if (path.equals("/api/jobs") && method.equals("POST")) { json(exchange, 201, manager.create(jobRequest(exchange))); return; }
            if (path.equals("/api/v1/health") && method.equals("GET")) { json(exchange, 200, Map.of("ok", true, "version", VERSION)); return; }
            if(path.equals("/api/v1/site-profiles")) {
                if(method.equals("GET")){json(exchange,200,Map.of("profiles",SiteProfiles.listPublic()));return;}
                if(method.equals("POST")||method.equals("PUT")){json(exchange,200,SiteProfiles.save(request(exchange)));return;}
            }
            if(path.matches("/api/v1/site-profiles/[a-fA-F0-9-]{36}")&&method.equals("DELETE")){SiteProfiles.delete(path.substring(path.lastIndexOf('/')+1));json(exchange,200,Map.of("ok",true));return;}
            if(path.equals("/api/v1/uploads")) {
                if(method.equals("GET")){json(exchange,200,manager.uploads.state());return;}
                if(method.equals("POST")){json(exchange,201,manager.uploads.create(SiteProfiles.apply(request(exchange))));return;}
            }
            Matcher upload=Pattern.compile("/api/v1/uploads/([a-f0-9]{12})(?:/(pause|resume))?").matcher(path);
            if(upload.matches()) {
                String id=upload.group(1),action=upload.group(2);
                if(action==null&&method.equals("GET")){json(exchange,200,manager.uploads.snapshot(id));return;}
                if(action==null&&method.equals("DELETE")){json(exchange,200,manager.uploads.remove(id));return;}
                if(action!=null&&method.equals("POST")){json(exchange,200,action.equals("pause")?manager.uploads.pause(id):manager.uploads.resume(id));return;}
            }
            if(path.equals("/api/v1/remote/list")&&method.equals("POST")) {
                Map<String,Object> input=SiteProfiles.apply(request(exchange));
                Map<String,Object> route=input.get("route") instanceof Map<?,?>?obj(input.get("route")):manager.firstSourceRoute(input);
                json(exchange,200,SourceTransfer.listRemote(route,str(input.get("url"))));return;
            }
            if(path.equals("/api/v1/archives/preview")&&method.equals("POST")){json(exchange,200,ArchivePreview.inspect(request(exchange)));return;}
            if (path.equals("/api/v1/sources/inspect") && method.equals("POST")) {
                Map<String,Object> input = request(exchange);
                json(exchange, 200, manager.inspectSource(input)); return;
            }
            if(path.equals("/api/v1/sources/auth")||path.equals("/api/v1/source-auth")) {
                if(method.equals("GET")){json(exchange,200,Map.of("sources",SourceAuth.listPublic()));return;}
                if(method.equals("PUT")||method.equals("POST")){json(exchange,200,SourceAuth.save(request(exchange)));return;}
            }
            if(path.matches("/api/v1/(?:sources/auth|source-auth)/[a-f0-9-]{36}")&&method.equals("DELETE")){SourceAuth.delete(path.substring(path.lastIndexOf('/')+1));json(exchange,200,Map.of("ok",true));return;}
            if (path.equals("/api/v1/sources/download") && method.equals("POST")) {
                json(exchange, 201, manager.createFromSource(request(exchange))); return;
            }
            if (path.equals("/api/v1/network")) {
                if (method.equals("GET")) { json(exchange, 200, GatewayNetwork.settings()); return; }
                if (method.equals("POST")) {
                    Map<String,Object> input = request(exchange);
                    List<String> gateways = new ArrayList<>();
                    for (Object value : arr(input.get("gateways"))) gateways.add(str(value));
                    manager.configureNetwork(str(input.get("virtual_ip")), gateways);
                    json(exchange, 200, GatewayNetwork.settings()); return;
                }
            }
            if (path.equals("/api/v1/gateways/speed") && method.equals("POST")) {
                Map<String,Object> input = request(exchange);
                json(exchange, 200, SpeedTest.run(Map.of("type", "gateway", "gateway", str(input.get("gateway")),
                        "name", "Шлюз " + str(input.get("gateway"))), str(input.get("url")))); return;
            }
            if (path.equals("/api/v1/dialogs/directory") && method.equals("POST")) {
                json(exchange, 200, chooseDirectory(str(request(exchange).get("initial")))); return;
            }
            if (path.equals("/api/route-test") || path.equals("/api/v1/routes/test")) {
                Map<String,Object> route;
                if (method.equals("POST")) route = request(exchange);
                else { json(exchange, 405, Map.of("error", "Method not allowed")); return; }
                json(exchange, 200, Transport.probe(route)); return;
            }
            if (path.equals("/api/v1/routes/speed") && method.equals("POST")) {
                Map<String,Object> input = request(exchange);
                Map<String,Object> route = input.get("route") instanceof Map<?,?> ? obj(input.get("route")) : input;
                json(exchange, 200, SpeedTest.run(route, str(input.get("url")))); return;
            }
            if (path.equals("/api/v1/routes/discover") && method.equals("GET")) {
                json(exchange, 200, Map.of("gateways", WorkerDiscovery.find(2000))); return;
            }
            if (path.equals("/api/v1/routes/inspect") && method.equals("POST")) {
                Map<String,Object> input = request(exchange);
                json(exchange, 200, GatewayStatus.inspect(str(input.get("gateway")), str(input.get("url")))); return;
            }
            if(path.equals("/api/v1/queue")) {
                if(method.equals("GET")){json(exchange,200,manager.queueSettings());return;}
                if(method.equals("PUT")||method.equals("POST")){json(exchange,200,manager.updateQueueSettings(request(exchange)));return;}
            }
            if(path.equals("/api/v1/jobs/batch")&&method.equals("POST")) {
                Map<String,Object> input=request(exchange);if(!(input.get("jobs") instanceof List<?> raw))throw new IllegalArgumentException("Batch jobs must be a list");
                List<Map<String,Object>> jobs=new ArrayList<>();for(Object item:raw){if(!(item instanceof Map<?,?>))throw new IllegalArgumentException("Invalid batch job");jobs.add(obj(item));}
                json(exchange,200,manager.batchCreate(jobs));return;
            }
            if(path.equals("/api/v1/events")&&method.equals("GET")){try{manager.events.serve(exchange,manager.state());}catch(IOException disconnected){exchange.close();}return;}
            String normal = path.startsWith("/api/v1") ? path.substring(7) : path.startsWith("/api") ? path.substring(4) : path;
            Matcher action = JOB_ACTION.matcher(normal);
            if (action.matches() && method.equals("POST")) {
                if (manager.closing) throw new IllegalStateException("Приложение завершает работу");
                json(exchange, 200, manager.action(action.group(1), action.group(2))); return;
            }
            Matcher options = JOB_OPTIONS.matcher(normal);
            if(options.matches() && (method.equals("PUT")||method.equals("POST"))){json(exchange,200,manager.updateJobOptions(options.group(1),request(exchange)));return;}
            Matcher routes = JOB_ROUTES.matcher(normal);
            if (routes.matches() && (method.equals("PUT") || method.equals("POST"))) {
                Map<String,Object> input = request(exchange);
                if (!(input.get("routes") instanceof List<?>)) throw new IOException("Некорректный список шлюзов загрузки");
                json(exchange, 200, manager.updateJobRoutes(routes.group(1), arr(input.get("routes")))); return;
            }
            Matcher directory = JOB_DIRECTORY.matcher(normal);
            if (directory.matches() && method.equals("POST")) {
                json(exchange, 200, manager.changeDirectory(directory.group(1), str(request(exchange).get("directory")))); return;
            }
            Matcher single = JOB_PATH.matcher(normal);
            if (single.matches() && method.equals("GET")) { json(exchange, 200, manager.job(single.group(1)).snapshot()); return; }
            if (single.matches() && method.equals("DELETE")) { json(exchange, 200, manager.remove(single.group(1), deleteFilesRequested(exchange.getRequestURI().getRawQuery()))); return; }
            json(exchange, 404, Map.of("error", "Не найдено"));
        } catch (Exception error) {
            json(exchange, 400, Map.of("error", str(error.getMessage())));
        }
    }

    /** Java 17 on Windows otherwise inherits the console code page for redirected diagnostics. */
    static synchronized void configureUtf8Console() {
        if (utf8ConsoleConfigured) return;
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        utf8ConsoleConfigured = true;
    }
    private static boolean utf8ConsoleConfigured;

    public static void main(String[] args) {
        configureUtf8Console();
        boolean showError = !Arrays.asList(args).contains("--worker") && !Arrays.asList(args).contains("--headless") && !Arrays.asList(args).contains("--no-browser");
        try { start(args); }
        catch (Exception error) {
            restoreStartupLanguage(args);
            String message = error instanceof BindException ? "Порт занят другим приложением. Укажите другой --port." : str(error.getMessage());
            if (message.isEmpty()) message = error.toString();
            AppLog.error("Не удалось запустить MultiRoute Resume: %s", DiagnosticText.arg(message));
            if (showError && !java.awt.GraphicsEnvironment.isHeadless()) {
                final String text = DiagnosticText.render(message);
                try { SwingUtilities.invokeAndWait(() -> javax.swing.JOptionPane.showMessageDialog(null, text,
                        I18n.t("MultiRoute Resume — ошибка запуска"), javax.swing.JOptionPane.ERROR_MESSAGE)); }
                catch (Exception ignored) { }
            }
            System.exit(1);
        }
    }

    /** A failed pre-guard launch may read its language setting, but never opens or rewrites logs. */
    static void restoreStartupLanguage(String[] args) {
        if (Arrays.asList(args).contains("--worker")) return;
        try {
            Path launch = Path.of("").toAbsolutePath().normalize();
            Path directory = DesktopLauncher.nativeDesktop() ? DesktopLauncher.userRoot().resolve("data") : launch.resolve("data"); boolean explicit = false;
            for (int i = 0; i + 1 < args.length; i++) if (args[i].equals("--data-dir")) {
                directory = launch.resolve(Path.of(args[i + 1])).normalize(); explicit = true; break;
            }
            Path profile = (DesktopLauncher.nativeDesktop() ? DesktopLauncher.userRoot() : launch).resolve("data-location.config");
            if (!explicit && Files.isRegularFile(profile) && Files.size(profile) < 65536) {
                String saved = Files.readString(profile, StandardCharsets.UTF_8).trim();
                if (!saved.isEmpty()) directory = launch.resolve(Path.of(saved)).normalize();
            }
            Path settings = directory.resolve("desktop-settings.config");
            if (!Files.isRegularFile(settings) || Files.size(settings) > 1024 * 1024) return;
            Object parsed = Json.parse(Files.readString(settings, StandardCharsets.UTF_8));
            if (parsed instanceof Map<?,?> saved && saved.get("language") instanceof String language)
                I18n.setLanguage(language);
        } catch (Exception ignored) { /* startup's original failure remains the reported error */ }
    }

    static void start(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--worker")) { Worker.main(Arrays.copyOfRange(args, 1, args.length)); return; }
        String bind = "127.0.0.1"; int port = 8765; Path data = DesktopLauncher.nativeDesktop() ? DesktopLauncher.userRoot().resolve("data") : Path.of("data"); boolean desktop = true, explicitData = false;
        for (int i = 0; i < args.length; i++) switch (args[i]) {
            case "--bind" -> bind = args[++i];
            case "--port" -> port = Integer.parseInt(args[++i]);
            case "--data-dir" -> { data = Path.of(args[++i]); explicitData = true; }
            case "--headless", "--no-browser" -> desktop = false;
            default -> throw new IllegalArgumentException("Неизвестный параметр: " + args[i]);
        }
        if (!bind.equals("127.0.0.1") && !bind.equals("::1") && !bind.equals("localhost"))
            throw new IllegalArgumentException("Контроллер разрешён только на localhost; API не имеет удалённой аутентификации");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Порт должен быть от 1 до 65535");
        if (desktop && java.awt.GraphicsEnvironment.isHeadless())
            throw new IllegalArgumentException("Для окна приложения нужен графический рабочий стол; используйте --headless для запуска только API");
        Path launchDirectory = Path.of("").toAbsolutePath().normalize();
        Path profile = (DesktopLauncher.nativeDesktop() ? DesktopLauncher.userRoot() : launchDirectory).resolve("data-location.config");
        if (DesktopLauncher.nativeDesktop()) Files.createDirectories(DesktopLauncher.userRoot());
        if (!explicitData && Files.isRegularFile(profile)) {
            String saved = Files.readString(profile, StandardCharsets.UTF_8).trim();
            if (!saved.isEmpty()) data = launchDirectory.resolve(Path.of(saved)).normalize();
        }
        SingleInstance.Guard instance = SingleInstance.acquire(data.toAbsolutePath().normalize(), bind, port, !explicitData);
        HttpServer server = null; Manager manager = null;
        ExecutorService requests = Executors.newCachedThreadPool();
        try {
            Path desktopSettings = instance.dataDir().resolve("desktop-settings.config");
            if (Files.isRegularFile(desktopSettings)) {
                try { I18n.setLanguage(str(obj(Json.parse(Files.readString(desktopSettings, StandardCharsets.UTF_8))).get("language"))); }
                catch (IOException | IllegalArgumentException invalidSettings) { /* the UI can repair its settings */ }
            }
            AppLog.initialize(instance.dataDir());
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
            if (!explicitData && !instance.dataDir().equals(launchDirectory.resolve("data").normalize())) {
                Path location;
                try { location = launchDirectory.relativize(instance.dataDir()); }
                catch (IllegalArgumentException differentDrive) { location = instance.dataDir(); }
                Files.writeString(profile, location.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            GatewayNetwork.initialize(instance.dataDir());
            SourceAuth.configure(instance.dataDir());
            SiteProfiles.configure(instance.dataDir());
            manager = new Manager(instance.dataDir());
            final Manager active = manager;
            final HttpServer listening = server;
            AtomicBoolean stopping = new AtomicBoolean();
            Runnable shutdown = () -> {
                if (!stopping.compareAndSet(false, true)) return;
                AppLog.info("Приложение завершает работу");
                try { active.shutdown(); }
                finally {
                    listening.stop(0); requests.shutdownNow();
                    GatewayNetwork.close();
                    SwingUtilities.invokeLater(() -> { for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose(); });
                    AppLog.close();
                    try { instance.close(); } catch (Exception error) { AppLog.error("Не удалось закрыть экземпляр приложения: %s", DiagnosticText.arg(error.getMessage())); }
                }
            };
            Runnable exit = () -> {
                Thread thread = new Thread(() -> { shutdown.run(); System.exit(0); }, "controller-close");
                thread.start();
            };
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "controller-save-on-exit"));
            server.createContext("/", exchange -> handle(exchange, active, instance, exit));
            server.setExecutor(requests); server.start();
            AppLog.info("MultiRoute Resume %s; API http://%s:%d; данные: %s", VERSION, bind, port, instance.dataDir());
            if (desktop) DesktopUI.open(manager, exit);
        } catch (Exception error) {
            AppLog.error("Не удалось запустить MultiRoute Resume: %s", DiagnosticText.arg(error.getMessage()));
            if (manager != null) manager.shutdown();
            if (server != null) server.stop(0);
            GatewayNetwork.close(); requests.shutdownNow();
            AppLog.close(); instance.close();
            throw error;
        }
    }
}
