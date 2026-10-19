package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Actual EDT/Manager regression using owned completed files, never user profiles or network. */
public final class BulkDeletionTest41 {
    static int checks;
    static final List<String> cases=new ArrayList<>();
    static final List<String> images=new ArrayList<>();
    static final AtomicInteger networkCalls=new AtomicInteger();
    interface Task{void run()throws Exception;}
    static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    static void edt(Task task)throws Exception{AtomicReference<Throwable> failure=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{try{task.run();}catch(Throwable error){failure.set(error);}});if(failure.get() instanceof Exception error)throw error;if(failure.get() instanceof Error error)throw error;}
    static void drain()throws Exception{for(int index=0;index<4;index++)edt(()->{});}
    static Object field(Object owner,String name)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
    static void update(DesktopUI ui,List<Object> snapshots)throws Exception{Method method=DesktopUI.class.getDeclaredMethod("updateJobs",List.class);method.setAccessible(true);method.invoke(ui,snapshots);}
    static void layout(Container parent){parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container nested)layout(nested);}
    static List<Object> snapshots(Main.Manager manager){return manager.jobs.values().stream().sorted(Comparator.comparing(job->Main.str(job.state.get("id")))).map(job->(Object)job.snapshot()).toList();}
    static String id(int number){return String.format(Locale.ROOT,"%012x",number);}
    static Map<String,Object> create(Main.Manager manager,Path own,int number)throws Exception{
        String id=id(number),name=(number%2==1?"Batch_":"Other_")+"Файл_"+number+"_日本語_é_🚀.bin";Path directory=Files.createDirectories(own.resolve("outputs"));byte[] data=("Owned UTF-8 файл "+number+" 日本語 🚀").getBytes(StandardCharsets.UTF_8);Path file=directory.resolve(name);Files.write(file,data);
        Map<String,Object> state=new LinkedHashMap<>();state.put("id",id);state.put("filename",name);state.put("directory",directory.toString());state.put("kind","http");state.put("url","https://example.invalid/owned/"+id);state.put("status","complete");state.put("size",(long)data.length);state.put("downloaded",(long)data.length);state.put("routes",new ArrayList<>());state.put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
        Path part=directory.resolve("."+name+"."+id+".part");Files.writeString(part,"Own saved part",StandardCharsets.UTF_8);state.put("part_path",part.toString());Main.Job job=new Main.Job(state,manager.dataDir);job.persist();manager.jobs.put(id,job);return state;
    }
    static String rowId(DesktopUI ui,int view){if(view<0)return null;return new ArrayList<>(((Map<?,?>)uncheckedField(ui,"jobs")).keySet()).get(ui.jobsTable.convertRowIndexToModel(view)).toString();}
    static Object uncheckedField(Object owner,String name){try{return field(owner,name);}catch(Exception error){throw new IllegalStateException(error);}}
    static Set<String> selected(DesktopUI ui){Set<String> result=new LinkedHashSet<>();for(int view:ui.jobsTable.getSelectedRows())result.add(rowId(ui,view));return result;}
    static int view(DesktopUI ui,String id){int model=new ArrayList<>(((Map<?,?>)uncheckedField(ui,"jobs")).keySet()).indexOf(id);return model<0?-1:ui.jobsTable.convertRowIndexToView(model);}
    static void select(DesktopUI ui,String... ids){ui.jobsTable.clearSelection();for(String id:ids){int row=view(ui,id);if(row>=0)ui.jobsTable.addRowSelectionInterval(row,row);}}
    static void regression(DesktopUI ui,Main.Manager manager,boolean baseline)throws Exception{
        List<String> failures=new ArrayList<>();edt(()->{
            update(ui,snapshots(manager));
            if(ui.jobsTable.getSelectionModel().getSelectionMode()!=ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)failures.add("Published table uses SINGLE_SELECTION");
            ui.jobsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);select(ui,id(1),id(3));String lead=rowId(ui,ui.jobsTable.getSelectionModel().getLeadSelectionIndex());update(ui,snapshots(manager));
            if(!selected(ui).equals(Set.of(id(1),id(3)))||!Objects.equals(lead,rowId(ui,ui.jobsTable.getSelectionModel().getLeadSelectionIndex())))failures.add("Periodic refresh loses selected IDs or lead");
        });
        checks+=2;if(!failures.isEmpty())throw new AssertionError(String.join("; ",failures));check(!baseline,"Regression mode must fail against the immutable published single-selection JAR");cases.add("published_single_selection_and_refresh_loss_regression_fixed");
    }
    static void waitFor(java.util.function.BooleanSupplier condition,String why)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(!condition.getAsBoolean()&&System.nanoTime()<until){Thread.sleep(25);edt(()->{});}check(condition.getAsBoolean(),why);drain();}
    static List<Component> children(Component owner){List<Component> result=new ArrayList<>();result.add(owner);if(owner instanceof Container parent)for(Component child:parent.getComponents())result.addAll(children(child));return result;}
    static Path file(Map<String,Object> job){return Path.of(Main.str(job.get("directory"))).resolve(Main.str(job.get("filename")));}
    static void capture(JDialog dialog,Path own,String name)throws Exception{Container content=dialog.getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=image.createGraphics();try{content.printAll(graphics);}finally{graphics.dispose();}Path output=own.resolve(name+".png");check(ImageIO.write(image,"png",output.toFile()),"Could not capture own bulk prompt");images.add(output.toString());}

    static void selection(DesktopUI ui,Main.Manager manager)throws Exception{
        edt(()->{
            select(ui,id(1),id(3));javax.swing.DefaultListSelectionModel model=(javax.swing.DefaultListSelectionModel)ui.jobsTable.getSelectionModel();model.setAnchorSelectionIndex(view(ui,id(1)));model.moveLeadSelectionIndex(view(ui,id(3)));ui.jobsSorter.setSortKeys(List.of(new RowSorter.SortKey(0,SortOrder.DESCENDING)));
        });drain();
        edt(()->{
            check(selected(ui).equals(Set.of(id(1),id(3))),"Sort changed selected model IDs");check(rowId(ui,ui.jobsTable.getSelectionModel().getLeadSelectionIndex()).equals(id(3)),"Sort changed the lead job");
            String beforeAnchor=rowId(ui,ui.jobsTable.getSelectionModel().getAnchorSelectionIndex());update(ui,snapshots(manager));check(selected(ui).equals(Set.of(id(1),id(3))),"Refresh lost multiple IDs");check(rowId(ui,ui.jobsTable.getSelectionModel().getAnchorSelectionIndex()).equals(id(1)),"Refresh lost the Shift anchor; before="+beforeAnchor+", after="+rowId(ui,ui.jobsTable.getSelectionModel().getAnchorSelectionIndex()));
            check(!ui.optionsButton.isEnabled()&&!ui.openFolderButton.isEnabled()&&!ui.pauseButton.isEnabled()&&!ui.resumeButton.isEnabled(),"Single-job action remains enabled for a group");
            JPopupMenu menu=ui.jobsTable.getComponentPopupMenu();for(int index=0;index<5;index++)check(!menu.getComponent(index).isEnabled(),"Keyboard popup exposes an enabled single action for a group");check(menu.getComponent(5).isEnabled()&&!menu.getComponent(7).isEnabled(),"Keyboard popup bulk/copy states are stale");
            int row=view(ui,id(1));Rectangle cell=ui.jobsTable.getCellRect(row,0,true);MouseEvent popup=new MouseEvent(ui.jobsTable,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,cell.x+4,cell.y+4,1,true,MouseEvent.BUTTON3);for(MouseListener listener:ui.jobsTable.getMouseListeners())listener.mousePressed(popup);menu.setVisible(false);check(selected(ui).equals(Set.of(id(1),id(3))),"Right click collapsed an existing selected group");
            ui.jobsTable.clearSelection();update(ui,snapshots(manager));check(selected(ui).isEmpty()&&!ui.removeButton.isEnabled(),"Refresh silently selected a row after explicit deselection");
            select(ui,id(1),id(2),id(3),id(4));ui.searchField.setText("Batch_");check(selected(ui).equals(Set.of(id(1),id(3))),"Filter retained hidden targets");
            ui.jobsTable.getActionMap().get("multiroute.select-visible").actionPerformed(new ActionEvent(ui,0,"owned-select-visible"));check(selected(ui).equals(Set.of(id(1),id(3)))&&ui.jobsTable.getRowCount()==2,"Ctrl/Cmd+A selected filtered-out jobs");
            ui.searchField.setText("");check(selected(ui).equals(Set.of(id(1),id(3))),"Clearing the filter resurrected hidden targets");
        });
        for(String language:I18n.codes())edt(()->{
            ui.languageCombo.setSelectedItem(language);ui.themeCombo.setSelectedItem(language.equals("ar")?"dark":"light");check(selected(ui).equals(Set.of(id(1),id(3))),"Theme/language changed selected IDs in "+language);check(ui.selectionCountLabel.getText().equals(I18n.t("Выбрано: %d",2)),"Selection count is not translated in "+language);
        });
        edt(()->ui.languageCombo.setSelectedItem("en"));cases.add("sorted_filtered_selection_anchor_empty_selection_context_menu_and_twelve_language_restore");
    }

    static final class Prompts implements AutoCloseable{
        final AtomicInteger confirmations=new AtomicInteger(),errors=new AtomicInteger();final AtomicReference<Throwable> failure=new AtomicReference<>();final javax.swing.Timer timer;final List<Map<String,Object>> expected;final DesktopUI.DeleteChoice choice;final boolean expectError;final Path own;
        Prompts(DesktopUI ui,List<Map<String,Object>> expected,DesktopUI.DeleteChoice choice,boolean expectError,Path own){this.expected=expected;this.choice=choice;this.expectError=expectError;this.own=own;timer=new javax.swing.Timer(50,event->poll(ui));timer.start();}
        void poll(DesktopUI ui){
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getOwner()==ui.frame){
                JOptionPane pane=children(dialog).stream().filter(value->value instanceof JOptionPane).map(value->(JOptionPane)value).findFirst().orElse(null);if(pane==null)continue;
                try{
                    if(dialog.getTitle().equals(I18n.t("Удаление загрузки"))&&confirmations.get()==0){
                        confirmations.incrementAndGet();JCheckBox checkbox=children(dialog).stream().filter(value->value instanceof JCheckBox).map(value->(JCheckBox)value).findFirst().orElseThrow();check(!checkbox.isSelected(),"Delete-files checkbox is checked by default");
                        String text=String.join("\n",children(dialog).stream().filter(value->value instanceof JTextArea).map(value->((JTextArea)value).getText()).toList());
                        for(Map<String,Object> job:expected)check(text.contains(Main.str(job.get("filename")))&&text.contains(file(job).toString()),"Confirmation lost a full Unicode filename/path");
                        if(expected.size()>1){check(children(dialog).stream().filter(value->value instanceof JLabel).map(value->((JLabel)value).getText()).anyMatch(value->Objects.equals(value,I18n.t("Убрать выбранные загрузки (%d) из приложения?",expected.size()))),"Confirmation does not state the exact selected count");JTextArea list=children(dialog).stream().filter(value->value instanceof JTextArea area&&area.getText().contains(file(expected.get(0)).toString())).map(value->(JTextArea)value).findFirst().orElseThrow();check(list.getParent() instanceof JViewport&&list.getComponentOrientation().isLeftToRight(),"Full path list is not scrollable or logical LTR");if(I18n.language().equals("ar"))capture(dialog,own,"OWN-BULK-CONFIRMATION-ar");}
                        checkbox.setSelected(choice==DesktopUI.DeleteChoice.DELETE_FILES);pane.setValue(choice==DesktopUI.DeleteChoice.CANCEL?JOptionPane.CANCEL_OPTION:JOptionPane.OK_OPTION);
                    }else if(dialog.getTitle().equals(I18n.t("Ошибка"))&&expectError&&errors.get()==0){
                        errors.incrementAndGet();String text=String.join("\n",children(dialog).stream().filter(value->value instanceof JTextArea).map(value->((JTextArea)value).getText()).toList());check(text.contains(Main.str(expected.get(0).get("filename")))&&text.contains(file(expected.get(0)).toString()),"Partial-failure report omits failed filename/path");check(children(dialog).stream().anyMatch(value->value instanceof JLabel label&&Objects.equals(label.getText(),I18n.t("Удалено: %d из %d. Не удалось удалить: %d.",expected.size()-1,expected.size(),1))),"Partial failure claims full success or hides counts");pane.setValue(JOptionPane.OK_OPTION);
                    }
                }catch(Throwable error){failure.set(error);pane.setValue(JOptionPane.CANCEL_OPTION);dialog.dispose();timer.stop();}
            }
        }
        public void close(){timer.stop();if(failure.get()!=null)throw new AssertionError(failure.get());check(confirmations.get()==1,"Bulk action did not use exactly one confirmation");check(errors.get()==(expectError?1:0),"Bulk action did not use one honest aggregate error report");}
    }
    static void remove(DesktopUI ui,List<Map<String,Object>> expected,DesktopUI.DeleteChoice choice,boolean error,Path own,java.util.function.BooleanSupplier done)throws Exception{
        AtomicReference<Prompts> prompts=new AtomicReference<>();
        edt(()->{prompts.set(new Prompts(ui,expected,choice,error,own));ui.removeButton.doClick();});
        try{waitFor(()->prompts.get().failure.get()!=null||done.getAsBoolean()&&(!error||prompts.get().errors.get()==1),"Owned bulk removal did not finish");}finally{edt(()->prompts.get().close());}
    }
    static void deletion(DesktopUI ui,Main.Manager manager,Path own)throws Exception{
        Map<String,Object> one=manager.jobs.get(id(1)).snapshot(),three=manager.jobs.get(id(3)).snapshot();
        edt(()->{select(ui,id(1),id(3));ui.languageCombo.setSelectedItem("ar");});remove(ui,List.of(one,three),DesktopUI.DeleteChoice.CANCEL,false,own,()->!(Boolean)uncheckedField(ui,"jobActionRunning"));
        check(manager.jobs.size()==4&&Files.exists(file(one))&&Files.exists(file(three)),"Cancel changed job records or files");
        edt(()->ui.languageCombo.setSelectedItem("en"));remove(ui,List.of(one,three),DesktopUI.DeleteChoice.KEEP_FILES,false,own,()->!manager.jobs.containsKey(id(1))&&!manager.jobs.containsKey(id(3))&&!(Boolean)uncheckedField(ui,"jobActionRunning"));
        check(Files.exists(file(one))&&Files.exists(Path.of(Main.str(one.get("part_path"))))&&Files.exists(file(three)),"Default removal deleted an owned completed file/part");check(manager.jobs.containsKey(id(2))&&manager.jobs.containsKey(id(4)),"Bulk removal removed an unselected job");
        edt(()->check(selected(ui).isEmpty()&&!ui.removeButton.isEnabled(),"Successful group removal selected an unrelated surviving job"));
        Map<String,Object> five=create(manager,own,5),seven=create(manager,own,7);edt(()->{update(ui,snapshots(manager));select(ui,id(5),id(7));});remove(ui,List.of(five,seven),DesktopUI.DeleteChoice.DELETE_FILES,false,own,()->!manager.jobs.containsKey(id(5))&&!manager.jobs.containsKey(id(7))&&!(Boolean)uncheckedField(ui,"jobActionRunning"));
        check(!Files.exists(file(five))&&!Files.exists(Path.of(Main.str(five.get("part_path"))))&&!Files.exists(file(seven)),"Explicit delete-files did not remove verified owned results and parts");check(Files.exists(file(one))&&Files.exists(file(three)),"Explicit group deletion affected previously retained files");
        Map<String,Object> bad=create(manager,own,6),good=create(manager,own,8),last=create(manager,own,10);manager.jobs.get(id(6)).state.put("sha256","0".repeat(64));manager.jobs.get(id(6)).persist();
        edt(()->{update(ui,snapshots(manager));ui.jobsSorter.setSortKeys(List.of(new RowSorter.SortKey(0,SortOrder.ASCENDING)));select(ui,id(6),id(8),id(10));});remove(ui,List.of(bad,good,last),DesktopUI.DeleteChoice.DELETE_FILES,true,own,()->!manager.jobs.containsKey(id(8))&&!manager.jobs.containsKey(id(10))&&!(Boolean)uncheckedField(ui,"jobActionRunning"));
        check(manager.jobs.containsKey(id(6))&&Files.exists(file(bad))&&Files.exists(manager.dataDir.resolve(id(6)+".json")),"Failed safe-file validation lost its job or result");check(!Files.exists(file(good))&&!Files.exists(file(last)),"First failure prevented later selected jobs from being removed");edt(()->check(selected(ui).equals(Set.of(id(6)))&&ui.removeButton.isEnabled(),"Failed job was not kept selected for retry"));
        Map<String,Object> eleven=create(manager,own,11),thirteen=create(manager,own,13);Map<String,Object> hidden=create(manager,own,12);
        edt(()->{update(ui,snapshots(manager));ui.searchField.setText("Batch_");ui.jobsTable.getActionMap().get("multiroute.select-visible").actionPerformed(new ActionEvent(ui,0,"owned"));check(selected(ui).equals(Set.of(id(11),id(13))),"Filtered Ctrl/Cmd+A has hidden targets before real deletion");});remove(ui,List.of(eleven,thirteen),DesktopUI.DeleteChoice.KEEP_FILES,false,own,()->!manager.jobs.containsKey(id(11))&&!manager.jobs.containsKey(id(13))&&!(Boolean)uncheckedField(ui,"jobActionRunning"));
        check(manager.jobs.containsKey(id(12))&&Files.exists(file(hidden))&&manager.jobs.containsKey(id(2))&&manager.jobs.containsKey(id(4))&&manager.jobs.containsKey(id(6)),"Filtered bulk action removed a hidden or failed unselected job");
        cases.add("one_confirmation_cancel_default_keep_explicit_verified_file_deletion_partial_failure_continuation_and_filtered_safety");
    }
    static void run(Path own,boolean baseline,boolean visible)throws Exception{
        AppLog.initialize(own.resolve("owned-log"));Main.Manager manager=new Main.Manager(own.resolve("owned-profile"),(url,route)->{networkCalls.incrementAndGet();throw new AssertionError("No source access");});AtomicReference<DesktopUI> reference=new AtomicReference<>();CountDownLatch closed=new CountDownLatch(1);
        try{
            for(int index=1;index<=4;index++)create(manager,own,index);
            edt(()->{DesktopUI ui=new DesktopUI(manager,closed::countDown,false,(gateway,url)->{networkCalls.incrementAndGet();throw new AssertionError("No discovery");},(ip,gateways)->{networkCalls.incrementAndGet();throw new AssertionError("No route changes");},(route,url)->{networkCalls.incrementAndGet();throw new AssertionError("No speed tests");});reference.set(ui);((javax.swing.Timer)uncheckedField(ui,"refreshTimer")).stop();ui.frame.setSize(1280,860);if(visible)ui.frame.setVisible(true);else ui.frame.addNotify();ui.frame.validate();layout(ui.frame.getContentPane());});drain();
            regression(reference.get(),manager,baseline);
            if(!baseline){selection(reference.get(),manager);deletion(reference.get(),manager,own);}
        }finally{if(reference.get()!=null){edt(()->reference.get().frame.dispose());check(closed.await(8,TimeUnit.SECONDS),"Owned UI did not close");}manager.shutdown();AppLog.close();}
        check(networkCalls.get()==0,"Bulk deletion made a network request");
    }
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);String mode=args.length>1?args[1]:"visible";Throwable failure=null;
        try{run(own,mode.equals("baseline"),mode.equals("visible"));}catch(Throwable error){failure=error;}
        Map<String,Object> report=new LinkedHashMap<>();report.put("status",failure==null?"PASS":"FAIL");report.put("checks",checks);report.put("cases",cases);report.put("images",images);report.put("version",Main.VERSION);report.put("mode",mode);report.put("network_calls",networkCalls.get());report.put("user_files_or_profile_or_clipboard_read",false);report.put("java_home",System.getProperty("java.home"));report.put("java_version",System.getProperty("java.version"));if(failure!=null)report.put("failure",failure.toString());Files.writeString(own.resolve("BULK-DELETION-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println(Json.stringify(report));if(failure!=null)failure.printStackTrace();System.exit(failure==null?0:1);
    }
}
