package multiroute;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Owned protocol lines; checks the native pipe, UTF-8 persistence and language switching. */
public final class DnsFallbackLogTest41 {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        Path own = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(own);
        try {
            I18n.setLanguage("en");
            AppLog.initialize(own.resolve("профиль-日本語"));
            AppLog.nativeLine("DNS_TCP_FALLBACK gateway=192.168.1.1 resolver=1.1.1.1 result=ok");
            AppLog.nativeLine("DNS_TCP_FALLBACK gateway=192.168.1.4 resolver=192.168.1.4 result=failed");
            AppLog.flush();
            String english = AppLog.snapshotText();
            check(english.contains("DNS over TCP succeeded: gateway=192.168.1.1 resolver=1.1.1.1"), "Success protocol fields lost");
            check(english.contains("DNS over TCP failed: gateway=192.168.1.4 resolver=192.168.1.4"), "Failure protocol fields lost");
            check(!Files.exists(AppLog.logsDirectory().resolve("network-errors.log")), "Fallback observation was recorded as a tunnel failure");
            for (String language : List.of("en", "ru", "de", "es", "fr", "zh", "ko", "ar", "it", "ja", "hi", "vi")) {
                I18n.setLanguage(language);
                AppLog.flush();
                String snapshot = AppLog.snapshotText();
                check(snapshot.contains("192.168.1.1") && snapshot.contains("1.1.1.1"), "Native identity changed during translation");
                check(!snapshot.contains("\ufffd") && !snapshot.contains("DNS_TCP_FALLBACK"), "UTF-8 or protocol presentation leaked");
                if (!language.equals("en")) check(!snapshot.contains("DNS over TCP succeeded:"), "Untranslated fallback success: " + language);
                check(snapshot.equals(Files.readString(own.resolve("профиль-日本語/network/gateway-network.log"), StandardCharsets.UTF_8)), "Persistent rendering differs from UI snapshot");
            }
            AppLog.nativeLine("DNS_TCP_FALLBACK gateway=192.168.1.999 resolver=1.1.1.1 result=ok");
            AppLog.nativeLine("DNS_TCP_FALLBACK gateway=192.168.1.1 resolver=1.1.1.1 result=ok /secret?token=owned-private");
            AppLog.flush();
            check(!AppLog.snapshotText().contains("owned-private") && !AppLog.snapshotText().contains("192.168.1.999"), "Malformed protocol leaked payload");
            AppLog.close();
            I18n.setLanguage("ru");
            AppLog.initialize(own.resolve("профиль-日本語"));
            AppLog.flush();
            check(AppLog.snapshotText().contains("192.168.1.1") && !AppLog.snapshotText().contains("DNS over TCP succeeded:"), "Canonical event lost across restart");
        } finally {
            AppLog.close();
            I18n.setLanguage("en");
        }
        String report = Json.stringify(Map.of("status", "PASS", "checks", checks, "languages", 12,
                "user_logs_read", false, "network_calls", 0));
        Files.writeString(own.resolve("DNS-FALLBACK-LOG-TESTS.json"), report, StandardCharsets.UTF_8);
        System.out.println(report);
    }
}
