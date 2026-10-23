package multiroute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http2.hpack.HPackDecoder;
import org.apache.hc.core5.http2.hpack.HPackEncoder;
import org.apache.hc.core5.http2.hpack.HPackException;
import org.apache.hc.core5.util.ByteArrayBuffer;

/** One bounded DoH exchange on an already authenticated, deadline-controlled h2 TLS stream. */
final class Http2Dns {
    @FunctionalInterface interface Reader { byte[] read(int length) throws IOException; }
    @FunctionalInterface interface Check { void check() throws IOException; }

    private static final int FRAME = 16384, HEADER_LIST = 32768, HEADER_BLOCK = 32768;
    private static final int MAX_FRAMES = 256, MAX_WIRE = 262144, MAX_ALL_HEADERS = 65536;
    private static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final Set<String> FORBIDDEN = Set.of("connection", "proxy-connection", "keep-alive", "transfer-encoding", "upgrade");
    private Http2Dns() { }

    static byte[] query(byte[] dns, URI uri, OutputStream out, Reader reader, Check check) throws IOException {
        Objects.requireNonNull(dns); Objects.requireNonNull(uri); Objects.requireNonNull(out);
        Objects.requireNonNull(reader); Objects.requireNonNull(check);
        if (dns.length < 12 || dns.length > DnsMessage.MAX_MESSAGE) throw bad("Invalid DNS request size");
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getFragment() != null) throw bad("Invalid DoH URI");
        return new Exchange(dns.clone(), uri, out, reader, check).run();
    }

    private static final class Exchange {
        final byte[] request;
        final URI uri;
        final OutputStream out;
        final Reader reader;
        final Check check;
        final HPackDecoder decoder = new HPackDecoder(4096, StandardCharsets.US_ASCII);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        int frames, wireBytes, allHeaders, allDecodedHeaders, sent, informational;
        long sendConnection = 65535, sendStream = 65535, initialWindow = 65535, concurrent = Long.MAX_VALUE;
        boolean first = true, requestHeaders, finalHeaders, trailers, continuation, blockEnd, settingsAck;
        long expectedLength = -1, age;

        Exchange(byte[] request, URI uri, OutputStream out, Reader reader, Check check) {
            this.request = request; this.uri = uri; this.out = out; this.reader = reader; this.check = check;
            decoder.setMaxListSize(HEADER_LIST); // Configure BEFORE any untrusted HPACK decoding.
            decoder.setMaxTableSize(4096);
        }

        byte[] run() throws IOException {
            check.check(); out.write(PREFACE);
            ByteBuffer settings = ByteBuffer.allocate(24);
            settings.putShort((short)1).putInt(4096).putShort((short)2).putInt(0)
                    .putShort((short)4).putInt(MAX_WIRE).putShort((short)6).putInt(HEADER_LIST);
            frame(4, 0, 0, settings.array());
            // The complete bounded response, including DATA padding, fits this credit. There
            // is no need to write late WINDOW_UPDATE frames after a peer has finished/closed.
            window(0, MAX_WIRE - 65535); out.flush();
            while (true) {
                check.check();
                byte[] header = exact(9);
                int length = ((header[0] & 255) << 16) | ((header[1] & 255) << 8) | (header[2] & 255);
                int type = header[3] & 255, flags = header[4] & 255;
                int stream = integer(header, 5) & 0x7fffffff;
                if (++frames > MAX_FRAMES || length > FRAME || (wireBytes += 9 + length) > MAX_WIRE)
                    throw bad("HTTP/2 response limit exceeded");
                if (first && (type != 4 || stream != 0 || (flags & 1) != 0)) throw bad("Missing server SETTINGS preface");
                if (continuation && (type != 9 || stream != 1)) throw bad("Interrupted HTTP/2 header block");
                byte[] payload = exact(length);
                first = false;
                switch (type) {
                    case 0 -> {
                        if (stream != 1 || !requestHeaders || !finalHeaders || trailers || continuation) throw bad("Unexpected HTTP/2 DATA");
                        int offset = 0, padding = 0;
                        if ((flags & 8) != 0) { if (length == 0) throw bad("Invalid DATA padding"); padding = payload[offset++] & 255; }
                        int count = length - offset - padding;
                        if (count < 0 || body.size() + count > DnsMessage.MAX_MESSAGE) throw bad("Invalid DoH body size");
                        body.write(payload, offset, count);
                        if ((flags & 1) != 0) return finish();
                    }
                    case 1 -> {
                        if (stream != 1 || !requestHeaders || trailers) throw bad("Unexpected HTTP/2 HEADERS");
                        blockEnd = (flags & 1) != 0;
                        int offset = 0, padding = 0;
                        if ((flags & 8) != 0) { if (length == 0) throw bad("Invalid HEADERS padding"); padding = payload[offset++] & 255; }
                        if ((flags & 32) != 0) {
                            if (length - offset < 5) throw bad("Invalid HEADERS priority");
                            if ((integer(payload, offset) & 0x7fffffff) == 1) throw bad("Self-dependent HTTP/2 stream");
                            offset += 5;
                        }
                        appendHeaders(payload, offset, length - offset - padding);
                        continuation = (flags & 4) == 0;
                        if (!continuation && decodeHeaders()) return finish();
                    }
                    case 2 -> {
                        if (stream == 0 || length != 5 || (integer(payload, 0) & 0x7fffffff) == stream)
                            throw bad("Invalid HTTP/2 PRIORITY");
                    }
                    case 3 -> {
                        if (stream != 1 || !requestHeaders || length != 4) throw bad("Invalid HTTP/2 RST_STREAM");
                        throw bad("DoH HTTP/2 stream reset (code " + unsigned(payload, 0) + ")");
                    }
                    case 4 -> settings(payload, flags, stream);
                    case 5 -> throw bad("Unsolicited HTTP/2 server push");
                    case 6 -> {
                        if (stream != 0 || length != 8) throw bad("Invalid HTTP/2 PING");
                        if ((flags & 1) == 0) { frame(6, 1, 0, payload); out.flush(); }
                    }
                    case 7 -> {
                        if (stream != 0 || length < 8) throw bad("Invalid HTTP/2 GOAWAY");
                        if ((integer(payload, 0) & 0x7fffffff) < 1 || unsigned(payload, 4) != 0)
                            throw bad("DoH HTTP/2 connection closed by server");
                        // A graceful GOAWAY for stream 1 still permits its existing response to finish.
                    }
                    case 8 -> {
                        if (length != 4 || (stream != 0 && (stream != 1 || !requestHeaders))) throw bad("Invalid HTTP/2 WINDOW_UPDATE");
                        long increment = integer(payload, 0) & 0x7fffffff;
                        if (increment == 0) throw bad("Zero HTTP/2 window increment");
                        if (stream == 0) { sendConnection += increment; if (sendConnection > 0x7fffffffL) throw bad("HTTP/2 connection window overflow"); }
                        else { sendStream += increment; if (sendStream > 0x7fffffffL) throw bad("HTTP/2 stream window overflow"); }
                    }
                    case 9 -> {
                        if (!continuation || stream != 1) throw bad("Unexpected HTTP/2 CONTINUATION");
                        appendHeaders(payload, 0, length);
                        if ((flags & 4) != 0) { continuation = false; if (decodeHeaders()) return finish(); }
                    }
                    default -> { /* RFC 9113: ignore unknown extension frames, within the common bounds. */ }
                }
                sendRequest();
            }
        }

        void settings(byte[] payload, int flags, int stream) throws IOException {
            if (stream != 0 || payload.length % 6 != 0) throw bad("Invalid HTTP/2 SETTINGS");
            if ((flags & 1) != 0) {
                if (payload.length != 0 || settingsAck) throw bad("Invalid HTTP/2 SETTINGS acknowledgement");
                settingsAck = true; return;
            }
            for (int at = 0; at < payload.length; at += 6) {
                int id = ((payload[at] & 255) << 8) | (payload[at + 1] & 255);
                long value = unsigned(payload, at + 2);
                if (id == 2) throw bad("Server sent SETTINGS_ENABLE_PUSH");
                if (id == 3) concurrent = value;
                if (id == 4) {
                    if (value > 0x7fffffffL) throw bad("Invalid HTTP/2 initial window");
                    sendStream += value - initialWindow; initialWindow = value;
                    if (sendStream > 0x7fffffffL) throw bad("HTTP/2 stream window overflow");
                }
                if (id == 5 && (value < FRAME || value > 16777215)) throw bad("Invalid HTTP/2 maximum frame size");
                if (id == 8 && value > 1) throw bad("Invalid HTTP/2 CONNECT setting");
            }
            frame(4, 1, 0, new byte[0]); out.flush();
        }

        void sendRequest() throws IOException {
            if (!requestHeaders && concurrent > 0) {
                String authority = uri.getHost() + (uri.getPort() < 0 || uri.getPort() == 443 ? "" : ":" + uri.getPort());
                String path = uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
                List<Header> headers = List.of(new BasicHeader(":method", "POST"), new BasicHeader(":scheme", "https"),
                        new BasicHeader(":authority", authority), new BasicHeader(":path", path),
                        new BasicHeader("accept", "application/dns-message"), new BasicHeader("content-type", "application/dns-message"),
                        new BasicHeader("accept-encoding", "identity"), new BasicHeader("content-length", Integer.toString(request.length)));
                ByteArrayBuffer encoded = new ByteArrayBuffer(256);
                new HPackEncoder(0, StandardCharsets.US_ASCII).encodeHeaders(encoded, headers, true);
                if (encoded.length() > HEADER_BLOCK) throw bad("DoH request headers too large");
                for (int at = 0; at < encoded.length();) {
                    int count = Math.min(FRAME, encoded.length() - at);
                    frame(at == 0 ? 1 : 9, at + count == encoded.length() ? 4 : 0, 1, Arrays.copyOfRange(encoded.array(), at, at + count));
                    at += count;
                }
                requestHeaders = true;
            }
            while (requestHeaders && sent < request.length && sendConnection > 0 && sendStream > 0) {
                int count = (int)Math.min(Math.min(FRAME, request.length - sent), Math.min(sendConnection, sendStream));
                frame(0, sent + count == request.length ? 1 : 0, 1, Arrays.copyOfRange(request, sent, sent + count));
                sent += count; sendConnection -= count; sendStream -= count;
            }
            out.flush(); check.check();
        }

        void appendHeaders(byte[] payload, int offset, int length) throws IOException {
            if (length < 0 || compressed.size() + length > HEADER_BLOCK || (allHeaders += Math.max(0, length)) > MAX_ALL_HEADERS)
                throw bad("HTTP/2 compressed header limit exceeded");
            compressed.write(payload, offset, length);
        }

        boolean decodeHeaders() throws IOException {
            List<Header> decoded;
            try { decoded = decoder.decodeHeaders(ByteBuffer.wrap(compressed.toByteArray())); }
            catch (HPackException | IllegalArgumentException malformed) { throw new IOException("Invalid DoH HPACK headers", malformed); }
            compressed.reset();
            if (decoded.size() > 128) throw bad("Too many DoH headers");
            for (Header header : decoded) {
                allDecodedHeaders += header.getName().length() + header.getValue().length() + 32;
                if (allDecodedHeaders > MAX_ALL_HEADERS) throw bad("HTTP/2 decoded header limit exceeded");
            }
            boolean regular = false; String status = null;
            Map<String,List<String>> fields = new HashMap<>();
            for (Header header : decoded) {
                String name = header.getName(), value = header.getValue();
                if (name.isEmpty() || !name.equals(name.toLowerCase(Locale.ROOT)) || value == null) throw bad("Invalid HTTP/2 header name");
                for (int at = 0; at < name.length(); at++) {
                    char c = name.charAt(at);
                    if ((c == ':' && at == 0) || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0) continue;
                    throw bad("Invalid HTTP/2 header name");
                }
                if (!value.equals(value.strip()) || value.chars().anyMatch(c -> c == 0 || c == 10 || c == 13 || c == 127 || (c < 32 && c != 9)))
                    throw bad("Invalid HTTP/2 header value");
                if (name.charAt(0) == ':') {
                    if (regular || finalHeaders || !name.equals(":status") || status != null) throw bad("Invalid response pseudo-header");
                    status = value;
                } else {
                    regular = true;
                    if (FORBIDDEN.contains(name) || name.equals("te")) throw bad("Forbidden HTTP/2 connection header");
                    fields.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
                }
            }
            if (finalHeaders) {
                if (!blockEnd || status != null || fields.containsKey("content-length") || fields.containsKey("content-type")
                        || fields.containsKey("content-encoding") || fields.containsKey("age")) throw bad("Invalid DoH trailers");
                trailers = true; return true;
            }
            if (status == null || !status.matches("[1-5][0-9]{2}")) throw bad("Missing DoH HTTP/2 status");
            int code = Integer.parseInt(status);
            if (code < 200) {
                if (code == 101 || blockEnd || ++informational > 4) throw bad("Unexpected informational DoH response");
                return false;
            }
            if (code != 200) throw bad("DoH HTTP status " + code);
            String type = single(fields, "content-type", true);
            if (!type.split(";", 2)[0].trim().equalsIgnoreCase("application/dns-message")) throw bad("Invalid DoH Content-Type");
            String encoding = single(fields, "content-encoding", false);
            if (encoding != null && !encoding.equalsIgnoreCase("identity")) throw bad("Unexpected DoH Content-Encoding");
            String length = single(fields, "content-length", false);
            if (length != null) {
                expectedLength = decimal(length, "Content-Length");
                if (expectedLength < 12 || expectedLength > DnsMessage.MAX_MESSAGE) throw bad("Invalid DoH Content-Length");
            }
            String rawAge = single(fields, "age", false);
            if (rawAge != null) age = decimal(rawAge, "Age");
            finalHeaders = true;
            return blockEnd;
        }

        byte[] finish() throws IOException {
            check.check();
            if (!finalHeaders || body.size() < 12 || (expectedLength >= 0 && expectedLength != body.size())) throw bad("Incomplete DoH HTTP/2 body");
            return age == 0 ? body.toByteArray() : DnsMessage.reduceTtlsByAge(body.toByteArray(), age);
        }

        byte[] exact(int length) throws IOException {
            check.check(); byte[] bytes = reader.read(length);
            if (bytes == null || bytes.length != length) throw bad("Truncated HTTP/2 frame");
            check.check(); return bytes;
        }
        void window(int stream, int increment) throws IOException { frame(8, 0, stream, ByteBuffer.allocate(4).putInt(increment).array()); }
        void frame(int type, int flags, int stream, byte[] payload) throws IOException {
            check.check();
            byte[] header = new byte[9]; int length = payload.length;
            header[0] = (byte)(length >>> 16); header[1] = (byte)(length >>> 8); header[2] = (byte)length;
            header[3] = (byte)type; header[4] = (byte)flags;
            ByteBuffer.wrap(header, 5, 4).putInt(stream);
            out.write(header); out.write(payload); check.check();
        }
    }

    private static String single(Map<String,List<String>> fields, String name, boolean required) throws IOException {
        List<String> values = fields.get(name);
        if (values == null) { if (required) throw bad("Missing DoH " + name); return null; }
        if (values.size() != 1) throw bad("Duplicate DoH " + name);
        return values.get(0);
    }
    private static long decimal(String value, String name) throws IOException {
        if (value.isEmpty() || value.length() > 19 || !value.chars().allMatch(c -> c >= '0' && c <= '9')) throw bad("Invalid DoH " + name);
        try { return Long.parseLong(value); } catch (NumberFormatException large) { throw bad("Invalid DoH " + name); }
    }
    private static int integer(byte[] bytes, int at) { return ByteBuffer.wrap(bytes, at, 4).getInt(); }
    private static long unsigned(byte[] bytes, int at) { return Integer.toUnsignedLong(integer(bytes, at)); }
    private static IOException bad(String reason) { return new IOException(reason); }
}
