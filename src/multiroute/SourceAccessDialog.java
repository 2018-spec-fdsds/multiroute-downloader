package multiroute;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Explicit source-only credentials. Desktop settings never contain their secret values. */
final class SourceAccessDialog {
    private SourceAccessDialog(){}
    static void open(JFrame owner,String url,java.util.function.Consumer<String> error){
        JPanel panel=new JPanel(new GridBagLayout());JTextField origin=new JTextField(url,38),name=new JTextField(38),username=new JTextField(38),fingerprint=new JTextField(38);
        JPasswordField password=new JPasswordField(38),token=new JPasswordField(38),cookie=new JPasswordField(38);JTextArea headers=new JTextArea(4,38);
        JComboBox<String> type=new JComboBox<>(new String[]{"basic","bearer","cookie","headers","sftp","ftp"});
        JComboBox<Map<String,Object>> profiles=new JComboBox<>();
        try { for(Map<String,Object> row:SourceAuth.listPublic())profiles.addItem(row); }
        catch(Exception failure){error.accept(failure.getMessage()==null?failure.toString():failure.getMessage());return;}
        profiles.setRenderer(new DefaultListCellRenderer(){@Override public java.awt.Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
            Map<String,Object> row=value instanceof Map<?,?>?Main.obj(value):Map.of();JLabel label=(JLabel)super.getListCellRendererComponent(list,Main.str(row.get("name"))+" "+Main.str(row.get("origin")),index,selected,focus);label.putClientProperty("html.disable",true);return label;}});
        JComponent[] controls={profiles,origin,name,type,username,password,token,cookie,new JScrollPane(headers),fingerprint};
        String[] labels={"Сохранённые источники","URL или точный адрес источника","Название","Тип доступа","Имя пользователя","Пароль","Bearer токен","Cookie","Заголовки (Name: value)","Отпечаток SFTP сервера (SHA256:…)"};
        for(int i=0;i<controls.length;i++){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=i;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(4,4,4,8);JLabel label=new JLabel();I18n.text(label,labels[i]);panel.add(label,c);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;panel.add(controls[i],c);}
        JTextArea explanation=new JTextArea(4,48);explanation.setEditable(false);explanation.setOpaque(false);explanation.setLineWrap(true);explanation.setWrapStyleWord(true);
        I18n.text(explanation,"Доступ действует только для точного протокола, хоста и порта источника. При перенаправлении секреты не передаются другому источнику. Для SFTP сверяйте отпечаток сервера по доверенному каналу. Пароли не выводятся в журнал и настройки окна.");
        JPanel body=new JPanel(new BorderLayout(8,8));body.add(panel,BorderLayout.CENTER);body.add(explanation,BorderLayout.SOUTH);ClipboardSupport.install(body);
        Runnable enabled=()->{String selected=Main.str(type.getSelectedItem());username.setEnabled(List.of("basic","sftp","ftp").contains(selected));password.setEnabled(username.isEnabled());token.setEnabled("bearer".equals(selected));cookie.setEnabled("cookie".equals(selected));headers.setEnabled("headers".equals(selected));fingerprint.setEnabled("sftp".equals(selected));};
        type.addActionListener(e->enabled.run());enabled.run();
        Object[] options={I18n.t("Сохранить"),I18n.t("Удалить сохранённый доступ"),I18n.t("Отмена")};
        try{while(true){int action=JOptionPane.showOptionDialog(owner,new JScrollPane(body),I18n.t("Доступ к источнику…"),JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
            if(action<0||action==2)return;
            try{
                if(action==1){Object selected=profiles.getSelectedItem();if(selected instanceof Map<?,?> row){SourceAuth.delete(Main.str(row.get("id")));profiles.removeItem(selected);}continue;}
                Map<String,Object> record=new LinkedHashMap<>();record.put("origin",origin.getText().strip());record.put("name",name.getText().strip());record.put("type",Main.str(type.getSelectedItem()));
                if(username.isEnabled()){record.put("username",username.getText());record.put("password",new String(password.getPassword()));}
                if(token.isEnabled())record.put("token",new String(token.getPassword()));if(cookie.isEnabled())record.put("cookie",new String(cookie.getPassword()));
                if(headers.isEnabled()){Map<String,Object> values=new LinkedHashMap<>();for(String line:headers.getText().split("\\R")){if(line.isBlank())continue;int colon=line.indexOf(':');if(colon<=0)throw new IllegalArgumentException("Заголовок должен иметь формат Name: value");values.put(line.substring(0,colon).strip(),line.substring(colon+1).strip());}record.put("headers",values);}
                if(fingerprint.isEnabled())record.put("host_key_sha256",fingerprint.getText().strip());SourceAuth.save(record);return;
            }catch(Exception ex){JOptionPane.showMessageDialog(owner,DiagnosticText.render(ex.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}
        }}finally{password.setText("");token.setText("");cookie.setText("");headers.setText("");}
    }
}
