package multiroute;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** User-controlled list preview; only the confirmed list is submitted to the queue. */
final class BulkImportDialog {
    private BulkImportDialog(){}
    static List<String> parse(String text){
        if(text.length()>1048576)throw new IllegalArgumentException("Список ссылок превышает 1 МиБ");
        LinkedHashSet<String> urls=new LinkedHashSet<>();
        for(String line:text.split("\\R")){String value=line.strip();if(value.isEmpty()||value.startsWith("#"))continue;
            if(!(value.matches("(?i)(https?|ftp|ftps|sftp|webdav|webdavs)://\\S+")||value.startsWith("magnet:?")||value.matches("(?i)file:.*\\.torrent")))throw new IllegalArgumentException("Некорректная ссылка: "+value);
            if(value.contains("\u0000"))throw new IllegalArgumentException("Некорректная ссылка");urls.add(value);
            if(urls.size()>1000)throw new IllegalArgumentException("Не более 1000 ссылок за один импорт");}
        return List.copyOf(urls);
    }
    static List<String> choose(JFrame owner,String initial){
        AtomicReference<List<String>> result=new AtomicReference<>();JDialog dialog=new JDialog(owner,true);I18n.title(dialog,"Список ссылок…");
        JTextArea input=new JTextArea(initial==null?"":initial,14,74);input.setLineWrap(false);ClipboardSupport.install(input);
        JPanel body=new JPanel(new BorderLayout(8,10));body.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JLabel heading=new JLabel();I18n.text(heading,"Одна ссылка на строку. Дубликаты исключаются. До 1000 ссылок.");body.add(heading,BorderLayout.NORTH);body.add(new JScrollPane(input),BorderLayout.CENTER);
        JButton txt=new JButton(),preview=new JButton(),cancel=new JButton();I18n.text(txt,"Импорт TXT…");I18n.text(preview,"Проверить список и добавить");I18n.text(cancel,"Отмена");
        txt.addActionListener(e->{JFileChooser chooser=new JFileChooser();chooser.setFileFilter(new FileNameExtensionFilter("TXT","txt"));if(chooser.showOpenDialog(dialog)!=JFileChooser.APPROVE_OPTION)return;
            try{Path path=chooser.getSelectedFile().toPath();if(Files.size(path)>1048576)throw new IllegalArgumentException("Список ссылок превышает 1 МиБ");String text=Files.readString(path);input.append((input.getText().isBlank()?"":"\n")+text);}
            catch(Exception error){JOptionPane.showMessageDialog(dialog,DiagnosticText.render(error.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}});
        preview.addActionListener(e->{try{List<String> urls=parse(input.getText());if(urls.isEmpty())return;
            JTextArea rows=new JTextArea(String.join("\n",urls),Math.min(14,urls.size()),68);rows.setEditable(false);rows.putClientProperty("html.disable",true);
            if(JOptionPane.showConfirmDialog(dialog,new JScrollPane(rows),I18n.t("Добавить %d загрузок?",urls.size()),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){result.set(urls);dialog.dispose();}}
            catch(Exception error){JOptionPane.showMessageDialog(dialog,DiagnosticText.render(error.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}});
        cancel.addActionListener(e->dialog.dispose());JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.add(txt);actions.add(cancel);actions.add(preview);body.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(body);dialog.pack();dialog.setMinimumSize(new Dimension(600,360));dialog.setLocationRelativeTo(owner);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);dialog.setVisible(true);return result.get();
    }
}
