package multiroute;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real ephemeral Main.handle HTTP requests and an owned browser-harness receiver; no download network. */
public final class BrowserBridgeTest40 {
    private static final String STATUS = "/api/v1/browser/status", DOWNLOAD = "/api/v1/browser/download";
    private static final String URL = "https://owned-browser.invalid/Каталог/Файл_日本語_é_🚀.7z?signature=own-value";
    private static final String FIXED_ORIGIN = "chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl";
    private static final List<String> cases = new ArrayList<>();
    private static int checks;
    private static final AtomicInteger sourceCalls = new AtomicInteger();
    private static void check(boolean valid, String name) { checks++; if (!valid) throw new AssertionError(name); cases.add(name); }
    private record Response(int status, Map<String,List<String>> headers, String body) {
        String header(String name) { return headers.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry->entry.getValue().stream()).findFirst().orElse(""); }
        Map<String,Object> json() { return Main.obj(Json.parse(body)); }
    }
    private static final class Harness implements AutoCloseable {
        final Main.Manager manager;
        final HttpServer server;
        final ExecutorService threads = Executors.newCachedThreadPool(task->{Thread thread=new Thread(task,"owned-browser-api");thread.setDaemon(true);return thread;});
        final HttpClient client;
        final int port;
        Harness(Path root) throws Exception {
            SourceAuth.configure(root); AppLog.initialize(root);
            manager = new Main.Manager(root, (url, route)->{sourceCalls.incrementAndGet();throw new AssertionError("No source lookup in browser bridge fixture");},
                    path->Long.MAX_VALUE,(route,torrent)->{});manager.queueTimer.shutdownNow();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(threads);
            server.createContext("/",exchange->Main.handle(exchange,manager,null,()->{}));server.start();port=server.getAddress().getPort();
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).proxy(new ProxySelector(){
                public List<Proxy> select(URI uri){return List.of(Proxy.NO_PROXY);}
                public void connectFailed(URI uri,SocketAddress address,IOException error){}
            }).build();
        }
        Response send(String method,String path,String origin,String body,Map<String,String> extra) throws Exception {
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(5));
            if(origin!=null)builder.header("Origin",origin);
            for(var header:extra.entrySet())builder.header(header.getKey(),header.getValue());
            if(body!=null)builder.header("Content-Type","application/json; charset=UTF-8");
            HttpResponse<byte[]> response=client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofByteArray());
            String text=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(response.body())).toString();
            return new Response(response.statusCode(),response.headers().map(),text);
        }
        Response json(String method,String path,String origin,Map<String,Object> input) throws Exception {
            return send(method,path,origin,input==null?null:Json.stringify(input),Map.of());
        }
        void expect(int status,Response response,String name) {
            check(response.status==status,name+" (expected "+status+", got "+response.status+" "+response.body+")");
            if(status!=204)check(response.header("Content-Type").toLowerCase(Locale.ROOT).contains("utf-8")&&response.body.indexOf('\ufffd')<0,
                    "HTTP bridge response is explicit strict UTF-8: "+name);
        }
        int foreignHost() throws Exception {
            try(Socket socket=new Socket("127.0.0.1",port)) {
                socket.setSoTimeout(5000);
                String text="GET "+STATUS+" HTTP/1.1\r\nHost: foreign-owned.invalid:"+port+"\r\nOrigin: "+FIXED_ORIGIN+"\r\nConnection: close\r\n\r\n";
                socket.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));socket.getOutputStream().flush();
                String line=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)).readLine();
                return Integer.parseInt(line.split(" ")[1]);
            }
        }
        public void close(){manager.shutdown();server.stop(0);threads.shutdownNow();AppLog.close();}
    }
    private static Path root(String text) throws Exception {
        Path path=Path.of(text).toAbsolutePath().normalize();Files.createDirectories(path);return path.toRealPath();
    }
    private static void writeUrls(Path path,List<String> urls) throws IOException {
        Path pending=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(pending,Json.stringify(urls)+"\n",StandardCharsets.UTF_8);
        try{Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException unsupported){Files.move(pending,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    private static void serve(Path own) throws Exception {
        try(Harness harness=new Harness(own.resolve("profile"))) {
            harness.manager.browser.enable(true);List<String> urls=new ArrayList<>();Path received=own.resolve("received-urls.json");writeUrls(received,urls);
            harness.manager.browserDownload=url->{synchronized(urls){urls.add(url);try{writeUrls(received,urls);}catch(IOException error){throw new UncheckedIOException(error);}}};
            Main.save(own.resolve("server-info.json"),Map.of("port",harness.port,"pid",ProcessHandle.current().pid(),"origin",FIXED_ORIGIN,"received_file",received.toString()));
            System.out.println("OWNED_BROWSER_BRIDGE_READY "+harness.port);System.out.flush();
            long deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(5);
            while(!Files.exists(own.resolve("STOP"),LinkOption.NOFOLLOW_LINKS)&&System.nanoTime()<deadline)Thread.sleep(100);
        }
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        if(args.length==2&&args[0].equals("--serve")){serve(root(args[1]));return;}
        Path own=root(args[0]),profile=own.resolve("profile");List<String> received=new CopyOnWriteArrayList<>();
        try(Harness harness=new Harness(profile)) {
            check(!harness.manager.browser.enabled()&&!Files.exists(profile.resolve("browser-integration.config")),"Browser bridge is disabled by default without an implicit saved opt-in");
            harness.expect(403,harness.json("GET",STATUS,FIXED_ORIGIN,null),"Disabled bridge rejects fixed extension status");
            harness.expect(403,harness.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",URL)),"Disabled bridge rejects downloads before any callback");
            harness.expect(403,harness.send("OPTIONS",DOWNLOAD,FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","POST","Access-Control-Request-Headers","content-type")),"Disabled bridge rejects preflight");
            harness.manager.browser.enable(true);
            check(Boolean.TRUE.equals(Main.obj(Json.parse(Files.readString(profile.resolve("browser-integration.config"),StandardCharsets.UTF_8))).get("enabled")),"Explicit enable persists a UTF-8 opt-in setting");
            for(String origin:List.of("https://foreign-owned.invalid","http://127.0.0.1:"+harness.port,"chrome-extension://aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",FIXED_ORIGIN+"/","null")) {
                Response rejected=harness.json("GET",STATUS,origin,null);harness.expect(403,rejected,"Only exact fixed extension origin is allowed: "+origin);
                check(rejected.header("Access-Control-Allow-Origin").isEmpty(),"Rejected foreign origin receives no CORS grant");
            }
            harness.expect(403,harness.json("GET",STATUS,null,null),"Missing origin cannot use the opt-in browser bridge");
            check(harness.foreignHost()==403,"Foreign Host is rejected before browser-origin handling");
            Response preflight=harness.send("OPTIONS",DOWNLOAD,FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","POST","Access-Control-Request-Headers","Content-Type","Access-Control-Request-Private-Network","true"));
            harness.expect(204,preflight,"Exact origin and content-type preflight succeeds without dispatching a URL");
            check(FIXED_ORIGIN.equals(preflight.header("Access-Control-Allow-Origin"))&&preflight.header("Vary").equalsIgnoreCase("Origin"),"Preflight grants only the fixed extension origin and varies by origin");
            check(preflight.header("Access-Control-Allow-Headers").equalsIgnoreCase("Content-Type")&&!preflight.header("Access-Control-Allow-Headers").contains("*"),"Preflight permits only content-type rather than arbitrary API headers");
            check(preflight.header("Access-Control-Allow-Private-Network").equals("true"),"Explicit private-network preflight receives the scoped permission");
            harness.expect(403,harness.send("OPTIONS",DOWNLOAD,FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","DELETE")),"Preflight rejects a destructive method");
            harness.expect(403,harness.send("OPTIONS",DOWNLOAD,FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","POST","Access-Control-Request-Headers","content-type, x-api-key")),"Preflight rejects unrelated API headers");
            harness.expect(403,harness.send("OPTIONS",DOWNLOAD,FIXED_ORIGIN,null,Map.of()),"Preflight requires an explicit permitted method");
            harness.expect(404,harness.send("OPTIONS","/api/v1/browser/unknown",FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","POST")),"Preflight scope excludes unknown browser endpoints");
            harness.expect(403,harness.send("OPTIONS","/api/v1/jobs",FIXED_ORIGIN,null,Map.of("Access-Control-Request-Method","POST")),"Browser origin cannot preflight the ordinary jobs API");
            Response notReady=harness.json("GET",STATUS,FIXED_ORIGIN,null);harness.expect(200,notReady,"Enabled headless bridge exposes status");
            check(Boolean.FALSE.equals(notReady.json().get("ready"))&&Brand.NAME.equals(notReady.json().get("application")),"Status distinguishes an absent desktop receiver and displays the new application identity");
            harness.expect(503,harness.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",URL)),"Headless bridge never accepts a download with no desktop receiver");
            harness.manager.browserDownload=received::add;
            Response ready=harness.json("GET",STATUS,FIXED_ORIGIN,null);harness.expect(200,ready,"Attached desktop callback exposes ready status");check(Boolean.TRUE.equals(ready.json().get("ready")),"Status becomes ready only after a receiver is attached");
            Response accepted=harness.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",URL));harness.expect(202,accepted,"Valid download is sent to the desktop callback");
            check(Boolean.TRUE.equals(accepted.json().get("accepted"))&&received.equals(List.of(URL)),"Callback receives the exact full Unicode URL once without changing encoded user data");
            String magnet="magnet:?xt=urn:btih:0123456789012345678901234567890123456789&dn=Свой_日本語_🚀";
            harness.expect(202,harness.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",magnet)),"Explicit magnet link is passed to desktop input");check(received.get(1).equals(magnet),"Magnet Unicode display name survives HTTP JSON and callback unchanged");
            int before=received.size();
            for(String invalid:List.of("file:///owned/private.bin","javascript:alert(1)","https://user:password@owned-browser.invalid/file.bin","https://owned-browser.invalid/file.bin#fragment","magnet:?xt=own#fragment","https://owned-browser.invalid/line\nbreak"))
                harness.expect(400,harness.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",invalid)),"Invalid or credential-bearing URL cannot reach the callback");
            for(Map<String,Object> invalid:List.<Map<String,Object>>of(Map.of("url",URL,"api_key","own"),Map.of("url",URL,"directory",own.resolve("outside-browser-choice").toString()),Map.of("url",URL,"path","../../owned-target"),Map.of("url",17),Map.of()))
                harness.expect(400,harness.json("POST",DOWNLOAD,FIXED_ORIGIN,invalid),"Browser bridge accepts only one textual URL field");
            harness.expect(400,harness.send("POST",DOWNLOAD,FIXED_ORIGIN,"{broken",Map.of()),"Malformed JSON cannot reach the callback");
            for(String method:List.of("GET","PUT","DELETE"))harness.expect(405,harness.json(method,DOWNLOAD,FIXED_ORIGIN,null),"Download endpoint rejects unrelated method "+method);
            harness.expect(405,harness.json("POST",STATUS,FIXED_ORIGIN,Map.of()),"Status endpoint rejects download-style POST");
            check(received.size()==before,"Every rejected request leaves the desktop receiver untouched");
            for(String path:List.of("/api/v1/jobs","/api/v1/queue","/api/v1/site-profiles","/api/v1/uploads","/api/v1/health"))
                harness.expect(403,harness.json("GET",path,FIXED_ORIGIN,null),"Extension origin remains blocked from ordinary application API: "+path);
            check(harness.manager.jobs.isEmpty()&&Main.arr(harness.manager.uploads.state().get("uploads")).isEmpty(),"Bridge acceptance never silently creates direct-route jobs or upload tasks");
        }
        try(Harness restarted=new Harness(profile)) {
            check(restarted.manager.browser.enabled(),"Saved explicit browser opt-in survives a real Manager restart");
            Response status=restarted.json("GET",STATUS,FIXED_ORIGIN,null);restarted.expect(200,status,"Restarted bridge responds to the fixed extension");check(Boolean.FALSE.equals(status.json().get("ready")),"Restart does not fabricate a desktop receiver");
            restarted.manager.browser.enable(false);
        }
        try(Harness disabled=new Harness(profile)){check(!disabled.manager.browser.enabled(),"Explicit bridge disable also survives restart");disabled.expect(403,disabled.json("POST",DOWNLOAD,FIXED_ORIGIN,Map.of("url",URL)),"Persisted disable rejects download after restart");}
        check(sourceCalls.get()==0,"All real HTTP bridge checks make zero source, router or downloader network calls");
        writeUrls(own.resolve("received-urls.json"),received);check(Files.readString(own.resolve("received-urls.json"),StandardCharsets.UTF_8).contains("Файл_日本語_é_🚀")&&!Files.readString(own.resolve("received-urls.json"),StandardCharsets.UTF_8).contains("\ufffd"),"Owned receiver output file preserves the exact Unicode URL in UTF-8");
        Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("version",Main.VERSION);report.put("checks",checks);report.put("cases",cases);
        report.put("source_network_calls",sourceCalls.get());report.put("external_network_calls",0);report.put("actual_loopback_http",true);report.put("user_data_read",false);report.put("download_jobs_created",0);
        Files.writeString(own.resolve("BROWSER-BRIDGE-TESTS.json"),Json.stringify(report)+"\n",StandardCharsets.UTF_8);System.out.println("PASS "+checks+" browser bridge checks");
    }
}
