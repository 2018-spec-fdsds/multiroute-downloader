package multiroute;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.HttpServer;

/** Owned localhost integration. Resolver addresses are logical fixture targets;
 * the simulated native tunnel terminates TLS locally and never contacts them. */
public final class ManagedDnsTest {
    private static final List<String> GATEWAYS = List.of("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4");
    private static final byte[] MAGIC = "MRDNS2".getBytes(StandardCharsets.US_ASCII);
    private static final List<String> cases = new ArrayList<>();
    private static int checks;
    private static Path root;
    private static SSLContext trusted, serverContext;
    private static final AtomicInteger systemCalls = new AtomicInteger();
    private interface Checked { void run() throws Exception; }
    private record Reply(int status, byte[] body) {
        String text() throws CharacterCodingException { return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(body)).toString(); }
    }

    public static void main(String[] args) throws Exception {
        root = Path.of(args[0]).toAbsolutePath().normalize(); Files.createDirectories(root);
        trusted = context(root.resolve("trust.p12"), false); serverContext = context(root.resolve("server.p12"), true);
        AppLog.initialize(root.resolve("журнал-日本語"));
        try {
            check("defaults, isolated persistence and Unicode profile path", ManagedDnsTest::persistence);
            check("invalid saved settings fail closed", ManagedDnsTest::invalidSaved);
            check("authenticated MRDNS2 system lookups and all four gateways", ManagedDnsTest::systemBroker);
            check("MRDNT1 TLS egress, exact policy revisions and rollback", ManagedDnsTest::encryptedBroker);
            check("generation refresh, cancellation and unrelated socket ownership", ManagedDnsTest::generations);
            check("deterministic authenticated-generation and system-result race guards", ManagedDnsTest::generationGuards);
            check("draft testing is independent of applied configuration", ManagedDnsTest::draftTests);
            check("ongoing DNS failures retain periodic logging eligibility", ManagedDnsTest::loggingCooldown);
            check("real localhost DNS API, persistence and native V4 rollback", ManagedDnsTest::apiIntegration);
            check("bounded broker queue and socket registry cleanup", ManagedDnsTest::queueCleanup);
            AppLog.flush();
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("status", "PASS"); result.put("version", Main.VERSION); result.put("checks", checks); result.put("cases", cases);
            result.put("loopback_only", true); result.put("external_dns_queries", false); result.put("real_os_dns", false);
            result.put("installed_application_accessed", false); result.put("os_network_changed", false);
            result.put("utf8_path", "журнал-日本語"); result.put("expiry_clock_injected", true);
            result.put("generation_race_window_injected", true);
            Files.writeString(root.resolve("MANAGED-DNS-TESTS.json"), Json.stringify(result) + "\n", StandardCharsets.UTF_8);
            if (Files.readString(root.resolve("MANAGED-DNS-TESTS.json"), StandardCharsets.UTF_8).contains("\ufffd")) throw new AssertionError("UTF-8 report roundtrip");
            System.out.println("MANAGED_DNS_TEST_OK checks=" + checks + " loopback_only=true os_dns=false");
        } finally { AppLog.close(); }
    }

    private static void persistence() throws Exception {
        Path directory = directory("профиль-日本語");
        try (ManagedDns dns = owner(directory, host -> publicAnswer())) {
            require(dns.snapshot().config().equals(SecureDns.Config.defaults()) && dns.snapshot().revision() == 1 && dns.snapshot().error().isBlank());
            require(Boolean.TRUE.equals(dns.settings().get("encrypted")) && Boolean.FALSE.equals(dns.settings().get("plaintext_fallback")));
            expectIo(() -> dns.publishEgress(1234, GATEWAYS));
            ManagedDns.Bridge bridge = dns.beginGeneration(); require(bridge.port() > 0 && bridge.key().matches("[a-f0-9]{64}"));
            expectIo(() -> dns.publishEgress(0, GATEWAYS)); expectIo(() -> dns.publishEgress(1234, List.of()));
            dns.stage(new ManagedDns.State(system(), 2, "")); dns.persist();
            String text = Files.readString(directory.resolve("dns-settings.json"), StandardCharsets.UTF_8);
            require(!text.contains("\ufffd") && !text.contains(bridge.key()) && text.contains("\"revision\":2"));
        }
        try (ManagedDns restored = owner(directory, host -> publicAnswer())) {
            require(restored.snapshot().config().equals(system()) && restored.snapshot().revision() == 2 && restored.snapshot().error().isBlank());
            require(Boolean.FALSE.equals(restored.settings().get("helper_ready")));
        }
    }

    private static void invalidSaved() throws Exception {
        List<Object> revisions = Arrays.asList(0, -1, 1.5, "2", Long.MAX_VALUE, null);
        for (int i = 0; i < revisions.size(); i++) {
            Path directory = directory("invalid-revision-" + i); Map<String,Object> saved = new LinkedHashMap<>(system().toJson()); saved.put("revision", revisions.get(i));
            Files.writeString(directory.resolve("dns-settings.json"), Json.stringify(saved), StandardCharsets.UTF_8);
            try (ManagedDns dns = owner(directory, host -> { throw new AssertionError("Invalid saved policy used OS lookup"); })) {
                require(!dns.snapshot().error().isBlank() && dns.snapshot().revision() >= 1);
                ManagedDns.Bridge bridge = dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS);
                require(query(bridge, bridge.key(), "192.168.1.1", dns.snapshot().revision(), 1000, DnsMessage.queryA("example.com")).status() == 0);
            }
        }
        for (String text : List.of("{broken", "{\"mode\":\"plain\",\"revision\":1}", "x".repeat(65537))) {
            Path directory = directory("invalid-file-" + cases.size() + "-" + checks); Files.writeString(directory.resolve("dns-settings.json"), text, StandardCharsets.UTF_8);
            try (ManagedDns dns = owner(directory, host -> publicAnswer())) { require(!dns.snapshot().error().isBlank()); }
        }
        Path directory = directory("invalid-directory"); Files.createDirectory(directory.resolve("dns-settings.json"));
        try (ManagedDns dns = owner(directory, host -> publicAnswer())) { require(!dns.snapshot().error().isBlank()); }
    }

    private static void systemBroker() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (ManagedDns dns = owner(directory("system-broker"), host -> { calls.incrementAndGet(); return publicAnswer(); })) {
            dns.stage(new ManagedDns.State(system(), 2, "")); ManagedDns.Bridge bridge = dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS);
            byte[] wire = DnsMessage.queryA("пример.рф");
            for (String gateway : GATEWAYS) {
                Reply result = query(bridge, bridge.key(), gateway, 2, 1000, wire);
                require(result.status() == 1 && DnsMessage.publicAddresses(wire, result.body()).equals(List.of("9.9.9.9")) && ttl(result.body()) == 0);
            }
            // Diagnostics are published after the reply is flushed. Await the worker's
            // finally block (which removes its owned socket), not a scheduler-dependent delay.
            waitEmpty(dns);
            require(calls.get() == 4 && Main.arr(dns.settings().get("gateway_status")).size() == 4);
            int before = calls.get();
            expectIo(() -> query(bridge, "b".repeat(64), "192.168.1.1", 2, 1000, wire));
            for (long revision : new long[]{0, -1, 99999}) require(query(bridge, bridge.key(), "192.168.1.1", revision, 1000, wire).status() == 0);
            require(query(bridge, bridge.key(), "192.168.1.99", 2, 1000, wire).status() == 0);
            for (int budget : new int[]{0, 8001, -1}) require(query(bridge, bridge.key(), "192.168.1.1", 2, budget, wire).status() == 0);
            try (Socket socket = brokerSocket(bridge)) { socket.getOutputStream().write("MRDNS1".getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().flush(); require(socket.getInputStream().read() == -1); }
            byte[] malformed = packet(bridge.key(), "192.168.1.1", 2, 1000, wire); malformed[85] = 42;
            require(exchange(bridge, malformed).status() == 0);
            malformed = packet(bridge.key(), "192.168.1.1", 2, 1000, wire); malformed[98] = 0; malformed[99] = 11;
            require(exchange(bridge, malformed).status() == 0);
            malformed = packet(bridge.key(), "192.168.1.1", 2, 1000, wire); malformed[98] = 16; malformed[99] = 1;
            require(exchange(bridge, malformed).status() == 0);
            expire(dns, 1); require(query(bridge, bridge.key(), "192.168.1.1", 1, 1000, wire).status() == 0);
            require(calls.get() == before);
            long started = System.nanoTime();
            try (Socket socket = brokerSocket(bridge)) {
                byte[] request = packet(bridge.key(), "192.168.1.1", 2, 80, wire); socket.getOutputStream().write(request, 0, 98); socket.getOutputStream().flush(); Thread.sleep(120);
                require(readReply(socket).status() == 0);
            }
            require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 700 && calls.get() == before);
            dns.invalidateGeneration(); require(Boolean.FALSE.equals(dns.settings().get("helper_ready")));
            require(query(bridge, bridge.key(), "192.168.1.1", 2, 1000, wire).status() == 0);
        }
    }

    private static void encryptedBroker() throws Exception {
        AtomicInteger unexpected = new AtomicInteger();
        try (ManagedDns dns = owner(directory("encrypted-broker"), host -> { unexpected.incrementAndGet(); throw new UnknownHostException("No real OS lookup allowed"); }); NativeEgress nativeSide = new NativeEgress()) {
            ManagedDns.Bridge bridge = dns.beginGeneration(); nativeSide.key.set(bridge.key()); dns.publishEgress(nativeSide.port(), GATEWAYS);
            byte[] wire = DnsMessage.queryA("example.com"); SecureDns.Config doh = secureConfig("doh"), dot = secureConfig("dot");
            dns.stage(new ManagedDns.State(doh, 2, ""));
            for (String gateway : GATEWAYS) require(query(bridge, bridge.key(), gateway, 2, 2000, wire).status() == 1);
            require(nativeSide.routes.size() == 4 && nativeSide.routes.stream().allMatch(value -> value.endsWith("|9.9.9.9|8443")));
            for (String gateway : GATEWAYS) require(nativeSide.routes.stream().anyMatch(value -> value.startsWith(gateway + "|")));
            dns.stage(new ManagedDns.State(dot, 3, "")); require(Main.arr(dns.settings().get("gateway_status")).isEmpty());
            require(query(bridge, bridge.key(), "192.168.1.2", 2, 2000, wire).status() == 1);
            require(nativeSide.routes.get(nativeSide.routes.size() - 1).endsWith("|8443") && Main.arr(dns.settings().get("gateway_status")).isEmpty());
            for (String gateway : GATEWAYS) require(query(bridge, bridge.key(), gateway, 3, 2000, wire).status() == 1);
            require(nativeSide.routes.stream().filter(value -> value.endsWith("|8853")).count() == 4 && unexpected.get() == 0);
            require(Main.arr(dns.settings().get("gateway_status")).stream().allMatch(raw -> Main.num(Main.obj(raw).get("revision"), -1) == 3));
            dns.stage(new ManagedDns.State(doh, 4, "")); dns.persist(); require(query(bridge, bridge.key(), "192.168.1.1", 4, 2000, wire).status() == 1);
            require(dns.snapshot().config().equals(doh) && dns.snapshot().revision() == 4);
            int before = nativeSide.routes.size(); expire(dns, 2);
            require(query(bridge, bridge.key(), "192.168.1.1", 2, 2000, wire).status() == 0 && nativeSide.routes.size() == before);
            nativeSide.reject.set(true); require(query(bridge, bridge.key(), "192.168.1.1", 4, 2000, wire).status() == 0 && unexpected.get() == 0);
            nativeSide.reject.set(false); require(nativeSide.sni.stream().allMatch("resolver.invalid"::equals));
            require(!Json.stringify(dns.settings()).contains(bridge.key())); nativeSide.assertHealthy();
        }
    }

    private static void generations() throws Exception {
        try (ManagedDns dns = owner(directory("generation"), host -> { throw new UnknownHostException(); }); NativeEgress nativeSide = new NativeEgress();
             ServerSocket unrelated = new ServerSocket(0, 1, loopback()); Socket untouched = new Socket(loopback(), unrelated.getLocalPort()); Socket peer = unrelated.accept()) {
            ManagedDns.Bridge bridge = dns.beginGeneration(); nativeSide.key.set(bridge.key()); dns.publishEgress(nativeSide.port(), GATEWAYS); dns.stage(new ManagedDns.State(secureConfig("dot"), 2, ""));
            byte[] wire = DnsMessage.queryA("example.com"); nativeSide.hold();
            CompletableFuture<Reply> pending = async(() -> query(bridge, bridge.key(), "192.168.1.3", 2, 2000, wire));
            require(nativeSide.entered.await(1, TimeUnit.SECONDS)); dns.publishEgress(nativeSide.port(), GATEWAYS); nativeSide.release.countDown();
            require(pending.get(3, TimeUnit.SECONDS).status() == 1);
            nativeSide.hold(); pending = async(() -> query(bridge, bridge.key(), "192.168.1.3", 2, 2000, wire)); require(nativeSide.entered.await(1, TimeUnit.SECONDS));
            ManagedDns.Bridge replacement = dns.beginGeneration(); nativeSide.release.countDown(); require(!replacement.key().equals(bridge.key()));
            try { pending.get(2, TimeUnit.SECONDS); throw new AssertionError("Old generation request survived cancellation"); } catch (ExecutionException expected) { require(expected.getCause() instanceof IOException); }
            expectIo(() -> query(replacement, bridge.key(), "192.168.1.1", 2, 1000, wire));
            nativeSide.key.set(replacement.key()); dns.publishEgress(nativeSide.port(), GATEWAYS); require(query(replacement, replacement.key(), "192.168.1.4", 2, 2000, wire).status() == 1);
            untouched.getOutputStream().write(42); untouched.getOutputStream().flush(); peer.setSoTimeout(500); require(peer.getInputStream().read() == 42);
            dns.close(); waitEmpty(dns); expectIo(dns::beginGeneration); nativeSide.assertHealthy();
        }
    }

    private static void draftTests() throws Exception {
        AtomicReference<String> lookedUp = new AtomicReference<>();
        try (ManagedDns dns = owner(directory("draft"), host -> { lookedUp.set(host); return publicAnswer(); })) {
            ManagedDns.State before = dns.snapshot(); dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS);
            Map<String,Object> result = dns.test(system(), GATEWAYS, "пример.рф");
            require(Boolean.TRUE.equals(result.get("ok")) && Main.arr(result.get("results")).size() == 4 && lookedUp.get().equals("xn--e1afmkfd.xn--p1ai"));
            require(dns.snapshot().equals(before) && !Files.exists(directoryOf(dns).resolve("dns-settings.json")));
            expectIo(() -> dns.test(system(), List.of(), "example.com")); expectIo(() -> dns.test(system(), List.of(GATEWAYS.get(0), GATEWAYS.get(0)), "example.com")); expectIo(() -> dns.test(system(), List.of("192.168.1.99"), "example.com"));
        }
    }

    private static void generationGuards() throws Exception {
        AtomicInteger calls = new AtomicInteger(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try (ManagedDns dns = owner(directory("generation-guards"), host -> { calls.incrementAndGet(); entered.countDown(); try { release.await(); } catch (InterruptedException end) { Thread.currentThread().interrupt(); throw new UnknownHostException(); } return publicAnswer(); })) {
            ManagedDns.Bridge bridge = dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS); dns.stage(new ManagedDns.State(system(), 2, "")); byte[] wire = DnsMessage.queryA("example.com");
            Field keyField = ManagedDns.class.getDeclaredField("key"); keyField.setAccessible(true); keyField.set(dns, "b".repeat(64));
            ManagedDns.Bridge mismatched = new ManagedDns.Bridge(bridge.port(), "b".repeat(64));
            require(query(mismatched, mismatched.key(), GATEWAYS.get(0), 2, 1000, wire).status() == 0 && calls.get() == 0);
            keyField.set(dns, bridge.key()); waitEmpty(dns);
            CompletableFuture<Reply> pending = async(() -> query(bridge, bridge.key(), GATEWAYS.get(0), 2, 2000, wire)); require(entered.await(1, TimeUnit.SECONDS));
            /* Deterministically model an accept missed by the generation invalidation
             * iterator. Only the fixture's own incoming socket escapes cancellation. */
            Set<Socket> registry = socketRegistry(dns); List<Socket> escaped = new ArrayList<>(registry); require(escaped.size() == 1); registry.removeAll(escaped);
            ManagedDns.Bridge replacement = dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS); require(!replacement.key().equals(bridge.key())); release.countDown();
            try { require(pending.get(2, TimeUnit.SECONDS).status() == 0); } finally { for (Socket socket : escaped) socket.close(); }
            waitEmpty(dns);
        } finally { release.countDown(); }
    }

    private static void queueCleanup() throws Exception {
        CountDownLatch entered = new CountDownLatch(4), release = new CountDownLatch(1); List<Socket> clients = new ArrayList<>();
        try (ManagedDns dns = owner(directory("bounded-queue"), host -> { entered.countDown(); try { release.await(); } catch (InterruptedException end) { Thread.currentThread().interrupt(); throw new UnknownHostException("owned lookup cancelled"); } return publicAnswer(); })) {
            ManagedDns.Bridge bridge = dns.beginGeneration(); dns.stage(new ManagedDns.State(system(), 2, "")); dns.publishEgress(1234, GATEWAYS);
            byte[] request = packet(bridge.key(), GATEWAYS.get(0), 2, 4000, DnsMessage.queryA("example.com"));
            for (int i = 0; i < 40; i++) { Socket client = brokerSocket(bridge); clients.add(client); try { client.getOutputStream().write(request); client.getOutputStream().flush(); } catch (IOException rejected) { } }
            require(entered.await(1, TimeUnit.SECONDS)); Thread.sleep(100);
            require(socketCount(dns) <= 24); dns.invalidateGeneration(); require(socketCount(dns) == 0);
            release.countDown(); for (Socket client : clients) client.close(); dns.close(); waitEmpty(dns);
        } finally { release.countDown(); for (Socket client : clients) try { client.close(); } catch (IOException ignored) { } }
    }

    private static void apiIntegration() throws Exception {
        Map<String,Object> saved = new LinkedHashMap<>();
        for (String name : List.of("virtualIp", "gateways", "dataDirectory", "process", "outputPump", "readyFile", "stopFile", "ready", "nativeMacSession", "controlRevision", "managedDns", "dnsBridge")) saved.put(name, gatewayField(name));
        require(saved.get("process") == null);
        Path profile = directory("api-профиль"), readyFile = profile.resolve("ready-owned.json");
        ManagedDns dns = owner(profile, host -> publicAnswer()); Main.Manager manager = null; HttpServer server = null; ExecutorService apiPool = Executors.newCachedThreadPool();
        try (NativeEgress nativeSide = new NativeEgress()) {
            gatewayField("virtualIp", "192.168.1.70"); gatewayField("gateways", GATEWAYS); gatewayField("dataDirectory", profile); gatewayField("managedDns", dns);
            manager = new Main.Manager(profile, (url,route) -> { throw new AssertionError("DNS API inspected an external source"); }, path -> Long.MAX_VALUE, (route,torrent) -> { throw new AssertionError("DNS API started a payload"); });
            manager.queueTimer.shutdownNow(); Main.Manager handler = manager;
            server = HttpServer.create(new InetSocketAddress(loopback(), 0), 0); server.setExecutor(apiPool); server.createContext("/", exchange -> Main.handle(exchange, handler, null, () -> { })); server.start();
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newBuilder().proxy(new ProxySelector() { public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); } public void connectFailed(URI uri, SocketAddress address, IOException error) { } }).build();
            require(api(client, base, "GET", "/api/v1/network/dns", null, 200).get("mode").equals("doh"));
            Map<String,Object> providers = api(client, base, "GET", "/api/v1/network/dns/providers", null, 200);
            require(Main.arr(providers.get("providers")).size() == 3 && Main.arr(providers.get("modes")).equals(List.of("system", "doh", "dot")));
            Map<String,Object> sameDefault = api(client, base, "PUT", "/api/v1/network/dns", SecureDns.Config.defaults().toJson(), 200);
            require(Main.num(sameDefault.get("revision"), 0) == 1 && Files.isRegularFile(profile.resolve("dns-settings.json")));
            Map<String,Object> applied = api(client, base, "PUT", "/api/v1/network/dns", system().toJson(), 200);
            require(applied.get("mode").equals("system") && Main.num(applied.get("revision"), 0) == 2);
            byte[] disk = Files.readAllBytes(profile.resolve("dns-settings.json"));
            api(client, base, "PUT", "/api/v1/network/dns", Map.of("mode", "plain"), 400);
            require(Arrays.equals(disk, Files.readAllBytes(profile.resolve("dns-settings.json"))) && dns.snapshot().revision() == 2 && dns.snapshot().config().equals(system()));
            Main.save(profile.resolve("desktop-settings.config"), new LinkedHashMap<>(Map.of("language", "ru", "virtual_ip", "192.168.1.70", "gateways", GATEWAYS)));
            require(Arrays.equals(disk, Files.readAllBytes(profile.resolve("dns-settings.json"))));
            GatewayNetwork.initialize(profile); ManagedDns restored = (ManagedDns)gatewayField("managedDns");
            require(restored.snapshot().config().equals(system()) && restored.snapshot().revision() == 2);
            restored.close(); dns = owner(profile, host -> publicAnswer()); gatewayField("managedDns", dns);
            ManagedDns.Bridge bridge = dns.beginGeneration(); nativeSide.key.set(bridge.key()); gatewayField("dnsBridge", bridge);
            InertProcess process = new InertProcess(); Map<String,Object> report = nativeReport(2, nativeSide.port());
            Main.save(readyFile, report); gatewayField("readyFile", readyFile); gatewayField("ready", report); gatewayField("process", process); gatewayField("controlRevision", 0L);
            try (ControlAck ack = new ControlAck(readyFile, nativeSide.port(), bridge.key())) {
                Map<String,Object> test = api(client, base, "POST", "/api/v1/network/dns/test", Map.of("settings", secureConfig("dot").toJson(), "gateways", GATEWAYS, "hostname", "пример.рф"), 200);
                require(Boolean.TRUE.equals(test.get("ok")) && Main.arr(test.get("results")).size() == 4 && dns.snapshot().config().equals(system()) && dns.snapshot().revision() == 2);
                require(process.alive && process.destroyed == 0 && Arrays.equals(disk, Files.readAllBytes(profile.resolve("dns-settings.json"))));
                Map<String,Object> hot = api(client, base, "PUT", "/api/v1/network/dns", secureConfig("doh").toJson(), 200);
                require(hot.get("mode").equals("doh") && Main.num(hot.get("revision"), 0) == 3 && ack.commands.get() == 1 && process.destroyed == 0);
                String command = Files.readString(readyFile.resolveSibling(readyFile.getFileName() + ".gateways"), StandardCharsets.US_ASCII);
                require(command.startsWith("MR_GATEWAYS_4\n1\n") && command.endsWith("\n3\n"));
                ManagedDns.State before = dns.snapshot(); ack.rejectNext.set(true);
                api(client, base, "PUT", "/api/v1/network/dns", secureConfig("dot").toJson(), 400);
                require(dns.snapshot().config().equals(before.config()) && dns.snapshot().revision() > before.revision() && process.alive && process.destroyed == 0 && ack.commands.get() == 3);
                Map<String,Object> rollbackDisk = new LinkedHashMap<>(Main.obj(Json.parse(Files.readString(profile.resolve("dns-settings.json"), StandardCharsets.UTF_8))));
                require(Main.num(rollbackDisk.remove("revision"), -1) == dns.snapshot().revision() && SecureDns.Config.fromJson(rollbackDisk).equals(dns.snapshot().config()));
                Map<String,Object> missing = new LinkedHashMap<>(Main.obj(Json.parse(Files.readString(readyFile, StandardCharsets.UTF_8)))); missing.remove("managed_dns"); Main.save(readyFile, missing);
                int attempts = nativeSide.routes.size(); api(client, base, "POST", "/api/v1/network/dns/test", Map.of("settings", system().toJson(), "gateways", GATEWAYS), 400);
                require(nativeSide.routes.size() == attempts && process.destroyed == 0);
                for (String field : List.of("dns_revision", "dns_tunnel_port")) {
                    for (boolean numericString : List.of(true, false)) {
                        Map<String,Object> malformed = nativeReport(dns.snapshot().revision(), nativeSide.port());
                        Object exact = malformed.get(field); malformed.put(field, numericString ? exact.toString() : ((Number)exact).doubleValue() + 0.5);
                        Main.save(readyFile, malformed);
                        api(client, base, "POST", "/api/v1/network/dns/test", Map.of("settings", system().toJson(), "gateways", GATEWAYS), 400);
                        require(nativeSide.routes.size() == attempts && process.destroyed == 0);
                    }
                }
                Main.save(readyFile, nativeReport(dns.snapshot().revision(), nativeSide.port()));
                require(!Json.stringify(api(client, base, "GET", "/api/v1/network", null, 200)).contains(bridge.key()));
                ack.assertHealthy();
            }
            nativeSide.assertHealthy();
            gatewayField("process", null); gatewayField("readyFile", null); gatewayField("ready", null);
            Main.Manager reloaded = new Main.Manager(profile, (url,route) -> { throw new AssertionError(); }, path -> Long.MAX_VALUE, (route,torrent) -> { throw new AssertionError(); }); reloaded.queueTimer.shutdownNow(); reloaded.shutdown();
            AppLog.flush();
            try (var paths = Files.walk(root.resolve("журнал-日本語"))) {
                for (Path path : paths.filter(value -> Files.isRegularFile(value) && value.toString().endsWith(".log")).toList()) {
                    String text = Files.readString(path, StandardCharsets.UTF_8); require(!text.lines().anyMatch(line -> line.contains("Invalid manifest") && line.contains("dns-settings.json")));
                }
            }
        } finally {
            if (server != null) server.stop(0); apiPool.shutdownNow(); if (manager != null) manager.shutdown(); dns.close();
            gatewayField("process", null); GatewayNetwork.close();
            for (var entry : saved.entrySet()) gatewayField(entry.getKey(), entry.getValue());
        }
    }

    private static void loggingCooldown() throws Exception {
        try (ManagedDns dns = owner(directory("logging-cooldown"), host -> { throw new UnknownHostException("owned persistent failure"); })) {
            dns.stage(new ManagedDns.State(system(), 2, "")); ManagedDns.Bridge bridge = dns.beginGeneration(); dns.publishEgress(1234, GATEWAYS);
            Field field = ManagedDns.class.getDeclaredField("lastErrorLog"); field.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String,Long> timestamps = (Map<String,Long>)field.get(dns);
            String gateway = GATEWAYS.get(0); long previous = System.nanoTime() - TimeUnit.SECONDS.toNanos(13); timestamps.put(gateway, previous);
            require(query(bridge, bridge.key(), gateway, 2, 1000, DnsMessage.queryA("example.com")).status == 0);
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (Objects.equals(timestamps.get(gateway), previous) && System.nanoTime() < until) Thread.sleep(2);
            long emitted = timestamps.get(gateway); require(emitted > previous);
            for (int count=0; count<3; count++) require(query(bridge, bridge.key(), gateway, 2, 1000, DnsMessage.queryA("example.com")).status == 0);
            waitEmpty(dns); require(timestamps.get(gateway) == emitted);
        }
    }

    private static Map<String,Object> api(HttpClient client, String base, String method, String path, Map<String,Object> body, int expected) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(java.time.Duration.ofSeconds(5));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(Json.stringify(body), StandardCharsets.UTF_8));
        if (body != null) builder.header("Content-Type", "application/json; charset=utf-8");
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != expected) throw new AssertionError(method + " " + path + " => " + response.statusCode() + " " + response.body());
        require(!response.body().contains("\ufffd")); return Main.obj(Json.parse(response.body()));
    }
    private static Object gatewayField(String name) throws Exception { Field field = GatewayNetwork.class.getDeclaredField(name); field.setAccessible(true); return field.get(null); }
    private static void gatewayField(String name, Object value) throws Exception { Field field = GatewayNetwork.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value); }
    private static Map<String,Object> nativeReport(long dnsRevision, int port) {
        Map<String,Object> report = new LinkedHashMap<>(); report.put("ok", true); report.put("virtual_ip", "192.168.1.70"); report.put("configured_gateways", GATEWAYS);
        report.put("computer_ipv4", "192.168.1.90"); report.put("netmask", "255.255.255.0"); report.put("prefix_length", 24L); report.put("network_cidr", "192.168.1.0/24");
        report.put("interface_index", 42L); report.put("network_generation", 1L); report.put("interface", "Owned LAN Сеть 日本語"); report.put("network_validation_ok", true); report.put("network_validation_status", "valid");
        report.put("hot_gateway_updates", true); report.put("managed_dns", true); report.put("dns_revision", dnsRevision); report.put("dns_tunnel_port", port); report.put("control_revision", 0L);
        report.put("gateway_profiles", GATEWAYS.stream().map(gateway -> Map.<String,Object>of("gateway", gateway, "available", true, "port", 1234L, "socks_port", 1235L)).toList()); return report;
    }
    private static final class InertProcess extends Process {
        volatile boolean alive = true; int destroyed;
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); } public InputStream getInputStream() { return InputStream.nullInputStream(); } public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        public int waitFor() { alive = false; return 0; } public boolean waitFor(long timeout, TimeUnit unit) { return !alive; } public int exitValue() { if (alive) throw new IllegalThreadStateException(); return 0; }
        public void destroy() { destroyed++; alive = false; } public boolean isAlive() { return alive; }
    }
    private static final class ControlAck implements AutoCloseable {
        final Path ready; final int port; final String key; final AtomicBoolean rejectNext = new AtomicBoolean(); final AtomicInteger commands = new AtomicInteger(); final AtomicReference<Throwable> error = new AtomicReference<>(); final Thread thread; volatile boolean closed;
        ControlAck(Path ready, int port, String key) { this.ready = ready; this.port = port; this.key = key; thread = new Thread(this::run, "owned-native-control"); thread.setDaemon(true); thread.start(); }
        void run() { long handled = 0; while (!closed) try {
            Path command = ready.resolveSibling(ready.getFileName() + ".gateways"); if (!Files.isRegularFile(command)) { Thread.sleep(5); continue; }
            String[] lines = Files.readString(command, StandardCharsets.US_ASCII).split("\n"); if (lines.length != 6 || !lines[0].equals("MR_GATEWAYS_4")) throw new AssertionError("Invalid V4 command");
            long revision = Long.parseLong(lines[1]); if (revision <= handled) { Thread.sleep(5); continue; }
            if (!lines[2].equals(String.join(",", GATEWAYS)) || !lines[4].equals(key) || Integer.parseInt(lines[3]) < 1) throw new AssertionError("Wrong V4 profile or capability");
            long dnsRevision = Long.parseLong(lines[5]); Map<String,Object> response = nativeReport(dnsRevision + (rejectNext.getAndSet(false) ? 1 : 0), port); response.put("control_revision", revision); Main.save(ready, response); handled = revision; commands.incrementAndGet();
        } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); break; } catch (Throwable failure) { error.compareAndSet(null, failure); break; } }
        void assertHealthy() { require(error.get() == null); }
        public void close() throws InterruptedException { closed = true; thread.interrupt(); thread.join(500); assertHealthy(); }
    }

    private static ManagedDns owner(Path directory, SecureDns.SystemLookup lookup) throws IOException { return new ManagedDns(directory, new SecureDns(trusted, lookup)); }
    private static InetAddress[] publicAnswer() throws UnknownHostException { systemCalls.incrementAndGet(); return new InetAddress[]{InetAddress.getByAddress(new byte[]{9,9,9,9}), InetAddress.getByAddress(new byte[]{10,0,0,1})}; }
    private static SecureDns.Config system() { return SecureDns.Config.preset("cloudflare", "system"); }
    private static SecureDns.Config secureConfig(String mode) { return SecureDns.Config.fromJson(Map.of("mode", mode, "provider", "custom", "doh_url", "https://resolver.invalid:8443/dns-query", "dot_host", "resolver.invalid", "dot_port", 8853, "bootstrap_ips", List.of("9.9.9.9"))); }
    private static Path directory(String name) throws IOException { return Files.createDirectory(root.resolve(name)); }
    private static Path directoryOf(ManagedDns dns) throws Exception { Field f = ManagedDns.class.getDeclaredField("path"); f.setAccessible(true); return ((Path)f.get(dns)).getParent(); }
    private static InetAddress loopback() throws UnknownHostException { return InetAddress.getByAddress(new byte[]{127,0,0,1}); }
    private static Socket brokerSocket(ManagedDns.Bridge bridge) throws IOException { Socket socket = new Socket(); socket.connect(new InetSocketAddress(loopback(), bridge.port()), 1000); socket.setSoTimeout(3000); return socket; }
    private static byte[] packet(String key, String gateway, long revision, int budget, byte[] query) throws IOException { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes); out.write(MAGIC); out.write(key.getBytes(StandardCharsets.US_ASCII)); out.write(Arrays.copyOf(gateway.getBytes(StandardCharsets.US_ASCII), 16)); out.writeLong(revision); out.writeInt(budget); out.writeShort(query.length); out.write(query); return bytes.toByteArray(); }
    private static Reply query(ManagedDns.Bridge bridge, String key, String gateway, long revision, int budget, byte[] query) throws IOException { return exchange(bridge, packet(key, gateway, revision, budget, query)); }
    private static Reply exchange(ManagedDns.Bridge bridge, byte[] request) throws IOException { try (Socket socket = brokerSocket(bridge)) { socket.getOutputStream().write(request); socket.getOutputStream().flush(); return readReply(socket); } }
    private static Reply readReply(Socket socket) throws IOException { DataInputStream in = new DataInputStream(socket.getInputStream()); int status = in.readUnsignedByte(), length = in.readUnsignedShort(); if (status < 0 || status > 1 || (status == 0 && length > 512)) throw new IOException("Invalid owned broker reply"); byte[] body = in.readNBytes(length); if (body.length != length) throw new EOFException(); Reply reply = new Reply(status, body); if (status == 0) reply.text(); return reply; }
    private static void expire(ManagedDns dns, long revision) throws Exception { Field field = ManagedDns.class.getDeclaredField("policies"); field.setAccessible(true); @SuppressWarnings("unchecked") Map<Long,Object> map = (Map<Long,Object>)field.get(dns); Object old = Objects.requireNonNull(map.get(revision)); Method state = old.getClass().getDeclaredMethod("state"); state.setAccessible(true); Constructor<?> constructor = old.getClass().getDeclaredConstructor(ManagedDns.State.class, long.class); constructor.setAccessible(true); map.put(revision, constructor.newInstance(state.invoke(old), System.nanoTime() - 1)); }
    @SuppressWarnings("unchecked") private static Set<Socket> socketRegistry(ManagedDns dns) throws Exception { Field field = ManagedDns.class.getDeclaredField("sockets"); field.setAccessible(true); return (Set<Socket>)field.get(dns); }
    private static int socketCount(ManagedDns dns) throws Exception { return socketRegistry(dns).size(); }
    private static void waitEmpty(ManagedDns dns) throws Exception { long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1); while (socketCount(dns) != 0 && System.nanoTime() < until) Thread.sleep(10); require(socketCount(dns) == 0); }
    private static <T> CompletableFuture<T> async(Callable<T> task) { CompletableFuture<T> future = new CompletableFuture<>(); Thread thread = new Thread(() -> { try { future.complete(task.call()); } catch (Throwable error) { future.completeExceptionally(error); } }, "owned-managed-query"); thread.setDaemon(true); thread.start(); return future; }
    private static long ttl(byte[] wire) { int i = wire.length - 10; return ((long)(wire[i]&255)<<24)|((long)(wire[i+1]&255)<<16)|((long)(wire[i+2]&255)<<8)|(wire[i+3]&255); }
    private static void check(String name, Checked action) throws Exception { action.run(); cases.add(name); }
    private static void require(boolean condition) { checks++; if (!condition) throw new AssertionError("Managed DNS assertion " + checks); }
    private static void expectIo(Checked action) throws Exception { try { action.run(); } catch (IOException expected) { require(true); return; } throw new AssertionError("Expected IOException"); }
    private static SSLContext context(Path path, boolean server) throws Exception { KeyStore store = KeyStore.getInstance("PKCS12"); try (InputStream in = Files.newInputStream(path)) { store.load(in, "owned-fixture-only".toCharArray()); } SSLContext result = SSLContext.getInstance("TLS"); if (server) { KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); factory.init(store, "owned-fixture-only".toCharArray()); result.init(factory.getKeyManagers(), null, null); } else { TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); factory.init(store); result.init(null, factory.getTrustManagers(), null); } return result; }

    private static final class NativeEgress implements AutoCloseable {
        final ServerSocket listener; final ExecutorService pool = Executors.newCachedThreadPool(task -> { Thread t = new Thread(task, "owned-native-dns"); t.setDaemon(true); return t; });
        final AtomicReference<String> key = new AtomicReference<>(""); final AtomicBoolean reject = new AtomicBoolean();
        final List<String> routes = new CopyOnWriteArrayList<>(), sni = new CopyOnWriteArrayList<>(); final List<Throwable> failures = new CopyOnWriteArrayList<>(); final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
        volatile boolean closed; volatile CountDownLatch entered = new CountDownLatch(0), release = new CountDownLatch(0);
        NativeEgress() throws IOException { listener = new ServerSocket(0, 16, loopback()); pool.execute(() -> { while (!closed) try { Socket socket = listener.accept(); sockets.add(socket); pool.execute(() -> serve(socket)); } catch (IOException stopped) { if (!closed) failures.add(stopped); } }); }
        int port() { return listener.getLocalPort(); } void hold() { entered = new CountDownLatch(1); release = new CountDownLatch(1); }
        void serve(Socket socket) {
            try (socket) {
                socket.setSoTimeout(3000); DataInputStream in = new DataInputStream(socket.getInputStream()); byte[] magic = in.readNBytes(6), supplied = in.readNBytes(64), gateway = in.readNBytes(16), address = in.readNBytes(4); int port = in.readUnsignedShort();
                if (!Arrays.equals(magic, "MRDNT1".getBytes(StandardCharsets.US_ASCII)) || !Arrays.equals(supplied, key.get().getBytes(StandardCharsets.US_ASCII))) throw new AssertionError("Wrong native tunnel capability");
                String route = new String(gateway, StandardCharsets.US_ASCII).replace("\0", ""); if (!GATEWAYS.contains(route) || !Arrays.equals(address, new byte[]{9,9,9,9}) || (port != 8443 && port != 8853)) throw new AssertionError("Wrong numeric gateway target");
                routes.add(route + "|9.9.9.9|" + port); entered.countDown(); if (!release.await(2, TimeUnit.SECONDS)) throw new IOException("Owned egress latch timed out");
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                if (reject.get()) { byte[] message = "Связь — 接続失敗".getBytes(StandardCharsets.UTF_8); out.writeByte(0); out.writeShort(message.length); out.write(message); out.flush(); return; }
                out.writeByte(1); out.flush();
                try (SSLSocket tls = (SSLSocket)serverContext.getSocketFactory().createSocket(socket, "resolver.invalid", port, false)) {
                    tls.setUseClientMode(false); tls.setSoTimeout(3000); tls.startHandshake();
                    sni.add(((SNIHostName)((ExtendedSSLSession)tls.getSession()).getRequestedServerNames().get(0)).getAsciiName());
                    byte[] query = port == 8443 ? httpQuery(tls.getInputStream()) : dotQuery(tls.getInputStream());
                    byte[] answer = DnsMessage.systemAnswer(query, publicAnswer()); answer[answer.length - 7] = 60;
                    DataOutputStream response = new DataOutputStream(tls.getOutputStream());
                    if (port == 8443) response.write(("HTTP/1.1 200 OK\r\nContent-Type: application/dns-message\r\nContent-Length: " + answer.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII)); else response.writeShort(answer.length);
                    response.write(answer); response.flush();
                }
            } catch (IOException expectedClosure) { } catch (Throwable failure) { failures.add(failure); } finally { sockets.remove(socket); }
        }
        void assertHealthy() { require(failures.isEmpty()); }
        @Override public void close() throws IOException { closed = true; release.countDown(); listener.close(); for (Socket socket : sockets) socket.close(); pool.shutdownNow(); }
    }
    private static byte[] dotQuery(InputStream stream) throws IOException { DataInputStream in = new DataInputStream(stream); int size = in.readUnsignedShort(); if (size < 12 || size > 4096) throw new IOException("Own DoT length"); byte[] query = in.readNBytes(size); if (query.length != size) throw new EOFException(); return query; }
    private static byte[] httpQuery(InputStream stream) throws IOException { ByteArrayOutputStream header = new ByteArrayOutputStream(); int state = 0; while (state < 4 && header.size() < 16384) { int value = stream.read(); if (value < 0) throw new EOFException(); header.write(value); state = value == (state == 0 || state == 2 ? '\r' : '\n') ? state + 1 : value == '\r' ? 1 : 0; } String text = header.toString(StandardCharsets.US_ASCII); if (!text.startsWith("POST /dns-query HTTP/1.1")) throw new IOException("Own DoH method"); int size = -1; for (String line : text.split("\r\n")) if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) size = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim()); if (size < 12 || size > 4096) throw new IOException("Own DoH length"); byte[] query = stream.readNBytes(size); if (query.length != size) throw new EOFException(); return query; }
}
