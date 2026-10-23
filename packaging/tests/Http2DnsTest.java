package multiroute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http2.hpack.HPackDecoder;
import org.apache.hc.core5.http2.hpack.HPackEncoder;
import org.apache.hc.core5.util.ByteArrayBuffer;

/** Owned byte streams and authenticated loopback TLS only. No public DNS or installed app. */
public final class Http2DnsTest {
    private static int checks;
    private static final List<String> cases = new ArrayList<>();
    private static final URI URI_FIXTURE = URI.create("https://resolver.invalid:8443/dns-query");
    private static final byte[] QUERY;
    static { try { QUERY = DnsMessage.queryA("пример.рф"); } catch (IOException bad) { throw new ExceptionInInitializerError(bad); } }
    private interface Checked { void run() throws Exception; }
    private interface Handler { void run(SSLSocket socket) throws Exception; }

    public static void main(String[] args) throws Exception {
        Path owned = Path.of(args[0]); Files.createDirectories(owned);
        byte[] answer = answer(QUERY, 60);
        test("Huffman POST, settings ACK, complete DNS and Unicode", () -> {
            ByteArrayOutputStream input = new ByteArrayOutputStream();
            frame(input, 4, 0, 0, new byte[0]);
            frame(input, 1, 4, 1, headers(true, ":status", "200", "content-type", "application/dns-message", "content-length", "" + answer.length, "age", "10"));
            frame(input, 0, 1, 1, answer);
            ByteArrayOutputStream sent = new ByteArrayOutputStream(); byte[] result = invoke(input.toByteArray(), sent);
            require(ttl(result) == 50); DnsMessage.validateResponse(QUERY, result);
            inspectRequest(sent.toByteArray(), QUERY);
        });
        test("continuation, DATA padding, PING and graceful GOAWAY", () -> {
            ByteArrayOutputStream input = new ByteArrayOutputStream(); frame(input, 4, 0, 0, new byte[0]);
            byte[] fields = headers(true, ":status", "200", "content-type", "application/dns-message");
            frame(input, 6, 0, 0, new byte[]{0,1,2,3,4,5,6,7});
            frame(input, 1, 0, 1, Arrays.copyOfRange(fields, 0, 2)); frame(input, 9, 4, 1, Arrays.copyOfRange(fields, 2, fields.length));
            frame(input, 7, 0, 0, ByteBuffer.allocate(8).putInt(1).putInt(0).array());
            byte[] data = new byte[answer.length + 4]; data[0] = 3; System.arraycopy(answer, 0, data, 1, answer.length);
            frame(input, 0, 9, 1, data); ByteArrayOutputStream sent = new ByteArrayOutputStream();
            require(Arrays.equals(invoke(input.toByteArray(), sent), answer));
            List<Frame> written = frames(sent.toByteArray(), 24); require(written.stream().anyMatch(f -> f.type == 6 && f.flags == 1 && Arrays.equals(f.data, new byte[]{0,1,2,3,4,5,6,7})));
            require(written.stream().filter(f -> f.type == 8).count() == 1);
        });
        test("zero initial window then update and split request DATA", () -> {
            ByteArrayOutputStream input = new ByteArrayOutputStream(); frame(input, 4, 0, 0, ByteBuffer.allocate(6).putShort((short)4).putInt(0).array());
            frame(input, 8, 0, 1, ByteBuffer.allocate(4).putInt(5).array());
            frame(input, 8, 0, 1, ByteBuffer.allocate(4).putInt(65530).array()); response(input, answer);
            ByteArrayOutputStream sent = new ByteArrayOutputStream(); invoke(input.toByteArray(), sent);
            List<Frame> data = frames(sent.toByteArray(), 24).stream().filter(f -> f.type == 0).toList();
            require(data.size() == 2 && data.get(0).data.length == 5 && data.get(1).flags == 1);
        });
        test("informational headers and safe trailers", () -> {
            ByteArrayOutputStream input = preface(); frame(input, 1, 4, 1, headers(false, ":status", "103", "link", "</own>"));
            frame(input, 1, 4, 1, headers(false, ":status", "200", "content-type", "application/dns-message"));
            frame(input, 0, 0, 1, answer); frame(input, 1, 5, 1, headers(true, "x-own", "complete"));
            require(Arrays.equals(invoke(input.toByteArray(), new ByteArrayOutputStream()), answer));
        });
        test("HEADERS priority and padding", () -> {
            ByteArrayOutputStream input = preface(); byte[] fields = headers(true, ":status", "200", "content-type", "application/dns-message");
            ByteArrayOutputStream padded = new ByteArrayOutputStream(); padded.write(2); padded.write(new byte[]{0,0,0,0,10}); padded.write(fields); padded.write(new byte[]{0,0});
            frame(input, 1, 4 | 8 | 32, 1, padded.toByteArray()); frame(input, 0, 1, 1, answer);
            require(Arrays.equals(invoke(input.toByteArray(), new ByteArrayOutputStream()), answer));
        });
        test("maximum DNS body across frames", () -> {
            byte[] large = new byte[65535]; ByteArrayOutputStream input = preface();
            frame(input, 1, 4, 1, headers(false, ":status", "200", "content-type", "application/dns-message", "content-length", "65535"));
            for (int at = 0; at < large.length; at += 16384) frame(input, 0, at + 16384 >= large.length ? 1 : 0, 1, Arrays.copyOfRange(large, at, Math.min(large.length, at + 16384)));
            require(invoke(input.toByteArray(), new ByteArrayOutputStream()).length == 65535);
        });
        List<String[]> invalid = List.of(
            new String[]{":status","302","content-type","application/dns-message"},
            new String[]{":status","429","content-type","application/dns-message"},
            new String[]{":status","204","content-type","application/dns-message"},
            new String[]{":status","201","content-type","application/dns-message"},
            new String[]{":status","206","content-type","application/dns-message"},
            new String[]{":status","200","content-type","text/html"},
            new String[]{":status","200","content-type","application/dns-message","content-encoding","gzip"},
            new String[]{":status","200","content-type","application/dns-message","content-length","12","content-length","12"},
            new String[]{":status","200","content-type","application/dns-message","content-length","65536"},
            new String[]{":status","200","content-type","application/dns-message","content-length","-1"},
            new String[]{":status","200","content-type","application/dns-message","age","9223372036854775808"},
            new String[]{":status","200","content-type","application/dns-message","age","1","age","2"},
            new String[]{":status","200","content-type","application/dns-message","connection","close"},
            new String[]{":status","200","content-type","application/dns-message","transfer-encoding","chunked"},
            new String[]{":status","200","Content-Type","application/dns-message"},
            new String[]{":status","200","content-type","application/dns-message","x-own","line\r\nbreak"},
            new String[]{"content-type","application/dns-message",":status","200"},
            new String[]{":status","200",":status","200","content-type","application/dns-message"},
            new String[]{":method","GET",":status","200","content-type","application/dns-message"},
            new String[]{":status","101","content-type","application/dns-message"});
        for (int index = 0; index < invalid.size(); index++) {
            String[] bad = invalid.get(index); test("malformed response headers " + index, () -> {
                ByteArrayOutputStream input = preface(); frame(input, 1, 4, 1, headers(false, bad)); frame(input, 0, 1, 1, answer); rejected(input);
            });
        }
        test("response length mismatch and empty body", () -> {
            ByteArrayOutputStream input = preface(); frame(input, 1, 4, 1, headers(false, ":status", "200", "content-type", "application/dns-message", "content-length", "12")); frame(input, 0, 1, 1, answer); rejected(input);
            input = preface(); frame(input, 1, 5, 1, headers(false, ":status", "200", "content-type", "application/dns-message")); rejected(input);
        });
        test("body over limit", () -> {
            ByteArrayOutputStream input = preface(); frame(input, 1, 4, 1, headers(false, ":status", "200", "content-type", "application/dns-message"));
            for (int index = 0; index < 4; index++) frame(input, 0, index == 3 ? 1 : 0, 1, new byte[16384]); rejected(input);
        });
        test("header list expansion and dynamic table bounded before decode", () -> {
            ByteArrayOutputStream input = preface(); String repeated = "a".repeat(20000);
            byte[] fields = headers(true, ":status","200","content-type","application/dns-message","x-one",repeated,"x-two",repeated);
            frame(input, 1, 0, 1, Arrays.copyOfRange(fields, 0, 16000)); frame(input, 9, 4, 1, Arrays.copyOfRange(fields, 16000, fields.length)); rejected(input);
            // HPACK table update to 4097 (RFC 7541 integer), exceeds the declared 4096 ceiling.
            input = preface(); frame(input, 1, 4, 1, new byte[]{0x3f,(byte)0xe2,0x1f}); rejected(input);
            input = preface(); frame(input, 1, 4, 1, new byte[]{(byte)0x80}); rejected(input);
        });
        test("compressed headers and frame size bounded before allocation", () -> {
            ByteArrayOutputStream input = preface(); frame(input, 1, 0, 1, new byte[16384]); frame(input, 9, 0, 1, new byte[16384]); frame(input, 9, 4, 1, new byte[1]); rejected(input);
            input = preface(); frame(input, 99, 0, 0, new byte[16385]); rejected(input);
        });
        test("cumulative decoded headers bounded across informational responses", () -> {
            ByteArrayOutputStream input = preface();
            for (int at = 0; at < 4; at++) frame(input,1,4,1,headers(true,":status","103","x-bounded","a".repeat(17000)));
            response(input,answer); rejected(input);
        });
        test("bounded config URI and valid UTF-8 path", () -> {
            expectIllegal(() -> SecureDns.Config.fromJson(Map.of("mode","doh","provider","custom","doh_url","https://resolver.invalid/" + "a".repeat(8200),"bootstrap_ips",List.of("9.9.9.9"))));
            expectIllegal(() -> SecureDns.Config.fromJson(Map.of("mode","doh","provider","custom","doh_url","https://resolver.invalid/" + "界".repeat(2000),"bootstrap_ips",List.of("9.9.9.9"))));
            require(SecureDns.Config.fromJson(Map.of("mode","doh","provider","custom","doh_url","https://resolver.invalid/пример/日本語","bootstrap_ips",List.of("9.9.9.9"))).dohUrl().contains("%"));
        });
        test("frame flooding and exact read contract", () -> {
            ByteArrayOutputStream input = preface(); for (int index = 0; index < 256; index++) frame(input, 99, 0, 0, new byte[0]); rejected(input);
            expectIo(() -> Http2Dns.query(QUERY, URI_FIXTURE, new ByteArrayOutputStream(), length -> new byte[Math.max(0,length - 1)], () -> {}));
        });
        test("continuation ordering and wrong stream", () -> {
            for (int type : new int[]{0,4,6,1}) {
                ByteArrayOutputStream input = preface(); frame(input, 1, 0, 1, new byte[]{(byte)0x88}); frame(input, type, 0, type == 4 || type == 6 ? 0 : 1, new byte[0]); rejected(input);
            }
            ByteArrayOutputStream input = preface(); frame(input, 9, 4, 1, new byte[0]); rejected(input);
            input = preface(); frame(input, 1, 4, 3, new byte[]{(byte)0x88}); rejected(input);
        });
        test("invalid preface settings controls windows and reset", () -> {
            List<Frame> bad = List.of(new Frame(4,1,0,new byte[0]), new Frame(4,0,1,new byte[0]),
                new Frame(4,0,0,new byte[]{0}), new Frame(4,0,0,ByteBuffer.allocate(6).putShort((short)4).putInt(-1).array()),
                new Frame(4,0,0,ByteBuffer.allocate(6).putShort((short)5).putInt(10).array()),
                new Frame(4,0,0,ByteBuffer.allocate(6).putShort((short)2).putInt(0).array()),
                new Frame(6,0,1,new byte[8]),new Frame(6,0,0,new byte[7]),new Frame(8,0,0,new byte[4]),
                new Frame(8,0,0,ByteBuffer.allocate(4).putInt(0x7fffffff).array()),new Frame(3,0,1,new byte[4]),
                new Frame(5,4,1,new byte[4]), new Frame(7,0,0,new byte[8]),new Frame(2,0,1,new byte[]{0,0,0,1,0}));
            for (Frame malformed : bad) { ByteArrayOutputStream input = preface(); frame(input,malformed.type,malformed.flags,malformed.stream,malformed.data); rejected(input); }
            ByteArrayOutputStream input = new ByteArrayOutputStream(); frame(input,6,0,0,new byte[8]); rejected(input);
        });
        test("padding, DATA before HEADERS and trailer semantics", () -> {
            ByteArrayOutputStream input = preface(); frame(input,0,1,1,answer); rejected(input);
            input = preface(); frame(input,1,12,1,new byte[]{5,0}); rejected(input);
            input = preface(); frame(input,1,4,1,headers(false,":status","200","content-type","application/dns-message")); frame(input,0,9,1,new byte[]{3,0}); rejected(input);
            input = preface(); frame(input,1,4,1,headers(false,":status","200","content-type","application/dns-message")); frame(input,0,0,1,answer); frame(input,1,5,1,headers(false,"content-length","12")); rejected(input);
        });
        test("explicit cancellation before I/O and between frames", () -> {
            AtomicInteger calls = new AtomicInteger(); ByteArrayOutputStream sent = new ByteArrayOutputStream();
            expectIo(() -> Http2Dns.query(QUERY,URI_FIXTURE,sent, length -> { throw new AssertionError("read after cancellation"); }, () -> { throw new IOException("owned cancellation"); })); require(sent.size() == 0);
            ByteArrayOutputStream input = preface(); response(input,answer); ByteArrayInputStream bytes = new ByteArrayInputStream(input.toByteArray());
            expectIo(() -> Http2Dns.query(QUERY,URI_FIXTURE,new ByteArrayOutputStream(),length -> exact(bytes,length), () -> { if (calls.incrementAndGet() > 12) throw new IOException("owned cancellation"); })); require(calls.get() > 12);
        });
        test("idle stream response rejected and max request flow bounded", () -> {
            ByteArrayOutputStream input = new ByteArrayOutputStream(); frame(input,4,0,0,ByteBuffer.allocate(6).putShort((short)3).putInt(0).array());
            frame(input,1,4,1,headers(false,":status","200","content-type","application/dns-message")); rejected(input);
            input = preface(); response(input,answer); ByteArrayInputStream source = new ByteArrayInputStream(input.toByteArray()); ByteArrayOutputStream sent = new ByteArrayOutputStream();
            byte[] large = new byte[65535]; Http2Dns.query(large,URI_FIXTURE,sent,length -> exact(source,length),() -> {});
            List<Frame> data = frames(sent.toByteArray(),24).stream().filter(f -> f.type == 0).toList(); require(data.size() == 4 && data.get(3).flags == 1); require(data.stream().mapToInt(f -> f.data.length).sum() == 65535);
            expectIo(() -> Http2Dns.query(new byte[65536],URI_FIXTURE,new ByteArrayOutputStream(),length -> new byte[length],() -> {}));
        });
        test("HPACK Huffman invalid EOS and repeated header field count", () -> {
            ByteArrayOutputStream input = preface(); frame(input,1,4,1,new byte[]{0,1,'x',(byte)0x84,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff}); rejected(input);
            List<String> pairs = new ArrayList<>(List.of(":status","200","content-type","application/dns-message")); for (int at=0;at<128;at++) { pairs.add("x-small"); pairs.add("a"); }
            input = preface(); frame(input,1,4,1,headers(true,pairs.toArray(String[]::new))); rejected(input);
        });

        SSLContext trust = context(owned.resolve("trust.p12"), false), serverContext = context(owned.resolve("server.p12"), true);
        test("real TLS ALPN h2 verified SNI and selected numeric gateway", () -> {
            AtomicReference<String> negotiated = new AtomicReference<>();
            try (TlsServer server = new TlsServer(serverContext, socket -> {
                negotiated.set(socket.getApplicationProtocol()); byte[] request = readRequest(socket.getInputStream());
                byte[] reply = answer(request,60); ByteArrayOutputStream encoded = preface(); response(encoded,reply); socket.getOutputStream().write(encoded.toByteArray()); socket.getOutputStream().flush();
            }); SecureDns dns = secure(trust)) {
                List<String> routes = new ArrayList<>(); byte[] result = dns.query(config(server.port()),"192.168.1.3",QUERY,(gateway,address,port,timeout) -> {
                    routes.add(gateway + "|" + address.getHostAddress() + "|" + port); Socket socket = new Socket(); socket.connect(new InetSocketAddress("127.0.0.1",server.port()),timeout); return socket;
                },after(2500));
                require(negotiated.get().equals("h2")); DnsMessage.validateResponse(QUERY,result);
                require(routes.equals(List.of("192.168.1.3|9.9.9.9|" + server.port()))); server.await();
            }
        });
        test("all four selected gateways retain numeric h2 egress identity", () -> {
            for (int at=1;at<=4;at++) {
                String expected = "192.168.1." + at;
                try (TlsServer server = new TlsServer(serverContext,socket -> { byte[] request=readRequest(socket.getInputStream()); ByteArrayOutputStream encoded=new ByteArrayOutputStream(); response(encoded,answer(request,60)); socket.getOutputStream().write(encoded.toByteArray()); socket.getOutputStream().flush(); }); SecureDns dns=secure(trust)) {
                    List<String> actual=new ArrayList<>(); byte[] result=dns.query(config(server.port()),expected,QUERY,(gateway,address,port,timeout) -> { actual.add(gateway + "|" + address.getHostAddress()); Socket socket=new Socket(); socket.connect(new InetSocketAddress("127.0.0.1",server.port()),timeout); return socket; },after(2000));
                    DnsMessage.validateResponse(QUERY,result); require(actual.equals(List.of(expected+"|9.9.9.9"))); server.await();
                }
            }
        });
        test("real h2 deadline closes stalled TLS stream", () -> {
            try (TlsServer server = new TlsServer(serverContext,socket -> { readRequest(socket.getInputStream()); try { Thread.sleep(1000); } catch (InterruptedException ignored) {} }); SecureDns dns = secure(trust)) {
                long start = System.nanoTime(); expectIo(() -> dns.query(config(server.port()),"192.168.1.2",QUERY,connector(server.port()),after(180)));
                require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 900);
            }
        });
        test("real h2 cancellation and no OS lookup", () -> {
            CountDownLatch waiting = new CountDownLatch(1); AtomicReference<Throwable> failure = new AtomicReference<>();
            try (TlsServer server = new TlsServer(serverContext,socket -> { readRequest(socket.getInputStream()); waiting.countDown(); try { Thread.sleep(1000); } catch (InterruptedException ignored) {} }); SecureDns dns = secure(trust)) {
                Thread caller = new Thread(() -> { try { dns.query(config(server.port()),"192.168.1.1",QUERY,connector(server.port()),after(3000)); } catch (Throwable bad) { failure.set(bad); } },"owned-h2-cancel"); caller.start();
                require(waiting.await(2,TimeUnit.SECONDS)); dns.close(); caller.join(900); require(!caller.isAlive() && failure.get() instanceof IOException);
            }
        });
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status","PASS"); report.put("checks",checks); report.put("cases",cases.size());
        report.put("loopback_only",true); report.put("real_tls_h2_alpn",true); report.put("no_public_dns",true); report.put("no_os_fallback",true);
        report.put("installed_app_accessed",false); report.put("os_network_changed",false); report.put("unicode", "пример.рф 日本語"); report.put("case_names",cases);
        Files.writeString(owned.resolve("HTTP2-DNS-TESTS.json"),json(report) + "\n",StandardCharsets.UTF_8);
        System.out.println("HTTP2_DNS_TEST_OK checks=" + checks + " cases=" + cases.size() + " loopback_only=true real_tls_h2_alpn=true no_public_dns=true no_os_fallback=true");
    }

    private static byte[] invoke(byte[] bytes, ByteArrayOutputStream sent) throws IOException { ByteArrayInputStream in = new ByteArrayInputStream(bytes); return Http2Dns.query(QUERY,URI_FIXTURE,sent,length -> exact(in,length), () -> {}); }
    private static void rejected(ByteArrayOutputStream bytes) throws Exception { expectIo(() -> invoke(bytes.toByteArray(),new ByteArrayOutputStream())); }
    private static ByteArrayOutputStream preface() throws IOException { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); frame(bytes,4,0,0,new byte[0]); return bytes; }
    private static void response(ByteArrayOutputStream bytes, byte[] answer) throws IOException { frame(bytes,1,4,1,headers(true,":status","200","content-type","application/dns-message","content-length","" + answer.length)); frame(bytes,0,1,1,answer); }
    private static byte[] headers(boolean huffman,String... pairs) throws IOException {
        List<Header> headers = new ArrayList<>(); for (int at = 0; at < pairs.length; at += 2) headers.add(new BasicHeader(pairs[at],pairs[at + 1]));
        ByteArrayBuffer bytes = new ByteArrayBuffer(128); new HPackEncoder(4096,StandardCharsets.US_ASCII).encodeHeaders(bytes,headers,huffman); return Arrays.copyOf(bytes.array(),bytes.length());
    }
    private static void frame(ByteArrayOutputStream bytes,int type,int flags,int stream,byte[] data) throws IOException {
        DataOutputStream out = new DataOutputStream(bytes); out.writeByte(data.length >>> 16); out.writeShort(data.length); out.writeByte(type); out.writeByte(flags); out.writeInt(stream); out.write(data);
    }
    private record Frame(int type,int flags,int stream,byte[] data) {}
    private static List<Frame> frames(byte[] bytes,int skip) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes,skip,bytes.length - skip); List<Frame> result = new ArrayList<>();
        while (in.available() > 0) { byte[] head = exact(in,9); int length = ((head[0]&255)<<16)|((head[1]&255)<<8)|(head[2]&255); result.add(new Frame(head[3]&255,head[4]&255,ByteBuffer.wrap(head,5,4).getInt(),exact(in,length))); } return result;
    }
    private static void inspectRequest(byte[] sent,byte[] query) throws Exception {
        require(new String(sent,0,24,StandardCharsets.US_ASCII).equals("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n")); List<Frame> frames = frames(sent,24);
        require(frames.get(0).type == 4); require(frames.stream().anyMatch(f -> f.type == 4 && f.flags == 1));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream(), data = new ByteArrayOutputStream();
        for (Frame f : frames) { if (f.type == 1 || f.type == 9) compressed.write(f.data); if (f.type == 0) data.write(f.data); }
        HPackDecoder decoder = new HPackDecoder(4096,StandardCharsets.US_ASCII); decoder.setMaxListSize(32768); List<Header> fields = decoder.decodeHeaders(ByteBuffer.wrap(compressed.toByteArray()));
        require(fields.get(0).getName().equals(":method") && fields.get(0).getValue().equals("POST")); require(fields.stream().anyMatch(h -> h.getName().equals(":authority") && h.getValue().equals("resolver.invalid:8443")));
        require(fields.stream().anyMatch(h -> h.getName().equals(":path") && h.getValue().equals("/dns-query"))); require(Arrays.equals(data.toByteArray(),query));
    }
    private static byte[] readRequest(InputStream input) throws Exception {
        require(new String(exact(input,24),StandardCharsets.US_ASCII).equals("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n"));
        // The client waits for peer SETTINGS before sending a request; emit it in the TLS handler before reading the body.
        return readRequestBody(input);
    }
    private static byte[] readRequestBody(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int count = 0; count < 30; count++) { byte[] head = exact(input,9); int length = ((head[0]&255)<<16)|((head[1]&255)<<8)|(head[2]&255); if (length > 16384) throw new IOException("owned request size"); byte[] data = exact(input,length); if (head[3] == 0) { bytes.write(data); if ((head[4]&1) != 0) return bytes.toByteArray(); } }
        throw new IOException("owned request missing END_STREAM");
    }
    private static byte[] exact(InputStream input,int length) throws IOException { byte[] result = input.readNBytes(length); if (result.length != length) throw new EOFException("Owned frame truncated"); return result; }
    private static byte[] answer(byte[] query,long ttl) throws IOException { byte[] wire = DnsMessage.systemAnswer(query,new InetAddress[]{InetAddress.getByAddress(new byte[]{9,9,9,9})}); int at = wire.length - 10; ByteBuffer.wrap(wire,at,4).putInt((int)ttl); return wire; }
    private static long ttl(byte[] wire) { return Integer.toUnsignedLong(ByteBuffer.wrap(wire,wire.length-10,4).getInt()); }
    private static SecureDns secure(SSLContext trust) { return new SecureDns(trust,name -> { throw new AssertionError("OS DNS fallback"); }); }
    private static SecureDns.Config config(int port) { return SecureDns.Config.fromJson(Map.of("mode","doh","provider","custom","doh_url","https://resolver.invalid:" + port + "/dns-query","bootstrap_ips",List.of("9.9.9.9"))); }
    private static SecureDns.Connector connector(int port) { return (g,a,p,t) -> { Socket socket = new Socket(); socket.connect(new InetSocketAddress("127.0.0.1",port),t); return socket; }; }
    private static long after(long millis) { return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis); }
    private static SSLContext context(Path store,boolean server) throws Exception {
        KeyStore keys = KeyStore.getInstance("PKCS12"); char[] password = "owned-fixture-only".toCharArray(); try (InputStream in = Files.newInputStream(store)) { keys.load(in,password); }
        SSLContext result = SSLContext.getInstance("TLS"); if (server) { KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); factory.init(keys,password); result.init(factory.getKeyManagers(),null,null); }
        else { TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); factory.init(keys); result.init(null,factory.getTrustManagers(),null); } return result;
    }
    private static final class TlsServer implements AutoCloseable {
        final SSLServerSocket socket; final Thread worker; final AtomicReference<Throwable> failure = new AtomicReference<>(); volatile SSLSocket accepted;
        TlsServer(SSLContext context,Handler handler) throws IOException {
            socket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(0,8,InetAddress.getByAddress(new byte[]{127,0,0,1})); socket.setSoTimeout(4000);
            SSLParameters parameters = socket.getSSLParameters(); parameters.setApplicationProtocols(new String[]{"h2"}); socket.setSSLParameters(parameters);
            worker = new Thread(() -> { try (SSLSocket tls = (SSLSocket)socket.accept()) { accepted = tls; tls.setSoTimeout(3500); tls.startHandshake(); ByteArrayOutputStream settings = preface(); tls.getOutputStream().write(settings.toByteArray()); tls.getOutputStream().flush(); handler.run(tls); } catch (Throwable bad) { if (!socket.isClosed()) failure.set(bad); } },"owned-h2-server"); worker.setDaemon(true); worker.start();
        }
        int port() { return socket.getLocalPort(); }
        void await() throws Exception { worker.join(1500); require(!worker.isAlive()); if (failure.get() != null) throw new AssertionError("Owned TLS server failed",failure.get()); }
        @Override public void close() throws Exception { socket.close(); if (accepted != null) accepted.close(); worker.interrupt(); worker.join(1500); if (worker.isAlive()) throw new AssertionError("Owned TLS worker leaked"); }
    }
    private static void test(String name,Checked checked) throws Exception { try { checked.run(); cases.add(name); System.out.println("PASS " + name); } catch (Throwable bad) { throw new AssertionError(name,bad); } }
    private static void require(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static void expectIo(Checked checked) throws Exception { try { checked.run(); } catch (IOException expected) { checks++; return; } throw new AssertionError("Expected IOException"); }
    private static void expectIllegal(Checked checked) throws Exception { try { checked.run(); } catch (IllegalArgumentException expected) { checks++; return; } throw new AssertionError("Expected IllegalArgumentException"); }
    private static String json(Object value) { if (value instanceof Map<?,?> map) { List<String> parts = new ArrayList<>(); map.forEach((key,item) -> parts.add(json(key.toString()) + ":" + json(item))); return "{" + String.join(",",parts) + "}"; } if (value instanceof List<?> list) return "[" + String.join(",",list.stream().map(Http2DnsTest::json).toList()) + "]"; if (value instanceof String text) return "\"" + text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r") + "\""; return value.toString(); }
}
