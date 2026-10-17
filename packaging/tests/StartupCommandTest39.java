package multiroute;

import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Own Java helpers and own temporary directory only; no application profile, sockets or user processes. */
public final class StartupCommandTest39 {
    static int checks;
    static final List<String> cases = new ArrayList<>();
    static final Map<Long,Instant> owned = new LinkedHashMap<>();
    static final Set<Long> initialThreads = new HashSet<>();
    static boolean cleanupOk = true;
    static int remainingCollectors;
    static Path own;
    interface Work { void run() throws Exception; }
    static void check(boolean condition, String why) { checks++; if (!condition) throw new AssertionError(why); }
    static Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = SingleInstance.class.getDeclaredMethod(name, types); method.setAccessible(true);
        try { return method.invoke(null, args); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception value) throw value;
            if (error.getCause() instanceof Error value) throw value;
            throw error;
        }
    }
    static Object command(int seconds, String... argv) throws Exception {
        return invoke("runCommand", new Class<?>[]{int.class, String[].class}, seconds, argv);
    }
    static Object capture(int seconds, int limit, String... argv) throws Exception {
        return invoke("captureCommand", new Class<?>[]{int.class, int.class, String[].class}, seconds, limit, argv);
    }
    static Object value(Object record, String name) throws Exception {
        Method method = record.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(record);
    }
    static String java() {
        return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString();
    }
    static String[] helper(Path marker, String mode, String... extra) {
        List<String> argv = new ArrayList<>(List.of(java(), "-Duser.home=" + own.resolve("home"),
                "-Djava.io.tmpdir=" + own.resolve("helper-temp"), "-cp", System.getProperty("java.class.path"),
                StartupCommandTest39.class.getName(), "--owned-helper", marker.toString(), mode));
        argv.addAll(List.of(extra)); return argv.toArray(String[]::new);
    }
    static void mark(Path marker) throws Exception {
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, ProcessHandle.current().pid() + "\n" + ProcessHandle.current().info().startInstant().orElseThrow() + "\n");
    }
    static ProcessHandle register(Path marker, long timeoutMs) throws Exception {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < until) {
            try {
                List<String> rows = Files.readAllLines(marker);
                if (rows.size() >= 2) {
                    long pid = Long.parseLong(rows.get(0)); Instant started = Instant.parse(rows.get(1));
                    owned.put(pid, started);
                    return ProcessHandle.of(pid).filter(handle -> handle.info().startInstant().orElse(Instant.EPOCH).equals(started)).orElse(null);
                }
            } catch (IOException | RuntimeException notReady) { }
            Thread.sleep(10);
        }
        throw new AssertionError("Owned helper did not write its PID marker: " + marker.getFileName());
    }
    static boolean alive(long pid, Instant started) {
        return ProcessHandle.of(pid).filter(handle -> handle.isAlive()
                && handle.info().startInstant().orElse(Instant.EPOCH).equals(started)).isPresent();
    }
    static void gone(Path marker) throws Exception {
        register(marker, 2000);
        List<String> rows = Files.readAllLines(marker); long pid = Long.parseLong(rows.get(0)); Instant started = Instant.parse(rows.get(1));
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (alive(pid, started) && System.nanoTime() < until) Thread.sleep(15);
        check(!alive(pid, started), "Own command helper is still running after timeout/interruption/return");
    }
    static void expectIo(Work work, String why) throws Exception {
        try { work.run(); throw new AssertionError(why); }
        catch (IOException expected) { check(true, why); }
    }
    static byte[] binary() {
        byte[] bytes = new byte[256 * 1024];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i & 255);
        return bytes;
    }
    static void worker(String[] args) throws Exception {
        mark(Path.of(args[1])); String mode = args[2];
        switch (mode) {
            case "text" -> {
                System.out.write("OWN-OUT\n".getBytes(StandardCharsets.US_ASCII)); System.out.flush();
                System.err.write("OWN-ERR\n".getBytes(StandardCharsets.US_ASCII)); System.err.flush();
                System.exit(7);
            }
            case "binary" -> { System.out.write(binary()); System.out.flush(); }
            case "delayed-tail" -> {
                System.out.write("HEAD\n".getBytes(StandardCharsets.US_ASCII)); System.out.flush();
                Thread.sleep(25); System.out.write(binary()); System.out.flush();
            }
            case "flood" -> {
                int remaining = Integer.parseInt(args[3]); byte[] chunk = new byte[8192]; Arrays.fill(chunk, (byte) 'Z');
                while (remaining > 0) { int count = Math.min(remaining, chunk.length); System.out.write(chunk, 0, count); remaining -= count; }
                System.out.flush();
            }
            case "sleep" -> { System.out.write("OWN-SLEEP\n".getBytes(StandardCharsets.US_ASCII)); System.out.flush(); Thread.sleep(60_000); }
            default -> throw new AssertionError("Unknown owned helper mode");
        }
    }
    static void probe(String[] args) throws Exception {
        Path directory = Path.of(args[1]); own = directory; Files.createDirectories(directory.resolve("helper-temp"));
        mark(directory.resolve("probe.pid")); Map<String,Object> result = new LinkedHashMap<>();
        try {
            Object response = command(5, helper(directory.resolve("probe-helper.pid"), "text"));
            result.put("command_ok", response != null && ((Integer) value(response, "exitCode")) == 7
                    && value(response, "output").equals("OWN-OUT\nOWN-ERR\n"));
        } catch (IOException expected) { result.put("command_ok", false); result.put("exception", expected.getClass().getName()); }
        Files.writeString(directory.resolve("PROBE.json"), Json.stringify(result));
    }
    static void invalidTemp(boolean baseline) throws Exception {
        Path directory = own.resolve("invalid-temp-probe"); Files.createDirectories(directory.resolve("helper-temp"));
        Path invalid = directory.resolve("ordinary-file-not-a-directory"); Files.writeString(invalid, "owned fixture\n");
        Process child = new ProcessBuilder(java(), "-Duser.home=" + directory.resolve("home"), "-Djava.io.tmpdir=" + invalid,
                "-cp", System.getProperty("java.class.path"), StartupCommandTest39.class.getName(), "--owned-probe", directory.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("probe.log").toFile()).start();
        owned.put(child.pid(), child.info().startInstant().orElseThrow());
        try { check(child.waitFor(12, TimeUnit.SECONDS) && child.exitValue() == 0, "Owned invalid-temp subprocess failed"); }
        finally { if (child.isAlive()) { child.destroyForcibly(); child.waitFor(3, TimeUnit.SECONDS); } }
        Map<String,Object> result = Main.obj(Json.parse(Files.readString(directory.resolve("PROBE.json"))));
        check(Boolean.TRUE.equals(result.get("command_ok")) != baseline,
                baseline ? "Published 3.8 no longer reproduces the temporary-file dependency" : "Pipe command still requires a usable java.io.tmpdir");
        register(directory.resolve("probe.pid"), 1000);
        if (Files.exists(directory.resolve("probe-helper.pid"))) gone(directory.resolve("probe-helper.pid"));
        cases.add(baseline ? "published_3_8_owned_invalid_temp_dependency_reproduced" : "commands_do_not_require_temporary_output_files");
    }
    static void baselineWindowsLock() throws Exception {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) return;
        AtomicReference<AutoCloseable> held = new AtomicReference<>(); AtomicReference<Throwable> watcherError = new AtomicReference<>();
        CountDownLatch opened = new CountDownLatch(1); AtomicBoolean done = new AtomicBoolean();
        Thread observer = new Thread(() -> {
            try {
                long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (!done.get() && System.nanoTime() < until) {
                    try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
                        Optional<Path> file = files.filter(path -> path.getFileName().toString().startsWith("multiroute-port-")).findFirst();
                        if (file.isPresent()) {
                            Class<?> type = Class.forName("com.sun.nio.file.ExtendedOpenOption");
                            @SuppressWarnings({"unchecked", "rawtypes"}) OpenOption noDelete = (OpenOption) Enum.valueOf((Class) type, "NOSHARE_DELETE");
                            held.set(FileChannel.open(file.get(), Set.of(StandardOpenOption.READ, noDelete))); opened.countDown(); return;
                        }
                    }
                    Thread.sleep(5);
                }
            } catch (Throwable error) { watcherError.set(error); }
        }, "owned-startup39-temp-observer");
        observer.setDaemon(true); observer.start();
        Path marker = own.resolve("baseline-sleep.pid"); Throwable failure = null;
        try { command(1, helper(marker, "sleep")); }
        catch (Throwable error) { failure = error; }
        finally {
            done.set(true); observer.join(2000);
            if (held.get() != null) held.get().close();
        }
        check(watcherError.get() == null && opened.getCount() == 0, "Cannot hold own baseline output file against deletion: " + watcherError.get());
        check(failure instanceof FileSystemException, "Published Windows 3.8 did not reproduce its output-file sharing exception: " + failure);
        gone(marker);
        cases.add("published_windows_3_8_timeout_delete_sharing_violation_reproduced_with_owned_file_handle");
    }
    static void fixedCommands() throws Exception {
        Path marker = own.resolve("text.pid"); Object response = command(5, helper(marker, "text"));
        check(response != null && ((Integer) value(response, "exitCode")) == 7, "Nonzero helper exit was not returned intact");
        check(value(response, "output").equals("OWN-OUT\nOWN-ERR\n"), "stdout/stderr merged capture differs"); gone(marker);
        cases.add("stdout_stderr_and_nonzero_exit_are_returned_intact");

        marker = own.resolve("binary.pid"); response = command(5, helper(marker, "binary"));
        check(Arrays.equals(binary(), ((String) value(response, "output")).getBytes(StandardCharsets.ISO_8859_1)), "Output larger than pipe buffer loses raw/OEM bytes"); gone(marker);
        Path captureMarker = own.resolve("capture-binary.pid"); Object raw = capture(5, 300_000, helper(captureMarker, "binary"));
        check(raw != null && ((Integer) value(raw, "exitCode")) == 0 && Arrays.equals(binary(), (byte[]) value(raw, "output")), "Shared raw capture path alters bytes"); gone(captureMarker);
        cases.add("large_pipe_and_all_256_byte_values_roundtrip_without_charset_loss");
        byte[] tailExpected = new byte[binary().length + 5];
        System.arraycopy("HEAD\n".getBytes(StandardCharsets.US_ASCII), 0, tailExpected, 0, 5);
        System.arraycopy(binary(), 0, tailExpected, 5, binary().length);
        for (int i = 0; i < 3; i++) {
            Path delayed = own.resolve("delayed-tail-" + i + ".pid");
            Object tail = capture(5, 300_000, helper(delayed, "delayed-tail"));
            check(tail != null && Arrays.equals(tailExpected, (byte[]) value(tail, "output")), "Last output before process exit was lost"); gone(delayed);
        }
        cases.add("three_delayed_final_pipe_bursts_are_fully_drained_before_return");

        Path flood = own.resolve("overlimit.pid");
        expectIo(() -> command(30, helper(flood, "flood", "4200000")), "runCommand did not reject output over four million bytes"); gone(flood);
        Path smallFlood = own.resolve("capture-overlimit.pid");
        expectIo(() -> capture(5, 1024, helper(smallFlood, "flood", "65536")), "Raw capture path accepted output above its explicit limit"); gone(smallFlood);
        cases.add("output_limits_are_enforced_without_pipe_deadlock_or_leaked_helpers");

        Path timeout = own.resolve("timeout.pid"); long started = System.nanoTime();
        check(command(1, helper(timeout, "sleep")) == null, "Timed-out command does not return the fallback signal");
        check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 6000, "Timed-out command exceeded its bounded cleanup period"); gone(timeout);
        cases.add("timeout_returns_fallback_after_terminating_only_its_owned_helper");

        Path interrupted = own.resolve("interrupted.pid"); AtomicReference<Throwable> error = new AtomicReference<>(); AtomicBoolean interruptFlag = new AtomicBoolean();
        Thread thread = new Thread(() -> {
            try { command(30, helper(interrupted, "sleep")); }
            catch (Throwable failure) { error.set(failure); interruptFlag.set(Thread.currentThread().isInterrupted()); }
        }, "owned-startup39-interrupt");
        thread.start(); register(interrupted, 5000); thread.interrupt(); thread.join(6000);
        check(!thread.isAlive(), "Interrupted command waiter did not return");
        check(error.get() instanceof IOException && interruptFlag.get(), "Interrupted command loses IOException or the restored interrupt flag"); gone(interrupted);
        cases.add("interruption_restores_flag_and_reaps_the_owned_helper");

        check(command(1, own.resolve("does-not-exist-owned-command").toString()) == null, "Missing command does not return the fallback signal");
        Path after = own.resolve("after-errors.pid"); response = command(5, helper(after, "text"));
        check(response != null && ((Integer) value(response, "exitCode")) == 7 && value(response, "output").equals("OWN-OUT\nOWN-ERR\n"), "Successful command is broken after previous failures"); gone(after);
        cases.add("missing_command_and_successful_execution_after_all_failure_paths");
        try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            check(files.noneMatch(path -> path.getFileName().toString().startsWith("multiroute-port-") || path.getFileName().toString().startsWith("multiroute-jcmd-")),
                    "Pipe implementation still creates captured-output temporary files");
        }
    }
    static void cleanup() throws Exception {
        try (var files = Files.walk(own)) {
            for (Path marker : files.filter(path -> path.getFileName().toString().endsWith(".pid")).toList()) {
                try { register(marker, 100); } catch (Exception | AssertionError ignored) { }
            }
        }
        for (Map.Entry<Long,Instant> item : owned.entrySet()) {
            Optional<ProcessHandle> process = ProcessHandle.of(item.getKey()).filter(handle -> handle.isAlive()
                    && handle.info().startInstant().orElse(Instant.EPOCH).equals(item.getValue()));
            if (process.isPresent()) {
                process.get().destroyForcibly(); long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (process.get().isAlive() && System.nanoTime() < until) Thread.sleep(15);
            }
            if (alive(item.getKey(), item.getValue())) cleanupOk = false;
        }
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            remainingCollectors = (int) Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.isAlive() && !initialThreads.contains(thread.getId())
                    && thread.getName().toLowerCase(Locale.ROOT).matches(".*command.*(output|capture|reader).*|.*(output|capture|reader).*command.*")).count();
            if (remainingCollectors > 0) Thread.sleep(20);
        } while (remainingCollectors > 0 && System.nanoTime() < until);
        check(cleanupOk, "Owned helper cleanup left a process running");
        check(remainingCollectors == 0, "Command output collector remains alive after cleanup grace period");
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--owned-helper")) { worker(args); return; }
        if (args.length > 0 && args[0].equals("--owned-probe")) { probe(args); return; }
        own = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(own.resolve("helper-temp"));
        String mode = args.length > 1 ? args[1] : "headless"; boolean baseline = mode.equals("baseline");
        Thread.getAllStackTraces().keySet().forEach(thread -> initialThreads.add(thread.getId()));
        Throwable failure = null;
        try { invalidTemp(baseline); if (baseline) baselineWindowsLock(); else fixedCommands(); }
        catch (Throwable error) { failure = error; }
        finally { try { cleanup(); } catch (Throwable cleanup) { if (failure == null) failure = cleanup; else failure.addSuppressed(cleanup); } }
        Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("status", failure == null ? "PASS" : "FAIL"); report.put("checks", checks); report.put("cases", cases);
        report.put("version", Main.VERSION); report.put("jar_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        report.put("mode", mode); report.put("headless_only", true); report.put("baseline_reproduction", baseline);
        report.put("created_helper_pids", new ArrayList<>(owned.keySet())); report.put("helper_cleanup", cleanupOk); report.put("collector_threads_alive", remainingCollectors);
        report.put("user_profile_or_logs_read", false); report.put("user_media_or_clipboard_read", false); report.put("network_calls", 0);
        if (failure != null) report.put("failure", failure.toString());
        Files.writeString(own.resolve("STARTUP-COMMAND-TESTS.json"), Json.stringify(report));
        System.out.println(Json.stringify(report)); if (failure != null) failure.printStackTrace();
        System.exit(failure == null ? 0 : 1);
    }
}
