package multiroute;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.SSLSession;

/**
 * Actual native progress before a 16 KiB block completes. Both GET and Range
 * resolver paths use the owned SOCKS server; an application-gateway fallback fails.
 * No physical adapter, public seed, tracker or user file is used.
 */
public final class TorrentPartialBlockTestRelease41 {
    private static void require(boolean valid, String reason) { if (!valid) throw new AssertionError(reason); }
    private static byte[] encode(Object value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); write(out, value); return out.toByteArray();
    }
    private static void write(OutputStream out, Object value) throws IOException {
        if (value instanceof String text) write(out, text.getBytes(StandardCharsets.UTF_8));
        else if (value instanceof byte[] bytes) { out.write((bytes.length + ":").getBytes(StandardCharsets.US_ASCII)); out.write(bytes); }
        else if (value instanceof Number number) out.write(("i" + number.longValue() + "e").getBytes(StandardCharsets.US_ASCII));
        else if (value instanceof List<?> list) { out.write('l'); for (Object item : list) write(out, item); out.write('e'); }
        else if (value instanceof Map<?,?> map) { out.write('d'); for (String key : new TreeSet<>(map.keySet().stream().map(String::valueOf).toList())) { write(out, key); write(out, map.get(key)); } out.write('e'); }
        else throw new IOException("Unsupported owned metainfo value");
    }
    private static String line(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int b; (b = input.read()) != -1;) { if (b == '\n') break; if (b != '\r') bytes.write(b); if (bytes.size() > 16384) throw new IOException("Owned header bound exceeded"); }
        return bytes.toString(StandardCharsets.US_ASCII);
    }
    private static final class Socks implements AutoCloseable {
        final ServerSocket server = new ServerSocket(0, 16, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
        final ExecutorService threads = Executors.newCachedThreadPool(task -> { Thread thread = new Thread(task, "owned-partial41-socks"); thread.setDaemon(true); return thread; });
        final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
        final Set<DatagramSocket> datagrams = ConcurrentHashMap.newKeySet();
        final AtomicInteger connects = new AtomicInteger(), forbidden = new AtomicInteger(), udpAssociations = new AtomicInteger();
        final List<String> rejections = new CopyOnWriteArrayList<>();
        final int originPort;
        volatile boolean closed;
        Socks(int originPort) throws Exception {
            this.originPort = originPort;
            threads.submit(() -> { while (!closed) try { Socket client = server.accept(); sockets.add(client); threads.submit(() -> relay(client)); } catch (IOException stopped) { if (!closed) throw new UncheckedIOException(stopped); } });
        }
        void relay(Socket client) {
            try (client) {
                client.setSoTimeout(5000); DataInputStream input = new DataInputStream(client.getInputStream()); OutputStream output = client.getOutputStream();
                if (input.readUnsignedByte() != 5) return; input.readNBytes(input.readUnsignedByte()); output.write(new byte[]{5, 0}); output.flush();
                if (input.readUnsignedByte() != 5) return; int command = input.readUnsignedByte(); input.readUnsignedByte(); int type = input.readUnsignedByte();
                String host = type == 1 ? InetAddress.getByAddress(input.readNBytes(4)).getHostAddress() : type == 3 ? new String(input.readNBytes(input.readUnsignedByte()), StandardCharsets.US_ASCII) : "forbidden";
                if (type != 1 && type != 3) { forbidden.incrementAndGet(); return; }
                int port = input.readUnsignedShort();
                if (command == 3) {
                    // Model an available SOCKS UDP association. Its datagrams are consumed locally,
                    // with no DNS lookup or forwarding to a public tracker/bootstrap destination.
                    try (DatagramSocket udp = new DatagramSocket(0, InetAddress.getByAddress(new byte[]{127,0,0,1}))) {
                        datagrams.add(udp); udpAssociations.incrementAndGet(); int bound=udp.getLocalPort();
                        output.write(new byte[]{5,0,0,1,127,0,0,1,(byte)(bound>>>8),(byte)bound});output.flush();
                        threads.submit(()->{byte[] bytes=new byte[65535];while(!udp.isClosed())try{udp.receive(new DatagramPacket(bytes,bytes.length));}catch(IOException stopped){}});
                        client.setSoTimeout(0);while(input.read()!=-1){};datagrams.remove(udp);
                    }
                    return;
                }
                // Match literal coordinates before any DNS lookup or socket connect.
                if (command != 1 || !host.equals("127.0.0.1") || port != originPort) { forbidden.incrementAndGet(); rejections.add(command+":"+host+":"+port); output.write(new byte[]{5, 2, 0, 1, 127, 0, 0, 1, 0, 0}); output.flush(); return; }
                try (Socket remote = new Socket("127.0.0.1", originPort)) {
                    sockets.add(remote); connects.incrementAndGet(); output.write(new byte[]{5, 0, 0, 1, 127, 0, 0, 1, 0, 0}); output.flush();
                    Future<?> reverse = threads.submit(() -> { try { remote.getInputStream().transferTo(output); client.shutdownOutput(); } catch (IOException closed) { } });
                    try { input.transferTo(remote.getOutputStream()); remote.shutdownOutput(); } catch (IOException closed) { }
                    try { reverse.get(5, TimeUnit.SECONDS); } catch (Exception closed) { }
                    sockets.remove(remote);
                }
            } catch (IOException closed) { } finally { sockets.remove(client); }
        }
        InetSocketAddress endpoint() { return new InetSocketAddress("127.0.0.1", server.getLocalPort()); }
        public void close() throws Exception { closed = true; server.close(); for (Socket socket : sockets) socket.close();for(DatagramSocket udp:datagrams)udp.close(); threads.shutdownNow(); threads.awaitTermination(3, TimeUnit.SECONDS); }
    }
    private record Response(int statusCode, URI uri, InputStream body, HttpHeaders headers) implements HttpResponse<InputStream> {
        public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
    private static HttpResponse<InputStream> request(Socks proxy, String target, long start, long end) throws Exception {
        URI uri = URI.create(target); require(uri.getHost().equals("127.0.0.1") && uri.getPort() == proxy.originPort, "Only the owned HTTP seed may be requested");
        Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, proxy.endpoint())); socket.connect(new InetSocketAddress("127.0.0.1", uri.getPort()), 5000); socket.setSoTimeout(5000);
        String range = start < 0 ? "" : "Range: bytes=" + start + "-" + end + "\r\n";
        socket.getOutputStream().write(("GET " + uri.getRawPath() + " HTTP/1.1\r\nHost: 127.0.0.1\r\n" + range + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush();
        InputStream input = socket.getInputStream(); int status = Integer.parseInt(line(input).split(" ")[1]); Map<String,List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String header; !(header = line(input)).isEmpty();) { int colon = header.indexOf(':'); require(colon > 0, "Owned HTTP response header"); headers.computeIfAbsent(header.substring(0, colon), key -> new ArrayList<>()).add(header.substring(colon + 1).trim()); }
        InputStream body = new FilterInputStream(input) { public void close() throws IOException { try { super.close(); } finally { socket.close(); } } };
        return new Response(status, uri, body, HttpHeaders.of(headers, (a,b) -> true));
    }
    private static void ready(TorrentEngine.Channel channel, String source, byte expected) throws Exception {
        URI target=URI.create(channel.network.rewrite(source));
        try(Socket socket=new Socket(new Proxy(Proxy.Type.SOCKS,channel.network.endpoint()))){
            socket.connect(new InetSocketAddress("127.0.0.1",target.getPort()),5000);socket.setSoTimeout(5000);
            socket.getOutputStream().write(("GET "+target.getRawPath()+" HTTP/1.1\r\nHost: 127.0.0.1\r\nRange: bytes=0-0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
            InputStream input=socket.getInputStream();require(line(input).startsWith("HTTP/1.1 206"),"Actual owned SOCKS/relay returns HTTP206 before native sampling");
            while(!line(input).isEmpty()){};require(input.read()==(expected&255),"Actual owned SOCKS/relay returns the exact readiness byte");
        }
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); Path root = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(root);
        final int piece = 256 * 1024; byte[] bytes = new byte[piece]; new Random(28333).nextBytes(bytes);
        AtomicInteger requests = new AtomicInteger(), getCalls = new AtomicInteger(), rangeCalls = new AtomicInteger(), fallbackCalls = new AtomicInteger(); AtomicLong emitted = new AtomicLong();
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.setExecutor(Executors.newCachedThreadPool(task -> { Thread thread = new Thread(task, "owned-partial41-origin"); thread.setDaemon(true); return thread; }));
        origin.createContext("/owned.bin", exchange -> {
            try {
                requests.incrementAndGet(); String range = exchange.getRequestHeaders().getFirst("Range"); int begin = 0, end = bytes.length - 1;
                if (range != null) { String[] bounds = range.substring(6).split("-", -1); begin = Integer.parseInt(bounds[0]); if (!bounds[1].isEmpty()) end = Math.min(end, Integer.parseInt(bounds[1])); exchange.getResponseHeaders().set("Content-Range", "bytes " + begin + "-" + end + "/" + bytes.length); }
                exchange.getResponseHeaders().set("Accept-Ranges", "bytes"); exchange.sendResponseHeaders(range == null ? 200 : 206, end - begin + 1);
                try (OutputStream body = exchange.getResponseBody()) { for (int offset = begin; offset <= end; offset += 128) { int n = Math.min(128, end - offset + 1); body.write(bytes, offset, n); body.flush(); emitted.addAndGet(n); Thread.sleep(50); } }
            } catch (Exception closedByFixture) { } finally { exchange.close(); }
        }); origin.start();
        String url = "http://127.0.0.1:" + origin.getAddress().getPort() + "/owned.bin";
        byte[] torrent = encode(Map.of("info", Map.of("name", "owned.bin", "piece length", piece, "length", piece, "pieces", MessageDigest.getInstance("SHA-1").digest(bytes), "private", 1), "url-list", List.of(url)));
        SourceTransfer.EndpointResolver saved = SourceTransfer.endpoints;
        SourceTransfer.endpoints = route -> { fallbackCalls.incrementAndGet(); throw new AssertionError("Real application-gateway fallback is forbidden in this fixture"); };
        Throwable failure = null; long partial = 0,startupMs=0; int connects = 0, forbidden = 0; List<String> alerts=new CopyOnWriteArrayList<>(),rejections=new ArrayList<>();
        try (Socks proxy = new Socks(origin.getAddress().getPort())) {
            TorrentEngine.Resolver resolver = new TorrentEngine.Resolver() {
                public InetSocketAddress resolve(Map<String,Object> route) { return proxy.endpoint(); }
                public HttpResponse<InputStream> open(Map<String,Object> route, String target, long start, long end) throws Exception { rangeCalls.incrementAndGet(); return request(proxy, target, start, end); }
                public HttpResponse<InputStream> get(Map<String,Object> route, String target) throws Exception { getCalls.incrementAndGet(); return request(proxy, target, -1, -1); }
            };
            Map<String,Object> route = new LinkedHashMap<>(Map.of("type", "gateway", "gateway", "192.168.1.1", "enabled", true));
            Path storage = root.resolve("storage"); Files.createDirectories(storage); TorrentEngine.Channel channel = new TorrentEngine.Channel(route, storage, root.resolve("resume.bin"), resolver, true);
            channel.session.addListener(new AlertListener(){public int[] types(){return null;}public void alert(Alert<?> alert){if(alerts.size()<100)alerts.add(alert.getClass().getSimpleName()+": "+alert.message());}});
            try {
                ready(channel,url,bytes[0]);int readyRequests=requests.get();
                channel.add(new TorrentInfo(torrent), new Priority[]{Priority.NORMAL}); channel.handle.resume();
                // Native webseed reconnect/backoff is a separate bounded readiness phase. It must
                // reach this real owned seed; it cannot substitute data for the partial-block check.
                long started=System.nanoTime(),startupDeadline=started+TimeUnit.SECONDS.toNanos(70);
                while(requests.get()==readyRequests&&System.nanoTime()<startupDeadline)Thread.sleep(20);
                startupMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
                require(requests.get()>readyRequests,"Native webseed reaches the ready owned seed within its recovery budget");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12); Map<Integer,Long> snapshot = Map.of();
                while (System.nanoTime() < deadline) { snapshot = channel.pieceProgress(); if (snapshot.getOrDefault(0, 0L) > 0 && snapshot.get(0) < 16384) break; Thread.sleep(30); }
                partial = snapshot.getOrDefault(0, 0L); require(partial > 0 && partial < 16384, "Native peer must report real bytes before a complete block: " + snapshot);
                PieceScheduler queue = new PieceScheduler(new boolean[]{true}, new boolean[]{false}, new int[]{piece}, 1, 0);
                queue.observePiece(0, 0, partial, TimeUnit.SECONDS.toNanos(59)); queue.refill(TimeUnit.SECONDS.toNanos(60)); require(queue.expired() == 0, "Native partial bytes renew this piece deadline");
                queue.refill(TimeUnit.SECONDS.toNanos(119)); require(queue.expired() == 1, "Piece expires exactly sixty seconds after its last partial bytes");
                require(fallbackCalls.get() == 0 && proxy.connects.get() > 0 && requests.get() > 0, "All payload used actual owned SOCKS and HTTP endpoints");
            } finally { channel.network.close(); channel.session.stop(); connects = proxy.connects.get(); forbidden = proxy.forbidden.get(); rejections.addAll(proxy.rejections); }
        } catch (Throwable error) { failure = error; }
        finally { SourceTransfer.endpoints = saved; origin.stop(0); }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", failure == null ? "PASS" : "FAIL"); report.put("version", Main.VERSION); report.put("actual_native_partial_bytes", partial);
        report.put("sub_block_progress_renews_piece_clock", failure == null); report.put("independent_sixty_second_deadline", failure == null); report.put("owned_http_requests", requests.get()); report.put("owned_socks_connects", connects);
        report.put("owned_get_calls", getCalls.get()); report.put("owned_range_calls", rangeCalls.get()); report.put("origin_bytes_emitted", emitted.get()); report.put("forbidden_proxy_requests_rejected", forbidden);
        report.put("native_startup_readiness_ms",startupMs);report.put("native_startup_budget_seconds",70);report.put("partial_sampling_budget_seconds",12);
        report.put("real_gateway_fallback_calls", fallbackCalls.get()); report.put("native_capture_helper_started", false); report.put("owned_loopback_only", true);
        report.put("owned_proxy_rejections",rejections);report.put("native_alerts",alerts);
        if (failure != null) report.put("failure", failure.toString()); Files.writeString(root.resolve("TORRENT-PARTIAL-BLOCK-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8); System.out.println(Json.stringify(report));
        if (failure != null) { failure.printStackTrace(); System.exit(1); }
    }
}
