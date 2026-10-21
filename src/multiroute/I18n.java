package multiroute;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/** Desktop languages; bindings always retain the source template and raw arguments. */
public final class I18n {
    private static final List<String> CODES = List.of("ru", "en", "de", "es", "fr", "zh", "ko", "ar", "it", "ja", "hi", "vi");
    private static final String TEXT = "multiroute.i18n.text";
    private static final String TOOLTIP = "multiroute.i18n.tooltip";
    private static final String TITLE = "multiroute.i18n.title";
    private static final Map<String,String[]> ROWS = new LinkedHashMap<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    public static final String DEFAULT_LANGUAGE = "en";
    private static volatile String language = DEFAULT_LANGUAGE;
    private record Binding(String source, Object[] args) {
        Binding { args = args.clone(); }
        String render() { return t(source, args); }
    }

    static {
        catalog();
    }

    private I18n() { }
    public static List<String> codes() { return CODES; }
    public static String language() { return language; }
    public static Locale locale() { return Locale.forLanguageTag(language); }
    public static String displayName(String code) {
        return switch (code == null ? "" : code) {
            case "en" -> "English";
            case "de" -> "Deutsch";
            case "es" -> "Español";
            case "fr" -> "Français";
            case "ru" -> "Русский";
            case "zh" -> "简体中文";
            case "ko" -> "한국어";
            case "ar" -> "العربية";
            case "it" -> "Italiano";
            case "ja" -> "日本語";
            case "hi" -> "हिन्दी";
            case "vi" -> "Tiếng Việt";
            default -> "English";
        };
    }
    public static void setLanguage(String code) {
        String selected = code != null && CODES.contains(code) ? code : DEFAULT_LANGUAGE;
        Runnable change = () -> {
            boolean changed = !language.equals(selected);
            language = selected;
            Locale.setDefault(Locale.Category.DISPLAY, locale());
            JComponent.setDefaultLocale(locale());
            installSwingDefaults();
            if (changed) {
                for (Runnable listener : LISTENERS) listener.run();
                for (Window window : Window.getWindows()) {
                    if (!window.isDisplayable()) continue;
                    refresh(window);
                    window.invalidate();
                    window.validate();
                    window.repaint();
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) change.run();
        else {
            try { SwingUtilities.invokeAndWait(change); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            catch (java.lang.reflect.InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        }
    }
    public static void addLanguageListener(Runnable listener) { LISTENERS.add(listener); }
    public static void removeLanguageListener(Runnable listener) { LISTENERS.remove(listener); }
    static Map<String,String[]> sourceRows() { return Map.copyOf(ROWS); }
    public static String t(String source, Object... args) {
        return tFor(language, source, args);
    }
    /** Pure rendering for an async log event; it never changes Swing or the global locale. */
    public static String tFor(String code, String source, Object... args) {
        if (source == null) return "";
        String selected = code != null && CODES.contains(code) ? code : DEFAULT_LANGUAGE;
        String[] row = ROWS.get(source);
        String translated = row == null ? DiagnosticText.translation(selected, source) : row[CODES.indexOf(selected)];
        if (row != null) translated = Brand.display(translated);
        Object[] values = args.clone();
        for (int i = 0; i < values.length; i++)
            if (values[i] instanceof DiagnosticText.Arg value) values[i] = DiagnosticText.renderFor(selected, value.raw());
        return values.length == 0 ? translated : String.format(Locale.forLanguageTag(selected), translated, values);
    }
    /** Application diagnostics retain their canonical form until the presentation boundary. */
    public static String error(String source) { return DiagnosticText.render(source); }
    public static <T extends JComponent> T text(T component, String source, Object... args) {
        if (component instanceof JTextComponent field && field.isEditable())
            throw new IllegalArgumentException("Do not bind editable text; it may contain user data");
        Binding binding = new Binding(source, args);
        component.putClientProperty(TEXT, binding);
        applyText(component, binding.render());
        return component;
    }
    public static <T extends JComponent> T tooltip(T component, String source, Object... args) {
        if (source == null) {
            component.putClientProperty(TOOLTIP, null);
            component.setToolTipText(null);
        } else {
            Binding binding = new Binding(source, args);
            component.putClientProperty(TOOLTIP, binding);
            component.setToolTipText(binding.render());
        }
        return component;
    }
    public static void title(Window window, String source, Object... args) {
        if (!(window instanceof javax.swing.RootPaneContainer root)) return;
        Binding binding = new Binding(source, args);
        root.getRootPane().putClientProperty(TITLE, binding);
        applyTitle(window, binding.render());
    }
    public static void refresh(Component component) {
        component.setLocale(locale());
        // Custom bold headings/buttons must share the same script coverage as theme defaults.
        java.awt.Font font=component.getFont(),readable=DesktopTheme.readableFont(font);
        if(readable!=font)component.setFont(readable);
        // URLs, paths, IPs and checksums keep their logical order when the surrounding UI is Arabic.
        component.setComponentOrientation(component instanceof javax.swing.JTextField
                ? java.awt.ComponentOrientation.LEFT_TO_RIGHT : java.awt.ComponentOrientation.getOrientation(locale()));
        if (component instanceof JComponent swing) {
            if (swing.getClientProperty(TEXT) instanceof Binding binding) applyText(swing, binding.render());
            if (swing.getClientProperty(TOOLTIP) instanceof Binding binding) swing.setToolTipText(binding.render());
        }
        if (component instanceof Window window && window instanceof javax.swing.RootPaneContainer root
                && root.getRootPane().getClientProperty(TITLE) instanceof Binding binding)
            applyTitle(window, binding.render());
        if (component instanceof Container container)
            for (Component child : container.getComponents()) refresh(child);
    }
    private static void applyTitle(Window window, String value) {
        if (window instanceof JFrame frame) frame.setTitle(value);
        else if (window instanceof JDialog dialog) dialog.setTitle(value);
    }
    private static void applyText(JComponent component, String value) {
        if (component instanceof JLabel label) label.setText(value);
        else if (component instanceof AbstractButton button) button.setText(value);
        else if (component instanceof JProgressBar progress) progress.setString(value);
        else if (component instanceof JTextComponent text && !text.isEditable()) text.setText(value);
        else throw new IllegalArgumentException("Unsupported translatable component: " + component.getClass().getName());
    }
    public static void installSwingDefaults() {
        defaults("OptionPane.okButtonText", "ОК");
        defaults("OptionPane.yesButtonText", "Да");
        defaults("OptionPane.noButtonText", "Нет");
        defaults("OptionPane.cancelButtonText", "Отмена");
        defaults("OptionPane.titleText", "Сообщение");
        defaults("OptionPane.inputDialogTitle", "Ввод");
        defaults("OptionPane.messageDialogTitle", "Сообщение");
        defaults("FileChooser.openButtonText", "Открыть");
        defaults("FileChooser.saveButtonText", "Сохранить");
        defaults("FileChooser.cancelButtonText", "Отмена");
        defaults("FileChooser.updateButtonText", "Обновить");
        defaults("FileChooser.helpButtonText", "Помощь");
        defaults("FileChooser.directoryOpenButtonText", "Открыть");
        defaults("FileChooser.chooseButtonText", "Выбрать");
        defaults("FileChooser.openDialogTitleText", "Открыть");
        defaults("FileChooser.saveDialogTitleText", "Сохранить");
        defaults("FileChooser.fileNameLabelText", "Имя файла:");
        defaults("FileChooser.folderNameLabelText", "Имя папки:");
        defaults("FileChooser.filesOfTypeLabelText", "Тип файлов:");
        defaults("FileChooser.lookInLabelText", "Папка:");
        defaults("FileChooser.saveInLabelText", "Сохранить в:");
        defaults("FileChooser.acceptAllFileFilterText", "Все файлы");
        defaults("FileChooser.openButtonToolTipText", "Открыть выбранный файл");
        defaults("FileChooser.saveButtonToolTipText", "Сохранить выбранный файл");
        defaults("FileChooser.cancelButtonToolTipText", "Закрыть выбор файла");
        defaults("FileChooser.directoryOpenButtonToolTipText", "Открыть выбранную папку");
        defaults("FileChooser.updateButtonToolTipText", "Обновить список файлов");
        defaults("FileChooser.helpButtonToolTipText", "Помощь по выбору файла");
        defaults("FileChooser.upFolderToolTipText", "На одну папку выше");
        defaults("FileChooser.newFolderToolTipText", "Создать папку");
        defaults("FileChooser.listViewButtonToolTipText", "Список");
        defaults("FileChooser.detailsViewButtonToolTipText", "Подробности");
        defaults("FileChooser.homeFolderToolTipText", "Домашняя папка");
        defaults("FileChooser.viewMenuButtonToolTipText", "Вид");
        defaults("FileChooser.viewMenuButtonAccessibleName", "Вид");
        defaults("FileChooser.newFolderAccessibleName", "Создать папку");
        defaults("FileChooser.upFolderAccessibleName", "На одну папку выше");
        defaults("FileChooser.homeFolderAccessibleName", "Домашняя папка");
        defaults("FileChooser.fileNameHeaderText", "Имя");
        defaults("FileChooser.fileSizeHeaderText", "Размер");
        defaults("FileChooser.fileTypeHeaderText", "Тип");
        defaults("FileChooser.fileDateHeaderText", "Изменён");
        defaults("FileChooser.fileAttrHeaderText", "Атрибуты");
        defaults("FileChooser.viewMenuLabelText", "Вид");
        defaults("FileChooser.refreshActionLabelText", "Обновить");
        defaults("FileChooser.newFolderActionLabelText", "Создать папку");
        defaults("FileChooser.listViewActionLabelText", "Список");
        defaults("FileChooser.detailsViewActionLabelText", "Подробности");
        defaults("FileChooser.newFolderDialogText", "Имя новой папки:");
        defaults("FileChooser.newFolderErrorText", "Не удалось создать папку");
        defaults("FileChooser.newFolderErrorSeparator", ": ");
        defaults("FileChooser.newFolderParentDoesntExistTitleText", "Не удалось создать папку");
        defaults("FileChooser.newFolderParentDoesntExistText", "Родительская папка не существует");
        defaults("FileChooser.renameErrorTitleText", "Не удалось переименовать файл");
        defaults("FileChooser.renameErrorText", "Не удалось переименовать {0}");
        defaults("FileChooser.renameErrorFileExistsText", "Файл с именем {0} уже существует");
        defaults("FileChooser.fileDescriptionText", "Файл");
        defaults("FileChooser.directoryDescriptionText", "Папка");
        defaults("FileChooser.newFolderDefaultName", "Новая папка");
        defaults("FileChooser.other.newFolder", "Новая папка");
        defaults("FileChooser.other.newFolder.subsequent", "Новая папка.{0}");
        defaults("FileChooser.win32.newFolder", "Новая папка");
        defaults("FileChooser.win32.newFolder.subsequent", "Новая папка ({0})");
        defaults("FileChooser.openButtonMnemonic", "0");
        defaults("FileChooser.saveButtonMnemonic", "0");
        defaults("FileChooser.cancelButtonMnemonic", "0");
    }
    private static void defaults(String key, String source) { UIManager.put(key, t(source)); }
    private static void row(String ru, String en, String de, String es, String fr) {
        if (ROWS.putIfAbsent(ru, LocalizedCatalog.extend(ru, new String[]{ru, en, de, es, fr})) != null)
            throw new IllegalStateException("Duplicate localization key: " + ru);
    }
    private static void row(String source, String ru, String en, String de, String es, String fr) {
        if (ROWS.putIfAbsent(source, LocalizedCatalog.extend(source, new String[]{ru, en, de, es, fr})) != null)
            throw new IllegalStateException("Duplicate localization key: " + source);
    }
    private static void catalog() {
        row("DNS over TCP succeeded: gateway=%s resolver=%s", "DNS через TCP: запрос выполнен; шлюз=%s DNS-сервер=%s", "DNS over TCP succeeded: gateway=%s resolver=%s", "DNS über TCP erfolgreich: Gateway=%s Resolver=%s", "DNS por TCP completado: puerta de enlace=%s servidor DNS=%s", "DNS sur TCP réussi : passerelle=%s résolveur=%s");
        row("DNS over TCP failed: gateway=%s resolver=%s", "DNS через TCP: запрос не удался; шлюз=%s DNS-сервер=%s", "DNS over TCP failed: gateway=%s resolver=%s", "DNS über TCP fehlgeschlagen: Gateway=%s Resolver=%s", "Falló DNS por TCP: puerta de enlace=%s servidor DNS=%s", "Échec de DNS sur TCP : passerelle=%s résolveur=%s");
        row("Докачка через выбранные шлюзы · MD5, SHA-256, SHA-512", "Resumable downloads through selected gateways · MD5, SHA-256, SHA-512", "Fortsetzbare Downloads über ausgewählte Gateways · MD5, SHA-256, SHA-512", "Descargas reanudables por las puertas de enlace seleccionadas · MD5, SHA-256, SHA-512", "Téléchargements reprenables via les passerelles sélectionnées · MD5, SHA-256, SHA-512");
        row("Ожидаемая контрольная сумма (необязательно)", "Expected checksum (optional)", "Erwartete Prüfsumme (optional)", "Suma esperada (opcional)", "Somme attendue (facultatif)");
        row("Тип определяется автоматически: MD5 — 32, SHA-256 — 64, SHA-512 — 128 шестнадцатеричных символов.", "Type detected automatically: MD5 = 32, SHA-256 = 64, SHA-512 = 128 hexadecimal characters.", "Typ automatisch erkannt: MD5 = 32, SHA-256 = 64, SHA-512 = 128 hexadezimale Zeichen.", "Tipo detectado automáticamente: MD5 = 32, SHA-256 = 64, SHA-512 = 128 caracteres hexadecimales.", "Type détecté automatiquement : MD5 = 32, SHA-256 = 64, SHA-512 = 128 caractères hexadécimaux.");
        row("Показать дополнительные эталоны", "Show additional references", "Zusätzliche Referenzwerte anzeigen", "Mostrar referencias adicionales", "Afficher les références supplémentaires");
        row("Вставьте контрольную сумму из доверенного источника: MD5 — 32, SHA-256 — 64 или SHA-512 — 128 шестнадцатеричных символов. Тип определяется автоматически. Ctrl+V вставляет сумму; на macOS также Cmd+V. Эталоны источника также ищутся автоматически.", "Paste a checksum from a trusted source: MD5 = 32, SHA-256 = 64 or SHA-512 = 128 hexadecimal characters. The type is detected automatically. Paste with Ctrl+V, or Cmd+V on macOS. Source references are also discovered automatically.", "Prüfsumme aus einer vertrauenswürdigen Quelle einfügen: MD5 = 32, SHA-256 = 64 oder SHA-512 = 128 hexadezimale Zeichen. Der Typ wird automatisch erkannt. Mit Strg+V oder auf macOS auch Cmd+V einfügen. Referenzwerte der Quelle werden ebenfalls automatisch gesucht.", "Pegue una suma de una fuente de confianza: MD5 = 32, SHA-256 = 64 o SHA-512 = 128 caracteres hexadecimales. El tipo se detecta automáticamente. Pegue con Ctrl+V, o Cmd+V en macOS. Las referencias del origen también se buscan automáticamente.", "Collez une somme d’une source fiable : MD5 = 32, SHA-256 = 64 ou SHA-512 = 128 caractères hexadécimaux. Le type est détecté automatiquement. Collez avec Ctrl+V, ou Cmd+V sur macOS. Les références de la source sont aussi recherchées automatiquement.");
        row("Показать поля для дополнительных эталонов MD5 и SHA-512. Введённые значения используются и после сворачивания раздела; очистите поле, чтобы убрать эталон. Все указанные эталоны должны совпасть с файлом.", "Show fields for additional MD5 and SHA-512 references. Entered values remain in use when this section is collapsed; clear a field to remove its reference. The file must match every supplied reference.", "Felder für zusätzliche MD5- und SHA-512-Referenzwerte anzeigen. Eingetragene Werte bleiben beim Zuklappen aktiv; zum Entfernen das Feld leeren. Die Datei muss allen angegebenen Referenzwerten entsprechen.", "Mostrar campos de referencias MD5 y SHA-512 adicionales. Los valores siguen utilizándose al contraer la sección; borre un campo para quitar su referencia. El archivo debe coincidir con todas las referencias indicadas.", "Afficher les champs de références MD5 et SHA-512 supplémentaires. Les valeurs restent utilisées lorsque la section est réduite ; effacez un champ pour retirer sa référence. Le fichier doit correspondre à toutes les références indiquées.");
        row("Контрольная сумма должна содержать 32, 64 или 128 шестнадцатеричных символов: MD5, SHA-256 или SHA-512.", "A checksum must contain 32, 64 or 128 hexadecimal characters: MD5, SHA-256 or SHA-512.", "Eine Prüfsumme muss 32, 64 oder 128 hexadezimale Zeichen enthalten: MD5, SHA-256 oder SHA-512.", "Una suma debe contener 32, 64 o 128 caracteres hexadecimales: MD5, SHA-256 o SHA-512.", "Une somme doit contenir 32, 64 ou 128 caractères hexadécimaux : MD5, SHA-256 ou SHA-512.");
        row("Файл: MD5, SHA-256, SHA-512 по доступным эталонам. Части BitTorrent проверяются отдельно и автоматически: v1 — SHA-1, v2 — SHA-256 Merkle из метаданных раздачи.", "File: MD5, SHA-256 and SHA-512 against available references. BitTorrent pieces are checked separately and automatically: v1 uses SHA-1, v2 uses SHA-256 Merkle from torrent metadata.", "Datei: MD5, SHA-256 und SHA-512 anhand verfügbarer Referenzwerte. BitTorrent-Teile werden separat und automatisch geprüft: v1 mit SHA-1, v2 mit SHA-256-Merkle aus Torrent-Metadaten.", "Archivo: MD5, SHA-256 y SHA-512 con las referencias disponibles. Las partes BitTorrent se verifican aparte y automáticamente: v1 usa SHA-1; v2 usa SHA-256 Merkle de los metadatos del torrent.", "Fichier : MD5, SHA-256 et SHA-512 selon les références disponibles. Les pièces BitTorrent sont vérifiées séparément et automatiquement : v1 utilise SHA-1, v2 utilise SHA-256 Merkle des métadonnées du torrent.");
        row("Докачка через выбранные шлюзы с проверкой контрольных сумм", "Resumable downloads through selected gateways with checksum verification", "Fortsetzbare Downloads über ausgewählte Gateways mit Prüfsummenprüfung", "Descargas reanudables por las puertas de enlace seleccionadas con verificación de sumas", "Téléchargements reprenables via les passerelles sélectionnées avec vérification des sommes");
        row("Ожидаемая MD5 (необязательно)", "Expected MD5 (optional)", "Erwarteter MD5-Wert (optional)", "MD5 esperado (opcional)", "MD5 attendu (facultatif)");
        row("Ожидаемая SHA-512 (необязательно)", "Expected SHA-512 (optional)", "Erwarteter SHA-512-Wert (optional)", "SHA-512 esperado (opcional)", "SHA-512 attendu (facultatif)");
        row("MD5, SHA-256 и SHA-512: сверка с доступными эталонами источника. Torrent: обязательная отдельная проверка частей.", "MD5, SHA-256 and SHA-512: compare with available source references. Torrent: separate mandatory piece verification.", "MD5, SHA-256 und SHA-512: mit verfügbaren Referenzwerten der Quelle vergleichen. Torrent: separate verpflichtende Prüfung der Teile.", "MD5, SHA-256 y SHA-512: comparar con las referencias disponibles del origen. Torrent: verificación independiente y obligatoria de las partes.", "MD5, SHA-256 et SHA-512 : comparer aux références disponibles de la source. Torrent : vérification séparée et obligatoire des pièces.");
        row("Необязательная MD5 из доверенного источника: 32 шестнадцатеричных символа. Ctrl+V вставляет сумму; на macOS также Cmd+V. Все указанные эталоны должны совпасть с файлом.", "Optional MD5 from a trusted source: 32 hexadecimal characters. Paste with Ctrl+V, or Cmd+V on macOS. The file must match every supplied reference.", "Optionale MD5 aus einer vertrauenswürdigen Quelle: 32 hexadezimale Zeichen. Mit Strg+V oder auf macOS auch Cmd+V einfügen. Die Datei muss allen angegebenen Referenzwerten entsprechen.", "MD5 opcional de una fuente de confianza: 32 caracteres hexadecimales. Pegue con Ctrl+V, o Cmd+V en macOS. El archivo debe coincidir con todas las referencias indicadas.", "MD5 facultatif d’une source fiable : 32 caractères hexadécimaux. Collez avec Ctrl+V, ou Cmd+V sur macOS. Le fichier doit correspondre à toutes les références indiquées.");
        row("Необязательная SHA-512 из доверенного источника: 128 шестнадцатеричных символов. Ctrl+V вставляет сумму; на macOS также Cmd+V. Все указанные эталоны должны совпасть с файлом.", "Optional SHA-512 from a trusted source: 128 hexadecimal characters. Paste with Ctrl+V, or Cmd+V on macOS. The file must match every supplied reference.", "Optionaler SHA-512-Wert aus einer vertrauenswürdigen Quelle: 128 hexadezimale Zeichen. Mit Strg+V oder auf macOS auch Cmd+V einfügen. Die Datei muss allen angegebenen Referenzwerten entsprechen.", "SHA-512 opcional de una fuente de confianza: 128 caracteres hexadecimales. Pegue con Ctrl+V, o Cmd+V en macOS. El archivo debe coincidir con todas las referencias indicadas.", "SHA-512 facultatif d’une source fiable : 128 caractères hexadécimaux. Collez avec Ctrl+V, ou Cmd+V sur macOS. Le fichier doit correspondre à toutes les références indiquées.");
        row("Ожидаемая MD5 должна содержать 32 шестнадцатеричных символа", "Expected MD5 must contain 32 hexadecimal characters", "Der erwartete MD5-Wert muss 32 hexadezimale Zeichen enthalten", "El MD5 esperado debe contener 32 caracteres hexadecimales", "Le MD5 attendu doit contenir 32 caractères hexadécimaux");
        row("Ожидаемая SHA-512 должна содержать 128 шестнадцатеричных символов", "Expected SHA-512 must contain 128 hexadecimal characters", "Der erwartete SHA-512-Wert muss 128 hexadezimale Zeichen enthalten", "El SHA-512 esperado debe contener 128 caracteres hexadecimales", "Le SHA-512 attendu doit contenir 128 caractères hexadécimaux");
        row("Ожидаемые контрольные суммы относятся к одному файлу. Выберите один файл или очистите поля MD5, SHA-256 и SHA-512 для групповой загрузки.", "Expected checksums apply to one file. Select one file, or clear MD5, SHA-256 and SHA-512 for a batch download.", "Erwartete Prüfsummen gelten für eine Datei. Eine Datei auswählen oder MD5, SHA-256 und SHA-512 für einen Sammeldownload leeren.", "Las sumas esperadas corresponden a un archivo. Seleccione un archivo o borre MD5, SHA-256 y SHA-512 para una descarga por lotes.", "Les sommes attendues s’appliquent à un fichier. Sélectionnez un fichier ou effacez MD5, SHA-256 et SHA-512 pour un téléchargement groupé.");
        row("\nПРОВЕРКА %s\n", "\n%s VERIFICATION\n", "\n%s-PRÜFUNG\n", "\nVERIFICACIÓN %s\n", "\nVÉRIFICATION %s\n");
        row("ожидается расчёт", "calculation pending", "Berechnung ausstehend", "cálculo pendiente", "calcul en attente");
        row("отсутствует", "none", "nicht vorhanden", "ninguna", "aucune");
        row("не указан", "not specified", "nicht angegeben", "no indicado", "non précisée");
        row("ожидается расчёт; эталона нет", "calculation pending; no reference", "Berechnung ausstehend; kein Referenzwert", "cálculo pendiente; sin referencia", "calcul en attente ; aucune référence");
        row("локальная сумма; эталона нет", "local checksum only; no reference", "nur lokale Prüfsumme; kein Referenzwert", "solo suma local; sin referencia", "somme locale uniquement ; aucune référence");
        row("НЕ СОВПАДАЕТ ✗", "DOES NOT MATCH ✗", "STIMMT NICHT ÜBEREIN ✗", "NO COINCIDE ✗", "NE CORRESPOND PAS ✗");
        row("\nКОНТРОЛЬНЫЕ СУММЫ ФАЙЛОВ\n", "\nFILE CHECKSUMS\n", "\nDATEIPRÜFSUMMEN\n", "\nSUMAS DE LOS ARCHIVOS\n", "\nSOMMES DES FICHIERS\n");
        row("Поиск эталона: ", "Reference lookup: ", "Referenzsuche: ", "Búsqueda de referencia: ", "Recherche de référence : ");
        row("Открыть приложение", "Open application", "Anwendung öffnen", "Abrir aplicación", "Ouvrir l’application");
        row("Выйти из приложения", "Exit application", "Anwendung beenden", "Salir de la aplicación", "Quitter l’application");
        row("Все загрузки: %.2f Мбит/с (%d Б/с)", "All downloads: %.2f Mbit/s (%d B/s)", "Alle Downloads: %.2f Mbit/s (%d B/s)", "Todas las descargas: %.2f Mbit/s (%d B/s)", "Tous les téléchargements : %.2f Mbit/s (%d B/s)");
        row("Все загрузки: %.2f Гбит/с (%d Б/с)", "All downloads: %.2f Gbit/s (%d B/s)", "Alle Downloads: %.2f Gbit/s (%d B/s)", "Todas las descargas: %.2f Gbit/s (%d B/s)", "Tous les téléchargements : %.2f Gbit/s (%d B/s)");
        row("%.1f Гбит/с", "%.1f Gbit/s", "%.1f Gbit/s", "%.1f Gbit/s", "%.1f Gbit/s");
        row("Завершить приложение и сохранить загрузки для продолжения. Крестик окна сворачивает приложение в трей.", "Exit the application and save downloads for resuming. The window close button minimizes to the system tray.", "Anwendung beenden und Downloads zum Fortsetzen speichern. Die Schließen-Schaltfläche minimiert ins Systemtray.", "Salir de la aplicación y guardar las descargas para reanudarlas. El botón de cierre minimiza a la bandeja del sistema.", "Quitter l’application et enregistrer les téléchargements pour les reprendre. Le bouton de fermeture réduit l’application dans la zone de notification.");
        row("Системный трей недоступен. Окно свёрнуто, загрузки продолжаются. Для выхода нажмите «Выйти из приложения».", "The system tray is unavailable. The window is minimized; downloads continue. Use Exit application to quit.", "Das Systemtray ist nicht verfügbar. Das Fenster ist minimiert; Downloads laufen weiter. Zum Beenden Anwendung beenden wählen.", "La bandeja del sistema no está disponible. La ventana está minimizada; las descargas continúan. Use Salir de la aplicación para cerrar.", "La zone de notification est indisponible. La fenêtre est réduite ; les téléchargements continuent. Utilisez Quitter l’application pour fermer.");
        row("Файл", "File", "Datei", "Archivo", "Fichier");
        row("Состояние", "Status", "Status", "Estado", "État");
        row("Прогресс", "Progress", "Fortschritt", "Progreso", "Progression");
        row("Загружено: %s из %s · %s", "Downloaded: %s of %s · %s", "Heruntergeladen: %s von %s · %s", "Descargado: %s de %s · %s", "Téléchargé : %s sur %s · %s");
        row("Получено: %s · размер неизвестен · %s", "Received: %s · size unknown · %s", "Empfangen: %s · Größe unbekannt · %s", "Recibido: %s · tamaño desconocido · %s", "Reçu : %s · taille inconnue · %s");
        row("Получено: %s · размер неизвестен", "Received: %s · size unknown", "Empfangen: %s · Größe unbekannt", "Recibido: %s · tamaño desconocido", "Reçu : %s · taille inconnue");
        row("Прогресс каждой загрузки. Если размер неизвестен, показан объём полученных данных. Подробности доступны при выборе строки.", "Progress for each download. If the size is unknown, received bytes are shown. Select a row for details.", "Fortschritt jedes Downloads. Bei unbekannter Größe werden die empfangenen Daten angezeigt. Eine Zeile für Details auswählen.", "Progreso de cada descarga. Si se desconoce el tamaño, se muestran los datos recibidos. Seleccione una fila para ver los detalles.", "Progression de chaque téléchargement. Si la taille est inconnue, le volume reçu est affiché. Sélectionnez une ligne pour les détails.");
        row("Готово", "Complete", "Abgeschlossen", "Completado", "Terminé");
        row("Скорость", "Speed", "Geschwindigkeit", "Velocidad", "Vitesse");
        row("Прошло", "Elapsed", "Vergangen", "Transcurrido", "Écoulé");
        row("Осталось", "Remaining", "Verbleibend", "Restante", "Restant");
        row("Шлюзы", "Gateways", "Gateways", "Puertas de enlace", "Passerelles");
        row("Проверить и применить", "Check and apply", "Prüfen und anwenden", "Comprobar y aplicar", "Vérifier et appliquer");
        row("Адрес ещё не проверен", "Address not checked yet", "Adresse noch nicht geprüft", "Dirección aún sin comprobar", "Adresse pas encore vérifiée");
        row("Вставить URL", "Paste URL", "URL einfügen", "Pegar URL", "Coller l’URL");
        row("Открыть torrent…", "Open torrent…", "Torrent öffnen…", "Abrir torrent…", "Ouvrir un torrent…");
        row("Обзор…", "Browse…", "Durchsuchen…", "Examinar…", "Parcourir…");
        row("Начать загрузку", "Start download", "Download starten", "Iniciar descarga", "Démarrer le téléchargement");
        row("Выбрать все шлюзы", "Select all gateways", "Alle Gateways auswählen", "Seleccionar todas las puertas de enlace", "Sélectionner toutes les passerelles");
        row("Снять выбор", "Clear selection", "Auswahl aufheben", "Quitar selección", "Effacer la sélection");
        row("Проверить шлюзы", "Check gateways", "Gateways prüfen", "Comprobar puertas de enlace", "Vérifier les passerelles");
        row("Проверить скорости", "Test speeds", "Geschwindigkeiten testen", "Probar velocidades", "Tester les vitesses");
        row("Пауза", "Pause", "Pause", "Pausa", "Pause");
        row("Продолжить", "Resume", "Fortsetzen", "Reanudar", "Reprendre");
        row("Изменить папку…", "Change folder…", "Ordner ändern…", "Cambiar carpeta…", "Changer de dossier…");
        row("Удалить задание", "Remove download", "Download entfernen", "Eliminar descarga", "Supprimer le téléchargement");
        row("Готово к работе", "Ready", "Bereit", "Listo", "Prêt");
        row("Выберите загрузку", "Select a download", "Download auswählen", "Seleccione una descarga", "Sélectionnez un téléchargement");
        row("Общая скорость: —", "Total speed: —", "Gesamtgeschwindigkeit: —", "Velocidad total: —", "Vitesse totale : —");
        row("Прошло: —", "Elapsed: —", "Vergangen: —", "Transcurrido: —", "Écoulé : —");
        row("Осталось: —", "Remaining: —", "Verbleibend: —", "Restante: —", "Restant : —");
        row("Докачка через выбранные шлюзы с проверкой SHA-256", "Resumable downloads through selected gateways with SHA-256 verification", "Fortsetzbare Downloads über ausgewählte Gateways mit SHA-256-Prüfung", "Descargas reanudables por las puertas de enlace seleccionadas con verificación SHA-256", "Téléchargements reprenables via les passerelles sélectionnées avec vérification SHA-256");
        row("Локальное приложение  •  Java 17", "Desktop application  •  Java 17", "Desktop-Anwendung  •  Java 17", "Aplicación de escritorio  •  Java 17", "Application de bureau  •  Java 17");
        row("Ссылка HTTP/HTTPS, magnet или torrent", "HTTP/HTTPS, magnet or torrent link", "HTTP-/HTTPS-, Magnet- oder Torrent-Link", "Enlace HTTP/HTTPS, magnet o torrent", "Lien HTTP/HTTPS, magnet ou torrent");
        row("Вставьте полную ссылку; длина не ограничена 128 символами", "Paste the full link; it is not limited to 128 characters", "Vollständigen Link einfügen; keine Begrenzung auf 128 Zeichen", "Pegue el enlace completo; no está limitado a 128 caracteres", "Collez le lien complet ; il n’est pas limité à 128 caractères");
        row("Новая загрузка", "New download", "Neuer Download", "Nueva descarga", "Nouveau téléchargement");
        row("Адрес файла и место сохранения", "File URL and save location", "Dateiadresse und Speicherort", "Dirección del archivo y ubicación de guardado", "Adresse du fichier et emplacement d’enregistrement");
        row("   или вставьте magnet-ссылку", "   or paste a magnet link", "   oder Magnet-Link einfügen", "   o pegue un enlace magnet", "   ou collez un lien magnet");
        row("Имя файла HTTP (torrent: имена из раздачи)", "HTTP file name (torrent: original names)", "HTTP-Dateiname (Torrent: Originalnamen)", "Nombre de archivo HTTP (torrent: nombres originales)", "Nom du fichier HTTP (torrent : noms d’origine)");
        row("Папка для файла", "Download folder", "Download-Ordner", "Carpeta de descarga", "Dossier de téléchargement");
        row("Ожидаемая SHA-256 (необязательно)", "Expected SHA-256 (optional)", "Erwarteter SHA-256-Wert (optional)", "SHA-256 esperado (opcional)", "SHA-256 attendu (facultatif)");
        row("HTTP: поиск SHA-256 у источника. Torrent: проверка частей по метаданным раздачи.", "HTTP: discover SHA-256 at the source. Torrent: verify pieces against torrent metadata.", "HTTP: SHA-256 beim Anbieter suchen. Torrent: Teile anhand der Torrent-Metadaten prüfen.", "HTTP: buscar SHA-256 en el origen. Torrent: verificar partes con los metadatos del torrent.", "HTTP : rechercher le SHA-256 auprès de la source. Torrent : vérifier les pièces avec les métadonnées du torrent.");
        row("Сегмент:  ", "Segment:  ", "Segment:  ", "Segmento:  ", "Segment :  ");
        row("  МиБ  ·  от 1 до 64", "  MiB  ·  1 to 64", "  MiB  ·  1 bis 64", "  MiB  ·  de 1 a 64", "  Mio  ·  de 1 à 64");
        row("Настройки сети", "Network settings", "Netzwerkeinstellungen", "Configuración de red", "Paramètres réseau");
        row("Виртуальный адрес и ваши шлюзы", "Virtual address and your gateways", "Virtuelle Adresse und Ihre Gateways", "Dirección virtual y sus puertas de enlace", "Adresse virtuelle et vos passerelles");
        row("Виртуальный IP приложения", "Application virtual IP", "Virtuelle IP der Anwendung", "IP virtual de la aplicación", "IP virtuelle de l’application");
        row("Виртуальный адрес приложения; должен быть свободен в локальной сети.", "The application’s virtual address must be unused on the local network.", "Die virtuelle Adresse der Anwendung muss im lokalen Netzwerk frei sein.", "La dirección virtual de la aplicación debe estar libre en la red local.", "L’adresse virtuelle de l’application doit être libre sur le réseau local.");
        row("Адреса можно менять. Приложение проверит соединение через каждый шлюз.", "You can edit the addresses. The application checks the connection through each gateway.", "Sie können die Adressen ändern. Die Anwendung prüft die Verbindung über jedes Gateway.", "Puede cambiar las direcciones. La aplicación comprobará la conexión por cada puerta de enlace.", "Vous pouvez modifier les adresses. L’application vérifiera la connexion via chaque passerelle.");
        row("Только тест скорости: до 16 МиБ или 5 секунд.\nЗагрузка файлов этими пределами не ограничена.\nБез прямой HTTP-ссылки — тестовый файл Selectel.", "Speed test only: up to 16 MiB or 5 seconds.\nFile downloads are not limited by these values.\nWithout a direct HTTP link, a Selectel test file is used.", "Nur beim Geschwindigkeitstest: bis zu 16 MiB oder 5 Sekunden.\nDiese Grenzen gelten nicht für Datei-Downloads.\nOhne direkten HTTP-Link wird eine Selectel-Testdatei verwendet.", "Solo la prueba de velocidad: hasta 16 MiB o 5 segundos.\nEstas cifras no limitan las descargas de archivos.\nSin enlace HTTP directo, se usa un archivo de prueba de Selectel.", "Test de vitesse uniquement : jusqu’à 16 Mio ou 5 secondes.\nCes limites ne s’appliquent pas aux téléchargements.\nSans lien HTTP direct, un fichier de test Selectel est utilisé.");
        row("Перед загрузкой приложение проверит соединение через каждый выбранный шлюз.", "Before downloading, the application checks the connection through each selected gateway.", "Vor dem Download prüft die Anwendung die Verbindung über jedes ausgewählte Gateway.", "Antes de descargar, la aplicación comprobará la conexión por cada puerta de enlace seleccionada.", "Avant le téléchargement, l’application vérifiera la connexion via chaque passerelle sélectionnée.");
        row("Загрузки", "Downloads", "Downloads", "Descargas", "Téléchargements");
        row("Нет выбранной загрузки", "No download selected", "Kein Download ausgewählt", "Ninguna descarga seleccionada", "Aucun téléchargement sélectionné");
        row("После запуска здесь появятся сведения о шлюзах и проверке файла.", "After starting, gateway and file verification details will appear here.", "Nach dem Start erscheinen hier Informationen zu den Gateways und zur Dateiprüfung.", "Tras iniciar, aquí aparecerán los datos de las puertas de enlace y de la verificación del archivo.", "Après le démarrage, les informations sur les passerelles et la vérification du fichier s’afficheront ici.");
        row("Буфер обмена пуст", "Clipboard is empty", "Die Zwischenablage ist leer", "El portapapeles está vacío", "Le presse-papiers est vide");
        row("Выберите папку для загрузки", "Select a download folder", "Download-Ordner auswählen", "Seleccione una carpeta de descarga", "Sélectionnez un dossier de téléchargement");
        row("Удалить также файлы и скачанные части", "Also delete files and downloaded parts", "Auch Dateien und heruntergeladene Teile löschen", "Eliminar también archivos y partes descargadas", "Supprimer aussi les fichiers et les parties téléchargées");
        row("Удаление загрузки", "Remove download", "Download entfernen", "Eliminar descarga", "Supprimer le téléchargement");
        row("Выберите новую папку для загрузки", "Select a new download folder", "Neuen Download-Ordner auswählen", "Seleccione una nueva carpeta de descarga", "Sélectionnez un nouveau dossier de téléchargement");
        row("Откройте файл torrent", "Open a torrent file", "Torrent-Datei öffnen", "Abra un archivo torrent", "Ouvrez un fichier torrent");
        row("Раздача BitTorrent (*.torrent)", "BitTorrent file (*.torrent)", "BitTorrent-Datei (*.torrent)", "Archivo BitTorrent (*.torrent)", "Fichier BitTorrent (*.torrent)");
        row("Файл torrent выбран; отметьте шлюзы и нажмите «Начать загрузку»", "Torrent file selected; select gateways and click “Start download”", "Torrent-Datei ausgewählt; Gateways wählen und „Download starten“ anklicken", "Archivo torrent seleccionado; marque las puertas de enlace y pulse «Iniciar descarga»", "Fichier torrent sélectionné ; choisissez les passerelles et cliquez sur « Démarrer le téléchargement »");
        row("Недоступен", "Unavailable", "Nicht verfügbar", "No disponible", "Indisponible");
        row("Маршрут ещё не проверен", "Route not checked yet", "Route noch nicht geprüft", "Ruta aún sin comprobar", "Route pas encore vérifiée");
        row("Тест скорости", "Speed test", "Geschwindigkeitstest", "Prueba de velocidad", "Test de vitesse");
        row("Скорость: —", "Speed: —", "Geschwindigkeit: —", "Velocidad: —", "Vitesse : —");
        row("IPv4 адрес вашего шлюза", "Your gateway’s IPv4 address", "IPv4-Adresse Ihres Gateways", "Dirección IPv4 de su puerta de enlace", "Adresse IPv4 de votre passerelle");
        row("Шлюз", "Gateway", "Gateway", "Puerta de enlace", "Passerelle");
        row("Шлюз не найден или ещё не проверен", "Gateway not found or not checked yet", "Gateway nicht gefunden oder noch nicht geprüft", "Puerta de enlace no encontrada o aún sin comprobar", "Passerelle introuvable ou pas encore vérifiée");
        row("ОК", "OK", "OK", "Aceptar", "OK");
        row("Некорректный IP", "Invalid IP", "Ungültige IP", "IP no válida", "IP non valide");
        row("Повтор IP", "Duplicate IP", "Doppelte IP", "IP duplicada", "IP en double");
        row("Настройки сети приложения обновлены через API", "Application network settings updated through the API", "Netzwerkeinstellungen der Anwendung über die API aktualisiert", "Configuración de red de la aplicación actualizada mediante la API", "Paramètres réseau de l’application mis à jour via l’API");
        row("Проверка нужна", "Check required", "Prüfung erforderlich", "Comprobación necesaria", "Vérification nécessaire");
        row("Настройки сети обновлены через API; соединение нужно проверить повторно", "Network settings updated through the API; check the connection again", "Netzwerkeinstellungen über die API aktualisiert; Verbindung erneut prüfen", "Configuración de red actualizada mediante la API; vuelva a comprobar la conexión", "Paramètres réseau mis à jour via l’API ; vérifiez de nouveau la connexion");
        row("Укажите корректный виртуальный IPv4 адрес приложения", "Enter a valid virtual IPv4 address for the application", "Gültige virtuelle IPv4-Adresse für die Anwendung eingeben", "Introduzca una dirección IPv4 virtual válida para la aplicación", "Saisissez une adresse IPv4 virtuelle valide pour l’application");
        row("Виртуальный IP должен отличаться от адресов шлюзов", "The virtual IP must differ from the gateway addresses", "Die virtuelle IP muss sich von den Gateway-Adressen unterscheiden", "La IP virtual debe ser distinta de las direcciones de las puertas de enlace", "L’IP virtuelle doit être différente des adresses des passerelles");
        row("Настройка сети прервана", "Network configuration interrupted", "Netzwerkkonfiguration unterbrochen", "Configuración de red interrumpida", "Configuration réseau interrompue");
        row("Не удалось проверить настройки сети", "Could not check network settings", "Netzwerkeinstellungen konnten nicht geprüft werden", "No se pudo comprobar la configuración de red", "Impossible de vérifier les paramètres réseau");
        row("Адреса изменились; повторите действие после настройки сети", "Addresses changed; try again after configuring the network", "Adressen geändert; nach der Netzwerkkonfiguration erneut versuchen", "Las direcciones han cambiado; repita la acción tras configurar la red", "Les adresses ont changé ; réessayez après avoir configuré le réseau");
        row("Адрес изменён; нажмите «Проверить и применить»", "Address changed; click “Check and apply”", "Adresse geändert; „Prüfen und anwenden“ anklicken", "Dirección modificada; pulse «Comprobar y aplicar»", "Adresse modifiée ; cliquez sur « Vérifier et appliquer »");
        row("Виртуальный адрес изменён; примените настройки сети приложения", "Virtual address changed; apply the application network settings", "Virtuelle Adresse geändert; Netzwerkeinstellungen der Anwendung anwenden", "Dirección virtual modificada; aplique la configuración de red de la aplicación", "Adresse virtuelle modifiée ; appliquez les paramètres réseau de l’application");
        row("Адреса изменились; примените настройки повторно", "Addresses changed; apply the settings again", "Adressen geändert; Einstellungen erneut anwenden", "Las direcciones han cambiado; vuelva a aplicar la configuración", "Les adresses ont changé ; appliquez de nouveau les paramètres");
        row("Адреса изменились; исправьте настройки сети", "Addresses changed; correct the network settings", "Adressen geändert; Netzwerkeinstellungen korrigieren", "Las direcciones han cambiado; corrija la configuración de red", "Les adresses ont changé ; corrigez les paramètres réseau");
        row("Настройки изменены; нажмите «Проверить и применить»", "Settings changed; click “Check and apply”", "Einstellungen geändert; „Prüfen und anwenden“ anklicken", "Configuración modificada; pulse «Comprobar y aplicar»", "Paramètres modifiés ; cliquez sur « Vérifier et appliquer »");
        row("Адрес шлюза изменён; нажмите «Проверить шлюзы»", "Gateway address changed; click “Check gateways”", "Gateway-Adresse geändert; „Gateways prüfen“ anklicken", "Dirección de la puerta de enlace modificada; pulse «Comprobar puertas de enlace»", "Adresse de la passerelle modifiée ; cliquez sur « Vérifier les passerelles »");
        row("Нажмите «Проверить шлюзы» для проверки адреса", "Click “Check gateways” to verify the address", "Zum Prüfen der Adresse „Gateways prüfen“ anklicken", "Pulse «Comprobar puertas de enlace» para verificar la dirección", "Cliquez sur « Vérifier les passerelles » pour vérifier l’adresse");
        row("Проверяем маршруты шлюзов для этого источника…", "Checking gateway routes for this source…", "Gateway-Routen für diese Quelle werden geprüft…", "Comprobando rutas de las puertas de enlace para este origen…", "Vérification des routes des passerelles pour cette source…");
        row("Проверяем доступность выбранных шлюзов для этого источника…", "Checking selected gateways for this source…", "Ausgewählte Gateways für diese Quelle werden geprüft…", "Comprobando las puertas de enlace seleccionadas para este origen…", "Vérification des passerelles sélectionnées pour cette source…");
        row("Настройки нужны", "Configuration needed", "Konfiguration erforderlich", "Se necesita configuración", "Configuration nécessaire");
        row("Проверка…", "Checking…", "Wird geprüft…", "Comprobando…", "Vérification…");
        row("Ссылка изменилась; повторите проверку шлюза для этого файла", "Link changed; check the gateway again for this file", "Link geändert; Gateway für diese Datei erneut prüfen", "El enlace ha cambiado; vuelva a comprobar la puerta de enlace para este archivo", "Le lien a changé ; vérifiez de nouveau la passerelle pour ce fichier");
        row("Не подтверждён выбранный выход в интернет", "Selected internet connection could not be confirmed", "Ausgewählter Internetzugang konnte nicht bestätigt werden", "No se pudo confirmar la salida a Internet seleccionada", "La connexion Internet sélectionnée n’a pas pu être confirmée");
        row("Маршрут через шлюз подтверждён", "Route through gateway confirmed", "Route über das Gateway bestätigt", "Ruta por la puerta de enlace confirmada", "Route via la passerelle confirmée");
        row("Нет отдельного соединения через этот шлюз", "No dedicated connection through this gateway", "Keine separate Verbindung über dieses Gateway", "No hay conexión independiente por esta puerta de enlace", "Aucune connexion dédiée via cette passerelle");
        row("Нет соединения", "No connection", "Keine Verbindung", "Sin conexión", "Aucune connexion");
        row("Выберите хотя бы один шлюз", "Select at least one gateway", "Mindestens ein Gateway auswählen", "Seleccione al menos una puerta de enlace", "Sélectionnez au moins une passerelle");
        row("Ссылка изменилась; маршрут будет проверен перед загрузкой или тестом", "Link changed; the route will be checked before downloading or testing", "Link geändert; Route wird vor Download oder Test geprüft", "El enlace ha cambiado; se comprobará la ruta antes de descargar o probar", "Le lien a changé ; la route sera vérifiée avant le téléchargement ou le test");
        row("Скорость: проверка…", "Speed: testing…", "Geschwindigkeit: wird getestet…", "Velocidad: probando…", "Vitesse : test…");
        row("Последовательно проверяем скорости шлюзов…", "Testing gateway speeds one at a time…", "Gateway-Geschwindigkeiten werden nacheinander getestet…", "Probando las velocidades de las puertas de enlace una por una…", "Test des vitesses des passerelles une par une…");
        row("Не получен результат замера", "No speed test result received", "Kein Ergebnis des Geschwindigkeitstests erhalten", "No se recibió ningún resultado de velocidad", "Aucun résultat du test de vitesse reçu");
        row("Некорректный результат замера", "Invalid speed test result", "Ungültiges Ergebnis des Geschwindigkeitstests", "Resultado de velocidad no válido", "Résultat du test de vitesse non valide");
        row("Нет замера", "Not measured", "Nicht gemessen", "Sin medición", "Non mesuré");
        row("Скорость: не измерена", "Speed: not measured", "Geschwindigkeit: nicht gemessen", "Velocidad: sin medir", "Vitesse : non mesurée");
        row("Ссылка или сеть изменились; повторите тест скорости", "Link or network changed; repeat the speed test", "Link oder Netzwerk geändert; Geschwindigkeitstest wiederholen", "El enlace o la red han cambiado; repita la prueba de velocidad", "Le lien ou le réseau a changé ; répétez le test de vitesse");
        row("Укажите ссылку на файл", "Enter a file link", "Dateilink eingeben", "Introduzca un enlace de archivo", "Saisissez un lien vers un fichier");
        row("Ожидаемая SHA-256 должна содержать 64 шестнадцатеричных символа", "Expected SHA-256 must contain 64 hexadecimal characters", "Der erwartete SHA-256-Wert muss 64 hexadezimale Zeichen enthalten", "El SHA-256 esperado debe contener 64 caracteres hexadecimales", "Le SHA-256 attendu doit contenir 64 caractères hexadécimaux");
        row("Подготовка…", "Preparing…", "Vorbereitung…", "Preparando…", "Préparation…");
        row("Создаём загрузку и проверяем выбранные шлюзы…", "Creating the download and checking selected gateways…", "Download wird erstellt, ausgewählte Gateways werden geprüft…", "Creando la descarga y comprobando las puertas de enlace seleccionadas…", "Création du téléchargement et vérification des passerelles sélectionnées…");
        row("Получаем сведения о раздаче через выбранный шлюз…", "Fetching torrent metadata through the selected gateway…", "Torrent-Metadaten werden über das ausgewählte Gateway abgerufen…", "Obteniendo los metadatos del torrent por la puerta de enlace seleccionada…", "Récupération des métadonnées du torrent via la passerelle sélectionnée…");
        row("Выбор файлов…", "Selecting files…", "Dateiauswahl…", "Seleccionando archivos…", "Sélection des fichiers…");
        row("Выбор файлов отменён", "File selection cancelled", "Dateiauswahl abgebrochen", "Selección de archivos cancelada", "Sélection des fichiers annulée");
        row("Выберите хотя бы один файл раздачи", "Select at least one torrent file", "Mindestens eine Datei des Torrents auswählen", "Seleccione al menos un archivo del torrent", "Sélectionnez au moins un fichier du torrent");
        row("Получаем список файлов Hugging Face…", "Fetching the Hugging Face file list…", "Hugging-Face-Dateiliste wird abgerufen…", "Obteniendo la lista de archivos de Hugging Face…", "Récupération de la liste des fichiers Hugging Face…");
        row("Выберите хотя бы один файл", "Select at least one file", "Mindestens eine Datei auswählen", "Seleccione al menos un archivo", "Sélectionnez au moins un fichier");
        row("Ссылка или настройки изменились; начните загрузку повторно", "Link or settings changed; start the download again", "Link oder Einstellungen geändert; Download erneut starten", "El enlace o la configuración han cambiado; inicie la descarga de nuevo", "Le lien ou les paramètres ont changé ; démarrez de nouveau le téléchargement");
        row("указан вручную", "entered manually", "manuell eingegeben", "introducido manualmente", "saisi manuellement");
        row("совпадает ✓", "matches ✓", "stimmt überein ✓", "coincide ✓", "identique ✓");
        row("ожидается проверка", "verification pending", "Prüfung ausstehend", "verificación pendiente", "vérification en attente");
        row("Повторяем запрос к источнику. Причина уточняется.", "Retrying the source request. Waiting for details.", "Anfrage an die Quelle wird wiederholt. Details werden ermittelt.", "Reintentando la solicitud al origen. Esperando detalles.", "Nouvelle tentative auprès de la source. En attente de détails.");
        row("Не удалось загрузить файл. Подробности ниже.", "Could not download the file. Details below.", "Datei konnte nicht heruntergeladen werden. Details unten.", "No se pudo descargar el archivo. Detalles abajo.", "Impossible de télécharger le fichier. Détails ci-dessous.");
        row("Повторяем запрос к источнику", "Retrying the source request", "Anfrage an die Quelle wird wiederholt", "Reintentando la solicitud al origen", "Nouvelle tentative auprès de la source");
        row("Не удалось получить файл", "Could not retrieve the file", "Datei konnte nicht abgerufen werden", "No se pudo obtener el archivo", "Impossible de récupérer le fichier");
        row("Загрузка приостановлена", "Download paused", "Download angehalten", "Descarga pausada", "Téléchargement en pause");
        row("Ожидаем начала запроса", "Waiting for the request to start", "Warten auf den Beginn der Anfrage", "Esperando el inicio de la solicitud", "En attente du début de la requête");
        row("Определяем размер", "Determining size", "Größe wird ermittelt", "Determinando tamaño", "Détermination de la taille");
        row("[адрес источника]", "[source URL]", "[Quelladresse]", "[dirección del origen]", "[adresse de la source]");
        row("не выбран", "not selected", "nicht ausgewählt", "sin seleccionar", "non sélectionné");
        row("пауза", "paused", "angehalten", "pausada", "en pause");
        row("ошибка", "error", "Fehler", "error", "erreur");
        row("проверка файла", "verifying file", "Datei wird geprüft", "verificando archivo", "vérification du fichier");
        row("оценивается…", "estimating…", "wird geschätzt…", "estimando…", "estimation…");
        row("части проверены ✓", "pieces verified ✓", "Teile geprüft ✓", "partes verificadas ✓", "pièces vérifiées ✓");
        row("ожидается проверка частей", "piece verification pending", "Prüfung der Teile ausstehend", "verificación de partes pendiente", "vérification des pièces en attente");
        row("сумма появится после завершения", "checksum available when complete", "Prüfsumme nach Abschluss verfügbar", "suma disponible al finalizar", "somme disponible après la fin");
        row("Загрузка продолжена", "Download resumed", "Download fortgesetzt", "Descarga reanudada", "Téléchargement repris");
        row("Удаление отменено", "Removal cancelled", "Entfernen abgebrochen", "Eliminación cancelada", "Suppression annulée");
        row("Удаляем задание, файлы и скачанные части…", "Removing download, files and downloaded parts…", "Download, Dateien und heruntergeladene Teile werden entfernt…", "Eliminando la descarga, los archivos y las partes descargadas…", "Suppression du téléchargement, des fichiers et des parties téléchargées…");
        row("Удаляем запись о загрузке…", "Removing download entry…", "Download-Eintrag wird entfernt…", "Eliminando la entrada de descarga…", "Suppression de l’entrée de téléchargement…");
        row("Задание, его файлы и скачанные части удалены.", "Download, its files and downloaded parts removed.", "Download, seine Dateien und heruntergeladenen Teile entfernt.", "Descarga, sus archivos y partes descargadas eliminados.", "Téléchargement, fichiers et parties téléchargées supprimés.");
        row("Запись удалена. Файлы и скачанные части сохранены.", "Entry removed. Files and downloaded parts kept.", "Eintrag entfernt. Dateien und heruntergeladene Teile bleiben erhalten.", "Entrada eliminada. Se han conservado los archivos y las partes descargadas.", "Entrée supprimée. Fichiers et parties téléchargées conservés.");
        row("Выбор папки отменён", "Folder selection cancelled", "Ordnerauswahl abgebrochen", "Selección de carpeta cancelada", "Sélection du dossier annulée");
        row("Переносим скачанные части в выбранную папку…", "Moving downloaded parts to the selected folder…", "Heruntergeladene Teile werden in den ausgewählten Ordner verschoben…", "Moviendo las partes descargadas a la carpeta seleccionada…", "Déplacement des parties téléchargées vers le dossier sélectionné…");
        row("Папка изменена. Скачанные части сохранены; нажмите «Продолжить».", "Folder changed. Downloaded parts kept; click “Resume”.", "Ordner geändert. Heruntergeladene Teile bleiben erhalten; „Fortsetzen“ anklicken.", "Carpeta cambiada. Partes descargadas conservadas; pulse «Reanudar».", "Dossier modifié. Parties téléchargées conservées ; cliquez sur « Reprendre ».");
        row("Настройки восстановлены; шлюзы нужно проверить повторно", "Settings restored; check gateways again", "Einstellungen wiederhergestellt; Gateways erneut prüfen", "Configuración restaurada; vuelva a comprobar las puertas de enlace", "Paramètres restaurés ; vérifiez de nouveau les passerelles");
        row("В очереди", "Queued", "In Warteschlange", "En cola", "En attente");
        row("Загрузка", "Downloading", "Wird heruntergeladen", "Descargando", "Téléchargement");
        row("Повторная попытка", "Retrying", "Erneuter Versuch", "Reintentando", "Nouvelle tentative");
        row("Проверка", "Verifying", "Wird geprüft", "Verificando", "Vérification");
        row("Ошибка", "Error", "Fehler", "Error", "Erreur");
        row("Язык", "Language", "Sprache", "Idioma", "Langue");
        row("Язык интерфейса", "Interface language", "Sprache der Oberfläche", "Idioma de la interfaz", "Langue de l’interface");
        row("Язык интерфейса изменён", "Interface language changed", "Sprache der Oberfläche geändert", "Idioma de la interfaz cambiado", "Langue de l’interface modifiée");
        row("%.1f Мбит/с", "%.1f Mbit/s", "%.1f Mbit/s", "%.1f Mbit/s", "%.1f Mbit/s");
        row("Скорость: %.1f Мбит/с", "Speed: %.1f Mbit/s", "Geschwindigkeit: %.1f Mbit/s", "Velocidad: %.1f Mbit/s", "Vitesse : %.1f Mbit/s");
        row("%d Б", "%d B", "%d B", "%d B", "%d o");
        row("%.1f КБ", "%.1f KB", "%.1f KB", "%.1f KB", "%.1f Ko");
        row("%.1f МБ", "%.1f MB", "%.1f MB", "%.1f MB", "%.1f Mo");
        row("%.2f ГБ", "%.2f GB", "%.2f GB", "%.2f GB", "%.2f Go");
        row("%.1f КиБ", "%.1f KiB", "%.1f KiB", "%.1f KiB", "%.1f Kio");
        row("%.1f МиБ", "%.1f MiB", "%.1f MiB", "%.1f MiB", "%.1f Mio");
        row("%.2f ГиБ", "%.2f GiB", "%.2f GiB", "%.2f GiB", "%.2f Gio");
        row("Hugging Face — выбрать файлы", "Hugging Face — select files", "Hugging Face — Dateien auswählen", "Hugging Face — seleccionar archivos", "Hugging Face — sélectionner les fichiers");
        row("BitTorrent — выбрать файлы", "BitTorrent — select files", "BitTorrent — Dateien auswählen", "BitTorrent — seleccionar archivos", "BitTorrent — sélectionner les fichiers");
        row("%s  ·  версия %s", "%s  ·  revision %s", "%s  ·  Version %s", "%s  ·  versión %s", "%s  ·  version %s");
        row("Только GGUF", "GGUF only", "Nur GGUF", "Solo GGUF", "GGUF uniquement");
        row("Все файлы", "All files", "Alle Dateien", "Todos los archivos", "Tous les fichiers");
        row("Выбрать показанные", "Select visible files", "Angezeigte Dateien auswählen", "Seleccionar los archivos visibles", "Sélectionner les fichiers affichés");
        row("Выбрать все файлы", "Select all files", "Alle Dateien auswählen", "Seleccionar todos los archivos", "Sélectionner tous les fichiers");
        row("Снять всё", "Clear all", "Alles abwählen", "Quitar toda la selección", "Tout désélectionner");
        row("Загрузить выбранные", "Download selected", "Auswahl herunterladen", "Descargar seleccionados", "Télécharger la sélection");
        row("Отмена", "Cancel", "Abbrechen", "Cancelar", "Annuler");
        row("Показать:", "Show:", "Anzeigen:", "Mostrar:", "Afficher :");
        row("Выберите нужную квантизацию. Части одного GGUF выбираются вместе.", "Select the quantization you need. All parts of one GGUF are selected together.", "Gewünschte Quantisierung auswählen. Alle Teile derselben GGUF-Datei werden gemeinsam ausgewählt.", "Seleccione la cuantización que necesita. Las partes de un mismo GGUF se seleccionan juntas.", "Sélectionnez la quantification souhaitée. Toutes les parties d’un même GGUF sont sélectionnées ensemble.");
        row("Части проверяются по метаданным раздачи. Имена и папки сохраняются.", "Pieces are verified against torrent metadata. File names and folders are preserved.", "Teile werden anhand der Torrent-Metadaten geprüft. Dateinamen und Ordner bleiben erhalten.", "Las partes se verifican con los metadatos del torrent. Se conservan los nombres y las carpetas.", "Les pièces sont vérifiées avec les métadonnées du torrent. Les noms et les dossiers sont conservés.");
        row("Выбрано: %d файлов · %s", "Selected: %d files · %s", "Ausgewählt: %d Dateien · %s", "Seleccionados: %d archivos · %s", "Sélection : %d fichiers · %s");
        row("Выбрать", "Select", "Auswählen", "Seleccionar", "Sélectionner");
        row("Размер", "Size", "Größe", "Tamaño", "Taille");
        row("Хранилище", "Storage", "Speicher", "Almacenamiento", "Stockage");
        row("SHA-256 источника", "Source SHA-256", "SHA-256 der Quelle", "SHA-256 del origen", "SHA-256 de la source");
        row("Не опубликована", "Not published", "Nicht veröffentlicht", "No publicado", "Non publié");
        row("Тип", "Type", "Typ", "Tipo", "Type");
        row("Служебное заполнение", "Padding", "Fülldaten", "Relleno", "Remplissage");
        row("\nВыберите «Изменить папку…» и папку со свободным местом, затем нажмите «Продолжить». Скачанные части сохранятся.\n", "\nClick “Change folder…”, select a folder with free space, then click “Resume”. Downloaded parts will be kept.\n", "\n„Ordner ändern…“ anklicken, einen Ordner mit freiem Speicher auswählen und „Fortsetzen“ anklicken. Heruntergeladene Teile bleiben erhalten.\n", "\nPulse «Cambiar carpeta…», seleccione una carpeta con espacio libre y pulse «Reanudar». Se conservarán las partes descargadas.\n", "\nCliquez sur « Changer de dossier… », choisissez un dossier avec de l’espace libre, puis cliquez sur « Reprendre ». Les parties téléchargées seront conservées.\n");
        row("\nВыберите папку со свободным местом.", "\nSelect a folder with free space.", "\nOrdner mit freiem Speicher auswählen.", "\nSeleccione una carpeta con espacio libre.", "\nSélectionnez un dossier avec de l’espace libre.");
        row("\nЛОКАЛЬНЫЕ SHA-256 ФАЙЛОВ\n", "\nLOCAL FILE SHA-256 CHECKSUMS\n", "\nLOKALE SHA-256-DATEIPRÜFSUMMEN\n", "\nSUMAS SHA-256 LOCALES DE LOS ARCHIVOS\n", "\nSOMMES SHA-256 LOCALES DES FICHIERS\n");
        row("\nНажмите «Изменить папку…», выберите папку со свободным местом, затем «Продолжить».", "\nClick “Change folder…”, select a folder with free space, then click “Resume”.", "\n„Ordner ändern…“ anklicken, Ordner mit freiem Speicher auswählen und „Fortsetzen“ anklicken.", "\nPulse «Cambiar carpeta…», seleccione una carpeta con espacio libre y pulse «Reanudar».", "\nCliquez sur « Changer de dossier… », choisissez un dossier avec de l’espace libre, puis cliquez sur « Reprendre ».");
        row("\nОШИБКА\n", "\nERROR\n", "\nFEHLER\n", "\nERROR\n", "\nERREUR\n");
        row("\nПРОВЕРКА SHA-256\n", "\nSHA-256 VERIFICATION\n", "\nSHA-256-PRÜFUNG\n", "\nVERIFICACIÓN SHA-256\n", "\nVÉRIFICATION SHA-256\n");
        row("\nПРОВЕРКА ЧАСТЕЙ BITTORRENT\n", "\nBITTORRENT PIECE VERIFICATION\n", "\nBITTORRENT-TEILPRÜFUNG\n", "\nVERIFICACIÓN DE PARTES BITTORRENT\n", "\nVÉRIFICATION DES PIÈCES BITTORRENT\n");
        row("\nУказанная SHA-256 одного файла: ", "\nExpected SHA-256 of one file: ", "\nAngegebener SHA-256-Wert einer Datei: ", "\nSHA-256 indicado de un archivo: ", "\nSHA-256 indiqué d’un fichier : ");
        row("\nШЛЮЗЫ\n", "\nGATEWAYS\n", "\nGATEWAYS\n", "\nPUERTAS DE ENLACE\n", "\nPASSERELLES\n");
        row("  Последняя ошибка: ", "  Last error: ", "  Letzter Fehler: ", "  Último error: ", "  Dernière erreur : ");
        row(" сегм.\n", " segments\n", " Segmente\n", " segmentos\n", " segments\n");
        row("; внешний IP %s", "; public IP %s", "; öffentliche IP %s", "; IP pública %s", "; IP publique %s");
        row("ID раздачи v1: ", "Torrent ID v1: ", "Torrent-ID v1: ", "ID del torrent v1: ", "ID du torrent v1 : ");
        row("ID раздачи v2: ", "Torrent ID v2: ", "Torrent-ID v2: ", "ID del torrent v2: ", "ID du torrent v2 : ");
        row("Torrent требует соединение приложения через шлюз %s; нажмите «Проверить и применить» в настройках сети", "Torrent requires an application connection through gateway %s; click “Check and apply” in network settings", "Torrent benötigt eine Anwendungsverbindung über Gateway %s; in den Netzwerkeinstellungen „Prüfen und anwenden“ anklicken", "Torrent requiere una conexión de la aplicación por la puerta de enlace %s; pulse «Comprobar y aplicar» en la configuración de red", "Torrent nécessite une connexion de l’application via la passerelle %s ; cliquez sur « Vérifier et appliquer » dans les paramètres réseau");
        row("Адрес %s свободен, настройки применены", "Address %s is available; settings applied", "Adresse %s ist frei; Einstellungen angewendet", "La dirección %s está libre; configuración aplicada", "L’adresse %s est libre ; paramètres appliqués");
        row("В буфере обмена нет текста", "Clipboard contains no text", "Die Zwischenablage enthält keinen Text", "El portapapeles no contiene texto", "Le presse-papiers ne contient aucun texte");
        row("Выбрано шлюзов: %d", "Selected gateways: %d", "Ausgewählte Gateways: %d", "Puertas de enlace seleccionadas: %d", "Passerelles sélectionnées : %d");
        row("Загрузка завершена: %s", "Download complete: %s", "Download abgeschlossen: %s", "Descarga completada: %s", "Téléchargement terminé : %s");
        row("Загрузка создана: %s", "Download created: %s", "Download erstellt: %s", "Descarga creada: %s", "Téléchargement créé : %s");
        row("Загрузка уже есть в списке: %s. Выделено существующее задание.", "Download already listed: %s. Existing download selected.", "Download bereits vorhanden: %s. Vorhandener Download ausgewählt.", "La descarga ya está en la lista: %s. Se ha seleccionado la descarga existente.", "Téléchargement déjà présent : %s. Le téléchargement existant est sélectionné.");
        row("Загрузка уже есть в списке: %s. Нажмите «Продолжить», чтобы докачать её.", "Download already listed: %s. Click “Resume” to finish it.", "Download bereits vorhanden: %s. Zum Abschließen „Fortsetzen“ anklicken.", "La descarga ya está en la lista: %s. Pulse «Reanudar» para completarla.", "Téléchargement déjà présent : %s. Cliquez sur « Reprendre » pour le terminer.");
        row("Источник эталона: ", "Reference source: ", "Quelle des Referenzwerts: ", "Origen de la suma de referencia: ", "Source de la somme de référence : ");
        row("Источник: ", "Source: ", "Quelle: ", "Origen: ", "Source : ");
        row("Контрольные суммы частей берутся из метаданных раздачи.\n", "Piece checksums are taken from torrent metadata.\n", "Die Prüfsummen der Teile stammen aus den Torrent-Metadaten.\n", "Las sumas de las partes se obtienen de los metadatos del torrent.\n", "Les sommes des pièces proviennent des métadonnées du torrent.\n");
        row("Маршрут через шлюз %s подтверждён", "Route through gateway %s confirmed", "Route über Gateway %s bestätigt", "Ruta por la puerta de enlace %s confirmada", "Route via la passerelle %s confirmée");
        row("Настройки окна не прочитаны: %s", "Could not read window settings: %s", "Fenstereinstellungen konnten nicht gelesen werden: %s", "No se pudo leer la configuración de la ventana: %s", "Impossible de lire les paramètres de la fenêtre : %s");
        row("Настройки сети не применены:\n%s", "Network settings not applied:\n%s", "Netzwerkeinstellungen nicht angewendet:\n%s", "Configuración de red no aplicada:\n%s", "Paramètres réseau non appliqués :\n%s");
        row("Настройки сети не применены: %s", "Network settings not applied: %s", "Netzwerkeinstellungen nicht angewendet: %s", "Configuración de red no aplicada: %s", "Paramètres réseau non appliqués : %s");
        row("Не удалось вставить URL: %s", "Could not paste URL: %s", "URL konnte nicht eingefügt werden: %s", "No se pudo pegar la URL: %s", "Impossible de coller l’URL : %s");
        row("Не удалось выбрать папку: %s", "Could not select folder: %s", "Ordner konnte nicht ausgewählt werden: %s", "No se pudo seleccionar la carpeta: %s", "Impossible de sélectionner le dossier : %s");
        row("Не удалось изменить папку: %s", "Could not change folder: %s", "Ordner konnte nicht geändert werden: %s", "No se pudo cambiar la carpeta: %s", "Impossible de changer de dossier : %s");
        row("Не удалось изменить состояние загрузки: %s%s", "Could not change download status: %s%s", "Download-Status konnte nicht geändert werden: %s%s", "No se pudo cambiar el estado de la descarga: %s%s", "Impossible de changer l’état du téléchargement : %s%s");
        row("Не удалось обновить список: %s", "Could not update list: %s", "Liste konnte nicht aktualisiert werden: %s", "No se pudo actualizar la lista: %s", "Impossible de mettre à jour la liste : %s");
        row("Не удалось открыть подтверждение удаления: %s", "Could not open removal confirmation: %s", "Entfernungsbestätigung konnte nicht geöffnet werden: %s", "No se pudo abrir la confirmación de eliminación: %s", "Impossible d’ouvrir la confirmation de suppression : %s");
        row("Не удалось проверить соединение шлюза: %s", "Could not check gateway connection: %s", "Gateway-Verbindung konnte nicht geprüft werden: %s", "No se pudo comprobar la conexión de la puerta de enlace: %s", "Impossible de vérifier la connexion de la passerelle : %s");
        row("Не удалось создать загрузку:\n%s%s", "Could not create download:\n%s%s", "Download konnte nicht erstellt werden:\n%s%s", "No se pudo crear la descarga:\n%s%s", "Impossible de créer le téléchargement :\n%s%s");
        row("Не удалось создать загрузку: %s", "Could not create download: %s", "Download konnte nicht erstellt werden: %s", "No se pudo crear la descarga: %s", "Impossible de créer le téléchargement : %s");
        row("Не удалось сохранить настройки окна: %s", "Could not save window settings: %s", "Fenstereinstellungen konnten nicht gespeichert werden: %s", "No se pudo guardar la configuración de la ventana: %s", "Impossible d’enregistrer les paramètres de la fenêtre : %s");
        row("Не удалось удалить задание: %s", "Could not remove download: %s", "Download konnte nicht entfernt werden: %s", "No se pudo eliminar la descarga: %s", "Impossible de supprimer le téléchargement : %s");
        row("Новых загрузок: %d; уже есть в списке: %d. Выделено существующее задание.", "New downloads: %d; already listed: %d. Existing download selected.", "Neue Downloads: %d; bereits vorhanden: %d. Vorhandener Download ausgewählt.", "Nuevas descargas: %d; ya en la lista: %d. Se ha seleccionado una descarga existente.", "Nouveaux téléchargements : %d ; déjà présents : %d. Un téléchargement existant est sélectionné.");
        row("Общая скорость: %s", "Total speed: %s", "Gesamtgeschwindigkeit: %s", "Velocidad total: %s", "Vitesse totale : %s");
        row("Осталось: %s", "Remaining: %s", "Verbleibend: %s", "Restante: %s", "Restant : %s");
        row("Ошибка загрузки: %s", "Download error: %s", "Download-Fehler: %s", "Error de descarga: %s", "Erreur de téléchargement : %s");
        row("Ошибка: %s", "Error: %s", "Fehler: %s", "Error: %s", "Erreur : %s");
        row("Повтор запроса: %s", "Retrying request: %s", "Anfrage wird wiederholt: %s", "Reintentando solicitud: %s", "Nouvelle tentative : %s");
        row("Поиск эталона у источника и проверка выполняются автоматически.\n", "The reference checksum is discovered at the source and verified automatically.\n", "Der Referenzwert wird bei der Quelle gesucht und automatisch geprüft.\n", "La suma de referencia se busca en el origen y se verifica automáticamente.\n", "La somme de référence est recherchée auprès de la source et vérifiée automatiquement.\n");
        row("Полученная: ", "Calculated: ", "Berechnet: ", "Calculada: ", "Calculée : ");
        row("Получено %d байт за %d мс", "Received %d bytes in %d ms", "%d Bytes in %d ms empfangen", "Recibidos %d bytes en %d ms", "%d octets reçus en %d ms");
        row("Последняя ошибка: %s", "Last error: %s", "Letzter Fehler: %s", "Último error: %s", "Dernière erreur : %s");
        row("Причина: %s", "Reason: %s", "Ursache: %s", "Motivo: %s", "Cause : %s");
        row("Проверено и сохранено: ", "Verified and saved: ", "Geprüft und gespeichert: ", "Verificado y guardado: ", "Vérifié et enregistré : ");
        row("Проверка скоростей завершена: %d/%d", "Speed tests complete: %d/%d", "Geschwindigkeitstests abgeschlossen: %d/%d", "Pruebas de velocidad completadas: %d/%d", "Tests de vitesse terminés : %d/%d");
        row("Проверяем маршрут через шлюз %s", "Checking route through gateway %s", "Route über Gateway %s wird geprüft", "Comprobando la ruta por la puerta de enlace %s", "Vérification de la route via la passerelle %s");
        row("Проверяем скорость: шлюз %s", "Testing speed: gateway %s", "Geschwindigkeitstest: Gateway %s", "Probando velocidad: puerta de enlace %s", "Test de vitesse : passerelle %s");
        row("Проверяем, свободен ли адрес %s…", "Checking whether address %s is available…", "Verfügbarkeit der Adresse %s wird geprüft…", "Comprobando si la dirección %s está libre…", "Vérification de la disponibilité de l’adresse %s…");
        row("Прошло: %s", "Elapsed: %s", "Vergangen: %s", "Transcurrido: %s", "Écoulé : %s");
        row("Размер: ", "Size: ", "Größe: ", "Tamaño: ", "Taille : ");
        row("Результат: ", "Result: ", "Ergebnis: ", "Resultado: ", "Résultat : ");
        row("Сверка: ", "Verification: ", "Vergleich: ", "Verificación: ", "Vérification : ");
        row("Создано загрузок: %d", "Downloads created: %d", "Erstellte Downloads: %d", "Descargas creadas: %d", "Téléchargements créés : %d");
        row("Суммы будут рассчитаны после сборки файлов.\n", "Checksums will be calculated after assembling the files.\n", "Prüfsummen werden nach dem Zusammensetzen der Dateien berechnet.\n", "Las sumas se calcularán después de ensamblar los archivos.\n", "Les sommes seront calculées après l’assemblage des fichiers.\n");
        row("Тип: BitTorrent\n", "Type: BitTorrent\n", "Typ: BitTorrent\n", "Tipo: BitTorrent\n", "Type : BitTorrent\n");
        row("Убрать загрузку «%s» из приложения?\nПапка: %s\n\nБез галочки готовые файлы и скачанные части останутся на диске.\nС галочкой будут удалены только файлы и части этой загрузки.", "Remove download “%s” from the application?\nFolder: %s\n\nLeave the box unchecked to keep completed files and downloaded parts on disk.\nCheck it to delete only files and parts belonging to this download.", "Download „%s“ aus der Anwendung entfernen?\nOrdner: %s\n\nOhne Häkchen bleiben fertige Dateien und heruntergeladene Teile auf dem Datenträger.\nMit Häkchen werden nur Dateien und Teile dieses Downloads gelöscht.", "¿Eliminar la descarga «%s» de la aplicación?\nCarpeta: %s\n\nSin marcar la casilla, los archivos completos y las partes descargadas permanecerán en el disco.\nSi la marca, solo se eliminarán los archivos y las partes de esta descarga.", "Supprimer le téléchargement « %s » de l’application ?\nDossier : %s\n\nSans cocher la case, les fichiers terminés et les parties téléchargées restent sur le disque.\nEn la cochant, seuls les fichiers et les parties de ce téléchargement seront supprimés.");
        row("Узел не подтвердил маршрут: %s. ", "The host did not confirm the route: %s. ", "Der Host hat die Route nicht bestätigt: %s. ", "El nodo no confirmó la ruta: %s. ", "L’hôte n’a pas confirmé la route : %s. ");
        row("Укажите уникальный IPv4 адрес шлюза", "Enter a unique IPv4 gateway address", "Eindeutige IPv4-Adresse des Gateways eingeben", "Introduzca una dirección IPv4 única para la puerta de enlace", "Saisissez une adresse IPv4 unique pour la passerelle");
        row("Файл: ", "File: ", "Datei: ", "Archivo: ", "Fichier : ");
        row("Шлюз ", "Gateway ", "Gateway ", "Puerta de enlace ", "Passerelle ");
        row("Шлюз %s", "Gateway %s", "Gateway %s", "Puerta de enlace %s", "Passerelle %s");
        row("Шлюз %s · скорость: %.1f Мбит/с", "Gateway %s · speed: %.1f Mbit/s", "Gateway %s · Geschwindigkeit: %.1f Mbit/s", "Puerta de enlace %s · velocidad: %.1f Mbit/s", "Passerelle %s · vitesse : %.1f Mbit/s");
        row("Шлюз %s готов", "Gateway %s ready", "Gateway %s bereit", "Puerta de enlace %s lista", "Passerelle %s prête");
        row("Шлюз %s: %s", "Gateway %s: %s", "Gateway %s: %s", "Puerta de enlace %s: %s", "Passerelle %s : %s");
        row("Эталон источником не опубликован · рассчитана только локальная сумма\n", "No reference checksum published by the source · local checksum calculated only\n", "Keine Referenzprüfsumme vom Anbieter veröffentlicht · nur lokale Prüfsumme berechnet\n", "El origen no publica una suma de referencia · solo se calcula la suma local\n", "Aucune somme de référence publiée par la source · seule la somme locale est calculée\n");
        row("Эталон:    ", "Reference:    ", "Referenz:    ", "Referencia:    ", "Référence :    ");
        row("Язык:", "Language:", "Sprache:", "Idioma:", "Langue :");
        row("Да", "Yes", "Ja", "Sí", "Oui");
        row("Нет", "No", "Nein", "No", "Non");
        row("Сообщение", "Message", "Meldung", "Mensaje", "Message");
        row("Ввод", "Input", "Eingabe", "Entrada", "Saisie");
        row("Открыть", "Open", "Öffnen", "Abrir", "Ouvrir");
        row("Сохранить", "Save", "Speichern", "Guardar", "Enregistrer");
        row("Обновить", "Refresh", "Aktualisieren", "Actualizar", "Actualiser");
        row("Помощь", "Help", "Hilfe", "Ayuda", "Aide");
        row("Инструкция", "Instructions", "Anleitung", "Instrucciones", "Instructions");
        row("Работа с API", "API guide", "API-Anleitung", "Guía de la API", "Guide API");
        row("Открытие браузера недоступно", "Opening a browser is unavailable", "Das Öffnen eines Browsers ist nicht verfügbar", "No se puede abrir un navegador", "L’ouverture d’un navigateur est indisponible");
        row("Открыть инструкцию на выбранном языке в браузере.", "Open the instructions in your browser in the selected language.", "Die Anleitung in der ausgewählten Sprache im Browser öffnen.", "Abrir las instrucciones en el navegador en el idioma seleccionado.", "Ouvrir les instructions dans le navigateur dans la langue sélectionnée.");
        row("Открыть примеры запросов и описание API на выбранном языке в браузере.", "Open API request examples and documentation in your browser in the selected language.", "API-Anfragebeispiele und Dokumentation in der ausgewählten Sprache im Browser öffnen.", "Abrir ejemplos de solicitudes y la documentación de la API en el navegador en el idioma seleccionado.", "Ouvrir les exemples de requêtes et la documentation de l’API dans le navigateur dans la langue sélectionnée.");
        row("Не удалось открыть инструкцию: %s\nСсылка: %s", "Could not open the instructions: %s\nLink: %s", "Die Anleitung konnte nicht geöffnet werden: %s\nLink: %s", "No se pudieron abrir las instrucciones: %s\nEnlace: %s", "Impossible d’ouvrir les instructions : %s\nLien : %s");
        row("Имя файла:", "File name:", "Dateiname:", "Nombre de archivo:", "Nom du fichier :");
        row("Имя папки:", "Folder name:", "Ordnername:", "Nombre de carpeta:", "Nom du dossier :");
        row("Тип файлов:", "Files of type:", "Dateityp:", "Archivos de tipo:", "Type de fichiers :");
        row("Папка:", "Folder:", "Ordner:", "Carpeta:", "Dossier :");
        row("Сохранить в:", "Save in:", "Speichern in:", "Guardar en:", "Enregistrer dans :");
        row("Открыть выбранный файл", "Open selected file", "Ausgewählte Datei öffnen", "Abrir el archivo seleccionado", "Ouvrir le fichier sélectionné");
        row("Сохранить выбранный файл", "Save selected file", "Ausgewählte Datei speichern", "Guardar el archivo seleccionado", "Enregistrer le fichier sélectionné");
        row("Закрыть выбор файла", "Close file chooser", "Dateiauswahl schließen", "Cerrar la selección de archivos", "Fermer la sélection de fichiers");
        row("Открыть выбранную папку", "Open selected folder", "Ausgewählten Ordner öffnen", "Abrir la carpeta seleccionada", "Ouvrir le dossier sélectionné");
        row("Обновить список файлов", "Refresh file list", "Dateiliste aktualisieren", "Actualizar la lista de archivos", "Actualiser la liste des fichiers");
        row("Помощь по выбору файла", "File selection help", "Hilfe zur Dateiauswahl", "Ayuda sobre la selección de archivos", "Aide à la sélection de fichiers");
        row("На одну папку выше", "Up one folder", "Übergeordneter Ordner", "Subir una carpeta", "Dossier parent");
        row("Создать папку", "Create folder", "Ordner erstellen", "Crear carpeta", "Créer un dossier");
        row("Список", "List", "Liste", "Lista", "Liste");
        row("Подробности", "Details", "Details", "Detalles", "Détails");
        row("Домашняя папка", "Home folder", "Persönlicher Ordner", "Carpeta personal", "Dossier personnel");
        row("Имя", "Name", "Name", "Nombre", "Nom");
        row("Изменён", "Modified", "Geändert", "Modificado", "Modifié");
        row("Атрибуты", "Attributes", "Attribute", "Atributos", "Attributs");
        row("Вид", "View", "Ansicht", "Vista", "Affichage");
        row("Имя новой папки:", "New folder name:", "Name des neuen Ordners:", "Nombre de la nueva carpeta:", "Nom du nouveau dossier :");
        row("Не удалось создать папку", "Could not create folder", "Ordner konnte nicht erstellt werden", "No se pudo crear la carpeta", "Impossible de créer le dossier");
        row("Родительская папка не существует", "Parent folder does not exist", "Übergeordneter Ordner existiert nicht", "La carpeta superior no existe", "Le dossier parent n’existe pas");
        row("Не удалось переименовать файл", "Could not rename file", "Datei konnte nicht umbenannt werden", "No se pudo cambiar el nombre del archivo", "Impossible de renommer le fichier");
        row("Не удалось переименовать {0}", "Could not rename {0}", "{0} konnte nicht umbenannt werden", "No se pudo cambiar el nombre de {0}", "Impossible de renommer {0}");
        row("Файл с именем {0} уже существует", "A file named {0} already exists", "Eine Datei mit dem Namen {0} existiert bereits", "Ya existe un archivo llamado {0}", "Un fichier nommé {0} existe déjà");
        row("Папка", "Folder", "Ordner", "Carpeta", "Dossier");
        row("Новая папка", "New folder", "Neuer Ordner", "Nueva carpeta", "Nouveau dossier");
        row("Новая папка.{0}", "New folder.{0}", "Neuer Ordner.{0}", "Nueva carpeta.{0}", "Nouveau dossier.{0}");
        row("Новая папка ({0})", "New folder ({0})", "Neuer Ordner ({0})", "Nueva carpeta ({0})", "Nouveau dossier ({0})");
        row("Тема:", "Theme:", "Design:", "Tema:", "Thème :");
        row("Светлая", "Light", "Hell", "Claro", "Clair");
        row("Тёмная", "Dark", "Dunkel", "Oscuro", "Sombre");
        row("Системная", "System", "System", "Sistema", "Système");
        row("Выберите светлую, тёмную или системную тему. Настройка сохраняется; загрузки продолжаются.", "Choose a light, dark or system theme. Your choice is saved; downloads keep running.", "Wählen Sie ein helles, dunkles oder Systemdesign. Die Auswahl wird gespeichert; Downloads laufen weiter.", "Elija un tema claro, oscuro o del sistema. La elección se guarda; las descargas continúan.", "Choisissez le thème clair, sombre ou système. Le choix est enregistré ; les téléchargements continuent.");
        row("Вставьте ссылку для загрузки", "Paste a download link", "Download-Link einfügen", "Pegue un enlace de descarga", "Collez un lien de téléchargement");
        row("Поиск загрузок", "Search downloads", "Downloads suchen", "Buscar descargas", "Rechercher des téléchargements");
        row("Поиск загрузки по имени файла, пути или ссылке. Ctrl/Cmd+F — поиск; Escape — очистить.", "Search downloads by file name, path or URL. Ctrl/Cmd+F to search; Escape to clear.", "Downloads nach Dateiname, Pfad oder URL suchen. Strg/Cmd+F zum Suchen; Escape zum Leeren.", "Buscar descargas por nombre, ruta o URL. Ctrl/Cmd+F para buscar; Escape para borrar.", "Rechercher par nom, chemin ou URL. Ctrl/Cmd+F pour rechercher ; Échap pour effacer.");
        row("Всего: %d · Активных: %d · Завершено: %d", "All: %d · Active: %d · Complete: %d", "Alle: %d · Aktiv: %d · Fertig: %d", "Total: %d · Activas: %d · Completadas: %d", "Total : %d · En cours : %d · Terminés : %d");
        row("Всего: 0 · Активных: 0 · Завершено: 0", "All: 0 · Active: 0 · Complete: 0", "Alle: 0 · Aktiv: 0 · Fertig: 0", "Total: 0 · Activas: 0 · Completadas: 0", "Total : 0 · En cours : 0 · Terminés : 0");
        row("Полное имя файла:", "Full file name:", "Vollständiger Dateiname:", "Nombre completo del archivo:", "Nom complet du fichier :");
        row("Полный путь:", "Full path:", "Vollständiger Pfad:", "Ruta completa:", "Chemin complet :");
        row("История", "History", "Verlauf", "Historial", "Historique");
        row("Загрузка восстановлена после перезапуска", "Download recovered after restart", "Download nach Neustart wiederhergestellt", "Descarga recuperada tras reiniciar", "Téléchargement récupéré après redémarrage");
        row("Событие загрузки", "Download event", "Download-Ereignis", "Evento de descarga", "Événement du téléchargement");
        row("История появится после начала загрузки. Старые даты не восстанавливаются предположительно.", "History appears after a download starts. Missing dates from older downloads are not guessed.", "Der Verlauf erscheint nach dem Start. Fehlende Daten älterer Downloads werden nicht geschätzt.", "El historial aparece al iniciar una descarga. No se estiman fechas que falten de descargas antiguas.", "L’historique apparaît au démarrage. Les dates manquantes des anciens téléchargements ne sont pas estimées.");
        row("Копировать полный путь", "Copy full path", "Vollständigen Pfad kopieren", "Copiar ruta completa", "Copier le chemin complet");
        row("Полный путь скопирован", "Full path copied", "Vollständiger Pfad kopiert", "Ruta completa copiada", "Chemin complet copié");
        row("Не удалось скопировать путь: %s", "Could not copy the path: %s", "Pfad konnte nicht kopiert werden: %s", "No se pudo copiar la ruta: %s", "Impossible de copier le chemin : %s");
        row("Загрузка начата: %s; полный путь: %s; задание: %s", "Download started: %s; full path: %s; job: %s", "Download gestartet: %s; vollständiger Pfad: %s; Auftrag: %s", "Descarga iniciada: %s; ruta completa: %s; tarea: %s", "Téléchargement démarré : %s ; chemin complet : %s ; tâche : %s");
        row("Загрузка продолжена: %s; полный путь: %s; задание: %s", "Download resumed: %s; full path: %s; job: %s", "Download fortgesetzt: %s; vollständiger Pfad: %s; Auftrag: %s", "Descarga reanudada: %s; ruta completa: %s; tarea: %s", "Téléchargement repris : %s ; chemin complet : %s ; tâche : %s");
        row("Загрузка приостановлена: %s; полный путь: %s; задание: %s", "Download paused: %s; full path: %s; job: %s", "Download pausiert: %s; vollständiger Pfad: %s; Auftrag: %s", "Descarga pausada: %s; ruta completa: %s; tarea: %s", "Téléchargement suspendu : %s ; chemin complet : %s ; tâche : %s");
        row("Загрузка завершена и проверена: %s; полный путь: %s; задание: %s", "Download completed and verified: %s; full path: %s; job: %s", "Download abgeschlossen und geprüft: %s; vollständiger Pfad: %s; Auftrag: %s", "Descarga completada y verificada: %s; ruta completa: %s; tarea: %s", "Téléchargement terminé et vérifié : %s ; chemin complet : %s ; tâche : %s");
        row("Ранее сохранённый файл проверен: %s; полный путь: %s; задание: %s", "Previously saved file verified: %s; full path: %s; job: %s", "Bereits gespeicherte Datei geprüft: %s; vollständiger Pfad: %s; Auftrag: %s", "Archivo guardado anteriormente verificado: %s; ruta completa: %s; tarea: %s", "Fichier précédemment enregistré vérifié : %s ; chemin complet : %s ; tâche : %s");
        row("Загрузка завершилась ошибкой: %s; полный путь: %s; задание: %s", "Download failed: %s; full path: %s; job: %s", "Download fehlgeschlagen: %s; vollständiger Pfad: %s; Auftrag: %s", "La descarga falló: %s; ruta completa: %s; tarea: %s", "Échec du téléchargement : %s ; chemin complet : %s ; tâche : %s");
        row("Не удалось записать историю загрузок: %s", "Could not write download history: %s", "Downloadverlauf konnte nicht geschrieben werden: %s", "No se pudo escribir el historial de descargas: %s", "Impossible d’écrire l’historique des téléchargements : %s");
        row("История загрузок появится после начала первой загрузки.", "Download history will appear after the first download starts.", "Der Downloadverlauf erscheint nach dem Start des ersten Downloads.", "El historial aparecerá cuando comience la primera descarga.", "L’historique apparaîtra au démarrage du premier téléchargement.");
        row("Последние записи истории: %d. Полная история: %s", "Recent history entries: %d. Complete history: %s", "Letzte Verlaufseinträge: %d. Vollständiger Verlauf: %s", "Entradas recientes del historial: %d. Historial completo: %s", "Entrées récentes de l’historique : %d. Historique complet : %s");
        row("Не удалось прочитать историю загрузок.", "Could not read download history.", "Downloadverlauf konnte nicht gelesen werden.", "No se pudo leer el historial de descargas.", "Impossible de lire l’historique des téléchargements.");
        row("История загрузок", "Download history", "Downloadverlauf", "Historial de descargas", "Historique des téléchargements");
        row("События приложения", "Application events", "Anwendungsereignisse", "Eventos de la aplicación", "Événements de l’application");
        row("Загрузка завершилась ошибкой", "Download failed", "Download fehlgeschlagen", "La descarga falló", "Échec du téléchargement");
        row("Ранее сохранённый файл проверен", "Previously saved file verified", "Zuvor gespeicherte Datei geprüft", "Archivo guardado previamente verificado", "Fichier précédemment enregistré vérifié");
        row("Загрузка начата", "Download started", "Download gestartet", "Descarga iniciada", "Téléchargement démarré");
        row("Загрузка завершена", "Download completed", "Download abgeschlossen", "Descarga completada", "Téléchargement terminé");
        row("SHA-256 после обратного чтения с сервера", "SHA-256 verified by reading back from the server", "SHA-256 nach erneutem Lesen vom Server geprüft", "SHA-256 verificado mediante lectura del servidor", "SHA-256 vérifié par relecture depuis le serveur");
        row("Адрес назначения:", "Destination URL:", "Ziel-URL:", "URL de destino:", "URL de destination :");
        row("Адрес назначения: ", "Destination URL: ", "Ziel-URL: ", "URL de destino: ", "URL de destination : ");
        row("Адрес сайта", "Site URL", "Website-URL", "URL del sitio", "URL du site");
        row("Без профиля", "No profile", "Kein Profil", "Sin perfil", "Aucun profil");
        row("Выберите локальные файлы явно. Отправки используют выбранные шлюзы; несколько файлов могут идти одновременно. Существующие файлы на сервере не заменяются. Передача одного файла идёт через один шлюз за попытку.", "Choose local files explicitly. Uploads use selected gateways; multiple files can run in parallel. Existing server files are not replaced. Each file uses one gateway per attempt.", "Wählen Sie lokale Dateien ausdrücklich aus. Uploads nutzen ausgewählte Gateways; mehrere Dateien können parallel laufen. Vorhandene Serverdateien werden nicht ersetzt. Jede Datei nutzt ein Gateway pro Versuch.", "Seleccione los archivos locales. Las subidas usan las puertas seleccionadas; varios archivos pueden ir en paralelo. No se reemplazan archivos existentes. Cada archivo usa una puerta por intento.", "Sélectionnez explicitement les fichiers locaux. Les envois utilisent les passerelles choisies ; plusieurs fichiers peuvent partir en parallèle. Les fichiers existants ne sont pas remplacés. Un fichier utilise une passerelle par tentative.");
        row("Выберите отправку", "Select an upload", "Upload auswählen", "Seleccione una subida", "Sélectionnez un envoi");
        row("Выберите отправку для просмотра локального пути, адреса назначения и результата проверки.", "Select an upload to view its local path, destination URL and verification result.", "Wählen Sie einen Upload für lokalen Pfad, Ziel-URL und Prüfergebnis.", "Seleccione una subida para ver su ruta local, URL de destino y verificación.", "Sélectionnez un envoi pour afficher son chemin local, sa destination et sa vérification.");
        row("Выберите файлы для отправки", "Select files to upload", "Dateien zum Hochladen auswählen", "Seleccione archivos para subir", "Sélectionnez les fichiers à envoyer");
        row("Выбрать профиль", "Select profile", "Profil auswählen", "Seleccionar perfil", "Sélectionner le profil");
        row("Выбрать файлы и отправить…", "Choose files and upload…", "Dateien auswählen und hochladen…", "Elegir archivos y subir…", "Choisir les fichiers et envoyer…");
        row("Для чтения заголовков нужен пароль архива.", "An archive password is required to read the headers.", "Zum Lesen der Archivköpfe wird ein Passwort benötigt.", "Se necesita la contraseña del archivo para leer las cabeceras.", "Un mot de passe est nécessaire pour lire les en-têtes de l’archive.");
        row("Добавлено отправок: %d", "Uploads added: %d", "Uploads hinzugefügt: %d", "Subidas añadidas: %d", "Envois ajoutés : %d");
        row("Добавлено отправок: %d. Ошибка: %s", "Uploads added: %d. Error: %s", "Uploads hinzugefügt: %d. Fehler: %s", "Subidas añadidas: %d. Error: %s", "Envois ajoutés : %d. Erreur : %s");
        row("Завершено: ", "Completed: ", "Abgeschlossen: ", "Finalizado: ", "Terminé : ");
        row("Заменить сохранённые учётные данные", "Replace saved credentials", "Gespeicherte Zugangsdaten ersetzen", "Reemplazar credenciales guardadas", "Remplacer les identifiants enregistrés");
        row("Зашифрован", "Encrypted", "Verschlüsselt", "Cifrado", "Chiffré");
        row("Изменить профиль", "Edit profile", "Profil bearbeiten", "Editar perfil", "Modifier le profil");
        row("Изменить…", "Edit…", "Bearbeiten…", "Editar…", "Modifier…");
        row("Локальный файл: ", "Local file: ", "Lokale Datei: ", "Archivo local: ", "Fichier local : ");
        row("Начало: ", "Started: ", "Gestartet: ", "Iniciado: ", "Démarré : ");
        row("Не удалось открыть содержимое архива: %s", "Could not preview the archive: %s", "Archivvorschau fehlgeschlagen: %s", "No se pudo ver el archivo: %s", "Impossible de prévisualiser l’archive : %s");
        row("Не удалось прочитать профили сайтов: %s", "Could not read site profiles: %s", "Website-Profile konnten nicht gelesen werden: %s", "No se pudieron leer los perfiles: %s", "Impossible de lire les profils de sites : %s");
        row("Новый профиль", "New profile", "Neues Profil", "Nuevo perfil", "Nouveau profil");
        row("Новый профиль…", "New profile…", "Neues Profil…", "Nuevo perfil…", "Nouveau profil…");
        row("Отправки", "Uploads", "Uploads", "Subidas", "Envois");
        row("Отправлено: %s из %s", "Uploaded: %s of %s", "Hochgeladen: %s von %s", "Subido: %s de %s", "Envoyé : %s sur %s");
        row("Отправок: %d · Активных: %d · Завершено: %d · Общая скорость: %s", "Uploads: %d · Active: %d · Complete: %d · Total speed: %s", "Uploads: %d · Aktiv: %d · Fertig: %d · Gesamtgeschwindigkeit: %s", "Subidas: %d · Activas: %d · Completadas: %d · Velocidad total: %s", "Envois : %d · Actifs : %d · Terminés : %d · Vitesse totale : %s");
        row("Папка для скачивания", "Download folder", "Download-Ordner", "Carpeta de descarga", "Dossier de téléchargement");
        row("Папка на сервере", "Server folder", "Serverordner", "Carpeta del servidor", "Dossier du serveur");
        row("Папка на сервере — URL FTP/FTPS/SFTP", "Server folder — FTP/FTPS/SFTP URL", "Serverordner — FTP/FTPS/SFTP-URL", "Carpeta del servidor — URL FTP/FTPS/SFTP", "Dossier du serveur — URL FTP/FTPS/SFTP");
        row("Пароль архива (не сохраняется)", "Archive password (not saved)", "Archivpasswort (wird nicht gespeichert)", "Contraseña del archivo (no se guarda)", "Mot de passe de l’archive (non enregistré)");
        row("Показана только часть содержимого архива.", "Only part of the archive contents is shown.", "Nur ein Teil des Archivinhalts wird angezeigt.", "Solo se muestra parte del contenido del archivo.", "Seule une partie du contenu de l’archive est affichée.");
        row("Показать список файлов архива, читая ограниченные метаданные через выбранный шлюз. Без распаковки и запуска файлов.", "List archive files by reading limited metadata through the selected gateway. No extraction or execution.", "Archivdateien durch begrenztes Lesen von Metadaten über das ausgewählte Gateway anzeigen. Kein Entpacken oder Ausführen.", "Mostrar archivos leyendo metadatos limitados por la puerta seleccionada. Sin extraer ni ejecutar.", "Afficher les fichiers en lisant des métadonnées limitées via la passerelle sélectionnée. Aucune extraction ni exécution.");
        row("При редактировании пустые поля сохраняют прежний доступ, если адрес и тип не изменены. Для замены отметьте флажок. Для SFTP укажите проверенный отпечаток сервера. Пароль не добавляется в URL.", "When editing, blank fields keep existing access if the origin and type are unchanged. Check the box to replace credentials. SFTP requires a verified server fingerprint. Passwords are not added to URLs.", "Beim Bearbeiten behalten leere Felder den Zugang, wenn Ursprung und Typ gleich bleiben. Zum Ersetzen das Kästchen markieren. SFTP benötigt einen geprüften Serverfingerabdruck. Passwörter werden nicht in URLs eingefügt.", "Al editar, los campos vacíos conservan el acceso si el origen y tipo no cambian. Marque la casilla para reemplazar credenciales. SFTP requiere una huella verificada. No se añaden contraseñas a las URL.", "Lors de la modification, les champs vides conservent l’accès si l’origine et le type ne changent pas. Cochez la case pour remplacer les identifiants. SFTP exige une empreinte vérifiée. Les mots de passe ne sont pas ajoutés aux URL.");
        row("Проверка: ", "Verification: ", "Prüfung: ", "Verificación: ", "Vérification : ");
        row("Продолжено: ", "Resumed: ", "Fortgesetzt: ", "Reanudado: ", "Repris : ");
        row("Просмотр архива не выполнен: %s", "Archive preview failed: %s", "Archivvorschau fehlgeschlagen: %s", "Falló la vista del archivo: %s", "Échec de la prévisualisation : %s");
        row("Просмотр завершён без распаковки файлов.", "Preview completed without extracting files.", "Vorschau ohne Entpacken abgeschlossen.", "Vista completada sin extraer archivos.", "Prévisualisation terminée sans extraction.");
        row("Просмотр читает только ограниченные метаданные. Архив не распаковывается; файлы не запускаются. Список может быть неполным, если формат или шифрование не позволяют прочитать заголовки.", "Preview reads only limited metadata. It does not extract or execute files. The list may be incomplete if the format or encryption prevents reading the headers.", "Die Vorschau liest nur begrenzte Metadaten. Sie entpackt keine Dateien und führt nichts aus. Die Liste kann unvollständig sein, wenn Format oder Verschlüsselung das Lesen der Köpfe verhindern.", "La vista solo lee metadatos limitados. No extrae ni ejecuta archivos. La lista puede estar incompleta si el formato o cifrado impide leer las cabeceras.", "La prévisualisation ne lit que des métadonnées limitées. Elle n’extrait ni n’exécute de fichiers. La liste peut être incomplète si le format ou le chiffrement empêche la lecture des en-têtes.");
        row("Профили сайтов", "Site profiles", "Website-Profile", "Perfiles de sitios", "Profils de sites");
        row("Профили сайтов…", "Site profiles…", "Website-Profile…", "Perfiles de sitios…", "Profils de sites…");
        row("Профиль сайта", "Site profile", "Website-Profil", "Perfil de sitio", "Profil de site");
        row("Профиль хранит адрес сайта, папки, выбранные шлюзы и доступ. Секреты не показываются в списке и не записываются в настройки окна.", "A profile stores the site URL, folders, selected gateways and access. Secrets are not displayed in lists or stored in window settings.", "Ein Profil speichert Website-URL, Ordner, Gateways und Zugang. Geheimnisse erscheinen weder in Listen noch in Fenstereinstellungen.", "Un perfil guarda URL, carpetas, puertas y acceso. Los secretos no aparecen en listas ni ajustes de la ventana.", "Un profil conserve l’URL, les dossiers, les passerelles et l’accès. Les secrets ne sont ni affichés dans les listes ni stockés dans les réglages de fenêtre.");
        row("Сжатый размер", "Compressed size", "Komprimierte Größe", "Tamaño comprimido", "Taille compressée");
        row("Содержимое архива", "Archive contents", "Archivinhalt", "Contenido del archivo", "Contenu de l’archive");
        row("Содержимое архива…", "Archive contents…", "Archivinhalt…", "Contenido del archivo…", "Contenu de l’archive…");
        row("Создано: ", "Created: ", "Erstellt: ", "Creado: ", "Créé : ");
        row("Удалить выбранный профиль и его учётные данные?", "Delete the selected profile and its credentials?", "Ausgewähltes Profil und seine Zugangsdaten löschen?", "¿Eliminar el perfil seleccionado y sus credenciales?", "Supprimer le profil sélectionné et ses identifiants ?");
        row("Удалить запись", "Remove entry", "Eintrag entfernen", "Eliminar entrada", "Supprimer l’entrée");
        row("Удалить запись об отправке? Локальный и готовый удалённый файл сохранятся.", "Remove the upload entry? Local and completed remote files will be kept.", "Upload-Eintrag entfernen? Lokale und fertige entfernte Dateien bleiben erhalten.", "¿Eliminar la entrada de subida? Se conservan los archivos locales y remotos completados.", "Supprimer l’entrée d’envoi ? Les fichiers locaux et distants terminés seront conservés.");
        row("Удалить профиль", "Delete profile", "Profil löschen", "Eliminar perfil", "Supprimer le profil");
        row("Укажите URL папки FTP/FTPS/SFTP без пароля и параметров", "Enter an FTP/FTPS/SFTP folder URL without passwords or query parameters", "FTP/FTPS/SFTP-Ordner-URL ohne Passwort oder Parameter eingeben", "Introduzca una URL de carpeta FTP/FTPS/SFTP sin contraseñas ni parámetros", "Saisissez l’URL d’un dossier FTP/FTPS/SFTP sans mot de passe ni paramètres");
        row("Формат: %s · Записей: %d · Метаданные: %s из %s · Чтение частей: %s", "Format: %s · Entries: %d · Metadata: %s of %s · Range reading: %s", "Format: %s · Einträge: %d · Metadaten: %s von %s · Bereichslesen: %s", "Formato: %s · Entradas: %d · Metadatos: %s de %s · Lectura por rangos: %s", "Format : %s · Entrées : %d · Métadonnées : %s sur %s · Lecture par plages : %s");
        row("Читаем содержимое архива…", "Reading archive contents…", "Archivinhalt wird gelesen…", "Leyendo contenido del archivo…", "Lecture du contenu de l’archive…");
        row("Шлюзы (через запятую)", "Gateways (comma separated)", "Gateways (kommagetrennt)", "Puertas de enlace (separadas por comas)", "Passerelles (séparées par des virgules)");
        row("Без авторизации", "No authentication", "Ohne Anmeldung", "Sin autenticación", "Sans authentification");
        row("Отправляется", "Uploading", "Wird hochgeladen", "Subiendo", "Envoi en cours");
        row("Расписание и очередь…", "Schedule and queue…", "Zeitplan und Warteschlange…", "Horario y cola…", "Planification et file…");
        row("Приоритет и расписание…", "Priority and schedule…", "Priorität und Zeitplan…", "Prioridad y horario…", "Priorité et planification…");
        row("Запустить позже", "Start later", "Später starten", "Iniciar más tarde", "Démarrer plus tard");
        row("Дата и время запуска", "Start date and time", "Startdatum und Uhrzeit", "Fecha y hora de inicio", "Date et heure de démarrage");
        row("Время указано в часовом поясе компьютера. Выберите часть даты или времени и меняйте её стрелками.", "Times use the computer's time zone. Select part of the date or time and adjust it with the arrow buttons.", "Es gilt die Zeitzone des Computers. Wählen Sie einen Teil des Datums oder der Uhrzeit und ändern Sie ihn mit den Pfeiltasten.", "Se utiliza la zona horaria del equipo. Seleccione una parte de la fecha u hora y ajústela con las flechas.", "Les heures utilisent le fuseau horaire de l’ordinateur. Sélectionnez une partie de la date ou de l’heure et modifiez-la avec les flèches.");
        row("Chrome / Edge…", "Chrome / Edge…", "Chrome / Edge…", "Chrome / Edge…", "Chrome / Edge…");
        row("Очередь ссылок из браузера заполнена. Повторите отправку после создания текущих загрузок.", "The browser link queue is full. Send the link again after the current downloads are created.", "Die Warteschlange für Browserlinks ist voll. Senden Sie den Link erneut, sobald die aktuellen Downloads angelegt wurden.", "La cola de enlaces del navegador está llena. Vuelva a enviar el enlace cuando se hayan creado las descargas actuales.", "La file des liens du navigateur est pleine. Renvoyez le lien après la création des téléchargements en cours.");
        row("Интеграция с Chrome / Edge", "Chrome / Edge integration", "Chrome-/Edge-Integration", "Integración con Chrome / Edge", "Intégration Chrome / Edge");
        row("Разрешить расширению Chrome / Edge передавать ссылки", "Allow the Chrome / Edge extension to send links", "Der Chrome-/Edge-Erweiterung das Senden von Links erlauben", "Permitir que la extensión Chrome / Edge envíe enlaces", "Autoriser l’extension Chrome / Edge à envoyer des liens");
        row("Открыть папку расширения", "Open extension folder", "Erweiterungsordner öffnen", "Abrir carpeta de la extensión", "Ouvrir le dossier de l’extension");
        row("В Chrome откройте chrome://extensions, в Edge — edge://extensions. Включите режим разработчика, нажмите «Загрузить распакованное» и выберите папку расширения. Это нужно один раз. Затем нажимайте «Скачать через MultiRoute» в меню ссылки или кнопку расширения. Приложение должно быть запущено, в том числе в трее. Расширение не читает cookies и не перехватывает остальные загрузки браузера.", "In Chrome open chrome://extensions; in Edge open edge://extensions. Enable Developer mode, choose Load unpacked and select the extension folder. Do this once. Then use Download with MultiRoute in a link menu or the extension button. Keep the desktop application running, including in the tray. The extension does not read cookies or intercept other browser downloads.", "Öffnen Sie in Chrome chrome://extensions und in Edge edge://extensions. Aktivieren Sie den Entwicklermodus, wählen Sie „Entpackte Erweiterung laden“ und den Erweiterungsordner. Dies ist nur einmal nötig. Verwenden Sie danach „Mit MultiRoute herunterladen“ im Linkmenü oder die Erweiterungsschaltfläche. Die Desktop-Anwendung muss laufen, auch im Infobereich. Die Erweiterung liest keine Cookies und fängt keine anderen Browserdownloads ab.", "En Chrome abra chrome://extensions; en Edge, edge://extensions. Active el modo de desarrollador, elija Cargar descomprimida y seleccione la carpeta de la extensión. Solo es necesario una vez. Después use Descargar con MultiRoute en el menú del enlace o el botón de la extensión. Mantenga abierta la aplicación de escritorio, incluso en la bandeja. La extensión no lee cookies ni intercepta otras descargas del navegador.", "Dans Chrome, ouvrez chrome://extensions ; dans Edge, edge://extensions. Activez le mode développeur, choisissez Charger l’extension non empaquetée et sélectionnez le dossier de l’extension. Cette opération n’est nécessaire qu’une fois. Utilisez ensuite Télécharger avec MultiRoute dans le menu d’un lien ou le bouton de l’extension. L’application de bureau doit rester ouverte, y compris dans la zone de notification. L’extension ne lit pas les cookies et n’intercepte pas les autres téléchargements du navigateur.");
        row("Выбрано: %d", "Selected: %d", "Ausgewählt: %d", "Seleccionadas: %d", "Sélection : %d");
        row("Выбор нескольких: Ctrl/Cmd или Shift. Ctrl/Cmd+A — выбрать все видимые строки. Действия с одной загрузкой доступны при выборе одной строки.", "Multiple selection: Ctrl/Cmd or Shift. Ctrl/Cmd+A selects all visible rows. Single-download actions are available when exactly one row is selected.", "Mehrfachauswahl: Strg/Cmd oder Umschalt (Shift). Strg/Cmd+A wählt alle sichtbaren Zeilen. Aktionen für einen einzelnen Download sind verfügbar, wenn genau eine Zeile ausgewählt ist.", "Selección múltiple: Ctrl/Cmd o Mayús (Shift). Ctrl/Cmd+A selecciona todas las filas visibles. Las acciones de una descarga están disponibles cuando se selecciona una sola fila.", "Sélection multiple : Ctrl/Cmd ou Maj (Shift). Ctrl/Cmd+A sélectionne toutes les lignes visibles. Les actions d’un téléchargement sont disponibles lorsqu’une seule ligne est sélectionnée.");
        row("Удалить выбранные (%d)…", "Remove selected (%d)…", "Auswahl entfernen (%d)…", "Eliminar seleccionadas (%d)…", "Supprimer la sélection (%d)…");
        row("Убрать выбранные загрузки (%d) из приложения?", "Remove selected downloads (%d) from the application?", "Ausgewählte Downloads (%d) aus der Anwendung entfernen?", "¿Quitar las descargas seleccionadas (%d) de la aplicación?", "Retirer les téléchargements sélectionnés (%d) de l’application ?");
        row("Без галочки файлы и скачанные части останутся на диске. С галочкой будут удалены только файлы и части выбранных загрузок.", "With the box unchecked, files and downloaded parts stay on disk. When checked, only files and parts belonging to the selected downloads are deleted.", "Ohne Häkchen bleiben Dateien und geladene Teile auf dem Datenträger. Mit Häkchen werden nur die Dateien und Teile der ausgewählten Downloads gelöscht.", "Sin marcar la casilla, los archivos y las partes descargadas permanecen en el disco. Si se marca, solo se borran los archivos y las partes de las descargas seleccionadas.", "Si la case est décochée, les fichiers et parties téléchargées restent sur le disque. Si elle est cochée, seuls les fichiers et parties des téléchargements sélectionnés sont supprimés.");
        row("Удаляем выбранные загрузки: %d…", "Removing selected downloads: %d…", "Ausgewählte Downloads werden entfernt: %d…", "Eliminando las descargas seleccionadas: %d…", "Suppression des téléchargements sélectionnés : %d…");
        row("Удалено загрузок: %d. Файлы и скачанные части сохранены.", "Downloads removed: %d. Files and downloaded parts kept.", "Downloads entfernt: %d. Dateien und geladene Teile bleiben erhalten.", "Descargas eliminadas: %d. Se conservaron los archivos y las partes descargadas.", "Téléchargements supprimés : %d. Fichiers et parties téléchargées conservés.");
        row("Удалено загрузок: %d. Файлы и скачанные части удалены.", "Downloads removed: %d. Files and downloaded parts deleted.", "Downloads entfernt: %d. Dateien und geladene Teile wurden gelöscht.", "Descargas eliminadas: %d. Se borraron los archivos y las partes descargadas.", "Téléchargements supprimés : %d. Fichiers et parties téléchargées supprimés.");
        row("Удалено: %d из %d. Не удалось удалить: %d.", "Removed: %d of %d. Failed to remove: %d.", "Entfernt: %d von %d. Entfernen fehlgeschlagen: %d.", "Eliminadas: %d de %d. No se pudieron eliminar: %d.", "Supprimés : %d sur %d. Échecs de suppression : %d.");
        // CAT_ROWS: source keys intentionally match UI prose, never user-provided data.
    }
}
