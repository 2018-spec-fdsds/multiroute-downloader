package multiroute;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/** Serializes controllers and replaces only a positively identified earlier controller. */
public final class SingleInstance {
    public static final String APP_ID = "multiroute-resume";
    public static final String STOP_HEADER = "X-Instance-Key";
    private static final String VERSION = Main.VERSION;
    private static final long DEADLINE_MS = 20_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SingleInstance() { }

    public static Guard acquire(Path requestedDataDir, String bind, int port) throws IOException {
        return acquire(requestedDataDir, bind, port, true);
    }

    /**
     * Call before constructing Manager. If adoptPreviousData is true, a verified previous
     * controller on the same port contributes its state directory to the new launch.
     * An explicit --data-dir should use false. No unknown process is terminated.
     */
    public static Guard acquire(Path requestedDataDir, String bind, int port, boolean adoptPreviousData) throws IOException {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Неверный порт контроллера");
        if (!List.of("127.0.0.1", "localhost", "::1").contains(bind))
            throw new IllegalArgumentException("Контроллер должен слушать localhost");
        Path requested = requestedDataDir.toAbsolutePath().normalize();
        Path portDir = Path.of(System.getProperty("java.io.tmpdir"), "multiroute-resume").toAbsolutePath().normalize();
        Files.createDirectories(portDir);
        Path portLockPath = portDir.resolve("port-" + port + ".lock");
        Path portInfoPath = portDir.resolve("port-" + port + ".instance");
        Path selected = requested;

        // First look at the listening service. A previous release has no lock or descriptor.
        Peer peer = onPort(bind, port, portInfoPath);
        if (peer != null) {
            if (adoptPreviousData) selected = peer.dataDir;
            stop(peer);
        }

        FileChannel portChannel = FileChannel.open(portLockPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        FileLock portLock = null;
        FileChannel dataChannel = null;
        FileLock dataLock = null;
        try {
            long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DEADLINE_MS);
            while ((portLock = tryLock(portChannel)) == null) {
                Peer holder = descriptorPeer(portInfoPath);
                if (holder != null) {
                    if (adoptPreviousData) selected = holder.dataDir;
                    stop(holder);
                } else if (System.nanoTime() >= until) {
                    throw new IOException("Порт " + port + " занят контроллером без проверяемого описателя; остановка отклонена");
                }
                sleep(200);
            }

            // The port mutex is ours. Check again: a legacy release never owned it.
            peer = onPort(bind, port, portInfoPath);
            if (peer != null) {
                if (adoptPreviousData) selected = peer.dataDir;
                stop(peer);
            }
            Files.createDirectories(selected);
            Path dataLockPath = selected.resolve(".controller.lock");
            Path dataInfoPath = selected.resolve(".controller.instance");
            dataChannel = FileChannel.open(dataLockPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            while ((dataLock = tryLock(dataChannel)) == null) {
                Peer holder = descriptorPeer(dataInfoPath);
                if (holder == null) {
                    if (System.nanoTime() >= until)
                        throw new IOException("Каталог заданий занят контроллером без проверяемого описателя; остановка отклонена");
                } else stop(holder);
                sleep(200);
            }
            // An unrelated service may have acquired the port while the old controller stopped.
            if (listeningPid(port) > 0) {
                throw new IOException("Порт " + port + " занят другим процессом; он не будет остановлен");
            }
            String secret = randomSecret();
            long pid = ProcessHandle.current().pid();
            Instant started = ProcessHandle.current().info().startInstant().orElse(Instant.now());
            Descriptor own = new Descriptor(pid, started, bind, port, selected, secret);
            writeDescriptor(dataInfoPath, own);
            try { writeDescriptor(portInfoPath, own); }
            catch (IOException error) { Files.deleteIfExists(dataInfoPath); throw error; }
            return new Guard(selected, dataInfoPath, portInfoPath, own, dataChannel, dataLock, portChannel, portLock);
        } catch (IOException | RuntimeException error) {
            release(dataLock, dataChannel);
            release(portLock, portChannel);
            throw error;
        }
    }

    public static final class Guard implements AutoCloseable {
        private final Path dataDir, dataInfoPath, portInfoPath;
        private final Descriptor own;
        private final FileChannel dataChannel, portChannel;
        private final FileLock dataLock, portLock;
        private boolean closed;

        private Guard(Path dataDir, Path dataInfoPath, Path portInfoPath, Descriptor own,
                      FileChannel dataChannel, FileLock dataLock, FileChannel portChannel, FileLock portLock) {
            this.dataDir = dataDir; this.dataInfoPath = dataInfoPath; this.portInfoPath = portInfoPath;
            this.own = own; this.dataChannel = dataChannel; this.dataLock = dataLock;
            this.portChannel = portChannel; this.portLock = portLock;
        }
        public Path dataDir() { return dataDir; }
        public String secret() { return own.secret; }
        public Map<String,Object> instanceInfo() {
            Map<String,Object> info = new LinkedHashMap<>();
            info.put("app_id", APP_ID); info.put("pid", own.pid); info.put("version", VERSION);
            info.put("data_dir", own.dataDir.toString()); info.put("started_at", own.started.toString());
            info.put("bind", own.bind); info.put("port", own.port);
            return info;
        }
        @Override public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            try {
                deleteIfOwned(dataInfoPath, own);
                deleteIfOwned(portInfoPath, own);
            } finally {
                release(dataLock, dataChannel);
                release(portLock, portChannel);
            }
        }
    }

