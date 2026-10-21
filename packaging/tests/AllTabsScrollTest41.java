package multiroute;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Actual desktop timers and owned Unicode fixtures; no source, gateway or personal data access. */
public final class AllTabsScrollTest41 {
    interface Work { void run() throws Exception; }
    static int checks;
    static final AtomicInteger network = new AtomicInteger();
    static final List<String> cases = new ArrayList<>();
    static final Map<String,Object> observations = new LinkedHashMap<>();
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() instanceof Exception failure) throw failure;
        if (error.get() instanceof Error failure) throw failure;
    }
    static void drain() throws Exception { for (int i=0;i<6;i++) edt(() -> {}); }
    static Object field(Object owner,String name) throws Exception { Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner); }
    static void call(Object owner,String name) throws Exception { Method m=owner.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(owner); }
    static void layout(Container root) { root.doLayout();for(Component child:root.getComponents())if(child instanceof Container nested)layout(nested); }
    static List<String> gateways() { List<String> values=new ArrayList<>();for(int n=1;n<=32;n++)values.add("192.168.1."+n);return values; }
    static Map<String,Object> settings(String host) { return Map.of("virtual_ip","192.168.1.70","gateways",gateways(),"network_validation",Map.of("status","valid","computer_ipv4",host,"network_cidr","192.168.1.0/24")); }
    static void waitTicks(AtomicInteger reads,int count) throws Exception {
        int start=reads.get();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(reads.get()<start+count&&System.nanoTime()<end)Thread.sleep(25);
        check(reads.get()>=start+count,"Production900ms timer did not run");drain();
    }
    static int middle(JScrollPane pane) {
        JScrollBar bar=pane.getVerticalScrollBar();int limit=bar.getMaximum()-bar.getVisibleAmount();
        check(limit>40,"Owned pane is not scrollable");bar.setValue(Math.max(20,limit/2));
        int y=pane.getViewport().getViewPosition().y;check(y>0,"Owned pane did not scroll");return y;
    }
    static void unchanged(JScrollPane pane,int y,String name) { int actual=pane.getViewport().getViewPosition().y;check(Math.abs(actual-y)<=1,name+" moved: "+y+" -> "+actual); }
    static boolean bottom(JScrollPane pane) { JScrollBar bar=pane.getVerticalScrollBar();return bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-2; }
    static void hold(JScrollPane pane,AtomicInteger reads,String name) throws Exception {
        AtomicInteger y=new AtomicInteger();edt(()->y.set(middle(pane)));waitTicks(reads,2);edt(()->unchanged(pane,y.get(),name));cases.add(name+"_preserves_reader_across_real_timer");
    }
    static void rows(DesktopUI ui,List<String> gateways) throws Exception { Method method=DesktopUI.class.getDeclaredMethod("replaceGatewayRows",List.class);method.setAccessible(true);method.invoke(ui,gateways); }
    static Map<String,Object> event(Path own,int n) { return Map.of("event_id",UUID.nameUUIDFromBytes(("owned-event-"+n).getBytes(StandardCharsets.UTF_8)).toString(),"job_id","000000000001","kind","paused","time","2026-10-21T12:00:00Z","filename","История_日本語_"+n+".bin","path",own.resolve("outputs/История_"+n).toString()); }
    static Map<String,Object> job(Path own,int index) {
        Map<String,Object> value=new LinkedHashMap<>();String id=String.format(Locale.ROOT,"%012x",index+1);
        value.put("id",id);value.put("filename","Файл_日本語_"+index+"_é.bin");value.put("directory",own.resolve("outputs").toString());value.put("url","https://example.invalid/owned/"+index);
        value.put("status","paused");value.put("kind","http");value.put("size",1024L*1024*1024);value.put("downloaded",200_000L+index);value.put("elapsed_ms",3000L);value.put("created_at",String.format(Locale.ROOT,"2026-10-21T12:%02d:%02dZ",index/60,index%60));
        value.put("error","Owned Unicode diagnostic Проверка 日本語 "+"wrapped safe text ".repeat(12));
        List<Object> routes=new ArrayList<>();for(String ip:gateways())routes.add(new LinkedHashMap<>(Map.of("gateway",ip,"name","Owned "+ip,"type","gateway","enabled",true,"virtual_ip","192.168.1.70","bytes",1000L,"received_bytes",1000L,"speed_bps",0,"last_error","Owned trace "+"данные 日本語 ".repeat(12))));value.put("routes",routes);
        List<Object> history=new ArrayList<>();for(int n=0;n<80;n++)history.add(event(own,n));value.put("lifecycle_history",history);value.put("lifecycle_version",1);return value;
    }
    static void allPanes(DesktopUI ui,Main.Manager manager,Path own,AtomicInteger reads,AtomicReference<Map<String,Object>> state) throws Exception {
        JScrollPane networkPane=(JScrollPane)ui.mainTabs.getComponentAt(1);AtomicInteger y=new AtomicInteger();
        edt(()->{y.set(middle(networkPane));state.set(Map.of("virtual_ip","192.168.1.70","gateways",gateways(),"network_validation",Map.of("status","error","message","Owned long subnet diagnostic Проверка 日本語 "+"wrapped safe message ".repeat(60))));});waitTicks(reads,2);
        edt(()->{ui.frame.validate();layout(ui.frame);unchanged(networkPane,y.get(),"Long topology replacement/full form layout");});
        edt(()->{state.set(settings("192.168.1.91"));});waitTicks(reads,2);
        edt(()->{y.set(middle(networkPane));rows(ui,gateways().subList(0,31));ui.frame.validate();layout(ui.frame);});drain();
        edt(()->{unchanged(networkPane,y.get(),"Removing a gateway row");rows(ui,gateways());ui.frame.validate();layout(ui.frame);});drain();
        edt(()->unchanged(networkPane,y.get(),"Adding a gateway row"));
        for(String language:I18n.codes()) {
            edt(()->{y.set(middle(networkPane));I18n.setLanguage(language);call(ui,"refreshLanguage");ui.frame.validate();layout(ui.frame);});drain();
            edt(()->{unchanged(networkPane,y.get(),"Language change "+language);ui.mainTabs.setSelectedIndex(0);ui.mainTabs.setSelectedIndex(1);});drain();
            edt(()->unchanged(networkPane,y.get(),"Tab roundtrip "+language));
        }
        cases.add("long_topology_full_layout_gateway_add_remove_twelve_languages_tab_roundtrip");
        edt(()->{I18n.setLanguage("en");call(ui,"refreshLanguage");ui.mainTabs.setSelectedIndex(0);ui.frame.setSize(1180,590);ui.frame.validate();layout(ui.frame);});drain();
        JScrollPane editor=(JScrollPane)((JSplitPane)ui.mainTabs.getComponentAt(0)).getLeftComponent();hold(editor,reads,"Download_editor");hold(ui.jobsScroll,reads,"Download_table");
        edt(()->{Action last=ui.jobsTable.getActionMap().get("selectLastRow");check(last!=null,"Missing table keyboard navigation");last.actionPerformed(new ActionEvent(ui.jobsTable,0,"owned keyboard End"));});drain();
        edt(()->check(bottom(ui.jobsScroll),"Explicit table End action no longer scrolls to the last row"));
        hold(ui.detailScroll,reads,"Outer_download_details");
        JScrollPane metrics=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,ui.gatewaySpeedsArea);hold(metrics,reads,"Gateway_metrics");
        edt(()->{ui.detailScroll.getVerticalScrollBar().setValue(0);ui.gatewaySpeedsArea.requestFocusInWindow();});drain();
        edt(()->{Action end=ui.gatewaySpeedsArea.getActionMap().get(javax.swing.text.DefaultEditorKit.endAction);check(end!=null,"Missing text End action");end.actionPerformed(new ActionEvent(ui.gatewaySpeedsArea,0,"owned keyboard End"));});drain();
        edt(()->{check(ui.gatewaySpeedsArea.getCaretPosition()==ui.gatewaySpeedsArea.getDocument().getLength(),"Text End did not reach the document end");Rectangle bounds=ui.gatewaySpeedsArea.modelToView2D(ui.gatewaySpeedsArea.getCaretPosition()).getBounds();check(metrics.getViewport().getViewRect().intersects(bounds),"Keyboard End caret is outside metrics viewport: "+bounds+" / "+metrics.getViewport().getViewRect());});
        LogView details=(LogView)field(ui,"detailsView"),history=(LogView)field(ui,"historyView");
        edt(()->{ui.detailTabs.setSelectedIndex(0);});drain();hold(details,reads,"Download_details_text");
        edt(()->{ui.detailTabs.setSelectedIndex(1);});drain();hold(history,reads,"Download_history_text");
        AtomicReference<Map<String,Object>> uploadState=new AtomicReference<>();List<Object> transfers=new ArrayList<>();
        for(int n=0;n<65;n++){Map<String,Object> transfer=new LinkedHashMap<>(job(own,n));transfer.put("local_path",own.resolve("uploads/Файл_日本語_"+n).toString());transfer.put("target_url","sftp://example.invalid/owned/"+n);transfer.put("uploaded_bytes",1000L);transfers.add(transfer);}uploadState.set(Map.of("uploads",transfers));
        edt(()->{ui.uploadPanel.close();ui.uploadPanel=new UploadPanel(new UploadPanel.Backend(){public Map<String,Object> state(){return uploadState.get();}public Map<String,Object> create(Map<String,Object> v){throw new AssertionError("No upload");}public Map<String,Object> pause(String id){throw new AssertionError("No pause");}public Map<String,Object> resume(String id){throw new AssertionError("No resume");}public Map<String,Object> remove(String id){throw new AssertionError("No remove");}},ui.frame,()->{network.incrementAndGet();throw new AssertionError("No upload route");},profile->{},error->{throw new AssertionError(error);},owner->{throw new AssertionError("No chooser");});ui.mainTabs.setComponentAt(2,ui.uploadPanel);ui.mainTabs.setSelectedIndex(2);ui.frame.validate();layout(ui.frame);});waitTicks(reads,2);
        hold(ui.uploadPanel.scroll,reads,"Upload_table");hold(ui.uploadPanel.detailsView,reads,"Upload_details_text");
        Path journal=manager.dataDir.resolve("logs/downloads-history.jsonl");Files.createDirectories(journal.getParent());StringBuilder historyLines=new StringBuilder();for(int n=0;n<200;n++)historyLines.append(Json.stringify(event(own,n))).append('\n');Files.writeString(journal,historyLines,StandardCharsets.UTF_8);
        for(int n=0;n<250;n++)AppLog.info("Owned scroll journal Проверка_日本語 %d",n);AppLog.flush();edt(()->{ui.openLog(false);ui.logDialog.validate();layout(ui.logDialog);});drain();
        LogView application=(LogView)field(ui,"logView"),downloads=(LogView)field(ui,"downloadLogView");JTabbedPane logTabs=(JTabbedPane)field(ui,"logTabs");
        edt(()->{logTabs.setSelectedIndex(0);y.set(middle(application));});AppLog.info("Owned appended event Проверка_日本語");AppLog.flush();waitTicks(reads,2);edt(()->unchanged(application,y.get(),"Application log reading"));
        edt(()->{application.text.setCaretPosition(0);application.getVerticalScrollBar().setValue(application.getVerticalScrollBar().getMaximum());check(bottom(application),"Cannot reach log tail");});AppLog.info("Owned tail event Проверка_日本語");AppLog.flush();waitTicks(reads,2);edt(()->check(bottom(application),"Log should follow only an already selected tail"));
        edt(()->{logTabs.setSelectedIndex(1);layout(ui.logDialog);y.set(middle(downloads));});Files.writeString(journal,Json.stringify(event(own,201))+"\n",StandardCharsets.UTF_8,StandardOpenOption.APPEND);waitTicks(reads,2);edt(()->unchanged(downloads,y.get(),"Download log reading"));
        cases.add("both_log_tabs_keep_middle_and_follow_only_existing_tail");
        edt(()->{ui.logDialog.dispose();ui.mainTabs.setSelectedIndex(1);ui.frame.validate();layout(ui.frame);y.set(middle(networkPane));networkPane.getVerticalScrollBar().setValueIsAdjusting(true);});waitTicks(reads,2);edt(()->{unchanged(networkPane,y.get(),"Active scrollbar drag");networkPane.getVerticalScrollBar().setValueIsAdjusting(false);});
        cases.add("explicit_keyboard_navigation_and_active_scrollbar_drag_remain_available");
        check(!ui.detailsArea.getText().contains("\ufffd")&&!ui.uploadPanel.detailsView.text.getText().contains("\ufffd"),"Unicode text was corrupted");
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);
        boolean baseline=args.length>1&&args[1].equals("baseline");
        AtomicReference<Map<String,Object>> networkState=new AtomicReference<>(settings("192.168.1.90"));AtomicInteger reads=new AtomicInteger();
        SourceTransfer.endpoints=route->{network.incrementAndGet();throw new AssertionError("No gateway endpoint");};
        Main.Manager manager=new Main.Manager(own.resolve("profile"),(url,route)->{network.incrementAndGet();throw new AssertionError("No source inspection");},path->Long.MAX_VALUE,(route,torrent)->{network.incrementAndGet();throw new AssertionError("No route validation");});
        for(int n=0;n<65;n++){Map<String,Object> value=job(own,n);manager.jobs.put(Main.str(value.get("id")),new Main.Job(value,manager.dataDir,(route,url,start,end,etag,modified)->{network.incrementAndGet();throw new AssertionError("No payload");},Main.HTTP_IDLE_TIMEOUT_NANOS));}
        Files.writeString(manager.dataDir.resolve("desktop-settings.config"),Json.stringify(Map.of("gateways",gateways(),"virtual_ip","192.168.1.70","language","en","theme","light")),StandardCharsets.UTF_8);
        AppLog.initialize(own.resolve("owned-log"));AtomicReference<DesktopUI> reference=new AtomicReference<>();CountDownLatch closed=new CountDownLatch(1);
        try {
            edt(()->{
                DesktopUI ui=new DesktopUI(manager,closed::countDown,false,(gateway,url)->{network.incrementAndGet();throw new AssertionError("No discovery");},(ip,values)->{network.incrementAndGet();throw new AssertionError("No configuration");},(route,url)->{network.incrementAndGet();throw new AssertionError("No speed test");},()->{reads.incrementAndGet();return networkState.get();});
                reference.set(ui);ui.frame.setSize(1180,720);ui.frame.setVisible(true);ui.mainTabs.setSelectedIndex(1);ui.frame.validate();layout(ui.frame);
            });drain();DesktopUI ui=reference.get();JScrollPane pane=(JScrollPane)ui.mainTabs.getComponentAt(1);AtomicInteger y=new AtomicInteger();
            edt(()->{ui.routes.get(0).gatewayField.requestFocusInWindow();});drain();
            edt(()->y.set(middle(pane)));waitTicks(reads,3);
            edt(()->{int actual=pane.getViewport().getViewPosition().y;observations.put("network_before",y.get());observations.put("network_after",actual);
                if(baseline)check(Math.abs(actual-y.get())>100,"Current baseline did not reproduce network jump");else unchanged(pane,y.get(),"Network settings");});
            if(baseline)cases.add("packaged41_network_timer_jump_reproduced_with_focused_gateway");
            else {
                cases.add("network_32_gateways_stable_across_actual900ms_timer");
                edt(()->{y.set(middle(pane));networkState.set(settings("192.168.1.91"));});waitTicks(reads,2);
                edt(()->{unchanged(pane,y.get(),"Changed topology message");check(ui.networkTopologyStatusLabel.getText().contains("192.168.1.91"),"Topology no longer updates");});
                allPanes(ui,manager,own,reads,networkState);
            }
            check(network.get()==0,"Scroll test used a network seam");
            Map<String,Object> report=new LinkedHashMap<>();report.put("status",baseline?"BASELINE_REPRODUCED":"PASS");report.put("version",Main.VERSION);report.put("checks",checks);report.put("cases",cases);report.put("observations",observations);report.put("real_timer_interval_ms",900);report.put("network_calls",network.get());report.put("user_profile_or_logs_read",false);report.put("user_media_or_clipboard_read",false);
            String json=Json.stringify(report);Files.writeString(own.resolve("ALL-TABS-SCROLL-TESTS.json"),json,StandardCharsets.UTF_8);check(Files.readString(own.resolve("ALL-TABS-SCROLL-TESTS.json"),StandardCharsets.UTF_8).equals(json)&&!json.contains("\ufffd"),"UTF8 report corrupted");System.out.println(json);
        } finally {
            if(reference.get()!=null){edt(()->reference.get().frame.dispose());check(closed.await(8,TimeUnit.SECONDS),"Owned frame did not close");}
            manager.shutdown();AppLog.close();
        }
    }
}
