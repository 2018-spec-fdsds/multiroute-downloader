package multiroute;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Real desktop bindings with synthetic subnet observations. Never changes or probes host networking. */
public final class NetworkTopologyDesktopTest42 {
    static int checks;
    interface Work { void run() throws Exception; }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() instanceof Exception error) throw error;
        if (failure.get() instanceof Error error) throw error;
    }
    static Map<String,Object> settings(String host, String status) {
        Map<String,Object> validation = new LinkedHashMap<>(); validation.put("status", status);
        validation.put("computer_ipv4", host); validation.put("network_cidr", "192.168.1.0/24");
        validation.put("message", "Gateway component is not running");
        return Map.of("virtual_ip", "192.168.1.70", "gateways", GatewayNetwork.DEFAULT_GATEWAYS,
                "network_validation", validation);
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console(); Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        AtomicInteger networkCalls = new AtomicInteger(); CountDownLatch closed = new CountDownLatch(1);
        Main.Manager manager = new Main.Manager(own.resolve("profile"), (url, route) -> { networkCalls.incrementAndGet(); throw new AssertionError("No source requests"); });
        AtomicReference<DesktopUI> reference = new AtomicReference<>();
        AtomicReference<Map<String,Object>> observed = new AtomicReference<>(settings("192.168.1.90", "valid"));
        AppLog.initialize(own.resolve("logs-profile"));
        try {
            edt(() -> {
                I18n.setLanguage("en"); DesktopTheme.install("light");
                DesktopUI ui = new DesktopUI(manager, closed::countDown, false,
                        (gateway, url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No gateway probes"); },
                        (ip, gateways) -> { networkCalls.incrementAndGet(); throw new AssertionError("No configuration"); },
                        (route, url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No speed tests"); }, observed::get);
                java.lang.reflect.Field timer = DesktopUI.class.getDeclaredField("refreshTimer"); timer.setAccessible(true);
                ((javax.swing.Timer)timer.get(ui)).stop(); ui.frame.addNotify(); reference.set(ui);
                ui.refreshNetworkTopology(observed.get());
                check(ui.networkTopologyStatusLabel.getText().contains("192.168.1.90") && ui.networkTopologyStatusLabel.getText().contains("192.168.1.70"), "Verified topology evidence missing from real label");
                ui.virtualIpField.setText("192.168.1.71"); ui.refreshNetworkTopology(observed.get());
                check(ui.networkTopologyStatusLabel.getText().contains("settings changed") && !ui.networkTopologyStatusLabel.getText().contains("LAN verified"), "Draft IP inherits stale proof");
                ui.virtualIpField.setText("192.168.1.70"); observed.set(settings("192.168.1.91", "valid"));
                ui.refreshNetworkTopology(observed.get());
                check(ui.networkTopologyStatusLabel.getText().contains("192.168.1.91") && !ui.networkTopologyStatusLabel.getText().contains("192.168.1.90"), "Unchanged settings hide changed computer IP");
            });
            DesktopUI ui = reference.get(); Map<String,String> rendered = new LinkedHashMap<>();
            for (String language : I18n.codes()) edt(() -> {
                I18n.setLanguage(language); I18n.refresh(ui.frame);
                ui.refreshNetworkTopology(settings("192.168.1.91", "valid")); String text = ui.networkTopologyStatusLabel.getText();
                check(text.contains("192.168.1.91") && text.contains("192.168.1.0/24") && text.contains("192.168.1.70") && !text.contains("%s") && !text.contains("\ufffd"), "Topology text corrupted in " + language);
                if (!language.equals("en")) check(!text.startsWith("LAN verified:"), "Topology label not translated in " + language);
                String tip = ui.networkTopologyStatusLabel.getToolTipText();
                check(tip != null && !tip.isBlank() && !tip.contains("\ufffd"), "Missing topology tooltip " + language);
                if (!language.equals("en")) check(!tip.contains("All gateways and the application's"), "Topology tooltip not translated in " + language);
                rendered.put(language, text + "\n" + tip);
                if (List.of("en", "ru").contains(language)) {
                    ui.networkTopologyStatusLabel.setSize(650, 115);
                    BufferedImage image = new BufferedImage(650, 115, BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D graphics = image.createGraphics(); graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, 650, 115);
                    ui.networkTopologyStatusLabel.printAll(graphics); graphics.dispose(); ImageIO.write(image, "png", own.resolve("topology-" + language + ".png").toFile());
                }
                ui.refreshNetworkTopology(settings("192.168.1.91", "error"));
                check(!ui.networkTopologyStatusLabel.getText().contains("192.168.1.91"), "Failure still shows a successful subnet proof");
                ui.refreshNetworkTopology(settings("", "pending"));
                check(!ui.networkTopologyStatusLabel.getText().contains("192.168.1.70"), "Unchecked topology still shows a successful proof");
            });
            check(networkCalls.get() == 0, "Topology UI made a real network call");
            Files.writeString(own.resolve("topology-languages.json"), Json.stringify(rendered), StandardCharsets.UTF_8);
            check(Json.stringify(rendered).equals(Files.readString(own.resolve("topology-languages.json"), StandardCharsets.UTF_8)), "Unicode UI text did not roundtrip to UTF-8 output");
            Map<String,Object> report = Map.of("status", "PASS", "checks", checks, "languages", I18n.codes(), "real_network_calls", 0, "os_network_changed", false);
            Files.writeString(own.resolve("NETWORK-TOPOLOGY-DESKTOP-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
            System.out.println(Json.stringify(report));
        } finally {
            if (reference.get() != null) { edt(() -> reference.get().frame.dispose()); check(closed.await(8, TimeUnit.SECONDS), "Owned desktop did not close"); }
            manager.shutdown(); AppLog.close();
        }
    }
}