    private record Descriptor(long pid, Instant started, String bind, int port, Path dataDir, String secret) { }
    private record Peer(ProcessHandle process, Instant started, Path dataDir, String bind, int port, String secret, boolean legacy) { }

    private static FileLock tryLock(FileChannel channel) throws IOException {
        try { return channel.tryLock(); }
        catch (OverlappingFileLockException error) { return null; }
    }
    private static void release(FileLock lock, FileChannel channel) {
        if (lock != null) try { lock.release(); } catch (IOException ignored) { }
        if (channel != null) try { channel.close(); } catch (IOException ignored) { }
    }
    private static void sleep(long milliseconds) throws IOException {
        try { Thread.sleep(milliseconds); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException("Ожидание остановки прервано", error); }
    }
    private static String randomSecret() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    private static void writeDescriptor(Path path, Descriptor descriptor) throws IOException {
        Properties fields = new Properties();
        fields.setProperty("app_id", APP_ID); fields.setProperty("pid", Long.toString(descriptor.pid));
        fields.setProperty("started_at", descriptor.started.toString()); fields.setProperty("version", VERSION);
        fields.setProperty("bind", descriptor.bind); fields.setProperty("port", Integer.toString(descriptor.port));
        fields.setProperty("data_dir", descriptor.dataDir.toString()); fields.setProperty("secret", descriptor.secret);
        Path temp = Files.createTempFile(path.getParent(), path.getFileName() + ".tmp.", "");
        try {
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                fields.store(out, "MultiRoute controller instance");
            }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException error) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static Descriptor readDescriptor(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try (InputStream in = Files.newInputStream(path)) {
            Properties fields = new Properties(); fields.load(in);
            if (!APP_ID.equals(fields.getProperty("app_id"))) return null;
            String secret = fields.getProperty("secret", "");
            if (!secret.matches("[a-f0-9]{64}")) return null;
            Path dataDir = Path.of(fields.getProperty("data_dir")).toAbsolutePath().normalize();
            if (!dataDir.isAbsolute()) return null;
            return new Descriptor(Long.parseLong(fields.getProperty("pid")), Instant.parse(fields.getProperty("started_at")),
                fields.getProperty("bind"), Integer.parseInt(fields.getProperty("port")), dataDir, secret);
        } catch (Exception error) { return null; }
    }
    private static void deleteIfOwned(Path path, Descriptor own) throws IOException {
        Descriptor current = readDescriptor(path);
        if (current != null && current.pid == own.pid && current.secret.equals(own.secret)) Files.deleteIfExists(path);
    }
    private static Peer descriptorPeer(Path path) {
        Descriptor descriptor = readDescriptor(path);
        if (descriptor == null) return null;
        Optional<ProcessHandle> found = ProcessHandle.of(descriptor.pid);
        if (found.isEmpty() || !found.get().isAlive() || descriptor.pid == ProcessHandle.current().pid()) return null;
        Instant actual = found.get().info().startInstant().orElse(null);
        if (actual == null || !actual.equals(descriptor.started)) return null;
        return new Peer(found.get(), descriptor.started, descriptor.dataDir, descriptor.bind, descriptor.port, descriptor.secret, false);
    }

