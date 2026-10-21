package multiroute;

import java.net.URI;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Safe, bounded context for transient failures; it never selects or changes a route. */
final class NetworkDiagnostics {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?|ftps?|sftp|webdavs)://[^\\s<>\\\"']+");
    private static final Pattern AUTH = Pattern.compile("(?i)\\b(?:proxy-authorization|authorization)\\s*[:=]\\s*[^\\r\\n]*");
    private static final Pattern SECRET = Pattern.compile("(?i)(\\b(?:token|access_token|password|passwd|secret|signature|x-amz-[\\w-]+|x-goog-[\\w-]+)\\s*[:=]\\s*)[^\\s&;]+");
    private static final Pattern CREDENTIAL = Pattern.compile("(?i)\\b(?:Bearer|Basic)\\s+[A-Za-z0-9+/=_.-]+|\\bhf_[A-Za-z0-9_-]+");

    private NetworkDiagnostics() { }

    static Attempt attempt(String job, Map<String,Object> route, String url, String operation,
            int segment, long start, long end, BooleanSupplier active) {
        return new Attempt(job, route, uri(url), operation, segment, start + "-" + end, active);
    }

    static URI uri(String text) {
        try { return URI.create(text); }
        catch (RuntimeException malformed) { return null; }
    }

    /** No URL path, query, fragment or embedded credentials enter a diagnostic event. */
    static String server(URI uri) {
        if (uri == null || uri.getHost() == null) return "unknown";
        String host = uri.getHost();
        if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
        if (host.contains(":")) host = "[" + host + "]";
        int port = uri.getPort();
        if (port < 0) port = switch (uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT)) {
            case "https", "webdavs" -> 443;
            case "http" -> 80;
            case "sftp" -> 22;
            case "ftp", "ftps" -> 21;
            default -> -1;
        };
        return safe(port < 0 ? host : host + ":" + port);
    }

    static String safe(String text) {
        String value = text == null ? "" : text;
        value = URL.matcher(value).replaceAll(match -> java.util.regex.Matcher.quoteReplacement(serverWithoutSanitize(uri(match.group()))));
        value = AUTH.matcher(value).replaceAll("authorization=[hidden]");
        value = SECRET.matcher(value).replaceAll("$1[hidden]");
        value = CREDENTIAL.matcher(value).replaceAll("[hidden]");
        value = value.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ");
        int count = value.codePointCount(0, value.length());
        return count <= 1536 ? value : value.substring(0, value.offsetByCodePoints(0, 1536)) + "…";
    }

    private static String serverWithoutSanitize(URI uri) {
        if (uri == null || uri.getHost() == null) return "[source URL hidden]";
        return uri.getHost() + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
    }

    static String reason(Throwable error) {
        return error == null ? "unknown" : safe(error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    private static String gateway(Map<String,Object> route) {
        if (route == null) return "unknown";
        Object gateway = route.get("gateway");
        return safe(gateway == null || gateway.toString().isBlank() ? String.valueOf(route.getOrDefault("type", "unknown")) : gateway.toString());
    }

    static void requestFailure(Map<String,Object> route, URI source, String operation, long started, Throwable error) {
        if (Thread.currentThread().isInterrupted() || error instanceof InterruptedException) return;
        Attempt context = CURRENT.get();
        if (context != null) context.failure(source, operation, error);
        else AppLog.networkError("Network request failed: gateway=%s server=%s operation=%s elapsed_ms=%d error=%s",
                gateway(route), server(source), DiagnosticText.arg(operation), elapsed(started), DiagnosticText.arg(reason(error)));
    }

    private static long elapsed(long started) { return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)); }

    static final class Attempt implements AutoCloseable {
        final String job, operation, range;
        final Map<String,Object> route;
        final URI source;
        final int segment;
        final long started = System.nanoTime();
        final BooleanSupplier active;
        final Attempt previous;
        final Set<Throwable> reported;

        Attempt(String job, Map<String,Object> route, URI source, String operation, int segment, String range, BooleanSupplier active) {
            this.job = safe(job); this.route = route; this.source = source; this.operation = operation;
            this.segment = segment; this.range = range; this.active = active;
            previous = CURRENT.get();
            reported = previous == null ? Collections.newSetFromMap(new IdentityHashMap<>()) : previous.reported;
            CURRENT.set(this);
        }

        void failure(Throwable error) { failure(source, operation, error); }

        void failure(URI destination, String stage, Throwable error) {
            if (error instanceof InterruptedException || Thread.currentThread().isInterrupted() || !active.getAsBoolean() || !reported.add(error)) return;
            AppLog.networkError("Download attempt failed: job=%s gateway=%s server=%s operation=%s segment=%d range=%s elapsed_ms=%d error=%s",
                    job, gateway(route), server(destination), DiagnosticText.arg(stage), segment, range,
                    elapsed(started), DiagnosticText.arg(reason(error)));
        }

        @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
