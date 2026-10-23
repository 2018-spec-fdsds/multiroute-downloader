package multiroute;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.event.*;

/** A stable network editor: refreshes never replace controls or overwrite an unsaved draft. */
final class DnsSettingsPanel extends JPanel implements AutoCloseable {
    interface Controller {
        Map<String,Object> apply(Map<String,Object> settings) throws Exception;
        Map<String,Object> test(Map<String,Object> settings, List<String> gateways) throws Exception;
    }
    private static final List<String> CONFIG_KEYS = List.of("mode", "provider", "doh_url", "dot_host", "dot_port", "bootstrap_ips");
    private static final String SYSTEM_HELP = "System DNS follows the computer or VPN resolver policy; file connections still use their selected gateway.";
    private static final String SECURE_HELP = "DoH and DoT encrypt DNS through each selected gateway. Certificates and server names are verified; there is no fallback to unencrypted DNS.";
    private static final String BOOTSTRAP_HELP = "Numeric bootstrap addresses connect to the DNS provider without an initial unencrypted lookup.";
    final JComboBox<String> mode = new JComboBox<>(new String[]{"system", "doh", "dot"});
    final JComboBox<String> provider = new JComboBox<>(new String[]{"cloudflare", "quad9", "google", "custom"});
    final JTextField dohUrl = new JTextField(40), dotHost = new JTextField(30), bootstrap = new JTextField(40);
    final JSpinner dotPort = new JSpinner(new SpinnerNumberModel(853, 1, 65535, 1));
    final JButton apply = I18n.text(new JButton(), "Apply DNS settings"), test = I18n.text(new JButton(), "Test DNS");
    final JLabel applied = new JLabel(), status = new JLabel();
    final JTextArea help = new StatusTextArea(3, 20);
    final LogView results = new LogView(false);
    Controller controller;
    private final Executor background;
    private final Supplier<List<String>> selectedGateways;
    private final Runnable languageListener = this::languageChanged;
    private boolean loading, dirty, busy, closed;
    private long appliedRevision = -1;
    private Map<String,Object> lastApplied = Map.of(), lastResults = Map.of();