    private static Peer onPort(String bind, int port, Path portInfoPath) throws IOException {
        long owner = listeningPid(port);
        if (owner <= 0) return null;
        Map<String,Object> info = getInstance(bind, port);
        if (info != null && APP_ID.equals(String.valueOf(info.get("app_id")))) {
            Peer peer = descriptorPeer(portInfoPath);
            if (peer == null) {
                Path described = safePath(info.get("data_dir"));
                if (described != null) peer = descriptorPeer(described.resolve(".controller.instance"));
            }
            if (peer == null || peer.process.pid() != owner || peer.port != port ||
                !String.valueOf(peer.process.pid()).equals(String.valueOf(info.get("pid"))) ||
                !peer.dataDir.toString().equals(String.valueOf(info.get("data_dir"))) ||
                !peer.process.info().startInstant().orElse(Instant.EPOCH).toString().equals(String.valueOf(info.get("started_at"))))
                throw new IOException("Процесс на порту " + port + " не прошёл проверку идентичности; остановка отклонена");
            return peer;
        }
        Map<String,Object> health = getHealth(bind, port);
        if (health != null && Boolean.TRUE.equals(health.get("ok")) &&
            List.of("1.0", "1.1", "1.2").contains(String.valueOf(health.get("version")))) {
            Peer legacy = legacyPeer(owner, port);
            if (legacy == null) throw new IOException("Старый контроллер на порту " + port + " не прошёл проверку PID/JAR; остановка отклонена");
            return legacy;
        }
        throw new IOException("Порт " + port + " занят неизвестным сервисом; он не будет остановлен");
    }
    private static Path safePath(Object value) {
        try { Path path = Path.of(String.valueOf(value)); return path.isAbsolute() ? path.normalize() : null; }
        catch (Exception error) { return null; }
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> getJson(String bind, int port, String path) {
        for (String host : hosts(bind)) try {
            HttpURLConnection connection = (HttpURLConnection) URI.create("http://" + host + ":" + port + path).toURL().openConnection();
            connection.setConnectTimeout(1000); connection.setReadTimeout(1500); connection.setRequestProperty("Host", host + ":" + port);
            connection.setRequestProperty("Accept", "application/json");
            try {
                if (connection.getResponseCode() != 200) continue;
                try (InputStream in = connection.getInputStream()) {
                    byte[] content = in.readNBytes(16_385);
                    if (content.length > 16_384) continue;
                    Object parsed = Json.parse(new String(content, java.nio.charset.StandardCharsets.UTF_8));
                    if (parsed instanceof Map<?,?>) return (Map<String,Object>) parsed;
                }
            } finally { connection.disconnect(); }
        } catch (Exception ignored) { }
        return null;
    }
    private static Map<String,Object> getInstance(String bind, int port) { return getJson(bind, port, "/api/v1/instance"); }
    private static Map<String,Object> getHealth(String bind, int port) { return getJson(bind, port, "/api/v1/health"); }
    private static List<String> hosts(String bind) {
        return bind.equals("::1") ? List.of("[::1]", "127.0.0.1") : List.of("127.0.0.1", "[::1]");
    }
    private static void stop(Peer peer) throws IOException {
        if (!peer.process.isAlive()) return;
        if (peer.process.pid() == ProcessHandle.current().pid()) throw new IOException("Контроллер уже запущен в этом процессе");
        if (!peer.started.equals(peer.process.info().startInstant().orElse(null)))
            throw new IOException("PID прежнего контроллера изменился; остановка отклонена");
        if (peer.legacy) {
            if (listeningPid(peer.port) != peer.process.pid())
                throw new IOException("Прежний контроллер больше не владеет портом; остановка отклонена");
            // 1.0–1.2 lack a stop endpoint. Their completed chunks remain in durable manifests;
            // the next Manager rehashes them before resuming. No other Java process is touched.
            if (!peer.process.destroy()) throw new IOException("Не удалось остановить старый контроллер PID " + peer.process.pid());
        } else {
            boolean accepted = false;
            long startupWait = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (!accepted && peer.process.isAlive() && System.nanoTime() < startupWait) {
                for (String host : hosts(peer.bind)) try {
                    HttpURLConnection connection = (HttpURLConnection) URI.create("http://" + host + ":" + peer.port + "/api/v1/instance/stop").toURL().openConnection();
                    connection.setRequestMethod("POST"); connection.setConnectTimeout(500); connection.setReadTimeout(1000);
                    connection.setRequestProperty(STOP_HEADER, peer.secret);
                    connection.setDoOutput(true);
                    try (OutputStream out = connection.getOutputStream()) { }
                    try { accepted = connection.getResponseCode() == 200 || connection.getResponseCode() == 202; }
                    finally { connection.disconnect(); }
                    if (accepted) break;
                } catch (Exception ignored) { }
                if (!accepted) sleep(200);
            }
            if (!accepted) throw new IOException("Работающий контроллер PID " + peer.process.pid() + " не подтвердил остановку");
        }
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DEADLINE_MS);
        while (peer.process.isAlive() && System.nanoTime() < until) sleep(150);
        if (peer.process.isAlive()) throw new IOException("Контроллер PID " + peer.process.pid() + " не завершился за 20 секунд");
    }

