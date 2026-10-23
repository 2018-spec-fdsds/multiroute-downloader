package multiroute;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/** Strict managed DNS. Encrypted modes never use an OS lookup or plaintext DNS. */
public final class SecureDns implements AutoCloseable {
    public interface Connector {
        /** Must connect numeric target through gateway and honor timeoutMillis. */
        Socket connect(String gateway, Inet4Address target, int port, int timeoutMillis) throws IOException;
    }
    interface SystemLookup { InetAddress[] resolve(String hostname) throws UnknownHostException; }

    public static final class Config {
        private static final Set<String> KEYS = Set.of("mode", "provider", "doh_url", "dot_host", "dot_port", "bootstrap_ips");
        private final String mode, provider, dohUrl, serverName;
        private final int port;
        private final List<String> bootstrap;

        private Config(String mode, String provider, String dohUrl, String serverName, int port, List<String> bootstrap) {
            this.mode = mode; this.provider = provider; this.dohUrl = dohUrl;
            this.serverName = serverName; this.port = port; this.bootstrap = List.copyOf(bootstrap);
        }
        public static Config defaults() { return preset("cloudflare", "doh"); }

        public static Config preset(String provider, String mode) {
            requireMode(mode);
            return switch (provider) {
                case "cloudflare" -> new Config(mode, provider, "https://cloudflare-dns.com/dns-query", "one.one.one.one", 853, List.of("1.1.1.1", "1.0.0.1"));
                case "quad9" -> new Config(mode, provider, "https://dns.quad9.net/dns-query", "dns.quad9.net", 853, List.of("9.9.9.9", "149.112.112.112"));
                case "google" -> new Config(mode, provider, "https://dns.google/dns-query", "dns.google", 853, List.of("8.8.8.8", "8.8.4.4"));
                case "custom" -> new Config(mode, provider, "https://cloudflare-dns.com/dns-query", "one.one.one.one", 853, List.of("1.1.1.1", "1.0.0.1"));
                default -> throw new IllegalArgumentException("Unknown DNS provider");
            };
        }

        public static Config fromJson(Map<String,Object> data) {
            Objects.requireNonNull(data, "DNS configuration");
            for (String key : data.keySet()) if (!KEYS.contains(key)) throw new IllegalArgumentException("Unknown DNS configuration field: " + key);
            String mode = text(data, "mode", "doh"), provider = text(data, "provider", "cloudflare");
            requireMode(mode);
            if (!Set.of("cloudflare", "quad9", "google", "custom").contains(provider)) throw new IllegalArgumentException("Unknown DNS provider");
            Config base = provider.equals("custom") ? new Config(mode, provider, "", "", 853, List.of()) : preset(provider, mode);
            try {
                String doh = text(data, "doh_url", base.dohUrl);
                String dot = text(data, "dot_host", base.serverName);
                if (!doh.isEmpty()) doh = dohUri(doh).toASCIIString();
                if (!dot.isEmpty()) {
                    dot = DnsMessage.hostname(dot);
                    if (dot.matches("[0-9.]+")) throw new IOException("DoT requires an authentication hostname");
                }
                Object rawPort = data.getOrDefault("dot_port", base.port);
                if (!(rawPort instanceof Number number) || number.longValue() != number.doubleValue()
                        || number.longValue() < 1 || number.longValue() > 65535 || number.intValue() == 53)
                    throw new IOException("Invalid DoT port");
                int port = number.intValue();
                Object rawIps = data.getOrDefault("bootstrap_ips", base.bootstrap);
                if (!(rawIps instanceof List<?> ips) || ips.size() > 8) throw new IOException("Expected at most eight bootstrap IPv4 addresses");
                LinkedHashSet<String> bootstrap = new LinkedHashSet<>();
                for (Object value : ips) {
                    if (!(value instanceof String ip)) throw new IOException("Expected literal bootstrap IPv4 addresses");
                    DnsMessage.ipv4(ip); bootstrap.add(ip);
                }
                if (!mode.equals("system") && bootstrap.isEmpty()) throw new IOException("Encrypted DNS needs a numeric bootstrap IPv4 address");
                if (mode.equals("doh") && doh.isEmpty()) throw new IOException("Missing DoH URL");
                if (mode.equals("dot") && dot.isEmpty()) throw new IOException("Missing DoT authentication hostname");
                Config result = new Config(mode, provider, doh, dot, port, new ArrayList<>(bootstrap));
                if (!provider.equals("custom") && !result.equals(base)) throw new IOException("Preset values are fixed; choose Custom to change servers");
                return result;
            } catch (IOException bad) { throw new IllegalArgumentException(bad.getMessage(), bad); }
        }

