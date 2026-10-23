package multiroute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Payload accounting, independent of UI history and translated at the presentation boundary.
 * Entries contain no source URL, path, credentials or file content. Disk writes run on AppLog's
 * bounded writer, never on a payload callback. Four 4 MiB files bound long-running retention. */
public final class TransferJournal {
    static final long PROGRESS_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(12);
    static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    static final int BACKUPS = 3;
    static final String FILE_NAME = "download-transfers.jsonl";
    static final String START = "Download attempt started: job=%s gateway=%s range=%s saved_bytes=%d wire_total_bytes=%d resumable=%s";
    static final String PROGRESS = "Download attempt progress: job=%s gateway=%s range=%s attempt_bytes=%d saved_bytes=%d wire_total_bytes=%d";
    static final String END = "Download attempt ended: job=%s gateway=%s range=%s status=%s attempt_bytes=%d saved_bytes=%d wire_total_bytes=%d elapsed_ms=%d reason=%s";
    static final String PRESERVED = "Download prefix preserved: job=%s gateway=%s saved_bytes=%d wire_total_bytes=%d reason=%s";
    static final String RESTART = "Download restarted from zero: job=%s gateway=%s discarded_bytes=%d wire_total_bytes=%d reason=%s";
    private static final Set<String> STATUSES = Set.of("completed", "failed", "paused", "retry", "interrupted");

    private TransferJournal() { }

    /** A negative range end denotes unknown length. Byte counters always refer to payload bytes. */
    public static Attempt begin(String jobId, String gateway, long start, long end,
            long persistedAtStart, long jobWireAtStart, boolean resumable) {
        return begin(jobId, gateway, start, end, persistedAtStart, jobWireAtStart, resumable, System::nanoTime);
    }

    static Attempt begin(String jobId, String gateway, long start, long end,
            long persistedAtStart, long jobWireAtStart, boolean resumable, LongSupplier clock) {
        return new Attempt(jobId, gateway, start, end, persistedAtStart, jobWireAtStart, resumable, clock);
    }

    public static void preserved(String jobId, String gateway, long prefixBytes, long jobWireTotal, String reason) {
        Map<String,Object> event = base("prefix_preserved", safe(jobId), safe(gateway), UUID.randomUUID().toString());
        event.put("persisted_bytes", nonnegative(prefixBytes)); event.put("wire_total_bytes", nonnegative(jobWireTotal));
        event.put("discarded_bytes", 0L); event.put("reason", safe(reason));
        emit(event, PRESERVED, safe(jobId), safe(gateway), nonnegative(prefixBytes), nonnegative(jobWireTotal), DiagnosticText.arg(safe(reason)));
    }

    /** Invoke before an explicitly authorized reset. This method never modifies a download file. */
    public static void restart(String jobId, String gateway, long discardedPrefixBytes, long jobWireTotal, String reason) {
        Map<String,Object> event = base("prefix_restarted", safe(jobId), safe(gateway), UUID.randomUUID().toString());
        event.put("persisted_bytes", 0L); event.put("wire_total_bytes", nonnegative(jobWireTotal));
        event.put("discarded_bytes", nonnegative(discardedPrefixBytes)); event.put("reason", safe(reason));
        emit(event, RESTART, safe(jobId), safe(gateway), nonnegative(discardedPrefixBytes), nonnegative(jobWireTotal), DiagnosticText.arg(safe(reason)));
    }

    public static final class Attempt {
        private final String jobId, gateway, id;
        private final long start, end, wireAtStart, started;
        private final boolean resumable;
        private final LongSupplier clock;
        private long lastSummary, attemptBytes;
        private boolean finished;

        private Attempt(String jobId, String gateway, long start, long end, long persisted,
                long wire, boolean resumable, LongSupplier clock) {
            this.jobId = safe(jobId); this.gateway = safe(gateway); this.id = UUID.randomUUID().toString();
            this.start = nonnegative(start); this.end = end < this.start ? -1 : end;
            this.wireAtStart = nonnegative(wire); this.resumable = resumable; this.clock = clock;
            this.started = clock.getAsLong(); this.lastSummary = started;
            Map<String,Object> event = event("attempt_started", 0, persisted, wireAtStart);
            emit(event, START, this.jobId, this.gateway, range(), nonnegative(persisted), wireAtStart,
                    AppLog.arg(resumable ? "Resume supported" : "Resume unavailable"));
        }

        /** Caller supplies cumulative bytes for this attempt, not a delta for each buffer. */
        public synchronized void progress(long received, long persisted) {
            progress(received, persisted, add(wireAtStart, nonnegative(received)));
        }

