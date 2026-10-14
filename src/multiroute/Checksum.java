package multiroute;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discover a whole-file SHA-256 from RFC 9530 or a publisher sidecar and its HTTPS mirrors. */
public final class Checksum {
    private static final int MAX_SMALL = 1024 * 1024;
    private static final Pattern REPR_SHA = Pattern.compile("(?i)^sha-256=:([A-Za-z0-9+/=]+):(?:;.*)?$");
    private static final Pattern SUM_LINE = Pattern.compile("^([0-9a-fA-F]{64}) ([ *])(.*)$");
    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{64}");
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "checksum-idle-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private Checksum() { }

    public record Discovery(String sha256, String source) { }

    /** A published checksum is inconsistent or a published Repr-Digest is malformed. */
    public static final class ConflictException extends IOException {
        public ConflictException(String message) { super(message); }
    }

    public static Discovery discover(Map<String, Object> route, String url,
            HttpResponse<InputStream> probe) throws IOException, InterruptedException {
        return discover(route, url, probe, NO_OBSERVER);
    }

    interface TransferObserver {
        void opened(InputStream stream) throws IOException;
        void received(int bytes);
        void closed(InputStream stream);
    }
    private static final TransferObserver NO_OBSERVER = new TransferObserver() {
        public void opened(InputStream stream) { }
        public void received(int bytes) { }
        public void closed(InputStream stream) { }
    };
    /** A job can cancel a checksum body and include its positive-byte progress in its metadata watchdog. */
    static Discovery discover(Map<String, Object> route, String url,
            HttpResponse<InputStream> probe, TransferObserver observer) throws IOException, InterruptedException {
        if (probe == null) throw new IOException("Missing Range probe");
        if (probe.statusCode() != 206 && probe.statusCode() != 200)
            throw new IOException("Expected successful file response for checksum discovery");
        Discovery header = fromHeaders(probe);
        if (!header.sha256().isEmpty()) return header;
        URI validated = SourceTransfer.validate(url);
        boolean fileProtocol = java.util.Set.of("ftp", "ftps", "sftp").contains(validated.getScheme().toLowerCase(Locale.ROOT));
        URI source = fileProtocol ? validated : Transport.uri(SourceTransfer.httpUrl(url));
        String rawPath = source.getRawPath();
        if (rawPath == null || rawPath.isEmpty() || rawPath.endsWith("/")) return new Discovery("", "");
        int slash = rawPath.lastIndexOf('/');
        String rawName = rawPath.substring(slash + 1);
        String filename = URI.create("http://example.invalid/" + rawName).getPath().substring(1);
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\")) return new Discovery("", "");
        String base = source.getScheme() + "://" + source.getRawAuthority() + rawPath.substring(0, slash + 1);
        Discovery result = new Discovery("", "");
        result = combine(result, sidecar(route, base + rawName + ".sha256", url, filename, true, observer), "file.sha256");
        result = combine(result, sidecar(route, base + "SHA256SUMS", url, filename, false, observer), "SHA256SUMS");
        return result;
    }

    /** Representation digest may validate a partial response; a payload digest cannot. */
    public static Discovery fromHeaders(HttpResponse<InputStream> probe) throws ConflictException {
        String repr = reprDigest(probe.headers().allValues("Repr-Digest"));
        return new Discovery(repr, repr.isEmpty() ? "" : "Repr-Digest");
    }

    private static String reprDigest(List<String> fields) throws ConflictException {
        String found = "";
        for (String field : fields) {
            for (String item : splitDictionary(field)) {
                String member = item.trim();
                if (!member.regionMatches(true, 0, "sha-256", 0, 7)) continue;
                Matcher match = REPR_SHA.matcher(member);
                if (!match.matches()) throw new ConflictException("Malformed published Repr-Digest sha-256");
                byte[] bytes;
                try { bytes = Base64.getDecoder().decode(match.group(1)); }
                catch (IllegalArgumentException error) {
                    throw new ConflictException("Malformed base64 in published Repr-Digest sha-256");
                }
                if (bytes.length != 32 || !Base64.getEncoder().encodeToString(bytes).equals(match.group(1)))
                    throw new ConflictException("Published Repr-Digest sha-256 is not canonical 32-byte base64");
                String sha = HexFormat.of().formatHex(bytes);
                if (!found.isEmpty() && !found.equals(sha))
                    throw new ConflictException("Conflicting published Repr-Digest sha-256 values");
                found = sha;
            }
        }
        return found;
    }

