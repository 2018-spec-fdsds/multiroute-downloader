package multiroute;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit system, remote worker and application-owned gateway transports. */
public final class Transport {
    public static final long MAX_SEGMENT = 64L * 1024 * 1024;
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");
    private static final ProxySelector NO_PROXY = new ProxySelector() {
        @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
        @Override public void connectFailed(URI uri, SocketAddress address, IOException error) { }
    };
    private static final HttpClient ORIGIN_CLIENT = HttpClient.newBuilder()
            .proxy(NO_PROXY)
            .sslContext(TlsTrust.context())
            .connectTimeout(Duration.ofSeconds(15))
            // Redirects are followed explicitly so a worker can verify every new destination's gateway.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final HttpClient WORKER_CLIENT = HttpClient.newBuilder()
            .proxy(NO_PROXY)
            .sslContext(TlsTrust.context())
            .connectTimeout(Duration.ofSeconds(15))
            // Never forward X-Route-Token to a redirect target.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final java.util.concurrent.ConcurrentMap<String,HttpClient> GATEWAY_CLIENTS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static HttpClient originClient(Map<String,Object> route) throws IOException, InterruptedException {
        if (!"gateway".equals(route.get("type"))) return ORIGIN_CLIENT;
        URI endpoint = SourceTransfer.endpoints.endpoint(route);
        return GATEWAY_CLIENTS.computeIfAbsent(endpoint.toString(), key -> HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", endpoint.getPort())))
                .sslContext(TlsTrust.context())
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    static void resetGatewayClients() { GATEWAY_CLIENTS.clear(); }

    private Transport() { }

    private static HttpResponse<InputStream> send(HttpClient client, Map<String,Object> route,
            HttpRequest request) throws IOException, InterruptedException {
        long started = System.nanoTime();
        try { return client.send(request, HttpResponse.BodyHandlers.ofInputStream()); }
        catch (IOException error) {
            NetworkDiagnostics.requestFailure(route, request.uri(), "HTTP request", started, error);
            throw error;
        }
    }

    /** A protocol or access failure that another identical attempt cannot repair. */
    public static class NonRetryableException extends IOException {
        public NonRetryableException(String message) { super(message); }
        public NonRetryableException(String message, Throwable cause) { super(message, cause); }
    }

    /** A publisher redirect failed URL, origin, TLS or redirect-count validation. */
    public static final class UnsafeRedirectException extends NonRetryableException {
        public UnsafeRedirectException(String message) { super(message); }
        public UnsafeRedirectException(String message, Throwable cause) { super(message, cause); }
    }

    public static HttpResponse<InputStream> open(Map<String, Object> route, String url,
            long start, long end, String etag, String modified) throws IOException, InterruptedException {
        if (route == null) throw new IOException("Missing route");
        uri(url);
        if (start < 0 || end < start || end - start >= MAX_SEGMENT)
            throw new IOException("Invalid or oversized byte range");
        String type = field(route, "type");
        HttpRequest.Builder request;
        if ("direct".equals(type) || "default".equals(type) || "gateway".equals(type)) {
            HttpClient client = originClient(route);
            URI current = uri(url);
            String gateway = route.get("gateway") instanceof String value ? value : "";
            for (int redirects = 0; redirects <= 5; redirects++) {
                if (!"gateway".equals(type) && !gateway.isBlank()) Worker.assertRoute(current.getHost(), gateway);
                request = HttpRequest.newBuilder(current)
                        .GET()
                        .header("Range", "bytes=" + start + "-" + end)
                        .header("Accept-Encoding", "identity")
                        .header("Want-Repr-Digest", "sha-256=10")
                        .header("Cache-Control", "no-transform")
                        .header("User-Agent", "MultiRoute-Downloader/4.0");
                if (etag != null && !etag.isBlank() && !etag.startsWith("W/")) request.header("If-Range", etag);
                else if (modified != null && !modified.isBlank()) request.header("If-Range", modified);
                SourceAuth.apply(request, current,route);
                HttpResponse<InputStream> response = send(client, route,
                        request.timeout(Duration.ofSeconds(60)).build());
                int status = response.statusCode();
                if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308)
                    return response;
                current = redirectTarget(current, response, redirects, false);
            }
            throw new IOException("Too many redirects");
        } else if ("worker".equals(type)) {
            if (!Main.str(route.get("site_profile_id")).isEmpty() || SourceAuth.hasCredentials(url)) throw new NonRetryableException("Source authentication requires an application gateway");
            URI endpoint = workerEndpoint(field(route, "endpoint"));
            String base = endpoint.toString().replaceAll("/+$", "");
            request = HttpRequest.newBuilder(uri(base + "/range"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "MultiRoute-Downloader/4.0")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(Map.of(
                            "url", url, "start", start, "end", end,
                            "etag", etag == null ? "" : etag,
                            "modified", modified == null ? "" : modified)), StandardCharsets.UTF_8));
            withWorkerToken(request, route);
        } else throw new IOException("Unknown route type: " + type);
        return checkedWorkerResponse(WORKER_CLIENT.send(request.timeout(Duration.ofSeconds(60)).build(),
                HttpResponse.BodyHandlers.ofInputStream()));
    }

    /** BitTorrent tracker/BEP17 GET, retaining the selected gateway and normal TLS validation. */
    static HttpResponse<InputStream> torrentGet(Map<String,Object> route,String url)throws IOException,InterruptedException {
        if(route==null||!"gateway".equals(route.get("type")))throw new IOException("Torrent GET requires a selected gateway");
        HttpClient client=originClient(route);URI current=uri(url);
        for(int redirects=0;redirects<=5;redirects++){
            HttpRequest.Builder builder=HttpRequest.newBuilder(current).GET().timeout(Duration.ofSeconds(60))
                    .header("Accept-Encoding","identity").header("User-Agent","MultiRoute-Downloader/4.0");
            SourceAuth.apply(builder,current,route);
            HttpRequest request=builder.build();
            HttpResponse<InputStream> response=send(client,route,request);int status=response.statusCode();
            if(status!=301&&status!=302&&status!=303&&status!=307&&status!=308)return response;
            current=redirectTarget(current,response,redirects,false);
        }throw new IOException("Too many redirects");
    }

    /** Check metadata before writing a segment; returns the full representation length. */
    public static long validate(HttpResponse<InputStream> response, long start, long end,
            Long expectedTotal, String etag, String modified) throws IOException {
        if (response.statusCode() != 206) throw rangeError(response.statusCode());
        List<String> ranges = response.headers().allValues("Content-Range");
        if (ranges.size() != 1) throw new IOException("Missing or duplicate Content-Range");
        Matcher match = CONTENT_RANGE.matcher(ranges.get(0).trim());
        if (!match.matches()) throw new IOException("Invalid Content-Range: " + ranges.get(0));
        long actualStart, actualEnd, total;
        try {
            actualStart = Long.parseLong(match.group(1));
            actualEnd = Long.parseLong(match.group(2));
            total = Long.parseLong(match.group(3));
        } catch (NumberFormatException e) { throw new IOException("Content-Range overflows long", e); }
        if (actualStart != start || actualEnd != end || total <= end)
            throw new IOException("Unexpected Content-Range: " + ranges.get(0));
        if (expectedTotal != null && total != expectedTotal)
            throw new IOException("Remote file size changed");
        if (etag != null && etag.startsWith("W/"))
            throw new IOException("Weak ETag cannot safely validate combined byte ranges");
        String encoding = response.headers().firstValue("Content-Encoding").orElse("identity");
        if (!encoding.equalsIgnoreCase("identity")) throw new IOException("Unexpected Content-Encoding: " + encoding);
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/"))
            throw new IOException("Multipart ranges are unsupported");
        String length = response.headers().firstValue("Content-Length").orElse(null);
        if (length != null) {
            try {
                if (Long.parseLong(length) != end - start + 1) throw new IOException("Content-Length does not match range");
            } catch (NumberFormatException e) { throw new IOException("Invalid Content-Length", e); }
        }
        if (etag != null && !etag.isBlank() && !etag.equals(response.headers().firstValue("ETag").orElse(null)))
            throw new IOException("Remote ETag changed or is missing");
        if ((etag == null || etag.isBlank()) && modified != null && !modified.isBlank() &&
                !modified.equals(response.headers().firstValue("Last-Modified").orElse(null)))
            throw new IOException("Remote Last-Modified changed or is missing");
        return total;
    }

    /** Return an egress check for the exact route used by downloads. */
    public static Map<String, Object> probe(Map<String, Object> route) throws IOException, InterruptedException {
        if (route == null) throw new IOException("Missing route");
        String type = field(route, "type");
        HttpRequest.Builder request;
        if ("direct".equals(type) || "default".equals(type) || "gateway".equals(type)) {
            String expected = route.get("gateway") instanceof String value ? value : "";
            if (!"gateway".equals(type) && !expected.isBlank()) Worker.assertRoute("api.ipify.org", expected);
            request = HttpRequest.newBuilder(uri("https://api.ipify.org"))
                    .GET().header("Accept-Encoding", "identity");
        } else if ("worker".equals(type)) {
            URI endpoint = workerEndpoint(field(route, "endpoint"));
            String base = endpoint.toString().replaceAll("/+$", "");
            request = HttpRequest.newBuilder(uri(base + "/probe"))
                    .GET();
            withWorkerToken(request, route);
        } else throw new IOException("Unknown route type: " + type);
        HttpResponse<InputStream> response = ("worker".equals(type) ? WORKER_CLIENT : originClient(route))
                .send(request.timeout(Duration.ofSeconds(20)).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            byte[] bytes = body.readNBytes(4097);
            if (bytes.length > 4096) throw new IOException("Probe response too large");
            if (response.statusCode() != 200) throw new IOException("Probe HTTP " + response.statusCode() + ": " +
                    new String(bytes, StandardCharsets.UTF_8).trim());
            String content = new String(bytes, StandardCharsets.UTF_8).trim();
            if ("direct".equals(type) || "default".equals(type) || "gateway".equals(type)) {
                if (!content.matches("[0-9a-fA-F:.]+")) throw new IOException("Invalid public IP response");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("ok", true);
                result.put("egress_ip", content);
                result.put("gateway_hint", "gateway".equals(type) ?
                        "Адрес приложения " + GatewayNetwork.settings().get("virtual_ip") + " → " + route.get("gateway") : Worker.gatewayHint());
                String expected = route.get("gateway") instanceof String value ? value : "";
                result.put("expected_gateway", expected);
                result.put("gateway_match", expected.isBlank() ? null : true);
                return result;
            }
            Object parsed = Json.parse(content);
            if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("Invalid worker probe JSON");
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new IOException("Invalid worker probe JSON key");
                result.put((String) entry.getKey(), entry.getValue());
            }
            if (!Boolean.TRUE.equals(result.get("ok")) || !(result.get("egress_ip") instanceof String ip) || ip.isBlank())
                throw new IOException("Worker did not verify its public IP");
            return result;
        }
    }

    /** Start on the publisher's origin; authenticated HTTPS mirrors may follow its redirects. */
    public static HttpResponse<InputStream> small(Map<String, Object> route, String url,
            String sourceOrigin) throws IOException, InterruptedException {
        if (route == null) throw new IOException("Missing route");
        URI source = sidecarUri(sourceOrigin);
        URI current = sidecarUri(url);
        if (!sameOrigin(current, source)) throw new UnsafeRedirectException("Sidecar must share source origin");
        String type = field(route, "type");
        if ("direct".equals(type) || "default".equals(type) || "gateway".equals(type)) {
            HttpClient client = originClient(route);
            String gateway = route.get("gateway") instanceof String value ? value : "";
            for (int redirects = 0; redirects <= 5; redirects++) {
                if (!"gateway".equals(type) && !gateway.isBlank()) Worker.assertRoute(current.getHost(), gateway);
                HttpRequest.Builder builder = HttpRequest.newBuilder(current).GET()
                        .header("Accept-Encoding", "identity")
                        .header("Cache-Control", "no-transform")
                        .header("User-Agent", "MultiRoute-Downloader/4.0")
                        .timeout(Duration.ofSeconds(30));
                SourceAuth.apply(builder,current,route);
                HttpRequest request=builder.build();
                HttpResponse<InputStream> response = send(client, route, request);
                int status = response.statusCode();
                if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308)
                    return response;
                current = redirectTarget(current, response, redirects, true);
            }
            throw new UnsafeRedirectException("Too many sidecar redirects");
        }
        if ("worker".equals(type)) {
            if (!Main.str(route.get("site_profile_id")).isEmpty() || SourceAuth.hasCredentials(url)) throw new NonRetryableException("Source authentication requires an application gateway");
            URI endpoint = workerEndpoint(field(route, "endpoint"));
            String base = endpoint.toString().replaceAll("/+$", "");
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(base + "/small"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(Map.of(
                            "url", url, "origin", sourceOrigin)), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(35));
            withWorkerToken(request, route);
            return checkedWorkerResponse(WORKER_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofInputStream()));
        }
        throw new IOException("Unknown route type: " + type);
    }

    /** Whole HTTP transfer for publishers that do not support ranges. No resume is claimed. */
    public static HttpResponse<InputStream> whole(Map<String,Object> route,String url)throws IOException,InterruptedException {
        return request(route,url,"GET",Map.of(),null);
    }

    /** Selected-gateway metadata requests; credentials are re-evaluated per redirect origin. */
    static HttpResponse<InputStream> request(Map<String,Object> route,String url,String method,Map<String,String> headers,String body)throws IOException,InterruptedException {
        if(route==null||!"gateway".equals(route.get("type")))
            throw new NonRetryableException("This source request requires a selected application gateway");
        HttpClient client=originClient(route);URI current=uri(url);
        for(int redirects=0;redirects<=5;redirects++){
            HttpRequest.Builder builder=HttpRequest.newBuilder(current).timeout(Duration.ofSeconds(60))
                    .header("Accept-Encoding","identity").header("User-Agent","MultiRoute-Downloader/4.0")
                    .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
            for(var header:headers.entrySet())builder.header(header.getKey(),header.getValue());
            SourceAuth.apply(builder,current,route);
            HttpResponse<InputStream> response=send(client,route,builder.build());
            int status=response.statusCode();
            if(status!=301&&status!=302&&status!=303&&status!=307&&status!=308)return response;
            if(!method.equals("GET")&&!method.equals("HEAD")){
                URI next=redirectTarget(current,response,redirects,false);
                if(status!=307&&status!=308)throw new UnsafeRedirectException("Metadata method redirect requires HTTP 307 or 308");
                if(!sameOrigin(current,next))throw new UnsafeRedirectException("Metadata redirect must retain the source origin");
                current=next;
            }else current=redirectTarget(current,response,redirects,false);
        }throw new UnsafeRedirectException("Too many redirects");
    }

    private static URI sidecarUri(String text) throws UnsafeRedirectException {
        try { return uri(text); }
        catch (IOException | IllegalArgumentException error) {
            throw new UnsafeRedirectException("Invalid checksum URL: " + error.getMessage(), error);
        }
    }

    private static HttpResponse<InputStream> checkedWorkerResponse(HttpResponse<InputStream> response) throws IOException {
        if (!"true".equalsIgnoreCase(response.headers().firstValue("X-MultiRoute-NonRetryable").orElse("")))
            return response;
        String message = "Worker rejected the source request (HTTP " + response.statusCode() + ")";
        try (InputStream body = response.body()) {
            byte[] bytes = body.readNBytes(4097);
            if (bytes.length <= 4096) {
                try {
                    Object value = Json.parse(new String(bytes, StandardCharsets.UTF_8));
                    if (value instanceof Map<?, ?> object && object.get("error") instanceof String error && !error.isBlank())
                        message = error;
                } catch (IllegalArgumentException ignored) { /* retain the bounded generic error */ }
            }
        }
        throw new NonRetryableException(message);
    }

    /** Validate before another request; the caller retains its selected client/worker route. */
    private static URI redirectTarget(URI current, HttpResponse<InputStream> response,
            int redirects, boolean sidecar) throws IOException {
        List<String> locations = response.headers().allValues("Location");
        response.body().close();
        if (redirects >= 5) throw new UnsafeRedirectException("Too many " + (sidecar ? "sidecar " : "") + "redirects");
        if (locations.size() != 1 || locations.get(0).isBlank())
            throw new UnsafeRedirectException("Missing, duplicate or empty redirect Location");
        URI next;
        try { next = uri(current.resolve(locations.get(0)).toString()); }
        catch (IOException | IllegalArgumentException error) {
            throw new UnsafeRedirectException("Invalid redirect Location", error);
        }
        if ("https".equalsIgnoreCase(current.getScheme()) && !"https".equalsIgnoreCase(next.getScheme()))
            throw new UnsafeRedirectException("HTTPS redirect downgrade refused");
        if (sidecar && !sameOrigin(current, next) && !"https".equalsIgnoreCase(next.getScheme()))
            throw new UnsafeRedirectException("Checksum mirror redirect requires HTTPS");
        return next;
    }

    private static void withWorkerToken(HttpRequest.Builder request, Map<String, Object> route) throws IOException {
        Object configured = route.get("token");
        if (configured == null) return;
        if (!(configured instanceof String token)) throw new IOException("Invalid worker token field");
        if (!token.isEmpty()) request.header("X-Route-Token", token);
    }

    /** Explains origin failures without exposing signed URLs or response bodies. */
    private static IOException rangeError(int status) {
        String message = switch (status) {
            case 200 -> "Источник не поддерживает запросы частей файла (HTTP 200 вместо 206)";
            case 401, 403 -> "Источник отказал в доступе (HTTP " + status + "). Проверьте, что файл общедоступен; закрытый репозиторий требует авторизации";
            case 404 -> "Файл не найден (HTTP 404). Используйте ссылку на файл или выберите его из списка репозитория";
            case 416 -> "Источник отклонил диапазон байтов (HTTP 416). Размер или версия файла могли измениться";
            case 429 -> "Источник временно ограничил частоту запросов (HTTP 429). Повторите загрузку позднее";
            default -> "Источник вернул HTTP " + status + " вместо ответа с частью файла";
        };
        return status == 200 || status == 416 || (status >= 400 && status < 500 && status != 429)
                ? new NonRetryableException(message) : new IOException(message);
    }

    static boolean sameOrigin(URI a, URI b) {
        if (!a.getScheme().equalsIgnoreCase(b.getScheme()) ||
                !a.getHost().equalsIgnoreCase(b.getHost())) return false;
        int aPort = a.getPort() < 0 ? ("https".equalsIgnoreCase(a.getScheme()) ? 443 : 80) : a.getPort();
        int bPort = b.getPort() < 0 ? ("https".equalsIgnoreCase(b.getScheme()) ? 443 : 80) : b.getPort();
        return aPort == bPort;
    }

    static URI uri(String text) throws IOException {
        try {
            URI result = URI.create(text);
            if (!("http".equalsIgnoreCase(result.getScheme()) || "https".equalsIgnoreCase(result.getScheme())) ||
                    result.getHost() == null || result.getRawUserInfo() != null || result.getRawFragment() != null ||
                    result.getPort() == 0 || result.getPort() > 65535)
                throw new IOException("Expected http(s) URL without credentials or fragment");
            return result;
        } catch (IllegalArgumentException e) { throw new IOException("Invalid URL", e); }
    }

    private static URI workerEndpoint(String text) throws IOException {
        URI endpoint = uri(text);
        if (endpoint.getRawQuery() != null) throw new IOException("Worker endpoint cannot contain a query");
        return endpoint;
    }

    static String field(Map<String, Object> object, String key) throws IOException {
        Object value = object.get(key);
        if (!(value instanceof String text)) throw new IOException("Missing text field: " + key);
        return text;
    }
}
