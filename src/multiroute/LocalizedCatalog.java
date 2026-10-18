package multiroute;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** UTF-8 language resources shared by interface bindings and canonical diagnostic templates. */
final class LocalizedCatalog {
    static final List<String> EXTRA = List.of("zh", "ko", "ar", "it");
    private static final Map<String,Map<String,String>> CACHE = new ConcurrentHashMap<>();
    private LocalizedCatalog() { }

    static String[] extend(String source, String[] originals) {
        String[] result = java.util.Arrays.copyOf(originals, originals.length + EXTRA.size());
        for (int index = 0; index < EXTRA.size(); index++)
            result[originals.length + index] = catalog(EXTRA.get(index)).getOrDefault(source, originals[1]);
        return result;
    }

    static Map<String,String> catalog(String language) {
        if (!EXTRA.contains(language)) return Map.of();
        return CACHE.computeIfAbsent(language, LocalizedCatalog::load);
    }

    private static Map<String,String> load(String language) {
        try (InputStream input = LocalizedCatalog.class.getResourceAsStream("i18n/" + language + ".json")) {
            // A plain javac-only developer build can omit resources. Release checks require all catalogs.
            if (input == null) return Map.of();
            byte[] bytes = input.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IllegalStateException("Oversized language resource: " + language);
            Object value = Json.parse(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString());
            if (!(value instanceof Map<?,?> rows)) throw new IllegalStateException("Invalid language resource: " + language);
            Map<String,String> result = new LinkedHashMap<>();
            for (var row : rows.entrySet()) {
                if (!(row.getKey() instanceof String key) || !(row.getValue() instanceof String text) || text.isBlank())
                    throw new IllegalStateException("Invalid language entry: " + language);
                result.put(key, text);
            }
            return Map.copyOf(result);
        } catch (Exception unavailable) { throw new IllegalStateException("Unable to read UTF-8 language resource: " + language, unavailable); }
    }
}
