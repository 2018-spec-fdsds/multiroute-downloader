package multiroute;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/** Finds LAN workers; route validation is still performed by the HTTP /probe request. */
public final class WorkerDiscovery {
    public static final int UDP_PORT = 38724;
    private static final String PROTOCOL = "multiroute-discovery-v1";
    private static final String APP_ID = "multiroute-resume";
    private static final int MAX_PACKET = 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WorkerDiscovery() { }

    public static void startResponder(String bind, int workerPort, String gateway) {
        if (gateway == null || gateway.isBlank()) return;
        try {
            launchResponder(bind, workerPort, gateway, UDP_PORT);
        } catch (IOException | IllegalArgumentException error) {
            // Discovery is optional. A blocked UDP port must not disable the HTTP worker.
            AppLog.error("Worker discovery unavailable: %s", DiagnosticText.arg(error.getMessage()));
        }
    }

    /** Test seam; the returned handle closes only this responder. */
    static AutoCloseable launchResponder(String bind, int workerPort, String gateway, int udpPort)
            throws IOException {
        if (!ipLiteral(gateway)) throw new IllegalArgumentException("Discovery gateway must be an IP address");
        if (workerPort < 1 || workerPort > 65535 || udpPort < 1 || udpPort > 65535)
            throw new IllegalArgumentException("Invalid discovery or worker port");
        InetAddress listen = loopbackBind(bind) ? InetAddress.getByName("127.0.0.1") :
                InetAddress.getByName("0.0.0.0");
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(listen, udpPort));
        Thread responder = new Thread(() -> respond(socket, gateway, workerPort), "worker-discovery");
        responder.setDaemon(true);
        responder.start();
        return socket::close;
    }

    private static boolean loopbackBind(String bind) {
        return "127.0.0.1".equals(bind) || "localhost".equalsIgnoreCase(bind);
    }

    private static void respond(DatagramSocket socket, String gateway, int workerPort) {
        byte[] buffer = new byte[MAX_PACKET + 1];
        while (!socket.isClosed()) {
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(incoming);
                if (!(incoming.getAddress() instanceof Inet4Address) || incoming.getLength() > MAX_PACKET)
                    continue;
                Map<?, ?> request = object(incoming);
                if (request == null || !PROTOCOL.equals(request.get("protocol")) ||
                        !"discover".equals(request.get("kind")) || !nonce(request.get("nonce"))) continue;
                byte[] answer = Json.stringify(Map.of("protocol", PROTOCOL, "kind", "worker",
                        "app_id", APP_ID, "nonce", request.get("nonce"), "gateway", gateway,
                        "port", workerPort)).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(answer, answer.length,
                        incoming.getAddress(), incoming.getPort()));
            } catch (SocketException error) {
                if (!socket.isClosed()) AppLog.error("Worker discovery socket error: %s", DiagnosticText.arg(error.getMessage()));
                return;
            } catch (IOException error) {
                if (!socket.isClosed()) AppLog.error("Worker discovery response error: %s", DiagnosticText.arg(error.getMessage()));
            } catch (IllegalArgumentException ignored) {
                // Malformed or unrelated LAN packets are not a worker response.
            }
        }
    }

    public static Map<String, String> find(int timeoutMs) throws IOException {
        return find(timeoutMs, UDP_PORT);
    }

    /** Test seam: the same protocol on a private UDP port. */
    static Map<String, String> find(int timeoutMs, int udpPort) throws IOException {
        if (udpPort < 1 || udpPort > 65535) throw new IllegalArgumentException("Invalid discovery port");
        int waitMs = Math.min(2000, Math.max(1, timeoutMs));
        byte[] random = new byte[16];
        RANDOM.nextBytes(random);
        StringBuilder nonce = new StringBuilder(32);
        for (byte value : random) nonce.append(String.format("%02x", value & 0xff));
        byte[] question = Json.stringify(Map.of("protocol", PROTOCOL, "kind", "discover",
                "nonce", nonce.toString())).getBytes(StandardCharsets.UTF_8);
        Map<String, String> found = new TreeMap<>();
        try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(
                InetAddress.getByName("0.0.0.0"), 0))) {
            socket.setBroadcast(true);
            int sent = 0;
            for (InetAddress destination : destinations()) {
                try {
                    socket.send(new DatagramPacket(question, question.length, destination, udpPort));
                    sent++;
                } catch (IOException ignored) {
                    // Some interfaces disallow broadcast; the others can still work.
                }
            }
            if (sent == 0) throw new IOException("Could not send LAN discovery request");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMs);
            byte[] buffer = new byte[MAX_PACKET + 1];
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                socket.setSoTimeout((int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                try { socket.receive(reply); }
                catch (java.net.SocketTimeoutException ignored) { break; }
                if (reply.getLength() > MAX_PACKET ||
                        !(reply.getAddress() instanceof Inet4Address source) ||
                        source.isAnyLocalAddress() || source.isMulticastAddress()) continue;
                Map<?, ?> answer = object(reply);
                if (answer == null || !PROTOCOL.equals(answer.get("protocol")) ||
                        !"worker".equals(answer.get("kind")) ||
                        !APP_ID.equals(answer.get("app_id")) ||
                        !nonce.toString().equals(answer.get("nonce"))) continue;
                Object gateway = answer.get("gateway");
                Object portValue = answer.get("port");
                if (!(gateway instanceof String gatewayIp) || !ipLiteral(gatewayIp) ||
                        !(portValue instanceof Number number)) continue;
                int port;
                try { port = Integer.parseInt(number.toString()); }
                catch (NumberFormatException ignored) { continue; }
                if (port < 1 || port > 65535) continue;
                String endpoint = "http://" + source.getHostAddress() + ":" + port;
                String previous = found.putIfAbsent(gatewayIp, endpoint);
                if (previous != null && !previous.equals(endpoint)) {
                    if (endpoint.compareTo(previous) < 0) found.put(gatewayIp, endpoint);
                    AppLog.error("Multiple workers advertise gateway %s; using %s after HTTP validation", gatewayIp, found.get(gatewayIp));
                }
            }
        }
        return new LinkedHashMap<>(found);
    }

    private static List<InetAddress> destinations() throws SocketException, IOException {
        TreeSet<String> addresses = new TreeSet<>();
        addresses.add("255.255.255.255");
        addresses.add("127.0.0.1");
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            try {
                if (!network.isUp() || network.isLoopback()) continue;
                for (InterfaceAddress address : network.getInterfaceAddresses()) {
                    InetAddress broadcast = address.getBroadcast();
                    if (address.getAddress() instanceof Inet4Address && broadcast instanceof Inet4Address)
                        addresses.add(broadcast.getHostAddress());
                }
            } catch (SocketException ignored) { }
        }
        List<InetAddress> result = new ArrayList<>();
        for (String address : addresses) result.add(InetAddress.getByName(address));
        return result;
    }

    private static Map<?, ?> object(DatagramPacket packet) {
        try {
            Object parsed = Json.parse(new String(packet.getData(), packet.getOffset(),
                    packet.getLength(), StandardCharsets.UTF_8));
            return parsed instanceof Map<?, ?> map ? map : null;
        } catch (IllegalArgumentException error) { return null; }
    }

    private static boolean nonce(Object value) {
        return value instanceof String text && text.matches("[0-9a-f]{32}");
    }

    private static boolean ipLiteral(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.indexOf(':') >= 0) {
            if (!text.matches("[0-9a-fA-F:.]+")) return false;
            try { return InetAddress.getByName(text) instanceof java.net.Inet6Address; }
            catch (Exception ignored) { return false; }
        }
        String[] octets = text.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String part : octets) {
            if (!part.matches("0|[1-9][0-9]{0,2}")) return false;
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }
}
