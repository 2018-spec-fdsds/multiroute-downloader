package multiroute;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real local API + real owned metainfo parsing; scheduled jobs never start native network sessions. */
public final class TorrentApiRelease41 {
    private static final List<String> cases = new ArrayList<>();
    private static HttpClient client;
    private static String base;
    private static void check(boolean valid, String name) { if (!valid) throw new AssertionError(name); cases.add(name); }
    private static byte[] encode(Object value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (value instanceof byte[] bytes) { out.write((bytes.length + ":").getBytes(StandardCharsets.US_ASCII)); out.write(bytes); }
        else if (value instanceof String text) return encode(text.getBytes(StandardCharsets.UTF_8));
        else if (value instanceof Number number) out.write(("i" + number.longValue() + "e").getBytes(StandardCharsets.US_ASCII));
        else if (value instanceof Map<?,?> map) { out.write('d'); for (String key : new TreeSet<>(map.keySet().stream().map(String::valueOf).toList())) { out.write(encode(key)); out.write(encode(map.get(key))); } out.write('e'); }
        else throw new IOException("Unsupported owned metainfo type");
        return out.toByteArray();
    }
    private static Map<String,Object> api(String method, String path, Map<String,Object> input, int expected, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(10));
        if (origin != null) request.header("Origin", origin);
        HttpResponse<String> response = client.send(request.method(method, input == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(Json.stringify(input), StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        check(response.statusCode() == expected, method + " " + path + " expected status " + expected + " got " + response.statusCode() + " " + response.body());
        check(response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("utf-8") && !response.body().contains("\ufffd"), "API response explicitly preserves UTF-8");
        return Main.obj(Json.parse(response.body()));
    }
    private static Map<String,Object> api(String method, String path, Map<String,Object> input, int expected) throws Exception { return api(method, path, input, expected, null); }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        byte[] payload = "ABC".getBytes(StandardCharsets.US_ASCII); String filename = "Проверка_日本語.bin";
        byte[] metadata = encode(Map.of("info", Map.of("name", filename, "piece length", 16384, "length", payload.length, "pieces", MessageDigest.getInstance("SHA-1").digest(payload), "private", 1)));
        Path torrent = own.resolve("Раздача_日本語.torrent"); Files.write(torrent, metadata);
        Path profile = own.resolve("profile"); AtomicInteger routeChecks = new AtomicInteger(), forbidden = new AtomicInteger();
        SourceAuth.configure(profile); AppLog.initialize(profile);
        Main.Manager manager = new Main.Manager(profile, (url,route) -> { throw new AssertionError("No external source inspector"); }, path -> Long.MAX_VALUE,
                (route,isTorrent) -> { check(isTorrent && route.get("type").equals("gateway"), "Torrent API uses only explicitly selected gateways"); routeChecks.incrementAndGet(); });
        manager.queueTimer.shutdownNow(); ExecutorService pool = Executors.newCachedThreadPool();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.setExecutor(pool);
        server.createContext("/", exchange -> Main.handle(exchange, manager, null, () -> { })); server.start(); base = "http://127.0.0.1:" + server.getAddress().getPort();
        client = HttpClient.newBuilder().proxy(new ProxySelector() { public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); } public void connectFailed(URI uri, SocketAddress address, IOException error) { } }).build();
        SourceTransfer.EndpointResolver saved = SourceTransfer.endpoints;
        SourceTransfer.endpoints = route -> { forbidden.incrementAndGet(); throw new AssertionError("Real gateway fallback forbidden"); };
        try {
            check(api("GET", "/api/v1/health", null, 200).get("version").equals(Main.VERSION), "API reports actual release version");
            List<Object> routes = List.of(Map.of("type", "gateway", "gateway", "192.168.1.1", "enabled", true), Map.of("type", "gateway", "gateway", "192.168.1.2", "enabled", true));
            Map<String,Object> inspect = Map.of("url", torrent.toString(), "routes", routes);
            Map<String,Object> listing = api("POST", "/api/v1/sources/inspect", inspect, 200);
            check(listing.get("kind").equals("torrent") && !listing.containsKey("torrent_metadata"), "Torrent inspection parses owned metadata and hides its private body");
            check(Main.str(Main.obj(Main.arr(listing.get("files")).get(0)).get("path")).equals(filename), "Torrent inspection preserves original Unicode filename");
            Map<String,Object> create = new LinkedHashMap<>(inspect); create.put("directory", own.resolve("download").toString()); create.put("selected_files", List.of(filename));
            create.put("info_hash_v1", listing.get("info_hash_v1")); create.put("start_after_ms", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
            create.put("expected_sha256", Main.hex(MessageDigest.getInstance("SHA-256").digest(payload)));
            Map<String,Object> bad = new LinkedHashMap<>(create); bad.put("info_hash_v1", "0".repeat(40)); api("POST", "/api/v1/sources/download", bad, 400);
            bad = new LinkedHashMap<>(create); bad.put("selected_files", List.of("../outside.bin")); api("POST", "/api/v1/sources/download", bad, 400);
            bad = new LinkedHashMap<>(create); bad.put("routes", List.of(Map.of("type", "direct", "enabled", true))); api("POST", "/api/v1/sources/download", bad, 400);
            Map<String,Object> created = api("POST", "/api/v1/sources/download", create, 201); Map<String,Object> job = Main.obj(Main.arr(created.get("jobs")).get(0)); String id = Main.str(job.get("id"));
            check(job.get("kind").equals("torrent") && Main.arr(job.get("routes")).size() == 2, "API creates one torrent job with both selected gateways");
            check(manager.jobs.get(id).thread == null && !Files.exists(own.resolve("download").resolve(filename)), "Scheduled API job starts no payload or native session");
            check(!job.containsKey("torrent_metadata") && job.get("selected_files").equals(List.of(filename)), "Job API exposes selection while hiding private metainfo");
            check(Files.readString(profile.resolve(id + ".json"), StandardCharsets.UTF_8).contains(filename), "Persisted torrent job preserves UTF-8");
            Map<String,Object> fetched = api("GET", "/api/v1/jobs/" + id, null, 200); check(fetched.get("info_hash_v1").equals(listing.get("info_hash_v1")), "Job API retains verified metainfo identity");
            Map<String,Object> paused = api("POST", "/api/v1/jobs/" + id + "/pause", Map.of(), 200); check(paused.get("status").equals("paused"), "API pauses a queued torrent job");
            api("POST", "/api/v1/jobs/" + id + "/pause", Map.of(), 403, "https://hostile.invalid");
            api("DELETE", "/api/v1/jobs/" + id + "?delete_files=false", null, 200); api("GET", "/api/v1/jobs/" + id, null, 400);
            check(Files.isRegularFile(torrent) && !Files.exists(profile.resolve(id + ".json")), "Metadata-only deletion removes job and preserves the source torrent file");
            check(forbidden.get() == 0 && routeChecks.get() > 0, "All API checks used owned metadata with no real gateway fallback");
        } finally { SourceTransfer.endpoints = saved; manager.shutdown(); AppLog.close(); server.stop(0); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS); }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", "PASS"); report.put("version", Main.VERSION); report.put("checks", cases.size()); report.put("cases", cases);
        report.put("network_scope", "owned loopback API only; local synthetic torrent metainfo"); report.put("native_capture_helper_started", false); report.put("real_gateway_fallback_calls", forbidden.get()); report.put("user_files_read", false);
        Files.writeString(own.resolve("TORRENT-API-RELEASE41-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8); System.out.println(Json.stringify(report));
    }
}
