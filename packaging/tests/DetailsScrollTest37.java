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

/** Synthetic task details only. No clipboard, user profile, user log or source connection. */
public final class DetailsScrollTest37 {
    static int checks;
    static final List<String> cases = new ArrayList<>();
    static final List<Integer> timerPositions = new ArrayList<>();
    static final AtomicInteger networkCalls = new AtomicInteger();
    interface Work { void run() throws Exception; }
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    static void edt(Work work) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { work.run(); } catch (Throwable value) { error.set(value); } });
        rethrow(error.get());
    }
    static void rethrow(Throwable error) throws Exception {
        if (error instanceof Exception value) throw value;
        if (error instanceof Error value) throw value;
    }
    static void drain() throws Exception { for (int i = 0; i < 6; i++) edt(() -> { }); }
    static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    static void call(DesktopUI ui, String name) throws Exception {
        Method method = DesktopUI.class.getDeclaredMethod(name); method.setAccessible(true); method.invoke(ui);
    }
    static void update(DesktopUI ui, List<Object> snapshots) throws Exception {
        Method method = DesktopUI.class.getDeclaredMethod("updateJobs", List.class);
        method.setAccessible(true); method.invoke(ui, snapshots);
    }
    static JScrollPane pane(DesktopUI ui) {
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, ui.detailsArea);
    }
    static boolean bottom(JScrollPane pane) {
        JScrollBar bar = pane.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 2;
    }
    static int topLine(DesktopUI ui) throws Exception {
        return ui.detailsArea.getLineOfOffset(ui.detailsArea.viewToModel2D(pane(ui).getViewport().getViewPosition()));
    }
    static void tail(DesktopUI ui) throws Exception {
        edt(() -> ui.detailsArea.setCaretPosition(0)); drain();
        edt(() -> {
            JScrollPane pane = pane(ui);
            pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum());
            check(bottom(pane) && pane.getViewport().getViewPosition().y > 0, "Fixture cannot reach the details tail");
            check(ui.detailsArea.getCaretPosition() == 0, "Scrollbar fixture moved the caret");
        });
    }
    static Map<String,Object> fixture(String id, Path own, boolean changing, int revision) {
        Map<String,Object> job = new LinkedHashMap<>();
        job.put("id", id); job.put("filename", "owned-" + id + ".bin");
        job.put("directory", own.resolve("owned-downloads").toString());
        job.put("url", "https://example.invalid/owned/" + "long-safe-source-path/".repeat(7) + id + ".bin");
        job.put("status", changing ? "running" : "complete"); job.put("kind", "http");
        job.put("size", 4L * 1024 * 1024 * 1024); job.put("downloaded", (changing ? 2L : 4L) * 1024 * 1024 * 1024 + revision);
        job.put("progress_bytes", job.get("downloaded")); job.put("speed_bps", changing ? 20_000_000L + revision : 0L);
        job.put("elapsed_ms", 330_000L + revision); job.put("eta_seconds", 100);
        job.put("resumable", true); job.put("distribution_supported", true);
        Map<String,Object> sources = new LinkedHashMap<>(), warnings = new LinkedHashMap<>();
        for (String algorithm : List.of("md5", "sha256", "sha512")) {
            int length = algorithm.equals("md5") ? 32 : algorithm.equals("sha256") ? 64 : 128;
            job.put(algorithm, "a".repeat(length)); job.put("expected_" + algorithm, "a".repeat(length));
            sources.put(algorithm, "https://example.invalid/owned/checksums/" + "safe-checksum-source/".repeat(8) + algorithm);
            warnings.put(algorithm, "Owned synthetic checksum note " + "safe wrapped report words ".repeat(12));
        }
        job.put("checksum_sources", sources); job.put("checksum_warnings", warnings);
        List<Object> routes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) routes.add(new LinkedHashMap<>(Map.of(
                "id", "owned-route-" + i, "name", "Owned gateway " + i, "gateway", "192.168.1." + i,
                "enabled", true, "speed_bps", changing ? 5_000_000L + revision : 0L,
                "bytes", 1024L * 1024 * (100 + revision), "chunks", 20 + revision,
                "last_error", "Owned prior route diagnostic " + "safe wrapped diagnostic words ".repeat(14))));
        job.put("routes", routes);
        return job;
    }
    static void componentUpdate(JScrollPane pane, String method, String snapshot) throws Exception {
        Method update = pane.getClass().getDeclaredMethod(method, String.class);
        update.setAccessible(true); update.invoke(pane, snapshot);
    }
    static String componentReport(int revision) {
        StringBuilder text = new StringBuilder("Owned report revision " + revision + "\n");
        for (String algorithm : List.of("MD5", "SHA-256", "SHA-512")) {
            text.append(algorithm).append("\n");
            for (int row = 0; row < 30; row++) text.append("Owned checksum report row ").append(row)
                    .append(" safe wrapped diagnostic words für данные test ").append("a".repeat(64)).append('\n');
        }
        for (int i = 1; i <= 4; i++) text.append("Owned gateway 192.168.1.").append(i).append("\n");
        return text.toString();
    }
    static void components() throws Exception {
        AtomicReference<JScrollPane> ref = new AtomicReference<>();
        edt(() -> {
            Constructor<?> constructor = Class.forName("multiroute.LogView").getDeclaredConstructor(boolean.class);
            constructor.setAccessible(true); JScrollPane view = (JScrollPane) constructor.newInstance(false);
            view.setSize(620, 260); layout(view); ref.set(view); componentUpdate(view, "updateSnapshot", componentReport(0));
        }); drain();
        JScrollPane view = ref.get(); JTextArea text = (JTextArea) view.getViewport().getView();
        edt(() -> {
            check(view.getViewport().getViewPosition().y == 0, "Details component should open at the report heading");
            check(view.getVerticalScrollBar().getMaximum() > view.getVerticalScrollBar().getVisibleAmount(), "Component report is not scrollable");
            text.setCaretPosition(0); view.getVerticalScrollBar().setValue(view.getVerticalScrollBar().getMaximum());
            check(bottom(view), "Component fixture cannot scroll to tail");
            componentUpdate(view, "updateSnapshot", componentReport(0));
        }); drain();
        edt(() -> { check(bottom(view), "Identical component snapshot loses tail"); componentUpdate(view, "updateSnapshot", componentReport(1)); }); drain();
        edt(() -> check(bottom(view), "Changed component snapshot loses manually selected tail"));
        AtomicInteger y = new AtomicInteger(), line = new AtomicInteger();
        edt(() -> { text.getCaret().setDot(300); text.getCaret().moveDot(180); }); drain();
        edt(() -> {
            view.getVerticalScrollBar().setValue(view.getVerticalScrollBar().getMaximum() / 3);
            y.set(view.getViewport().getViewPosition().y);
            line.set(text.getLineOfOffset(text.viewToModel2D(view.getViewport().getViewPosition())));
            componentUpdate(view, "updateSnapshot", componentReport(2));
        }); drain();
        edt(() -> {
            check(Math.abs(text.getLineOfOffset(text.viewToModel2D(view.getViewport().getViewPosition())) - line.get()) <= 1,
                    "Component changing report loses middle logical row");
            check(text.getCaret().getDot() == 180 && text.getCaret().getMark() == 300, "Component replacement loses reverse selection");
            componentUpdate(view, "updateSnapshot", componentReport(3));
            view.getVerticalScrollBar().setValue(y.get() + 90); y.set(view.getViewport().getViewPosition().y);
        }); drain();
        edt(() -> {
            check(view.getViewport().getViewPosition().y == y.get(), "Deferred component refresh overrides manual scrolling");
            componentUpdate(view, "resetSnapshot", componentReport(3));
        }); drain();
        edt(() -> {
            check(view.getViewport().getViewPosition().y == 0 && text.getCaretPosition() == 0 && text.getSelectionStart() == text.getSelectionEnd(),
                    "Explicit new-job reset does not clear prior viewport and selection");
            view.getVerticalScrollBar().setValue(view.getVerticalScrollBar().getMaximum());
            componentUpdate(view, "resetSnapshot", "");
        }); drain();
        edt(() -> check(view.getViewport().getViewPosition().y == 0 && text.getDocument().getLength() == 0, "Empty component reset leaves prior details"));
        cases.add("headless_actual_details_component_open_tail_same_id_selection_manual_scroll_and_new_id_reset");
    }
    /** A real Swing timer, rather than a manually invoked listener, exercises repeated updates. */
    static void timerUpdates(DesktopUI ui, List<Object> snapshots, boolean baseline) throws Exception {
        CountDownLatch ticks = new CountDownLatch(3);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<javax.swing.Timer> timer = new AtomicReference<>();
        edt(() -> {
            javax.swing.Timer value = new javax.swing.Timer(900, event -> {
                try {
                    update(ui, snapshots);
                    // Let queued document/caret/layout callbacks run before observing the viewport.
                    SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                        try {
                            timerPositions.add(pane(ui).getViewport().getViewPosition().y);
                            if (!baseline) check(bottom(pane(ui)), "900 ms completed-job refresh loses the report tail");
                        } catch (Throwable failure) { error.compareAndSet(null, failure); }
                        finally { ticks.countDown(); if (ticks.getCount() == 0) timer.get().stop(); }
                    }));
                } catch (Throwable failure) {
                    error.compareAndSet(null, failure); ticks.countDown();
                    if (ticks.getCount() == 0) timer.get().stop();
                }
            });
            timer.set(value); value.start();
        });
        try { check(ticks.await(7, TimeUnit.SECONDS), "Owned 900 ms details timer did not complete three ticks"); }
        finally { edt(() -> timer.get().stop()); }
        drain(); rethrow(error.get());
    }
    static void test(Path own, boolean baseline, boolean visible) throws Exception {
        AppLog.initialize(own.resolve("owned-log"));
        Main.Manager manager = new Main.Manager(own.resolve("owned-profile"), (url, route) -> {
            networkCalls.incrementAndGet(); throw new AssertionError("No source requests in details fixture");
        });
        AtomicReference<DesktopUI> ref = new AtomicReference<>();
        CountDownLatch closed = new CountDownLatch(1);
        try {
            edt(() -> {
                DesktopUI ui = new DesktopUI(manager, closed::countDown, false,
                        (gw,url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No discovery"); },
                        (ip,gws) -> { networkCalls.incrementAndGet(); throw new AssertionError("No configure"); },
                        (route,url) -> { networkCalls.incrementAndGet(); throw new AssertionError("No speed test"); });
                ref.set(ui); ((javax.swing.Timer) field(ui, "refreshTimer")).stop();
                ui.frame.setSize(1280, 860);
                if (visible) ui.frame.setVisible(true); else ui.frame.addNotify();
                ui.frame.validate(); layout(ui.frame.getContentPane());
            }); drain();
            DesktopUI ui = ref.get();
            Map<String,Object> completed = fixture("alpha", own, false, 0);
            edt(() -> update(ui, List.of(completed))); drain();
            edt(() -> {
                check(ui.detailsArea.getText().contains("MD5") && ui.detailsArea.getText().contains("SHA-256")
                        && ui.detailsArea.getText().contains("SHA-512"), "Not testing the actual checksum details report");
                check(ui.detailsArea.getText().contains("192.168.1.4"), "Four-gateway report fixture missing");
                check(ui.frame.isShowing() == visible, "Owned window visibility does not match test mode");
            });
            tail(ui); timerUpdates(ui, List.of(completed), baseline);
            if (baseline) {
                edt(() -> check(!bottom(pane(ui)) && pane(ui).getViewport().getViewPosition().y <= ui.detailsArea.getInsets().top,
                        "Packaged 3.6 does not reproduce completed-details reset"));
                cases.add("packaged_3_6_unchanged_completed_details_reset_reproduced_with_real_900ms_timer");
                return;
            }
            cases.add("unchanged_completed_details_remain_at_tail_across_three_real_900ms_ticks");

            AtomicInteger y = new AtomicInteger(), dot = new AtomicInteger(), mark = new AtomicInteger();
            edt(() -> { ui.detailsArea.getCaret().setDot(300); ui.detailsArea.getCaret().moveDot(180); }); drain();
            edt(() -> {
                JScrollPane pane = pane(ui); pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum() / 3);
                y.set(pane.getViewport().getViewPosition().y); dot.set(ui.detailsArea.getCaret().getDot()); mark.set(ui.detailsArea.getCaret().getMark());
                check(y.get() > 0 && !bottom(pane), "Middle details reading fixture failed");
                update(ui, List.of(completed));
            }); drain();
            edt(() -> {
                check(pane(ui).getViewport().getViewPosition().y == y.get(), "Identical details refresh loses middle position");
                check(ui.detailsArea.getCaret().getDot() == dot.get() && ui.detailsArea.getCaret().getMark() == mark.get(),
                        "Details refresh destroys selected text or selection direction");
            });
            cases.add("completed_middle_reading_and_reverse_selection_survive_refresh");

            AtomicInteger line = new AtomicInteger();
            edt(() -> { line.set(topLine(ui)); update(ui, List.of(fixture("alpha", own, true, 1))); }); drain();
            edt(() -> {
                check(Math.abs(topLine(ui) - line.get()) <= 1, "Changing same-id report loses visible logical line");
                check(pane(ui).getViewport().getViewPosition().y > 0, "Changing same-id report jumps to beginning");
                check(ui.detailsArea.getCaret().getDot() == dot.get() && ui.detailsArea.getCaret().getMark() == mark.get(),
                        "Changing same-id report destroys selection");
            });
            tail(ui);
            edt(() -> update(ui, List.of(fixture("alpha", own, true, 2)))); drain();
            edt(() -> check(bottom(pane(ui)), "Changing same-id report no longer follows manually selected tail"));
            cases.add("changing_same_id_report_preserves_reading_and_follows_selected_tail");

            Map<String,Object> second = fixture("beta", own, false, 0);
            edt(() -> update(ui, List.of(completed, second))); drain();
            edt(() -> ui.jobsTable.setRowSelectionInterval(ui.jobsTable.convertRowIndexToView(1), ui.jobsTable.convertRowIndexToView(1))); drain();
            edt(() -> {
                check(ui.detailsArea.getText().contains("owned-beta.bin"), "Selecting second job did not change details");
                check(pane(ui).getViewport().getViewPosition().y == 0 && ui.detailsArea.getCaretPosition() == 0,
                        "Newly selected job must start at the beginning of its own report");
            });
            tail(ui);
            edt(() -> ui.jobsTable.setRowSelectionInterval(ui.jobsTable.convertRowIndexToView(0), ui.jobsTable.convertRowIndexToView(0))); drain();
            edt(() -> check(pane(ui).getViewport().getViewPosition().y == 0 && ui.detailsArea.getText().contains("owned-alpha.bin"),
                    "Switching back retains another job's scroll position"));
            cases.add("switching_selected_jobs_resets_to_the_new_report_beginning");

            for (String code : List.of("ru", "en", "de", "es", "fr")) {
                edt(() -> {
                    ui.detailsArea.select(0, 0);
                    JScrollPane pane = pane(ui); pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum() / 3);
                    line.set(topLine(ui)); I18n.setLanguage(code); call(ui, "refreshLanguage");
                }); drain();
                edt(() -> {
                    check(Math.abs(topLine(ui) - line.get()) <= 1, "Actual language refresh loses logical details row: " + code);
                    check(pane(ui).getViewport().getViewPosition().y > 0, "Actual language refresh resets details to beginning: " + code);
                });
            }
            cases.add("five_language_refreshLanguage_paths_preserve_report_anchor");
            edt(() -> update(ui, List.of())); drain();
            edt(() -> check(pane(ui).getViewport().getViewPosition().y == 0 && !ui.detailsArea.getText().contains("owned-alpha.bin"),
                    "Empty selection leaves prior task details or scroll position"));
            cases.add("removing_all_jobs_clears_the_selected_report");
            check(networkCalls.get() == 0, "Details fixture attempted network access");
        } finally {
            try {
                if (ref.get() != null) {
                    edt(() -> ref.get().frame.dispose());
                    check(closed.await(6, TimeUnit.SECONDS), "Owned desktop workers did not close");
                }
            } finally { manager.shutdown(); AppLog.close(); }
        }
    }
    public static void main(String[] args) throws Exception {
        Path own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own);
        String mode = args.length > 1 ? args[1] : "desktop";
        Throwable failure = null;
        try {
            if (!mode.equals("baseline")) components();
            if (!mode.equals("headless")) test(own, mode.equals("baseline"), mode.equals("visible"));
        }
        catch (Throwable error) { failure = error; }
        Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("status", failure == null ? "PASS" : "FAIL"); report.put("checks", checks); report.put("cases", cases);
        report.put("version", Main.VERSION); report.put("jar_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        report.put("baseline_reproduction", mode.equals("baseline")); report.put("visible_mode", mode.equals("visible"));
        report.put("headless_only", mode.equals("headless"));
        report.put("real_swing_timer_interval_ms", 900); report.put("timer_view_positions", timerPositions);
        report.put("user_profile_or_logs_read", false); report.put("user_media_or_clipboard_read", false); report.put("network_calls", networkCalls.get());
        if (failure != null) report.put("failure", failure.toString());
        Files.writeString(own.resolve("DETAILS-SCROLL-TESTS.json"), Json.stringify(report));
        System.out.println(Json.stringify(report)); if (failure != null) failure.printStackTrace();
        System.exit(failure == null ? 0 : 1);
    }
}