    private static Peer legacyPeer(long pid, int requestedPort) throws IOException {
        Optional<ProcessHandle> found = ProcessHandle.of(pid);
        if (found.isEmpty() || !found.get().isAlive() || pid == ProcessHandle.current().pid()) return null;
        ProcessHandle handle = found.get();
        if (handle.info().startInstant().isEmpty()) return null;
        String command = handle.info().command().orElse("").toLowerCase(Locale.ROOT);
        if (!command.endsWith("java.exe") && !command.endsWith("javaw.exe") && !command.endsWith("java")) return null;
        String[] args = handle.info().arguments().orElse(null);
        LegacyVm vm = null;
        if (args == null) {
            // JBR on Windows can report command/start while omitting arguments.
            // jcmd is then the only trusted source for launcher arguments and cwd.
            vm = legacyVm(handle);
            List<String> parsed = new ArrayList<>();
            parsed.add("-jar"); parsed.add(vm.classpath);
            parsed.addAll(splitArguments(vm.tail));
            args = parsed.toArray(String[]::new);
        }
        int jarIndex = -1;
        for (int i = 0; i + 1 < args.length; i++) if (args[i].equals("-jar")) { jarIndex = i + 1; break; }
        if (jarIndex < 0 || !Brand.applicationJar(Path.of(args[jarIndex]).getFileName().toString())) return null;
        if (Arrays.asList(args).contains("--worker")) return null;
        int actualPort = 8765;
        Path dataDir = null;
        boolean explicitData = false;
        for (int i = jarIndex + 1; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                try { actualPort = Integer.parseInt(args[++i]); } catch (NumberFormatException error) { return null; }
            } else if (args[i].equals("--data-dir") && i + 1 < args.length) {
                dataDir = Path.of(args[++i]); explicitData = true;
            }
        }
        if (actualPort != requestedPort) return null;
        Path jar = Path.of(args[jarIndex]);
        if (!jar.isAbsolute() || !explicitData) {
            if (vm == null) vm = legacyVm(handle);
            Path workingDir = vm.cwd;
            Path classpath = Path.of(vm.classpath);
            classpath = classpath.isAbsolute() ? classpath.normalize() : workingDir.resolve(classpath).normalize();
            Path invoked = jar.isAbsolute() ? jar.normalize() : workingDir.resolve(jar).normalize();
            if (!classpath.equals(invoked)) throw new IOException("JAR аргумент и classpath прежнего контроллера не совпали");
            if (!jar.isAbsolute()) jar = workingDir.resolve(jar).normalize();
            if (!explicitData) dataDir = workingDir.resolve("data");
        }
        if (!Files.isRegularFile(jar) || !Brand.applicationJar(jar.getFileName().toString()))
            throw new IOException("JAR прежнего контроллера не подтверждён: " + jar);
        try (JarFile archive = new JarFile(jar.toFile())) {
            if (archive.getManifest() == null ||
                !"multiroute.Main".equals(archive.getManifest().getMainAttributes().getValue("Main-Class")) ||
                archive.getEntry("multiroute/Main.class") == null)
                throw new IOException("JAR прежнего контроллера не содержит ожидаемый Main-Class");
        }
        if (explicitData && !dataDir.isAbsolute()) {
            if (vm == null) vm = legacyVm(handle);
            dataDir = vm.cwd.resolve(dataDir).normalize();
        }
        return new Peer(handle, handle.info().startInstant().orElseThrow(), dataDir.toAbsolutePath().normalize(), "127.0.0.1", requestedPort, "", true);
    }

    /** Attach only to the already port-verified Java PID; inspect cwd/classpath without reading job data. */
    private record LegacyVm(Path cwd, String classpath, String tail) { }
    private static LegacyVm legacyVm(ProcessHandle handle) throws IOException {
        Path javaBinary = Path.of(handle.info().command().orElseThrow(() -> new IOException("Путь к Java старого контроллера неизвестен")));
        Path jcmd = javaBinary.resolveSibling("jcmd.exe");
        if (!Files.isRegularFile(jcmd)) throw new IOException("Для проверки относительного JAR нужен jcmd.exe рядом с " + javaBinary);
        CapturedCommand command;
        try {
            command = captureCommand(8, 2_000_000, jcmd.toString(), Long.toString(handle.pid()), "VM.system_properties");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Проверка jcmd прервана", error);
        } catch (OutputLimitException error) {
            throw new IOException("jcmd не смог безопасно подтвердить cwd/classpath старого контроллера", error);
        }
        if (command == null) throw new IOException("jcmd не завершил проверку прежнего контроллера за 8 секунд");
        if (command.exitCode != 0) throw new IOException("jcmd не смог безопасно подтвердить cwd/classpath старого контроллера");
        Properties properties = new Properties();
        properties.load(new StringReader(new String(command.output, java.nio.charset.StandardCharsets.UTF_8)));
        String directory = properties.getProperty("user.dir");
        String classpath = properties.getProperty("java.class.path");
        String javaCommand = properties.getProperty("sun.java.command");
        if (directory == null || classpath == null || javaCommand == null)
            throw new IOException("jcmd не вернул cwd/classpath/command старого контроллера");
        Path cwd = Path.of(directory).toAbsolutePath().normalize();
        if (!Files.isDirectory(cwd)) throw new IOException("Рабочий каталог прежнего контроллера недоступен");
        Path cp = Path.of(classpath);
        cp = cp.isAbsolute() ? cp.normalize() : cwd.resolve(cp).normalize();
        if (!Files.isRegularFile(cp) || !Brand.applicationJar(cp.getFileName().toString()))
            throw new IOException("Classpath прежнего контроллера не подтвердил JAR");
        String prefix = javaCommand.startsWith("\"" + classpath + "\"") ? "\"" + classpath + "\"" : classpath;
        if (!javaCommand.startsWith(prefix) ||
            (javaCommand.length() > prefix.length() && !Character.isWhitespace(javaCommand.charAt(prefix.length()))))
            throw new IOException("Команда запуска и classpath прежнего контроллера не совпали");
        return new LegacyVm(cwd, classpath, javaCommand.substring(prefix.length()).trim());
    }
    private static List<String> splitArguments(String value) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"') { quoted = !quoted; continue; }
            if (Character.isWhitespace(ch) && !quoted) {
                if (part.length() > 0) { result.add(part.toString()); part.setLength(0); }
            } else part.append(ch);
        }
        if (quoted) throw new IOException("Незакрытая кавычка в параметрах прежнего контроллера");
        if (part.length() > 0) result.add(part.toString());
        return result;
    }

    /** Windows port owner lookup. Unknown ownership is treated as unsafe, never as authority to kill. */
    private static long listeningPid(int port) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) return listeningPidUnix(port);
        String script = "Get-NetTCPConnection -LocalPort " + port + " -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique";
        CommandResult powershell = runCommand(7, "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script);
        if (powershell != null && powershell.exitCode == 0) {
            String output = powershell.output.trim();
            if (output.isEmpty()) return 0;
            if (output.matches("(?s)\\d+(?:\\s+\\d+)*")) {
                long[] owners = Arrays.stream(output.split("\\s+")).mapToLong(Long::parseLong).distinct().toArray();
                if (owners.length == 1) return owners[0];
                throw new IOException("Несколько процессов слушают порт " + port + "; остановка отклонена");
            }
        }
        CommandResult netstat = runCommand(7, "netstat.exe", "-ano", "-p", "tcp");
        if (netstat == null || netstat.exitCode != 0)
            throw new IOException("Не удалось проверить владельца порта через Get-NetTCPConnection и netstat");
        List<Long> owners = new ArrayList<>();
        for (String line : netstat.output.split("\\R")) {
            String[] cols = line.trim().split("\\s+");
            if (cols.length < 5 || !cols[0].equalsIgnoreCase("TCP") || !cols[3].equalsIgnoreCase("LISTENING")) continue;
            int colon = cols[1].lastIndexOf(':');
            if (colon < 0 || !cols[1].substring(colon + 1).equals(Integer.toString(port)) || !cols[4].matches("\\d+")) continue;
            long pid = Long.parseLong(cols[4]);
            if (!owners.contains(pid)) owners.add(pid);
        }
        if (owners.isEmpty()) return 0;
        if (owners.size() != 1) throw new IOException("Несколько процессов слушают порт " + port + "; остановка отклонена");
        return owners.get(0);
    }
    private record CommandResult(int exitCode, String output) { }
    private static CommandResult runCommand(int seconds, String... argv) throws IOException {
        try {
            CapturedCommand command = captureCommand(seconds, 4_000_000, argv);
            if (command == null) return null;
            // Only ASCII digits and netstat column names are parsed. Console error text
            // may use an OEM code page instead of UTF-8 on Russian Windows hosts.
            return new CommandResult(command.exitCode, new String(command.output, java.nio.charset.StandardCharsets.ISO_8859_1));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Проверка владельца порта прервана", error);
        } catch (OutputLimitException error) {
            throw new IOException("Слишком большой ответ при проверке владельца порта", error);
        }
    }

    private record CapturedCommand(int exitCode, byte[] output) { }
    private static final class OutputLimitException extends IOException { }

    /** Drain trusted diagnostic commands without a shared output file or a blocked reader thread. */
    private static CapturedCommand captureCommand(int seconds, int limit, String... argv)
            throws IOException, InterruptedException {
        if (seconds < 1 || limit < 1) throw new IllegalArgumentException("Invalid command capture limits");
        Process process;
        try { process = new ProcessBuilder(argv).redirectErrorStream(true).start(); }
        catch (IOException unavailable) { return null; }
        try {
            process.getOutputStream().close();
            InputStream pipe = process.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8192));
            byte[] buffer = new byte[8192];
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (true) {
                if (Thread.interrupted()) throw new InterruptedException();
                if (System.nanoTime() >= deadline) return null;
                boolean exited = !process.isAlive();
                int available = pipe.available();
                if (available > 0) {
                    int count = pipe.read(buffer, 0, Math.min(available, buffer.length));
                    if (count > 0) {
                        if (count > limit - output.size()) throw new OutputLimitException();
                        output.write(buffer, 0, count);
                    }
                } else if (exited) {
                    // available()==0 alone is not EOF: only finish after this producer has exited.
                    return new CapturedCommand(process.exitValue(), output.toByteArray());
                } else {
                    process.waitFor(Math.max(1, Math.min(TimeUnit.MILLISECONDS.toNanos(1), deadline - System.nanoTime())), TimeUnit.NANOSECONDS);
                }
            }
        } finally { closeCommand(process); }
    }

    /** Reap only the process we created, even when the calling thread was interrupted. */
    private static void closeCommand(Process process) throws IOException {
        boolean interrupted = Thread.interrupted();
        try {
            if (process.isAlive()) process.destroyForcibly();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (process.isAlive() && System.nanoTime() < deadline) {
                try { process.waitFor(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS); }
                catch (InterruptedException pending) { interrupted = true; }
            }
            if (process.isAlive()) throw new IOException("Не удалось завершить вспомогательную команду проверки");
        } finally {
            try { process.getOutputStream().close(); } catch (IOException ignored) { }
            try { process.getInputStream().close(); } catch (IOException ignored) { }
            try { process.getErrorStream().close(); } catch (IOException ignored) { }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
    private static long listeningPidUnix(int port) {
        // The new-version descriptor and file locks remain cross-platform. Legacy process
        // replacement is Windows-only because no portable socket-owner API exists in JDK.
        return 0;
    }
}

