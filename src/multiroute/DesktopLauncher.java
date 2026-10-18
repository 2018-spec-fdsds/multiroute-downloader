package multiroute;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** Native EXE/.app entry point. The JVM stays under the logged-in user's account. */
public final class DesktopLauncher {
    private DesktopLauncher() { }

    static final String SETUP_TITLE = "Application setup";
    static final String SETUP_STATUS = "Checking network support. Approve the official Npcap installer if requested.";

    private static final class SetupFailure extends IOException {
        SetupFailure(String message) { super(message); }
    }

    static String setupErrorText(Exception failure) {
        return failure instanceof SetupFailure ? failure.getMessage()
                : "Unable to prepare MultiRoute Resume. Reinstall the complete application and try again.";
    }

    /** Optional convenience link, under the logged-in user's existing Desktop. */
    static void createDesktopShortcutIfAbsent(Path desktop, Path application) {
        Path link = desktop.resolve("MultiRoute Resume.app");
        try {
            if (!Files.isDirectory(desktop) || Files.isSymbolicLink(desktop) || !Files.isDirectory(application)
                    || Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
            Files.createSymbolicLink(link, application.toAbsolutePath().normalize());
        } catch (IOException | SecurityException | UnsupportedOperationException ignored) {
            // Desktop privacy denial never blocks starting the installed application.
        }
    }

    static boolean nativeDesktop() { return Boolean.getBoolean("multiroute.native.desktop"); }

    static Path userRoot() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        Path home = Path.of(System.getProperty("user.home"));
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return (local == null || local.isBlank() ? home.resolve("AppData/Local") : Path.of(local))
                    .resolve("MultiRoute Resume").toAbsolutePath().normalize();
        }
        if (os.contains("mac")) return home.resolve("Library/Application Support/MultiRoute Resume");
        String data = System.getenv("XDG_DATA_HOME");
        return (data == null || data.isBlank() ? home.resolve(".local/share") : Path.of(data)).resolve("MultiRouteResume");
    }

    static Path applicationRoot() throws Exception {
        Path jar = Path.of(DesktopLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(jar)) throw new SetupFailure("The installed application JAR is missing.");
        return jar.getParent();
    }

    public static void main(String[] args) {
        Main.configureUtf8Console();
        System.setProperty("multiroute.native.desktop", "true");
        JFrame[] progress = new JFrame[1];
        boolean gui = !GraphicsEnvironment.isHeadless() && !Arrays.asList(args).contains("--headless")
                && !Arrays.asList(args).contains("--no-browser");
        try {
            // Installer diagnostics do not open a controller, a capture device or user settings.
            if (args.length == 2 && args[0].equals("--launcher-check")) {
                var info = new LinkedHashMap<String,Object>();
                info.put("ok", true); info.put("version", Main.VERSION);
                info.put("entry_point", DesktopLauncher.class.getName());
                info.put("os", System.getProperty("os.name")); info.put("arch", System.getProperty("os.arch"));
                info.put("java_home", System.getProperty("java.home"));
                info.put("java_version", System.getProperty("java.version"));
                info.put("modules", List.of("java.desktop", "java.net.http", "java.xml", "jdk.httpserver")
                        .stream().filter(n -> ModuleLayer.boot().findModule(n).isPresent()).toList());
                info.put("application_root", applicationRoot().toString());
                info.put("native_desktop", nativeDesktop());
                Files.writeString(Path.of(args[1]), Json.stringify(info), StandardCharsets.UTF_8);
                return;
            }
            Main.restoreStartupLanguage(args);
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                if (gui) SwingUtilities.invokeAndWait(() -> {
                    JFrame window = new JFrame("MultiRoute Resume");
                    window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    JLabel label = new JLabel(SETUP_STATUS);
                    label.setBorder(javax.swing.BorderFactory.createEmptyBorder(18,18,18,18));
                    window.add(label, java.awt.BorderLayout.CENTER);
                    JProgressBar bar = new JProgressBar(); bar.setIndeterminate(true);
                    window.add(bar, java.awt.BorderLayout.SOUTH);
                    window.pack(); window.setLocationRelativeTo(null); window.setVisible(true); progress[0] = window;
                });
                prepareWindows();
            }
            if (gui && System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")
                    && !"root".equals(System.getProperty("user.name")))
                createDesktopShortcutIfAbsent(Path.of(System.getProperty("user.home"), "Desktop"),
                        Path.of("/Applications/MultiRoute Resume.app"));
            if (progress[0] != null) SwingUtilities.invokeAndWait(progress[0]::dispose);
            Main.main(args);
        } catch (Exception failure) {
            if (progress[0] != null) SwingUtilities.invokeLater(progress[0]::dispose);
            String detail = setupErrorText(failure);
            if (gui) {
                JOptionPane.showMessageDialog(null, detail, SETUP_TITLE, JOptionPane.ERROR_MESSAGE);
            } else System.err.println(detail);
            System.exit(1);
        }
    }

    private static void prepareWindows() throws Exception {
        Path script = applicationRoot().resolve("windows/NativePrerequisites.ps1");
        if (!Files.isRegularFile(script)) throw new SetupFailure("Reinstall the complete application: its environment check is missing.");
        String system = System.getenv("SystemRoot");
        if (system == null || system.isBlank()) throw new SetupFailure("Windows system directory is unavailable.");
        Path powershell = Path.of(system, "System32/WindowsPowerShell/v1.0/powershell.exe");
        Process check = new ProcessBuilder(powershell.toString(), "-NoLogo", "-NoProfile", "-NonInteractive",
                "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                "-StateRoot", userRoot().resolve("setup").toString()).redirectErrorStream(true).start();
        var output = new java.util.concurrent.CompletableFuture<String>();
        Thread reader = new Thread(() -> {
            try (var stream = check.getInputStream()) {
                var kept = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
                while ((count = stream.read(buffer)) != -1) if (kept.size() < 65536)
                    kept.write(buffer, 0, Math.min(count, 65536-kept.size()));
                output.complete(kept.toString(StandardCharsets.UTF_8));
            } catch (Exception error) { output.complete(""); }
        }, "native-environment-check");
        reader.setDaemon(true); reader.start();
        if (!check.waitFor(30, TimeUnit.MINUTES)) {
            check.destroy(); throw new SetupFailure("Network setup timed out. Finish the official installer and reopen the application.");
        }
        String text = output.get(5, TimeUnit.SECONDS);
        if (check.exitValue() != 0) {
            String explanation = text.lines().filter(l -> l.startsWith("MultiRoute:")).reduce((a,b)->b)
                    .orElse("Network support is not ready. Finish the official Npcap installer or restart Windows, then reopen the application.");
            throw new SetupFailure(explanation);
        }
    }

}
