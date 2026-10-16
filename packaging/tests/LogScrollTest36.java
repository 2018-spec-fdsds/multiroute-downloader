package multiroute;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Owned log events and hidden Swing components only; no user log, clipboard or network. */
public final class LogScrollTest36 {
    static int checks;
    static final List<String> cases = new ArrayList<>();
    interface Work { void run() throws Exception; }
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void edt(Work work)throws Exception{
        AtomicReference<Throwable> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{try{work.run();}catch(Throwable value){error.set(value);}});
        if(error.get() instanceof Exception value)throw value;
        if(error.get() instanceof Error value)throw value;
    }
    static void drain()throws Exception{for(int i=0;i<4;i++)edt(()->{});}
    static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container nested)layout(nested);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static void call(Object object,String name)throws Exception{Method m=DesktopUI.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);}
    static JScrollPane scroll(JTextArea text){return (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,text);}
    static boolean bottom(JScrollPane pane){JScrollBar bar=pane.getVerticalScrollBar();return bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-2;}
    static String rows(int start,int count){StringBuilder text=new StringBuilder();for(int i=start;i<start+count;i++)text.append(String.format(Locale.ROOT,"Owned row %04d · synthetic download event · данные für test\n",i));return text.toString();}
    static void update(JScrollPane pane,String value)throws Exception{Method method=pane.getClass().getDeclaredMethod("updateSnapshot",String.class);method.setAccessible(true);method.invoke(pane,value);}
    static void components()throws Exception{
        AtomicReference<JScrollPane> ref=new AtomicReference<>();
        edt(()->{Constructor<?> ctor=Class.forName("multiroute.LogView").getDeclaredConstructor();ctor.setAccessible(true);JScrollPane pane=(JScrollPane)ctor.newInstance();pane.setSize(620,260);layout(pane);ref.set(pane);update(pane,rows(0,300));});drain();
        JScrollPane pane=ref.get();JTextArea text=(JTextArea)pane.getViewport().getView();
        edt(()->check(bottom(pane),"Opening a long log does not show latest entries"));
        edt(()->{text.setCaretPosition(0);});drain();
        edt(()->{pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum());check(text.getCaretPosition()==0,"Fixture scrollbar unexpectedly moved caret");update(pane,rows(0,301));});drain();
        edt(()->{check(bottom(pane),"Tail at caret0 jumps away from the newest entry");check(text.getCaretPosition()==0,"Following log moved the text caret");});
        cases.add("viewport_tail_following_independent_of_caret_and_initial_open");
        AtomicInteger position=new AtomicInteger();
        edt(()->{text.setCaretPosition(text.getDocument().getLength());});drain();
        edt(()->{pane.getVerticalScrollBar().setValue(970);position.set(pane.getViewport().getViewPosition().y);check(position.get()>0&&!bottom(pane),"Middle reading fixture failed");update(pane,rows(0,302));});drain();
        edt(()->{check(pane.getViewport().getViewPosition().y==position.get(),"Middle reading follows caret to another location");check(text.getCaretPosition()==rows(0,301).length(),"Append changes user's caret");});
        cases.add("middle_position_preserved_when_caret_is_at_old_document_end");
        AtomicInteger dot=new AtomicInteger(),mark=new AtomicInteger();
        edt(()->{text.getCaret().setDot(220);text.getCaret().moveDot(150);dot.set(text.getCaret().getDot());mark.set(text.getCaret().getMark());});drain();
        edt(()->{pane.getVerticalScrollBar().setValue(1200);position.set(pane.getViewport().getViewPosition().y);update(pane,rows(0,303));});drain();
        edt(()->{check(text.getCaret().getDot()==dot.get()&&text.getCaret().getMark()==mark.get(),"Append destroys selection or its direction");check(pane.getViewport().getViewPosition().y==position.get(),"Selection causes refresh scroll jump");});
        cases.add("selection_direction_and_reading_position_survive_append");
        edt(()->{update(pane,rows(0,304));pane.getVerticalScrollBar().setValue(1700);position.set(pane.getViewport().getViewPosition().y);});drain();
        edt(()->check(pane.getViewport().getViewPosition().y==position.get(),"Deferred refresh overrides subsequent manual scrolling"));
        edt(()->{pane.getVerticalScrollBar().setValueIsAdjusting(true);update(pane,rows(0,305));pane.getVerticalScrollBar().setValue(1900);position.set(pane.getViewport().getViewPosition().y);});drain();
        edt(()->{check(pane.getViewport().getViewPosition().y==position.get(),"Refresh fights an active scrollbar drag");pane.getVerticalScrollBar().setValueIsAdjusting(false);});
        cases.add("later_manual_scroll_and_active_thumb_drag_override_pending_restoration");
        AtomicInteger topLine=new AtomicInteger();
        edt(()->{text.setCaretPosition(0);});drain();
        edt(()->{pane.getVerticalScrollBar().setValue(2600);int offset=text.viewToModel2D(pane.getViewport().getViewPosition());topLine.set(text.getLineOfOffset(offset));update(pane,rows(5,305));});drain();
        edt(()->{int line=text.getLineOfOffset(text.viewToModel2D(pane.getViewport().getViewPosition()));check(Math.abs(line-(topLine.get()-5))<=1,"Bounded history drop loses surviving visible log row");});
        for(String language:List.of("en","ru","de","es","fr")){
            edt(()->{pane.getVerticalScrollBar().setValue(1900);int offset=text.viewToModel2D(pane.getViewport().getViewPosition());topLine.set(text.getLineOfOffset(offset));update(pane,rows(5,305).replace("synthetic download event",language+" translated synthetic event with more words"));});drain();
            edt(()->{int line=text.getLineOfOffset(text.viewToModel2D(pane.getViewport().getViewPosition()));check(Math.abs(line-topLine.get())<=1,"Language rendering loses visible event row");check(pane.getViewport().getViewPosition().y>0,"Language rendering jumps to beginning");});
        }
        cases.add("bounded_history_rotation_and_five_language_replacements_preserve_event_anchor");
        edt(()->{Point before=pane.getViewport().getViewPosition();update(pane,text.getText());check(before.equals(pane.getViewport().getViewPosition()),"Identical refresh changes viewport");update(pane,"short\n");});drain();
        edt(()->{check(pane.getViewport().getViewPosition().y>=0,"Shorter log creates invalid viewport");update(pane,"");});drain();
        edt(()->check(text.getDocument().getLength()==0&&pane.getViewport().getViewPosition().y==0,"Cleared log retains stale content or scroll"));
        cases.add("unchanged_empty_and_shortened_snapshots_remain_valid");
    }
    static void desktop(Path own,boolean baseline)throws Exception{
        AppLog.initialize(own.resolve("owned-log"));
        for(int i=0;i<350;i++)AppLog.info("Создано загрузок: %d",i);AppLog.flush();
        AtomicInteger network=new AtomicInteger();Main.Manager manager=new Main.Manager(own.resolve("owned-profile"),(url,route)->{network.incrementAndGet();throw new AssertionError("No source requests");});
        AtomicReference<DesktopUI> ref=new AtomicReference<>();CountDownLatch closed=new CountDownLatch(1);
        try{
            edt(()->{DesktopUI ui=new DesktopUI(manager,closed::countDown,false,(gw,url)->{network.incrementAndGet();throw new AssertionError("No discovery");},(ip,gateways)->{network.incrementAndGet();throw new AssertionError("No configure");},(route,url)->{network.incrementAndGet();throw new AssertionError("No speed test");});
                ((javax.swing.Timer)field(ui,"refreshTimer")).stop();ui.frame.addNotify();ui.openLog(false);((javax.swing.Timer)field(ui,"logTimer")).stop();ui.logDialog.setSize(860,500);ui.logDialog.validate();layout(ui.logDialog.getContentPane());ref.set(ui);});drain();
            DesktopUI ui=ref.get();JScrollPane pane=scroll(ui.logArea);
            edt(()->ui.logArea.setCaretPosition(0));drain();
            edt(()->{pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum());check(bottom(pane),"Cannot scroll real log dialog to tail");check(ui.logArea.getCaretPosition()==0,"Real log fixture moved caret");});
            AppLog.info("Owned newest entry %d",351);AppLog.flush();edt(()->call(ui,"refreshLog"));drain();
            if(baseline){edt(()->check(!bottom(pane),"Baseline no longer reproduces original tail jump"));cases.add("original_3_5_tail_jump_reproduced_with_scrollbar_and_caret0");return;}
            edt(()->check(bottom(pane),"Real DesktopUI refresh loses newest log entry"));
            AtomicInteger position=new AtomicInteger();
            edt(()->{pane.getVerticalScrollBar().setValue(1100);position.set(pane.getViewport().getViewPosition().y);});
            AppLog.info("Owned timer entry %d",352);AppLog.flush();
            edt(()->{javax.swing.Timer timer=(javax.swing.Timer)field(ui,"logTimer");for(ActionListener action:timer.getActionListeners())action.actionPerformed(new ActionEvent(timer,0,"own timer"));});drain();
            edt(()->check(pane.getViewport().getViewPosition().y==position.get(),"Actual timer jumps middle position"));
            for(String code:List.of("ru","en","de","es","fr")){edt(()->I18n.setLanguage(code));drain();edt(()->check(pane.getViewport().getViewPosition().y>0,"Language listener jumps real dialog to first line"));}
            edt(()->{call(ui,"refreshLog");ui.logDialog.dispose();ui.openLog(false);ui.logDialog.setSize(860,500);ui.logDialog.validate();layout(ui.logDialog.getContentPane());});drain();
            edt(()->{check(ui.logDialog.isDisplayable(),"Reopened log disappeared");check(field(ui,"logTimer")!=null,"Old closed callback stopped reopened log timer");((javax.swing.Timer)field(ui,"logTimer")).stop();check(!ui.frame.isShowing()&&!ui.logDialog.isShowing(),"Fixture exposed own test windows");});
            cases.add("actual_desktop_manual_timer_language_and_close_reopen_lifecycle");
            check(network.get()==0,"Log viewer made a source request");
        }finally{
            if(ref.get()!=null)edt(()->{ref.get().logDialog.dispose();ref.get().frame.dispose();});
            check(closed.await(6,TimeUnit.SECONDS),"Owned desktop workers did not close");manager.shutdown();AppLog.close();
        }
    }
    public static void main(String[] args)throws Exception{
        Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);boolean baseline=args.length>1&&args[1].equals("baseline"),headless=args.length>1&&args[1].equals("headless");Throwable failure=null;
        try{if(!baseline)components();if(!headless)desktop(own,baseline);}catch(Throwable error){failure=error;}
        Path jar=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Map<String,Object> report=new LinkedHashMap<>();report.put("status",failure==null?"PASS":"FAIL");report.put("checks",checks);report.put("cases",cases);report.put("version",Main.VERSION);report.put("jar_sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));report.put("baseline_reproduction",baseline);report.put("headless_only",headless);report.put("user_logs_read",false);report.put("user_media_or_clipboard_read",false);report.put("network_calls",0);if(failure!=null)report.put("failure",failure.toString());
        Files.writeString(own.resolve("LOG-SCROLL-TESTS.json"),Json.stringify(report));System.out.println(Json.stringify(report));if(failure!=null)failure.printStackTrace();System.exit(failure==null?0:1);
    }
}