        public Map<String,Object> toJson() {
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("mode", mode); result.put("provider", provider); result.put("doh_url", dohUrl);
            result.put("dot_host", serverName); result.put("dot_port", port); result.put("bootstrap_ips", bootstrap);
            return result;
        }
        public String mode() { return mode; }
        public String provider() { return provider; }
        public String dohUrl() { return dohUrl; }
        public String serverName() { return serverName; }
        public int port() { return port; }
        public List<String> bootstrapIpv4() { return bootstrap; }
        @Override public boolean equals(Object other) {
            return other instanceof Config value && mode.equals(value.mode) && provider.equals(value.provider)
                    && dohUrl.equals(value.dohUrl) && serverName.equals(value.serverName) && port == value.port && bootstrap.equals(value.bootstrap);
        }
        @Override public int hashCode() { return Objects.hash(mode, provider, dohUrl, serverName, port, bootstrap); }
        private static String text(Map<String,Object> data, String key, String fallback) {
            Object value = data.getOrDefault(key, fallback);
            if (!(value instanceof String text) || text.length() > 8192 || !text.equals(text.trim())) throw new IllegalArgumentException("Invalid DNS text field: " + key);
            return text;
        }
        private static void requireMode(String mode) {
            if (!Set.of("system", "doh", "dot").contains(mode)) throw new IllegalArgumentException("Unknown DNS mode");
        }
    }

