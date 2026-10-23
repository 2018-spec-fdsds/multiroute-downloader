package multiroute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** macOS graphical authorization for the fixed root-owned BPF component only. */
public final class MacGatewayLauncher {
    private static final String AUTHORIZE = """
            on run argv
                if (count of argv) is not 1 then error "Invalid gateway authorization request"
                with timeout of 2147483647 seconds
                    do shell script (item 1 of argv) with administrator privileges
                end timeout
            end run
            """;
    private static final List<String> OPTIONS = List.of("--virtual-ip", "--gateways", "--ready-file",
            "--parent-pid", "--drop-uid", "--drop-gid", "--stop-file");

    private MacGatewayLauncher() { }

    /**
     * The returned process remains alive for the helper lifetime. GatewayNetwork owns its
     * output drain, startup deadline and unique stop marker. No password is requested by Java.
     */
    public static Process startHelper(List<String> arguments, Path base) throws IOException, InterruptedException {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac"))
            throw new IOException("Graphical gateway authorization is available only on macOS");
        Path helper = expectedHelper();
        Map<String,String> options = validateArguments(arguments);
        int uid = identity("-u"), gid = identity("-g");
        if (uid < 1 || gid < 1 || !Integer.toString(uid).equals(options.get("--drop-uid")) ||
                !Integer.toString(gid).equals(options.get("--drop-gid")) ||
                !Long.toString(ProcessHandle.current().pid()).equals(options.get("--parent-pid")))
            throw new IOException("The gateway component must belong to the current non-root application user");
        verifyInstalledBinary(helper);
        verifyInstalledBinary(helper.resolveSibling("gateway-launch"));
        Path ready = Path.of(options.get("--ready-file"));
        Path network = ready.getParent();
        if (!Files.isDirectory(network, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(network) ||
                ((Number) Files.getAttribute(network, "unix:uid", LinkOption.NOFOLLOW_LINKS)).longValue() != uid)
            throw new IOException("The gateway state directory must belong to the current user");
        var permissions = Files.getPosixFilePermissions(network, LinkOption.NOFOLLOW_LINKS);
        if (permissions.contains(PosixFilePermission.GROUP_WRITE) || permissions.contains(PosixFilePermission.OTHERS_WRITE))
            throw new IOException("Other users must not be able to modify the gateway state directory");
        if (Files.exists(ready, LinkOption.NOFOLLOW_LINKS) ||
                Files.exists(Path.of(options.get("--stop-file")), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("The new gateway request must use unused state files");
        if (base == null || !Files.isDirectory(base)) throw new IOException("Application directory is unavailable");
        ProcessBuilder builder = new ProcessBuilder(authorizationArguments(arguments));
        builder.directory(base.toFile()).redirectErrorStream(true);
        builder.environment().keySet().removeIf(key -> key.equals("ENV") || key.equals("BASH_ENV") ||
                key.startsWith("DYLD_") || key.equals("LD_PRELOAD") || key.equals("LD_LIBRARY_PATH"));
        return builder.start();
    }

    static Path expectedHelper() {
        return Path.of("/Library/PrivilegedHelperTools/MultiRouteResume", Main.VERSION, "gateway-net");
    }

    /** Kept separate to verify quoting and option restrictions without macOS authorization. */
    static List<String> authorizationArguments(List<String> arguments) throws IOException {
        validateArguments(arguments);
        StringBuilder command = new StringBuilder("exec ").append(shellQuote(expectedHelper().resolveSibling("gateway-launch").toString()));
        for (String argument : arguments) command.append(' ').append(shellQuote(argument));
        return List.of("/usr/bin/osascript", "-e", AUTHORIZE, command.toString());
    }

    private static Map<String,String> validateArguments(List<String> arguments) throws IOException {
        boolean managed = arguments != null && arguments.size() == 2 + OPTIONS.size() * 2 &&
                "--managed-dns".equals(arguments.get(arguments.size() - 1));
        if (managed) arguments = arguments.subList(0, arguments.size() - 1);
        if (arguments == null || arguments.size() != 1 + OPTIONS.size() * 2 ||
                !expectedHelper().toString().equals(arguments.get(0)))
            throw new IOException("Only the installed MultiRoute gateway component can be authorized");
        Map<String,String> values = new LinkedHashMap<>();
        if (managed) values.put("--managed-dns", "true");
        for (int i = 1; i < arguments.size(); i += 2) {
            String option = arguments.get(i), value = arguments.get(i + 1);
            if (!OPTIONS.contains(option) || values.putIfAbsent(option, value) != null ||
                    value == null || value.isEmpty() || value.chars().anyMatch(ch -> ch < 32 || ch == 127))
                throw new IOException("Invalid gateway authorization arguments");
        }
        if (!values.keySet().containsAll(OPTIONS) || !ipv4(values.get("--virtual-ip")))
            throw new IOException("Invalid application virtual IPv4 address");
        String[] gateways = values.get("--gateways").split(",", -1);
        if (gateways.length < 1 || gateways.length > GatewayNetwork.MAX_GATEWAYS)
            throw new IOException("Invalid gateway list");
        var unique = new java.util.HashSet<String>();
        for (String gateway : gateways) if (!ipv4(gateway) || !unique.add(gateway) || gateway.equals(values.get("--virtual-ip")))
            throw new IOException("Invalid gateway list");
        for (String field : List.of("--parent-pid", "--drop-uid", "--drop-gid")) {
            String number = values.get(field);
            if (!number.matches("[1-9][0-9]{0,9}")) throw new IOException("Invalid gateway owner or parent");
            try { if (Long.parseLong(number) > Integer.MAX_VALUE) throw new IOException("Invalid gateway owner or parent"); }
            catch (NumberFormatException invalid) { throw new IOException("Invalid gateway owner or parent", invalid); }
        }
        try {
            Path ready = Path.of(values.get("--ready-file"));
            Path stop = Path.of(values.get("--stop-file"));
            if (!ready.isAbsolute() || !ready.equals(ready.normalize()) || ready.getParent() == null ||
                    !ready.getFileName().toString().matches("ready-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json") ||
                    !stop.equals(ready.resolveSibling(ready.getFileName() + ".stop")))
                throw new IOException("Invalid gateway state paths");
        } catch (java.nio.file.InvalidPathException invalid) { throw new IOException("Invalid gateway state paths", invalid); }
        return values;
    }

    private static boolean ipv4(String value) {
        if (value == null) return false;
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}")) return false;
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void verifyInstalledBinary(Path binary) throws IOException {
        verifyProtected(binary, false);
        Path marker = binary.resolveSibling(binary.getFileName() + ".sha256");
        verifyProtected(marker, false);
        if (Files.size(marker) > 128) throw new IOException("Invalid installed gateway checksum");
        String expected = Files.readString(marker, StandardCharsets.US_ASCII).trim();
        if (!expected.matches("[a-f0-9]{64}") || !expected.equals(sha256(binary)))
            throw new IOException("The installed gateway component does not match its protected checksum; reinstall the application");
        if (!Files.isExecutable(binary)) throw new IOException("The installed gateway component is not executable");
    }

    private static int identity(String option) throws IOException, InterruptedException {
        Process child = new ProcessBuilder("/usr/bin/id", option).start();
        try {
            if (!child.waitFor(2, TimeUnit.SECONDS)) throw new IOException("Cannot determine the current macOS user");
            String value = new String(child.getInputStream().readNBytes(32), StandardCharsets.US_ASCII).trim();
            if (child.exitValue() != 0 || !value.matches("[1-9][0-9]{0,9}"))
                throw new IOException("The desktop application must run as a normal macOS user");
            try { return Integer.parseInt(value); }
            catch (NumberFormatException invalid) { throw new IOException("Invalid macOS user identity", invalid); }
        } finally { if (child.isAlive()) child.destroyForcibly(); }
    }

    private static void verifyProtected(Path file, boolean directory) throws IOException {
        Path current = file;
        while (current != null && current.startsWith(Path.of("/Library"))) {
            boolean correctType = current.equals(file) && !directory
                    ? Files.isRegularFile(current, LinkOption.NOFOLLOW_LINKS)
                    : Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS);
            if (!correctType || Files.isSymbolicLink(current) ||
                    ((Number) Files.getAttribute(current, "unix:uid", LinkOption.NOFOLLOW_LINKS)).longValue() != 0)
                throw new IOException("The installed gateway component and its directories must belong to root");
            var permissions = Files.getPosixFilePermissions(current, LinkOption.NOFOLLOW_LINKS);
            if (permissions.contains(PosixFilePermission.GROUP_WRITE) || permissions.contains(PosixFilePermission.OTHERS_WRITE))
                throw new IOException("Other users must not be able to modify the installed gateway component");
            current = current.getParent();
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536];
                for (int count; (count = stream.read(buffer)) != -1;) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
