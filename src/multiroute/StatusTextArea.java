package multiroute;

import java.awt.Rectangle;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

/** Selectable status text whose background updates never scroll an enclosing form to its caret. */
final class StatusTextArea extends JTextArea {
    private static final long serialVersionUID = 1L;
    private boolean replacingText;

    StatusTextArea(int rows, int columns) {
        super(rows, columns);
        DefaultCaret caret = new DefaultCaret() {
            private static final long serialVersionUID = 1L;
            @Override protected void adjustVisibility(Rectangle location) {
                // Suppress before DefaultCaret queues its asynchronous scroll callback.
                // Arrow keys, Home/End and explicit navigation outside setText still scroll normally.
                if (!replacingText) super.adjustVisibility(location);
            }
        };
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        caret.setBlinkRate(UIManager.getInt("TextArea.caretBlinkRate"));
        setCaret(caret);
    }

    @Override public void setText(String value) {
        String next = value == null ? "" : value;
        if (next.equals(getText())) return;
        boolean previous = replacingText;
        replacingText = true;
        try { super.setText(next); }
        finally { replacingText = previous; }
    }
}
