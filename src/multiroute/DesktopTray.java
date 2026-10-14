package multiroute;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Tray presentation only: the controller owns downloads and shutdown. All lifecycle calls use the EDT. */
final class DesktopTray implements AutoCloseable {
    private final JFrame frame;
    private final Runnable exit;
    private final Runnable languageListener = this::refresh;
    private final PropertyChangeListener trayListener = event -> SwingUtilities.invokeLater(() -> {
        // A disappearing tray must never strand a hidden, still-running application.
        if (!this.closed && this.icon != null && !installed()) showWindow();
    });
    private SystemTray systemTray;
    private TrayIcon icon;
    private MenuItem openItem, speedItem, exitItem;
    private long bytesPerSecond;
    private boolean closed;
    private String lastIconText = "";
    private String lastTooltip = "";

    DesktopTray(JFrame frame, Runnable exit) {
        this.frame = frame;
        this.exit = exit;
        I18n.addLanguageListener(languageListener);
    }

    static boolean supported() {
        return !GraphicsEnvironment.isHeadless() && SystemTray.isSupported();
    }

    /** Returns false without hiding anything when desktop tray integration is unavailable. */
    boolean install() {
        if (closed) return false;
        if (installed()) return true;
        try {
            if (!supported()) return false;
            systemTray = SystemTray.getSystemTray();
            if (icon == null) {
                PopupMenu menu = new PopupMenu();
                openItem = new MenuItem();
                openItem.addActionListener(event -> onEdt(this::showWindow));
                speedItem = new MenuItem();
                speedItem.setEnabled(false);
                exitItem = new MenuItem();
                exitItem.addActionListener(event -> onEdt(() -> { if (!closed) exit.run(); }));
                menu.add(openItem); menu.add(speedItem); menu.addSeparator(); menu.add(exitItem);
                icon = new TrayIcon(drawIcon(bytesPerSecond, systemTray.getTrayIconSize()), "", menu);
                icon.setImageAutoSize(true);
                icon.addActionListener(event -> onEdt(this::showWindow));
                // macOS uses the primary click for its native tray menu; keep that gesture available.
                if (opensWindowOnSingleClick(System.getProperty("os.name", "")))
                    icon.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent event) {
                            if (event.getButton() == MouseEvent.BUTTON1) onEdt(DesktopTray.this::showWindow);
                        }
                    });
            }
            systemTray.add(icon);
            systemTray.removePropertyChangeListener("trayIcons", trayListener);
            systemTray.addPropertyChangeListener("trayIcons", trayListener);
            lastIconText = ""; lastTooltip = "";
            refresh();
            return true;
        } catch (AWTException | UnsupportedOperationException | SecurityException unavailable) {
            return false;
        }
    }

    boolean installed() {
        if (closed || systemTray == null || icon == null) return false;
        for (TrayIcon present : systemTray.getTrayIcons()) if (present == icon) return true;
        return false;
    }

    static boolean opensWindowOnSingleClick(String osName) {
        return osName == null || !osName.toLowerCase(Locale.ROOT).contains("mac");
    }

    boolean hideWindow() {
        if (!install()) return false;
        frame.setVisible(false);
        return true;
    }

    void showWindow() {
        if (closed) return;
        frame.setExtendedState(frame.getExtendedState() & ~JFrame.ICONIFIED);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
    }

    /** Uses the full Manager snapshots, independent of selected job, sorting and table filters. */
    void update(List<?> snapshots) {
        bytesPerSecond = totalBytesPerSecond(snapshots);
        refresh();
    }

    static long totalBytesPerSecond(List<?> snapshots) {
        long total = 0;
        for (Object raw : snapshots) {
            Map<String,Object> job = Main.obj(raw);
            // Ignore any stale imported counters for jobs which cannot currently receive data.
            if (List.of("paused", "failed", "complete", "queued", "verifying").contains(Main.str(job.get("status"))))
                continue;
            long rate = Math.max(0, Main.num(job.get("speed_bps"), 0));
            total = rate > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + rate;
        }
        return total;
    }

    static String tooltip(long rate) {
        long safe = Math.max(0, rate);
        return "MultiRoute Resume — " + (gigabitRate(safe)
                ? I18n.t("Все загрузки: %.2f Гбит/с (%d Б/с)", safe * 8.0 / 1_000_000_000, safe)
                : I18n.t("Все загрузки: %.2f Мбит/с (%d Б/с)", safe * 8.0 / 1_000_000, safe));
    }

    static String formatSpeed(long rate) {
        long safe = Math.max(0, rate);
        return gigabitRate(safe) ? I18n.t("%.1f Гбит/с", safe * 8.0 / 1_000_000_000)
                : I18n.t("%.1f Мбит/с", safe * 8.0 / 1_000_000);
    }

    private static boolean gigabitRate(long rate) { return rate >= 125_000_000L; }

    static String iconUnits(long rate) {
        return gigabitRate(rate) ? "Gb/s" : "Mb/s";
    }

    private void refresh() {
        if (closed || icon == null) return;
        openItem.setLabel(I18n.t("Открыть приложение"));
        exitItem.setLabel(I18n.t("Выйти из приложения"));
        String text = tooltip(bytesPerSecond);
        speedItem.setLabel(I18n.t("Общая скорость: %s", formatSpeed(bytesPerSecond)));
        if (!text.equals(lastTooltip)) { icon.setToolTip(text); lastTooltip = text; }
        String compact = compactRate(bytesPerSecond) + " " + iconUnits(bytesPerSecond);
        if (!compact.equals(lastIconText)) {
            icon.setImage(drawIcon(bytesPerSecond, systemTray.getTrayIconSize()));
            lastIconText = compact;
        }
    }

    static String compactRate(long rate) {
        double value = Math.max(0, rate) * 8.0 / (gigabitRate(rate) ? 1_000_000_000 : 1_000_000);
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.0fG", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.0fM", value / 1_000_000);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.0fk", value / 1_000);
        if (value >= 100) return String.format(Locale.ROOT, "%.0f", value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Two high-contrast rows: a live number and its unit, including in a 16-pixel Windows tray. */
    static BufferedImage drawIcon(long rate, Dimension size) {
        int width = Math.max(16, size.width), height = Math.max(16, size.height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(12, 61, 81));
            graphics.fillRoundRect(0, 0, width, height, Math.max(3, width / 5), Math.max(3, height / 5));
            graphics.setColor(Color.WHITE);
            String number = compactRate(rate);
            float numberSize = Math.max(8, height * .60f);
            Font numberFont = new Font(Font.SANS_SERIF, Font.BOLD, 10).deriveFont(numberSize);
            while (numberSize > 6 && graphics.getFontMetrics(numberFont).stringWidth(number) > width - 1) {
                numberSize -= .5f; numberFont = numberFont.deriveFont(numberSize);
            }
            graphics.setFont(numberFont);
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(number, (width - metrics.stringWidth(number)) / 2, Math.round(height * .62f));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 7).deriveFont(Math.max(5f, height * .30f)));
            String units = iconUnits(rate);
            graphics.drawString(units, (width - graphics.getFontMetrics().stringWidth(units)) / 2, height - 1);
        } finally { graphics.dispose(); }
        return image;
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run(); else SwingUtilities.invokeLater(action);
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        I18n.removeLanguageListener(languageListener);
        if (systemTray != null) {
            systemTray.removePropertyChangeListener("trayIcons", trayListener);
            if (icon != null) systemTray.remove(icon);
        }
        icon = null;
    }
}
