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

/** Five desktop languages; bindings always retain the source template and raw arguments. */
public final class I18n {
    private static final List<String> CODES = List.of("ru", "en", "de", "es", "fr");
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
        if (ROWS.putIfAbsent(ru, new String[]{ru, en, de, es, fr}) != null)
            throw new IllegalStateException("Duplicate localization key: " + ru);
    }
    private static void catalog() {
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
        // CAT_ROWS: source keys intentionally match UI prose, never user-provided data.
    }
}
