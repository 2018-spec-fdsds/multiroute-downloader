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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discover whole-file references from RFC 9530 or bounded publisher sidecars. */
public final class Checksum {
    private static final int MAX_SMALL = 1024 * 1024;
    private static final Pattern REPR = Pattern.compile("(?i)^(md5|sha-256|sha-512)=:([A-Za-z0-9+/=]+):(.*)$");
    private static final Pattern PARAMETER_VALUE = Pattern.compile("(?:\\?[01]|-?[0-9]{1,15}(?:\\.[0-9]{1,3})?|[A-Za-z*][A-Za-z0-9!#$%&'*+.^_`|~:/-]*)");
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "checksum-idle-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private Checksum() { }

    public record Discovery(String sha256, String source, Map<String,String> checksums, Map<String,String> sources, Map<String,String> warnings) {
        public Discovery {
            checksums = Map.copyOf(checksums); sources = Map.copyOf(sources); warnings = Map.copyOf(warnings);
        }
        public Discovery(String sha256, String source, Map<String,String> checksums, Map<String,String> sources) {
            this(sha256, source, checksums, sources, Map.of());
        }
        public Discovery(String sha256, String source) {
            this(sha256, source, sha256.isEmpty() ? Map.of() : Map.of("sha256", sha256),
                    sha256.isEmpty() || source.isEmpty() ? Map.of() : Map.of("sha256", source));
        }
        public String md5() { return checksums.getOrDefault("md5", ""); }
        public String sha512() { return checksums.getOrDefault("sha512", ""); }
    }
    private static Discovery discovery(Map<String,String> values, Map<String,String> sources) {
        return new Discovery(values.getOrDefault("sha256", ""), sources.getOrDefault("sha256", ""), values, sources);
    }

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
        return discover(route, url, probe, observer, Map.of());
    }
    static Discovery discover(Map<String, Object> route, String url,
            HttpResponse<InputStream> probe, TransferObserver observer, Map<String,String> trusted) throws IOException, InterruptedException {
        if (probe == null) throw new IOException("Missing Range probe");
        if (probe.statusCode() != 206 && probe.statusCode() != 200)
            throw new IOException("Expected successful file response for checksum discovery");
        Discovery header = fromHeaders(probe);
        if (header.checksums().size() == Checksums.ALGORITHMS.size()) return header;
        URI validated = SourceTransfer.validate(url);
        boolean fileProtocol = java.util.Set.of("ftp", "ftps", "sftp").contains(validated.getScheme().toLowerCase(Locale.ROOT));
        URI source = fileProtocol ? validated : Transport.uri(SourceTransfer.httpUrl(url));
        String rawPath = source.getRawPath();
        if (rawPath == null || rawPath.isEmpty() || rawPath.endsWith("/")) return header;
        int slash = rawPath.lastIndexOf('/');
        String rawName = rawPath.substring(slash + 1);
        String filename = URI.create("http://example.invalid/" + rawName).getPath().substring(1);
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\")) return header;
        String base = source.getScheme() + "://" + source.getRawAuthority() + rawPath.substring(0, slash + 1);
        Discovery result = header;
        Map<String,String> warnings = new LinkedHashMap<>();
        for (String algorithm : List.of("sha256", "sha512", "md5")) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Checksum discovery interrupted");
            if (header.checksums().containsKey(algorithm)) continue;
            if (algorithm.equals("sha256") && trusted.containsKey(algorithm)) continue;
            String sums = algorithm.toUpperCase(Locale.ROOT) + "SUMS";
            boolean optional = !algorithm.equals("sha256") || Checksums.strong(trusted) || Checksums.strong(header.checksums());
            AtomicBoolean timedOut = new AtomicBoolean(); AtomicReference<InputStream> active = new AtomicReference<>();
            Thread owner = Thread.currentThread();
            TransferObserver bounded = new TransferObserver() {
                public void opened(InputStream stream) throws IOException { active.set(stream); observer.opened(stream); }
                public void received(int bytes) { observer.received(bytes); }
                public void closed(InputStream stream) { active.compareAndSet(stream, null); observer.closed(stream); }
            };
            ScheduledFuture<?> deadline = optional ? WATCHDOG.schedule(() -> {
                timedOut.set(true); owner.interrupt(); InputStream stream = active.get();
                if(stream != null)try { stream.close(); } catch(IOException ignored) { }
            }, 3, TimeUnit.SECONDS) : null;
            try {
                result = combine(result, algorithm, sidecar(route, base + rawName + "." + algorithm, url, filename, algorithm, true, bounded), "file." + algorithm);
                if(timedOut.get())throw new IOException("Optional checksum lookup timeout");
                result = combine(result, algorithm, sidecar(route, base + sums, url, filename, algorithm, false, bounded), sums);
            } catch(ConflictException conflict) { throw conflict; }
            catch(IOException unavailable) {
                if(!optional)throw unavailable;
                warnings.put(algorithm, "Source lookup unavailable; existing references will still be verified");
            } catch(InterruptedException interrupted) {
                if(!timedOut.get())throw interrupted;
                warnings.put(algorithm, "Source lookup timed out; existing references will still be verified");
            } finally {
                if(deadline != null)deadline.cancel(false);
                if(timedOut.get())Thread.interrupted();
            }
        }
        return new Discovery(result.sha256(), result.source(), result.checksums(), result.sources(), warnings);
    }

    /** Representation digest may validate a partial response; a payload digest cannot. */
    public static Discovery fromHeaders(HttpResponse<InputStream> probe) throws ConflictException {
        Map<String,String> repr = reprDigest(probe.headers().allValues("Repr-Digest"));
        Map<String,String> sources = new LinkedHashMap<>();
        for (String algorithm : repr.keySet()) sources.put(algorithm, "Repr-Digest");
        return discovery(repr, sources);
    }

    private static Map<String,String> reprDigest(List<String> fields) throws ConflictException {
        Map<String,String> found = new LinkedHashMap<>();
        for (String field : fields) {
            for (String item : splitDictionary(field)) {
                String member = item.trim();
                String key = member.split("[=;\\s]", 2)[0].toLowerCase(Locale.ROOT);
                String algorithm = switch (key) { case "sha-256" -> "sha256"; case "sha-512" -> "sha512"; case "md5" -> "md5"; default -> ""; };
                if (algorithm.isEmpty()) continue;
                Matcher match = REPR.matcher(member);
                if (!match.matches() || !validParameters(match.group(3))) throw new ConflictException("Malformed published Repr-Digest " + key);
                byte[] bytes;
                try { bytes = Base64.getDecoder().decode(match.group(2)); }
                catch (IllegalArgumentException error) {
                    throw new ConflictException("Malformed base64 in published Repr-Digest " + key);
                }
                if (bytes.length != Checksums.hexLength(algorithm) / 2 || !Base64.getEncoder().encodeToString(bytes).equals(match.group(2)))
                    throw new ConflictException("Published Repr-Digest " + key + " has an invalid length or noncanonical base64");
                String sha = HexFormat.of().formatHex(bytes);
                found.put(algorithm, merge(found.getOrDefault(algorithm, ""), sha, algorithm));
            }
        }
        return found;
    }

    /** RFC 8941 parameters cannot turn an otherwise malformed field into a trusted digest. */
    private static boolean validParameters(String text) {
        int offset=0;
        while(offset<text.length()) {
            while(offset<text.length()&&text.charAt(offset)==' ')offset++;
            if(offset==text.length())return true;
            if(text.charAt(offset++)!=';'||offset==text.length())return false;
            char first=text.charAt(offset);if(first!='*'&&(first<'a'||first>'z'))return false;
            while(offset<text.length()&&"abcdefghijklmnopqrstuvwxyz0123456789_.*-".indexOf(text.charAt(offset))>=0)offset++;
            if(offset==text.length()||text.charAt(offset)!='=')continue;
            if(++offset==text.length())return false;
            char start=text.charAt(offset++);
            if(start=='"') {
                boolean closed=false;
                while(offset<text.length()) {
                    char c=text.charAt(offset++);if(c=='"'){closed=true;break;}
                    if(c=='\\'){if(offset==text.length())return false;char escaped=text.charAt(offset++);if(escaped!='"'&&escaped!='\\')return false;}
                    else if(c<32||c>126)return false;
                }
                if(!closed)return false;
            } else if(start==':') {
                int end=text.indexOf(':',offset);if(end<0)return false;
                String bytes=text.substring(offset,end);
                try{Base64.getDecoder().decode(bytes);}catch(IllegalArgumentException invalid){return false;}
                offset=end+1;
            } else {
                int begin=offset-1;while(offset<text.length()&&text.charAt(offset)!=' '&&text.charAt(offset)!=';')offset++;
                if(!PARAMETER_VALUE.matcher(text.substring(begin,offset)).matches())return false;
            }
            if(offset<text.length()&&text.charAt(offset)!=' '&&text.charAt(offset)!=';')return false;
        }
        return true;
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
            String sourceUrl, String filename, String algorithm, boolean allowBare, TransferObserver observer) throws IOException, InterruptedException {
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
            if (text.startsWith("\ufeff")) text = text.substring(1);
            String found = "";
            Pattern hex = Pattern.compile("[0-9a-fA-F]{" + Checksums.hexLength(algorithm) + "}");
            Pattern sumLine = Pattern.compile("^([0-9a-fA-F]{" + Checksums.hexLength(algorithm) + "}) ([ *])(.*)$");
            Pattern tagged = Pattern.compile("^" + Checksums.label(algorithm).replace("-", "") + " \\((.*)\\) = ([0-9a-fA-F]{" + Checksums.hexLength(algorithm) + "})$");
            for (String line : text.split("\\R")) {
                if (allowBare && hex.matcher(line.trim()).matches()) {
                    found = merge(found, line.trim().toLowerCase(Locale.ROOT), algorithm);
                    continue;
                }
                boolean escaped = line.startsWith("\\");
                String candidate = escaped ? line.substring(1) : line;
                Matcher tag = tagged.matcher(candidate);
                if (tag.matches()) {
                    String name = escaped ? unescapeFilename(tag.group(1)) : tag.group(1);
                    if (filename.equals(name) || ("./" + filename).equals(name)) found = merge(found, tag.group(2).toLowerCase(Locale.ROOT), algorithm);
                    continue;
                }
                Matcher match = sumLine.matcher(candidate);
                if (!match.matches()) continue;
                String name = escaped ? unescapeFilename(match.group(3)) : match.group(3);
                if (name == null) continue;
                if (name.equals(filename) || name.equals("./" + filename))
                    found = merge(found, match.group(1).toLowerCase(Locale.ROOT), algorithm);
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

    private static String merge(String previous, String next, String algorithm) throws ConflictException {
        if (!previous.isEmpty() && !previous.equals(next))
            throw new ConflictException("Conflicting published " + Checksums.label(algorithm) + " values");
        return next;
    }

    private static Discovery combine(Discovery previous, String algorithm, String next, String source) throws ConflictException {
        if (next.isEmpty()) return previous;
        Map<String,String> values = new LinkedHashMap<>(previous.checksums());
        Map<String,String> sources = new LinkedHashMap<>(previous.sources());
        values.put(algorithm, merge(values.getOrDefault(algorithm, ""), next, algorithm));
        String prior = sources.getOrDefault(algorithm, ""); sources.put(algorithm, prior.isEmpty() ? source : prior + ", " + source);
        return discovery(values, sources);
    }
}
