package multiroute;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** An application-owned Ethernet/TCP stack. Never edits OS routes or addresses. */
public final class GatewayNetwork {
    public static final String DEFAULT_IP = "192.168.1.70";
    public static final List<String> DEFAULT_GATEWAYS = List.of("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4");
    public static final int MAX_GATEWAYS = 32;
    private static String virtualIp = DEFAULT_IP;
    private static List<String> gateways = DEFAULT_GATEWAYS;
    private static Path dataDirectory = Path.of("data").toAbsolutePath();
    private static Process process;
    private static Thread outputPump;
    private static Path readyFile;
    private static Path stopFile;
    private static Map<String,Object> ready;
    private static long controlRevision;

    private GatewayNetwork() { }

    static synchronized void initialize(Path data) throws IOException {
        dataDirectory = data.toAbsolutePath().normalize();
        Path settings = dataDirectory.resolve("desktop-settings.config");
        if (Files.isRegularFile(settings)) {
            Map<String,Object> saved = Main.obj(Json.parse(Files.readString(settings)));
            String ip = Main.str(saved.get("virtual_ip"));
            List<String> values = new ArrayList<>();
            if (saved.get("gateways") instanceof List<?> list)
                for (Object value : list) if (value instanceof String text && !text.isBlank()) values.add(text.trim());
            try { configure(ip.isBlank() ? DEFAULT_IP : ip, values.isEmpty() ? DEFAULT_GATEWAYS : values); }
            catch (IOException invalidSavedSettings) {
                // The editor must still open so the saved fields can be corrected.
                AppLog.error("Проверьте настройки сети приложения: %s", DiagnosticText.arg(invalidSavedSettings.getMessage()));
            }
        }
    }

    public static void validateSettings(String ip, List<String> values) throws IOException {
        if (!validIpv4(ip) || ip.endsWith(".0") || ip.endsWith(".255"))
            throw new IOException("Укажите корректный свободный IPv4 адрес приложения");
        if (values == null || values.isEmpty() || values.size() > MAX_GATEWAYS)
            throw new IOException("Configure between 1 and 32 gateways");
        List<String> normalized = List.copyOf(values);
        for (String gateway : normalized) {
            if (!validIpv4(gateway)) throw new IOException("Некорректный IPv4 шлюза: " + gateway);
            if (gateway.equals(ip)) throw new IOException("Адрес приложения совпадает с адресом шлюза");
        }
        if (normalized.stream().distinct().count() != normalized.size())
            throw new IOException("Адреса шлюзов должны быть разными");
    }

    public static synchronized void configure(String ip, List<String> values) throws IOException {
        validateSettings(ip, values);
        List<String> normalized = List.copyOf(values);
        if (ip.equals(virtualIp) && normalized.equals(gateways)) return;
        if (ip.equals(virtualIp) && process != null && process.isAlive()) {
            try { applyHotProfiles(normalized); }
            catch(IOException failure) {
                boolean interrupted=Thread.interrupted();
                try { applyHotProfiles(gateways); } catch(IOException restore) { failure.addSuppressed(restore); }
                finally { if(interrupted)Thread.currentThread().interrupt(); }
                throw failure;
            }
            gateways = normalized; return;
        }
        close();
        virtualIp = ip;
        gateways = normalized;
    }

