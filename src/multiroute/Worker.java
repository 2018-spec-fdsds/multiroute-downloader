package multiroute;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** LAN agent. Run one instance on each host/VM whose OS route uses the requested router. */
public final class Worker {
    private static final int MAX_SMALL = 1024 * 1024;
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "worker-idle-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    private static final Pattern LINUX_VIA = Pattern.compile("(?:^|\\s)via\\s+([^\\s]+)");
    private static final Pattern MAC_GATEWAY = Pattern.compile("(?m)^\\s*gateway:\\s*(\\S+)");
    private final String token;
    private final String expectedGateway;

    private Worker(String token, String expectedGateway) {
        this.token = token;
        this.expectedGateway = expectedGateway;
    }

    public static void main(String[] args) throws Exception {
        String bind = "127.0.0.1";
        int port = 8724;
        String token = System.getenv().getOrDefault("MULTIROUTE_TOKEN", "");
        String gateway = "";
        for (int i = 0; i < args.length; i++) {
            if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + args[i]);
            String value = args[++i];
            switch (args[i - 1]) {
                case "--bind" -> bind = value;
                case "--port" -> port = Integer.parseInt(value);
                case "--token" -> token = value;
                case "--gateway" -> gateway = value;
                default -> throw new IllegalArgumentException("Unknown option: " + args[i - 1]);
            }
        }
        if (!token.isEmpty() && token.length() < 16)
            throw new IllegalArgumentException("--token or MULTIROUTE_TOKEN must be at least 16 characters when set");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        if (!gateway.isBlank() && !gateway.matches("[0-9a-fA-F:.]+"))
            throw new IllegalArgumentException("--gateway must be an IP address");
        Worker worker = new Worker(token, gateway);
        HttpServer server = HttpServer.create(new InetSocketAddress(bind, port), 32);
        AppLog.initialize(java.nio.file.Path.of("data-worker-" + port));
        Runtime.getRuntime().addShutdownHook(new Thread(AppLog::close, "worker-log-close"));
        server.createContext("/probe", worker::probe);
        server.createContext("/range", worker::range);
        server.createContext("/small", worker::small);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
        WorkerDiscovery.startResponder(bind, port, gateway);
        AppLog.info("MultiRoute worker listening on %s:%d; expected gateway %s", bind, port,
                gateway.isBlank() ? DiagnosticText.arg("unspecified") : gateway);
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        if (token.isEmpty()) return true;
        String supplied = exchange.getRequestHeaders().getFirst("X-Route-Token");
        if (supplied != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) return true;
        json(exchange, 403, Map.of("error", "Invalid worker token"));
        return false;
    }

    private void probe(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, Map.of("error", "GET required")); return; }
        if (!authorize(exchange)) return;
        try {
            RouteCheck route = routeCheck("api.ipify.org", expectedGateway);
            if (!expectedGateway.isBlank() && !route.match()) {
                json(exchange, 502, Map.of("ok", false, "error", "Cannot confirm expected gateway",
                        "gateway_hint", route.hint(), "expected_gateway", expectedGateway,
                        "gateway_match", false));
                return;
            }
            Map<String, Object> direct = Transport.probe(Map.of("type", "direct"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("egress_ip", direct.get("egress_ip"));
            result.put("gateway_hint", route.hint());
            result.put("expected_gateway", expectedGateway);
            result.put("gateway_match", expectedGateway.isBlank() ? null : true);
            result.put("host", hostName());
            json(exchange, 200, result);
        } catch (Exception error) {
            json(exchange, 502, Map.of("ok", false, "error", safeMessage(error),
                    "expected_gateway", expectedGateway));
        }
    }

    private void range(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { json(exchange, 405, Map.of("error", "POST required")); return; }
        if (!authorize(exchange)) return;
        HttpResponse<InputStream> upstream = null;
        boolean headersSent = false;
        try {
            String rawLength = exchange.getRequestHeaders().getFirst("Content-Length");
            int length = Integer.parseInt(rawLength == null ? "0" : rawLength);
            if (length < 1 || length > 4096) throw new IllegalArgumentException("Invalid request length");
            byte[] input = exchange.getRequestBody().readNBytes(length);
            if (input.length != length) throw new IllegalArgumentException("Truncated request");
            Object parsed = Json.parse(new String(input, StandardCharsets.UTF_8));
            if (!(parsed instanceof Map<?, ?> payload)) throw new IllegalArgumentException("Expected JSON object");
            String url = requiredText(payload, "url");
            Transport.uri(url);
            long start = requiredLong(payload, "start");
            long end = requiredLong(payload, "end");
            if (start < 0 || end < start || end - start >= Transport.MAX_SEGMENT)
                throw new IllegalArgumentException("Invalid or oversized byte range");
            String etag = optionalText(payload, "etag");
            String modified = optionalText(payload, "modified");
            // Transport repeats the route lookup before every connection, including redirect hosts.
            upstream = Transport.open(Map.of("type", "direct", "gateway", expectedGateway),
                    url, start, end, etag, modified);
            long total = Transport.validate(upstream, start, end, null, etag, modified);
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Encoding", "identity");
            exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + total);
            for (String header : List.of("ETag", "Last-Modified"))
                upstream.headers().firstValue(header).ifPresent(value -> exchange.getResponseHeaders().set(header, value));
            for (String value : upstream.headers().allValues("Repr-Digest"))
                exchange.getResponseHeaders().add("Repr-Digest", value);
            long remaining = end - start + 1;
            exchange.sendResponseHeaders(206, remaining);
            headersSent = true;
            try (OutputStream output = exchange.getResponseBody(); InputStream source = upstream.body();
                    IdleGuard guard = new IdleGuard(source)) {
                byte[] buffer = new byte[64 * 1024];
                while (remaining > 0) {
                    int read = source.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read < 0) throw new IOException("Origin closed before range completed");
                    if (read == 0) continue;
                    guard.progress();
                    output.write(buffer, 0, read);
                    guard.progress();
                    remaining -= read;
                }
            }
        } catch (IllegalArgumentException error) {
            if (!headersSent) json(exchange, 400, Map.of("error", safeMessage(error)));
        } catch (Exception error) {
            if (!headersSent) {
                if (error instanceof Transport.NonRetryableException)
                    exchange.getResponseHeaders().set("X-MultiRoute-NonRetryable", "true");
                json(exchange, 502, Map.of("error", safeMessage(error)));
            }
        } finally {
            if (upstream != null) upstream.body().close();
            exchange.close();
        }
    }

    private void small(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { json(exchange, 405, Map.of("error", "POST required")); return; }
        if (!authorize(exchange)) return;
        HttpResponse<InputStream> upstream = null;
        try {
            String rawLength = exchange.getRequestHeaders().getFirst("Content-Length");
            int length = Integer.parseInt(rawLength == null ? "0" : rawLength);
            if (length < 1 || length > 4096) throw new IllegalArgumentException("Invalid request length");
            byte[] input = exchange.getRequestBody().readNBytes(length);
            if (input.length != length) throw new IllegalArgumentException("Truncated request");
            Object parsed = Json.parse(new String(input, StandardCharsets.UTF_8));
            if (!(parsed instanceof Map<?, ?> payload)) throw new IllegalArgumentException("Expected JSON object");
            String url = requiredText(payload, "url");
            String origin = requiredText(payload, "origin");
            URI target = Transport.uri(url), source = Transport.uri(origin);
            if (!Transport.sameOrigin(target, source)) throw new IllegalArgumentException("Sidecar must share source origin");
            upstream = Transport.small(Map.of("type", "direct", "gateway", expectedGateway), url, origin);
            if (upstream.statusCode() != 200) {
                int status = upstream.statusCode();
                exchange.sendResponseHeaders(status >= 400 && status <= 599 ? status : 502, -1);
                return;
            }
            String encoding = upstream.headers().firstValue("Content-Encoding").orElse("identity");
            if (!encoding.equalsIgnoreCase("identity")) throw new IOException("Unexpected sidecar Content-Encoding");
            String upstreamLength = upstream.headers().firstValue("Content-Length").orElse(null);
            if (upstreamLength != null && Long.parseLong(upstreamLength) > MAX_SMALL)
                throw new IOException("Sidecar exceeds 1 MiB");
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream sourceBody = upstream.body(); IdleGuard guard = new IdleGuard(sourceBody)) {
                byte[] buffer = new byte[64 * 1024];
                while (body.size() <= MAX_SMALL) {
                    int read = sourceBody.read(buffer, 0, Math.min(buffer.length, MAX_SMALL + 1 - body.size()));
                    if (read < 0) break;
                    if (read == 0) continue;
                    body.write(buffer, 0, read);
                    guard.progress();
                }
            }
            if (body.size() > MAX_SMALL) throw new IOException("Sidecar exceeds 1 MiB");
            byte[] bytes = body.toByteArray();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Content-Encoding", "identity");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
        } catch (IllegalArgumentException error) {
            json(exchange, 400, Map.of("error", safeMessage(error)));
        } catch (Exception error) {
            if (error instanceof Transport.NonRetryableException)
                exchange.getResponseHeaders().set("X-MultiRoute-NonRetryable", "true");
            json(exchange, 502, Map.of("error", safeMessage(error)));
        } finally {
            if (upstream != null) upstream.body().close();
            exchange.close();
        }
    }

    private static final class IdleGuard implements AutoCloseable {
        private final InputStream source;
        private final Thread owner = Thread.currentThread();
        private final AtomicLong lastByte = new AtomicLong(System.nanoTime());
        private final ScheduledFuture<?> task;

        IdleGuard(InputStream source) {
            this.source = source;
            this.task = WATCHDOG.scheduleAtFixedRate(() -> {
                if (System.nanoTime() - lastByte.get() > TimeUnit.SECONDS.toNanos(60)) {
                    owner.interrupt();
                    try { source.close(); } catch (IOException ignored) { }
                }
            }, 5, 5, TimeUnit.SECONDS);
        }

        void progress() { lastByte.set(System.nanoTime()); }

        @Override public void close() {
            task.cancel(false);
            Thread.interrupted();
        }
    }

    private static String requiredText(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException("Missing " + field);
        return text;
    }

    private static String optionalText(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new IllegalArgumentException("Invalid " + field);
        return text.isBlank() ? null : text;
    }

    private static long requiredLong(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof Long number)) throw new IllegalArgumentException("Invalid integer " + field);
        return number;
    }

    private static void json(HttpExchange exchange, int status, Map<String, ?> value) throws IOException {
        byte[] bytes = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private static String hostName() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception error) { return "unknown"; }
    }

    /** Best-effort default-route hint for the controller's direct channel. */
    public static String gatewayHint() {
        try { return routeCheck("api.ipify.org", "").hint(); }
        catch (Exception error) { return "route unknown: " + safeMessage(error); }
    }

    record RouteCheck(boolean match, String hint) { }

    static RouteCheck routeCheck(String host, String expected) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) throw new IOException("Cannot resolve destination");
        List<String> hints = new ArrayList<>();
        boolean match = true;
        for (InetAddress address : addresses) {
            String ip = address.getHostAddress();
            String nextHop = nextHop(ip);
            hints.add(ip + " via " + (nextHop == null ? "unknown" : nextHop));
            if (!expected.isBlank() && !expected.equalsIgnoreCase(nextHop)) match = false;
        }
        return new RouteCheck(match, String.join("; ", hints));
    }

    static void assertRoute(String host, String expected) throws IOException {
        if (expected == null || expected.isBlank()) return;
        RouteCheck result = routeCheck(host, expected);
        if (!result.match()) throw new IOException("Expected gateway " + expected +
                " does not match destination route: " + result.hint());
    }

    private static String nextHop(String ip) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command;
        if (os.contains("win")) {
            if (parseIpv4(ip) < 0) return null;
            command = List.of("route", "print", "-4");
        } else if (os.contains("mac")) command = List.of("route", "-n", "get", ip);
        else if (os.contains("linux")) command = List.of("ip", ip.contains(":") ? "-6" : "-4", "route", "get", ip);
        else return null;
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            CompletableFuture<byte[]> reader = CompletableFuture.supplyAsync(() -> {
                try { return process.getInputStream().readNBytes(262145); }
                catch (IOException error) { return new byte[0]; }
            });
            if (!process.waitFor(5, TimeUnit.SECONDS)) { process.destroyForcibly(); return null; }
            byte[] bytes = reader.get(1, TimeUnit.SECONDS);
            if (bytes.length > 262144) return null;
            String output = new String(bytes, StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) return null;
            if (os.contains("win")) return windowsNextHop(ip, output);
            Matcher match = (os.contains("mac") ? MAC_GATEWAY : LINUX_VIA).matcher(output);
            return match.find() ? match.group(1) : null;
        } catch (Exception error) { return null; }
    }

    /** Parse active IPv4 rows from `route print -4`; persistent rows have no Interface column. */
    static String windowsNextHop(String target, String table) {
        long address = parseIpv4(target);
        if (address < 0) return null;
        String best = null;
        int bestBits = -1, bestMetric = Integer.MAX_VALUE;
        boolean ambiguous = false;
        for (String line : table.split("\\R")) {
            String[] columns = line.trim().split("\\s+");
            if (columns.length != 5) continue;
            long network = parseIpv4(columns[0]);
            long mask = parseIpv4(columns[1]);
            long interfaceIp = parseIpv4(columns[3]);
            if (network < 0 || mask < 0 || interfaceIp < 0 || (address & mask) != (network & mask)) continue;
            int metric;
            try { metric = Integer.parseInt(columns[4]); }
            catch (NumberFormatException error) { continue; }
            int bits = Long.bitCount(mask);
            if (bits > bestBits || (bits == bestBits && metric < bestMetric)) {
                best = columns[2]; bestBits = bits; bestMetric = metric; ambiguous = false;
            } else if (bits == bestBits && metric == bestMetric && !columns[2].equalsIgnoreCase(best)) {
                ambiguous = true;
            }
        }
        return ambiguous ? null : best;
    }

    private static long parseIpv4(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) return -1;
        long number = 0;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) return -1;
            int parsed;
            try { parsed = Integer.parseInt(octet); }
            catch (NumberFormatException error) { return -1; }
            if (parsed < 0 || parsed > 255) return -1;
            number = (number << 8) | parsed;
        }
        return number;
    }
}
