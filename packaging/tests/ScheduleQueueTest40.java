package multiroute;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLSession;

/** Actual persistent Manager queue and real owned range workers, with no network access. */
public final class ScheduleQueueTest40 {
    private static final byte[] DATA = new byte[65536 + 99];
    private static final List<String> cases = new ArrayList<>(), starts = new CopyOnWriteArrayList<>();
    private static final AtomicInteger rangeOpens = new AtomicInteger();
    private static int checks;
    private static Path own;
    private static String sha;
    private static void check(boolean valid, String name) { checks++; if (!valid) throw new AssertionError(name); cases.add(name); }
    @FunctionalInterface private interface Task { void run() throws Exception; }
    @FunctionalInterface private interface Condition { boolean get() throws Exception; }
    private static void rejected(Task action, String name) throws Exception {
        try { action.run(); throw new AssertionError(name + " was accepted"); }
        catch (IOException | IllegalArgumentException | DateTimeException expected) { check(true, name); }
    }
    private static void await(Condition condition, String name) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!condition.get() && System.nanoTime() < until) Thread.sleep(10);
        check(condition.get(), name);
    }
    private static Instant time(int hour, int minute) {
        return LocalDate.of(2026, 10, 20).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant();
    }
    private static Map<String,Object> window(boolean open) {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalTime from = open ? now.minusMinutes(3) : now.plusMinutes(3);
        LocalTime to = open ? now.plusMinutes(3) : now.plusMinutes(4);
        return Map.of("schedule_enabled", true, "schedule_start", from.format(DateTimeFormatter.ofPattern("HH:mm")),
                "schedule_end", to.format(DateTimeFormatter.ofPattern("HH:mm")));
    }
    private record Response(URI uri, InputStream body, HttpHeaders headers) implements HttpResponse<InputStream> {
        public int statusCode() { return 206; }
        public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
    private static final class Opener implements Main.Job.HttpOpener {
        final String name;
        final AtomicInteger blocked = new AtomicInteger();
        final List<InputStream> bodies = new CopyOnWriteArrayList<>();
        volatile boolean released;
        Opener(String name) { this.name = name; }
        void release() { released = true; for (InputStream body : bodies) synchronized(body) { body.notifyAll(); } }
        public HttpResponse<InputStream> open(Map<String,Object> route, String url, long from, long to, String etag, String modified) throws Exception {
            rangeOpens.incrementAndGet();
            InputStream body = new ByteArrayInputStream(Arrays.copyOfRange(DATA, (int)from, (int)to + 1));
            if (from == 0 && to == 0) starts.add(name);
            else {
                InputStream source = body;
                body = new InputStream() {
                    boolean closed, counted;
                    public int read() throws IOException { byte[] one = new byte[1]; return read(one,0,1)<0 ? -1 : one[0]&255; }
                    public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                        if (!counted) { counted = true; blocked.incrementAndGet(); }
                        while (!released && !closed) try { wait(20); }
                        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new InterruptedIOException("Owned scheduler range cancelled"); }
                        if (closed) throw new InterruptedIOException("Owned scheduler range closed");
                        return source.read(bytes,off,len);
                    }
                    public synchronized void close() throws IOException { closed = true; notifyAll(); source.close(); }
                };
                bodies.add(body);
            }
            StringBuilder digests = new StringBuilder();
            for (String algorithm : List.of("MD5", "SHA-256", "SHA-512")) {
                if (!digests.isEmpty()) digests.append(", ");
                digests.append(algorithm.toLowerCase(Locale.ROOT)).append("=:")
                        .append(Base64.getEncoder().encodeToString(MessageDigest.getInstance(algorithm).digest(DATA))).append(':');
            }
            return new Response(URI.create(url), body, HttpHeaders.of(Map.of("Content-Range", List.of("bytes " + from + "-" + to + "/" + DATA.length),
                    "Content-Length", List.of(Long.toString(to-from+1)), "Content-Type", List.of("application/octet-stream"),
                    "ETag", List.of("\"owned-scheduler-v1\""), "Repr-Digest", List.of(digests.toString())), (a,b)->true));
        }
    }
    private static Main.Manager manager(Path directory) throws IOException {
        Main.Manager manager = new Main.Manager(directory, (url,route)->{throw new AssertionError("No source network in scheduling fixture");},
                path->Long.MAX_VALUE, (route,torrent)->{});
        manager.queueTimer.shutdownNow(); return manager;
    }
    private static Main.Job install(Main.Manager manager, Map<String,Object> snapshot, Opener opener) {
        String id = Main.str(snapshot.get("id")); Main.Job previous = manager.job(id);
        check(previous.thread == null, "Owned transport is attached before any queue worker starts");
        Main.Job job = new Main.Job(previous.state, manager.dataDir, opener, Main.HTTP_IDLE_TIMEOUT_NANOS);
        job.transferLimits = manager.queue.limits; job.events = manager.events; manager.jobs.put(id, job); return job;
    }
    private static Map<String,Object> create(Main.Manager manager, String name, int priority, long after) throws Exception {
        return manager.create(Map.of("url", "https://owned-schedule.invalid/" + URI.create("file.bin"), "filename", name,
                "directory", own.resolve("Загрузки_日本語_ä_🚀").toString(), "priority", priority, "start_after_ms", after,
                "expected_sha256", sha, "routes", List.of(Map.of("type","direct","enabled",true))));
    }
    private static void complete(Main.Job job, Opener opener) throws Exception {
        opener.release(); await(()->job.thread==null, "Actual scheduled range worker stops after full checksum verification");
        check("complete".equals(job.snapshot().get("status")) && sha.equals(job.snapshot().get("sha256")), "Scheduled job publishes only checksum-verified completion");
        check(Arrays.equals(Files.readAllBytes(Path.of(Main.str(job.state.get("directory"))).resolve(Main.str(job.state.get("filename")))), DATA),
                "Actual scheduled Unicode output contains every owned range byte");
    }
    private static void policy() throws Exception {
        QueueController queue = new QueueController(own.resolve("policy"));
        queue.update(Map.of("schedule_enabled",true,"schedule_start","09:00","schedule_end","17:00","max_active_jobs",1));
        check(!queue.allowed(time(8,59)) && queue.allowed(time(9,0)) && queue.allowed(time(16,59)) && !queue.allowed(time(17,0)),
                "Daily schedule includes its start and excludes its end in the configured system timezone");
        queue.update(Map.of("schedule_start","22:00","schedule_end","06:00"));
        check(queue.allowed(time(22,0)) && queue.allowed(time(23,59)) && queue.allowed(time(0,0)) && queue.allowed(time(5,59))
                && !queue.allowed(time(6,0)) && !queue.allowed(time(12,0)), "Overnight schedule spans midnight with exact half-open boundaries");
        queue.update(Map.of("schedule_start","11:11","schedule_end","11:11"));
        check(queue.allowed(time(0,0)) && queue.allowed(time(11,11)) && queue.allowed(time(23,59)), "Equal start and end means a full-day schedule");
        queue.update(Map.of("schedule_enabled",false,"schedule_start","09:00","schedule_end","09:01"));
        check(queue.allowed(time(3,0)) && queue.allowed(time(19,0)), "Disabled schedule imposes no time window");
        Map<String,Object> before = queue.snapshot();
        rejected(()->queue.update(Map.of("schedule_start","25:00")), "Invalid clock hour is rejected");
        rejected(()->queue.update(Map.of("schedule_end","9:01")), "Schedule rejects a non-HH:mm time");
        rejected(()->queue.update(Map.of("max_active_jobs",0)), "Queue rejects a zero active-job limit");
        check(queue.snapshot().equals(before), "Invalid policy changes leave saved and active policy unchanged");
        QueueController restored = new QueueController(own.resolve("policy"));
        check(restored.snapshot().equals(queue.snapshot()), "Daily windows and queue limit survive policy restart exactly");
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); Path requested=Path.of(args[0]).toAbsolutePath().normalize(); Files.createDirectories(requested); own=requested.toRealPath();
        for(int i=0;i<DATA.length;i++)DATA[i]=(byte)(i*13+i/43);
        sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(DATA)); AppLog.initialize(own); SourceAuth.configure(own);
        policy(); Path profile=own.resolve("manager"); Main.Manager manager=manager(profile);
        String scheduleId, manualId; long future=System.currentTimeMillis()+86400000L;
        try {
            manager.updateQueueSettings(window(false)); manager.updateQueueSettings(Map.of("max_active_jobs",1));
            Main.Job low=install(manager,create(manager,"Низкий_é_🚀.bin",-20,future),new Opener("low"));
            Main.Job high=install(manager,create(manager,"Высокий_日本語.bin",50,future),new Opener("high"));
            Main.Job middle=install(manager,create(manager,"Средний_ä.bin",10,future),new Opener("middle"));
            Main.Job later=install(manager,create(manager,"Следующий_ä_🚀.bin",5,future),new Opener("later"));
            Opener lowOpen=(Opener)low.httpOpener, highOpen=(Opener)high.httpOpener, middleOpen=(Opener)middle.httpOpener, laterOpen=(Opener)later.httpOpener;
            manager.updateJobOptions(Main.str(low.state.get("id")),Map.of("start_after_ms",0L));
            manager.updateJobOptions(Main.str(high.state.get("id")),Map.of("start_after_ms",0L));
            manager.updateJobOptions(Main.str(middle.state.get("id")),Map.of("start_after_ms",0L));
            manager.updateJobOptions(Main.str(later.state.get("id")),Map.of("start_after_ms",0L));
            check(starts.isEmpty(), "Closed daily window keeps all eligible jobs queued without opening source ranges");
            manager.updateQueueSettings(Map.of("schedule_enabled",false));
            await(()->highOpen.blocked.get()>0,"Highest-priority queued job starts first and reaches an actual range body");
            check(low.thread==null && middle.thread==null && later.thread==null && Main.num(manager.queueSettings().get("active_jobs"),0)==1,
                    "Single active-job policy leaves lower priorities waiting");
            complete(high,highOpen); manager.updateQueueSettings(Map.of());
            await(()->middleOpen.blocked.get()>0,"Middle-priority job starts after the higher priority finishes");
            manager.updateJobOptions(Main.str(low.state.get("id")),Map.of("priority",100L));
            check(middle.thread!=null && low.thread==null,"Changing a queued priority does not preempt the active verified transfer");
            complete(middle,middleOpen); manager.updateQueueSettings(Map.of());
            await(()->lowOpen.blocked.get()>0,"Changed highest queued priority receives the next available slot");
            check(later.thread==null,"Priority edit moves the formerly lowest job ahead of another waiting download");complete(low,lowOpen);
            manager.updateQueueSettings(Map.of());await(()->laterOpen.blocked.get()>0,"Remaining queued download starts only after the promoted priority finishes");complete(later,laterOpen);
            check(starts.equals(List.of("high","middle","low","later")),"Actual Manager launch order follows descending priority and a live queued priority change");
            Main.Job deferred=install(manager,create(manager,"Отложенный_日本語_🚀.bin",3,future),new Opener("deferred"));
            manager.updateQueueSettings(Map.of());
            check(deferred.thread==null&&!starts.contains("deferred"),"One-off future start never launches before its timestamp");
            manager.updateJobOptions(Main.str(deferred.state.get("id")),Map.of("start_after_ms",System.currentTimeMillis()-1));
            Opener deferredOpen=(Opener)deferred.httpOpener;await(()->deferredOpen.blocked.get()>0,"Changing start time to an elapsed timestamp admits the real queued transfer");complete(deferred,deferredOpen);

            manager.updateQueueSettings(window(false));
            Main.Job scheduled=install(manager,create(manager,"Расписание_日本語_🚀.bin",17,0),new Opener("scheduled"));scheduleId=Main.str(scheduled.state.get("id"));
            Main.Job manual=install(manager,create(manager,"Ручная-пауза_ä_🚀.bin",2,0),new Opener("manual"));manualId=Main.str(manual.state.get("id"));
            manager.action(manualId,"pause");manager.updateQueueSettings(window(true));Opener scheduledOpen=(Opener)scheduled.httpOpener;
            await(()->scheduledOpen.blocked.get()>0,"Entering allowed daily window starts a queued download");
            manager.updateQueueSettings(window(false));await(()->scheduled.thread==null,"Leaving daily window cancels the actual active range worker");
            check("queued".equals(scheduled.snapshot().get("status"))&&"schedule".equals(scheduled.snapshot().get("queue_reason")),
                    "Schedule pause remains queued with its automatic-resume reason");
            check("paused".equals(manual.snapshot().get("status"))&&manual.thread==null,"Manual pause is preserved while schedule closes");
            manager.updateQueueSettings(window(true));await(()->scheduledOpen.blocked.get()>1,"Entering allowed window resumes the same scheduled job");
            check("paused".equals(manual.snapshot().get("status"))&&manual.thread==null&&!starts.contains("manual"),
                    "Opening schedule never automatically resumes a manually paused download");
            manager.updateQueueSettings(window(false));await(()->scheduled.thread==null,"Schedule can pause a resumed real worker again");
            manager.updateJobOptions(scheduleId,Map.of("priority",73L,"start_after_ms",future));
            check(Main.num(scheduled.snapshot().get("priority"),0)==73&&Main.num(scheduled.snapshot().get("start_after_ms"),0)==future,
                    "Priority and one-off start edits apply together to a waiting scheduled download");
        } finally {manager.shutdown();}
        Main.Manager restored=manager(profile);
        try {
            check(restored.queue.snapshot().equals(new QueueController(profile).snapshot()),"Manager restart restores the exact persisted daily schedule and concurrency policy");
            Main.Job scheduled=restored.job(scheduleId),manual=restored.job(manualId);
            check("queued".equals(scheduled.snapshot().get("status"))&&Main.num(scheduled.snapshot().get("priority"),0)==73
                    &&Main.num(scheduled.snapshot().get("start_after_ms"),0)==future,"Scheduled queued status priority and future start survive a real Manager restart");
            check("paused".equals(manual.snapshot().get("status")),"Manual paused status survives restart without becoming automatic queued work");
            Opener finalOpen=new Opener("scheduled-restarted");scheduled=install(restored,scheduled.snapshot(),finalOpen);Main.Job finalScheduled=scheduled;
            restored.updateQueueSettings(Map.of("schedule_enabled",false));check(scheduled.thread==null,"Restarted future start remains ineligible even when daily schedule opens");
            restored.updateJobOptions(scheduleId,Map.of("start_after_ms",0L));await(()->finalOpen.blocked.get()>0,"Restarted queued download starts after its one-off timestamp is cleared");
            check(manual.thread==null&&"paused".equals(manual.snapshot().get("status")),"Restart and subsequent queue ticks still respect manual pause");
            complete(finalScheduled,finalOpen);
            String manifest=Files.readString(profile.resolve(scheduleId+".json"),StandardCharsets.UTF_8);
            check(manifest.contains("Расписание_日本語_🚀.bin")&&!manifest.contains("\ufffd"),"Scheduler persistence and published output preserve the full UTF-8 filename");
        } finally {restored.shutdown();AppLog.close();}
        Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("version",Main.VERSION);report.put("checks",checks);report.put("cases",cases);
        report.put("network_calls",0);report.put("user_data_read",false);report.put("owned_range_opens",rangeOpens.get());report.put("actual_verified_files",6);
        report.put("system_timezone",ZoneId.systemDefault().getId());report.put("physical_gateway_tested",false);
        Files.writeString(own.resolve("SCHEDULE-QUEUE-TESTS.json"),Json.stringify(report)+"\n",StandardCharsets.UTF_8);
        System.out.println("PASS "+checks+" schedule and priority checks");
    }
}