        /** Parallel transfers supply the job's current wire total, including other active attempts. */
        public synchronized void progress(long received, long persisted, long jobWireTotal) {
            if (finished) return;
            attemptBytes = Math.max(attemptBytes, nonnegative(received));
            long now = clock.getAsLong();
            if (now - lastSummary < PROGRESS_INTERVAL_NANOS) return;
            lastSummary = now;
            long total = Math.max(nonnegative(jobWireTotal), add(wireAtStart, attemptBytes));
            Map<String,Object> event = event("attempt_progress", attemptBytes, persisted, total);
            emit(event, PROGRESS, jobId, gateway, range(), attemptBytes, nonnegative(persisted), event.get("wire_total_bytes"));
        }

        /** Exactly one terminal event. A retry retains its previous wire total and saved prefix. */
        public synchronized void finish(String status, String reason, long received, long persisted, long jobWireTotal) {
            if (finished) return;
            finished = true; attemptBytes = Math.max(attemptBytes, nonnegative(received));
            String outcome = STATUSES.contains(status == null ? "" : status) ? status : "failed";
            long total = Math.max(nonnegative(jobWireTotal), add(wireAtStart, attemptBytes));
            Map<String,Object> event = event("attempt_" + outcome, attemptBytes, persisted, total);
            event.put("status", outcome); event.put("reason", safe(reason));
            emit(event, END, jobId, gateway, range(), AppLog.arg(statusTemplate(outcome)), attemptBytes,
                    nonnegative(persisted), total, event.get("elapsed_ms"), DiagnosticText.arg(safe(reason)));
        }

        private String range() { return start + "-" + (end < 0 ? "?" : end); }
        private Map<String,Object> event(String kind, long received, long persisted, long wire) {
            Map<String,Object> event = base(kind, jobId, gateway, id);
            event.put("range_start", start); event.put("range_end", end); event.put("resumable", resumable);
            event.put("attempt_bytes", nonnegative(received)); event.put("persisted_bytes", nonnegative(persisted));
            event.put("wire_total_bytes", nonnegative(wire));
            event.put("elapsed_ms", Math.max(0, TimeUnit.NANOSECONDS.toMillis(clock.getAsLong() - started)));
            return event;
        }
    }

    private static String statusTemplate(String status) {
        return switch (status) {
            case "completed" -> "Transfer completed";
            case "paused" -> "Transfer paused";
            case "retry" -> "Transfer retry";
            case "interrupted" -> "Transfer interrupted";
            default -> "Transfer failed";
        };
    }
    private static Map<String,Object> base(String kind, String job, String gateway, String attempt) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("format", 1); value.put("category", "download-transfer");
        value.put("event_id", UUID.randomUUID().toString()); value.put("time", Instant.now().toString());
        value.put("event", kind); value.put("job_id", job); value.put("gateway", gateway); value.put("attempt_id", attempt);
        return value;
    }
    private static void emit(Map<String,Object> event, String template, Object... args) {
        AppLog.transferEvent(event, template, args);
    }
    private static String safe(String text) { return NetworkDiagnostics.safe(text); }
    private static long nonnegative(long number) { return Math.max(0, number); }
    private static long add(long left, long right) { return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right; }
    static boolean priority(Map<String,Object> event) { return !"attempt_progress".equals(event.get("event")); }

    /** AppLog's single writer calls this once at observation time, never when restoring UI events. */
    static void append(Path directory, Map<String,Object> event) throws IOException {
        byte[] bytes = (Json.stringify(event) + "\n").getBytes(StandardCharsets.UTF_8);
        Path file = directory.resolve(FILE_NAME); Main.safePath(file);
        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Files.size(file) + bytes.length > MAX_FILE_BYTES) {
            for (int index = BACKUPS; index >= 1; index--) {
                Path from = index == 1 ? file : directory.resolve(FILE_NAME + "." + (index - 1));
                Path to = directory.resolve(FILE_NAME + "." + index);
                Main.safePath(from); Main.safePath(to);
                if (Files.exists(from, LinkOption.NOFOLLOW_LINKS)) Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        try (FileChannel output = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            long length = output.size();
            if (length > 0) {
                ByteBuffer last = ByteBuffer.allocate(1); output.read(last, length - 1);
                if (last.array()[0] != '\n') {
                    output.position(length); output.write(ByteBuffer.wrap(new byte[]{'\n'})); length++;
                }
            }
            output.position(length); ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) output.write(buffer);
            output.force(false);
        }
    }

    static Map<String,Object> omitted(long count) {
        Map<String,Object> value = base("events_omitted", "", "", ""); value.put("count", count); return value;
    }
}