    private static void applyHotProfiles(List<String> desired) throws IOException {
        if (readyFile == null || ready == null || !Boolean.TRUE.equals(ready.get("hot_gateway_updates")))
            throw new IOException("Update the application gateway component to add gateways while downloading");
        long revision = ++controlRevision;
        Path command = readyFile.resolveSibling(readyFile.getFileName()+".gateways");
        Path temporary = command.resolveSibling(command.getFileName()+"."+UUID.randomUUID()+".tmp");
        String text = "MR_GATEWAYS_3\n"+revision+"\n"+String.join(",",desired)+"\n";
        try {
            try { Files.createFile(temporary,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))); }
            catch (UnsupportedOperationException nonPosix) { Files.createFile(temporary); }
            Files.writeString(temporary,text,StandardCharsets.US_ASCII,java.nio.file.StandardOpenOption.WRITE);
            try { Files.move(temporary,command,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary,command,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<deadline) {
                if(process==null||!process.isAlive())throw new IOException("The gateway component stopped during the profile update");
                try {
                    Map<String,Object> status=Main.obj(Json.parse(Files.readString(readyFile)));
                    if(Main.num(status.get("control_revision"),-1)==revision) {
                        String error=Main.str(status.get("control_error"));
                        if(!error.isBlank())throw new IOException(error);
                        if(!desired.equals(status.get("configured_gateways")))throw new IOException("Gateway update acknowledgement does not match the requested profiles");
                        ready=status;return;
                    }
                } catch(IllegalArgumentException partial) { }
                try { Thread.sleep(30); } catch(InterruptedException cancelled) {
                    Thread.currentThread().interrupt();throw new IOException("Gateway profile update was interrupted",cancelled);
                }
            }
            throw new IOException("Gateway profile update did not respond within 10 seconds");
        } finally { Files.deleteIfExists(temporary); }
    }

    public static synchronized Map<String,Object> settings() {
        return Map.of("virtual_ip", virtualIp, "gateways", gateways, "max_gateways",MAX_GATEWAYS,
                "running", process != null && process.isAlive(), "os_routes_changed", false);
    }

    /** Address probing and monitoring belong to the private stack, never to OS configuration. */
    public static synchronized Map<String,Object> checkAddress() throws IOException, InterruptedException {
        ensureStarted();
        return Map.of("ok", true, "virtual_ip", virtualIp, "address_available", true,
                "interface", Main.str(ready.get("interface")), "os_routes_changed", false);
    }

    /** Starts once, checks address conflicts, then resolves only the requested gateway. */
    public static synchronized Map<String,Object> route(String gateway) throws IOException, InterruptedException {
        if (!gateways.contains(gateway)) throw new IOException("Шлюз не настроен в приложении: " + gateway);
        ensureStarted();
        if (!(ready.get("gateway_profiles") instanceof List<?> profiles))
            throw new IOException("Сетевой компонент не вернул список шлюзов");
        for (Object value : profiles) {
            Map<String,Object> profile = Main.obj(value);
            if (!gateway.equals(Main.str(profile.get("gateway")))) continue;
            Map<String,Object> result = new LinkedHashMap<>(profile);
            result.keySet().removeIf(GatewayNetwork::hardwareField);
            result.put("type", "gateway");
            result.put("gateway", gateway);
            result.put("virtual_ip", virtualIp);
            result.put("interface", Main.str(ready.get("interface")));
            if (Boolean.TRUE.equals(profile.get("available"))) {
                long port = Main.num(profile.get("port"), 0);
                if (port < 1 || port > 65535) throw new IOException("Некорректный порт сетевого компонента");
                result.put("endpoint", "http://127.0.0.1:" + port);
                result.put("status", "Шлюз доступен");
                result.put("message", "Адрес приложения " + virtualIp + " → шлюз " + gateway + ".");
            } else {
                result.put("status", "Шлюз недоступен");
                result.put("message", "Шлюз " + gateway + " не отвечает в локальной сети");
            }
            return result;
        }
        throw new IOException("Сетевой компонент не настроил шлюз " + gateway);
    }

    static URI endpoint(Map<String,Object> route) throws IOException, InterruptedException {
        String requestedIp = Main.str(route.get("virtual_ip"));
        if (!requestedIp.isBlank() && !requestedIp.equals(virtualIp))
            throw new IOException("Адрес приложения изменён; верните " + requestedIp + " для продолжения загрузки");
        Map<String,Object> actual = route(Main.str(route.get("gateway")));
        if (!Boolean.TRUE.equals(actual.get("available"))) throw new IOException(Main.str(actual.get("message")));
        return URI.create(Main.str(actual.get("endpoint")));
    }

    /** UDP and peer TCP use the same private stack and requested gateway. */
    public static synchronized java.net.InetSocketAddress socksEndpoint(Map<String,Object> requested) throws IOException, InterruptedException {
        String ip = Main.str(requested.get("virtual_ip"));
        if (!ip.isBlank() && !ip.equals(virtualIp))
            throw new IOException("Адрес приложения изменён; приостановите и обновите настройки загрузки");
        if (!"gateway".equals(requested.get("type")))
            throw new IOException("Для torrent и magnet выберите шлюз");
        Map<String,Object> actual = route(Main.str(requested.get("gateway")));
        if (!Boolean.TRUE.equals(actual.get("available"))) throw new IOException(Main.str(actual.get("message")));
        long port = Main.num(actual.get("socks_port"), 0);
        if (port < 1 || port > 65535) throw new IOException("Сетевой компонент не поддерживает BitTorrent; обновите всю папку программы");
        return new java.net.InetSocketAddress("127.0.0.1", (int) port);
    }

    private static void ensureStarted() throws IOException, InterruptedException {
        if (process != null && ready != null) {
            if (readyFile != null && Files.isRegularFile(readyFile)) {
                try {
                    Map<String,Object> latest = Main.obj(Json.parse(Files.readString(readyFile)));
                    if (!Boolean.TRUE.equals(latest.get("ok"))) {
                        String error = humanError(Main.str(latest.get("error")));
                        close();
                        throw new IOException(error);
                    }
                    ready = latest;
                } catch (IllegalArgumentException partialJson) { /* retry next request */ }
            }
            if (process != null && process.isAlive()) return;
        }
        close();
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String platform = os.contains("win") ? "windows-x64" : os.contains("mac") ? "macos" : "linux";
        String executable = os.contains("win") ? "gateway-net.exe" : "gateway-net";
        Path base;
        try {
            Path location = Path.of(GatewayNetwork.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            base = Files.isRegularFile(location) ? location.getParent() : Path.of("").toAbsolutePath();
        } catch (Exception error) { throw new IOException("Не найден каталог программы", error); }
        boolean mac = os.contains("mac");
        boolean linux = platform.equals("linux");
        boolean privilegedPosix = mac || linux;
        Path helper = mac ? Path.of("/Library/PrivilegedHelperTools/MultiRouteResume", Main.VERSION, "gateway-net")
                : linux ? Path.of("/usr/local/libexec/MultiRouteResume", Main.VERSION, "gateway-net")
                : base.resolve("native").resolve(platform).resolve(executable);
        if (!Files.isRegularFile(helper)) throw new IOException(mac
                ? "Сетевой компонент macOS не установлен; выполните Deploy-macOS.command"
                : linux ? "The Linux gateway component is not installed; run Deploy-Linux.sh"
                : "Сетевой компонент для " + platform + " не найден в папке программы");
        if (mac) verifyMacHelper(helper);
        if (linux) verifyPosixHelper(helper, Path.of("/usr"), "Linux", "Deploy-Linux.sh");
        Files.createDirectories(dataDirectory.resolve("network"));
        readyFile = dataDirectory.resolve("network").resolve("ready-" + UUID.randomUUID() + ".json");
        controlRevision = 0;
        stopFile = privilegedPosix ? readyFile.resolveSibling(readyFile.getFileName() + ".stop") : null;
        Path log = dataDirectory.resolve("network").resolve("gateway-network.log");
        int uid = privilegedPosix ? macIdentity("-u") : -1;
        int gid = privilegedPosix ? macIdentity("-g") : -1;
        ProcessBuilder builder = new ProcessBuilder(helperArguments(platform, helper, virtualIp, gateways,
                readyFile, stopFile, ProcessHandle.current().pid(), uid, gid));
        builder.directory(base.toFile()).redirectErrorStream(true);
        process = builder.start();
        outputPump = drainOutput(process);
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        try {
            while (System.nanoTime() < deadline) {
                if (Files.isRegularFile(readyFile)) {
                    try {
                        Map<String,Object> report = Main.obj(Json.parse(Files.readString(readyFile)));
                        if (!Boolean.TRUE.equals(report.get("ok")))
                            throw new IOException(humanError(Main.str(report.get("error"))));
                        if (!virtualIp.equals(Main.str(report.get("virtual_ip"))))
                            throw new IOException("Сетевой компонент использует другой адрес приложения");
                        if (!process.isAlive()) throw new IOException("Сетевой компонент завершился");
                        ready = report;
                        return;
                    } catch (IllegalArgumentException partialJson) { /* retry incomplete atomic startup file */ }
                }
                if (!process.isAlive()) throw new IOException(linux
                        ? "Authorize the gateway component through Start-Linux.sh and check the gateways again"
                        : mac
                        ? "Разрешите запуск сетевого компонента через Start-macOS.command и повторите проверку шлюзов"
                        : "Сетевой компонент завершился; журнал: " + log);
                Thread.sleep(40);
            }
            throw new IOException("Подготовка сети приложения превысила 20 секунд; журнал: " + log);
        } catch (IOException | InterruptedException error) { close(); throw error; }
    }

    public static synchronized void close() {
        if (process != null) {
            boolean gracefulMac = stopFile != null;
            Map<Long,ProcessHandle> ownedChildren = new LinkedHashMap<>();
            if (gracefulMac) process.descendants().forEach(child -> ownedChildren.put(child.pid(), child));
            if (gracefulMac) {
                try {
                    Files.createFile(stopFile, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                            java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                } catch (IOException | UnsupportedOperationException stoppingFailure) {
                    AppLog.error("Не удалось создать сигнал остановки сетевого компонента: %s", DiagnosticText.arg(stoppingFailure.getMessage()));
                }
            } else process.destroy();
            try {
                if (!process.waitFor(gracefulMac ? 6 : 2, java.util.concurrent.TimeUnit.SECONDS)) {
                    if (gracefulMac) {
                        process.descendants().forEach(child -> ownedChildren.put(child.pid(), child));
                        ownedChildren.values().forEach(ProcessHandle::destroy);
                    }
                    process.destroy();
                    if (!process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        if (gracefulMac) ownedChildren.values().forEach(ProcessHandle::destroyForcibly);
                        process.destroyForcibly();
                    }
                }
            } catch (InterruptedException error) {
                if (gracefulMac) {
                    process.descendants().forEach(child -> ownedChildren.put(child.pid(), child));
                    ownedChildren.values().forEach(ProcessHandle::destroyForcibly);
                }
                process.destroyForcibly(); Thread.currentThread().interrupt();
            }
            // A privileged sudo monitor cannot always be killed by this user. Leave the marker
            // for a delayed, permanently unprivileged helper until every observed child has exited.
            if (gracefulMac && (process.isAlive() || ownedChildren.values().stream().anyMatch(ProcessHandle::isAlive)))
                stopFile = null; // the existing marker remains on disk, associated with the old UUID only
            try { process.getInputStream().close(); } catch (IOException ignored) { }
        }
        // The pump never takes this class's monitor. Closing its stream also unblocks a read.
        if (outputPump != null && outputPump != Thread.currentThread()) {
            try { outputPump.join(500); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
        outputPump = null;
        process = null;
        ready = null;
        Transport.resetGatewayClients();
        if (readyFile != null) try { Files.deleteIfExists(readyFile.resolveSibling(readyFile.getFileName()+".gateways"));Files.deleteIfExists(readyFile); } catch (IOException ignored) { }
        readyFile = null;
        if (stopFile != null) try { Files.deleteIfExists(stopFile); } catch (IOException ignored) { }
        stopFile = null;
    }

    /** Direct argv preserves Unicode/space paths; only the fixed root-owned macOS helper is elevated. */
    static List<String> helperArguments(boolean mac, Path helper, String ip, List<String> values,
            Path readyPath, Path stopPath, long parentPid, int uid, int gid) throws IOException {
        return helperArguments(mac ? "macos" : "windows-x64",helper,ip,values,readyPath,stopPath,parentPid,uid,gid);
    }

    static List<String> helperArguments(String platform, Path helper, String ip, List<String> values,
            Path readyPath, Path stopPath, long parentPid, int uid, int gid) throws IOException {
        List<String> command = new ArrayList<>();
        boolean mac = platform.equals("macos"), linux = platform.equals("linux");
        if (mac || linux) {
            Path expected = Path.of(mac ? "/Library/PrivilegedHelperTools/MultiRouteResume" : "/usr/local/libexec/MultiRouteResume", Main.VERSION, "gateway-net");
            if (!helper.equals(expected) || uid < 1 || gid < 1 || parentPid < 1 || stopPath == null ||
                    !readyPath.isAbsolute() || !stopPath.equals(readyPath.resolveSibling(readyPath.getFileName() + ".stop")))
                throw new IOException("Некорректные параметры запуска сетевого компонента macOS");
            command.addAll(List.of("/usr/bin/sudo", "-n", "--"));
        }
        command.addAll(List.of(helper.toString(), "--virtual-ip", ip, "--gateways", String.join(",", values),
                "--ready-file", readyPath.toString(), "--parent-pid", Long.toString(parentPid)));
        if (mac || linux) command.addAll(List.of("--drop-uid", Integer.toString(uid), "--drop-gid", Integer.toString(gid),
                "--stop-file", stopPath.toString()));
        return List.copyOf(command);
    }

    private static int macIdentity(String option) throws IOException, InterruptedException {
        Process identity = new ProcessBuilder("/usr/bin/id", option).start();
        try {
            if (!identity.waitFor(2, java.util.concurrent.TimeUnit.SECONDS))
                throw new IOException("Не удалось определить пользователя macOS");
            String value = new String(identity.getInputStream().readNBytes(32), StandardCharsets.US_ASCII).trim();
            if (identity.exitValue() != 0 || !value.matches("[0-9]{1,10}"))
                throw new IOException("Не удалось определить пользователя macOS");
            try { return Integer.parseInt(value); }
            catch (NumberFormatException invalid) { throw new IOException("Не удалось определить пользователя macOS", invalid); }
        } finally { if (identity.isAlive()) identity.destroyForcibly(); }
    }

    private static void verifyMacHelper(Path helper) throws IOException {
        verifyPosixHelper(helper,Path.of("/Library"),"macOS","Deploy-macOS.command");
    }

    private static void verifyPosixHelper(Path helper, Path anchor, String platform, String launcher) throws IOException {
        Path parent = helper;
        while (parent != null && parent.startsWith(anchor)) {
            Object owner = Files.getAttribute(parent, "unix:uid", java.nio.file.LinkOption.NOFOLLOW_LINKS);
            boolean correctType = parent.equals(helper)
                    ? Files.isRegularFile(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    : Files.isDirectory(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            if (!correctType || Files.isSymbolicLink(parent) || !(owner instanceof Number uid) || uid.longValue() != 0)
                throw new IOException("The "+platform+" gateway component must belong to root; run "+launcher);
            var permissions = Files.getPosixFilePermissions(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            if (permissions.contains(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE) ||
                    permissions.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE))
                throw new IOException("The "+platform+" gateway component can be modified by other users; run "+launcher);
            parent = parent.getParent();
        }
        if (!Files.isExecutable(helper))
            throw new IOException("The "+platform+" gateway component is not executable; run "+launcher);
    }

    static boolean hardwareField(String field) {
        String key = field.toLowerCase(Locale.ROOT);
        return key.equals("mac") || key.endsWith("_mac") || key.contains("mac_address") ||
                key.equals("hardware_address") || key.equals("physical_address");
    }

    /** Owns only the helper's output pipe; always consumes it, even when the log queue is full. */
    static Thread drainOutput(Process owned) {
        Thread thread = new Thread(() -> {
            try (InputStreamReader reader = new InputStreamReader(owned.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024]; StringBuilder line = new StringBuilder();
                int count;
                while ((count = reader.read(buffer)) != -1) for (int i = 0; i < count; i++) {
                    char value = buffer[i];
                    if (value == '\n') { if (!line.isEmpty()) AppLog.nativeLine(line.toString()); line.setLength(0); }
                    else if (value != '\r' && line.length() < 4096) line.append(value);
                }
                if (!line.isEmpty()) AppLog.nativeLine(line.toString());
            } catch (IOException error) {
                if (owned.isAlive()) AppLog.error("Не удалось прочитать вывод сетевого компонента: %s", DiagnosticText.arg(error.getMessage()));
            }
        }, "gateway-log-output");
        thread.setDaemon(true); thread.start(); return thread;
    }

    public static boolean validIpv4(String value) {
        if (value == null) return false;
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts)
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) return false;
        return true;
    }

    private static String humanError(String error) {
        if (error.isBlank()) return "Не удалось подготовить сеть приложения";
        if (error.contains("in use") || error.contains("conflict") || error.contains("already owned"))
            return "Адрес приложения " + virtualIp + " занят или обнаружен конфликт. Выберите свободный адрес.";
        if (error.contains("already assigned"))
            return "Адрес " + virtualIp + " уже назначен системе. Выберите другой адрес приложения.";
        if (error.contains("More than one")) return "Найдено несколько подходящих Ethernet-адаптеров: " + error;
        if (error.contains("No active physical"))
            return "Не найден активный Ethernet-адаптер в одной подсети с адресом приложения и шлюзами.";
        if (error.contains("Npcap")) return "Ошибка доступа к Npcap: " + error;
        return error;
    }
}
