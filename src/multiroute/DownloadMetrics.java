package multiroute;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

/** Monotonic active time and a three-second payload receive-rate window. Never a persisted runtime object. */
final class DownloadMetrics {
    private static final long WINDOW = TimeUnit.SECONDS.toNanos(3);
    private static final long BUCKET = TimeUnit.MILLISECONDS.toNanos(100);
    private final LongSupplier clock;
    private final Map<String,Counter> counters = new LinkedHashMap<>();
    private final Map<Integer,Long> inflight = new HashMap<>();
    private long elapsedBase, started, received;
    private boolean active;
    private static final class Sample {
        final long time;
        long bytes;
        Sample(long time, long bytes) { this.time = time; this.bytes = bytes; }
    }
    private static final class Counter {
        long received;
        final Deque<Sample> samples = new ArrayDeque<>();
    }
    DownloadMetrics(Map<String,Object> state) { this(state, System::nanoTime); }
    DownloadMetrics(Map<String,Object> state, LongSupplier clock) {
        this.clock = clock;
        elapsedBase = Math.max(0, Main.num(state.get("elapsed_ms"), 0));
        long routeBytes = 0;
        for (Object raw : Main.arr(state.get("routes"))) {
            Map<String,Object> route = Main.obj(raw);
            Counter counter = counter(route);
            counter.received = Math.max(0, Main.num(route.get("received_bytes"), Main.num(route.get("bytes"), 0)));
            routeBytes += counter.received;
        }
        received = Math.max(routeBytes, Math.max(0, Main.num(state.get("received_bytes"), 0)));
    }
    static String key(Map<String,Object> route) {
        return Main.str(route.get("type")) + "\n" + Main.str(route.get("gateway")) + "\n" +
                Main.str(route.get("endpoint")) + "\n" + Main.str(route.get("name"));
    }
    private Counter counter(Map<String,Object> route) { return counters.computeIfAbsent(key(route), ignored -> new Counter()); }
    synchronized void start() {
        if (active) return;
        started = clock.getAsLong(); active = true; inflight.clear();
        for (Counter counter : counters.values()) counter.samples.clear();
    }
    synchronized void stop() {
        if (active) elapsedBase += TimeUnit.NANOSECONDS.toMillis(Math.max(0, clock.getAsLong() - started));
        active = false; inflight.clear();
        for (Counter counter : counters.values()) counter.samples.clear();
    }
    synchronized boolean active() { return active; }
    synchronized long elapsed() {
        return elapsedBase + (active ? TimeUnit.NANOSECONDS.toMillis(Math.max(0, clock.getAsLong() - started)) : 0);
    }
    synchronized void receive(Map<String,Object> route, int chunk, long bytes) {
        if (bytes <= 0) return;
        add(counter(route), bytes);
        if (active) inflight.merge(chunk, bytes, Long::sum);
    }
    synchronized void endChunk(int chunk) { inflight.remove(chunk); }
    synchronized void observeRoutes(List<Object> routes) {
        for (Object raw : routes) {
            Map<String,Object> route = Main.obj(raw);
            Counter counter = counter(route);
            long current = Math.max(0, Main.num(route.get("received_bytes"), counter.received));
            if (current > counter.received) add(counter, current - counter.received);
        }
    }
    private void add(Counter counter, long bytes) {
        counter.received += bytes; received += bytes;
        if (!active) return;
        long now = clock.getAsLong(); trim(counter, now);
        Sample last = counter.samples.peekLast();
        if (last != null && now - last.time < BUCKET) last.bytes += bytes;
        else counter.samples.addLast(new Sample(now, bytes));
    }
    private static void trim(Counter counter, long now) {
        while (!counter.samples.isEmpty() && now - counter.samples.peekFirst().time >= WINDOW) counter.samples.removeFirst();
    }
    private double rate(Counter counter, long now) {
        if (!active) return 0;
        trim(counter, now); long bytes = 0;
        for (Sample sample : counter.samples) bytes += sample.bytes;
        double seconds = Math.max(.25, Math.min(WINDOW, Math.max(0, now - started)) / 1_000_000_000.0);
        return bytes / seconds;
    }
    synchronized void checkpoint(Map<String,Object> state) {
        state.put("elapsed_ms", elapsed()); state.put("received_bytes", received);
        for (Object raw : Main.arr(state.get("routes"))) {
            Map<String,Object> route = Main.obj(raw);
            route.put("received_bytes", counter(route).received);
            route.remove("speed_bps");
        }
        state.remove("speed_bps"); state.remove("eta_seconds"); state.remove("progress_bytes");
    }
    synchronized void decorate(Map<String,Object> snapshot) {
        long now = clock.getAsLong(); double totalRate = 0;
        for (Object raw : Main.arr(snapshot.get("routes"))) {
            Map<String,Object> route = Main.obj(raw); Counter counter = counter(route);
            double routeRate = Main.yes(route.get("enabled")) ? rate(counter, now) : 0;
            route.put("received_bytes", counter.received); route.put("speed_bps", routeRate); totalRate += routeRate;
        }
        long progress = Math.max(0, Main.num(snapshot.get("downloaded"), 0));
        if (active) for (long bytes : inflight.values()) progress += bytes;
        long size = Math.max(0, Main.num(snapshot.get("size"), 0));
        if (size > 0) progress = Math.min(size, progress);
        snapshot.put("progress_bytes", progress); snapshot.put("elapsed_ms", elapsed());
        snapshot.put("received_bytes", received); snapshot.put("speed_bps", totalRate);
        Object eta = null;
        if ("complete".equals(snapshot.get("status"))) eta = 0L;
        else if (active && size > 0 && totalRate > 0) eta = (long)Math.ceil(Math.max(0, size - progress) / totalRate);
        snapshot.put("eta_seconds", eta);
    }
}
