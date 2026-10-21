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
import javax.imageio.ImageIO;
import javax.swing.*;

/** Owned modern-desktop fixture: no source, browser, user profile, media or clipboard access. */
public final class ModernDesktopTest40 {
    static int checks;
    static final List<String> cases = new ArrayList<>(), images = new ArrayList<>();
    static final AtomicInteger networkCalls = new AtomicInteger();
    static final String unicodeName = "Модель_日本語_é_🚀_large-file.gguf";
    interface Work { void run() throws Exception; }
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() instanceof Exception failure) throw failure;
        if (error.get() instanceof Error failure) throw failure;
    }
    static void drain() throws Exception { for (int index = 0; index < 5; index++) edt(() -> { }); }
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    static void update(DesktopUI ui, List<Object> snapshots) throws Exception {
        Method method = DesktopUI.class.getDeclaredMethod("updateJobs", List.class); method.setAccessible(true); method.invoke(ui, snapshots);
    }
    static void layout(Container parent) {
        parent.doLayout(); for (Component child : parent.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    static Map<String,Object> job(Path own, String id, String name, String state, long speed) {
        Map<String,Object> job = new LinkedHashMap<>();
        job.put("id",id); job.put("filename",name); job.put("directory",own.resolve("Файлы_日本語_é_🚀").toString());
        job.put("status",state); job.put("url","https://example.invalid/owned/"+id); job.put("size",1000000L);
        job.put("downloaded","complete".equals(state)?1000000L:500000L); job.put("speed_bps",speed);
        job.put("routes",List.of(Map.of("gateway","192.168.1.1","enabled",true,"speed_bps",speed,"bytes",500000)));
        job.put("lifecycle_history",List.of(
                Map.of("kind","started","time","2026-10-18T08:01:02Z","filename",name,"path",DesktopUI.fullJobPath(job)),
                Map.of("kind","resumed","time","2026-10-18T08:03:04Z","filename",name,"path",DesktopUI.fullJobPath(job)),
                Map.of("kind","completed","time","2026-10-18T08:05:06Z","filename",name,"path",DesktopUI.fullJobPath(job))));
        return job;
    }
    static double luminance(Color color) {
        double[] channels = {color.getRed()/255.0,color.getGreen()/255.0,color.getBlue()/255.0};
        for(int index=0;index<channels.length;index++) channels[index] = channels[index] <= .04045 ? channels[index]/12.92 : Math.pow((channels[index]+.055)/1.055,2.4);
        return channels[0]*.2126+channels[1]*.7152+channels[2]*.0722;
    }
    static double contrast(Color one, Color other) { double a=luminance(one),b=luminance(other); return (Math.max(a,b)+.05)/(Math.min(a,b)+.05); }
    static void pure(Path own) {
        Map<String,Object> job=job(own,"owned-unicode",unicodeName,"running",1250000000L);
        String path=DesktopUI.fullJobPath(job);
        check(Path.of(path).isAbsolute()&&path.endsWith(unicodeName),"Unicode full target path is not absolute or lost its filename");
        String escaped=DesktopUI.escapeTooltip("<img src='owned'>&\"\nМодель_日本語_é_🚀");
        check(escaped.equals("&lt;img src=&#39;owned&#39;&gt;&amp;&quot;<br>Модель_日本語_é_🚀"),"Tooltip does not escape all HTML-sensitive data while retaining Unicode");
        String longName="W".repeat(120)+"_日本語_🚀_"+"W".repeat(60)+".gguf";
        String wrapped=DesktopUI.wrapTooltipText(longName);
        check(wrapped.replace("<br>","").equals(longName),"Tooltip wrapping lost characters or split an emoji");
        JToolTip tip=new JToolTip();tip.setTipText("<html>"+wrapped+"</html>");
        check(tip.getPreferredSize().width<=750,"Long tooltip extends beyond a normal screen");
        for(String theme:List.of("light","dark","system")) {
            DesktopTheme.install(theme);
            check(contrast(DesktopTheme.foreground(),DesktopTheme.surface())>=9,"Main foreground has insufficient contrast: "+theme);
            check(contrast(DesktopTheme.muted(),DesktopTheme.surface())>=4.5,"Muted labels have insufficient contrast: "+theme);
            check(contrast(DesktopTheme.accent(),DesktopTheme.surface())>=4.5,"Accent has insufficient contrast: "+theme);
            if(!theme.equals("system"))check(UIManager.getLookAndFeel().getClass().getName().equals("com.formdev.flatlaf.Flat"+(theme.equals("dark")?"Dark":"Light")+"Laf"),"FlatLaf is not available from the tested classpath");
        }
        check(DesktopTheme.normalize(null).equals("light")&&DesktopTheme.normalize("unexpected").equals("light"),"Invalid theme must default to light");
        check(DesktopUI.formatHistoryTime("2026-10-18T08:01:02Z").startsWith("2026-10-18 "),"Timeline lost its full date");
        cases.add("absolute_unicode_target_and_html_injection_escaping");
        cases.add("actual_bundled_flatlaf_light_dark_and_system_theme_contrast");
    }
    static String tooltip(DesktopUI ui,int row,int modelColumn) {
        int column=ui.jobsTable.convertColumnIndexToView(modelColumn);
        Rectangle cell=ui.jobsTable.getCellRect(row,column,true);
        return ui.jobsTable.getToolTipText(new MouseEvent(ui.jobsTable,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,cell.x+2,cell.y+2,0,false));
    }
    static void buttonVisible(DesktopUI ui,Component button) {
        Rectangle bounds=SwingUtilities.convertRectangle(button.getParent(),button.getBounds(),ui.frame.getContentPane());
        check(button.isVisible()&&button.getWidth()>=button.getPreferredSize().width,"Control text clipped: "+button);
        check(new Rectangle(0,0,ui.frame.getContentPane().getWidth(),ui.frame.getContentPane().getHeight()).contains(bounds),"Header control outside frame: "+button);
        check(((JComponent)button).getVisibleRect().contains(new Rectangle(0,0,button.getWidth(),button.getHeight())),"Control clipped by ancestor: "+button);
        if(button instanceof AbstractButton action)check(action.getFont().canDisplayUpTo(action.getText())<0,"Control has missing translated glyphs: "+action.getText());
    }
    static void emphasisReadable(DesktopUI ui) {
        for(JComponent component:List.of(ui.createButton,ui.aggregateSpeedLabel,ui.totalSpeedLabel)) {
            Font font=component.getFont();
            check(font!=null&&font.getSize2D()>=14f,"Primary download action or speed text is smaller than 14 points: "+font);
            check(font.isBold(),"Primary download action or speed text lost its bold emphasis: "+font);
        }
    }
    static void capture(DesktopUI ui,Path own,String theme,String language) throws Exception {
        emphasisReadable(ui);
        BufferedImage image=new BufferedImage(ui.frame.getContentPane().getWidth(),ui.frame.getContentPane().getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();try{ui.frame.getContentPane().printAll(graphics);}finally{graphics.dispose();}
        Path file=own.resolve("OWN-DESKTOP-"+theme+"-"+language+".png");
        check(ImageIO.write(image,"png",file.toFile()),"Own window preview could not be saved");images.add(file.toString());
    }

    static String anchorDate(LogView view) throws Exception {
        int offset=view.text.viewToModel2D(view.getViewport().getViewPosition());
        int line=view.text.getLineOfOffset(offset),start=view.text.getLineStartOffset(line),end=view.text.getLineEndOffset(line);
        return view.text.getText(start,Math.min(19,end-start));
    }

    static void historyLog(DesktopUI ui,Path own) throws Exception {
        Map<String,Object> state=job(own,"ab01cd23ef45",unicodeName,"running",0);
        List<Object> entries=new ArrayList<>();
        for(int index=0;index<100;index++) entries.add(Map.of("event_id",UUID.randomUUID().toString(),"job_id","ab01cd23ef45",
                "kind",index==0?"started":"resumed","time",java.time.Instant.parse("2026-10-18T08:01:02Z").plusSeconds(index).toString(),
                "filename",unicodeName,"path",DesktopUI.fullJobPath(state)));
        state.put("lifecycle_history",entries);
        DownloadHistory journal=new DownloadHistory(own.resolve("owned-profile"),state);journal.checkpoint(state);
        edt(()->{ui.openLog(false);((JTabbedPane)field(ui,"logTabs")).setSelectedIndex(1);ui.logDialog.validate();layout(ui.logDialog.getContentPane());});drain();
        LogView view=(LogView)field(ui,"downloadLogView");
        edt(()->{
            check(view.text.getText().contains(unicodeName)&&view.text.getText().contains(DesktopUI.fullJobPath(state)),"Durable history tab lost Unicode filename or exact full path");
            check(view.text.getText().contains("2026-10-18"),"Durable history tab lost event dates");
            view.getViewport().setViewPosition(new Point(0,450));
        });drain();
        String[] anchor={null};edt(()->anchor[0]=anchorDate(view));
        entries.add(Map.of("event_id",UUID.randomUUID().toString(),"job_id","ab01cd23ef45","kind","completed",
                "time","2026-10-18T08:09:10Z","filename",unicodeName,"path",DesktopUI.fullJobPath(state)));
        journal.checkpoint(state);
        edt(()->{javax.swing.Timer timer=(javax.swing.Timer)field(ui,"logTimer");for(ActionListener listener:timer.getActionListeners())listener.actionPerformed(new ActionEvent(timer,0,"owned-refresh"));});drain();
        edt(()->{check(anchor[0].equals(anchorDate(view)),"Durable history refresh reset the visible event");ui.languageCombo.setSelectedItem("de");});drain();
        edt(()->{
            check(anchor[0].equals(anchorDate(view)),"Changing history language reset the visible event date");
            check(view.text.getText().contains(unicodeName)&&view.text.getText().contains(DesktopUI.fullJobPath(state)),"Translated durable history altered a filename/path");
            check(view.text.getText().contains("2026-10-18")&&!view.text.getText().contains("\ufffd"),"Translated durable history lost UTF-8/date integrity");
            ui.logDialog.dispose();ui.languageCombo.setSelectedItem("en");
        });drain();
        cases.add("durable_history_tab_unicode_dates_append_and_language_switch_keep_reader_anchor");
    }
    static void desktop(Path own,boolean visible) throws Exception {
        AppLog.initialize(own.resolve("owned-log"));
        Main.Manager manager=new Main.Manager(own.resolve("owned-profile"),(url,route)->{networkCalls.incrementAndGet();throw new AssertionError("No source access");});
        AtomicReference<DesktopUI> ref=new AtomicReference<>();CountDownLatch closed=new CountDownLatch(1);
        try {
            edt(()->{
                // Match DesktopUI.open() before its component field initializers run.
                DesktopTheme.install("light");
                DesktopUI.installReadableFonts();
                DesktopUI ui=new DesktopUI(manager,closed::countDown,false,(gateway,url)->{networkCalls.incrementAndGet();throw new AssertionError("No discovery");},
                        (ip,gateways)->{networkCalls.incrementAndGet();throw new AssertionError("No route configuration");},
                        (route,url)->{networkCalls.incrementAndGet();throw new AssertionError("No speed tests");},null,
                        (url,route)->{networkCalls.incrementAndGet();throw new AssertionError("No hub requests");},(owner,inspection)->null,
                        (source,route)->{networkCalls.incrementAndGet();throw new AssertionError("No torrent requests");},(owner,inspection)->null,
                        (owner,job)->DesktopUI.DeleteChoice.CANCEL,(owner,directory)->null,uri->{throw new AssertionError("No browser launch");});
                ref.set(ui);((javax.swing.Timer)field(ui,"refreshTimer")).stop();
                ui.frame.setSize(1080,700);if(visible)ui.frame.setVisible(true);else ui.frame.addNotify();
                ui.frame.validate();layout(ui.frame.getContentPane());emphasisReadable(ui);
            });drain();DesktopUI ui=ref.get();
            check(manager.browserDownload!=null,"Desktop did not attach its browser intake");
            edt(()->{Field running=DesktopUI.class.getDeclaredField("createRunning");running.setAccessible(true);running.setBoolean(ui,true);});
            manager.browserDownload.accept("https://example.invalid/owned-first");manager.browserDownload.accept("https://example.invalid/owned-second");drain();
            edt(()->{Deque<?> queued=(Deque<?>)field(ui,"pendingBrowserUrls");check(queued.size()==2&&queued.getFirst().equals("https://example.invalid/owned-first"),"Browser links were dropped while download creation was busy");queued.clear();Field running=DesktopUI.class.getDeclaredField("createRunning");running.setAccessible(true);running.setBoolean(ui,false);});
            List<Object> snapshots=new ArrayList<>();
            snapshots.add(job(own,"owned-unicode",unicodeName,"running",1250000000L));
            snapshots.add(job(own,"owned-html","<img src='owned'> & \"file\".gguf","complete",0));
            for(int index=0;index<35;index++)snapshots.add(job(own,"owned-"+index,String.format(Locale.ROOT,"owned-file-%02d.bin",index),"paused",0));
            edt(()->{update(ui,snapshots);ui.jobsSorter.setSortKeys(List.of(new RowSorter.SortKey(0,SortOrder.DESCENDING)));});drain();
            edt(()->{
                int model=new ArrayList<>(((Map<?,?>)field(ui,"jobs")).keySet()).indexOf("owned-unicode"),view=ui.jobsTable.convertRowIndexToView(model);
                String tip=tooltip(ui,view,0);
                check(tip.contains(unicodeName)&&tip.replace("<br>","").contains(DesktopUI.escapeTooltip(DesktopUI.fullJobPath((Map<String,Object>)snapshots.get(0)))),"Sorted hover tooltip shows another job or truncates Unicode path");
                ui.jobsTable.setRowSelectionInterval(view,view);
                check(ui.selectionLabel.getToolTipText().equals(tip),"Selected filename tooltip differs from its table row");
                int html=ui.jobsTable.convertRowIndexToView(1);String injected=tooltip(ui,html,0);
                check(injected.contains("&lt;img src=&#39;owned&#39;&gt; &amp; &quot;file&quot;")&&!injected.contains("<img"),"Completed filename became executable HTML");
                check(tooltip(ui,view,2).contains("<br><br>")&&tooltip(ui,view,2).contains(unicodeName),"Progress hover lost full filename/path");
                check(ui.jobsTable.getToolTipText(new MouseEvent(ui.jobsTable,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,-1,-1,0,false))==null,"Empty table space must not display an unrelated job");
                Object original=snapshots.get(1);String longName="W".repeat(120)+"_日本語_🚀_"+"W".repeat(60)+".gguf";
                snapshots.set(1,job(own,"owned-long",longName,"complete",0));update(ui,snapshots);
                String longTip=tooltip(ui,ui.jobsTable.convertRowIndexToView(1),0);JToolTip actual=ui.jobsTable.createToolTip();actual.setTipText(longTip);
                check(actual.getPreferredSize().width<=750,"Actual long filename/path tooltip is wider than a normal screen");
                check(longTip.replace("<br>","").contains(longName),"Actual long filename tooltip lost characters");
                snapshots.set(1,original);update(ui,snapshots);
            });
            edt(()->ui.searchField.setText("Модель_日本語"));drain();
            edt(()->{check(ui.jobsTable.getRowCount()==1&&tooltip(ui,0,0).contains(unicodeName),"Search filter breaks tooltip row mapping");
                ui.searchField.setText("Файлы_日本語");check(ui.jobsTable.getRowCount()==37,"Directory search does not find paths");
                ui.searchField.setText("");ui.jobFilter.setSelectedItem("complete");check(ui.jobsTable.getRowCount()==1&&tooltip(ui,0,0).contains("&lt;img"),"Completed filter maps to the wrong filename");
                ui.jobFilter.setSelectedItem("all");
                check(ui.jobsTable.getComponentPopupMenu().getComponentCount()==8,"Existing menu actions or new copy-path action missing");
                int shortcut=Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                check(ui.frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_L,shortcut)).equals("multiroute.focus-url"),"URL shortcut not installed");
                check(ui.frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_F,shortcut)).equals("multiroute.focus-search"),"Search shortcut not installed");
                ui.searchField.setText("owned");ui.searchField.getActionMap().get("multiroute.clear-search").actionPerformed(new ActionEvent(ui,0,"owned"));
                check(ui.searchField.getText().isEmpty(),"Escape does not clear search");
                check(ui.aggregateSpeedLabel.getText().contains("10.0"),"Aggregate speed does not show the 10 Gbit/s active transfer");
                check(ui.summaryLabel.getText().contains("37")&&ui.summaryLabel.getText().contains("1"),"Download summary is missing task counts");
                check(ui.detailTabs.getTabCount()==2&&ui.detailTabs.getSelectedIndex()==0,"Details/history tabs did not preserve the existing initial view");
            });
            cases.add("sorted_filtered_running_completed_table_tooltips_show_actual_full_paths");
            cases.add("all_existing_context_actions_shortcuts_search_and_aggregate_metrics_preserved");
            final String enteredUrl="https://example.invalid/Модель_日本語_🚀?name=é";
            edt(()->ui.urlField.setText(enteredUrl));
            for(String language:I18n.codes()) {
                edt(()->{ui.languageCombo.setSelectedItem(language);emphasisReadable(ui);});drain();
                for(String theme:List.of("light","dark")) {
                    edt(()->{ui.themeCombo.setSelectedItem(theme);ui.frame.validate();layout(ui.frame.getContentPane());});drain();
                    edt(()->{
                        emphasisReadable(ui);
                        buttonVisible(ui,ui.instructionsButton);buttonVisible(ui,ui.apiGuideButton);buttonVisible(ui,ui.languageCombo);
                        buttonVisible(ui,ui.logButton);buttonVisible(ui,ui.exitButton);buttonVisible(ui,ui.themeCombo);buttonVisible(ui,ui.createButton);buttonVisible(ui,ui.browserButton);buttonVisible(ui,ui.queueButton);
                        check(contrast(ui.jobsTable.getForeground(),ui.jobsTable.getBackground())>=9,"Themed table contrast is unreadable");
                        check(ui.jobsTable.getBackground().equals(DesktopTheme.surface()),"Actual table retained an old theme background");
                        check(ui.frame.getContentPane().getBackground().equals(DesktopTheme.background()),"Actual root panel retained an old theme background");
                        check(contrast(ui.instructionsButton.getForeground(),ui.instructionsButton.getBackground())>=4.5,"Actual help button contrast is unreadable");
                        check(ui.frame.getContentPane().getComponentOrientation().isLeftToRight()!=language.equals("ar"),"Actual window direction does not match the selected language");
                        check(ui.urlField.getComponentOrientation().isLeftToRight()&&ui.directoryField.getComponentOrientation().isLeftToRight(),"URL and path fields must retain logical LTR order");
                        check(ui.urlField.getText().equals(enteredUrl),"A language/theme change altered the entered Unicode URL");
                        if(List.of("zh","ko","ar").contains(language)) {
                            String glyphs=language.equals("zh")?"使用说明":language.equals("ko")?"사용안내":"التعليمات";
                            check(ui.instructionsButton.getFont().canDisplayUpTo(glyphs)<0,"Actual help button font cannot display the selected language");
                            check(ui.instructionsButton.getText().codePoints().anyMatch(cp->language.equals("zh")?cp>=0x3400&&cp<=0x9fff:language.equals("ko")?cp>=0xac00&&cp<=0xd7a3:cp>=0x600&&cp<=0x6ff),"New language UI fell back to English");
                        }
                        if(language.equals("it"))check(ui.instructionsButton.getText().equals("Istruzioni"),"Italian instructions label is not translated");
                        for(JButton action:List.of(ui.pauseButton,ui.resumeButton,ui.changeGatewaysButton,ui.optionsButton,ui.openFolderButton,ui.changeDirectoryButton,ui.removeButton)) {
                            action.scrollRectToVisible(new Rectangle(0,0,action.getWidth(),action.getHeight()));
                            buttonVisible(ui,action);
                        }
                        ui.detailTabs.scrollRectToVisible(new Rectangle(0,0,ui.detailTabs.getWidth(),Math.min(45,ui.detailTabs.getHeight())));
                        check(ui.detailTabs.getVisibleRect().height>=Math.min(35,ui.detailTabs.getHeight()),"Detail/history tabs are not reachable by scrolling the details pane");
                        check(!ui.summaryLabel.getText().contains("\ufffd")&&!ui.historyArea.getText().contains("\ufffd"),"UI text contains Unicode replacement characters");
                        ui.detailScroll.getViewport().setViewPosition(new Point(0,0));
                        if(List.of("en","zh","ko","ar","it","ja","hi","vi").contains(language))capture(ui,own,theme,language);
                    });
                }
            }
            cases.add("twelve_languages_visible_header_actions_at_1080x700_in_light_and_dark");
            cases.add("public_launch_font_bootstrap_and_custom_bold_labels_stay_at_least_14_points");
            historyLog(ui,own);
            for(String theme:List.of("light","dark")) {
                edt(()->{
                    ui.languageCombo.setSelectedItem("en");ui.themeCombo.setSelectedItem(theme);
                    update(ui,List.of(snapshots.get(0),job(own,"owned-demo-complete","Project_archive.zip","complete",0),
                            job(own,"owned-demo-pause","Linux_image.iso","paused",0)));
                    ui.frame.setSize(1400,950);
                    ui.frame.validate();layout(ui.frame.getContentPane());int row=ui.jobsTable.convertRowIndexToView(0);ui.jobsTable.setRowSelectionInterval(row,row);
                    ((JSplitPane)ui.jobsScroll.getParent()).setDividerLocation(170);
                    ui.frame.validate();layout(ui.frame.getContentPane());
                    ui.jobsScroll.getViewport().setViewPosition(new Point(0,0));
                    ui.detailTabs.setSelectedIndex(0);ui.detailScroll.getViewport().setViewPosition(new Point(0,0));
                    Map<JViewport,Boolean> viewports=new LinkedHashMap<>();collectViewports(ui.mainTabs.getComponentAt(0),viewports);
                    for(JViewport viewport:viewports.keySet()) if(viewport.getView()!=ui.jobsTable)viewport.setViewPosition(new Point(0,0));
                });drain();
                edt(()->capture(ui,own,"release-"+theme,"en"));
            }
            edt(()->{
                update(ui,snapshots);
                ui.languageCombo.setSelectedItem("ru");int row=ui.jobsTable.convertRowIndexToView(0);ui.jobsTable.setRowSelectionInterval(row,row);
                check(ui.historyArea.getText().contains(unicodeName)&&ui.historyArea.getText().contains("2026-10-18")&&ui.historyArea.getText().contains("Загрузка продолжена"),"Selected history lacks its exact dates, resume event or Unicode filename");
                ui.jobsScroll.getViewport().setViewPosition(new Point(0,200));
                ui.directoryField.setText(own.resolve("Папка_日本語_🚀").toString());ui.urlField.setText("https://example.invalid/owned/Модель");
            });drain();
            final Point[] keep={null};final String[] id={null};
            edt(()->{keep[0]=ui.jobsScroll.getViewport().getViewPosition();id[0]=(String)field(ui,"selectedId");ui.themeCombo.setSelectedItem("dark");});drain();
            edt(()->{
                check(id[0].equals(field(ui,"selectedId")),"Theme change changed selected download");
                check(keep[0].equals(ui.jobsScroll.getViewport().getViewPosition()),"Theme change reset list scroll position");
                check(ui.directoryField.getText().contains("Папка_日本語_🚀")&&ui.urlField.getText().endsWith("Модель"),"Theme change replaced user form values");
                check(ui.historyArea.getText().contains("2026-10-18")&&ui.historyArea.getText().contains(unicodeName),"Theme switch destroyed the selected history");
                ui.saveSettings();
            });
            check(networkCalls.get()==0,"Owned modern-desktop test attempted external network access");
            cases.add("theme_switch_preserves_selected_job_scroll_forms_history_and_utf8_settings");
        } finally {
            if(ref.get()!=null){edt(()->ref.get().frame.dispose());check(closed.await(8,TimeUnit.SECONDS),"Owned UI workers did not close");}
            check(manager.browserDownload==null,"Closed desktop retained its browser intake");manager.shutdown();AppLog.close();
        }
        String settings=Files.readString(own.resolve("owned-profile/desktop-settings.config"),StandardCharsets.UTF_8);
        check(!settings.contains("\ufffd")&&settings.contains("Папка_日本語_🚀"),"UTF-8 settings lost Unicode data");
        check(Main.obj(Json.parse(settings)).get("theme").equals("dark"),"Chosen appearance was not persisted");
    }
    static void collectViewports(Component component,Map<JViewport,Boolean> values) {
        if(component instanceof JViewport viewport)values.put(viewport,true);
        if(component instanceof Container parent)for(Component child:parent.getComponents())collectViewports(child,values);
    }
    public static void main(String[] args) throws Exception {
        Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);
        String mode=args.length>1?args[1]:"desktop";Throwable failure=null;
        try {pure(own);if(!mode.equals("headless"))desktop(own,mode.equals("visible"));}catch(Throwable error){failure=error;}
        Map<String,Object> report=new LinkedHashMap<>();report.put("status",failure==null?"PASS":"FAIL");report.put("version",Main.VERSION);
        report.put("checks",checks);report.put("cases",cases);report.put("mode",mode);report.put("images",images);report.put("network_calls",networkCalls.get());
        report.put("user_profile_or_logs_read",false);report.put("user_media_or_clipboard_read",false);report.put("clipboard_actions_executed",0);
        Path jar=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(Files.isRegularFile(jar))report.put("jar_sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        if(failure!=null)report.put("failure",failure.toString());
        Files.writeString(own.resolve("MODERN-DESKTOP-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);
        System.out.println(Json.stringify(report));if(failure!=null)failure.printStackTrace();System.exit(failure==null?0:1);
    }
}
