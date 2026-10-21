package multiroute;

import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.swig.settings_pack;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** No session or sockets: verify the native proxy transport cannot inherit a VPN's default interface. */
public final class TorrentProxyBindingTest411 {
    private static final List<String> checks = new ArrayList<>();
    private static void require(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
        checks.add(reason);
    }
    private static void valid(String literal, boolean discovery) throws Exception {
        InetSocketAddress endpoint = new InetSocketAddress(InetAddress.getByName(literal), 41739);
        SettingsPack settings = TorrentEngine.settings(Map.of("type", "gateway", "gateway", "192.168.1.1"), route -> endpoint, discovery);
        String address = endpoint.getAddress().getHostAddress();
        require(settings.getString(settings_pack.string_types.outgoing_interfaces.swigValue()).equals(address), "Native outgoing socket explicitly binds its numeric local proxy address: " + address + " discovery=" + discovery);
        require(settings.getString(settings_pack.string_types.proxy_hostname.swigValue()).equals(address), "Proxy destination remains the resolved loopback address");
        require(settings.getInteger(settings_pack.int_types.proxy_port.swigValue()) == endpoint.getPort(), "Proxy port is retained");
        require(settings.getInteger(settings_pack.int_types.proxy_type.swigValue()) == settings_pack.proxy_type_t.socks5.swigValue(), "Native payload retains SOCKS5 transport");
        require(settings.getBoolean(settings_pack.bool_types.proxy_peer_connections.swigValue()) && settings.getBoolean(settings_pack.bool_types.proxy_tracker_connections.swigValue()) && settings.getBoolean(settings_pack.bool_types.proxy_hostnames.swigValue()), "Peers trackers and hostname resolution remain proxied");
        require(settings.getBoolean(settings_pack.bool_types.enable_outgoing_tcp.swigValue()) == discovery && settings.getBoolean(settings_pack.bool_types.enable_dht.swigValue()) == discovery, "Existing discovery controls are preserved");
        require(!settings.getBoolean(settings_pack.bool_types.enable_incoming_tcp.swigValue()) && !settings.getBoolean(settings_pack.bool_types.enable_incoming_utp.swigValue()) && !settings.getBoolean(settings_pack.bool_types.enable_outgoing_utp.swigValue()), "No incoming or direct uTP transport is enabled");
        require(!settings.getBoolean(settings_pack.bool_types.enable_upnp.swigValue()) && !settings.getBoolean(settings_pack.bool_types.enable_natpmp.swigValue()) && !settings.getBoolean(settings_pack.bool_types.enable_lsd.swigValue()), "No router mapping or local discovery is enabled");
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        for (String literal : List.of("127.0.0.1", "127.0.0.2", "::1")) {
            valid(literal, true);
            valid(literal, false);
        }
        List<InetSocketAddress> rejected = new ArrayList<>();
        rejected.add(null);
        rejected.add(InetSocketAddress.createUnresolved("owned-no-lookup.invalid", 41739));
        rejected.add(new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte)192, 0, 2, 1}), 41739));
        for (InetSocketAddress endpoint : rejected) {
            boolean failed = false;
            try { TorrentEngine.settings(Map.of(), route -> endpoint, true); }
            catch (IOException expected) { failed = true; }
            require(failed, "Nonlocal unresolved or absent proxy is rejected before settings can enable network transport");
        }
        Path root = Path.of(args[0]); Files.createDirectories(root);
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("status", "PASS"); report.put("version", Main.VERSION); report.put("checks", checks.size()); report.put("cases", checks);
        report.put("session_started", false); report.put("sockets_opened", 0); report.put("real_gateway_calls", 0); report.put("user_files_read", false);
        Files.writeString(root.resolve("TORRENT-PROXY-BINDING-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
        System.out.println(Json.stringify(report));
    }
}
