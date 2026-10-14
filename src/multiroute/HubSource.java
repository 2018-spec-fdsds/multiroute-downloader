package multiroute;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Public Hugging Face metadata and commit-pinned HTTP downloads, including Xet's HTTP bridge. */
public final class HubSource {
    private static final String ORIGIN = "https://huggingface.co";
    private static final int MAX_BODY = 1024 * 1024;
    private static final int MAX_PAGES = 100;
    private static final int MAX_FILES = 10000;
    private static final long MAX_TIME_NANOS = TimeUnit.SECONDS.toNanos(90);
    private static final Pattern SHA = Pattern.compile("[a-fA-F0-9]{64}");
    private static final Pattern COMMIT = Pattern.compile("[a-fA-F0-9]{40}");
    private static final Pattern REL = Pattern.compile("(?i)(?:^|;)\\s*rel\\s*=\\s*(?:\"([^\"]+)\"|([^;\\s]+))");
    private static final ScheduledExecutorService BODY_TIMEOUT = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "hugging-face-metadata-timeout");
        thread.setDaemon(true);
        return thread;
    });

    private HubSource() { }

    public static boolean isHubUrl(String value) {
        try { return "huggingface.co".equalsIgnoreCase(URI.create(value.trim()).getHost()); }
        catch (RuntimeException error) { return false; }
    }

    public static boolean isListingUrl(String value) {
        try { return !parse(value).file(); }
        catch (IOException | RuntimeException error) { return false; }
    }

    /** Convert a public file-page URL for a bounded speed test without fetching repository metadata. */
    public static String fileUrl(String value) throws IOException {
        Source source = parse(value);
        if (!source.file()) throw new IOException("Ссылка Hugging Face ведёт на список файлов. Выберите отдельный файл.");
        return ORIGIN + source.prefix() + encodePath(source.repo()) + "/resolve/" + encode(source.revision()) +
                "/" + encodePath(source.path());
    }

    /** Preserve the selected folder/file while pinning a choice made in the file picker. */
    public static String atRevision(String value, String revision) throws IOException {
        if (revision == null || !COMMIT.matcher(revision).matches())
            throw new IOException("Некорректная версия репозитория Hugging Face: нужен полный идентификатор коммита.");
        Source source = parse(value);
        return ORIGIN + source.prefix() + encodePath(source.repo()) + "/" + source.mode() + "/" +
                revision.toLowerCase(Locale.ROOT) + (source.path().isEmpty() ? "" : "/" + encodePath(source.path()));
    }

    public static Map<String,Object> inspect(String value, Map<String,Object> route) throws Exception {
        if (route == null) throw new IOException("Выберите шлюз для чтения источника.");
        return inspect(value, url -> fetch(route, url));
    }

    /* Package-only fixture seam. Production metadata has one fixed TLS origin and the chosen transport. */
    interface Fetcher { Page get(String url) throws Exception; }
    record Page(int status, String contentType, String body, List<String> links) { }
    record Source(String repoType, String repo, String revision, String mode, String path, boolean file) {
        String apiType() { return repoType + "s"; }
        String prefix() { return "model".equals(repoType) ? "/" : "/" + apiType() + "/"; }
    }

    static Map<String,Object> inspect(String value, Fetcher fetcher) throws Exception {
        Source requested = parse(value);
        long started = System.nanoTime();
        String infoUrl = ORIGIN + "/api/" + requested.apiType() + "/" + encodePath(requested.repo()) +
                "/revision/" + encode(requested.revision());
        Object infoValue = json(fetcher.get(infoUrl));
        if (!(infoValue instanceof Map<?,?> info)) throw invalidMetadata();
        String commit = text(info.get("sha"));
        if (!COMMIT.matcher(commit).matches()) throw new IOException("Hugging Face не сообщил точную версию файлов.");
        commit = commit.toLowerCase(Locale.ROOT);
        if (COMMIT.matcher(requested.revision()).matches() && !commit.equalsIgnoreCase(requested.revision()))
            throw new IOException("Версия файлов Hugging Face отличается от выбранного коммита.");
        // A successful origin-scoped authenticated API response is also valid for private/gated repositories.
        // Every subsequent tree/resolve request remains on the selected gateway and rechecks origin scope.
        String canonicalRepo = text(info.get("id"));
        if (canonicalRepo.isEmpty()) canonicalRepo = requested.repo();
        validateRepo(canonicalRepo, requested.repoType());
        Source source = new Source(requested.repoType(), canonicalRepo, commit, requested.mode(), requested.path(), requested.file());
        String folder = source.file() ? parent(source.path()) : source.path();
        String treePath = "/api/" + source.apiType() + "/" + encodePath(source.repo()) + "/tree/" + commit +
                (folder.isEmpty() ? "" : "/" + encodePath(folder));
        boolean recursive = !source.file();
        String next = ORIGIN + treePath + "?recursive=" + recursive + "&expand=false&limit=100";
        Set<String> visited = new HashSet<>();
        Set<String> paths = new HashSet<>();
        List<Map<String,Object>> files = new ArrayList<>();
        int entries = 0;
        for (int pageNumber = 0; next != null; pageNumber++) {
            if (pageNumber >= MAX_PAGES || System.nanoTime() - started > MAX_TIME_NANOS)
                throw new IOException("Репозиторий слишком большой или ответ слишком медленный. Откройте ссылку на отдельную папку.");
            if (!visited.add(next)) throw new IOException("Hugging Face повторил страницу списка файлов; неполный список отклонён.");
            Page page = fetcher.get(next);
            Object parsed = json(page);
            if (!(parsed instanceof List<?> list)) throw invalidMetadata();
            for (Object item : list) {
                if (++entries > MAX_FILES) throw new IOException("В папке больше 10 000 записей. Выберите отдельную подпапку.");
                if (!(item instanceof Map<?,?> entry)) throw invalidMetadata();
                String path = text(entry.get("path"));
                validatePath(path, false);
                if (!folder.isEmpty() && !path.startsWith(folder + "/"))
                    throw new IOException("Hugging Face вернул файл вне выбранной папки.");
                if (!recursive && !parent(path).equals(folder)) throw invalidMetadata();
                if (!paths.add(path)) throw new IOException("Hugging Face повторил файл между страницами; список отклонён.");
                String type = text(entry.get("type"));
                if ("directory".equals(type)) continue;
                if (!"file".equals(type)) throw invalidMetadata();
                if (source.file() && !source.path().equals(path)) continue;
                files.add(file(source, entry, path));
            }
            next = nextPage(page.links(), treePath, recursive);
        }
        if (System.nanoTime() - started > MAX_TIME_NANOS)
            throw new IOException("Истекло время получения полного списка Hugging Face. Повторите попытку.");
        if (source.file() && files.size() != 1)
            throw new IOException("Файл не найден в выбранной версии репозитория Hugging Face.");
        files.sort(Comparator.comparing(item -> (String)item.get("path")));
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("kind", source.file() ? "file" : "repository");
        result.put("source_url", value.trim());
        result.put("repo_id", source.repo());
        result.put("repo_type", source.repoType());
        result.put("revision", commit);
        result.put("files", files);
        return result;
    }

    private static Map<String,Object> file(Source source, Map<?,?> entry, String path) throws IOException {
        long size = whole(entry.get("size"));
        String sha = "";
        Object lfsValue = entry.get("lfs");
        if (lfsValue != null) {
            if (!(lfsValue instanceof Map<?,?> lfs)) throw invalidMetadata();
            String oid = text(lfs.get("oid"));
            if (oid.isEmpty()) oid = text(lfs.get("sha256"));
            if (oid.startsWith("sha256:")) oid = oid.substring(7);
            if (!SHA.matcher(oid).matches()) throw new IOException("Hugging Face вернул некорректную SHA-256 файла.");
            if (lfs.containsKey("size") && whole(lfs.get("size")) != size)
                throw new IOException("Размер файла отличается от метаданных LFS Hugging Face.");
            sha = oid.toLowerCase(Locale.ROOT);
        }
        String xet = text(entry.get("xetHash"));
        if (!xet.isEmpty() && !SHA.matcher(xet).matches()) throw invalidMetadata();
        Map<String,Object> file = new LinkedHashMap<>();
        file.put("path", path);
        file.put("filename", path.substring(path.lastIndexOf('/') + 1));
        file.put("url", ORIGIN + source.prefix() + encodePath(source.repo()) + "/resolve/" + source.revision() + "/" + encodePath(path));
        file.put("size", size);
        // Xet hashes identify its storage representation; they are not SHA-256 of the assembled file.
        file.put("sha256", sha);
        file.put("checksum_source", sha.isEmpty() ? "" : "huggingface-lfs-sha256");
        file.put("storage", !xet.isEmpty() ? "xet" : lfsValue != null ? "lfs" : "git");
        return file;
    }

    private static Page fetch(Map<String,Object> route, String url) throws Exception {
        HttpResponse<InputStream> response = Transport.small(route, url, ORIGIN);
        try (InputStream body = response.body()) {
            String length = response.headers().firstValue("Content-Length").orElse("");
            if (!length.isEmpty()) {
                try { if (Long.parseLong(length) > MAX_BODY) throw new IOException("Ответ Hugging Face превышает 1 МиБ. Выберите отдельную папку."); }
                catch (NumberFormatException error) { throw invalidMetadata(); }
            }
            var timeout = BODY_TIMEOUT.schedule(() -> { try { body.close(); } catch (IOException ignored) { } }, 15, TimeUnit.SECONDS);
            byte[] bytes;
            try { bytes = body.readNBytes(MAX_BODY + 1); }
            finally { timeout.cancel(false); }
            if (bytes.length > MAX_BODY) throw new IOException("Ответ Hugging Face превышает 1 МиБ; неполный список отклонён.");
            String content;
            try { content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
            catch (CharacterCodingException error) { throw invalidMetadata(); }
            return new Page(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
                    content, response.headers().allValues("Link"));
        }
    }

    private static Object json(Page page) throws IOException {
        switch (page.status()) {
            case 401, 403 -> throw new IOException("Hugging Face requires authorized source credentials or access approval from the repository owner");
            case 404 -> throw new IOException("Репозиторий, версия или файл Hugging Face не найден либо закрыт владельцем.");
            case 429 -> throw new IOException("Hugging Face временно ограничил число запросов. Повторите попытку позднее.");
            case 200 -> { }
            default -> throw new IOException("Не удалось прочитать Hugging Face: HTTP " + page.status() + ".");
        }
        String type = page.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!type.equals("application/json") && !type.endsWith("+json"))
            throw new IOException("Hugging Face вернул страницу сайта вместо списка файлов.");
        if (page.body().getBytes(StandardCharsets.UTF_8).length > MAX_BODY) throw invalidMetadata();
        try { return Json.parse(page.body()); }
        catch (RuntimeException error) { throw new IOException("Некорректный JSON списка Hugging Face.", error); }
    }

    static Source parse(String value) throws IOException {
        URI uri;
        try { uri = URI.create(value.trim()); }
        catch (RuntimeException error) { throw new IOException("Некорректная ссылка Hugging Face.", error); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"huggingface.co".equalsIgnoreCase(uri.getHost()) ||
                uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IOException("Используйте HTTPS-ссылку на huggingface.co.");
        String raw = uri.getRawPath();
        if (raw == null || !raw.startsWith("/")) throw new IOException("Укажите ссылку на репозиторий или файл Hugging Face.");
        while (raw.endsWith("/")) raw = raw.substring(0, raw.length() - 1);
        if (raw.isEmpty()) throw new IOException("Укажите ссылку на репозиторий Hugging Face.");
        String[] parts = raw.substring(1).split("/", -1);
        int index = 0;
        String repoType = "model";
        if ("datasets".equals(parts[0]) || "spaces".equals(parts[0])) {
            repoType = "datasets".equals(parts[0]) ? "dataset" : "space";
            index++;
        }
        if (index >= parts.length) throw new IOException("Укажите имя репозитория Hugging Face.");
        String repo = decode(parts[index++]);
        if (index < parts.length && !List.of("tree", "blob", "resolve").contains(parts[index])) repo += "/" + decode(parts[index++]);
        validateRepo(repo, repoType);
        String mode = "tree", revision = "main", path = "";
        if (index < parts.length) {
            mode = parts[index++];
            if (!List.of("tree", "blob", "resolve").contains(mode) || index >= parts.length)
                throw new IOException("Нужна ссылка на репозиторий, tree, blob или resolve Hugging Face.");
            revision = decode(parts[index++]);
            validatePath(revision, false);
            StringBuilder joined = new StringBuilder();
            while (index < parts.length) {
                String part = decode(parts[index++]);
                // Slashes in the revision must be encoded; path components cannot hide additional separators.
                if (part.isEmpty() || part.indexOf('/') >= 0) throw new IOException("Некорректное кодирование пути файла Hugging Face.");
                if (joined.length() > 0) joined.append('/');
                joined.append(part);
            }
            path = joined.toString();
        }
        boolean file = !"tree".equals(mode);
        validatePath(path, !file);
        return new Source(repoType, repo, revision, mode, path, file);
    }

    private static void validateRepo(String repo, String repoType) throws IOException {
        String[] pieces = repo.split("/", -1);
        if (pieces.length > 2 || pieces.length == 0 || (!"model".equals(repoType) && pieces.length != 2)) throw invalidMetadata();
        for (String piece : pieces)
            if (!piece.matches("[A-Za-z0-9][A-Za-z0-9_.-]*") || piece.equals(".") || piece.equals(".."))
                throw new IOException("Некорректное имя репозитория Hugging Face.");
    }

    private static void validatePath(String path, boolean emptyAllowed) throws IOException {
        if (path.isEmpty() && emptyAllowed) return;
        if (path.isEmpty() || path.length() > 4096 || path.indexOf('\\') >= 0)
            throw new IOException("Некорректный путь Hugging Face.");
        for (String piece : path.split("/", -1)) {
            if (piece.isEmpty() || piece.equals(".") || piece.equals("..")) throw new IOException("Недопустимый путь Hugging Face.");
            for (int i = 0; i < piece.length(); i++)
                if (Character.isISOControl(piece.charAt(i))) throw new IOException("Недопустимый символ пути Hugging Face.");
        }
    }

    private static String nextPage(List<String> headers, String treePath, boolean recursive) throws IOException {
        String next = null;
        int headerLength = 0;
        for (String header : headers) {
            if ((headerLength += header.length()) > 16384) throw invalidMetadata();
            for (String part : splitLinks(header)) {
                part = part.trim();
                int end = part.indexOf('>');
                if (!part.startsWith("<") || end < 1) throw invalidMetadata();
                Matcher relation = REL.matcher(part.substring(end + 1));
                boolean isNext = false;
                while (relation.find()) {
                    String relations = relation.group(1) != null ? relation.group(1) : relation.group(2);
                    if (List.of(relations.split("\\s+")).contains("next")) isNext = true;
                }
                if (!isNext) continue;
                if (next != null) throw new IOException("Hugging Face вернул неоднозначную следующую страницу.");
                URI uri;
                try { uri = URI.create(part.substring(1, end)); }
                catch (RuntimeException error) { throw invalidMetadata(); }
                if (!"https".equalsIgnoreCase(uri.getScheme()) || !"huggingface.co".equalsIgnoreCase(uri.getHost()) ||
                        uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getRawFragment() != null ||
                        !treePath.equals(uri.getRawPath()))
                    throw new IOException("Следующая страница Hugging Face вышла за пределы выбранной версии или папки.");
                Map<String,String> query = new LinkedHashMap<>();
                if (uri.getRawQuery() != null) for (String pair : uri.getRawQuery().split("&")) {
                    String[] kv = pair.split("=", 2);
                    String key = decode(kv[0]), val = kv.length == 2 ? decode(kv[1]) : "";
                    if (query.put(key, val) != null) throw invalidMetadata();
                }
                if (!String.valueOf(recursive).equals(query.get("recursive")) || !"false".equals(query.get("expand")) ||
                        !query.containsKey("cursor") || query.get("cursor").isEmpty())
                    throw new IOException("Hugging Face изменил параметры списка между страницами; неполный список отклонён.");
                next = uri.toString();
            }
        }
        return next;
    }

    private static List<String> splitLinks(String header) throws IOException {
        List<String> parts = new ArrayList<>();
        int start = 0; boolean angle = false, quote = false;
        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);
            if (c == '"' && !angle) quote = !quote;
            if (!quote) {
                if (c == '<') angle = true;
                if (c == '>') angle = false;
                if (c == ',' && !angle) { parts.add(header.substring(start, i)); start = i + 1; }
            }
        }
        if (angle || quote) throw invalidMetadata();
        if (start < header.length()) parts.add(header.substring(start));
        return parts;
    }

    private static long whole(Object value) throws IOException {
        try {
            if (!(value instanceof Number)) throw invalidMetadata();
            long number = new BigDecimal(value.toString()).longValueExact();
            if (number < 0) throw invalidMetadata();
            return number;
        } catch (ArithmeticException | NumberFormatException error) { throw invalidMetadata(); }
    }
    private static String parent(String path) { int slash = path.lastIndexOf('/'); return slash < 0 ? "" : path.substring(0, slash); }
    private static String text(Object value) { return value instanceof String string ? string : ""; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static String encodePath(String value) { return String.join("/", List.of(value.split("/", -1)).stream().map(HubSource::encode).toList()); }
    private static String decode(String value) throws IOException {
        try {
            String decoded = URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (decoded.indexOf('\uFFFD') >= 0) throw invalidMetadata();
            return decoded;
        } catch (IllegalArgumentException error) { throw new IOException("Некорректное кодирование ссылки Hugging Face.", error); }
    }
    private static IOException invalidMetadata() { return new IOException("Hugging Face вернул неполные или некорректные метаданные файлов."); }
}
