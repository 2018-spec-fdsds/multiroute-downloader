package multiroute;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

/** Catalog, formatting, Unicode output and real Swing direction tests using only owned text. */
public final class LocalesTest40 {
    private static int checks;
    private static final Pattern FORMAT = Pattern.compile("%(?:[1-9][0-9]*\\$)?[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[sdf%]");
    private static final Pattern BRACE = Pattern.compile("\\{[0-9]+\\}");
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static List<String> tokens(Pattern pattern, String value) {
        List<String> result = new ArrayList<>(); Matcher matcher = pattern.matcher(value);
        while (matcher.find()) result.add(matcher.group()); return result;
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        Path owned = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(owned);
        check(I18n.codes().equals(List.of("ru","en","de","es","fr","zh","ko","ar","it","ja","hi","vi")), "Twelve-language selector inventory differs");
        check(I18n.DEFAULT_LANGUAGE.equals("en"), "Default language changed");
        Map<String,String[]> all = new TreeMap<>(DiagnosticText.catalogRows()); all.putAll(I18n.sourceRows());
        check(all.size() == 1479, "Unexpected canonical message inventory");
        Map<String,Object> counts = new LinkedHashMap<>();
        for (String code : LocalizedCatalog.EXTRA) {
            Map<String,String> catalog = LocalizedCatalog.catalog(code);
            check(catalog.keySet().equals(all.keySet()), "Missing or unexpected language entries: " + code);
            counts.put(code, catalog.size());
            for (var row : all.entrySet()) {
                String source = row.getKey(), text = catalog.get(source);
                check(text != null && !text.isBlank() && text.indexOf('\ufffd') < 0, "Invalid Unicode translation: " + code + "/" + source);
                check(tokens(FORMAT, row.getValue()[1]).equals(tokens(FORMAT, text)), "Printf placeholders changed: " + code + "/" + source);
                check(tokens(BRACE, row.getValue()[1]).equals(tokens(BRACE, text)), "MessageFormat placeholders changed: " + code + "/" + source);
                check(row.getValue().length == I18n.codes().size() && text.equals(row.getValue()[I18n.codes().indexOf(code)]), "Catalog does not reach UI/diagnostic binding: " + code + "/" + source);
            }
            check(!I18n.displayName(code).equals("English"), "Missing native language name: " + code);
            String gateway = I18n.tFor(code, "Шлюз %s", "192.168.1.3");
            check(gateway.contains("192.168.1.3") && !gateway.contains("Шлюз"), "Gateway data changed or label remained Russian");
            String error = DiagnosticText.renderFor(code, "running");
            check(!error.equals("running") && error.indexOf('\ufffd') < 0, "Diagnostic was not translated: " + code);
        }
        String name = "Файл_日本語_한국어_العربية_é_🚀.bin";
        Map<String,Object> state = new LinkedHashMap<>(); state.put("id", "123456789abc"); state.put("filename", name);
        state.put("directory", owned.resolve("payload").toString()); DownloadHistory.initialize(state);
        DownloadHistory history = new DownloadHistory(owned, state); DownloadHistory.begin(state); history.checkpoint(state);
        for (String code : I18n.codes()) {
            String guide = "https://github.com/2018-spec-fdsds/multiroute-downloader/blob/v" + Main.VERSION + "/docs/" + code + ".md";
            check(DesktopUI.documentationUri(code, false).toString().equals(guide), "Incorrect language-specific guide URL: " + code);
            check(DesktopUI.documentationUri(code, true).toString().equals(guide + "#multiroute-api"), "Incorrect language-specific API guide URL: " + code);
            String rendered = DownloadHistory.snapshotText(owned, code);
            check(rendered.contains(name) && rendered.contains(owned.toString()) && rendered.indexOf('\ufffd') < 0, "History Unicode path did not reach viewer: " + code);
            Files.writeString(owned.resolve("history-" + code + ".txt"), rendered, StandardCharsets.UTF_8);
            check(Files.readString(owned.resolve("history-" + code + ".txt"), StandardCharsets.UTF_8).equals(rendered), "UTF-8 output roundtrip failed");
            String operation = "Resolve torrent bootstrap";
            String statusTemplate = "Gateway could not resolve torrent bootstrap (SOCKS status %d)";
            String status = String.format(Locale.ROOT, statusTemplate, 7);
            String localizedOperation = DiagnosticText.renderFor(code, operation);
            check(localizedOperation.equals(I18n.tFor(code, operation)) && (code.equals("en") || !localizedOperation.equals(operation)), "Bootstrap operation was not translated: " + code);
            check(DiagnosticText.renderFor(code, status).equals(I18n.tFor(code, statusTemplate, 7)), "Canonical SOCKS status did not switch language: " + code);
            String networkTemplate = "Network request failed: gateway=%s server=%s operation=%s elapsed_ms=%d error=%s";
            String canonical = String.format(Locale.ROOT, networkTemplate, "192.168.1.3", "router.bittorrent.com:6881", operation, 10000, status);
            String nested = DiagnosticText.renderFor(code, canonical);
            check(nested.equals(I18n.tFor(code, networkTemplate, "192.168.1.3", "router.bittorrent.com:6881", DiagnosticText.arg(operation), 10000, DiagnosticText.arg(status))), "Nested bootstrap network diagnostic lost a translated field: " + code);
            check(DiagnosticText.renderFor(code, "java.io.IOException: " + status).equals("java.io.IOException: " + I18n.tFor(code, statusTemplate, 7)), "Bootstrap exception prefix prevented translation: " + code);
            Path bootstrapLog = owned.resolve("bootstrap-" + code + ".txt");
            Files.writeString(bootstrapLog, nested, StandardCharsets.UTF_8);
            check(Files.readString(bootstrapLog, StandardCharsets.UTF_8).equals(nested) && nested.indexOf('\ufffd') < 0, "Bootstrap UTF-8 output roundtrip failed: " + code);
        }
        SwingUtilities.invokeAndWait(() -> {
            DesktopTheme.install("light"); DesktopUI.installReadableFonts();
            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = I18n.text(new JLabel(), "Загрузки"); JTextField url = new JTextField("https://example.invalid/Файл?token=own");
            panel.add(label, BorderLayout.LINE_START); panel.add(url, BorderLayout.LINE_END);
            for (String code : I18n.codes()) {
                I18n.setLanguage(code); I18n.refresh(panel);
                check(panel.getComponentOrientation().isLeftToRight() != code.equals("ar"), "Wrong interface direction: " + code);
                check(url.getComponentOrientation().isLeftToRight() && url.getText().equals("https://example.invalid/Файл?token=own"), "URL logical order or content changed");
                check(label.getText().equals(I18n.t("Загрузки")), "Label did not switch language");
                check(label.getFont().canDisplayUpTo(label.getText()) < 0, "System fallback font lacks translated label glyphs: " + code);
            }
            I18n.setLanguage("en");
        });
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", "PASS"); report.put("version", Main.VERSION);
        report.put("checks", checks); report.put("catalog_entries", all.size()); report.put("extra_locales", counts);
        report.put("languages", I18n.codes()); report.put("arabic_rtl_and_ltr_data_fields", true);
        report.put("utf8_ui_history_output_roundtrip", true); report.put("network_calls", 0); report.put("user_data_read", false);
        report.put("bootstrap_nested_diagnostics_and_utf8_output", true);
        Files.writeString(owned.resolve("LOCALES-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
        System.out.println("PASS " + checks + " locale checks");
    }
}
