package multiroute;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Protocol-neutral file preview for Metalink and WebDAV listings. */
final class SourceFileDialog {
    private SourceFileDialog(){}
    static List<String> choose(JFrame owner,Map<String,Object> inspection){
        AtomicReference<List<String>> result=new AtomicReference<>();JDialog dialog=new JDialog(owner,true);I18n.title(dialog,"Выбрать файлы источника");
        List<Map<String,Object>> files=new ArrayList<>();for(Object raw:Main.arr(inspection.get("files")))files.add(Main.obj(raw));
        DefaultTableModel model=new DefaultTableModel(new Object[]{I18n.t("Выбрать"),I18n.t("Файл"),I18n.t("Размер"),"SHA-256"},0){
            @Override public boolean isCellEditable(int row,int col){return col==0;}@Override public Class<?> getColumnClass(int col){return col==0?Boolean.class:String.class;}};
        for(Map<String,Object> file:files){long bytes=Main.num(file.get("size"),0);model.addRow(new Object[]{false,Main.str(file.get("path")),Long.toString(bytes),Main.str(file.get("sha256"))});}
        JTable table=new JTable(model);table.setAutoCreateRowSorter(true);DefaultTableCellRenderer plain=new DefaultTableCellRenderer();plain.putClientProperty("html.disable",true);table.setDefaultRenderer(String.class,plain);
        table.getColumnModel().getColumn(0).setPreferredWidth(75);table.getColumnModel().getColumn(1).setPreferredWidth(360);table.getColumnModel().getColumn(2).setPreferredWidth(100);table.getColumnModel().getColumn(3).setPreferredWidth(200);
        JButton all=new JButton(),none=new JButton(),download=new JButton(),cancel=new JButton();I18n.text(all,"Выбрать все");I18n.text(none,"Снять всё");I18n.text(download,"Загрузить выбранные");I18n.text(cancel,"Отмена");
        all.addActionListener(e->{for(int i=0;i<model.getRowCount();i++)model.setValueAt(true,i,0);});none.addActionListener(e->{for(int i=0;i<model.getRowCount();i++)model.setValueAt(false,i,0);});
        download.setEnabled(false);model.addTableModelListener(e->{boolean selected=false;for(int i=0;i<model.getRowCount();i++)selected|=Boolean.TRUE.equals(model.getValueAt(i,0));download.setEnabled(selected);});
        download.addActionListener(e->{if(table.isEditing())table.getCellEditor().stopCellEditing();List<String> selected=new ArrayList<>();for(int i=0;i<files.size();i++)if(Boolean.TRUE.equals(model.getValueAt(i,0)))selected.add(Main.str(files.get(i).get("path")));if(selected.isEmpty())return;result.set(selected);dialog.dispose();});
        cancel.addActionListener(e->dialog.dispose());JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(all);buttons.add(none);buttons.add(cancel);buttons.add(download);
        JPanel body=new JPanel(new BorderLayout(8,8));body.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));JLabel note=new JLabel();I18n.text(note,"Проверьте имена, размеры и опубликованные контрольные суммы.");body.add(note,BorderLayout.NORTH);body.add(new JScrollPane(table),BorderLayout.CENTER);body.add(buttons,BorderLayout.SOUTH);
        dialog.setContentPane(body);dialog.setSize(900,480);dialog.setMinimumSize(new Dimension(650,350));dialog.setLocationRelativeTo(owner);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);dialog.setVisible(true);return result.get();
    }
}
