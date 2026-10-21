package multiroute;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Synthetic native readiness snapshots and an inert Process; never launches a helper or changes the OS. */
public final class NetworkTopologyBridgeTest42 {
    private static int checks;
    private static final List<String> cases = new ArrayList<>();
    private static final List<String> GATEWAYS = List.of("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4");

    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); cases.add(message); }
    private static void field(String name, Object value) throws Exception { Field field = GatewayNetwork.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value); }
    private static Object field(String name) throws Exception { Field field = GatewayNetwork.class.getDeclaredField(name); field.setAccessible(true); return field.get(null); }
    private static Map<String,Object> topology(Map<String,Object> settings) { return Main.obj(settings.get("network_validation")); }
    private static void write(Path file, Map<String,Object> report) throws Exception { Files.writeString(file, Json.stringify(report), StandardCharsets.UTF_8); }
    private static String state(Map<String,Object> report, boolean alive, String vip, List<String> gateways) { return Main.str(GatewayNetwork.validationSnapshot(report, alive, vip, gateways).get("status")); }

    private static final class InertProcess extends Process {
        boolean alive = true;
        int destroyed, waited;
        final InputStream input = InputStream.nullInputStream();
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        public InputStream getInputStream() { return input; }
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        public int waitFor() { waited++; alive = false; return 0; }
        public boolean waitFor(long timeout, TimeUnit unit) { waited++; return !alive; }
        public int exitValue() { if (alive) throw new IllegalThreadStateException(); return 0; }
        public void destroy() { destroyed++; alive = false; }
        public boolean isAlive() { return alive; }
    }

    private static Map<String,Object> report(String computer, String vip, String mask, long prefix, String cidr, List<String> gateways) {
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("ok", true); report.put("virtual_ip", vip); report.put("configured_gateways", gateways);
        report.put("computer_ipv4", computer); report.put("netmask", mask); report.put("prefix_length", prefix); report.put("network_cidr", cidr);
        report.put("interface_index", 42L); report.put("network_generation", 1L); report.put("interface", "Owned LAN Сеть 中文");
        report.put("network_validation_ok", true); report.put("network_validation_status", "valid");
        List<Object> profiles = new ArrayList<>(); int port = 18765;
        for (String gateway : gateways) profiles.add(Map.of("gateway", gateway, "available", true, "port", (long)port++, "socks_port", 19765L,
                "gateway_mac", "02:00:00:00:00:01"));
        report.put("gateway_profiles", profiles); return report;
    }

    private static void pureEvidence() throws Exception {
        GatewayNetwork.validateSettings("192.168.1.0", List.of("192.168.0.1", "192.168.1.1"));
        GatewayNetwork.validateSettings("10.20.2.255", List.of("10.20.0.1"));
        check(true, "syntax accepts .0 and .255 so the real prefix decides host validity");
        Map<String,Object> slash23 = report("192.168.0.254", "192.168.1.0", "255.255.254.0", 23, "192.168.0.0/23", List.of("192.168.0.1", "192.168.1.1"));
        check(state(slash23, true, "192.168.1.0", List.of("192.168.0.1", "192.168.1.1")).equals("valid"), "/23 allows a .0 host inside the actual network");
        Map<String,Object> slash16 = report("10.20.1.12", "10.20.2.255", "255.255.0.0", 16, "10.20.0.0/16", List.of("10.20.0.1", "10.20.9.1"));
        check(state(slash16, true, "10.20.2.255", List.of("10.20.0.1", "10.20.9.1")).equals("valid"), "/16 allows a .255 host inside the actual network");
        Map<String,Object> baseline = report("192.168.1.90", "192.168.1.70", "255.255.255.0", 24, "192.168.1.0/24", GATEWAYS);
        check(state(baseline, true, "192.168.1.70", GATEWAYS).equals("valid"), "matching native /24 evidence is valid");
        for (String field : List.of("network_validation_ok", "network_validation_status", "computer_ipv4", "netmask", "prefix_length", "network_cidr", "interface_index", "network_generation", "configured_gateways")) {
            Map<String,Object> missing = new LinkedHashMap<>(baseline); missing.remove(field);
            check(!state(missing, true, "192.168.1.70", GATEWAYS).equals("valid"), "missing authoritative field never validates: " + field);
        }
        Map<String,Object> wrongMask = new LinkedHashMap<>(baseline); wrongMask.put("netmask", "255.255.0.0");
        check(!state(wrongMask, true, "192.168.1.70", GATEWAYS).equals("valid"), "mask must agree with native prefix");
        Map<String,Object> wrongCidr = new LinkedHashMap<>(baseline); wrongCidr.put("network_cidr", "192.168.2.0/24");
        check(!state(wrongCidr, true, "192.168.1.70", GATEWAYS).equals("valid"), "CIDR must agree with computer IP and mask");
        Map<String,Object> fractional = new LinkedHashMap<>(baseline); fractional.put("interface_index", 42.5);
        check(!state(fractional, true, "192.168.1.70", GATEWAYS).equals("valid"), "fractional adapter identity is not evidence");
        check(!state(baseline, true, "192.168.1.71", GATEWAYS).equals("valid"), "proof for another virtual IP cannot validate current settings");
        check(!state(baseline, true, "192.168.1.70", List.of("192.168.1.1")).equals("valid"), "proof must name the exact configured gateway set");

        for (String vip : List.of("192.168.0.0", "192.168.1.255")) {
            Map<String,Object> invalid = new LinkedHashMap<>(slash23); invalid.put("virtual_ip", vip);
            check(!state(invalid, true, vip, List.of("192.168.0.1", "192.168.1.1")).equals("valid"), "actual /23 network and broadcast are rejected: " + vip);
        }
        for (String computer : List.of("192.168.2.90", "192.168.1.70", "192.168.1.1")) {
            Map<String,Object> invalid = new LinkedHashMap<>(baseline); invalid.put("computer_ipv4", computer);
            if (computer.contains(".2.")) invalid.put("network_cidr", "192.168.2.0/24");
            check(!state(invalid, true, "192.168.1.70", GATEWAYS).equals("valid"), "foreign subnet or local-address collision is rejected: " + computer);
        }
        Map<String,Object> falseFlag = new LinkedHashMap<>(baseline); falseFlag.put("network_validation_ok", false); falseFlag.put("network_validation_status", "recheck_required");
        check(state(falseFlag, true, "192.168.1.70", GATEWAYS).equals("error"), "native invalidation flag overrides otherwise plausible addresses");
        check(state(baseline, false, "192.168.1.70", GATEWAYS).equals("error"), "dead helper cannot advertise valid topology");
        check(state(null, false, "192.168.1.70", GATEWAYS).equals("pending"), "unstarted topology remains pending");
    }

    private static void actualBridge(Path own) throws Exception {
        Map<String,Object> saved = new LinkedHashMap<>();
        for (String name : List.of("virtualIp", "gateways", "dataDirectory", "process", "outputPump", "readyFile", "stopFile", "ready", "nativeMacSession")) saved.put(name, field(name));
        check(saved.get("process") == null, "owned JVM starts without a native process");
        Path file = own.resolve("ready-中文.json"); InertProcess process = new InertProcess();
        Map<String,Object> baseline = report("192.168.1.90", "192.168.1.70", "255.255.255.0", 24, "192.168.1.0/24", GATEWAYS);
        try {
            field("virtualIp", "192.168.1.70"); field("gateways", GATEWAYS); field("dataDirectory", own);
            field("process", process); field("readyFile", file); field("ready", baseline); field("stopFile", null); field("outputPump", null); field("nativeMacSession", false);
            check(topology(GatewayNetwork.settings()).get("status").equals("pending"), "missing fresh readiness does not reuse cached success");
            write(file, baseline); byte[] before = Files.readAllBytes(file);
            Map<String,Object> settings = GatewayNetwork.settings();
            check(topology(settings).get("status").equals("valid") && topology(settings).get("computer_ipv4").equals("192.168.1.90"), "settings exposes current authoritative computer IP and topology");
            check(Arrays.equals(before, Files.readAllBytes(file)) && process.destroyed == 0 && process.waited == 0, "settings GET leaves helper and readiness file unchanged");
            Map<String,Object> address = GatewayNetwork.checkAddress();
            check(Boolean.TRUE.equals(address.get("address_available")) && topology(address).get("status").equals("valid"), "address availability requires fresh validated readiness");
            Map<String,Object> first = GatewayNetwork.route(GATEWAYS.get(0));
            check(first.get("computer_ipv4").equals("192.168.1.90") && !first.containsKey("gateway_mac"), "route includes topology evidence and strips hardware fields");
            URI endpoint = GatewayNetwork.endpoint(first);

            Map<String,Object> changed = new LinkedHashMap<>(baseline); changed.put("computer_ipv4", "192.168.1.91"); changed.put("network_generation", 2L);
            write(file, changed);
            Map<String,Object> current = GatewayNetwork.settings();
            check(topology(current).get("computer_ipv4").equals("192.168.1.91") && Main.num(topology(current).get("network_generation"), 0) == 2, "compatible DHCP address change appears without changing settings");
            Map<String,Object> next = GatewayNetwork.route(GATEWAYS.get(0));
            check(endpoint.equals(GatewayNetwork.endpoint(next)) && next.get("virtual_ip").equals("192.168.1.70") && process.destroyed == 0, "same-subnet PC address change preserves virtual IP and endpoints");
            check(GatewayNetwork.socksEndpoint(next).getPort() == 19765, "torrent gateway retains its listener after compatible PC address change");

            Files.writeString(file, "{", StandardCharsets.UTF_8);
            check(topology(GatewayNetwork.settings()).get("status").equals("pending"), "transient partial JSON cannot advertise cached validity");
            try { GatewayNetwork.checkAddress(); throw new AssertionError("Partial proof accepted"); }
            catch (IOException expected) { check(expected.getMessage().contains("subnet validation"), "address check blocks unverifiable readiness"); }
            check(process.destroyed == 0 && process.alive, "transient read failure does not stop an otherwise live helper");

            write(file, changed); process.alive = false;
            check(topology(GatewayNetwork.settings()).get("status").equals("error"), "dead helper with last successful report is never valid");
            Map<String,Object> invalid = new LinkedHashMap<>(changed); invalid.put("ok", false); invalid.put("network_validation_ok", false); invalid.put("network_validation_status", "recheck_required");
            invalid.put("error", "Owned LAN subnet changed; recheck required"); write(file, invalid);
            check(topology(GatewayNetwork.settings()).get("error").equals(invalid.get("error")), "final native failure reason survives helper exit");
            try { GatewayNetwork.route(GATEWAYS.get(0)); throw new AssertionError("Invalid route accepted"); }
            catch (IOException expected) { check(expected.getMessage().contains("Owned LAN subnet changed"), "route reports native cause even after helper exit"); }
            check(field("process") == null && topology(GatewayNetwork.settings()).get("status").equals("error"), "failure snapshot survives error cleanup until explicit reset");
            GatewayNetwork.close();
            check(topology(GatewayNetwork.settings()).get("status").equals("pending"), "explicit close clears stale proof and failure state");
        } finally {
            for (Map.Entry<String,Object> entry : saved.entrySet()) field(entry.getKey(), entry.getValue());
        }
    }

    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        Throwable failure = null;
        try { pureEvidence(); actualBridge(own); } catch (Throwable error) { failure = error; }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", failure == null ? "PASS" : "FAIL"); report.put("checks", checks); report.put("cases", cases);
        report.put("version", Main.VERSION); report.put("native_processes_started", 0); report.put("network_calls", 0); report.put("os_routes_changed", false); report.put("user_data_read", false);
        if (failure != null) report.put("failure", failure.toString());
        Files.writeString(own.resolve("NETWORK-TOPOLOGY-BRIDGE-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
        System.out.println(Json.stringify(report)); if (failure != null) { failure.printStackTrace(); System.exit(1); }
    }
}
