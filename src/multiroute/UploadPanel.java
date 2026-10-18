package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import javax.swing.*;
import javax.swing.table.*;

/** Native upload queue. Local files are read only after explicit file selection. */
final class UploadPanel extends JPanel implements AutoCloseable {
    @FunctionalInterface interface RouteProvider {List<Map<String,Object>> prepare()throws Exception;}
    @FunctionalInterface interface FileSelector {List<Path> choose(JFrame owner);}
    interface Backend {
        Map<String,Object> create(Map<String,Object> input)throws Exception;
        Map<String,Object> state();
        Map<String,Object> pause(String id)throws Exception;
        Map<String,Object> resume(String id)throws Exception;
        Map<String,Object> remove(String id)throws Exception;
    }
    private static final String[] HEADERS={"Файл","Состояние","Прогресс","Скорость","Прошло","Осталось","Шлюз"};
    final JComboBox<Map<String,Object>> profileCombo=SiteProfilesDialog.combo();
    final JTextField targetField=new JTextField(70);
    final JButton profilesButton=button("Профили сайтов…"),addButton=button("Выбрать файлы и отправить…"),pauseButton=button("Пауза"),resumeButton=button("Продолжить"),removeButton=button("Удалить запись");
    final JLabel summary=new JLabel(),selectedLabel=new JLabel();
    private final DefaultTableModel model=new DefaultTableModel(HEADERS,0){@Override public boolean isCellEditable(int row,int column){return false;}@Override public Class<?> getColumnClass(int column){return column==2?DownloadProgress.class:Object.class;}};
    final JTable table;
    final JScrollPane scroll;
    final LogView detailsView=new LogView(false);
    private final Map<String,Map<String,Object>> uploads=new LinkedHashMap<>();
    private final Backend manager;
    private final JFrame owner;
    private final RouteProvider routeProvider;
    private final Consumer<Map<String,Object>> applyProfileGateways;
    private final Consumer<String> errors;
    private final FileSelector fileSelector;
    private final ExecutorService background=Executors.newCachedThreadPool(task->{Thread thread=new Thread(task,"desktop-upload");thread.setDaemon(true);return thread;});
    private final AtomicBoolean refreshing=new AtomicBoolean();
    private final javax.swing.Timer timer;
    private boolean closing,editing,refreshingProfiles,updating;
    private String selectedId="",detailsId="";

