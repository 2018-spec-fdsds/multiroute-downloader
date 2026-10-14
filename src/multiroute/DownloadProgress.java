package multiroute;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/** A row's own transfer progress; rendering never depends on the selected job. */
record DownloadProgress(long size, long received, String status, String error)
        implements Comparable<DownloadProgress> {
    DownloadProgress {
        received = Math.max(0, received);
        status = status == null ? "" : status;
        error = error == null ? "" : error;
    }

    static DownloadProgress from(Map<String,Object> job) {
        return new DownloadProgress(Main.num(job.get("size"), 0),
                Main.num(job.get("progress_bytes"), Main.num(job.get("downloaded"), 0)),
                Main.str(job.get("status")), Main.str(job.get("error")));
    }

    boolean determinate() {
        return size > 0 || "complete".equals(status) || "seeding".equals(status);
    }

    int perMille() {
        if (size <= 0) return determinate() ? 1000 : 0;
        if (received >= size) return 1000;
        // Avoid long overflow and never round an incomplete transfer to 100%.
        return Math.min(999, (int) (1000.0 * received / size));
    }

    String text() {
        return determinate() ? String.format(I18n.locale(), "%.1f%%", perMille() / 10.0)
                : received > 0 ? DesktopUI.bytes(received) : "—";
    }

    String tooltip() {
        String text = size > 0 || size == 0 && determinate()
                ? I18n.t("Загружено: %s из %s · %s", DesktopUI.bytes(received),
                        DesktopUI.bytes(size), DesktopUI.statusName(status))
                : I18n.t("Получено: %s · размер неизвестен · %s", DesktopUI.bytes(received),
                        DesktopUI.statusName(status));
        return error.isBlank() ? text : text + " · " + I18n.t("Причина: %s", DesktopUI.conciseError(error, 360));
    }

    @Override public int compareTo(DownloadProgress other) {
        int known = Boolean.compare(determinate(), other.determinate());
        if (known != 0) return known;
        if (!determinate()) return Long.compare(received, other.received);
        double own = size <= 0 ? 1 : Math.min(1, (double) received / size);
        double next = other.size <= 0 ? 1 : Math.min(1, (double) other.received / other.size);
        return Double.compare(own, next);
    }

    @Override public String toString() { return text(); }

    static final class Renderer extends JPanel implements TableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 1000);

        Renderer() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            bar.setStringPainted(true);
            add(bar, BorderLayout.CENTER);
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            DownloadProgress progress = value instanceof DownloadProgress item ? item
                    : new DownloadProgress(0, 0, "", "");
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            bar.setFont(table.getFont());
            bar.setForeground(UIManager.getColor("ProgressBar.foreground"));
            bar.setBackground(UIManager.getColor("ProgressBar.background"));
            // JTable reuses this component. Unknown totals get bytes, not an invented percentage.
            bar.setIndeterminate(false);
            bar.setValue(progress.perMille());
            bar.setString(progress.text());
            String tooltip = progress.tooltip();
            setToolTipText(tooltip);
            bar.setToolTipText(tooltip);
            bar.getAccessibleContext().setAccessibleName(Main.str(table.getValueAt(row,
                    table.convertColumnIndexToView(0))) + " · " + tooltip);
            return this;
        }
    }
}
