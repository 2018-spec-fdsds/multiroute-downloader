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
    private static final List<String> DNS_KEYS = List.of(
            "DNS resolution",
            "System DNS (computer)",
            "DNS over HTTPS (DoH)",
            "DNS over TLS (DoT)",
            "DNS provider",
            "Custom",
            "DoH URL",
            "DoT server name",
            "DoT port",
            "Bootstrap IPv4 addresses",
            "Apply DNS settings",
            "Test DNS",
            "DNS settings saved",
            "Testing DNS through selected gateways…",
            "DNS test complete: %s/%s gateways",
            "Unsaved DNS settings",
            "Applied DNS: %s · %s",
            "Choose at least one gateway for the DNS test",
            "System DNS follows the computer or VPN resolver policy; file connections still use their selected gateway.",
            "DoH and DoT encrypt DNS through each selected gateway. Certificates and server names are verified; there is no fallback to unencrypted DNS.",
            "Numeric bootstrap addresses connect to the DNS provider without an initial unencrypted lookup.",
            "DNS settings could not be loaded: %s",
            "DNS lookup failed via gateway %s (%s): %s",
            "DNS settings applied: %s · %s",
            "DNS settings apply failed: %s",
            "DNS test interrupted",
            "DNS test timed out",
            "DNS gateway generation is not ready",
            "Check the application network before testing DNS",
            "Correct the saved DNS settings before connecting",
            "DNS response contains no public IPv4 answer",
            "Another DNS test is still running"
    );
    private static final List<String> DNS_PROTOCOL_KEYS = List.of(
            "Ambiguous DoH HTTP framing",
            "Conflicting DNS CNAME targets",
            "DNS CNAME loop",
            "DNS deadline expired",
            "DNS endpoint must use a public IPv4 address",
            "DNS name has both CNAME and A records",
            "DNS record count or query flags are invalid",
            "DNS response does not match its query",
            "DNS response has no usable public IPv4 answer",
            "DNS returned a non-public IPv4 answer",
            "DNS returned response code %d",
            "Default TLS context unavailable",
            "DoH Age overflow",
            "DoH Content-Length overflow",
            "DoH HTTP headers too large",
            "DoH HTTP line too large",
            "DoH HTTP status %d",
            "DoH body exceeds DNS size limit",
            "DoH requires an authentication hostname",
            "DoH trailers too large",
            "DoT requires an authentication hostname",
            "Encrypted DNS deadline expired",
            "Encrypted DNS failed for every configured bootstrap address",
            "Encrypted DNS needs a numeric bootstrap IPv4 address",
            "Encrypted DNS query interrupted",
            "Expected a literal public IPv4 address",
            "Expected an HTTPS DoH URL without credentials, query or fragment",
            "Expected at most eight bootstrap IPv4 addresses",
            "Expected literal bootstrap IPv4 addresses",
            "Forward or looping DNS compression pointer",
            "Invalid DNS A record length",
            "Invalid DNS CNAME record",
            "Invalid DNS OPT record",
            "Invalid DNS hostname",
            "Invalid DNS label length",
            "Invalid DNS message size",
            "Invalid DNS name character",
            "Invalid DNS name compression",
            "Invalid DNS text field: %s",
            "Invalid DoH Age",
            "Invalid DoH Content-Length",
            "Invalid DoH Content-Type",
            "Invalid DoH HTTP character",
            "Invalid DoH HTTP header",
            "Invalid DoH HTTP line ending",
            "Invalid DoH HTTP status line",
            "Invalid DoH URL",
            "Invalid DoH body size",
            "Invalid DoH chunk ending",
            "Invalid DoH chunk length",
            "Invalid DoH port",
            "Invalid DoH trailer",
            "Invalid DoT frame length",
            "Invalid DoT port",
            "Invalid literal IPv4 address",
            "Invalid or truncated DNS header",
            "Managed DNS is closed",
            "Managed DNS supports one IN A question",
            "Missing DoH URL",
            "Missing DoT authentication hostname",
            "Missing numeric DNS connector",
            "Missing or duplicated DoH age",
            "Missing or duplicated DoH content-encoding",
            "Missing or duplicated DoH content-length",
            "Missing or duplicated DoH content-type",
            "Missing or duplicated DoH transfer-encoding",
            "Numeric DNS connector returned an unconnected socket",
            "Preset values are fixed; choose Custom to change servers",
            "System DNS deadline expired",
            "System DNS has no usable public IPv4 answer",
            "System DNS lookup cancelled",
            "System DNS lookup capacity exhausted",
            "System DNS lookup failed",
            "System DNS lookup interrupted",
            "Too many DoH HTTP headers",
            "Too many DoH chunks",
            "Too many DoH trailers",
            "Truncated DNS name",
            "Truncated DNS record",
            "Truncated DNS record data",
            "Truncated encrypted DNS response",
            "Unexpected DoH Content-Encoding",
            "Unexpected DoH informational response",
            "Unexpected trailing DNS bytes",
            "Unknown DNS configuration field: %s",
            "Unknown DNS mode",
            "Unknown DNS provider",
            "Unsafe DoH framing trailer",
            "Unsupported DoH Transfer-Encoding"
    );
    private static final List<String> DNS_ROOT_KEYS = List.of(
            "Another DNS test is still running",
            "Check the application network before testing DNS",
            "Choose at least one gateway for the DNS test",
            "Choose configured gateways for the DNS test",
            "Correct the saved DNS settings before connecting",
            "DNS broker authentication rejected",
            "DNS connection closed early",
            "DNS gateway generation changed",
            "DNS gateway generation is not ready",
            "DNS policy revision is unavailable",
            "DNS query timed out",
            "DNS response contains no public IPv4 answer",
            "DNS service is closed",
            "DNS settings acknowledgement does not match the requested revision",
            "DNS settings revision exhausted",
            "DNS test interrupted",
            "DNS test timed out",
            "Invalid DNS broker protocol",
            "Invalid DNS gateway address",
            "Invalid DNS gateway field",
            "Invalid DNS query budget",
            "Invalid DNS query size",
            "Invalid DNS response size",
            "Invalid DNS settings file",
            "Invalid DNS settings revision",
            "Invalid DNS test gateway",
            "Invalid DNS test gateway list",
            "Invalid DNS tunnel acknowledgement",
            "Invalid DNS tunnel error size",
            "Invalid DNS tunnel target",
            "Invalid DoT port",
            "Invalid managed DNS readiness report",
            "Invalid managed DNS tunnel port",
            "Managed DNS bridge is not prepared",
            "Provide DNS settings for the test",
            "Refresh DNS controls on the EDT",
            "Update the network component for managed DNS"
    );
    private static final List<String> DNS_HTTP2_KEYS = List.of(
            "DoH HTTP status %d",
            "DoH HTTP/2 connection closed by server",
            "DoH HTTP/2 stream reset (code %d)",
            "DoH request headers too large",
            "Duplicate DoH %s",
            "Forbidden HTTP/2 connection header",
            "HTTP/2 compressed header limit exceeded",
            "HTTP/2 connection window overflow",
            "HTTP/2 decoded header limit exceeded",
            "HTTP/2 response limit exceeded",
            "HTTP/2 stream window overflow",
            "Incomplete DoH HTTP/2 body",
            "Interrupted HTTP/2 header block",
            "Invalid DATA padding",
            "Invalid DNS request size",
            "Invalid DoH %s",
            "Invalid DoH Content-Length",
            "Invalid DoH Content-Type",
            "Invalid DoH HPACK headers",
            "Invalid DoH URI",
            "Invalid DoH body size",
            "Invalid DoH trailers",
            "Invalid HEADERS padding",
            "Invalid HEADERS priority",
            "Invalid HTTP/2 CONNECT setting",
            "Invalid HTTP/2 GOAWAY",
            "Invalid HTTP/2 PING",
            "Invalid HTTP/2 PRIORITY",
            "Invalid HTTP/2 RST_STREAM",
            "Invalid HTTP/2 SETTINGS",
            "Invalid HTTP/2 SETTINGS acknowledgement",
            "Invalid HTTP/2 WINDOW_UPDATE",
            "Invalid HTTP/2 header name",
            "Invalid HTTP/2 header value",
            "Invalid HTTP/2 initial window",
            "Invalid HTTP/2 maximum frame size",
            "Invalid response pseudo-header",
            "Missing DoH %s",
            "Missing DoH HTTP/2 status",
            "Missing server SETTINGS preface",
            "Self-dependent HTTP/2 stream",
            "Server sent SETTINGS_ENABLE_PUSH",
            "Too many DoH headers",
            "Truncated HTTP/2 frame",
            "Unexpected DoH Content-Encoding",
            "Unexpected HTTP/2 CONTINUATION",
            "Unexpected HTTP/2 DATA",
            "Unexpected HTTP/2 HEADERS",
            "Unexpected informational DoH response",
            "Unsolicited HTTP/2 server push",
            "Zero HTTP/2 window increment"
    );
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
        check(all.size() == 1693, "Unexpected canonical message inventory");
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
            StringBuilder dnsText = new StringBuilder();
            for (String key : DNS_KEYS) {
                String[] row = I18n.sourceRows().get(key);
                check(row != null && row.length == 12, "Missing DNS source row: " + key);
                String translated = I18n.tFor(code, key);
                check(!translated.isBlank() && translated.indexOf('\ufffd') < 0, "Invalid DNS Unicode: " + code + "/" + key);
                check(code.equals("en") || !translated.equals(key), "DNS text fell back to English: " + code + "/" + key);
                check(tokens(FORMAT, key).equals(tokens(FORMAT, translated)), "DNS placeholders changed: " + code + "/" + key);
                dnsText.append(translated).append('\n');
            }
            String dnsTemplate = "DNS lookup failed via gateway %s (%s): %s";
            String dnsCanonical = String.format(Locale.ROOT, dnsTemplate, "192.168.1.3", "DoH", "DNS test timed out");
            String dnsDiagnostic = DiagnosticText.renderFor(code, dnsCanonical);
            check(dnsDiagnostic.equals(I18n.tFor(code, dnsTemplate, "192.168.1.3", "DoH", DiagnosticText.arg("DNS test timed out"))), "DNS nested diagnostic did not switch language: " + code);
            check(dnsDiagnostic.contains("192.168.1.3") && dnsDiagnostic.contains("DoH"), "DNS gateway/protocol data changed: " + code);
            for (String protocolKey : DNS_PROTOCOL_KEYS) {
                String[] row = all.get(protocolKey);
                check(row != null && row.length == 12, "Missing DNS protocol source: " + protocolKey);
                String translated = I18n.tFor(code, protocolKey);
                check(!translated.isBlank() && translated.indexOf('\ufffd') < 0, "Invalid DNS protocol Unicode: " + code + "/" + protocolKey);
                check(code.equals("en") || !translated.equals(protocolKey), "DNS protocol error fell back to English: " + code + "/" + protocolKey);
                check(tokens(FORMAT, protocolKey).equals(tokens(FORMAT, translated)), "DNS protocol placeholders changed: " + code + "/" + protocolKey);
                dnsText.append(translated).append('\n');
            }
            for (String protocolKey : List.of("Unknown DNS configuration field: %s", "Invalid DNS text field: %s", "DNS returned response code %d", "DoH HTTP status %d")) {
                Object argument = protocolKey.endsWith("%d") ? 7 : "DNS test timed out";
                String canonical = String.format(Locale.ROOT, protocolKey, argument);
                String localized = DiagnosticText.renderFor(code, canonical);
                check(localized.equals(I18n.tFor(code, protocolKey, argument)), "Protocol data argument was translated or lost its numeric type: " + code + "/" + protocolKey);
                check(DiagnosticText.renderFor(code, "java.io.IOException: " + canonical).equals("java.io.IOException: " + localized), "Protocol exception prefix prevented translation: " + code);
                dnsText.append(localized).append('\n');
            }
            String dnsNetworkTemplate = "Network request failed: gateway=%s server=%s operation=%s elapsed_ms=%d error=%s";
            String dnsNetwork = String.format(Locale.ROOT, dnsNetworkTemplate, "192.168.1.3", "resolver.invalid:443", "Test DNS", 8000, "DoH HTTP status 502");
            check(DiagnosticText.renderFor(code, dnsNetwork).equals(I18n.tFor(code, dnsNetworkTemplate, "192.168.1.3", "resolver.invalid:443", DiagnosticText.arg("Test DNS"), 8000, DiagnosticText.arg("DoH HTTP status 502"))), "Network journal did not rerender the nested DNS protocol error: " + code);
            for (String rootKey : DNS_ROOT_KEYS) {
                String[] row = all.get(rootKey);
                check(row != null && row.length == 12, "Missing root DNS source: " + rootKey);
                String translated = I18n.tFor(code, rootKey);
                check(!translated.isBlank() && translated.indexOf('\ufffd') < 0, "Invalid root DNS Unicode: " + code + "/" + rootKey);
                check(code.equals("en") || !translated.equals(rootKey), "Root DNS error fell back to English: " + code + "/" + rootKey);
                check(DiagnosticText.renderFor(code, rootKey).equals(translated), "Stored root DNS error did not rerender: " + code + "/" + rootKey);
                dnsText.append(translated).append('\n');
            }
            String brokerError = String.format(Locale.ROOT, dnsTemplate, "192.168.1.3", "DoH", "DNS broker authentication rejected");
            check(DiagnosticText.renderFor(code, brokerError).equals(I18n.tFor(code, dnsTemplate, "192.168.1.3", "DoH", DiagnosticText.arg("DNS broker authentication rejected"))), "Nested broker error did not rerender: " + code);
            for (String h2Key : DNS_HTTP2_KEYS) {
                check(all.containsKey(h2Key), "Missing HTTP/2 source: " + h2Key);
                String translated = I18n.tFor(code, h2Key);
                check(!translated.isBlank() && translated.indexOf('\ufffd') < 0, "Invalid HTTP/2 Unicode: " + code + "/" + h2Key);
                check(code.equals("en") || !translated.equals(h2Key), "HTTP/2 text fell back to English: " + code + "/" + h2Key);
                check(tokens(FORMAT, h2Key).equals(tokens(FORMAT, translated)), "HTTP/2 placeholders changed: " + code + "/" + h2Key);
                if (!h2Key.contains("%")) check(DiagnosticText.renderFor(code, h2Key).equals(translated), "Stored HTTP/2 error did not rerender: " + code + "/" + h2Key);
                dnsText.append(translated).append('\n');
            }
            for (String h2Key : List.of("DoH HTTP/2 stream reset (code %d)", "Missing DoH %s", "Duplicate DoH %s", "Invalid DoH %s")) {
                Object value = h2Key.contains("%d") ? 4294967295L : "DNS test timed out";
                String canonicalH2 = String.format(Locale.ROOT, h2Key, value);
                String expectedH2 = I18n.tFor(code, h2Key, value);
                check(DiagnosticText.renderFor(code, canonicalH2).equals(expectedH2), "HTTP/2 protocol data changed or argument type lost: " + code + "/" + h2Key);
                check(DiagnosticText.renderFor(code, "java.io.IOException: " + canonicalH2).equals("java.io.IOException: " + expectedH2), "HTTP/2 exception prefix prevented translation: " + code);
                String nestedH2 = String.format(Locale.ROOT, dnsTemplate, "192.168.1.3", "DoH", canonicalH2);
                check(DiagnosticText.renderFor(code, nestedH2).equals(I18n.tFor(code, dnsTemplate, "192.168.1.3", "DoH", DiagnosticText.arg(canonicalH2))), "HTTP/2 nested log lost translation: " + code);
                dnsText.append(expectedH2).append('\n');
            }
            Path dnsLog = owned.resolve("DNS_Ж_日本語_" + code + ".txt");
            String dnsOutput = dnsText + dnsDiagnostic + "\n";
            Files.writeString(dnsLog, dnsOutput, StandardCharsets.UTF_8);
            check(Files.readString(dnsLog, StandardCharsets.UTF_8).equals(dnsOutput), "DNS UTF-8 file/log roundtrip failed: " + code);
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
            JLabel dnsLabel = I18n.text(new JLabel(), "DNS resolution");
            String dnsTooltip = "DoH and DoT encrypt DNS through each selected gateway. Certificates and server names are verified; there is no fallback to unencrypted DNS.";
            I18n.tooltip(dnsLabel, dnsTooltip); panel.add(dnsLabel, BorderLayout.NORTH);
            for (String code : I18n.codes()) {
                I18n.setLanguage(code); I18n.refresh(panel);
                check(panel.getComponentOrientation().isLeftToRight() != code.equals("ar"), "Wrong interface direction: " + code);
                check(url.getComponentOrientation().isLeftToRight() && url.getText().equals("https://example.invalid/Файл?token=own"), "URL logical order or content changed");
                check(label.getText().equals(I18n.t("Загрузки")), "Label did not switch language");
                check(dnsLabel.getText().equals(I18n.t("DNS resolution")), "DNS label did not switch language: " + code);
                check(dnsLabel.getToolTipText().equals(I18n.t(dnsTooltip)), "DNS tooltip did not switch language: " + code);
                check(dnsLabel.getFont().canDisplayUpTo(dnsLabel.getText()) < 0, "System fallback font lacks DNS label glyphs: " + code);
                check(label.getFont().canDisplayUpTo(label.getText()) < 0, "System fallback font lacks translated label glyphs: " + code);
            }
            I18n.setLanguage("en");
        });
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", "PASS"); report.put("version", Main.VERSION);
        report.put("checks", checks); report.put("catalog_entries", all.size()); report.put("extra_locales", counts);
        report.put("languages", I18n.codes()); report.put("arabic_rtl_and_ltr_data_fields", true);
        report.put("utf8_ui_history_output_roundtrip", true); report.put("network_calls", 0); report.put("user_data_read", false);
        report.put("bootstrap_nested_diagnostics_and_utf8_output", true);
        report.put("dns_keys", DNS_KEYS.size()); report.put("dns_ui_nested_diagnostics_utf8_output", true);
        report.put("dns_protocol_keys", DNS_PROTOCOL_KEYS.size()); report.put("dns_protocol_dynamic_and_network_errors", true);
        report.put("dns_root_keys", DNS_ROOT_KEYS.size()); report.put("dns_root_and_nested_broker_errors", true);
        report.put("dns_http2_keys", DNS_HTTP2_KEYS.size()); report.put("dns_http2_nested_typed_data_utf8", true);
        Files.writeString(owned.resolve("LOCALES-TESTS.json"), Json.stringify(report), StandardCharsets.UTF_8);
        System.out.println("PASS " + checks + " locale checks");
    }
}
