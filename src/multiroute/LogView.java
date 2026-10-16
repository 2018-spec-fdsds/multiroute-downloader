package multiroute;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

/** Log refreshes preserve the reader's viewport independently of the text caret. */
final class LogView extends JScrollPane {
    private boolean updating, suppressCaretScroll, loaded;
    private long generation, interaction;
    private Point observed = new Point();
    private String previous = "";
    final JTextArea text = new JTextArea() {
        @Override public void scrollRectToVisible(Rectangle rectangle) {
            if (!suppressCaretScroll) super.scrollRectToVisible(rectangle);
        }
    };

    LogView() {
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.putClientProperty("html.disable", Boolean.TRUE);
        ((DefaultCaret) text.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        setViewportView(text);
        getViewport().addChangeListener(event -> {
            Point position = getViewport().getViewPosition();
            if (!updating && !position.equals(observed)) interaction++;
            observed = position;
        });
        addMouseWheelListener(event -> userAction());
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { userAction(); }
            @Override public void mouseDragged(MouseEvent event) { userAction(); }
        };
        text.addMouseListener(mouse);
        text.addMouseMotionListener(mouse);
        getVerticalScrollBar().addMouseListener(mouse);
        getHorizontalScrollBar().addMouseListener(mouse);
        text.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) { userAction(); }
        });
    }

    private void userAction() {
        interaction++;
        suppressCaretScroll = false;
    }

    void updateSnapshot(String snapshot) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Update log on the EDT");
        if (loaded && previous.equals(snapshot)) return;
        Point position = getViewport().getViewPosition();
        int dot = text.getCaret().getDot(), mark = text.getCaret().getMark();
        boolean follow = !loaded || (dot == mark && !getVerticalScrollBar().getValueIsAdjusting()
                && getVerticalScrollBar().getValue() + getVerticalScrollBar().getVisibleAmount()
                        >= getVerticalScrollBar().getMaximum() - 2);
        int anchor = text.viewToModel2D(position);
        double anchorOffset = 0;
        try {
            Rectangle2D bounds = text.modelToView2D(anchor);
            if (bounds != null) anchorOffset = position.y - bounds.getY();
        } catch (BadLocationException ignored) { }
        String old = previous;
        int removed = removedPrefix(old, snapshot);
        int newDot = mapOffset(dot, old, snapshot, removed);
        int newMark = mapOffset(mark, old, snapshot, removed);
        int newAnchor = mapOffset(anchor, old, snapshot, removed);
        suppressCaretScroll = true;
        updating = true;
        try {
            if (loaded && snapshot.startsWith(old)) text.append(snapshot.substring(old.length()));
            else {
                text.setText(snapshot);
                text.getCaret().setDot(newMark);
                text.getCaret().moveDot(newDot);
            }
            getViewport().setViewPosition(position);
        } finally { updating = false; }
        previous = snapshot;
        loaded = true;
        long revision = ++generation, activity = interaction;
        double pixelOffset = anchorOffset;
        SwingUtilities.invokeLater(() -> {
            if (revision != generation) return;
            suppressCaretScroll = false;
            if (activity != interaction || getVerticalScrollBar().getValueIsAdjusting()) return;
            updating = true;
            try {
                getViewport().doLayout();
                int y = position.y;
                if (follow) y = getViewport().getViewSize().height;
                else if (!snapshot.startsWith(old)) {
                    Rectangle2D bounds = text.modelToView2D(Math.min(newAnchor, text.getDocument().getLength()));
                    if (bounds != null) y = (int) Math.round(bounds.getY() + pixelOffset);
                }
                int maxY = Math.max(0, getViewport().getViewSize().height - getViewport().getExtentSize().height);
                int maxX = Math.max(0, getViewport().getViewSize().width - getViewport().getExtentSize().width);
                getViewport().setViewPosition(new Point(Math.min(position.x, maxX), Math.max(0, Math.min(y, maxY))));
            } catch (BadLocationException ignored) {
                // A shorter replacement still remains within its viewport bounds.
            } finally { updating = false; }
        });
    }

    /** Preserve surviving event/selection offsets when the bounded history drops old rows. */
    private static int removedPrefix(String old, String next) {
        if (next.startsWith(old)) return 0;
        int newline = next.indexOf('\n');
        if (newline >= 0) {
            int removed = old.indexOf(next.substring(0, newline + 1));
            if (removed >= 0 && next.startsWith(old.substring(removed))) return removed;
        }
        return -1;
    }

    private static int mapOffset(int offset, String old, String next, int removed) {
        if (removed >= 0) return Math.min(next.length(), Math.max(0, offset - removed));
        int line = 0, start = 0;
        for (int i = 0; i < Math.min(offset, old.length()); i++) if (old.charAt(i) == '\n') { line++; start = i + 1; }
        int nextStart = 0;
        for (int i = 0; i < line; i++) {
            int end = next.indexOf('\n', nextStart);
            if (end < 0) return next.length();
            nextStart = end + 1;
        }
        int end = next.indexOf('\n', nextStart);
        return Math.min(end < 0 ? next.length() : end, nextStart + offset - start);
    }

    void stop() { generation++; suppressCaretScroll = false; }
}
