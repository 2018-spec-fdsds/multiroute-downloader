package multiroute;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

/** Native repository file selection. Metadata is fetched before the dialog opens. */
final class HubFileDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Pattern SPLIT_GGUF = Pattern.compile("^(.*)-(\\d{5})-of-(\\d{5})(\\.gguf)$", Pattern.CASE_INSENSITIVE);
    private final List<Map<String,Object>> files;
    private final Map<String,Map<String,Object>> byPath = new LinkedHashMap<>();
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
    final JComboBox<String> filter = new JComboBox<>(new String[]{"Только GGUF", "Все файлы"});
    final JLabel totalLabel = new JLabel();
    final JButton allButton = new JButton("Выбрать показанные");
    final JButton noneButton = new JButton("Снять всё");
    final JButton confirmButton = new JButton("Загрузить выбранные");
    final JButton cancelButton = new JButton("Отмена");
    private final TableRowSorter<FileModel> sorter = new TableRowSorter<>(model);
    private final Runnable languageListener = this::languageChanged;
    private boolean updatingFilter;
    private List<String> result;

    static List<String> choose(Frame owner, Map<String,Object> inspection) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Show file picker on the EDT");
        HubFileDialog dialog = new HubFileDialog(owner, inspection);
        dialog.setVisible(true);
        return dialog.result;
    }

    /** Package visibility permits owned hidden table/cancel tests without showing a window. */
    HubFileDialog(Frame owner, Map<String,Object> inspection) {
        super(owner, I18n.t("Hugging Face — выбрать файлы"), true);
        I18n.title(this, "Hugging Face — выбрать файлы");
        files = new ArrayList<>();
        for (Object value : Main.arr(inspection.get("files"))) {
            Map<String,Object> file = Main.obj(value);
            String path = Main.str(file.get("path"));
            if (!path.isEmpty() && !byPath.containsKey(path)) {
                files.add(file);
                byPath.put(path, file);
            }
        }
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 440));
        setSize(1040, 630);
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { if (result == null) selected.clear(); }
        });
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        String revision = Main.str(inspection.get("revision"));
        JLabel source = label("%s  ·  версия %s", Main.str(inspection.get("repo_id")),
                revision.length() > 12 ? revision.substring(0, 12) : revision);
        source.putClientProperty("html.disable", true);
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(source, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(label("Показать:"));
        controls.add(filter);
        controls.add(allButton);
        controls.add(noneButton);
        top.add(controls, BorderLayout.CENTER);
        top.add(label("Выберите нужную квантизацию. Части одного GGUF выбираются вместе."), BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer plainText = new DefaultTableCellRenderer();
        plainText.putClientProperty("html.disable", true);
        table.setDefaultRenderer(String.class, plainText);
        table.setRowHeight(Math.max(24, table.getRowHeight()));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(62);
        table.getColumnModel().getColumn(1).setPreferredWidth(440);
        table.getColumnModel().getColumn(2).setPreferredWidth(112);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(490);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 6));
        bottom.add(totalLabel, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(cancelButton);
        actions.add(confirmButton);
        bottom.add(actions, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(confirmButton);
        getRootPane().registerKeyboardAction(event -> cancel(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

        I18n.text(allButton, "Выбрать показанные");
        I18n.text(noneButton, "Снять всё");
        I18n.text(confirmButton, "Загрузить выбранные");
        I18n.text(cancelButton, "Отмена");
        filter.addActionListener(event -> { if (!updatingFilter) applyFilter(); });
        allButton.addActionListener(event -> {
            for (int row = 0; row < table.getRowCount(); row++)
                selectPath(Main.str(files.get(table.convertRowIndexToModel(row)).get("path")), true);
            selectionChanged();
        });
        noneButton.addActionListener(event -> { selected.clear(); selectionChanged(); });
        confirmButton.addActionListener(event -> {
            if (selected.isEmpty()) return;
            result = selectedPaths();
            dispose();
        });
        cancelButton.addActionListener(event -> cancel());
        boolean ggufExists = files.stream().anyMatch(file -> Main.str(file.get("path")).toLowerCase(Locale.ROOT).endsWith(".gguf"));
        filter.setSelectedIndex(ggufExists ? 0 : 1);
        applyFilter();
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
        int selectedIndex = filter.getSelectedIndex();
        updatingFilter = true;
        try {
            filter.removeAllItems();
            filter.addItem(I18n.t("Только GGUF"));
            filter.addItem(I18n.t("Все файлы"));
            filter.setSelectedIndex(selectedIndex < 0 ? 0 : selectedIndex);
        } finally { updatingFilter = false; }
        for (int column = 0; column < model.getColumnCount(); column++)
            table.getColumnModel().getColumn(column).setHeaderValue(model.getColumnName(column));
        int checkboxWidth = Math.max(62, table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
                .stringWidth(model.getColumnName(0)) + 18);
        table.getColumnModel().getColumn(0).setPreferredWidth(checkboxWidth);
        table.getColumnModel().getColumn(0).setWidth(checkboxWidth);
        table.getTableHeader().repaint();
        selectionChanged();
    }

    List<String> selectedPaths() {
        List<String> resultPaths = new ArrayList<>();
        for (Map<String,Object> file : files) {
            String path = Main.str(file.get("path"));
            if (selected.contains(path)) resultPaths.add(path);
        }
        return List.copyOf(resultPaths);
    }

    long selectedBytes() {
        long bytes = 0;
        for (String path : selected) bytes = Math.addExact(bytes, Math.max(0, Main.num(byPath.get(path).get("size"), 0)));
        return bytes;
    }

    private void cancel() {
        result = null;
        selected.clear();
        dispose();
    }

    private void applyFilter() {
        sorter.setRowFilter(filter.getSelectedIndex() == 0 ? new RowFilter<FileModel,Integer>() {
            @Override public boolean include(Entry<? extends FileModel,? extends Integer> entry) {
                return Main.str(files.get(entry.getIdentifier()).get("path")).toLowerCase(Locale.ROOT).endsWith(".gguf");
            }
        } : null);
    }

    private void selectPath(String path, boolean include) {
        String group = splitGroup(path);
        for (String candidate : byPath.keySet()) {
            if (!candidate.equals(path) && (group == null || !group.equals(splitGroup(candidate)))) continue;
            if (include) selected.add(candidate);
            else selected.remove(candidate);
        }
    }

    private static String splitGroup(String path) {
        Matcher split = SPLIT_GGUF.matcher(path);
        if (!split.matches()) return null;
        return split.group(1) + "-of-" + split.group(3) + split.group(4).toLowerCase(Locale.ROOT);
    }

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
        private final String[] columns = {"Выбрать", "Файл", "Размер", "Хранилище", "SHA-256 источника"};
        @Override public int getRowCount() { return files == null ? 0 : files.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return I18n.t(columns[column]); }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        @Override public Object getValueAt(int row, int column) {
            Map<String,Object> file = files.get(row);
            return switch (column) {
                case 0 -> selected.contains(Main.str(file.get("path")));
                case 1 -> Main.str(file.get("path"));
                case 2 -> formatBytes(Main.num(file.get("size"), 0));
                case 3 -> Main.str(file.get("storage")).toUpperCase(Locale.ROOT);
                default -> Main.str(file.get("sha256")).isEmpty() ? I18n.t("Не опубликована") : Main.str(file.get("sha256"));
            };
        }
        @Override public void setValueAt(Object value, int row, int column) {
            if (column != 0 || row < 0 || row >= files.size()) return;
            selectPath(Main.str(files.get(row).get("path")), Boolean.TRUE.equals(value));
            selectionChanged();
        }
    }
}
