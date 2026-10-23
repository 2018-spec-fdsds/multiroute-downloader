package multiroute;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/** Own loopback TLS/DNS fixtures. Never contacts a public DNS service or installed app. */
public final class SecureDnsProtocolTest {
    private static int checks;
    private static final char[] PASSWORD = "owned-fixture-only".toCharArray();
    private static SSLContext trusted, serverContext, expiredContext;
    private static final AtomicInteger osLookups = new AtomicInteger();
    private static final List<String> names = new ArrayList<>();
    private interface Checked { void run() throws Exception; }
    private interface Handler { void run(SSLSocket socket) throws Exception; }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        trusted = context(root.resolve("trust.p12"), false);
        serverContext = context(root.resolve("server.p12"), true);
        expiredContext = context(root.resolve("expired.p12"), true);
        byte[] query = DnsMessage.queryA("example.com");

        check("config defaults and roundtrip", () -> {
            SecureDns.Config config = SecureDns.Config.defaults();
            require(config.mode().equals("doh") && config.provider().equals("cloudflare"));
            require(SecureDns.Config.fromJson(config.toJson()).equals(config));
            for (String provider : List.of("cloudflare", "quad9", "google"))
                for (String mode : List.of("system", "doh", "dot")) require(SecureDns.Config.fromJson(SecureDns.Config.preset(provider, mode).toJson()).mode().equals(mode));
        });
        check("unsafe config rejection", () -> {
            for (Map<String,Object> bad : List.of(
                    Map.<String,Object>of("mode", "gateway"), Map.<String,Object>of("insecure", true),
                    Map.<String,Object>of("provider", "custom", "doh_url", "http://resolver.invalid/dns-query", "bootstrap_ips", List.of("9.9.9.9")),
                    Map.<String,Object>of("provider", "custom", "doh_url", "https://u:p@resolver.invalid/dns-query", "bootstrap_ips", List.of("9.9.9.9")),
                    Map.<String,Object>of("provider", "custom", "doh_url", "https://resolver.invalid/dns-query?token=secret", "bootstrap_ips", List.of("9.9.9.9")),
                    Map.<String,Object>of("provider", "custom", "doh_url", "https://resolver.invalid/dns-query#fragment", "bootstrap_ips", List.of("9.9.9.9")),
                    Map.<String,Object>of("provider", "custom", "doh_url", "https://resolver.invalid/dns-query", "bootstrap_ips", List.of("127.0.0.1")),
                    Map.<String,Object>of("provider", "custom", "doh_url", "https://resolver.invalid/dns-query", "bootstrap_ips", List.of("not-a-number.invalid")),
                    Map.<String,Object>of("mode", "dot", "provider", "custom", "dot_host", "resolver.invalid", "dot_port", 53, "bootstrap_ips", List.of("9.9.9.9"))))
                expectIllegal(() -> SecureDns.Config.fromJson(bad));
        });
        check("Unicode query normalization", () -> require(DnsMessage.questionName(DnsMessage.queryA("пример.рф")).equals("xn--e1afmkfd.xn--p1ai")));
        check("compressed A and public address", () -> require(DnsMessage.publicAddresses(query, answer(query, 60)).equals(List.of("9.9.9.9"))));
        check("wrong ID rejected", () -> { byte[] wire = answer(query, 60); wire[0] ^= 1; expectIo(() -> DnsMessage.validateResponse(query, wire)); });
        check("private A rejected", () -> { byte[] wire = answer(query, 60); wire[wire.length - 4] = 10; expectIo(() -> DnsMessage.validateResponse(query, wire)); });
        check("truncation and trailing bytes rejected", () -> {
            byte[] wire = answer(query, 60); wire[2] |= 2; expectIo(() -> DnsMessage.validateResponse(query, wire));
            expectIo(() -> DnsMessage.validateResponse(query, Arrays.copyOf(answer(query, 60), answer(query, 60).length + 1)));
        });
        check("compression loop and oversize rejected", () -> {
            byte[] wire = answer(query, 60); int start = query.length; wire[start] = (byte)0xc0; wire[start + 1] = (byte)start;
            expectIo(() -> DnsMessage.validateResponse(query, wire)); expectIo(() -> DnsMessage.validateResponse(query, new byte[65536]));
        });
        check("CNAME chain and loop", () -> {
            require(DnsMessage.publicAddresses(query, cname(query, "cdn.example", true)).equals(List.of("9.9.9.9")));
            expectIo(() -> DnsMessage.publicAddresses(query, cname(query, "example.com", false)));
        });
        check("DNS RCODE distinguished from transport", () -> {
            byte[] wire = query.clone(); wire[2] = (byte)0x81; wire[3] = (byte)0x83;
            DnsMessage.validateResponse(query, wire); require(DnsMessage.rcode(wire) == 3); expectIo(() -> DnsMessage.publicAddresses(query, wire));
        });
        check("Age reduces TTL to zero without extending", () -> {
            byte[] wire = answer(query, 60); require(ttl(DnsMessage.reduceTtlsByAge(wire, 10)) == 50);
            require(ttl(DnsMessage.reduceTtlsByAge(wire, 1000)) == 0); expectIo(() -> DnsMessage.reduceTtlsByAge(wire, -1));
        });
        check("Age leaves EDNS OPT flags unchanged", () -> {
            byte[] wire = answer(query, 60); ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bytes.write(wire);
            try (DataOutputStream out = new DataOutputStream(bytes)) { out.writeByte(0); out.writeShort(41); out.writeShort(1232); out.writeInt(0x8000); out.writeShort(0); }
            byte[] full = bytes.toByteArray(); full[11] = 1; byte[] aged = DnsMessage.reduceTtlsByAge(full, 50);
            require(Arrays.equals(Arrays.copyOfRange(full, wire.length, full.length), Arrays.copyOfRange(aged, wire.length, aged.length)));
        });
        check("system isolation and TTL zero", () -> {
            AtomicInteger calls = new AtomicInteger();
            try (SecureDns dns = new SecureDns(trusted, host -> { calls.incrementAndGet(); return new InetAddress[]{InetAddress.getByAddress(new byte[]{9,9,9,9}), InetAddress.getByAddress(new byte[]{10,0,0,1})}; })) {
                byte[] result = dns.query(SecureDns.Config.preset("cloudflare", "system"), "192.168.1.1", query, (g,a,p,t) -> { throw new AssertionError("System mode used gateway egress"); }, after(1000));
                require(calls.get() == 1 && ttl(result) == 0 && DnsMessage.publicAddresses(query, result).size() == 1);
            }
        });
        check("system total timeout and bounded caller", () -> {
            try (SecureDns dns = new SecureDns(trusted, host -> { try { Thread.sleep(2000); } catch (InterruptedException ignored) { } return new InetAddress[]{InetAddress.getByAddress(new byte[]{9,9,9,9})}; })) {
                long start = System.nanoTime(); expectIo(() -> dns.query(SecureDns.Config.preset("google", "system"), "192.168.1.1", query, null, after(100)));
                require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 700);
            }
        });

        check("DoH length POST SNI hostname gateway numeric bootstrap", () -> {
            AtomicReference<String> request = new AtomicReference<>(); AtomicReference<String> sni = new AtomicReference<>();
            try (Server server = new Server(serverContext, socket -> {
                sni.set(((SNIHostName)((ExtendedSSLSession)socket.getSession()).getRequestedServerNames().get(0)).getAsciiName());
                byte[] received = httpQuery(socket.getInputStream(), request);
                byte[] wire = answer(received, 60); send(socket, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: " + wire.length + "\r\nAge: 10\r\n\r\n", wire);
            }); SecureDns dns = secure(trusted)) {
                List<String> routes = new ArrayList<>(); byte[] response = dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.3", query, connector(server.port(), routes), after(2000));
                require(ttl(response) == 50 && sni.get().equals("resolver.invalid"));
                require(routes.equals(List.of("192.168.1.3|9.9.9.9|" + server.port())) && request.get().contains("POST /dns-query HTTP/1.1") && request.get().contains("Host: resolver.invalid:"));
            }
        });
        check("DoH chunked and interim response", () -> {
            try (Server server = new Server(serverContext, socket -> {
                byte[] received = httpQuery(socket.getInputStream(), null), wire = answer(received, 60);
                socket.getOutputStream().write("HTTP/1.1 100 Continue\r\n\r\nHTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nTransfer-Encoding: chunked\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write((Integer.toHexString(wire.length) + ";own=yes\r\n").getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write(wire); socket.getOutputStream().write("\r\n0\r\nX-Own-Fixture: yes\r\n\r\n".getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush();
            }); SecureDns dns = secure(trusted)) {
                require(DnsMessage.publicAddresses(query, dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000))).equals(List.of("9.9.9.9")));
            }
        });
        check("DoH EOF body", () -> {
            try (Server server = new Server(serverContext, socket -> { byte[] received = httpQuery(socket.getInputStream(), null); send(socket, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\n\r\n", answer(received, 60)); }); SecureDns dns = secure(trusted)) {
                dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.2", query, connector(server.port(), new ArrayList<>()), after(2000));
            }
        });
        List<String> badHeaders = List.of(
                "HTTP/1.1 302 Found\r\nLocation: https://other.invalid/dns-query\r\nContent-Type: application/dns-message\r\nContent-Length: 12\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 12\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Encoding: gzip\r\nContent-Length: 12\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: 12\r\nContent-Length: 12\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: 12\r\nTransfer-Encoding: chunked\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: 65536\r\n\r\n",
                "HTTP/1.1 429 Too Many Requests\r\nContent-Type: application/dns-message\r\nContent-Length: 12\r\n\r\n",
                "HTTP/1.1 503 Unavailable\r\nContent-Type: application/dns-message\r\nContent-Length: 12\r\n\r\n");
        for (int i = 0; i < badHeaders.size(); i++) {
            String header = badHeaders.get(i); int index = i;
            check("DoH HTTP rejection " + index, () -> badHttp(query, header, new byte[12]));
        }
        for (String age : List.of("-1", "99999999999999999999")) check("DoH invalid Age " + age, () -> {
            try (Server server = new Server(serverContext, socket -> { byte[] wire = answer(httpQuery(socket.getInputStream(), null), 60); send(socket, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: " + wire.length + "\r\nAge: " + age + "\r\n\r\n", wire); }); SecureDns dns = secure(trusted)) {
                expectIo(() -> dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000)));
            }
        });
        check("DoH invalid chunk framing", () -> badHttp(query, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nTransfer-Encoding: chunked\r\n\r\n", "not-hex\r\n".getBytes(StandardCharsets.US_ASCII)));
        check("DoH oversized headers", () -> badHttp(query, "HTTP/1.1 200 OK\r\nX-Huge: " + "a".repeat(17000) + "\r\n\r\n", new byte[0]));
        check("DoH short body", () -> badHttp(query, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: 50\r\n\r\n", new byte[12]));
        check("DoH mismatched DNS ID", () -> {
            try (Server server = new Server(serverContext, socket -> { byte[] wire = answer(httpQuery(socket.getInputStream(), null), 60); wire[0] ^= 1; send(socket, "HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: " + wire.length + "\r\n\r\n", wire); }); SecureDns dns = secure(trusted)) {
                expectIo(() -> dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000)));
            }
        });
        check("DoT length frame and gateway affinity", () -> {
            try (Server server = new Server(serverContext, socket -> {
                byte[] prefix = socket.getInputStream().readNBytes(2); int length = ((prefix[0] & 255) << 8) | (prefix[1] & 255);
                byte[] received = socket.getInputStream().readNBytes(length), wire = answer(received, 60);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream()); out.writeShort(wire.length); out.write(wire); out.flush();
            }); SecureDns dns = secure(trusted)) {
                List<String> routes = new ArrayList<>(); byte[] response = dns.query(config("dot", server.port(), "resolver.invalid"), "192.168.1.2", query, connector(server.port(), routes), after(2000));
                require(DnsMessage.publicAddresses(query, response).equals(List.of("9.9.9.9")) && routes.get(0).startsWith("192.168.1.2|9.9.9.9|"));
            }
        });
        check("DoT invalid zero frame", () -> {
            try (Server server = new Server(serverContext, socket -> { socket.getInputStream().readNBytes(2); socket.getOutputStream().write(new byte[]{0,0}); socket.getOutputStream().flush(); }); SecureDns dns = secure(trusted)) {
                expectIo(() -> dns.query(config("dot", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000)));
            }
        });
        check("TLS hostname verification", () -> tlsFailure(query, trusted, serverContext, "other.invalid"));
        check("TLS CA verification", () -> tlsFailure(query, SSLContext.getDefault(), serverContext, "resolver.invalid"));
        check("TLS expiry verification", () -> tlsFailure(query, trusted, expiredContext, "resolver.invalid"));
        check("TLS handshake absolute deadline", () -> {
            try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()); SecureDns dns = secure(trusted)) {
                AtomicReference<Socket> accepted = new AtomicReference<>();
                Thread server = new Thread(() -> { try (Socket socket = listener.accept()) { accepted.set(socket); Thread.sleep(1000); } catch (Exception ignored) { } }); server.setDaemon(true); server.start();
                long start = System.nanoTime();
                expectIo(() -> dns.query(config("dot", listener.getLocalPort(), "resolver.invalid"), "192.168.1.1", query, connector(listener.getLocalPort(), new ArrayList<>()), after(200)));
                require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 900);
                if (accepted.get() != null) accepted.get().close();
            }
        });
        check("DoH drip-feed total deadline", () -> {
            try (Server server = new Server(serverContext, socket -> { httpQuery(socket.getInputStream(), null); for (byte value : "HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.US_ASCII)) { socket.getOutputStream().write(value); socket.getOutputStream().flush(); Thread.sleep(80); } }); SecureDns dns = secure(trusted)) {
                long start = System.nanoTime(); expectIo(() -> dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(250)));
                require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
            }
        });
        check("encrypted connector failure never calls system", () -> {
            try (SecureDns dns = secure(trusted)) {
                AtomicInteger attempts = new AtomicInteger(); expectIo(() -> dns.query(SecureDns.Config.defaults(), "192.168.1.1", query, (g,a,p,t) -> { attempts.incrementAndGet(); throw new IOException("owned unavailable connector"); }, after(1000)));
                require(attempts.get() == 2 && osLookups.get() == 0);
            }
        });
        check("interruption closes only owned query sockets", () -> cancellation(query, false));
        check("resolver close cancels in-flight query only", () -> cancellation(query, true));
        check("closed resolver rejects work", () -> { SecureDns dns = secure(trusted); dns.close(); expectIo(() -> dns.query(SecureDns.Config.defaults(), "192.168.1.1", query, null, after(1000))); });
        require(osLookups.get() == 0);
        StringBuilder report = new StringBuilder("{\n  \"status\": \"PASS\",\n  \"checks\": " + checks + ",\n  \"owned_loopback\": true,\n  \"no_public_dns\": true,\n  \"no_os_fallback\": true,\n  \"headless\": true,\n  \"external_network_used\": false,\n  \"cases\": [\n");
        for (int i = 0; i < names.size(); i++) report.append("    ").append(jsonString(names.get(i))).append(i + 1 == names.size() ? "\n" : ",\n");
        report.append("  ]\n}\n");
        Files.writeString(root.resolve("SECURE-DNS-PROTOCOL-TESTS.json"), report, StandardCharsets.UTF_8);
        System.out.println("SECURE_DNS_PROTOCOL_PASS checks=" + checks + " owned_tls=true public_dns_requests=0 os_fallback_calls=0 utf8=Кириллица✓");
        for (String name : names) System.out.println("PASS " + name);
    }

    private static SecureDns secure(SSLContext context) { return new SecureDns(context, host -> { osLookups.incrementAndGet(); throw new UnknownHostExceptionForFixture(); }); }
    private static final class UnknownHostExceptionForFixture extends java.net.UnknownHostException { }
    private static long after(long millis) { return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis); }
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Owned DNS fixture assertion failed"); }
    private static void check(String name, Checked test) throws Exception { test.run(); checks++; names.add(name); }
    private static void expectIo(Checked test) throws Exception { try { test.run(); } catch (IOException expected) { return; } throw new AssertionError("Expected IOException"); }
    private static void expectIllegal(Checked test) throws Exception { try { test.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("Expected config rejection"); }
    private static SecureDns.Config config(String mode, int port, String name) {
        return SecureDns.Config.fromJson(Map.of("mode", mode, "provider", "custom", "doh_url", "https://" + name + ":" + port + "/dns-query", "dot_host", name, "dot_port", port, "bootstrap_ips", List.of("9.9.9.9")));
    }
    private static SecureDns.Connector connector(int localPort, List<String> routes) {
        return (gateway, address, port, timeout) -> {
            require(address.getHostAddress().equals("9.9.9.9")); routes.add(gateway + "|" + address.getHostAddress() + "|" + port);
            Socket socket = new Socket(); socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), localPort), timeout); return socket;
        };
    }
    private static SSLContext context(Path store, boolean server) throws Exception {
        KeyStore keys = KeyStore.getInstance("PKCS12"); try (InputStream in = Files.newInputStream(store)) { keys.load(in, PASSWORD); }
        SSLContext context = SSLContext.getInstance("TLS");
        if (server) { KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); factory.init(keys, PASSWORD); context.init(factory.getKeyManagers(), null, null); }
        else { TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); factory.init(keys); context.init(null, factory.getTrustManagers(), null); }
        return context;
    }
    private static final class Server implements AutoCloseable {
        final SSLServerSocket listener; final Thread thread; volatile SSLSocket accepted;
        Server(SSLContext context, Handler handler) throws IOException {
            listener = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getLoopbackAddress());
            thread = new Thread(() -> { try (SSLSocket socket = (SSLSocket)listener.accept()) { accepted = socket; socket.setSoTimeout(3000); socket.startHandshake(); handler.run(socket); } catch (Exception ignored) { } }, "owned-dns-tls-server");
            thread.setDaemon(true); thread.start();
        }
        int port() { return listener.getLocalPort(); }
        @Override public void close() throws Exception { listener.close(); if (accepted != null) accepted.close(); thread.interrupt(); thread.join(500); }
    }
    private static byte[] httpQuery(InputStream input, AtomicReference<String> request) throws IOException {
        ByteArrayOutputStream headers = new ByteArrayOutputStream(); int previous = 0, matched = 0;
        while (headers.size() < 16384 && matched < 4) { int value = input.read(); if (value < 0) throw new IOException("Own HTTP request truncated"); headers.write(value); matched = value == (matched == 0 || matched == 2 ? '\r' : '\n') ? matched + 1 : value == '\r' ? 1 : 0; previous = value; }
        String text = headers.toString(StandardCharsets.US_ASCII); if (request != null) request.set(text);
        int length = -1;
        for (String line : text.split("\r\n")) if (line.startsWith("Content-Length: ")) length = Integer.parseInt(line.substring(16));
        if (length < 12 || length > 4096) throw new IOException("Own HTTP query length invalid");
        byte[] query = input.readNBytes(length); if (query.length != length) throw new IOException("Own HTTP query truncated"); return query;
    }
    private static void send(SSLSocket socket, String header, byte[] body) throws IOException { socket.getOutputStream().write(header.getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().write(body); socket.getOutputStream().flush(); }
    private static void badHttp(byte[] query, String headers, byte[] body) throws Exception {
        try (Server server = new Server(serverContext, socket -> { httpQuery(socket.getInputStream(), null); send(socket, headers, body); }); SecureDns dns = secure(trusted)) {
            expectIo(() -> dns.query(config("doh", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000)));
        }
    }
    private static void tlsFailure(byte[] query, SSLContext client, SSLContext serverContext, String hostname) throws Exception {
        AtomicInteger sent = new AtomicInteger();
        try (Server server = new Server(serverContext, socket -> { sent.incrementAndGet(); }); SecureDns dns = secure(client)) {
            expectIo(() -> dns.query(config("dot", server.port(), hostname), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(2000)));
            require(sent.get() == 0);
        }
    }
    private static void cancellation(byte[] query, boolean close) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        try (Server server = new Server(serverContext, socket -> {
            byte[] prefix = socket.getInputStream().readNBytes(2);
            socket.getInputStream().readNBytes(((prefix[0] & 255) << 8) | (prefix[1] & 255));
            entered.countDown(); Thread.sleep(3000);
        }); SecureDns dns = secure(trusted);
             ServerSocket unrelated = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket untouched = new Socket(InetAddress.getLoopbackAddress(), unrelated.getLocalPort());
             Socket peer = unrelated.accept()) {
            AtomicReference<Throwable> result = new AtomicReference<>();
            Thread worker = new Thread(() -> { try { dns.query(config("dot", server.port(), "resolver.invalid"), "192.168.1.1", query, connector(server.port(), new ArrayList<>()), after(8000)); } catch (Throwable error) { result.set(error); } }, "owned-cancel-test");
            worker.setDaemon(true); worker.start(); require(entered.await(2, TimeUnit.SECONDS));
            if (close) dns.close(); else worker.interrupt();
            worker.join(700); require(!worker.isAlive() && result.get() instanceof IOException);
            untouched.getOutputStream().write(42); untouched.getOutputStream().flush(); peer.setSoTimeout(500); require(peer.getInputStream().read() == 42);
        }
    }
    private static String jsonString(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""; }
    private static byte[] answer(byte[] query, long ttl) throws IOException {
        byte[] response = DnsMessage.systemAnswer(query, new InetAddress[]{InetAddress.getByAddress(new byte[]{9,9,9,9})});
        int offset = response.length - 10; for (int i = 3; i >= 0; i--) { response[offset + i] = (byte)ttl; ttl >>>= 8; } return response;
    }
    private static long ttl(byte[] answer) { int offset = answer.length - 10; return ((long)(answer[offset] & 255) << 24) | ((long)(answer[offset + 1] & 255) << 16) | ((long)(answer[offset + 2] & 255) << 8) | (answer[offset + 3] & 255); }
    private static byte[] cname(byte[] query, String target, boolean address) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] prefix = query.clone(); prefix[2] = (byte)0x81; prefix[3] = (byte)0x80; prefix[7] = (byte)(address ? 2 : 1); bytes.write(prefix);
        ByteArrayOutputStream targetBytes = new ByteArrayOutputStream(); for (String label : target.split("\\.")) { targetBytes.write(label.length()); targetBytes.write(label.getBytes(StandardCharsets.US_ASCII)); } targetBytes.write(0);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeShort(0xc00c); out.writeShort(5); out.writeShort(1); out.writeInt(60); out.writeShort(targetBytes.size()); out.write(targetBytes.toByteArray());
            if (address) { out.write(targetBytes.toByteArray()); out.writeShort(1); out.writeShort(1); out.writeInt(30); out.writeShort(4); out.write(new byte[]{9,9,9,9}); }
        }
        return bytes.toByteArray();
    }
}