    private static List<String> splitDictionary(String field) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false, escaped = false;
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (escaped) { current.append(c); escaped = false; continue; }
            if (quoted && c == '\\') { current.append(c); escaped = true; continue; }
            if (c == '"') quoted = !quoted;
            if (c == ',' && !quoted) { items.add(current.toString()); current.setLength(0); }
            else current.append(c);
        }
        items.add(current.toString());
        return items;
    }

    private static String sidecar(Map<String, Object> route, String sidecarUrl,
            String sourceUrl, String filename, boolean allowBare, TransferObserver observer) throws IOException, InterruptedException {
        boolean fileProtocol = java.util.Set.of("ftp", "ftps", "sftp").contains(SourceTransfer.validate(sidecarUrl).getScheme().toLowerCase(Locale.ROOT));
        HttpResponse<InputStream> response;
        try { response = fileProtocol ? SourceTransfer.small(route, sidecarUrl) : Transport.small(route, sidecarUrl, sourceUrl); }
        catch (SourceTransfer.MissingSourceException absent) { return ""; }
        try (InputStream stream = response.body()) {
            observer.opened(stream);
            if (response.statusCode() == 429 || response.statusCode() >= 500)
                throw new IOException("Источник контрольной суммы временно недоступен (HTTP " + response.statusCode() + ")");
            if (response.statusCode() != 200 && !(fileProtocol && response.statusCode() == 206)) return "";
            String encoding = response.headers().firstValue("Content-Encoding").orElse("identity");
            if (!encoding.equalsIgnoreCase("identity")) throw new IOException("Unexpected sidecar Content-Encoding");
            String length = response.headers().firstValue("Content-Length").orElse(null);
            if (length != null) {
                try { if (Long.parseLong(length) > MAX_SMALL) throw new IOException("Checksum sidecar exceeds 1 MiB"); }
                catch (NumberFormatException error) { throw new IOException("Invalid checksum sidecar length", error); }
            }
            byte[] bytes = readBounded(stream, observer);
            if (bytes.length > MAX_SMALL) throw new IOException("Checksum sidecar exceeds 1 MiB");
            String text;
            try {
                text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException error) { throw new IOException("Invalid UTF-8 checksum sidecar", error); }
            String found = "";
            for (String line : text.split("\\R")) {
                if (allowBare && HEX.matcher(line.trim()).matches()) {
                    found = merge(found, line.trim().toLowerCase(Locale.ROOT));
                    continue;
                }
                boolean escaped = line.startsWith("\\");
                String candidate = escaped ? line.substring(1) : line;
                Matcher match = SUM_LINE.matcher(candidate);
                if (!match.matches()) continue;
                String name = escaped ? unescapeFilename(match.group(3)) : match.group(3);
                if (name == null) continue;
                if (name.equals(filename) || name.equals("./" + filename))
                    found = merge(found, match.group(1).toLowerCase(Locale.ROOT));
            }
            return found;
        } finally { observer.closed(response.body()); }
    }

    private static byte[] readBounded(InputStream stream, TransferObserver observer) throws IOException {
        Thread owner = Thread.currentThread();
        AtomicLong lastByte = new AtomicLong(System.nanoTime());
        ScheduledFuture<?> task = WATCHDOG.scheduleAtFixedRate(() -> {
            if (System.nanoTime() - lastByte.get() > TimeUnit.SECONDS.toNanos(60)) {
                owner.interrupt();
                try { stream.close(); } catch (IOException ignored) { }
            }
        }, 5, 5, TimeUnit.SECONDS);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            while (output.size() <= MAX_SMALL) {
                int got = stream.read(buffer, 0, Math.min(buffer.length, MAX_SMALL + 1 - output.size()));
                if (got < 0) break;
                if (got == 0) continue;
                output.write(buffer, 0, got);
                lastByte.set(System.nanoTime());
                observer.received(got);
            }
            return output.toByteArray();
        } finally {
            task.cancel(false);
            Thread.interrupted();
        }
    }

    private static String unescapeFilename(String escaped) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c != '\\') { result.append(c); continue; }
            if (++i >= escaped.length()) return null;
            switch (escaped.charAt(i)) {
                case '\\' -> result.append('\\');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                default -> { return null; }
            }
        }
        return result.toString();
    }

    private static String merge(String previous, String next) throws ConflictException {
        if (!previous.isEmpty() && !previous.equals(next))
            throw new ConflictException("Conflicting published SHA-256 values");
        return next;
    }

    private static Discovery combine(Discovery previous, String next, String source) throws ConflictException {
        if (next.isEmpty()) return previous;
        String sha = merge(previous.sha256(), next);
        return new Discovery(sha, previous.source().isEmpty() ? source : previous.source() + ", " + source);
    }
}