    UploadPanel(UploadManager manager,JFrame owner,RouteProvider routes,Consumer<Map<String,Object>> applyGateways,Consumer<String> errors){
        this(backend(manager),owner,routes,applyGateways,errors,UploadPanel::chooseFiles);
    }
    UploadPanel(UploadManager manager,JFrame owner,RouteProvider routes,Consumer<Map<String,Object>> applyGateways,Consumer<String> errors,FileSelector fileSelector){
        this(backend(manager),owner,routes,applyGateways,errors,fileSelector);
    }
    private static Backend backend(UploadManager value){return new Backend(){
        public Map<String,Object> create(Map<String,Object> input)throws Exception{return value.create(input);}
        public Map<String,Object> state(){return value.state();}
        public Map<String,Object> pause(String id)throws Exception{return value.pause(id);}
        public Map<String,Object> resume(String id)throws Exception{return value.resume(id);}
        public Map<String,Object> remove(String id)throws Exception{return value.remove(id);}
    };}
    /** Owned UI fixtures capture dispatch; protocol integration is tested by UploadManager separately. */
    UploadPanel(Backend manager,JFrame owner,RouteProvider routes,Consumer<Map<String,Object>> applyGateways,Consumer<String> errors,FileSelector fileSelector){
        super(new BorderLayout(10,10));this.manager=manager;this.owner=owner;routeProvider=routes;applyProfileGateways=applyGateways;this.errors=errors;this.fileSelector=fileSelector;
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top=new JPanel(new BorderLayout(8,9));JPanel profileLine=new JPanel(new BorderLayout(7,0));profileLine.add(profileCombo,BorderLayout.CENTER);profileLine.add(profilesButton,BorderLayout.LINE_END);top.add(profileLine,BorderLayout.NORTH);
        JPanel destination=new JPanel(new BorderLayout(7,5));JLabel label=new JLabel();I18n.text(label,"Папка на сервере — URL FTP/FTPS/SFTP");destination.add(label,BorderLayout.NORTH);destination.add(targetField,BorderLayout.CENTER);destination.add(addButton,BorderLayout.LINE_END);top.add(destination,BorderLayout.CENTER);
        JTextArea note=new JTextArea(2,60);note.setEditable(false);note.setOpaque(false);note.setLineWrap(true);note.setWrapStyleWord(true);note.setFont(UIManager.getFont("Label.font"));I18n.text(note,"Выберите локальные файлы явно. Отправки используют выбранные шлюзы; несколько файлов могут идти одновременно. Существующие файлы на сервере не заменяются. Передача одного файла идёт через один шлюз за попытку.");top.add(note,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        table=new JTable(model){@Override public String getToolTipText(MouseEvent event){if(event.getX()<0||event.getY()<0)return null;Map<String,Object> row=atViewRow(rowAtPoint(event.getPoint()));return row==null?null:tooltip(row);}};
        table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFillsViewportHeight(true);table.setRowHeight(32);
        DefaultTableCellRenderer plain=new DefaultTableCellRenderer();plain.putClientProperty("html.disable",Boolean.TRUE);table.setDefaultRenderer(Object.class,plain);table.setDefaultRenderer(DownloadProgress.class,new DownloadProgress.Renderer());
        int[] widths={230,120,160,120,90,110,140};for(int index=0;index<widths.length;index++)table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        scroll=new JScrollPane(table);scroll.setMinimumSize(new Dimension(200,130));
        JPanel detail=new JPanel(new BorderLayout(7,7));JPanel heading=new JPanel(new BorderLayout(8,5));heading.add(selectedLabel,BorderLayout.NORTH);heading.add(summary,BorderLayout.CENTER);JPanel actions=new JPanel(new FlowLayout(FlowLayout.TRAILING));for(JButton control:List.of(pauseButton,resumeButton,removeButton))actions.add(control);heading.add(actions,BorderLayout.SOUTH);detail.add(heading,BorderLayout.NORTH);detail.add(detailsView,BorderLayout.CENTER);detail.setMinimumSize(new Dimension(200,160));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,scroll,detail);split.setResizeWeight(.60);split.setDividerLocation(260);split.setBorder(null);add(split,BorderLayout.CENTER);
        selectedLabel.putClientProperty("html.disable",Boolean.TRUE);summary.putClientProperty("html.disable",Boolean.TRUE);
        table.getSelectionModel().addListSelectionListener(event->{if(!updating&&!event.getValueIsAdjusting())select();});
        addButton.addActionListener(event->addSelectedFiles());pauseButton.addActionListener(event->action("pause"));resumeButton.addActionListener(event->action("resume"));removeButton.addActionListener(event->action("remove"));
        profilesButton.addActionListener(event->{String id=SiteProfilesDialog.choose(owner,SiteProfilesDialog.selectedId(profileCombo));refreshProfiles(id==null?SiteProfilesDialog.selectedId(profileCombo):id);if(id!=null)applyProfile();});
        profileCombo.addActionListener(event->{if(!refreshingProfiles)applyProfile();});refreshProfiles("");
        ClipboardSupport.install(this);timer=new javax.swing.Timer(900,event->refresh());timer.start();refreshLanguage();refresh();
    }
    void refreshProfiles(String id){refreshingProfiles=true;try{SiteProfilesDialog.refresh(profileCombo,id);}catch(Exception error){errors.accept(message(error));}finally{refreshingProfiles=false;}}
    private void applyProfile(){
        Object raw=profileCombo.getSelectedItem();if(!(raw instanceof Map<?,?>)||Main.str(Main.obj(raw).get("id")).isBlank())return;
        Map<String,Object> profile=Main.obj(raw);targetField.setText(profileDirectory(profile));applyProfileGateways.accept(profile);
    }
    static String profileDirectory(Map<String,Object> profile){
        String base=Main.str(profile.get("base_url")),directory=Main.str(profile.get("remote_directory"));
        if(base.isBlank())return "";
        try{URI uri=URI.create(base);String path=directory.isBlank()?uri.getPath():directory.startsWith("/")?directory:(Main.str(uri.getPath()).endsWith("/")?Main.str(uri.getPath()):Main.str(uri.getPath())+"/")+directory;if(path.isBlank())path="/";if(!path.endsWith("/"))path+="/";return new URI(uri.getScheme(),null,uri.getHost(),uri.getPort(),path,null,null).toASCIIString();}
        catch(Exception invalid){return base;}
    }
    static String targetUrl(String directory,Path file)throws Exception{
        URI uri=URI.create(directory.strip());String scheme=Main.str(uri.getScheme()).toLowerCase(Locale.ROOT);
        if(!List.of("ftp","ftps","sftp").contains(scheme)||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("Укажите URL папки FTP/FTPS/SFTP без пароля и параметров");
        String path=Main.str(uri.getPath());if(!path.endsWith("/"))path+="/";path+=file.getFileName().toString();
        return new URI(scheme,null,uri.getHost(),uri.getPort(),path,null,null).toASCIIString();
    }
    void addSelectedFiles(){
        if(closing||editing)return;String destination=targetField.getText().strip();String profile=SiteProfilesDialog.selectedId(profileCombo);
        try{targetUrl(destination,Path.of("file"));}catch(Exception error){errors.accept(message(error));return;}
        List<Path> files=fileSelector.choose(owner);if(files==null||files.isEmpty())return;
        editing=true;addButton.setEnabled(false);
        background.execute(()->{int count=0;try{
            List<Map<String,Object>> routes=routeProvider.prepare();
            for(Path file:files){Map<String,Object> input=new LinkedHashMap<>();input.put("local_path",file.toAbsolutePath().normalize().toString());input.put("target_url",targetUrl(destination,file));input.put("routes",routes);input.put("overwrite",false);if(!profile.isBlank())input.put("site_profile_id",profile);manager.create(input);count++;}
            int added=count;SwingUtilities.invokeLater(()->{if(!closing)I18n.text(summary,"Добавлено отправок: %d",added);});
        }catch(Exception error){int added=count;SwingUtilities.invokeLater(()->{if(!closing)errors.accept(I18n.t("Добавлено отправок: %d. Ошибка: %s",added,DiagnosticText.arg(message(error))));});}
        finally{SwingUtilities.invokeLater(()->{if(!closing){editing=false;addButton.setEnabled(true);refresh();}});}});
    }
    private static List<Path> chooseFiles(JFrame owner){JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);chooser.setDialogTitle(I18n.t("Выберите файлы для отправки"));ClipboardSupport.install(chooser);if(chooser.showOpenDialog(owner)!=JFileChooser.APPROVE_OPTION)return List.of();return Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();}
    void refresh(){if(closing||!refreshing.compareAndSet(false,true))return;background.execute(()->{try{Map<String,Object> state=manager.state();SwingUtilities.invokeLater(()->{if(!closing)update(state);});}catch(Exception error){SwingUtilities.invokeLater(()->{if(!closing)errors.accept(message(error));});}finally{refreshing.set(false);}});}
    void update(Map<String,Object> state){
        Point position=scroll.getViewport().getViewPosition();String keep=selectedId;updating=true;uploads.clear();model.setRowCount(0);int selected=-1;long speed=0;int active=0,complete=0;
        if(state.get("uploads") instanceof List<?> rows)for(Object raw:rows){Map<String,Object> row=Main.obj(raw);String id=Main.str(row.get("id"));uploads.put(id,row);String status=Main.str(row.get("status"));long bytes=Main.num(row.get("uploaded_bytes"),0),size=Main.num(row.get("size"),0),rate=Math.max(0,Main.num(row.get("speed_bps"),0));
            if(List.of("queued","running","retrying","verifying").contains(status)){active++;speed=Long.MAX_VALUE-speed<rate?Long.MAX_VALUE:speed+rate;}if(status.equals("complete"))complete++;
            model.addRow(new Object[]{Main.str(row.get("filename")),statusName(status),new DownloadProgress(size,bytes,status,Main.str(row.get("error"))),DesktopTray.formatSpeed(rate),duration(Main.num(row.get("elapsed_ms"),0)),remaining(row),Main.str(row.get("active_gateway"))});if(id.equals(keep))selected=model.getRowCount()-1;
        }
        if(selected<0&&model.getRowCount()>0)selected=model.getRowCount()-1;int view=selected<0?-1:table.convertRowIndexToView(selected);if(view>=0)table.setRowSelectionInterval(view,view);updating=false;select();
        scroll.getViewport().setViewPosition(new Point(Math.max(0,Math.min(position.x,Math.max(0,table.getPreferredSize().width-scroll.getViewport().getWidth()))),Math.max(0,Math.min(position.y,Math.max(0,table.getPreferredSize().height-scroll.getViewport().getHeight())))));
        I18n.text(summary,"Отправок: %d · Активных: %d · Завершено: %d · Общая скорость: %s",uploads.size(),active,complete,DesktopTray.formatSpeed(speed));
    }
    private void select(){Map<String,Object> row=atViewRow(table.getSelectedRow());selectedId=row==null?"":Main.str(row.get("id"));render(row);}
    private Map<String,Object> atViewRow(int view){if(view<0||view>=table.getRowCount())return null;int row=table.convertRowIndexToModel(view);List<Map<String,Object>> all=new ArrayList<>(uploads.values());return row<all.size()?all.get(row):null;}
    static String tooltip(Map<String,Object> row){return "<html><b>"+DesktopUI.escapeTooltip(I18n.t("Полный путь:"))+"</b><br>"+DesktopUI.wrapTooltipText(Main.str(row.get("local_path")))+"<br><br><b>"+DesktopUI.escapeTooltip(I18n.t("Адрес назначения:"))+"</b><br>"+DesktopUI.wrapTooltipText(Main.str(row.get("target_url")))+"</html>";}
    private void render(Map<String,Object> row){String state=row==null?"":Main.str(row.get("status"));pauseButton.setEnabled(!editing&&List.of("queued","running","retrying","verifying").contains(state));resumeButton.setEnabled(!editing&&List.of("paused","failed").contains(state));removeButton.setEnabled(!editing&&row!=null);
        if(row==null){detailsId="";selectedLabel.setToolTipText(null);I18n.text(selectedLabel,"Выберите отправку");detailsView.updateSnapshot(I18n.t("Выберите отправку для просмотра локального пути, адреса назначения и результата проверки."));return;}
        I18n.text(selectedLabel,"%s",Main.str(row.get("filename"))+" · "+statusName(state));selectedLabel.setToolTipText(tooltip(row));
        StringBuilder detail=new StringBuilder();detail.append(I18n.t("Локальный файл: ")).append(Main.str(row.get("local_path"))).append('\n');detail.append(I18n.t("Адрес назначения: ")).append(Main.str(row.get("target_url"))).append('\n');detail.append(I18n.t("Отправлено: %s из %s",DesktopUI.bytes(Main.num(row.get("uploaded_bytes"),0)),DesktopUI.bytes(Main.num(row.get("size"),0)))).append('\n');
        for(String key:List.of("created_at","first_started_at","last_resumed_at","completed_at")){String value=Main.str(row.get(key));if(!value.isBlank()){String label=switch(key){case "created_at"->"Создано: ";case "first_started_at"->"Начало: ";case "last_resumed_at"->"Продолжено: ";default->"Завершено: ";};detail.append(I18n.t(label)).append(DesktopUI.formatHistoryTime(value)).append('\n');}}
        if(!Main.str(row.get("verification")).isBlank())detail.append(I18n.t("Проверка: ")).append(I18n.t("SHA-256 после обратного чтения с сервера")).append('\n');if(!Main.str(row.get("error")).isBlank())detail.append(I18n.t("Ошибка: %s",DiagnosticText.arg(Main.str(row.get("error"))))).append('\n');
        String id=Main.str(row.get("id"));if(id.equals(detailsId))detailsView.updateSnapshot(detail.toString());else detailsView.resetSnapshot(detail.toString());detailsId=id;
    }
    private void action(String action){String id=selectedId;if(id.isBlank()||editing)return;if(action.equals("remove")&&JOptionPane.showConfirmDialog(owner,I18n.t("Удалить запись об отправке? Локальный и готовый удалённый файл сохранятся."),I18n.t("Удалить запись"),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;editing=true;render(uploads.get(id));background.execute(()->{try{switch(action){case "pause"->manager.pause(id);case "resume"->manager.resume(id);default->manager.remove(id);}}catch(Exception error){SwingUtilities.invokeLater(()->{if(!closing)errors.accept(message(error));});}finally{SwingUtilities.invokeLater(()->{if(!closing){editing=false;refresh();}});}});}
    void refreshLanguage(){for(int index=0;index<HEADERS.length;index++)table.getColumnModel().getColumn(index).setHeaderValue(I18n.t(HEADERS[index]));profileCombo.repaint();update(Map.of("uploads",new ArrayList<>(uploads.values())));table.getTableHeader().repaint();}
    void refreshTheme(){table.setDefaultRenderer(DownloadProgress.class,new DownloadProgress.Renderer());refreshLanguage();}
    private static String remaining(Map<String,Object> row){if("complete".equals(row.get("status")))return "00:00:00";long seconds=Main.num(row.get("eta_seconds"),-1);return seconds<0?I18n.t("оценивается…"):duration(seconds*1000);}
    private static String statusName(String value){return value.equals("running")?I18n.t("Отправляется"):DesktopUI.statusName(value);}
    private static String duration(long ms){long seconds=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);}
    private static JButton button(String source){JButton value=new JButton();I18n.text(value,source);return value;}
    private static String message(Throwable error){while(error.getCause()!=null)error=error.getCause();return error.getMessage()==null?error.toString():error.getMessage();}
    @Override public void close(){closing=true;timer.stop();detailsView.stop();background.shutdownNow();}
}
