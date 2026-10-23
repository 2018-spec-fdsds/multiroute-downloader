package multiroute;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** App-owned DNS settings and an authenticated loopback bridge for the private network stack. */
final class ManagedDns implements AutoCloseable {
    record State(SecureDns.Config config, long revision, String error) { }
    record Bridge(int port, String key) { }
    private record Egress(int port, String key, Set<String> gateways) { }
    private record Retained(State state, long expires) { }
    private static final byte[] BROKER_MAGIC = "MRDNS2".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EGRESS_MAGIC = "MRDNT1".getBytes(StandardCharsets.US_ASCII);
    private static final int BUDGET_MS = 8000;
    private final Path path;
    private final SecureDns resolver;
    private final AtomicReference<State> state;
    private final ConcurrentHashMap<Long,Retained> policies = new ConcurrentHashMap<>();
    private final AtomicReference<Egress> egress = new AtomicReference<>();
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor workers;
    private final ExecutorService testers;
    private final ConcurrentHashMap<String,Map<String,Object>> observations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Long> lastErrorLog = new ConcurrentHashMap<>();
    private volatile ServerSocket server;
    private volatile String key = "";
    private volatile boolean closed;

    ManagedDns(Path dataDirectory) throws IOException { this(dataDirectory, new SecureDns()); }

