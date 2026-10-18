package multiroute;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** Shared desktop palette. Download and network state never depend on the chosen appearance. */
final class DesktopTheme {
    private static boolean dark;
    private DesktopTheme() { }

    static String normalize(String value) {
        return value != null && java.util.List.of("light", "dark", "system").contains(value) ? value : "light";
    }

    static void install(String requested) {
        String selected = normalize(requested);
        try {
            String name = "system".equals(selected) ? UIManager.getSystemLookAndFeelClassName()
                    : "dark".equals(selected) ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
            UIManager.setLookAndFeel(name);
        } catch (Exception unavailable) {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
        }
        Color panel = UIManager.getLookAndFeelDefaults().getColor("Panel.background");
        dark = "dark".equals(selected) || "system".equals(selected) && panel != null
                && panel.getRed() + panel.getGreen() + panel.getBlue() < 380;
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.margin", new java.awt.Insets(5, 11, 5, 11));
        UIManager.put("TextField.margin", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
        color("Panel.background", background());
        color("Table.background", surface());
        color("Table.foreground", foreground());
        color("Table.selectionBackground", dark ? new Color(36, 67, 96) : new Color(218, 237, 251));
        color("Table.selectionForeground", foreground());
        color("TableHeader.background", dark ? new Color(37, 45, 55) : new Color(236, 242, 249));
        color("TableHeader.foreground", muted());
        color("ProgressBar.foreground", accent());
        color("ProgressBar.background", dark ? new Color(47, 58, 72) : new Color(225, 233, 241));
        color("ProgressBar.selectionBackground", foreground());
        color("ProgressBar.selectionForeground", dark ? Color.WHITE : new Color(8, 35, 45));
    }

    private static void color(String key, Color value) {
        // UIResource lets updateComponentTreeUI replace old theme defaults on existing widgets.
        UIManager.put(key, new javax.swing.plaf.ColorUIResource(value));
    }

    static boolean isDark() { return dark; }
    static Color background() { return dark ? new Color(24, 30, 39) : new Color(244, 247, 251); }
    static Color surface() { return dark ? new Color(31, 39, 49) : Color.WHITE; }
    static Color foreground() { return dark ? new Color(231, 237, 244) : new Color(31, 44, 61); }
    static Color muted() { return dark ? new Color(160, 175, 192) : new Color(88, 104, 124); }
    static Color border() { return dark ? new Color(57, 70, 85) : new Color(216, 226, 237); }
    static Color accent() { return dark ? new Color(92, 183, 250) : new Color(17, 109, 174); }
    static Color success() { return dark ? new Color(107, 218, 159) : new Color(18, 116, 73); }
    static Color warning() { return dark ? new Color(245, 195, 96) : new Color(142, 91, 10); }
    static Color danger() { return dark ? new Color(255, 145, 145) : new Color(177, 50, 57); }
    static Color graphColor(int index) {
        return Color.getHSBColor((index * .173f) % 1f, dark ? .48f : .68f, dark ? .94f : .70f);
    }

    static void card(JPanel panel) {
        panel.putClientProperty("multiroute.card", Boolean.TRUE);
        panel.setBackground(surface());
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border(), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    /** Reapply only semantic colors; values entered by the user are left intact. */
    static void decorate(Component component) { decorate(component, false); }
    private static void decorate(Component component, boolean parentCard) {
        boolean inCard = parentCard;
        if (component instanceof JComponent swing) {
            inCard |= Boolean.TRUE.equals(swing.getClientProperty("multiroute.card"));
            if (inCard && component instanceof JPanel) component.setBackground(surface());
            if (Boolean.TRUE.equals(swing.getClientProperty("multiroute.card")) && swing instanceof JPanel panel)
                card(panel);
            if ("muted".equals(swing.getClientProperty("multiroute.tone"))) component.setForeground(muted());
            if ("accent".equals(swing.getClientProperty("multiroute.tone"))) component.setForeground(accent());
            if ("success".equals(swing.getClientProperty("multiroute.tone"))) component.setForeground(success());
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) decorate(child, inCard);
    }
}
