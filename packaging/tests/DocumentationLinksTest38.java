package multiroute;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Owned help-navigation fixture: injected browser capture, no user files or network. */
public final class DocumentationLinksTest38 {
    static int checks, computedUris;
    static final List<String> cases = new ArrayList<>(), capturedUris = new ArrayList<>(), images = new ArrayList<>();
    static final AtomicInteger networkCalls = new AtomicInteger(), browserCallsOnEdt = new AtomicInteger();
    static final List<String> languages = List.of("ru", "en", "de", "es", "fr");
    static final String[] keys = {
        "Инструкция", "Работа с API",
        "Открыть инструкцию на выбранном языке в браузере.",
        "Открыть примеры запросов и описание API на выбранном языке в браузере.",
        "Не удалось открыть инструкцию: %s\nСсылка: %s", "Открытие браузера недоступно"
    };
    static final String[][] translations = {
        {"Инструкция", "Instructions", "Anleitung", "Instrucciones", "Instructions"},
        {"Работа с API", "API guide", "API-Anleitung", "Guía de la API", "Guide API"},
        {"Открыть инструкцию на выбранном языке в браузере.", "Open the instructions in your browser in the selected language.",
            "Die Anleitung in der ausgewählten Sprache im Browser öffnen.", "Abrir las instrucciones en el navegador en el idioma seleccionado.",
            "Ouvrir les instructions dans le navigateur dans la langue sélectionnée."},
        {"Открыть примеры запросов и описание API на выбранном языке в браузере.",
            "Open API request examples and documentation in your browser in the selected language.",
            "API-Anfragebeispiele und Dokumentation in der ausgewählten Sprache im Browser öffnen.",
            "Abrir ejemplos de solicitudes y la documentación de la API en el navegador en el idioma seleccionado.",
            "Ouvrir les exemples de requêtes et la documentation de l’API dans le navigateur dans la langue sélectionnée."},
        {"Не удалось открыть инструкцию: %s\nСсылка: %s", "Could not open the instructions: %s\nLink: %s",
            "Die Anleitung konnte nicht geöffnet werden: %s\nLink: %s", "No se pudieron abrir las instrucciones: %s\nEnlace: %s",
            "Impossible d’ouvrir les instructions : %s\nLien : %s"},
        {"Открытие браузера недоступно", "Opening a browser is unavailable", "Das Öffnen eines Browsers ist nicht verfügbar",
            "No se puede abrir un navegador", "L’ouverture d’un navigateur est indisponible"}
    };
    interface Work { void run() throws Exception; }
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    static void rethrow(Throwable error) throws Exception {
        if (error instanceof Exception value) throw value;
        if (error instanceof Error value) throw value;
    }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable failure) { error.set(failure); } });
        rethrow(error.get());
    }
    static void drain() throws Exception { for (int i = 0; i < 5; i++) edt(() -> { }); }
    static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    static String expected(String code, int key) { return translations[key][languages.indexOf(code)]; }
    static String uri(String code, boolean api) {
        return "https://github.com/2018-spec-fdsds/multiroute-downloader/blob/v" + Main.VERSION + "/docs/" + code + ".md"
                + (api ? "#multiroute-api" : "");
    }
    static void verifyUri(String input, String selected, boolean api) {
        URI value = DesktopUI.documentationUri(input, api); computedUris++;
        check(uri(selected, api).equals(value.toString()), "Documentation URI is not pinned to the selected language and current version: " + input);
        check("https".equals(value.getScheme()) && "github.com".equals(value.getHost()), "Documentation left the intended HTTPS host");
        check(value.getRawQuery() == null && value.getUserInfo() == null, "Documentation URI contains unintended query or authority data");
        check(Objects.equals(value.getFragment(), api ? "multiroute-api" : null), "API link does not have its exact stable fragment");
    }
    static void pure(Path repo) throws Exception {
        for (String code : languages) {
            verifyUri(code, code, false); verifyUri(code, code, true);
            for (int key = 0; key < keys.length; key++) {
                String actual = key == 4 ? I18n.tFor(code, keys[key], "owned error", "owned link") : I18n.tFor(code, keys[key]);
                String wanted = key == 4 ? String.format(Locale.ROOT, expected(code, key), "owned error", "owned link") : expected(code, key);
                check(wanted.equals(actual), "Help text is not translated correctly: " + code + " / " + key);
            }
        }
        cases.add("five_languages_current_version_https_guides_and_exact_api_fragments");
        for (String input : Arrays.asList(null, "", "ja", "EN", " en", "../ru", "en?other", "en#other", "https://example.invalid/")) {
            verifyUri(input, "en", false); verifyUri(input, "en", true);
            for (int key = 0; key < keys.length; key++) {
                String actual = key == 4 ? I18n.tFor(input, keys[key], "owned error", "owned link") : I18n.tFor(input, keys[key]);
                String wanted = key == 4 ? String.format(Locale.ROOT, expected("en", key), "owned error", "owned link") : expected("en", key);
                check(wanted.equals(actual), "Unsupported help language does not fall back to English");
            }
        }
        cases.add("unsupported_language_inputs_fall_back_to_english_without_url_injection");
        if (repo != null) {
            for (String code : languages) {
                Path guide = repo.resolve("docs").resolve(code + ".md");
                check(Files.isRegularFile(guide), "Local language guide does not exist: " + code);
                String text = Files.readString(guide);
                check(text.contains("<a name=\"multiroute-api\"></a>"), "Language guide lacks the exact explicit API anchor: " + code);
                check(text.contains("http://127.0.0.1:8765/api/v1"), "Language API guide lacks the local API base address: " + code);
            }
            cases.add("five_owned_repository_guides_contain_api_anchor_and_local_endpoint");
        }
    }
    static void visibleButton(DesktopUI ui, JButton button) {
        Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), ui.frame.getContentPane());
        check(button.isVisible() && button.isEnabled(), "Help button is not available");
        check(button.getWidth() >= button.getPreferredSize().width && button.getHeight() >= button.getPreferredSize().height,
                "Translated help button text is clipped: " + button.getText());
        check(button.getVisibleRect().contains(new Rectangle(0, 0, button.getWidth(), button.getHeight())), "Help button is clipped by an ancestor");
        check(new Rectangle(0, 0, ui.frame.getContentPane().getWidth(), ui.frame.getContentPane().getHeight()).contains(bounds),
                "Help button extends outside the content frame");
        Rectangle language = SwingUtilities.convertRectangle(ui.languageCombo.getParent(), ui.languageCombo.getBounds(), ui.frame.getContentPane());
        check(bounds.y >= language.y + language.height, "Help buttons should be on their own row below the language selector");
    }
    static void captureHeader(DesktopUI ui, Path own, String language) throws Exception {
        JComponent header = (JComponent) ui.instructionsButton.getParent().getParent().getParent();
        BufferedImage image = new BufferedImage(header.getWidth(), header.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { header.printAll(graphics); } finally { graphics.dispose(); }
        Path output = own.resolve("OWN-HELP-HEADER-" + language + ".png");
        check(ImageIO.write(image, "png", output.toFile()), "Cannot save own help header preview");
        images.add(output.toString());
    }
    static void desktop(Path own, boolean visible) throws Exception {
        AppLog.initialize(own.resolve("owned-log"));
        Main.Manager manager = new Main.Manager(own.resolve("owned-profile"), (url, route) -> {
            networkCalls.incrementAndGet(); throw new AssertionError("No source connections in documentation fixture");
        });
        AtomicReference<DesktopUI> ref = new AtomicReference<>();
        BlockingQueue<URI> captures = new LinkedBlockingQueue<>();
        CountDownLatch closed = new CountDownLatch(1);
        try {
            edt(() -> {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
                Method fonts = DesktopUI.class.getDeclaredMethod("installReadableFonts"); fonts.setAccessible(true); fonts.invoke(null);
                DesktopUI ui = new DesktopUI(manager, closed::countDown, false,
                        (gateway,url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No gateway discovery"); },
                        (ip,gws) -> { networkCalls.incrementAndGet(); throw new AssertionError("No network configuration"); },
                        (route,url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No speed test"); },
                        null,
                        (url,route) -> { networkCalls.incrementAndGet(); throw new AssertionError("No Hub requests"); },
                        (owner,inspection) -> { throw new AssertionError("No Hub dialog"); },
                        (source,route) -> { networkCalls.incrementAndGet(); throw new AssertionError("No torrent requests"); },
                        (owner,inspection) -> { throw new AssertionError("No torrent dialog"); },
                        (owner,job) -> DesktopUI.DeleteChoice.CANCEL,
                        (owner,directory) -> null,
                        value -> { if (SwingUtilities.isEventDispatchThread()) browserCallsOnEdt.incrementAndGet(); captures.add(value); });
                ref.set(ui); ((javax.swing.Timer) field(ui, "refreshTimer")).stop();
                ui.frame.setSize(1280, 820);
                if (visible) ui.frame.setVisible(true); else ui.frame.addNotify();
                ui.frame.validate(); layout(ui.frame.getContentPane());
            }); drain();
            DesktopUI ui = ref.get();
            for (String code : languages) {
                edt(() -> ui.languageCombo.setSelectedItem(code)); drain();
                for (Dimension size : List.of(new Dimension(1080, 700), new Dimension(1280, 820))) {
                    edt(() -> { ui.frame.setSize(size); ui.frame.validate(); layout(ui.frame.getContentPane()); }); drain();
                    edt(() -> {
                        check(I18n.language().equals(code), "Actual language selector did not switch languages");
                        check(ui.instructionsButton.getText().equals(expected(code, 0)), "Instructions button label is stale");
                        check(ui.apiGuideButton.getText().equals(expected(code, 1)), "API guide button label is stale");
                        check(ui.instructionsButton.getToolTipText().contains(expected(code, 2)), "Instructions tooltip is not in selected language");
                        check(ui.apiGuideButton.getToolTipText().contains(expected(code, 3)), "API tooltip is not in selected language");
                        visibleButton(ui, ui.instructionsButton); visibleButton(ui, ui.apiGuideButton);
                        check(ui.frame.isShowing() == visible, "Owned fixture visibility does not match the test mode");
                    });
                }
                edt(() -> ui.instructionsButton.doClick(0));
                URI instructions = captures.poll(4, TimeUnit.SECONDS);
                check(URI.create(uri(code, false)).equals(instructions), "Actual Instructions click opened the wrong language/version");
                capturedUris.add(instructions.toString());
                edt(() -> ui.apiGuideButton.doClick(0));
                URI api = captures.poll(4, TimeUnit.SECONDS);
                check(URI.create(uri(code, true)).equals(api), "Actual API guide click lost the language or API fragment");
                capturedUris.add(api.toString());
                if (visible && List.of("en", "ru").contains(code)) edt(() -> captureHeader(ui, own, code));
            }
            check(captures.isEmpty() && capturedUris.size() == 10, "Help controls generated unexpected browser requests");
            check(browserCallsOnEdt.get() == 0, "Browser handoff blocks the Swing event thread");
            check(networkCalls.get() == 0, "Documentation fixture attempted network access");
            cases.add("actual_five_language_button_clicks_capture_ten_correct_async_browser_uris");
            cases.add("translated_labels_tooltips_and_fully_visible_buttons_at_1080x700_and_1280x820");
            if (visible) cases.add("own_visible_header_previews_printed_without_desktop_capture");
        } finally {
            try {
                if (ref.get() != null) {
                    edt(() -> ref.get().frame.dispose());
                    check(closed.await(6, TimeUnit.SECONDS), "Owned desktop workers did not close");
                }
            } finally { manager.shutdown(); AppLog.close(); }
        }
    }
    public static void main(String[] args) throws Exception {
        Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        String mode = args.length > 1 ? args[1] : "desktop";
        Path repo = args.length > 2 ? Path.of(args[2]).toAbsolutePath() : null;
        Throwable failure = null;
        try { pure(repo); if (!mode.equals("headless")) desktop(own, mode.equals("visible")); }
        catch (Throwable error) { failure = error; }
        Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("status", failure == null ? "PASS" : "FAIL"); report.put("checks", checks); report.put("cases", cases);
        report.put("version", Main.VERSION); report.put("jar_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        report.put("mode", mode); report.put("visible_mode", mode.equals("visible")); report.put("headless_only", mode.equals("headless"));
        report.put("computed_uri_count", computedUris); report.put("captured_uris", capturedUris); report.put("own_header_images", images);
        report.put("local_guide_anchors_checked", repo != null); report.put("network_calls", networkCalls.get());
        report.put("browser_launches", 0); report.put("real_browser_handoff_exercised", false);
        report.put("user_profile_or_logs_read", false); report.put("user_media_or_clipboard_read", false);
        if (failure != null) report.put("failure", failure.toString());
        Files.writeString(own.resolve("DOCUMENTATION-LINKS-TESTS.json"), Json.stringify(report));
        System.out.println(Json.stringify(report)); if (failure != null) failure.printStackTrace();
        System.exit(failure == null ? 0 : 1);
    }
}
