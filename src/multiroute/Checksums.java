package multiroute;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Whole-file references and simultaneous local hashes; segment identity remains SHA-256. */
public final class Checksums {
    public static final List<String> ALGORITHMS = List.of("md5", "sha256", "sha512");
    private Checksums() { }

    public static int hexLength(String algorithm) {
        return switch (algorithm) { case "md5" -> 32; case "sha256" -> 64; case "sha512" -> 128;
            default -> throw new IllegalArgumentException("Unsupported checksum algorithm: " + algorithm); };
    }
    public static String label(String algorithm) {
        return switch (algorithm) { case "md5" -> "MD5"; case "sha256" -> "SHA-256"; case "sha512" -> "SHA-512";
            default -> throw new IllegalArgumentException("Unsupported checksum algorithm: " + algorithm); };
    }
    public static String normalize(String algorithm, Object value) {
        if (value != null && !(value instanceof String)) throw new IllegalArgumentException(label(algorithm) + " must be hexadecimal text");
        String text = value == null ? "" : ((String) value).trim().toLowerCase(Locale.ROOT);
        if (!text.isEmpty() && !text.matches("[0-9a-f]{" + hexLength(algorithm) + "}"))
            throw new IllegalArgumentException(label(algorithm) + " must contain " + hexLength(algorithm) + " hexadecimal characters");
        return text;
    }
    public static Map<String,String> expected(Map<?,?> state) {
        Map<String,String> result = new LinkedHashMap<>();
        for (String algorithm : ALGORITHMS) {
            String value = normalize(algorithm, state.get("expected_" + algorithm));
            if (!value.isEmpty()) result.put(algorithm, value);
        }
        return result;
    }
    public static Map<String,String> values(Map<?,?> fields) {
        Map<String,String> result = new LinkedHashMap<>();
        for (String algorithm : ALGORITHMS) {
            String value = normalize(algorithm, fields.get(algorithm));
            if (!value.isEmpty()) result.put(algorithm, value);
        }
        return result;
    }
    public static boolean strong(Map<String,String> values) {
        return !values.getOrDefault("sha256", "").isEmpty() || !values.getOrDefault("sha512", "").isEmpty();
    }
    static Map<String,String> sources(Map<?,?> state) {
        Map<String,String> result = new LinkedHashMap<>();
        if (state.get("checksum_sources") instanceof Map<?,?> saved) for (String algorithm : ALGORITHMS)
            if (saved.get(algorithm) instanceof String value && !value.isBlank()) result.put(algorithm, value);
        if (!result.containsKey("sha256") && state.get("checksum_source") instanceof String value && !value.isBlank()
                && !normalize("sha256", state.get("expected_sha256")).isEmpty()) result.put("sha256", value);
        return result;
    }
    /** Old SHA-256-only manifests remain valid; new fields are ordinary JSON strings/maps. */
    static void normalizeState(Map<String,Object> state) {
        Map<String,String> expected = expected(state);
        for (String algorithm : ALGORITHMS) {
            state.put("expected_" + algorithm, expected.getOrDefault(algorithm, ""));
            state.put(algorithm, normalize(algorithm, state.get(algorithm)));
            if (state.containsKey("source_" + algorithm)) state.put("source_" + algorithm, normalize(algorithm, state.get("source_" + algorithm)));
        }
        state.put("checksum_sources", sources(state));
    }
    static void initialize(Map<String,Object> state, Map<String,String> expected) {
        Map<String,String> sources = new LinkedHashMap<>();
        for (String algorithm : ALGORITHMS) {
            state.put("expected_" + algorithm, expected.getOrDefault(algorithm, "")); state.put(algorithm, "");
            if (expected.containsKey(algorithm)) sources.put(algorithm, "manual");
        }
        state.put("checksum_sources", sources);
        state.put("checksum_source", sources.getOrDefault("sha256", ""));
    }
    static void checkDiscovery(Map<?,?> state, Map<String,String> discovered) throws Checksum.ConflictException {
        Map<String,String> expected = expected(state);
        for (var entry : discovered.entrySet()) {
            String algorithm = entry.getKey(), value = normalize(algorithm, entry.getValue());
            String pinned = expected.getOrDefault(algorithm, normalize(algorithm, state.get("source_" + algorithm)));
            if (!pinned.isEmpty() && !value.equals(pinned))
                throw new Checksum.ConflictException("Published " + label(algorithm) + " differs from the trusted file checksum");
        }
    }
    static void acceptDiscovery(Map<String,Object> state, Map<String,String> discovered, Map<String,String> origin) throws Checksum.ConflictException {
        checkDiscovery(state, discovered);
        Map<String,String> sources = sources(state);
        for (var entry : discovered.entrySet()) {
            String algorithm = entry.getKey(), value = normalize(algorithm, entry.getValue());
            state.put("expected_" + algorithm, value); state.put("source_" + algorithm, value);
            String source = origin.getOrDefault(algorithm, "");
            if (!source.isEmpty()) sources.put(algorithm, source);
        }
        state.put("checksum_sources", sources);
        if (sources.containsKey("sha256")) state.put("checksum_source", sources.get("sha256"));
    }
    public static void verify(Map<String,String> actual, Map<String,String> expected) throws Checksum.ConflictException {
        for (var entry : expected.entrySet()) if (!entry.getValue().equalsIgnoreCase(actual.getOrDefault(entry.getKey(), "")))
            throw new Checksum.ConflictException(label(entry.getKey()) + " does not match the expected checksum; partial data preserved");
    }
    /** Calculate all supported whole-file digests in one read, without following a final symlink. */
    public static Map<String,String> file(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Checksum input is not a regular file");
        Map<String,MessageDigest> hashes = new LinkedHashMap<>();
        for (String algorithm : ALGORITHMS) try { hashes.put(algorithm, MessageDigest.getInstance(label(algorithm))); }
        catch (NoSuchAlgorithmException unavailable) { throw new IOException("Required checksum algorithm unavailable", unavailable); }
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[4 * 1024 * 1024]; int got;
            while ((got = input.read(buffer)) >= 0) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Checksum calculation interrupted");
                if (got > 0) for (MessageDigest hash : hashes.values()) hash.update(buffer, 0, got);
            }
        }
        Map<String,String> result = new LinkedHashMap<>();
        hashes.forEach((algorithm, hash) -> result.put(algorithm, HexFormat.of().formatHex(hash.digest())));
        return result;
    }
}