    ManagedDns(Path dataDirectory, SecureDns resolver) throws IOException {
        this.resolver = Objects.requireNonNull(resolver);
        path = dataDirectory.resolve("dns-settings.json");
        SecureDns.Config config = SecureDns.Config.defaults(); long revision = 1; String error = "";
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path) || Files.size(path) > 65536)
                    throw new IOException("Invalid DNS settings file");
                Map<String,Object> saved = Main.obj(Json.parse(Files.readString(path, StandardCharsets.UTF_8)));
                Map<String,Object> projection = new LinkedHashMap<>(saved); projection.remove("revision");
                config = SecureDns.Config.fromJson(projection);
                Object rawRevision = saved.getOrDefault("revision", 1L);
                if (!(rawRevision instanceof Number number) || !Double.isFinite(number.doubleValue()) ||
                        number.doubleValue() != number.longValue() || number.longValue() < 1 || number.longValue() > Long.MAX_VALUE - 1024)
                    throw new IOException("Invalid DNS settings revision");
                revision = number.longValue();
            } catch (IOException | RuntimeException invalid) {
                error = NetworkDiagnostics.safe(invalid.getMessage());
                AppLog.error("DNS settings could not be loaded: %s", DiagnosticText.arg(error));
            }
        }
        state = new AtomicReference<>(new State(config, revision, error));
        policies.put(revision, new Retained(state.get(), Long.MAX_VALUE));
        AtomicInteger number = new AtomicInteger();
        ThreadFactory factory = task -> { Thread t = new Thread(task, "managed-dns-" + number.incrementAndGet()); t.setDaemon(true); return t; };
        workers = new ThreadPoolExecutor(8, 8, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16), factory, new ThreadPoolExecutor.AbortPolicy());
        testers = new ThreadPoolExecutor(8, 8, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    State snapshot() { return state.get(); }
    synchronized void stage(State next) {
        State previous = state.get(); long now = System.nanoTime();
        policies.put(previous.revision(), new Retained(previous, now + TimeUnit.SECONDS.toNanos(10)));
        policies.put(next.revision(), new Retained(next, Long.MAX_VALUE)); state.set(next);
        policies.entrySet().removeIf(entry -> entry.getValue().expires() < now);
        if (policies.size() > 64) policies.keySet().stream().filter(revision -> revision != next.revision()).sorted()
                .limit(policies.size() - 64).toList().forEach(policies::remove);
        observations.clear();
    }
    void persist() throws IOException {
        State current = state.get(); Map<String,Object> saved = new LinkedHashMap<>(current.config().toJson());
        saved.put("revision", current.revision()); Main.save(path, saved);
    }
    Map<String,Object> settings() {
        State current = state.get(); Map<String,Object> result = new LinkedHashMap<>(current.config().toJson());
        result.put("revision", current.revision()); result.put("configuration_error", current.error());
        result.put("encrypted", !current.config().mode().equals("system"));
        result.put("plaintext_fallback", false); result.put("timeout_ms", BUDGET_MS);
        result.put("helper_ready", egress.get() != null);
        result.put("gateway_status", observations.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList());
        return result;
    }

    synchronized Bridge beginGeneration() throws IOException {
        if (closed) throw new IOException("DNS service is closed");
        invalidateGeneration();
        if (server == null) {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(loopback(), 0), 32);
            ServerSocket owned = server;
            Thread accept = new Thread(() -> accept(owned), "managed-dns-broker"); accept.setDaemon(true); accept.start();
        }
        byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
        key = HexFormat.of().formatHex(random);
        return new Bridge(server.getLocalPort(), key);
    }

    void publishEgress(int port, List<String> gateways) throws IOException {
        if (port < 1 || port > 65535 || key.length() != 64 || gateways.isEmpty()) throw new IOException("Invalid managed DNS readiness report");
        Egress next = new Egress(port, key, Set.copyOf(gateways));
        egress.updateAndGet(current -> next.equals(current) ? current : next);
    }

    void invalidateGeneration() {
        egress.set(null);
        for (Socket owned : sockets) closeSocket(owned);
        sockets.removeIf(Socket::isClosed);
        observations.clear();
    }

    private void accept(ServerSocket owned) {
        while (!closed && !owned.isClosed()) {
            Socket client = null;
            try {
                client = owned.accept(); sockets.add(client); Socket accepted = client;
                long received = System.nanoTime();
                workers.execute(() -> serve(accepted, received));
            } catch (RejectedExecutionException full) { if (client != null) sockets.remove(client); closeSocket(client); }
            catch (IOException stopped) { if (client != null) sockets.remove(client); closeSocket(client); if (!owned.isClosed()) try { Thread.sleep(20); } catch (InterruptedException end) { Thread.currentThread().interrupt(); break; } }
        }
    }

    private void serve(Socket client, long received) {
        boolean authenticated = false; String gateway = ""; long started = System.nanoTime(); State policy = state.get();
        try {
            long initialDeadline = received + TimeUnit.MILLISECONDS.toNanos(BUDGET_MS);
            if (!MessageDigest.isEqual(read(client, 6, initialDeadline), BROKER_MAGIC)) throw new IOException("Invalid DNS broker protocol");
            byte[] supplied = read(client, 64, initialDeadline); String authenticatedKey = key;
            if (!MessageDigest.isEqual(supplied, authenticatedKey.getBytes(StandardCharsets.US_ASCII))) throw new IOException("DNS broker authentication rejected");
            authenticated = true;
            gateway = paddedGateway(read(client, 16, initialDeadline));
            byte[] policyBytes = read(client, 8, initialDeadline); long requestedRevision = 0;
            for (byte value : policyBytes) requestedRevision = (requestedRevision << 8) | (value & 255L);
            Retained retained = policies.get(requestedRevision);
            if (requestedRevision < 1 || retained == null || retained.expires() < System.nanoTime())
                throw new IOException("DNS policy revision is unavailable");
            policy = retained.state();
            byte[] budget = read(client, 4, initialDeadline);
            long requested = ((long)(budget[0]&255)<<24) | ((long)(budget[1]&255)<<16) | ((long)(budget[2]&255)<<8) | (budget[3]&255);
            if (requested < 1 || requested > BUDGET_MS) throw new IOException("Invalid DNS query budget");
            long deadline = Math.min(initialDeadline, received + TimeUnit.MILLISECONDS.toNanos(requested));
            byte[] length = read(client, 2, deadline); int size = ((length[0]&255)<<8) | (length[1]&255);
            if (size < 12 || size > 4096) throw new IOException("Invalid DNS query size");
            byte[] query = read(client, size, deadline);
            Egress endpoint = egress.get();
            if (endpoint == null || !endpoint.gateways().contains(gateway)) throw new IOException("DNS gateway generation is not ready");
            if (!endpoint.key().equals(authenticatedKey)) throw new IOException("DNS gateway generation changed");
            if (!policy.error().isBlank()) throw new IOException("Correct the saved DNS settings before connecting");
            byte[] response = resolver.query(policy.config(), gateway, query,
                    (route, address, port, timeout) -> connect(endpoint, route, address, port, timeout), deadline);
            if (!validEndpoint(endpoint, gateway)) throw new IOException("DNS gateway generation changed");
            if (response.length < 12 || response.length > 65535) throw new IOException("Invalid DNS response size");
            send(client, 1, response); observe(gateway, policy, started, "", response.length);
        } catch (IOException | RuntimeException failure) {
            String reason = NetworkDiagnostics.safe(failure.getMessage());
            if (authenticated) try { send(client, 0, utf8Error(reason)); } catch (IOException ignored) { }
            if (!gateway.isBlank()) {
                observe(gateway, policy, started, reason, 0);
                long now = System.nanoTime(); boolean[] emit = {false};
                lastErrorLog.compute(gateway, (name, previous) -> {
                    if (previous == null || now - previous >= TimeUnit.SECONDS.toNanos(12)) { emit[0] = true; return now; }
                    return previous;
                });
                if (emit[0]) AppLog.error("DNS lookup failed via gateway %s (%s): %s", gateway, policy.config().mode(), DiagnosticText.arg(reason));
            }
        } finally { sockets.remove(client); closeSocket(client); }
    }

    private Socket connect(Egress endpoint, String gateway, Inet4Address address, int port, int timeout) throws IOException {
        if (!validEndpoint(endpoint, gateway)) throw new IOException("DNS gateway generation changed");
        if (port < 1 || port > 65535 || timeout < 1) throw new IOException("Invalid DNS tunnel target");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.min(timeout, BUDGET_MS));
        Socket result = new OwnedSocket(); sockets.add(result);
        try {
            result.connect(new InetSocketAddress(loopback(), endpoint.port()), remaining(deadline));
            DataOutputStream out = new DataOutputStream(result.getOutputStream());
            out.write(EGRESS_MAGIC); out.write(endpoint.key().getBytes(StandardCharsets.US_ASCII));
            out.write(gatewayBytes(gateway)); out.write(address.getAddress()); out.writeShort(port); out.flush();
            int accepted = read(result, 1, deadline)[0] & 255;
            if (accepted != 1) {
                if (accepted != 0) throw new IOException("Invalid DNS tunnel acknowledgement");
                byte[] count = read(result, 2, deadline); int length = ((count[0]&255)<<8) | (count[1]&255);
                if (length > 512) throw new IOException("Invalid DNS tunnel error size");
                throw new IOException(NetworkDiagnostics.safe(new String(read(result, length, deadline), StandardCharsets.UTF_8)));
            }
            if (!validEndpoint(endpoint, gateway)) throw new IOException("DNS gateway generation changed");
            return result;
        } catch (IOException | RuntimeException failure) { sockets.remove(result); closeSocket(result); throw failure; }
    }

    Map<String,Object> test(SecureDns.Config draft, List<String> gateways, String hostname) throws IOException {
        Egress endpoint = egress.get();
        if (endpoint == null) throw new IOException("Check the application network before testing DNS");
        if (gateways.isEmpty() || gateways.size() > 32 || new HashSet<>(gateways).size() != gateways.size() || !endpoint.gateways().containsAll(gateways))
            throw new IOException("Choose configured gateways for the DNS test");
        byte[] query = DnsMessage.queryA(hostname);
        List<Future<Map<String,Object>>> futures = new ArrayList<>();
        try { for (String gateway : gateways) futures.add(testers.submit(() -> {
            long start = System.nanoTime(); Map<String,Object> row = new LinkedHashMap<>(); row.put("gateway", gateway);
            try {
                byte[] answer = resolver.query(draft, gateway, query,
                        (route, address, port, timeout) -> connect(endpoint, route, address, port, timeout), start + TimeUnit.MILLISECONDS.toNanos(BUDGET_MS));
                if (!validEndpoint(endpoint, gateway)) throw new IOException("DNS gateway generation changed");
                List<String> addresses = DnsMessage.publicAddresses(query, answer);
                if (addresses.isEmpty()) throw new IOException("DNS response contains no public IPv4 answer");
                row.put("ok", true); row.put("addresses", addresses);
            } catch (IOException | RuntimeException failure) { row.put("ok", false); row.put("error", NetworkDiagnostics.safe(failure.getMessage())); }
            row.put("elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)); return row;
        })); } catch (RejectedExecutionException busy) {
            for (Future<?> future : futures) future.cancel(true);
            throw new IOException("Another DNS test is still running", busy);
        }
        List<Map<String,Object>> results = new ArrayList<>();
        long testDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        try { for (Future<Map<String,Object>> future : futures) results.add(future.get(Math.max(1, testDeadline - System.nanoTime()), TimeUnit.NANOSECONDS)); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("DNS test interrupted", interrupted); }
        catch (ExecutionException | TimeoutException failed) { throw new IOException("DNS test timed out", failed); }
        finally { for (Future<?> future : futures) if (!future.isDone()) future.cancel(true); }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("mode", draft.mode()); report.put("provider", draft.provider());
        report.put("hostname", hostname); report.put("results", results); report.put("ok", results.stream().allMatch(row -> Boolean.TRUE.equals(row.get("ok"))));
        report.put("plaintext_fallback", false); report.put("os_routes_changed", false); return report;
    }

    private void observe(String gateway, State policy, long start, String error, int bytes) {
        if (policy.revision() != state.get().revision()) return;
        Map<String,Object> value = new LinkedHashMap<>(); value.put("gateway", gateway); value.put("mode", policy.config().mode());
        value.put("provider", policy.config().provider()); value.put("revision", policy.revision()); value.put("ok", error.isBlank());
        value.put("elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)); value.put("response_bytes", bytes);
        value.put("error", error); value.put("at", java.time.Instant.now().toString()); observations.put(gateway, Map.copyOf(value));
    }
    private static InetAddress loopback() throws UnknownHostException { return InetAddress.getByAddress(new byte[]{127,0,0,1}); }
    private boolean validEndpoint(Egress expected, String gateway) {
        Egress current = egress.get();
        return current != null && current.port() == expected.port() && current.key().equals(expected.key()) && current.gateways().contains(gateway);
    }
    private static int remaining(long deadline) throws SocketTimeoutException {
        long nanos = deadline - System.nanoTime(); if (nanos <= 0) throw new SocketTimeoutException("DNS query timed out");
        return (int)Math.max(1, Math.min(BUDGET_MS, TimeUnit.NANOSECONDS.toMillis(nanos)));
    }
    private static byte[] read(Socket socket, int length, long deadline) throws IOException {
        byte[] bytes = new byte[length]; int offset = 0; InputStream in = socket.getInputStream();
        while (offset < length) { socket.setSoTimeout(remaining(deadline)); int got = in.read(bytes, offset, length - offset); if (got < 0) throw new EOFException("DNS connection closed early"); offset += got; }
        return bytes;
    }
    private static String paddedGateway(byte[] bytes) throws IOException {
        int end = 0; while (end < bytes.length && bytes[end] != 0) end++;
        for (int i = end; i < bytes.length; i++) if (bytes[i] != 0) throw new IOException("Invalid DNS gateway field");
        String text = new String(bytes, 0, end, StandardCharsets.US_ASCII);
        if (!GatewayNetwork.validIpv4(text)) throw new IOException("Invalid DNS gateway address"); return text;
    }
    private static byte[] gatewayBytes(String gateway) throws IOException {
        if (!GatewayNetwork.validIpv4(gateway)) throw new IOException("Invalid DNS gateway address");
        return Arrays.copyOf(gateway.getBytes(StandardCharsets.US_ASCII), 16);
    }
    private static void send(Socket socket, int status, byte[] payload) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream()); out.writeByte(status); out.writeShort(payload.length); out.write(payload); out.flush();
    }
    private static byte[] utf8Error(String text) {
        String safe = text == null || text.isBlank() ? "DNS request failed" : text;
        while (safe.getBytes(StandardCharsets.UTF_8).length > 512) safe = safe.substring(0, safe.offsetByCodePoints(safe.length(), -1));
        return safe.getBytes(StandardCharsets.UTF_8);
    }
    private static void closeSocket(Socket socket) { if (socket != null) try { socket.close(); } catch (IOException ignored) { } }
    private final class OwnedSocket extends Socket {
        @Override public synchronized void close() throws IOException { try { super.close(); } finally { sockets.remove(this); } }
    }
    @Override public synchronized void close() {
        closed = true; invalidateGeneration(); if (server != null) try { server.close(); } catch (IOException ignored) { }
        workers.shutdownNow(); testers.shutdownNow(); resolver.close();
    }
}
