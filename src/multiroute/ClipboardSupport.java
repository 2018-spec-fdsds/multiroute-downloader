package multiroute;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/** Paste belongs to the focused editable field; the clipboard is read only on a paste action. */
public final class ClipboardSupport {
    private static final String ACTION = "multiroute.paste";
    private static final String URL = "multiroute.clipboard.url";
    private static final String PROVIDER = "multiroute.clipboard.provider";
    private static final String ERROR_HANDLER = "multiroute.clipboard.error";

    @FunctionalInterface interface TextProvider { String read() throws Exception; }

    private ClipboardSupport() { }

    public static void install(Component root) {
        if (root instanceof JTextComponent field && field.isEditable()) install(field, false);
        if (root instanceof Container container)
            for (Component child : container.getComponents()) install(child);
    }

    public static void installUrl(JTextComponent field) { install(field, true); }

    private static void install(JTextComponent field, boolean url) {
        if (url) field.putClientProperty(URL, Boolean.TRUE);
        field.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), ACTION);
        int nativeModifier;
        try { nativeModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); }
        catch (RuntimeException noDesktop) { nativeModifier = InputEvent.CTRL_DOWN_MASK; }
        field.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, nativeModifier), ACTION);
        // Also explicitly register Command for macOS; this does not create a global shortcut.
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac"))
            field.getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), ACTION);
        field.getActionMap().put(ACTION, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { paste(field, false); }
        });
    }

    static void provider(JTextComponent field, TextProvider provider) {
        field.putClientProperty(PROVIDER, Objects.requireNonNull(provider));
    }

    static void clipboard(JTextComponent field, Clipboard clipboard) {
        provider(field, () -> stringFrom(clipboard));
    }

    static void errors(JTextComponent field, Consumer<String> errors) {
        field.putClientProperty(ERROR_HANDLER, errors);
    }

    static void paste(JTextComponent field, boolean replaceAll) {
        if (!field.isEditable() || !field.isEnabled()) return;
        try {
            TextProvider provider = field.getClientProperty(PROVIDER) instanceof TextProvider configured
                    ? configured : () -> stringFrom(Toolkit.getDefaultToolkit().getSystemClipboard());
            String value = provider.read();
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Буфер обмена пуст");
            if (Boolean.TRUE.equals(field.getClientProperty(URL))) value = trimUrlBoundary(value);
            if (value.isBlank()) throw new IllegalArgumentException("Буфер обмена пуст");
            if (replaceAll) field.selectAll();
            field.replaceSelection(value);
            field.requestFocusInWindow();
        } catch (Exception error) {
            String reason = error.getMessage();
            if (reason == null || reason.isBlank()) reason = error.getClass().getSimpleName();
            String message = I18n.tFor("ru", "Не удалось вставить URL: %s", reason);
            if (field.getClientProperty(ERROR_HANDLER) instanceof Consumer<?> consumer) {
                @SuppressWarnings("unchecked") Consumer<String> handler = (Consumer<String>) consumer;
                handler.accept(message);
            } else UIManager.getLookAndFeel().provideErrorFeedback(field);
        }
    }

    private static String stringFrom(Clipboard clipboard) throws Exception {
        if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
            throw new IllegalArgumentException("В буфере обмена нет текста");
        Object value = clipboard.getData(DataFlavor.stringFlavor);
        if (!(value instanceof String text)) throw new IllegalArgumentException("В буфере обмена нет текста");
        return text;
    }

    private static String trimUrlBoundary(String value) {
        int start = 0, end = value.length();
        while (start < end && boundarySpace(value.charAt(start))) start++;
        while (end > start && boundarySpace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean boundarySpace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
