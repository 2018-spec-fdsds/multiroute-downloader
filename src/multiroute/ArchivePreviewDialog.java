package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** Metadata-only archive listing. This dialog never extracts or executes archive contents. */
final class ArchivePreviewDialog {
    @FunctionalInterface interface Inspector { Map<String,Object> inspect(Map<String,Object> input)throws Exception; }
    final JDialog dialog;
    final JTable table;
    final JTextArea message=new JTextArea(3,64);
    final JTextArea budget=new JTextArea(2,64);
    final JPasswordField password=new JPasswordField(20);
    final JButton inspectButton=button("Обновить");
    private static final String[] HEADERS={"Имя","Размер","Сжатый размер","Тип","Зашифрован"};
    private final DefaultTableModel model=new DefaultTableModel(HEADERS,0){@Override public boolean isCellEditable(int row,int column){return false;}@Override public Class<?> getColumnClass(int column){return column==1||column==2?Long.class:Object.class;}};
    private final Inspector inspector;
    private final Map<String,Object> original;
    private final List<Map<String,Object>> entries=new ArrayList<>();
    private SwingWorker<Map<String,Object>,Void> worker;
    private Map<String,Object> lastResult;
    private final Runnable languageListener=this::refreshLanguage;
    private boolean closed;

    static void open(JFrame owner,Map<String,Object> input){ArchivePreviewDialog view=new ArchivePreviewDialog(owner,input,ArchivePreview::inspect);view.start();view.dialog.setVisible(true);}
    ArchivePreviewDialog(JFrame owner,Map<String,Object> input,Inspector inspector){
        this.inspector=inspector;this.original=new LinkedHashMap<>(input);original.remove("password");
        dialog=new JDialog(owner,false);I18n.title(dialog,"Содержимое архива");dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(8,10));root.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));
        JTextField url=new JTextField(Main.str(input.get("url")),70);url.setEditable(false);root.add(url,BorderLayout.NORTH);
        table=new JTable(model){@Override public String getToolTipText(MouseEvent event){if(event.getX()<0||event.getY()<0)return null;int view=rowAtPoint(event.getPoint());if(view<0||view>=getRowCount())return null;int row=convertRowIndexToModel(view);return "<html>"+DesktopUI.wrapTooltipText(Main.str(entries.get(row).get("path")))+"</html>";}};
        table.setAutoCreateRowSorter(true);table.setFillsViewportHeight(true);table.setRowHeight(30);
        DefaultTableCellRenderer plain=new DefaultTableCellRenderer();plain.putClientProperty("html.disable",Boolean.TRUE);table.setDefaultRenderer(Object.class,plain);
        table.setDefaultRenderer(Long.class,new DefaultTableCellRenderer(){@Override protected void setValue(Object value){setText(DesktopUI.bytes(value instanceof Number number?number.longValue():-1));}});
        table.getColumnModel().getColumn(0).setPreferredWidth(380);root.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(6,8));
        message.setEditable(false);message.setOpaque(false);message.setLineWrap(true);message.setWrapStyleWord(true);message.setFont(UIManager.getFont("Label.font"));
        budget.setEditable(false);budget.setOpaque(false);budget.setLineWrap(true);budget.setWrapStyleWord(true);budget.setFont(UIManager.getFont("Label.font"));
        I18n.text(message,"Просмотр читает только ограниченные метаданные. Архив не распаковывается; файлы не запускаются. Список может быть неполным, если формат или шифрование не позволяют прочитать заголовки.");
        JPanel notes=new JPanel(new BorderLayout(0,5));notes.add(message,BorderLayout.CENTER);notes.add(budget,BorderLayout.SOUTH);footer.add(notes,BorderLayout.NORTH);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.TRAILING,6,0));JLabel passwordLabel=new JLabel();I18n.text(passwordLabel,"Пароль архива (не сохраняется)");controls.add(passwordLabel);controls.add(password);controls.add(inspectButton);JButton close=button("Закрыть");controls.add(close);footer.add(controls,BorderLayout.SOUTH);root.add(footer,BorderLayout.SOUTH);
        inspectButton.addActionListener(event->start());close.addActionListener(event->dialog.dispose());
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent event){closed=true;password.setText("");I18n.removeLanguageListener(languageListener);if(worker!=null)worker.cancel(true);}});
        dialog.setContentPane(root);ClipboardSupport.install(root);I18n.refresh(dialog);dialog.setMinimumSize(new Dimension(720,380));dialog.setSize(980,600);dialog.setLocationRelativeTo(owner);
        I18n.addLanguageListener(languageListener);refreshLanguage();
    }
    void start(){
        if(closed||worker!=null&&!worker.isDone())return;
        Map<String,Object> input=new LinkedHashMap<>(original);char[] secret=password.getPassword();if(secret.length>0)input.put("password",secret);
        inspectButton.setEnabled(false);I18n.text(budget,"Читаем содержимое архива…");
        worker=new SwingWorker<>(){
            protected Map<String,Object> doInBackground()throws Exception{try{return inspector.inspect(input);}finally{Arrays.fill(secret,'\0');input.remove("password");}}
            protected void done(){if(closed)return;inspectButton.setEnabled(true);try{showResult(get());}catch(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();I18n.text(budget,"Просмотр архива не выполнен: %s",DiagnosticText.arg(Main.str(cause.getMessage())));}}
        };worker.execute();
    }
    void showResult(Map<String,Object> result){
        lastResult=result;
        entries.clear();model.setRowCount(0);
        if(result.get("entries") instanceof List<?> rows)for(Object raw:rows){Map<String,Object> entry=Main.obj(raw);entries.add(entry);model.addRow(new Object[]{Main.str(entry.get("name")).isBlank()?Main.str(entry.get("path")):Main.str(entry.get("name")),Main.num(entry.get("size"),-1),Main.num(entry.get("compressed_size"),-1),I18n.t(Boolean.TRUE.equals(entry.get("directory"))?"Папка":"Файл"),I18n.t(Boolean.TRUE.equals(entry.get("encrypted"))?"Да":"Нет")});}
        String detail=DiagnosticText.render(Main.str(result.get("message")));
        if(Boolean.TRUE.equals(result.get("truncated")))detail=I18n.t("Показана только часть содержимого архива.")+" "+detail;
        if(Boolean.TRUE.equals(result.get("password_required")))detail=I18n.t("Для чтения заголовков нужен пароль архива.")+" "+detail;
        I18n.text(message,detail.isBlank()?"Просмотр завершён без распаковки файлов.":"%s",detail.isBlank()?new Object[]{}:new Object[]{detail});
        I18n.text(budget,"Формат: %s · Записей: %d · Метаданные: %s из %s · Чтение частей: %s",Main.str(result.get("format")),entries.size(),DesktopUI.bytes(Main.num(result.get("bytes_fetched"),0)),DesktopUI.bytes(Main.num(result.get("metadata_budget_bytes"),0)),I18n.t(Boolean.TRUE.equals(result.get("range_supported"))?"Да":"Нет"));
    }
    private void refreshLanguage(){
        int selected=table.getSelectedRow();String path=selected<0?null:Main.str(entries.get(table.convertRowIndexToModel(selected)).get("path"));
        for(int index=0;index<HEADERS.length;index++)table.getColumnModel().getColumn(index).setHeaderValue(I18n.t(HEADERS[index]));
        if(lastResult!=null){showResult(lastResult);if(path!=null)for(int index=0;index<entries.size();index++)if(path.equals(Main.str(entries.get(index).get("path")))){int view=table.convertRowIndexToView(index);if(view>=0)table.setRowSelectionInterval(view,view);break;}}
        table.getTableHeader().repaint();
    }
    private static JButton button(String source){JButton value=new JButton();I18n.text(value,source);return value;}
}
