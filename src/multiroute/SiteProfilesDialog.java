package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Explicit profile editing. Public records never reveal previously saved credentials. */
final class SiteProfilesDialog {
    private SiteProfilesDialog() { }

    static JComboBox<Map<String,Object>> combo() {
        JComboBox<Map<String,Object>> result = new JComboBox<>();
        result.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                Map<String,Object> profile=value instanceof Map<?,?>?Main.obj(value):Map.of();
                String label=Main.str(profile.get("id")).isBlank()?I18n.t("Без профиля")
                        :Main.str(profile.get("name"))+" · "+Main.str(profile.get("base_url"));
                JLabel component=(JLabel)super.getListCellRendererComponent(list,label,index,selected,focus);
                component.putClientProperty("html.disable",Boolean.TRUE);return component;
            }
        });
        result.setPrototypeDisplayValue(Map.of("id","prototype","name","Profile","base_url","sftp://server.example"));
        return result;
    }

    static void refresh(JComboBox<Map<String,Object>> combo,String selected) throws java.io.IOException {
        combo.removeAllItems();combo.addItem(Map.of());
        for(Map<String,Object> profile:SiteProfiles.listPublic())combo.addItem(profile);
        for(int index=0;index<combo.getItemCount();index++)
            if(Main.str(combo.getItemAt(index).get("id")).equals(selected)){combo.setSelectedIndex(index);return;}
        combo.setSelectedIndex(0);
    }

    static String selectedId(JComboBox<Map<String,Object>> combo) {
        return combo.getSelectedItem() instanceof Map<?,?> value?Main.str(value.get("id")):"";
    }

    static String choose(JFrame owner,String selectedId) {
        JDialog dialog=new JDialog(owner,true);I18n.title(dialog,"Профили сайтов");
        JComboBox<Map<String,Object>> profiles=combo();
        try {refresh(profiles,selectedId);}catch(Exception error){showError(dialog,error);return null;}
        JPanel root=new JPanel(new BorderLayout(8,10));root.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        root.add(profiles,BorderLayout.NORTH);
        JTextArea explanation=new JTextArea(4,52);explanation.setEditable(false);explanation.setOpaque(false);explanation.setLineWrap(true);explanation.setWrapStyleWord(true);
        I18n.text(explanation,"Профиль хранит адрес сайта, папки, выбранные шлюзы и доступ. Секреты не показываются в списке и не записываются в настройки окна.");root.add(explanation,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(0,3,6,6));
        JButton add=button("Новый профиль…"),edit=button("Изменить…"),remove=button("Удалить профиль"),choose=button("Выбрать профиль"),close=button("Закрыть");
        for(JButton control:List.of(add,edit,remove,choose,close))actions.add(control);root.add(actions,BorderLayout.SOUTH);
        String[] result={null};
        add.addActionListener(event->{Map<String,Object> saved=edit(dialog,Map.of());if(saved!=null)refreshSafely(dialog,profiles,Main.str(saved.get("id")));});
        edit.addActionListener(event->{Object item=profiles.getSelectedItem();if(item instanceof Map<?,?> raw&&!Main.str(raw.get("id")).isBlank()){
            Map<String,Object> saved=edit(dialog,Main.obj(item));if(saved!=null)refreshSafely(dialog,profiles,Main.str(saved.get("id")));}});
        remove.addActionListener(event->{String id=selectedId(profiles);if(id.isBlank())return;
            if(JOptionPane.showConfirmDialog(dialog,I18n.t("Удалить выбранный профиль и его учётные данные?"),I18n.t("Удалить профиль"),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            remove.setEnabled(false);new SwingWorker<Void,Void>(){protected Void doInBackground() throws Exception{SiteProfiles.delete(id);return null;}
                protected void done(){remove.setEnabled(true);try{get();refreshSafely(dialog,profiles,"");}catch(Exception error){showError(dialog,error);}}}.execute();});
        choose.addActionListener(event->{result[0]=selectedId(profiles);dialog.dispose();});close.addActionListener(event->dialog.dispose());
        dialog.setContentPane(root);I18n.refresh(dialog);dialog.pack();dialog.setMinimumSize(new Dimension(650,230));dialog.setLocationRelativeTo(owner);dialog.setVisible(true);return result[0];
    }

    private static void refreshSafely(Component owner,JComboBox<Map<String,Object>> profiles,String id) {
        try{refresh(profiles,id);}catch(Exception error){showError(owner,error);}
    }

    static Map<String,Object> edit(Component owner,Map<String,Object> existing) {
        Window window=owner instanceof Window direct?direct:SwingUtilities.getWindowAncestor(owner);JDialog dialog=new JDialog(window,Dialog.ModalityType.APPLICATION_MODAL);
        I18n.title(dialog,existing.isEmpty()?"Новый профиль":"Изменить профиль");
        JPanel fields=new JPanel(new GridBagLayout());
        JTextField name=field(existing,"name"),url=field(existing,"base_url"),local=field(existing,"download_directory"),remote=field(existing,"remote_directory"),gateways=new JTextField(42),username=new JTextField(42),fingerprint=field(existing,"host_key_sha256");
        if(existing.get("gateways") instanceof List<?> values)gateways.setText(String.join(", ",values.stream().map(Main::str).toList()));
        JComboBox<String> type=new JComboBox<>(new String[]{"none","basic","bearer","cookie","headers","ftp","sftp"});
        type.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){String kind=Main.str(value);return super.getListCellRendererComponent(list,kind.equals("none")?I18n.t("Без авторизации"):kind,index,selected,focus);}});
        type.setSelectedItem(existing.getOrDefault("type",existing.getOrDefault("auth_type","none")));
        JPasswordField password=new JPasswordField(42),token=new JPasswordField(42),cookie=new JPasswordField(42);
        JTextArea headers=new JTextArea(3,42);JCheckBox replace=new JCheckBox();I18n.text(replace,"Заменить сохранённые учётные данные");
        String[] labels={"Название","Адрес сайта","Папка для скачивания","Папка на сервере","Шлюзы (через запятую)","Тип доступа","Имя пользователя","Пароль","Bearer токен","Cookie","Заголовки (Name: value)","Отпечаток SFTP сервера (SHA256:…)"};
        String[] keys={"name","base_url","download_directory","remote_directory","gateways","type","username","password","token","cookie","headers","host_key_sha256"};
        JComponent[] inputs={name,url,local,remote,gateways,type,username,password,token,cookie,headers,fingerprint};
        for(int index=0;index<inputs.length;index++){inputs[index].setName("site-profile-"+keys[index]);inputs[index].getAccessibleContext().setAccessibleName(I18n.t(labels[index]));}
        JPanel localPicker=new JPanel(new BorderLayout(6,0));localPicker.add(local,BorderLayout.CENTER);JButton browse=button("Обзор…");localPicker.add(browse,BorderLayout.LINE_END);
        browse.addActionListener(event->{JFileChooser chooser=new JFileChooser();chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);String current=local.getText().strip();if(!current.isBlank())chooser.setSelectedFile(new java.io.File(current));if(chooser.showOpenDialog(dialog)==JFileChooser.APPROVE_OPTION)local.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());});
        JComponent[] controls={name,url,localPicker,remote,gateways,type,username,password,token,cookie,new JScrollPane(headers),fingerprint};
        for(int index=0;index<controls.length;index++){
            GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=index*2;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.LINE_START;c.insets=new Insets(3,4,3,4);
            JLabel label=new JLabel();I18n.text(label,labels[index]);fields.add(label,c);c.gridy++;fields.add(controls[index],c);
        }
        GridBagConstraints tail=new GridBagConstraints();tail.gridy=controls.length*2;tail.weightx=1;tail.fill=GridBagConstraints.HORIZONTAL;tail.insets=new Insets(8,4,8,4);fields.add(replace,tail);
        JTextArea note=new JTextArea(3,52);note.setEditable(false);note.setOpaque(false);note.setLineWrap(true);note.setWrapStyleWord(true);
        I18n.text(note,"При редактировании пустые поля сохраняют прежний доступ, если адрес и тип не изменены. Для замены отметьте флажок. Для SFTP укажите проверенный отпечаток сервера. Пароль не добавляется в URL.");
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(new JScrollPane(fields),BorderLayout.CENTER);root.add(note,BorderLayout.NORTH);
        JButton save=button("Сохранить"),cancel=button("Отмена");JPanel actions=new JPanel(new FlowLayout(FlowLayout.TRAILING));actions.add(save);actions.add(cancel);root.add(actions,BorderLayout.SOUTH);
        Runnable enabled=()->{String selected=Main.str(type.getSelectedItem());boolean user=List.of("basic","ftp","sftp").contains(selected);username.setEnabled(user);password.setEnabled(user);token.setEnabled(selected.equals("bearer"));cookie.setEnabled(selected.equals("cookie"));headers.setEnabled(selected.equals("headers"));fingerprint.setEnabled(selected.equals("sftp"));};type.addActionListener(event->enabled.run());enabled.run();
        java.util.concurrent.atomic.AtomicReference<Map<String,Object>> result=new java.util.concurrent.atomic.AtomicReference<>();
        save.addActionListener(event->{
            Map<String,Object> input=new LinkedHashMap<>();if(!existing.isEmpty())input.put("id",Main.str(existing.get("id")));
            input.put("name",name.getText().strip());input.put("base_url",url.getText().strip());input.put("download_directory",local.getText().strip());input.put("remote_directory",remote.getText().strip());
            input.put("gateways",Arrays.stream(gateways.getText().split("[,;\\s]+" )).map(String::strip).filter(value->!value.isBlank()).toList());input.put("type",Main.str(type.getSelectedItem()));
            if(replace.isSelected())input.put("replace_credentials",true);
            if(username.isEnabled()&&!username.getText().isBlank())input.put("username",username.getText());
            putSecret(input,"password",password);putSecret(input,"token",token);putSecret(input,"cookie",cookie);
            if(fingerprint.isEnabled()&&!fingerprint.getText().isBlank())input.put("host_key_sha256",fingerprint.getText().strip());
            if(headers.isEnabled()&&!headers.getText().isBlank()){
                Map<String,Object> values=new LinkedHashMap<>();for(String line:headers.getText().split("\\R")){if(line.isBlank())continue;int colon=line.indexOf(':');if(colon<=0){showError(dialog,new IllegalArgumentException("Заголовок должен иметь формат Name: value"));return;}values.put(line.substring(0,colon).strip(),line.substring(colon+1).strip());}input.put("headers",values);
            }
            save.setEnabled(false);new SwingWorker<Map<String,Object>,Void>(){
                protected Map<String,Object> doInBackground()throws Exception{return SiteProfiles.save(input);}
                protected void done(){save.setEnabled(true);try{result.set(get());dialog.dispose();}catch(Exception error){showError(dialog,error);}}
            }.execute();
        });cancel.addActionListener(event->dialog.dispose());
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent event){password.setText("");token.setText("");cookie.setText("");headers.setText("");}});
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);dialog.setContentPane(root);ClipboardSupport.install(root);I18n.refresh(dialog);dialog.setSize(740,680);dialog.setLocationRelativeTo(owner);dialog.setVisible(true);return result.get();
    }
    private static void putSecret(Map<String,Object> input,String key,JPasswordField field){if(!field.isEnabled())return;char[] value=field.getPassword();try{if(value.length>0)input.put(key,new String(value));}finally{Arrays.fill(value,'\0');}}
    private static JTextField field(Map<String,Object> value,String key){return new JTextField(Main.str(value.get(key)),42);}
    private static JButton button(String key){JButton value=new JButton();I18n.text(value,key);return value;}
    private static void showError(Component owner,Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();JOptionPane.showMessageDialog(owner,DiagnosticText.render(cause.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}
}
