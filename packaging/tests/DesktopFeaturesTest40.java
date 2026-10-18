package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.*;

/** Owned profile/upload/archive fixture. Transfer dispatch is captured; no external source is contacted. */
public final class DesktopFeaturesTest40 {
    static int checks;
    static final List<String> cases=new ArrayList<>(),images=new ArrayList<>();
    static final String NAME="Файл_日本語_한국어_é_🚀.bin";
    interface Task{void run()throws Exception;}
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void edt(Task task)throws Exception{AtomicReference<Throwable> failure=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{try{task.run();}catch(Throwable error){failure.set(error);}});if(failure.get() instanceof Exception error)throw error;if(failure.get() instanceof Error error)throw error;}
    static void waitFor(java.util.function.BooleanSupplier condition,String why)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(!condition.getAsBoolean()&&System.nanoTime()<deadline){Thread.sleep(25);edt(()->{});}check(condition.getAsBoolean(),why);edt(()->{});}
    static void layout(Container parent){parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container nested)layout(nested);}
    static List<Component> children(Component parent){List<Component> result=new ArrayList<>();result.add(parent);if(parent instanceof Container container)for(Component child:container.getComponents())result.addAll(children(child));return result;}
    static JTextField input(JDialog dialog,String key){return (JTextField)children(dialog).stream().filter(value->("site-profile-"+key).equals(value.getName())).findFirst().orElseThrow();}
    static JButton button(Container owner,String source){return children(owner).stream().filter(value->value instanceof JButton item&&item.getText().equals(I18n.t(source))).map(value->(JButton)value).findFirst().orElseThrow();}
    static void visible(Window window,JComponent item){Rectangle rectangle=SwingUtilities.convertRectangle(item.getParent(),item.getBounds(),((RootPaneContainer)window).getContentPane());Container content=((RootPaneContainer)window).getContentPane();check(item.isShowing()&&item.getWidth()>=item.getPreferredSize().width,"Clipped control: "+item);check(new Rectangle(0,0,content.getWidth(),content.getHeight()).contains(rectangle),"Control outside own window: "+item);check(item.getVisibleRect().contains(new Rectangle(0,0,item.getWidth(),item.getHeight())),"Control clipped by ancestor: "+item);}
    static void capture(Window window,Path output,String name)throws Exception{Container content=((RootPaneContainer)window).getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=image.createGraphics();try{content.printAll(graphics);}finally{graphics.dispose();}Path file=output.resolve(name+".png");check(ImageIO.write(image,"png",file.toFile()),"Own PNG failed");images.add(file.toString());}
    static void pure(Path own)throws Exception{
        Path file=own.resolve(NAME);String target=UploadPanel.targetUrl("sftp://example.invalid/目录/",file);
        check(URI.create(target).getPath().equals("/目录/"+NAME),"Unicode upload URI lost characters");
        check(target.contains("%")&&!target.contains(" "),"Upload URL was not safely encoded");
        check(UploadPanel.profileDirectory(Map.of("base_url","ftps://example.invalid/base/","remote_directory","место/日本語")).equals("ftps://example.invalid/base/%D0%BC%D0%B5%D1%81%D1%82%D0%BE/%E6%97%A5%E6%9C%AC%E8%AA%9E/"),"Profile remote directory is incorrect");
        for(String invalid:List.of("https://example.invalid/","ftp://user:password@example.invalid/","sftp://example.invalid/?token=secret","ftp://example.invalid/#fragment")){boolean rejected=false;try{UploadPanel.targetUrl(invalid,file);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unsafe/unsupported target accepted: "+invalid);}
        String tip=UploadPanel.tooltip(Map.of("local_path","<img src='owned'>&\"/"+NAME,"target_url","sftp://example.invalid/<owned>&"));
        check(tip.contains("&lt;img")&&!tip.contains("<img")&&tip.contains(NAME),"Upload tooltip injects HTML or corrupts Unicode");
        cases.add("unicode_upload_destination_encoding_protocol_validation_and_safe_tooltips");
    }
    static final class RecordingBackend implements UploadPanel.Backend{
        final List<Map<String,Object>> created=new CopyOnWriteArrayList<>(),rows=new CopyOnWriteArrayList<>();
        final List<String> actions=new CopyOnWriteArrayList<>();
        public Map<String,Object> create(Map<String,Object> input){if(SwingUtilities.isEventDispatchThread())throw new AssertionError("Upload create on EDT");created.add(new LinkedHashMap<>(input));Map<String,Object> row=new LinkedHashMap<>(input);row.put("id","own-"+created.size());row.put("filename",Path.of(Main.str(input.get("local_path"))).getFileName().toString());row.putAll(Map.of("status","queued","size",1000L,"uploaded_bytes",0L,"speed_bps",0L,"elapsed_ms",0L,"eta_seconds",-1L));rows.add(row);return row;}
        public Map<String,Object> state(){return Map.of("uploads",new ArrayList<>(rows));}
        public Map<String,Object> pause(String id){actions.add("pause:"+id);return change(id,"paused");}
        public Map<String,Object> resume(String id){actions.add("resume:"+id);return change(id,"running");}
        public Map<String,Object> remove(String id){actions.add("remove:"+id);rows.removeIf(row->id.equals(row.get("id")));return Map.of("removed",true);}
        Map<String,Object> change(String id,String status){for(Map<String,Object> row:rows)if(id.equals(row.get("id"))){row.put("status",status);return row;}throw new AssertionError("Unknown own upload");}
    }
    static Map<String,Object> editProfile(JFrame owner,Path own)throws Exception{
        AtomicReference<Map<String,Object>> saved=new AtomicReference<>();AtomicReference<Throwable> error=new AtomicReference<>();
        edt(()->{
            javax.swing.Timer fill=new javax.swing.Timer(80,null);fill.addActionListener(event->{for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getTitle().equals(I18n.t("Новый профиль"))){fill.stop();try{
                input(dialog,"name").setText("Owned profile 日本語 🚀");input(dialog,"base_url").setText("ftp://upload.example/root/");input(dialog,"download_directory").setText(own.resolve("Загрузки_日本語").toString());input(dialog,"remote_directory").setText("incoming");input(dialog,"gateways").setText("192.168.1.2, 192.168.1.4");
                JComboBox<?> type=(JComboBox<?>)children(dialog).stream().filter(value->"site-profile-type".equals(value.getName())).findFirst().orElseThrow();type.setSelectedItem("ftp");input(dialog,"username").setText("owned-user");input(dialog,"password").setText("owned-password-ONLY-FIXTURE");button(dialog,"Сохранить").doClick();
            }catch(Throwable failure){error.set(failure);dialog.dispose();}break;}});fill.start();
            javax.swing.Timer timeout=new javax.swing.Timer(9000,event->{fill.stop();for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing())dialog.dispose();error.compareAndSet(null,new AssertionError("Own profile edit timed out"));});timeout.setRepeats(false);timeout.start();
            try{saved.set(SiteProfilesDialog.edit(owner,Map.of()));}finally{timeout.stop();fill.stop();}
        });
        if(error.get()!=null)throw new AssertionError(error.get());check(saved.get()!=null,"Profile editor did not save");
        check(Main.str(saved.get().get("type")).equals("ftp")&&Boolean.TRUE.equals(saved.get().get("credentials_configured")),"Profile editor saved wrong authentication type");
        String publicText=Json.stringify(saved.get());check(!publicText.contains("owned-password")&&!publicText.contains("owned-user"),"Public UI profile leaked credentials");
        cases.add("actual_modal_profile_editor_saves_ftp_auth_and_public_records_hide_secrets");return saved.get();
    }
    static String tooltip(JTable table,int row){Rectangle rectangle=table.getCellRect(row,0,true);return table.getToolTipText(new MouseEvent(table,MouseEvent.MOUSE_MOVED,0,0,rectangle.x+2,rectangle.y+2,0,false));}
    static Component named(Container owner,String name){return children(owner).stream().filter(value->name.equals(value.getName())).findFirst().orElseThrow();}
    static Map<String,Object> options(JFrame owner,String title,java.util.function.Supplier<Map<String,Object>> edit,java.util.function.Consumer<JDialog> fill)throws Exception{
        AtomicReference<Map<String,Object>> result=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        edt(()->{
            javax.swing.Timer timer=new javax.swing.Timer(80,null);timer.addActionListener(event->{for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getTitle().equals(I18n.t(title))){timer.stop();try{fill.accept(dialog);JOptionPane pane=(JOptionPane)children(dialog).stream().filter(value->value instanceof JOptionPane).findFirst().orElseThrow();pane.setValue(JOptionPane.OK_OPTION);}catch(Throwable error){failure.set(error);dialog.dispose();}break;}});timer.start();
            javax.swing.Timer timeout=new javax.swing.Timer(9000,event->{failure.set(new AssertionError("Owned schedule dialog timed out"));for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getTitle().equals(I18n.t(title)))dialog.dispose();});timeout.setRepeats(false);timeout.start();
            try{result.set(edit.get());}finally{timer.stop();timeout.stop();}
        });if(failure.get()!=null)throw new AssertionError(failure.get());check(result.get()!=null,"Schedule editor cancelled unexpectedly");return result.get();
    }
    static void scheduleEditors(JFrame owner)throws Exception{
        long future=java.time.Instant.parse("2035-01-02T08:35:00Z").toEpochMilli();
        Map<String,Object> job=Map.of("kind","torrent","priority",0L,"start_after_ms",0L,"seeding_enabled",true,"seeding_minutes",30L,"seeding_ratio",1.5,"selected_files",List.of(NAME),"files",List.of(Map.of("path",NAME)),"file_priorities",Map.of(NAME,7L));
        Map<String,Object> edited=options(owner,"Приоритет и расписание…",()->DesktopOptionsDialog.job(owner,job),dialog->{
            ((JSpinner)named(dialog,"job-priority")).setValue(42L);JSpinner start=(JSpinner)named(dialog,"job-start-time");check(start.getModel() instanceof SpinnerDateModel&&!start.isEnabled(),"Job start is still an ISO-only input");((JCheckBox)named(dialog,"job-start-enabled")).doClick();check(start.isEnabled(),"Delayed start checkbox did not enable date picker");start.setValue(new java.util.Date(future));
        });
        check(Main.num(edited.get("priority"),0)==42L&&Main.num(edited.get("start_after_ms"),0)==future,"Date/priority editor changed canonical API values");check(Boolean.TRUE.equals(edited.get("seeding_enabled"))&&Main.num(Main.obj(edited.get("file_priorities")).get(NAME),0)==7,"Date picker lost existing torrent options or Unicode paths");
        Map<String,Object> queue=options(owner,"Расписание и очередь…",()->DesktopOptionsDialog.queue(owner,Map.of("max_active_jobs",3L,"global_limit_bps",524288L,"schedule_enabled",true,"schedule_start","20:00","schedule_end","07:00"),List.of("192.168.1.2")),dialog->{
            JSpinner from=(JSpinner)named(dialog,"queue-start-time"),to=(JSpinner)named(dialog,"queue-end-time");check(from.getModel() instanceof SpinnerDateModel&&to.getModel() instanceof SpinnerDateModel,"Daily schedule lacks time pickers");from.setValue(java.util.Date.from(java.time.LocalDate.now().atTime(23,30).atZone(java.time.ZoneId.systemDefault()).toInstant()));to.setValue(java.util.Date.from(java.time.LocalDate.now().atTime(6,15).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        });
        check(queue.get("schedule_start").equals("23:30")&&queue.get("schedule_end").equals("06:15"),"Time pickers changed overnight schedule values");check(Main.num(queue.get("max_active_jobs"),0)==3&&Main.num(queue.get("global_limit_bps"),0)==524288L,"Time picker lost existing queue limits");
        cases.add("actual_date_time_priority_and_daily_schedule_editors_preserve_canonical_options");
    }
    static void desktop(Path own,boolean show)throws Exception{
        SiteProfiles.configure(own.resolve("owned-profile"));AppLog.initialize(own.resolve("owned-log"));DesktopTheme.install("light");DesktopUI.installReadableFonts();I18n.setLanguage("en");
        JFrame owner=new JFrame(Brand.NAME+" — owned UI fixture");RecordingBackend backend=new RecordingBackend();List<String> errors=new CopyOnWriteArrayList<>();AtomicInteger routeCalls=new AtomicInteger(),chooserCalls=new AtomicInteger();AtomicReference<UploadPanel> panelRef=new AtomicReference<>();AtomicReference<ArchivePreviewDialog> archiveRef=new AtomicReference<>();
        Path first=own.resolve(NAME),second=own.resolve("Данные_ß_第二.bin");Files.writeString(first,"Собственный тест UTF-8 日本語 🚀",StandardCharsets.UTF_8);Files.writeString(second,"Second owned fixture",StandardCharsets.UTF_8);
        try{
            edt(()->{owner.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);owner.setSize(1080,700);owner.setLocation(30,30);owner.setVisible(show);});
            scheduleEditors(owner);
            Map<String,Object> profile=editProfile(owner,own);String profileId=Main.str(profile.get("id"));
            edt(()->{UploadPanel panel=new UploadPanel(backend,owner,()->{check(!SwingUtilities.isEventDispatchThread(),"Route preparation blocked EDT");routeCalls.incrementAndGet();return List.of(Map.of("type","gateway","gateway","192.168.1.2","virtual_ip","192.168.1.70","enabled",true));},ignored->{},errors::add,window->{check(SwingUtilities.isEventDispatchThread(),"File chooser is not on EDT");chooserCalls.incrementAndGet();return List.of(first,second);});panelRef.set(panel);owner.setContentPane(panel);SiteProfilesDialog.refresh(panel.profileCombo,profileId);owner.validate();layout(owner.getContentPane());check(panel.targetField.getText().equals("ftp://upload.example/root/incoming/"),"Choosing a saved profile did not apply the remote directory");panel.addButton.doClick();});
            UploadPanel panel=panelRef.get();waitFor(()->backend.created.size()==2,"Multiple explicitly selected files were not dispatched");
            check(routeCalls.get()==1&&chooserCalls.get()==1,"One multi-file action prepared multiple routes or repeated the chooser");
            for(int index=0;index<2;index++){Map<String,Object> input=backend.created.get(index);check(profileId.equals(input.get("site_profile_id")),"Profile identity lost during upload dispatch");check(Boolean.FALSE.equals(input.get("overwrite")),"Existing remote files may be overwritten by default");check(URI.create(Main.str(input.get("target_url"))).getPath().endsWith((index==0?first:second).getFileName().toString()),"Dispatch corrupted Unicode filename");check(Main.arr(input.get("routes")).size()==1,"Selected gateway list lost");}
            Map<String,Object> row=backend.rows.get(0);row.putAll(Map.of("status","running","size",1000L,"uploaded_bytes",500L,"speed_bps",1250000000L,"elapsed_ms",125000L,"eta_seconds",2L,"active_gateway","192.168.1.2","first_started_at","2026-10-19T10:20:30Z"));backend.rows.get(1).putAll(Map.of("status","complete","size",1000L,"uploaded_bytes",1000L,"verification","sha256-readback","completed_at","2026-10-19T10:22:30Z"));
            edt(()->{panel.update(backend.state());panel.table.getRowSorter().toggleSortOrder(0);int view=panel.table.convertRowIndexToView(0);panel.table.setRowSelectionInterval(view,view);check(tooltip(panel.table,view).contains(NAME),"Sorted upload tooltip points at a different file");check(panel.detailsView.text.getText().contains(first.toString())&&panel.detailsView.text.getText().contains("2026-10-19"),"Upload details lost path or dated start time");check(panel.summary.getText().contains("10.0"),"Aggregate upload rate does not display 10 Gbit/s");check(((DownloadProgress)panel.table.getModel().getValueAt(0,2)).perMille()==500&&((DownloadProgress)panel.table.getModel().getValueAt(1,2)).perMille()==1000,"Per-file upload progress uses the selected row");panel.pauseButton.doClick();});
            waitFor(()->backend.actions.contains("pause:own-1"),"Pause did not reach upload backend");edt(()->panel.update(backend.state()));edt(()->{check(panel.resumeButton.isEnabled(),"Paused upload cannot resume");panel.resumeButton.doClick();});waitFor(()->backend.actions.contains("resume:own-1"),"Resume did not reach upload backend");
            AtomicReference<char[]> seenPassword=new AtomicReference<>();AtomicInteger inspected=new AtomicInteger();
            Map<String,Object> archiveResult=Map.of("format","ZIP","entries",List.of(Map.of("name",NAME,"path","目录/"+NAME,"size",10000L,"compressed_size",5000L,"directory",false,"encrypted",false),Map.of("name","<img src='owned'>&\".txt","path","<img src='owned'>&\".txt","size",2L,"compressed_size",1L,"directory",false,"encrypted",true)),"truncated",true,"bytes_fetched",4096L,"metadata_budget_bytes",16777216L,"range_supported",true,"message","Archive directory read without extracting files");
            edt(()->{ArchivePreviewDialog archive=new ArchivePreviewDialog(owner,Map.of("url","https://example.invalid/owned.zip","route",Map.of("type","gateway","gateway","192.168.1.2")),input->{check(!SwingUtilities.isEventDispatchThread(),"Archive metadata fetch blocked EDT");seenPassword.set((char[])input.get("password"));inspected.incrementAndGet();return archiveResult;});archiveRef.set(archive);archive.password.setText("owned-ephemeral");archive.dialog.setVisible(show);archive.start();});
            ArchivePreviewDialog archive=archiveRef.get();waitFor(()->inspected.get()==1&&archive.table.getRowCount()==2,"Archive listing did not reach its table");check(Arrays.equals(seenPassword.get(),new char["owned-ephemeral".length()]),"Archive password was not cleared after inspection");
            edt(()->{archive.table.getRowSorter().toggleSortOrder(1);check(archive.table.convertRowIndexToModel(0)==1,"Archive size sorting is lexical instead of numeric");String tip=tooltip(archive.table,0);check(tip.contains("&lt;img")&&!tip.contains("<img"),"Archive entry tooltip executes HTML");check(archive.message.getText().contains(I18n.t("Показана только часть содержимого архива.")),"Partial archive listing looks complete");});
            cases.add("multi_file_gateway_profile_dispatch_pause_resume_metrics_and_ephemeral_archive_preview");
            for(String language:I18n.codes())for(String theme:List.of("light","dark"))edt(()->{
                I18n.setLanguage(language);DesktopTheme.install(theme);DesktopUI.installReadableFonts();SwingUtilities.updateComponentTreeUI(owner);SwingUtilities.updateComponentTreeUI(archive.dialog);panel.refreshTheme();I18n.refresh(owner);I18n.refresh(archive.dialog);owner.validate();archive.dialog.validate();layout(owner.getContentPane());layout(archive.dialog.getContentPane());
                visible(owner,panel.addButton);visible(owner,panel.profilesButton);visible(owner,panel.pauseButton);visible(owner,panel.resumeButton);visible(owner,panel.removeButton);visible(archive.dialog,archive.inspectButton);visible(archive.dialog,button(archive.dialog,"Закрыть"));
                check(panel.targetField.getComponentOrientation().isLeftToRight(),"Upload URL direction changed with Arabic UI");check(panel.targetField.getText().equals("ftp://upload.example/root/incoming/"),"Language/theme replaced editable upload URL");check(panel.table.getColumnModel().getColumn(1).getHeaderValue().equals(I18n.t("Состояние")),"Upload table header did not translate");check(archive.table.getColumnModel().getColumn(2).getHeaderValue().equals(I18n.t("Сжатый размер")),"Archive table header did not translate");check(archive.budget.getText().contains("ZIP")&&!archive.budget.getText().contains("\ufffd"),"Archive summary lost data/UTF-8");
                check(panel.addButton.getFont().canDisplayUpTo(panel.addButton.getText())<0,"Upload action contains missing glyphs in "+language);check(panel.selectedLabel.getText().contains(NAME),"Language/theme reset selected upload heading");
                if(LocalizedCatalog.EXTRA.contains(language)){check(LocalizedCatalog.catalog(language).containsKey("Отправки"),"New upload text missing from locale "+language);check(LocalizedCatalog.catalog(language).containsKey("Archive directory read without extracting files"),"New archive diagnostic missing from locale "+language);}
                if(List.of("en","zh","ar","ja","hi","vi").contains(language)){capture(owner,own,"OWN-UPLOAD-"+theme+"-"+language);capture(archive.dialog,own,"OWN-ARCHIVE-"+theme+"-"+language);}
            });
            edt(()->{backend.rows.clear();panel.update(backend.state());check(panel.selectedLabel.getToolTipText()==null&&!panel.removeButton.isEnabled(),"Empty upload queue retains a stale file tooltip/action");});
            check(errors.isEmpty(),"Unexpected owned UI error: "+errors);check(Files.readString(first,StandardCharsets.UTF_8).equals("Собственный тест UTF-8 日本語 🚀"),"UI modified an explicitly selected local file");
            cases.add("twelve_languages_light_dark_minimum_layout_live_headers_rtl_and_preserved_user_data");
        }finally{edt(()->{if(archiveRef.get()!=null)archiveRef.get().dialog.dispose();if(panelRef.get()!=null)panelRef.get().close();owner.dispose();});AppLog.close();}
    }
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);String mode=args.length>1?args[1]:"visible";Throwable failure=null;
        try{pure(own);if(!mode.equals("headless"))desktop(own,mode.equals("visible"));}catch(Throwable error){failure=error;}
        Map<String,Object> report=new LinkedHashMap<>();report.put("status",failure==null?"PASS":"FAIL");report.put("version",Main.VERSION);report.put("checks",checks);report.put("cases",cases);report.put("images",images);report.put("mode",mode);report.put("network_calls",0);report.put("user_profile_or_media_or_clipboard_read",false);if(failure!=null)report.put("failure",failure.toString());
        Files.writeString(own.resolve("DESKTOP-FEATURES-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println(Json.stringify(report));if(failure!=null)failure.printStackTrace();System.exit(failure==null?0:1);
    }
}
