package multiroute;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Native file selection after validated torrent metadata has arrived through a gateway. */
final class TorrentFileDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final List<Map<String,Object>> files = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();
    final FileModel model = new FileModel();
    final JTable table = new JTable(model) {
        @Override public javax.swing.JToolTip createToolTip() {
            javax.swing.JToolTip tip = super.createToolTip();
            tip.putClientProperty("html.disable", true);
            return tip;
        }
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
            if (row < 0 || column <= 0) return null;
            return String.valueOf(model.getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(column)));
        }
    };
    final JLabel totalLabel = new JLabel();
    final JButton allButton = new JButton("Выбрать все файлы");
    final JButton noneButton = new JButton("Снять всё");
    final JButton confirmButton = new JButton("Загрузить выбранные");
    final JButton cancelButton = new JButton("Отмена");
    private final Runnable languageListener = this::languageChanged;
    private List<String> result;

    static List<String> choose(Frame owner, Map<String,Object> inspection) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Show torrent file picker on EDT");
        TorrentFileDialog dialog = new TorrentFileDialog(owner, inspection);
        dialog.setVisible(true);
        return dialog.result;
    }

    TorrentFileDialog(Frame owner, Map<String,Object> inspection) {
        super(owner, I18n.t("BitTorrent — выбрать файлы"), true);
        I18n.title(this, "BitTorrent — выбрать файлы");
        for (Object value : Main.arr(inspection.get("files"))) {
            Map<String,Object> file = Main.obj(value);
            if (!Main.str(file.get("path")).isEmpty()) files.add(file);
        }
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(660, 410));
        setSize(890, 560);
        setLocationRelativeTo(owner);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        JLabel nameLabel = new JLabel();
        nameLabel.putClientProperty("html.disable", true);
        nameLabel.setText(Main.str(inspection.get("name")));
        top.add(nameLabel, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(allButton); controls.add(noneButton);
        top.add(controls, BorderLayout.CENTER);
        top.add(label("Части проверяются по метаданным раздачи. Имена и папки сохраняются."), BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        DefaultTableCellRenderer plainText = new DefaultTableCellRenderer();
        plainText.putClientProperty("html.disable", true);
        table.setDefaultRenderer(String.class, plainText);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(Math.max(24, table.getRowHeight()));
        table.getColumnModel().getColumn(0).setMinWidth(62);
        table.getColumnModel().getColumn(0).setMaxWidth(74);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(10, 6));
        bottom.add(totalLabel, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(cancelButton); actions.add(confirmButton);
        bottom.add(actions, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(confirmButton);
        getRootPane().registerKeyboardAction(event -> cancel(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        I18n.text(allButton, "Выбрать все файлы");
        I18n.text(noneButton, "Снять всё");
        I18n.text(confirmButton, "Загрузить выбранные");
        I18n.text(cancelButton, "Отмена");
        allButton.addActionListener(event -> {
            for (Map<String,Object> file : files)
                if (!Boolean.TRUE.equals(file.get("pad"))) selected.add(Main.str(file.get("path")));
            selectionChanged();
        });
        noneButton.addActionListener(event -> { selected.clear(); selectionChanged(); });
        confirmButton.addActionListener(event -> {
            if (selected.isEmpty()) return;
            result = selectedPaths(); dispose();
        });
        cancelButton.addActionListener(event -> cancel());
        List<Map<String,Object>> regular = files.stream().filter(file -> !Boolean.TRUE.equals(file.get("pad"))).toList();
        if (regular.size() == 1) selected.add(Main.str(regular.get(0).get("path")));
        I18n.addLanguageListener(languageListener);
        languageChanged();
    }

    @Override public void dispose() {
        I18n.removeLanguageListener(languageListener);
        super.dispose();
    }

    private static JLabel label(String source, Object... args) {
        JLabel label = new JLabel();
        I18n.text(label, source, args);
        return label;
    }

    private void languageChanged() {
        I18n.refresh(this);
        for (int column = 0; column < model.getColumnCount(); column++)
            table.getColumnModel().getColumn(column).setHeaderValue(model.getColumnName(column));
        int checkboxWidth = Math.max(74, table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
                .stringWidth(model.getColumnName(0)) + 18);
        table.getColumnModel().getColumn(0).setMaxWidth(checkboxWidth);
        table.getColumnModel().getColumn(0).setPreferredWidth(checkboxWidth);
        table.getColumnModel().getColumn(0).setWidth(checkboxWidth);
        table.getTableHeader().repaint();
        selectionChanged();
    }

    List<String> selectedPaths() {
        List<String> paths = new ArrayList<>();
        for (Map<String,Object> file : files) {
            String path = Main.str(file.get("path"));
            if (selected.contains(path) && !Boolean.TRUE.equals(file.get("pad"))) paths.add(path);
        }
        return List.copyOf(paths);
    }

    long selectedBytes() {
        long total = 0;
        for (Map<String,Object> file : files)
            if (selected.contains(Main.str(file.get("path"))) && !Boolean.TRUE.equals(file.get("pad")))
                total = Math.addExact(total, Math.max(0, Main.num(file.get("size"), 0)));
        return total;
    }

    private void cancel() { result = null; selected.clear(); dispose(); }

    private void selectionChanged() {
        model.fireTableDataChanged();
        confirmButton.setEnabled(!selected.isEmpty());
        I18n.text(totalLabel, "Выбрано: %d файлов · %s", selected.size(), formatBytes(selectedBytes()));
    }

    private static String formatBytes(long size) {
        if (size < 1024) return I18n.t("%d Б", size);
        if (size < 1024L * 1024) return I18n.t("%.1f КиБ", size / 1024.0);
        if (size < 1024L * 1024 * 1024) return I18n.t("%.1f МиБ", size / (1024.0 * 1024));
        return I18n.t("%.2f ГиБ", size / (1024.0 * 1024 * 1024));
    }

    final class FileModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columns = {"Выбрать", "Файл", "Размер", "Тип"};
        @Override public int getRowCount() { return files == null ? 0 : files.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return I18n.t(columns[column]); }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) {
            return column == 0 && !Boolean.TRUE.equals(files.get(row).get("pad"));
        }
        @Override public Object getValueAt(int row, int column) {
            Map<String,Object> file = files.get(row);
            return switch (column) {
                case 0 -> selected.contains(Main.str(file.get("path")));
                case 1 -> Main.str(file.get("path"));
                case 2 -> formatBytes(Main.num(file.get("size"), 0));
                default -> I18n.t(Boolean.TRUE.equals(file.get("pad")) ? "Служебное заполнение" : "Файл");
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            if (row < 0 || row >= files.size() || !isCellEditable(row, column)) return;
            String path = Main.str(files.get(row).get("path"));
            if (Boolean.TRUE.equals(value)) selected.add(path); else selected.remove(path);
            selectionChanged();
        }
    }
}