    private static final long MAX_NANOS = TimeUnit.SECONDS.toNanos(8);
    private final SSLContext context;
    private final SystemLookup systemLookup;
    private final ThreadPoolExecutor systemPool;
    private final ScheduledThreadPoolExecutor timer;
    private final Set<Budget> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Future<?>> lookups = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public SecureDns() { this(defaultContext(), InetAddress::getAllByName); }
    SecureDns(SSLContext context, SystemLookup systemLookup) {
        this.context = Objects.requireNonNull(context); this.systemLookup = Objects.requireNonNull(systemLookup);
        systemPool = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32), daemon("managed-system-dns"), new ThreadPoolExecutor.AbortPolicy());
        timer = new ScheduledThreadPoolExecutor(1, daemon("managed-dns-deadlines"));
        timer.setRemoveOnCancelPolicy(true);
    }

    public byte[] query(Config config, String gateway, byte[] query, Connector connector, long deadlineNanos) throws IOException {
        Objects.requireNonNull(config); Objects.requireNonNull(query);
        if (closed.get()) throw new IOException("Managed DNS is closed");
        long now = System.nanoTime(), remaining = Math.min(deadlineNanos - now, MAX_NANOS);
        if (remaining <= 0) throw new SocketTimeoutException("DNS deadline expired");
        long deadline = now + remaining;
        byte[] request = query.clone();
        DnsMessage.questionName(request); // Validate before any external I/O.
        if (config.mode.equals("system")) return systemQuery(request, deadline);
        if (connector == null) throw new IOException("Missing numeric DNS connector");
        URI doh = config.mode.equals("doh") ? dohUri(config.dohUrl) : null;
        String hostname = doh == null ? config.serverName : doh.getHost();
        int port = doh == null ? config.port : doh.getPort() < 0 ? 443 : doh.getPort();
        IOException last = null;
        try (Budget budget = new Budget(deadline)) {
            for (String bootstrap : config.bootstrap) {
                budget.check();
                try {
                    Inet4Address address = (Inet4Address)InetAddress.getByAddress(DnsMessage.ipv4(bootstrap));
                    Socket raw = connector.connect(gateway, address, port, Math.min(2000, budget.millis()));
                    if (raw == null) throw new IOException("Numeric DNS connector returned an unconnected socket");
                    budget.own(raw);
                    if (!raw.isConnected()) throw new IOException("Numeric DNS connector returned an unconnected socket");
                    raw.setSoTimeout(budget.millis());
                    try (raw; SSLSocket tls = (SSLSocket)context.getSocketFactory().createSocket(raw, hostname, port, true)) {
                        budget.own(tls);
                        SSLParameters parameters = tls.getSSLParameters();
                        parameters.setEndpointIdentificationAlgorithm("HTTPS");
                        parameters.setServerNames(List.of(new SNIHostName(hostname)));
                        parameters.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
                        if (doh != null) parameters.setApplicationProtocols(new String[]{"h2", "http/1.1"});
                        tls.setSSLParameters(parameters); tls.setSoTimeout(budget.millis());
                        tls.startHandshake(); budget.check();
                        WireInput input = new WireInput(tls, budget);
                        byte[] result = doh == null ? dot(request, tls.getOutputStream(), input, budget)
                                : "h2".equals(tls.getApplicationProtocol())
                                ? Http2Dns.query(request, doh, tls.getOutputStream(), input::exact, budget::check)
                                : doh(request, doh, tls.getOutputStream(), input, budget);
                        DnsMessage.validateResponse(request, result);
                        budget.check();
                        return result;
                    }
                } catch (IOException failure) {
                    budget.check(); last = failure;
                }
            }
        }
        throw new IOException("Encrypted DNS failed for every configured bootstrap address", last);
    }

    private byte[] systemQuery(byte[] query, long deadline) throws IOException {
        Future<InetAddress[]> future;
        try { future = systemPool.submit(() -> systemLookup.resolve(DnsMessage.questionName(query))); }
        catch (RejectedExecutionException full) { throw new IOException("System DNS lookup capacity exhausted", full); }
        lookups.add(future);
        try {
            if (closed.get()) future.cancel(true);
            long left = deadline - System.nanoTime();
            if (left <= 0) throw new TimeoutException();
            InetAddress[] addresses = future.get(left, TimeUnit.NANOSECONDS);
            if (closed.get()) throw new IOException("Managed DNS is closed");
            byte[] result = DnsMessage.systemAnswer(query, addresses);
            if (System.nanoTime() >= deadline) throw new SocketTimeoutException("System DNS deadline expired");
            return result;
        } catch (TimeoutException expired) {
            throw new SocketTimeoutException("System DNS deadline expired");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new InterruptedIOException("System DNS lookup interrupted");
        } catch (ExecutionException failed) {
            throw new IOException("System DNS lookup failed", failed.getCause());
        } catch (CancellationException cancelled) {
            throw new IOException("System DNS lookup cancelled", cancelled);
        } finally {
            future.cancel(true);
            lookups.remove(future);
            if (future instanceof Runnable task) systemPool.remove(task);
        }
    }

    private static byte[] dot(byte[] query, OutputStream output, WireInput input, Budget budget) throws IOException {
        budget.check();
        DataOutputStream out = new DataOutputStream(output);
        out.writeShort(query.length); out.write(query); out.flush();
        int length = (input.one() << 8) | input.one();
        if (length < 12 || length > DnsMessage.MAX_MESSAGE) throw new IOException("Invalid DoT frame length");
        return input.exact(length);
    }

    private static byte[] doh(byte[] query, URI uri, OutputStream output, WireInput input, Budget budget) throws IOException {
        String path = uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String authority = uri.getHost() + (uri.getPort() < 0 || uri.getPort() == 443 ? "" : ":" + uri.getPort());
        String headers = "POST " + path + " HTTP/1.1\r\nHost: " + authority + "\r\nAccept: application/dns-message\r\nContent-Type: application/dns-message\r\nAccept-Encoding: identity\r\nConnection: close\r\nContent-Length: " + query.length + "\r\n\r\n";
        budget.check(); output.write(headers.getBytes(StandardCharsets.US_ASCII)); output.write(query); output.flush();
        HttpHead head;
        int informational = 0;
        do {
            head = input.head();
            if (head.status == 101 || (++informational > 4)) throw new IOException("Unexpected DoH informational response");
        } while (head.status >= 100 && head.status < 200);
        if (head.status != 200) throw new IOException("DoH HTTP status " + head.status);
        String contentType = head.single("content-type", true);
        if (!contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/dns-message")) throw new IOException("Invalid DoH Content-Type");
        String encoding = head.single("content-encoding", false);
        if (encoding != null && !encoding.equalsIgnoreCase("identity")) throw new IOException("Unexpected DoH Content-Encoding");
        String length = head.single("content-length", false), transfer = head.single("transfer-encoding", false);
        if (length != null && transfer != null) throw new IOException("Ambiguous DoH HTTP framing");
        byte[] response;
        if (transfer != null) {
            if (!transfer.equalsIgnoreCase("chunked")) throw new IOException("Unsupported DoH Transfer-Encoding");
            response = input.chunked();
        } else if (length != null) {
            long count = decimal(length, "Content-Length");
            if (count < 12 || count > DnsMessage.MAX_MESSAGE) throw new IOException("Invalid DoH body size");
            response = input.exact((int)count);
        } else response = input.toEnd();
        String age = head.single("age", false);
        return DnsMessage.reduceTtlsByAge(response, age == null ? 0 : decimal(age, "Age"));
    }

    private record HttpHead(int status, Map<String,List<String>> headers) {
        String single(String key, boolean required) throws IOException {
            List<String> values = headers.get(key);
            if (values == null && !required) return null;
            if (values == null || values.size() != 1 || values.get(0).isEmpty()) throw new IOException("Missing or duplicated DoH " + key);
            return values.get(0);
        }
    }

    private static final class WireInput {
        private final Socket socket;
        private final InputStream input;
        private final Budget budget;
        WireInput(Socket socket, Budget budget) throws IOException { this.socket = socket; this.input = socket.getInputStream(); this.budget = budget; }
        int read(byte[] bytes, int offset, int length) throws IOException {
            budget.check(); socket.setSoTimeout(budget.millis()); return input.read(bytes, offset, length);
        }
        int one() throws IOException {
            budget.check(); socket.setSoTimeout(budget.millis());
            int result = input.read(); if (result < 0) throw new EOFException("Truncated encrypted DNS response"); return result;
        }
        byte[] exact(int length) throws IOException {
            byte[] result = new byte[length]; int offset = 0;
            while (offset < length) {
                int count = read(result, offset, length - offset);
                if (count < 0) throw new EOFException("Truncated encrypted DNS response");
                if (count == 0) result[offset++] = (byte)one(); else offset += count;
            }
            return result;
        }
        String line(int maximum) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (bytes.size() <= maximum) {
                int value = one();
                if (value == '\r') {
                    if (one() != '\n') throw new IOException("Invalid DoH HTTP line ending");
                    return bytes.toString(StandardCharsets.ISO_8859_1);
                }
                if (value == '\n' || value == 0 || (value < 32 && value != '\t') || value == 127) throw new IOException("Invalid DoH HTTP character");
                bytes.write(value);
            }
            throw new IOException("DoH HTTP line too large");
        }
        HttpHead head() throws IOException {
            String statusLine = line(512);
            if (!statusLine.matches("HTTP/1\\.[01] [0-9]{3}(?: .*)?")) throw new IOException("Invalid DoH HTTP status line");
            int status = Integer.parseInt(statusLine.substring(9, 12));
            Map<String,List<String>> headers = new LinkedHashMap<>();
            int total = statusLine.length() + 2;
            for (int count = 0; count <= 100; count++) {
                String line = line(8192); total += line.length() + 2;
                if (total > 16384) throw new IOException("DoH HTTP headers too large");
                if (line.isEmpty()) return new HttpHead(status, headers);
                int colon = line.indexOf(':');
                if (colon < 1 || !line.substring(0, colon).matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) throw new IOException("Invalid DoH HTTP header");
                String key = line.substring(0, colon).toLowerCase(Locale.ROOT), value = line.substring(colon + 1).trim();
                headers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
            }
            throw new IOException("Too many DoH HTTP headers");
        }
        byte[] chunked() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int chunks = 0; chunks < 4096; chunks++) {
                String header = line(1024), size = header.split(";", 2)[0].trim();
                if (!size.matches("[0-9a-fA-F]{1,8}")) throw new IOException("Invalid DoH chunk length");
                long length = Long.parseLong(size, 16);
                if (length > DnsMessage.MAX_MESSAGE - output.size()) throw new IOException("DoH body exceeds DNS size limit");
                if (length == 0) {
                    int total = 0;
                    for (int count = 0; count < 100; count++) {
                        String trailer = line(8192); total += trailer.length() + 2;
                        if (total > 16384) throw new IOException("DoH trailers too large");
                        if (trailer.isEmpty()) return output.toByteArray();
                        int colon = trailer.indexOf(':');
                        if (colon < 1 || !trailer.substring(0, colon).matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) throw new IOException("Invalid DoH trailer");
                        if (Set.of("content-length", "transfer-encoding", "content-type", "content-encoding", "age").contains(trailer.substring(0, colon).toLowerCase(Locale.ROOT))) throw new IOException("Unsafe DoH framing trailer");
                    }
                    throw new IOException("Too many DoH trailers");
                }
                output.write(exact((int)length));
                if (one() != '\r' || one() != '\n') throw new IOException("Invalid DoH chunk ending");
            }
            throw new IOException("Too many DoH chunks");
        }
        byte[] toEnd() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] bytes = new byte[4096];
            while (true) {
                int count = read(bytes, 0, Math.min(bytes.length, DnsMessage.MAX_MESSAGE + 1 - output.size()));
                if (count < 0) return output.toByteArray();
                if (count == 0) continue;
                output.write(bytes, 0, count);
                if (output.size() > DnsMessage.MAX_MESSAGE) throw new IOException("DoH body exceeds DNS size limit");
            }
        }
    }

    private final class Budget implements AutoCloseable {
        private final long deadline;
        private final Thread owner = Thread.currentThread();
        private final List<Socket> sockets = new CopyOnWriteArrayList<>();
        private final AtomicBoolean finished = new AtomicBoolean();
        private final ScheduledFuture<?> watch;
        Budget(long deadline) throws IOException {
            this.deadline = deadline;
            try { watch = timer.scheduleWithFixedDelay(() -> { if (closed.get() || owner.isInterrupted() || System.nanoTime() >= deadline) closeSockets(); }, 0, 20, TimeUnit.MILLISECONDS); }
            catch (RejectedExecutionException stopped) { throw new IOException("Managed DNS is closed", stopped); }
            // Publish only after watch is initialized: close() may run concurrently.
            active.add(this);
            if (closed.get()) { close(); throw new IOException("Managed DNS is closed"); }
        }
        void own(Socket socket) throws IOException { sockets.add(socket); if (finished.get() || closed.get()) { closeSockets(); throw new IOException("Managed DNS is closed"); } check(); }
        void check() throws IOException {
            if (owner.isInterrupted()) throw new InterruptedIOException("Encrypted DNS query interrupted");
            if (System.nanoTime() >= deadline) throw new SocketTimeoutException("Encrypted DNS deadline expired");
            if (closed.get() || finished.get()) throw new IOException("Managed DNS is closed");
        }
        int millis() throws IOException { check(); return (int)Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())); }
        private void closeSockets() { for (Socket socket : sockets) try { socket.close(); } catch (IOException ignored) { } }
        @Override public void close() { if (finished.compareAndSet(false, true)) { watch.cancel(false); closeSockets(); active.remove(this); } }
    }

    private static long decimal(String value, String field) throws IOException {
        if (!value.matches("[0-9]{1,19}")) throw new IOException("Invalid DoH " + field);
        try { return Long.parseLong(value); } catch (NumberFormatException bad) { throw new IOException("DoH " + field + " overflow", bad); }
    }
    private static URI dohUri(String text) throws IOException {
        try {
            URI uri = new URI(text);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null || uri.getRawFragment() != null || uri.getRawQuery() != null || uri.getRawAuthority() == null)
                throw new IOException("Expected an HTTPS DoH URL without credentials, query or fragment");
            String authority = uri.getRawAuthority(), host = authority; int port = -1;
            int colon = authority.lastIndexOf(':');
            if (colon >= 0) {
                host = authority.substring(0, colon); String rawPort = authority.substring(colon + 1);
                if (!rawPort.matches("[0-9]{1,5}")) throw new IOException("Invalid DoH port");
                port = Integer.parseInt(rawPort);
                if (port < 1 || port > 65535) throw new IOException("Invalid DoH port");
            }
            host = DnsMessage.hostname(host);
            if (host.matches("[0-9.]+")) throw new IOException("DoH requires an authentication hostname");
            String path = uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            if (!StandardCharsets.US_ASCII.newEncoder().canEncode(path)) path = new URI(uri.toASCIIString()).getRawPath();
            URI normalized = new URI("https://" + host + (port < 0 || port == 443 ? "" : ":" + port) + path);
            if (normalized.toASCIIString().length() > 8192) throw new IOException("Invalid DoH URL");
            return normalized;
        } catch (URISyntaxException | IllegalArgumentException bad) { throw new IOException("Invalid DoH URL"); }
    }
    private static SSLContext defaultContext() {
        try { return SSLContext.getDefault(); }
        catch (java.security.NoSuchAlgorithmException unavailable) { throw new IllegalStateException("Default TLS context unavailable", unavailable); }
    }
    private static ThreadFactory daemon(String name) { return task -> { Thread thread = new Thread(task, name); thread.setDaemon(true); return thread; }; }
    @Override public void close() {
        if (closed.compareAndSet(false, true)) { for (Budget budget : active) budget.close(); for (Future<?> lookup : lookups) lookup.cancel(true); systemPool.shutdownNow(); timer.shutdownNow(); }
    }
}
