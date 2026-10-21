package multiroute;

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

/** Owned loopback proxies and synthetic ranges only; no user data or real gateways. */
public final class NetworkDiagnosticsTest42 {
    private static final byte[] DATA = "Синтетика UTF-8 中文".getBytes(StandardCharsets.UTF_8);
    private static final List<String> cases = new ArrayList<>();
    private static int checks;
    private static Path own, profile;

    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); cases.add(name); }
    private static String journal() throws Exception { AppLog.flush(); return Files.readString(profile.resolve("logs/network-errors.log"), StandardCharsets.UTF_8); }
    private static int count(String value, String match) { return value.split(java.util.regex.Pattern.quote(match), -1).length - 1; }
    private static Map<String,Object> route(String gateway) { return new LinkedHashMap<>(Map.of("type", "gateway", "gateway", gateway, "virtual_ip", "192.0.2.70", "enabled", true, "name", "Owned gateway")); }
    private static Map<String,Object> state(String id, List<Map<String,Object>> routes) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("id", id); value.put("filename", "Проверка-中文.bin"); value.put("url", "https://publisher.owned.invalid/private-file?token=OWN-SECRET");
        value.put("directory", own.resolve(id).toString()); value.put("part_path", own.resolve(id + ".part").toString());
        value.put("size", (long)DATA.length); value.put("chunk_size", 65536L); value.put("downloaded", 0L); value.put("chunks", new LinkedHashMap<>());
        value.put("routes", new ArrayList<>(routes)); value.put("status", "running"); value.put("error", "");
        value.put("etag", ""); value.put("modified", "");
        return value;
    }

    private static final class Response implements HttpResponse<InputStream> {
        private final URI uri;
        private final InputStream body;
        private final HttpHeaders headers;
        Response(String url, long start, long end, InputStream body) throws Exception {
            uri = URI.create(url); this.body = body;
            StringBuilder repr = new StringBuilder();
            for (String algorithm : List.of("MD5", "SHA-256", "SHA-512")) {
                if (!repr.isEmpty()) repr.append(", ");
                repr.append(algorithm.toLowerCase(Locale.ROOT)).append("=:").append(Base64.getEncoder().encodeToString(MessageDigest.getInstance(algorithm).digest(DATA))).append(':');
            }
            headers = HttpHeaders.of(Map.of("Content-Range", List.of("bytes " + start + "-" + end + "/" + DATA.length),
                    "Content-Length", List.of(Long.toString(end - start + 1)), "Repr-Digest", List.of(repr.toString())), (a,b) -> true);
        }
        public int statusCode() { return 206; }
        public URI uri() { return uri; }
        public InputStream body() { return body; }
        public HttpHeaders headers() { return headers; }
        public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    private static final class ProxyFixture implements AutoCloseable {
        final ServerSocket server = new ServerSocket(0, 16, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
        final List<String> authorities = new CopyOnWriteArrayList<>();
        final AtomicInteger accepted = new AtomicInteger();
        final Thread thread;
        volatile Throwable failure;
        ProxyFixture() throws Exception {
            thread = new Thread(() -> {
                while (!server.isClosed()) try (Socket socket = server.accept()) {
                    accepted.incrementAndGet(); socket.setSoTimeout(3000);
                    ByteArrayOutputStream header = new ByteArrayOutputStream(); int state = 0;
                    while (state != 4) {
                        int b = socket.getInputStream().read(); if (b < 0 || header.size() >= 16384) throw new IOException("Owned proxy header incomplete");
                        header.write(b); state = switch (state) { case 0 -> b == 13 ? 1 : 0; case 1 -> b == 10 ? 2 : 0; case 2 -> b == 13 ? 3 : 0; default -> b == 10 ? 4 : 0; };
                    }
                    String first = header.toString(StandardCharsets.US_ASCII).split("\r\n", 2)[0];
                    String response;
                    if (first.startsWith("CONNECT ")) {
                        authorities.add(first.split(" ", 3)[1]);
                        if (first.contains("eof.owned.invalid")) continue;
                        response = "HTTP/1.1 502 Bad Gateway\r\nConnection: close\r\nContent-Length: 20\r\n\r\nDO-NOT-LOG-BODY-SECRET";
                    } else response = "HTTP/1.1 307 Temporary Redirect\r\nConnection: close\r\nContent-Length: 0\r\nLocation: https://cdn.owned.invalid/hidden-path?X-Amz-Signature=OWN-REDIRECT-SECRET\r\n\r\n";
                    socket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush();
                } catch (Exception error) { if (!server.isClosed()) { failure = error; break; } }
            }, "owned-tunnel-fixture"); thread.setDaemon(true); thread.start();
        }
        URI endpoint() { return URI.create("http://127.0.0.1:" + server.getLocalPort()); }
        public void close() throws Exception { server.close(); thread.join(5000); if (failure != null) throw new AssertionError("Owned proxy failed", failure); }
    }

    private static void actualTunnel() throws Exception {
        SourceTransfer.EndpointResolver saved = SourceTransfer.endpoints;
        try (ProxyFixture proxy = new ProxyFixture()) {
            SourceTransfer.endpoints = ignored -> proxy.endpoint(); Transport.resetGatewayClients();
            Map<String,Object> route = route("192.0.2.1"); IOException seen = null;
            NetworkDiagnostics.Attempt scope = NetworkDiagnostics.attempt("owned-tunnel", route, "http://publisher.owned.invalid/hidden", "Metadata", -1, 0, 0, () -> true);
            try (scope) {
                Transport.open(route, "http://publisher.owned.invalid/hidden?token=OWN-SECRET", 0, 0, null, null);
            } catch (IOException expected) { seen = expected; scope.failure(expected); }
            check(seen != null && seen.getMessage().contains("502"), "actual HttpClient CONNECT502 preserved");
            String log = journal();
            check(log.contains("cdn.owned.invalid:443") && log.contains("192.0.2.1"), "actual redirect destination and selected gateway logged");
            check(count(log, "owned-tunnel") == 1, "transport and attempt catches emit one event for same exception");
            check(proxy.authorities.contains("cdn.owned.invalid:443"), "actual CONNECT reached owned proxy only");
            check(!log.contains("OWN-SECRET") && !log.contains("OWN-REDIRECT-SECRET") && !log.contains("hidden-path") && !log.contains("DO-NOT-LOG"), "request query path and untrusted proxy body excluded");
            int before = log.length();
            try { SourceTransfer.connect(route, "source.owned.invalid", 22); throw new AssertionError("Tunnel unexpectedly succeeded"); }
            catch (IOException expected) { check(expected.getMessage().equals("Gateway refused source tunnel (HTTP 502)"), "raw CONNECT records only numeric HTTP status"); }
            check(journal().substring(before).contains("source.owned.invalid:22"), "FTP/SFTP source tunnel failure persisted");
            before = journal().length();
            try { SourceTransfer.connect(route, "eof.owned.invalid", 22); throw new AssertionError("EOF tunnel unexpectedly succeeded"); }
            catch (IOException expected) { check(expected.getMessage().equals("Invalid gateway CONNECT response"), "raw CONNECT EOF retains protocol error"); }
            check(journal().substring(before).contains("eof.owned.invalid:22"), "raw CONNECT EOF failure persisted");
        } finally { SourceTransfer.endpoints = saved; Transport.resetGatewayClients(); }
    }

    private static void fallbackAttempts() throws Exception {
        Map<String,Object> first = route("192.0.2.2"), second = route("192.0.2.3");
        AtomicInteger calls = new AtomicInteger();
        Main.Job metadata = new Main.Job(state("owned-metadata", List.of(first, second)), profile, (route,url,start,end,etag,modified) -> {
            calls.incrementAndGet(); if (route.get("gateway").equals("192.0.2.2")) throw new IOException("Owned metadata failure token=OWN-METADATA-SECRET");
            return new Response(url, start, end, new ByteArrayInputStream(Arrays.copyOfRange(DATA, (int)start, (int)end + 1)));
        }, Main.HTTP_IDLE_TIMEOUT_NANOS);
        check(metadata.metadata()[0] == DATA.length && calls.get() == 2, "metadata still succeeds through second gateway after first failure");
        check(journal().contains("owned-metadata") && journal().contains("192.0.2.2"), "intermediate metadata failure remains after alternate success");
        check(!journal().contains("OWN-METADATA-SECRET"), "metadata error credential redacted");

        Map<String,Object> rangeState = state("owned-range", List.of(first));
        rangeState.put("mirrors", List.of("https://first.owned.invalid/secret", "https://second.owned.invalid/secret"));
        Main.Job ranges = new Main.Job(rangeState, profile, (route,url,start,end,etag,modified) -> {
            if (url.contains("first.")) throw new IOException("Owned first mirror failed");
            return new Response(url, start, end, new ByteArrayInputStream(Arrays.copyOfRange(DATA, (int)start, (int)end + 1)));
        }, Main.HTTP_IDLE_TIMEOUT_NANOS);
        Map<String,Object> selected = ranges.routeReferences().get(0); Main.Job.HttpAttempt attempt = ranges.new HttpAttempt(0, 0, 0, selected);
        try (InputStream body = ranges.openRange(selected, attempt, 0, DATA.length - 1, null, null).body()) {
            check(Arrays.equals(body.readAllBytes(), DATA), "range mirror retry preserves synthetic Unicode payload");
        } finally { attempt.released(); }
        String log = journal();
        check(log.contains("owned-range") && log.contains("first.owned.invalid:443") && log.contains("range=0-" + (DATA.length - 1)), "range failure records job source segment and byte range");
    }

    private static void streamingFailure() throws Exception {
        Map<String,Object> data = state("owned-body", List.of(route("192.0.2.4")));
        Files.write(Path.of(String.valueOf(data.get("part_path"))), new byte[DATA.length]);
        AtomicInteger opens = new AtomicInteger();
        Main.Job job = new Main.Job(data, profile, (route,url,start,end,etag,modified) -> {
            InputStream stream = opens.getAndIncrement() == 0 ? new InputStream() {
                @Override public int read() throws IOException { throw new EOFException("Owned body disconnected"); }
            } : new ByteArrayInputStream(DATA);
            return new Response(url, start, end, stream);
        }, Main.HTTP_IDLE_TIMEOUT_NANOS);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> done = executor.submit(() -> { try { job.download(); } catch (Exception error) { throw new RuntimeException(error); } });
            done.get(10, TimeUnit.SECONDS);
            check(opens.get() == 2 && job.chunks().size() == 1, "streaming retry succeeds without scheduling change");
            check(journal().contains("Owned body disconnected") && journal().contains("owned-body"), "intermediate body disconnect persisted");
        } finally { job.stop = true; executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS); }
    }

    private static void redactionAndCancellation() throws Exception {
        String dangerous = "Ошибка 中文 https://user:PRIVATE-PASSWORD@cdn.owned.invalid/private/secret?unknown=PRIVATE-QUERY#PRIVATE-FRAGMENT token=PRIVATE-TOKEN hf_PRIVATE-HF\nAuthorization: Bearer PRIVATE-AUTH";
        String safe = NetworkDiagnostics.safe(dangerous);
        check(safe.contains("Ошибка 中文") && safe.contains("cdn.owned.invalid"), "UTF8 diagnostic text preserved");
        for (String secret : List.of("PRIVATE-PASSWORD", "PRIVATE-QUERY", "PRIVATE-FRAGMENT", "PRIVATE-TOKEN", "PRIVATE-HF", "PRIVATE-AUTH", "/private/secret")) check(!safe.contains(secret), "redaction excludes owned secret " + checks);
        check(!safe.contains("\n") && !safe.contains("\r"), "control characters cannot forge log lines");
        check(NetworkDiagnostics.safe("🧪".repeat(2000)).codePointCount(0, NetworkDiagnostics.safe("🧪".repeat(2000)).length()) == 1537, "bounded text truncates on Unicode code point boundary");
        String before = journal();
        try (NetworkDiagnostics.Attempt scope = NetworkDiagnostics.attempt("owned-cancelled", route("192.0.2.1"), "https://owned.invalid", "Segment", 0, 0, 1, () -> false)) {
            NetworkDiagnostics.requestFailure(route("192.0.2.1"), URI.create("https://owned.invalid"), "HTTP request", System.nanoTime(), new IOException("Intentional close"));
            scope.failure(new IOException("Intentional retirement"));
        }
        NetworkDiagnostics.requestFailure(route("192.0.2.1"), URI.create("https://owned.invalid"), "HTTP request", System.nanoTime(), new InterruptedException("Intentional interruption"));
        check(journal().equals(before), "intentional cancellation retirement and interruption emit no errors");
    }

    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        profile = own.resolve("profile"); Files.createDirectories(profile); I18n.setLanguage("en");
        SourceAuth.configure(profile); SiteProfiles.configure(profile); AppLog.initialize(profile);
        Throwable failure = null;
        try { actualTunnel(); fallbackAttempts(); streamingFailure(); redactionAndCancellation(); }
        catch (Throwable error) { failure = error; }
        finally { AppLog.close(); }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", failure == null ? "PASS" : "FAIL"); report.put("checks", checks); report.put("cases", cases);
        report.put("version", Main.VERSION); report.put("real_gateways_used", false); report.put("user_data_read", false); report.put("network", "owned loopback proxy only");
        if (failure != null) report.put("failure", failure.toString());
        Files.writeString(own.resolve("NETWORK-DIAGNOSTICS-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
        System.out.println(Json.stringify(report)); if (failure != null) { failure.printStackTrace(); System.exit(1); }
    }
}