    DnsSettingsPanel(Controller controller, Executor background, Supplier<List<String>> selectedGateways) {
        super(new GridBagLayout());
        this.controller = controller; this.background = background; this.selectedGateways = selectedGateways;
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(I18n.t("DNS resolution")),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        mode.setRenderer(renderer(true)); provider.setRenderer(renderer(false));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(3, 0, 3, 0);
        int row = 0;
        addRow(mode, c, row++); addRow(label("DNS provider"), c, row++); addRow(provider, c, row++);
        addRow(label("DoH URL"), c, row++); addRow(dohUrl, c, row++);
        JPanel dot = new JPanel(new BorderLayout(6, 0)); dot.add(dotHost, BorderLayout.CENTER); dot.add(dotPort, BorderLayout.LINE_END);
        JPanel dotLabels = new JPanel(new BorderLayout()); dotLabels.add(label("DoT server name"), BorderLayout.CENTER); dotLabels.add(label("DoT port"), BorderLayout.LINE_END);
        addRow(dotLabels, c, row++); addRow(dot, c, row++);
        addRow(label("Bootstrap IPv4 addresses"), c, row++); addRow(bootstrap, c, row++);
        help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false); help.setFont(UIManager.getFont("Label.font"));
        addRow(help, c, row++); addRow(applied, c, row++); addRow(status, c, row++);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0)); buttons.add(apply); buttons.add(test); addRow(buttons, c, row++);
        results.text.setRows(5); results.setPreferredSize(new Dimension(0, 115)); addRow(results, c, row);
        I18n.tooltip(mode, SECURE_HELP); I18n.tooltip(provider, SECURE_HELP); I18n.tooltip(bootstrap, BOOTSTRAP_HELP);
        I18n.tooltip(dohUrl, SECURE_HELP); I18n.tooltip(dotHost, SECURE_HELP); I18n.tooltip(dotPort, SECURE_HELP);
        I18n.tooltip(apply, "Apply DNS settings"); I18n.tooltip(test, "Test DNS");
        DocumentListener edits = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { edited(); }
            public void removeUpdate(DocumentEvent e) { edited(); }
            public void changedUpdate(DocumentEvent e) { edited(); }
        };
        for (JTextField field : List.of(dohUrl, dotHost, bootstrap)) field.getDocument().addDocumentListener(edits);
        dotPort.addChangeListener(event -> edited());
        mode.addActionListener(event -> { if (!loading) { presetIfSelected(); edited(); } updateEnabled(); });
        provider.addActionListener(event -> { if (!loading) { presetIfSelected(); edited(); } updateEnabled(); });
        apply.addActionListener(event -> run(false)); test.addActionListener(event -> run(true));
        I18n.addLanguageListener(languageListener);
        refresh(new LinkedHashMap<>(SecureDns.Config.defaults().toJson()));
        languageChanged();
    }

    private void addRow(Component component, GridBagConstraints c, int row) { c.gridy = row; add(component, c); }
    private static JLabel label(String key) { return I18n.text(new JLabel(), key); }
    static String modeKey(String value) { return switch(value) { case "system" -> "System DNS (computer)"; case "dot" -> "DNS over TLS (DoT)"; default -> "DNS over HTTPS (DoH)"; }; }
    static String providerKey(String value) { return switch(value) { case "quad9" -> "Quad9"; case "google" -> "Google"; case "custom" -> "Custom"; default -> "Cloudflare"; }; }
    private static ListCellRenderer<String> renderer(boolean modes) {
        return (list, value, index, selected, focus) -> {
            JLabel text = (JLabel)new DefaultListCellRenderer().getListCellRendererComponent(list, value, index, selected, focus);
            text.setText(I18n.t(modes ? modeKey(value == null ? "doh" : value) : providerKey(value == null ? "cloudflare" : value))); return text;
        };
    }

    void refresh(Map<String,Object> settings) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Refresh DNS controls on the EDT");
        long revision = Main.num(settings.get("revision"), 1);
        if (revision < appliedRevision) return;
        Map<String,Object> projection = projection(settings);
        if (projection.isEmpty()) projection = SecureDns.Config.defaults().toJson();
        if (lastApplied.equals(settings)) return;
        boolean configChanged = appliedRevision < 0 || !projection(lastApplied).equals(projection);
        lastApplied = new LinkedHashMap<>(settings); appliedRevision = revision;
        if (!dirty && !busy && configChanged) display(SecureDns.Config.fromJson(projection));
        I18n.text(applied, "Applied DNS: %s · %s", DiagnosticText.arg(modeKey(Main.str(projection.get("mode")))),
                DiagnosticText.arg(providerKey(Main.str(projection.get("provider")))));
        String error = Main.str(settings.get("configuration_error"));
        if (!error.isBlank()) I18n.text(status, "DNS settings could not be loaded: %s", DiagnosticText.arg(error));
        updateEnabled();
    }

    private void display(SecureDns.Config config) {
        loading = true;
        try {
            mode.setSelectedItem(config.mode()); provider.setSelectedItem(config.provider()); dohUrl.setText(config.dohUrl());
            dotHost.setText(config.serverName()); dotPort.setValue(config.port()); bootstrap.setText(String.join(", ", config.bootstrapIpv4()));
        } finally { loading = false; }
        updateEnabled();
    }
    private void presetIfSelected() {
        String chosen = Main.str(provider.getSelectedItem());
        if (!chosen.equals("custom")) display(SecureDns.Config.preset(chosen, Main.str(mode.getSelectedItem())));
    }
    private void edited() {
        if (loading || closed) return; dirty = true; I18n.text(status, "Unsaved DNS settings"); updateEnabled();
    }
    Map<String,Object> draft() {
        dotPortCommit();
        List<String> addresses = Arrays.stream(bootstrap.getText().trim().split("[,\\s]+", -1)).filter(value -> !value.isBlank()).toList();
        Map<String,Object> values = new LinkedHashMap<>(); values.put("mode", Main.str(mode.getSelectedItem()));
        values.put("provider", Main.str(provider.getSelectedItem())); values.put("doh_url", dohUrl.getText().trim());
        values.put("dot_host", dotHost.getText().trim()); values.put("dot_port", ((Number)dotPort.getValue()).intValue()); values.put("bootstrap_ips", addresses);
        return SecureDns.Config.fromJson(values).toJson();
    }
    private void dotPortCommit() { try { dotPort.commitEdit(); } catch (java.text.ParseException invalid) { throw new IllegalArgumentException("Invalid DoT port", invalid); } }
    private void updateEnabled() {
        boolean system = "system".equals(mode.getSelectedItem()), custom = "custom".equals(provider.getSelectedItem());
        mode.setEnabled(!busy); provider.setEnabled(!busy && !system);
        dohUrl.setEnabled(!busy && custom && "doh".equals(mode.getSelectedItem()));
        dotHost.setEnabled(!busy && custom && "dot".equals(mode.getSelectedItem()));
        dotPort.setEnabled(!busy && custom && "dot".equals(mode.getSelectedItem())); bootstrap.setEnabled(!busy && custom && !system);
        apply.setEnabled(!busy); test.setEnabled(!busy);
        I18n.text(help, system ? SYSTEM_HELP : SECURE_HELP);
    }

    private void run(boolean check) {
        if (busy || closed) return;
        final Map<String,Object> config; final List<String> gateways;
        try {
            config = draft(); gateways = check ? List.copyOf(selectedGateways.get()) : List.of();
            if (check && gateways.isEmpty()) throw new IllegalArgumentException("Choose at least one gateway for the DNS test");
        } catch (Exception invalid) { showFailure(invalid); return; }
        busy = true; updateEnabled(); if (check) I18n.text(status, "Testing DNS through selected gateways…");
        background.execute(() -> {
            try {
                Map<String,Object> result = check ? controller.test(config, gateways) : controller.apply(config);
                SwingUtilities.invokeLater(() -> {
                    if (closed) return; busy = false;
                    if (check) {
                        lastResults = new LinkedHashMap<>(result); renderResults();
                        List<Object> rows = Main.arr(result.get("results")); long okay = rows.stream().filter(row -> Boolean.TRUE.equals(Main.obj(row).get("ok"))).count();
                        I18n.text(status, "DNS test complete: %s/%s gateways", okay, rows.size());
                    } else { dirty = false; lastApplied = Map.of(); refresh(result); I18n.text(status, "DNS settings saved"); }
                    updateEnabled();
                });
            } catch (Exception failed) { SwingUtilities.invokeLater(() -> { if (!closed) { busy = false; showFailure(failed); updateEnabled(); } }); }
        });
    }
    private void showFailure(Exception failure) { I18n.text(status, "%s", DiagnosticText.arg(failure.getMessage())); }
    private void renderResults() {
        StringBuilder text = new StringBuilder();
        for (Object item : Main.arr(lastResults.getOrDefault("results", List.of()))) {
            Map<String,Object> row = Main.obj(item); text.append(Main.str(row.get("gateway"))).append(": ");
            if (Boolean.TRUE.equals(row.get("ok"))) text.append("OK · ").append(Main.num(row.get("elapsed_ms"), 0)).append(" ms · ").append(row.get("addresses"));
            else text.append(I18n.error(Main.str(row.get("error")))); text.append('\n');
        }
        results.updateSnapshot(text.toString());
    }
    private void languageChanged() {
        if (closed) return;
        ((javax.swing.border.TitledBorder)((javax.swing.border.CompoundBorder)getBorder()).getOutsideBorder()).setTitle(I18n.t("DNS resolution"));
        mode.repaint(); provider.repaint(); updateEnabled(); renderResults(); repaint();
    }
    static Map<String,Object> projection(Map<String,Object> settings) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (String name : CONFIG_KEYS) if (settings.containsKey(name)) result.put(name, settings.get(name)); return result;
    }
    boolean hasUnsavedDraft() { return dirty; }
    boolean isBusy() { return busy; }
    @Override public void close() { closed = true; I18n.removeLanguageListener(languageListener); }
}
