package multiroute;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded, read-only HTTP Range throughput test over exactly the download transport. */
public final class SpeedTest {
    public static final String DEFAULT_URL = "https://speedtest.selectel.ru/10MB";
    public static final long MAX_BYTES = 16L * 1024 * 1024;
    public static final long DURATION_LIMIT_MS = 5_000;
    private static final long HARD_LIMIT_MS = 14_000;
    private static final long REQUEST_BYTES = 4L * 1024 * 1024;
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1, daemon("speed-limit"));

    private SpeedTest() { }

    /** Never writes downloaded bytes to disk and never touches jobs or checksums. */
    public static Map<String,Object> run(Map<String,Object> route, String url) throws IOException, InterruptedException {
        Objects.requireNonNull(route, "route");
        String requested = url == null || url.isBlank() ? DEFAULT_URL : url.trim();
        String target = TorrentSource.isTorrentSource(requested) ? DEFAULT_URL : HubSource.isHubUrl(requested) ?
                (HubSource.isListingUrl(requested) ? DEFAULT_URL : HubSource.fileUrl(requested)) : requested;
        // ARP conflict detection is a one-time preparation, outside the transfer's time budget.
        if ("gateway".equals(route.get("type"))) GatewayNetwork.endpoint(route);
        Probe probe = new Probe();
        ExecutorService executor = Executors.newSingleThreadExecutor(daemon("speed-transfer"));
        Future<Map<String,Object>> future = executor.submit(() -> measure(route, target, probe));
        try {
            return future.get(HARD_LIMIT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException error) {
            probe.abort();
            future.cancel(true);
            throw new IOException("Тест скорости превысил общий лимит 14 секунд", error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof IOException io) throw io;
            if (cause instanceof InterruptedException interrupted) throw interrupted;
            throw new IOException("Ошибка теста скорости: " + cause.getMessage(), cause);
        } catch (InterruptedException error) {
            probe.abort();
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw error;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Map<String,Object> measure(Map<String,Object> route, String target, Probe probe)
            throws IOException, InterruptedException {
        probe.owner.set(Thread.currentThread());
        String gateway = route.get("gateway") instanceof String value ? value : "";
        if ("worker".equals(route.get("type")) && !gateway.isBlank()) {
            Map<String,Object> check = Transport.probe(route);
            if (!Boolean.TRUE.equals(check.get("ok")) ||
                !gateway.equals(check.get("expected_gateway")) ||
                !Boolean.TRUE.equals(check.get("gateway_match")))
                throw new IOException("Worker не подтвердил выход через выбранный шлюз " + gateway);
        }
        long total;
        String etag, modified;
        HttpResponse<InputStream> metadata = Transport.open(route, target, 0, 0, null, null);
        probe.register(metadata.body());
        try {
            total = Transport.validate(metadata, 0, 0, null, null, null);
            int first = metadata.body().read();
            if (first < 0) throw new EOFException("Пробный сегмент оборвался");
            if (metadata.body().read() >= 0) throw new IOException("Пробный сегмент содержит лишние байты");
            String candidate = metadata.headers().firstValue("ETag").orElse("");
            etag = candidate.startsWith("W/") ? "" : candidate;
            modified = etag.isEmpty() ? metadata.headers().firstValue("Last-Modified").orElse("") : "";
        } finally { probe.closeActive(); }

        long started = System.nanoTime();
        long durationDeadline = started + TimeUnit.MILLISECONDS.toNanos(DURATION_LIMIT_MS);
        AtomicBoolean durationReached = new AtomicBoolean();
        ScheduledFuture<?> timer = TIMER.schedule(() -> {
            durationReached.set(true);
            probe.abort();
        }, DURATION_LIMIT_MS, TimeUnit.MILLISECONDS);
        long bytes = 0;
        long offset = 0;
        boolean limitReached = false;
        try {
            while (offset < total && bytes < MAX_BYTES) {
                if (durationReached.get() || System.nanoTime() >= durationDeadline) {
                    limitReached = true;
                    break;
                }
                long end = Math.min(total - 1, Math.min(offset + REQUEST_BYTES - 1, offset + (MAX_BYTES - bytes) - 1));
                HttpResponse<InputStream> response;
                try { response = Transport.open(route, target, offset, end, etag, modified); }
                catch (IOException | InterruptedException error) {
                    if (durationReached.get() && bytes > 0) { limitReached = true; break; }
                    throw error;
                }
                try { probe.register(response.body()); }
                catch (IOException error) {
                    if (durationReached.get() && bytes > 0) { limitReached = true; break; }
                    throw error;
                }
                try {
                    Transport.validate(response, offset, end, total, etag, modified);
                    long remaining = end - offset + 1;
                    byte[] buffer = new byte[64 * 1024];
                    while (remaining > 0) {
                        if (durationReached.get()) { limitReached = true; break; }
                        int count;
                        try { count = response.body().read(buffer, 0, (int) Math.min(buffer.length, remaining)); }
                        catch (IOException error) {
                            if (durationReached.get() && bytes > 0) { limitReached = true; break; }
                            throw error;
                        }
                        if (count < 0) throw new EOFException("Сегмент теста скорости оборвался до конца диапазона");
                        if (count == 0) continue;
                        bytes += count;
                        remaining -= count;
                    }
                    if (limitReached) break;
                    int extra;
                    try { extra = response.body().read(); }
                    catch (IOException error) {
                        if (durationReached.get() && bytes > 0) { limitReached = true; break; }
                        throw error;
                    }
                    if (extra >= 0) throw new IOException("Сегмент содержит лишние байты");
                    offset = end + 1;
                } finally { probe.closeActive(); }
            }
            if (bytes <= 0) throw new IOException("Тест скорости не получил ни одного байта данных");
            if (bytes >= MAX_BYTES || durationReached.get()) limitReached = true;
            long elapsedNanos = Math.max(1, System.nanoTime() - started);
            long elapsedMs = Math.max(1, TimeUnit.NANOSECONDS.toMillis(elapsedNanos));
            double seconds = elapsedNanos / 1_000_000_000.0;
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("mbps", bytes * 8.0 / seconds / 1_000_000.0);
            result.put("mib_per_second", bytes / seconds / (1024.0 * 1024.0));
            result.put("bytes", bytes);
            result.put("elapsed_ms", elapsedMs);
            result.put("url", target);
            result.put("route_name", String.valueOf(route.getOrDefault("name", "")));
            result.put("gateway", gateway);
            result.put("limit_reached", limitReached);
            result.put("max_bytes", MAX_BYTES);
            result.put("duration_limit_ms", DURATION_LIMIT_MS);
            return result;
        } finally {
            timer.cancel(false);
            probe.closeActive();
        }
    }

    private static ThreadFactory daemon(String name) {
        return task -> { Thread thread = new Thread(task, name); thread.setDaemon(true); return thread; };
    }
    private static final class Probe {
        final AtomicReference<Thread> owner = new AtomicReference<>();
        final AtomicReference<InputStream> active = new AtomicReference<>();
        final AtomicBoolean aborted = new AtomicBoolean();
        void register(InputStream stream) throws IOException {
            if (aborted.get()) { closeAsync(stream); throw new IOException("Тест скорости прерван по лимиту времени"); }
            active.set(stream);
            if (aborted.get()) {
                InputStream current = active.getAndSet(null);
                if (current != null) closeAsync(current);
                throw new IOException("Тест скорости прерван по лимиту времени");
            }
        }
        void abort() {
            aborted.set(true);
            Thread thread = owner.get();
            if (thread != null) thread.interrupt();
            InputStream stream = active.getAndSet(null);
            if (stream != null) closeAsync(stream);
        }
        void closeActive() throws IOException {
            InputStream stream = active.getAndSet(null);
            if (stream != null) {
                if (aborted.get()) closeAsync(stream);
                else stream.close();
            }
        }
        private static void closeAsync(InputStream stream) {
            Thread closer = new Thread(() -> { try { stream.close(); } catch (IOException ignored) { } }, "speed-close");
            closer.setDaemon(true); closer.start();
        }
    }
}
