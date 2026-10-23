package multiroute;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.Document;

/** Owned Swing DNS editor and actual desktop timer checks; never uses a real resolver or profile. */
public final class DnsSettingsUiTest {
    private static final Set<String> CONFIG_KEYS = Set.of("mode", "provider", "doh_url", "dot_host", "dot_port", "bootstrap_ips");
    private static final String SECURE_HELP = "DoH and DoT encrypt DNS through each selected gateway. Certificates and server names are verified; there is no fallback to unencrypted DNS.";
    private static final String SYSTEM_HELP = "System DNS follows the computer or VPN resolver policy; file connections still use their selected gateway.";
    private static final String BOOTSTRAP_HELP = "Numeric bootstrap addresses connect to the DNS provider without an initial unencrypted lookup.";
    private static final String UNICODE_URL = "https://resolver.invalid/dns-query/Проверка_日本語_é";
    private static final List<String> GATEWAYS = List.of("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4");
    private static final List<String> cases = new ArrayList<>();
    private static final Map<String,Object> observations = new LinkedHashMap<>();
    private static final AtomicInteger network = new AtomicInteger();
    private static int checks;
    interface Work { void run() throws Exception; }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() instanceof Exception failure) throw failure;
        if (error.get() instanceof Error failure) throw failure;
    }
    static void drain() throws Exception { for (int n=0;n<5;n++) edt(() -> {}); }
    static void layout(Container root) { root.doLayout(); for (Component child:root.getComponents()) if (child instanceof Container nested) layout(nested); }
    static void waitFor(BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime()<deadline) Thread.sleep(20);
        check(condition.getAsBoolean(), description); drain();
    }
    static boolean idle(DnsSettingsPanel panel) {
        AtomicBoolean value = new AtomicBoolean();
        try { edt(() -> value.set(!panel.isBusy())); } catch (Exception failure) { throw new RuntimeException(failure); }
        return value.get();
    }
    static Map<String,Object> applied(Map<String,Object> config, long revision) {
        Map<String,Object> result = new LinkedHashMap<>(config); result.put("revision",revision);
        result.put("managed_dns",Map.of("running",true,"queries",revision));
        result.put("configuration_error",""); result.put("gateway_status",Map.of("192.168.1.4","owned offline fixture")); return result;
    }
    static Map<String,Object> custom() {
        Map<String,Object> result = new LinkedHashMap<>(SecureDns.Config.preset("custom","doh").toJson());
        result.put("doh_url",UNICODE_URL); return SecureDns.Config.fromJson(result).toJson();
    }
    static List<Component> descendants(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component child:parent.getComponents()) { result.add(child); if (child instanceof Container nested) result.addAll(descendants(nested)); }
        return result;
    }
    static void cleanText(String text, String description) { check(text!=null&&!text.isBlank()&&!text.contains("\ufffd"),description+" is blank/corrupted"); }
    static void translations(DnsSettingsPanel panel, JFrame frame) throws Exception {
        check(I18n.codes().size()==12,"Expected all twelve languages");
        for (String language:I18n.codes()) {
            edt(() -> {
                I18n.setLanguage(language); frame.validate(); layout(frame);
                TitledBorder title=(TitledBorder)((CompoundBorder)panel.getBorder()).getOutsideBorder();
                check(title.getTitle().equals(I18n.tFor(language,"DNS resolution")),"DNS title translation "+language);
                List<Component> nodes=descendants(panel);
                for(String key:List.of("DNS provider","DoH URL","DoT server name","DoT port","Bootstrap IPv4 addresses")) {
                    String expected=I18n.tFor(language,key); cleanText(expected,"Label "+language+" "+key);
                    check(nodes.stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).anyMatch(label->expected.equals(label.getText())),"Missing translated label "+language+" "+key);
                }
                for(JButton button:List.of(panel.apply,panel.test)) {
                    String key=button==panel.apply?"Apply DNS settings":"Test DNS";
                    check(button.getText().equals(I18n.tFor(language,key)),"Button translation "+language);
                    check(button.getToolTipText().equals(I18n.tFor(language,key)),"Button tooltip translation "+language);
                }
                for(JComponent component:List.of(panel.mode,panel.provider,panel.dohUrl,panel.dotHost,panel.dotPort)) {
                    cleanText(component.getToolTipText(),"Secure tooltip "+language);
                    check(component.getToolTipText().equals(I18n.tFor(language,SECURE_HELP)),"Secure tooltip translation "+language);
                }
                check(panel.bootstrap.getToolTipText().equals(I18n.tFor(language,BOOTSTRAP_HELP)),"Bootstrap tooltip "+language);
                check(panel.help.getText().equals(I18n.tFor(language,SECURE_HELP)),"Secure help "+language);
                for(int n=0;n<panel.mode.getItemCount();n++) {
                    String value=panel.mode.getItemAt(n);
                    JLabel rendered=(JLabel)panel.mode.getRenderer().getListCellRendererComponent(new JList<>(),value,n,false,false);
                    check(rendered.getText().equals(I18n.tFor(language,DnsSettingsPanel.modeKey(value))),"Mode renderer "+language+" "+value);
                    cleanText(rendered.getText(),"Mode renderer text");
                }
                for(int n=0;n<panel.provider.getItemCount();n++) {
                    String value=panel.provider.getItemAt(n);
                    JLabel rendered=(JLabel)panel.provider.getRenderer().getListCellRendererComponent(new JList<>(),value,n,false,false);
                    check(rendered.getText().equals(I18n.tFor(language,DnsSettingsPanel.providerKey(value))),"Provider renderer "+language+" "+value);
                }
                check(panel.dohUrl.getComponentOrientation().isLeftToRight(),"URL field direction "+language);
                for(Component node:nodes) {
                    if(node instanceof JLabel label&&label.getText()!=null) check(!label.getText().contains("\ufffd"),"Label Unicode replacement "+language);
                    if(node instanceof JComponent component&&component.getToolTipText()!=null) check(!component.getToolTipText().contains("\ufffd"),"Tooltip Unicode replacement "+language);
                }
            });
        }
        edt(() -> I18n.setLanguage("en")); cases.add("twelve_languages_labels_tooltips_renderers_unicode_and_editable_url_direction");
    }
    static void enabled(DnsSettingsPanel p, boolean system, boolean doh, boolean dot, boolean bootstrap) {
        check(p.mode.isEnabled(),"Mode must remain editable when idle");
        check(p.provider.isEnabled()!=system,"System/provider enabling");
        check(p.dohUrl.isEnabled()==doh,"DoH field enabling");
        check(p.dotHost.isEnabled()==dot&&p.dotPort.isEnabled()==dot,"DoT fields enabling");
        check(p.bootstrap.isEnabled()==bootstrap,"Bootstrap enabling");
    }
    static void configuration(DnsSettingsPanel p) throws Exception {
        edt(() -> {
            check(p.draft().equals(SecureDns.Config.defaults().toJson()),"Strict DoH Cloudflare defaults");
            enabled(p,false,false,false,false);
            for(String mode:List.of("system","doh","dot")) for(String provider:List.of("cloudflare","quad9","google")) {
                p.provider.setSelectedItem(provider); p.mode.setSelectedItem(mode);
                check(p.draft().equals(SecureDns.Config.preset(provider,mode).toJson()),"Provider/mode preset "+provider+" "+mode);
                enabled(p,mode.equals("system"),false,false,false);
                check(p.help.getText().equals(I18n.t(mode.equals("system")?SYSTEM_HELP:SECURE_HELP)),"Mode explanatory text");
            }
            p.provider.setSelectedItem("custom"); p.mode.setSelectedItem("doh"); enabled(p,false,true,false,true);
            p.dohUrl.setText(UNICODE_URL); p.bootstrap.setText("1.1.1.1, 1.0.0.1");
            Map<String,Object> draft=p.draft(); check(draft.get("doh_url").equals(custom().get("doh_url")),"Unicode custom URL normalization");
            p.mode.setSelectedItem("dot"); enabled(p,false,false,true,true);
            p.dotHost.setText("resolver.invalid"); p.dotPort.setValue(8853);
            check(p.draft().get("dot_host").equals("resolver.invalid")&&p.draft().get("dot_port").equals(8853),"Custom DoT endpoint");
            p.mode.setSelectedItem("system"); enabled(p,true,false,false,false);
            p.mode.setSelectedItem("doh"); check(p.dohUrl.getText().equals(UNICODE_URL),"Custom fields survive mode switches");
            Map<String,Object> metadata=applied(custom(),10); Map<String,Object> projection=DnsSettingsPanel.projection(metadata);
            check(projection.keySet().equals(CONFIG_KEYS),"Readonly metadata leaked into persistence projection");
            check(SecureDns.Config.fromJson(projection).toJson().equals(custom()),"Projection not valid config");
            check(!projection.containsKey("revision")&&!projection.containsKey("managed_dns"),"Revision/runtime state persisted");
        }); cases.add("strict_defaults_three_modes_provider_presets_custom_unicode_and_readonly_projection");
    }
    static final class Call {
        final boolean test; final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        final AtomicReference<Map<String,Object>> config=new AtomicReference<>();
        final AtomicReference<List<String>> gateways=new AtomicReference<>();
        Map<String,Object> result; Exception failure;
        Call(boolean test,Map<String,Object> result) {this.test=test;this.result=result;}
    }
    static final class Controller implements DnsSettingsPanel.Controller {
        final AtomicReference<Call> next=new AtomicReference<>(); final AtomicInteger calls=new AtomicInteger();
        public Map<String,Object> apply(Map<String,Object> config)throws Exception{return execute(false,config,List.of());}
        public Map<String,Object> test(Map<String,Object> config,List<String> gateways)throws Exception{return execute(true,config,gateways);}
        Map<String,Object> execute(boolean test,Map<String,Object> config,List<String> gateways)throws Exception {
            Call call=next.getAndSet(null); if(call==null||call.test!=test)throw new AssertionError("Unexpected owned controller operation");
            if(SwingUtilities.isEventDispatchThread())throw new AssertionError("Controller called on EDT");
            if(!config.keySet().equals(CONFIG_KEYS))throw new AssertionError("Controller received readonly metadata");
            calls.incrementAndGet();call.config.set(new LinkedHashMap<>(config));call.gateways.set(List.copyOf(gateways));call.entered.countDown();
            if(!call.release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Owned controller not released");
            if(call.failure!=null)throw call.failure;return call.result;
        }
    }
    static void operation(DnsSettingsPanel p,Controller c,Call call,Work during) throws Exception {
        c.next.set(call);
        try {
            edt(() -> {if(call.test)p.test.doClick(0);else p.apply.doClick(0);});
            check(call.entered.await(5,TimeUnit.SECONDS),"Controller did not start");
            edt(() -> {
                check(p.isBusy(),"Async operation not busy");
                check(!p.mode.isEnabled()&&!p.provider.isEnabled()&&!p.apply.isEnabled()&&!p.test.isEnabled(),"Busy controls enabled");
                during.run();
            });
        } finally {call.release.countDown();}
        waitFor(()->idle(p),"Async operation did not finish");
    }
    static Map<String,Object> results() {
        List<Object> rows=new ArrayList<>();
        for(int n=0;n<4;n++) rows.add(n<3?Map.of("gateway",GATEWAYS.get(n),"ok",true,"elapsed_ms",n+1,"addresses",List.of("1.1.1.1")):
                Map.of("gateway",GATEWAYS.get(n),"ok",false,"error","Owned gateway unavailable Проверка_日本語"));
        return Map.of("results",rows);
    }
    static void async(DnsSettingsPanel p,Controller controller,AtomicReference<List<String>> selected) throws Exception {
        AtomicReference<Map<String,Object>> submitted=new AtomicReference<>();
        edt(() -> {p.mode.setSelectedItem("doh");p.provider.setSelectedItem("custom");p.dohUrl.setText(UNICODE_URL);submitted.set(p.draft());});
        Call test=new Call(true,results()); operation(p,controller,test,()->check(p.hasUnsavedDraft(),"Testing cleared draft before completion"));
        edt(() -> {
            check(test.config.get().equals(submitted.get()),"Test did not capture draft");check(test.gateways.get().equals(GATEWAYS),"Selected gateways lost");
            check(p.hasUnsavedDraft(),"Test saved unsaved configuration");
            check(p.status.getText().equals(I18n.t("DNS test complete: %s/%s gateways",3L,4)),"Partial gateway count");
            for(String gateway:GATEWAYS)check(p.results.text.getText().contains(gateway),"Missing per-gateway result");
            check(p.results.text.getText().contains("Проверка_日本語")&&!p.results.text.getText().contains("\ufffd"),"Unicode per-gateway failure");
        });
        int before=controller.calls.get(); selected.set(List.of());
        edt(() -> {p.test.doClick(0);check(!p.isBusy(),"Empty gateway validation became async");check(!p.status.getText().isBlank(),"Empty gateway diagnostic absent");});
        check(controller.calls.get()==before,"Empty gateway validation called controller");selected.set(GATEWAYS);
        Call apply=new Call(false,applied(submitted.get(),50)); operation(p,controller,apply,()->{});
        edt(() -> {check(!p.hasUnsavedDraft(),"Successful apply left draft dirty");check(p.draft().equals(submitted.get()),"Apply changed config");check(p.status.getText().equals(I18n.t("DNS settings saved")),"Apply success status");});
        Call failure=new Call(false,Map.of());failure.failure=new IllegalArgumentException("Invalid DoT port");
        edt(() -> p.dohUrl.setText(UNICODE_URL+"/черновик"));
        operation(p,controller,failure,()->{});
        edt(() -> {check(p.hasUnsavedDraft(),"Failed apply discarded draft");check(p.dohUrl.getText().endsWith("/черновик"),"Failed apply overwrote editable text");check(!p.status.getText().isBlank(),"Failed apply diagnostic absent");check(p.apply.isEnabled(),"Failed apply did not re-enable controls");});
        cases.add("async_off_edt_apply_test_busy_validation_partial_gateway_failure_and_failed_apply_preserve_draft");
    }
    static void preserve(DnsSettingsPanel p,Map<String,Object> update) throws Exception {
        AtomicReference<Document> document=new AtomicReference<>();AtomicReference<String> text=new AtomicReference<>();AtomicInteger dot=new AtomicInteger(),mark=new AtomicInteger();
        edt(() -> {document.set(p.dohUrl.getDocument());text.set(p.dohUrl.getText());p.dohUrl.setCaretPosition(9);p.dohUrl.moveCaretPosition(18);dot.set(p.dohUrl.getCaret().getDot());mark.set(p.dohUrl.getCaret().getMark());});
        for(String language:I18n.codes()) edt(() -> {p.refresh(update);I18n.setLanguage(language);check(p.dohUrl.getDocument()==document.get(),"Document replaced by refresh/language");check(p.dohUrl.getText().equals(text.get()),"Dirty draft overwritten by refresh/language");check(p.dohUrl.getCaret().getDot()==dot.get()&&p.dohUrl.getCaret().getMark()==mark.get(),"Selection/caret reset by refresh/language "+language);check(p.hasUnsavedDraft(),"Dirty flag reset by refresh/language");});
        edt(()->I18n.setLanguage("en"));cases.add("dirty_draft_document_selection_and_caret_survive_twelve_languages_and_new_applied_config");
    }
    static Map<String,Object> networkState(Map<String,Object> dns) {
        List<String> routes=new ArrayList<>();for(int n=1;n<=32;n++)routes.add("192.168.1."+n);
        return Map.of("virtual_ip","192.168.1.70","gateways",routes,"dns",dns,"network_validation",Map.of("status","valid","computer_ipv4","192.168.1.90","network_cidr","192.168.1.0/24"));
    }
    static int middle(JScrollPane pane) {
        JScrollBar bar=pane.getVerticalScrollBar();int limit=bar.getMaximum()-bar.getVisibleAmount();check(limit>40,"Network pane not scrollable");
        bar.setValue(Math.max(20,limit/2));int y=pane.getViewport().getViewPosition().y;check(y>0,"Network pane did not scroll");return y;
    }
    static void timerHold(DesktopUI ui,AtomicInteger reads,boolean dirty) throws Exception {
        DnsSettingsPanel p=ui.dnsPanel;JScrollPane pane=(JScrollPane)ui.mainTabs.getComponentAt(1);AtomicInteger y=new AtomicInteger(),dot=new AtomicInteger(),mark=new AtomicInteger();AtomicReference<String> text=new AtomicReference<>();Document document=p.dohUrl.getDocument();
        edt(() -> {p.dohUrl.requestFocusInWindow();if(dirty)p.dohUrl.setText(UNICODE_URL+"/черновик");p.dohUrl.setCaretPosition(9);p.dohUrl.moveCaretPosition(18);text.set(p.dohUrl.getText());dot.set(p.dohUrl.getCaret().getDot());mark.set(p.dohUrl.getCaret().getMark());});drain();
        edt(() -> y.set(middle(pane)));int first=reads.get();
        waitFor(()->reads.get()>=first+3,"Actual desktop 900ms timer did not refresh DNS");
        edt(() -> {
            int actual=pane.getViewport().getViewPosition().y;observations.put(dirty?"dirty_scroll_before":"clean_scroll_before",y.get());observations.put(dirty?"dirty_scroll_after":"clean_scroll_after",actual);
            check(Math.abs(actual-y.get())<=1,"900ms DNS refresh moved parent scroll "+y.get()+" -> "+actual);
            check(p.dohUrl.getDocument()==document&&p.dohUrl.getText().equals(text.get()),"900ms refresh replaced DNS editor/draft");
            check(p.dohUrl.getCaret().getDot()==dot.get()&&p.dohUrl.getCaret().getMark()==mark.get(),"900ms gateway-status-only refresh reset DNS caret");
            check(p.hasUnsavedDraft()==dirty,"900ms refresh changed dirty state");
        });
        if(dirty)for(String language:List.of("ru","ar","en")) {
            edt(()->{I18n.setLanguage(language);ui.frame.validate();layout(ui.frame);});drain();
            edt(()->{check(Math.abs(pane.getViewport().getViewPosition().y-y.get())<=1,"Language change moved DNS parent scroll "+language);check(p.dohUrl.getText().equals(text.get())&&p.dohUrl.getCaret().getDot()==dot.get()&&p.dohUrl.getCaret().getMark()==mark.get(),"Language change lost DNS draft/caret "+language);});
        }
        cases.add(dirty?"embedded_desktop_dirty_dns_draft_caret_scroll_real900ms_timer_and_languages":"embedded_desktop_clean_dns_caret_scroll_real900ms_status_only_refresh");
    }
    static void embedded(Path own) throws Exception {
        AtomicInteger reads=new AtomicInteger();AtomicReference<DesktopUI> reference=new AtomicReference<>();CountDownLatch closed=new CountDownLatch(1);
        Main.Manager manager=new Main.Manager(own.resolve("profile"),(url,route)->{network.incrementAndGet();throw new AssertionError("No source inspection");},path->Long.MAX_VALUE,(route,torrent)->{network.incrementAndGet();throw new AssertionError("No route validation");});
        List<String> routes=new ArrayList<>();for(int n=1;n<=32;n++)routes.add("192.168.1."+n);
        Files.writeString(manager.dataDir.resolve("desktop-settings.config"),Json.stringify(Map.of("gateways",routes,"virtual_ip","192.168.1.70","language","en","theme","light")),StandardCharsets.UTF_8);
        AppLog.initialize(own.resolve("owned-log"));
        try {
            edt(() -> {
                DesktopUI ui=new DesktopUI(manager,closed::countDown,false,(gateway,url)->{network.incrementAndGet();throw new AssertionError("No discovery");},(ip,values)->{network.incrementAndGet();throw new AssertionError("No network configuration");},(route,url)->{network.incrementAndGet();throw new AssertionError("No speed test");},()->networkState(applied(custom(),100+reads.incrementAndGet())));
                reference.set(ui);check(ui.dnsPanel!=null,"DNS panel not embedded in Network tab");
                ui.dnsPanel.controller=new Controller();ui.frame.setSize(1180,720);ui.frame.setVisible(true);ui.mainTabs.setSelectedIndex(1);ui.frame.validate();layout(ui.frame);
            });drain();DesktopUI ui=reference.get();timerHold(ui,reads,false);timerHold(ui,reads,true);observations.put("desktop_network_settings_reads",reads.get());
        } finally {
            if(reference.get()!=null){edt(()->reference.get().frame.dispose());check(closed.await(8,TimeUnit.SECONDS),"Owned desktop did not close");}
            manager.shutdown();AppLog.close();
        }
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();if(args.length!=1)throw new IllegalArgumentException("Expected one owned output directory");
        Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);Files.deleteIfExists(own.resolve("DNS-SETTINGS-UI-TESTS.json"));
        SourceTransfer.endpoints=route->{network.incrementAndGet();throw new AssertionError("No gateway endpoint");};
        ExecutorService background=Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"owned-dns-ui-controller");thread.setDaemon(true);return thread;});
        Controller controller=new Controller();AtomicReference<List<String>> selected=new AtomicReference<>(GATEWAYS);
        AtomicReference<DnsSettingsPanel> panel=new AtomicReference<>();AtomicReference<JFrame> frame=new AtomicReference<>();
        try {
            edt(()->{I18n.setLanguage("en");DnsSettingsPanel p=new DnsSettingsPanel(controller,background,selected::get);panel.set(p);JFrame f=new JFrame("Owned DNS settings Проверка_日本語");frame.set(f);f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);f.setContentPane(new JScrollPane(p));f.setSize(940,850);f.setVisible(true);f.validate();layout(f);});drain();
            translations(panel.get(),frame.get());configuration(panel.get());async(panel.get(),controller,selected);
            preserve(panel.get(),applied(SecureDns.Config.preset("quad9","dot").toJson(),1000));
        } finally {
            edt(()->{if(panel.get()!=null)panel.get().close();if(frame.get()!=null)frame.get().dispose();});background.shutdownNow();check(background.awaitTermination(5,TimeUnit.SECONDS),"Owned executor did not stop");
        }
        embedded(own);check(network.get()==0,"UI fixture attempted external network");
        Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("checks",checks);report.put("cases",cases);report.put("languages",I18n.codes());report.put("observations",observations);report.put("real_timer_interval_ms",900);report.put("network_calls",network.get());report.put("headless",false);report.put("injected_controller",true);report.put("owned_profile_only",true);report.put("installed_app_accessed",false);report.put("os_network_settings_changed",false);report.put("user_profile_or_logs_read",false);report.put("user_media_or_clipboard_read",false);report.put("utf8_example","Проверка_日本語_é");
        String json=Json.stringify(report);check(!json.contains("\ufffd"),"Receipt Unicode replacement");Files.writeString(own.resolve("DNS-SETTINGS-UI-TESTS.json"),json,StandardCharsets.UTF_8);check(Files.readString(own.resolve("DNS-SETTINGS-UI-TESTS.json"),StandardCharsets.UTF_8).equals(json),"Receipt UTF8 roundtrip");System.out.println(json);
    }
}
