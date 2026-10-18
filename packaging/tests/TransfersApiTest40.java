package multiroute;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Exercises the actual controller handler on an isolated ephemeral loopback port. */
public final class TransfersApiTest40 {
    static int checks;static final List<String> cases=new ArrayList<>();static String base;static HttpClient client;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);cases.add(name);}
    static HttpResponse<String> send(String method,String path,Map<String,Object> input,String origin)throws Exception{
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/json; charset=UTF-8");if(origin!=null)builder.header("Origin",origin);
        return client.send(builder.method(method,input==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(Json.stringify(input),StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    static Map<String,Object> api(String method,String path,Map<String,Object> input,int status)throws Exception{HttpResponse<String> response=send(method,path,input,null);check(response.statusCode()==status,method+" "+path+" expected "+status+" got "+response.statusCode()+" "+response.body());check(response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("utf-8"),"Explicit UTF-8 API response "+path);check(response.body().indexOf('\ufffd')<0,"No replacement characters "+path);return Main.obj(Json.parse(response.body()));}
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);SourceAuth.configure(own);AppLog.initialize(own);
        Main.Manager manager=new Main.Manager(own,(url,route)->{throw new IOException("No source network in API fixture");},path->Long.MAX_VALUE,(route,torrent)->{});
        manager.queueTimer.shutdownNow();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);ExecutorService threads=Executors.newCachedThreadPool();server.setExecutor(threads);server.createContext("/",exchange->Main.handle(exchange,manager,null,()->{}));server.start();base="http://127.0.0.1:"+server.getAddress().getPort();client=HttpClient.newBuilder().proxy(new ProxySelector(){public List<Proxy> select(URI uri){return List.of(Proxy.NO_PROXY);}public void connectFailed(URI uri,SocketAddress address,IOException error){}}).build();
        SourceTransfer.EndpointResolver previous=SourceTransfer.endpoints;SourceTransfer.endpoints=route->{throw new IOException("Owned fixture route is offline");};Transport.resetGatewayClients();
        try{
            check(Brand.NAME.equals(api("GET","/",null,200).get("application")),"New application identity is returned by API");
            Map<String,Object> profile=api("POST","/api/v1/site-profiles",Map.of("name","Сервер Япония 🚀","base_url","ftp://owned-api.invalid/каталог/","type","ftp","username","юзер","password","ПАРОЛЬ-СВОЙ","download_directory",own.resolve("Загрузки").toString(),"gateways",List.of("192.0.2.15")),200);String profileId=Main.str(profile.get("id"));check(profile.get("name").equals("Сервер Япония 🚀"),"Unicode profile name returned unchanged");check(!Json.stringify(profile).contains("ПАРОЛЬ-СВОЙ")&&!profile.containsKey("username"),"Profile save response has no credentials");
            Map<String,Object> listed=api("GET","/api/v1/site-profiles",null,200);check(Main.arr(listed.get("profiles")).size()==1&&!Json.stringify(listed).contains("ПАРОЛЬ-СВОЙ"),"Public site profile list contains no secrets");
            api("PUT","/api/v1/site-profiles",Map.of("id",profileId,"name","Изменённое имя 🚀"),200);
            Map<String,Object> original=new LinkedHashMap<>();original.put("url","файл.zip");original.put("site_profile_id",profileId);Map<String,Object> prepared=SiteProfiles.apply(original);check(Main.str(prepared.get("url")).contains("файл.zip")&&Main.arr(prepared.get("routes")).size()==1,"Relative download URL and saved gateways applied");check(!original.containsKey("routes"),"Profile request application does not mutate caller input");
            // Queue creation proves the saved profile survives normalization and persistence.
            prepared.put("filename","Файл 🚀.zip");prepared.put("start_after_ms",System.currentTimeMillis()+86400000L);prepared.put("size",17);Map<String,Object> download=api("POST","/api/v1/jobs",prepared,201);String downloadId=Main.str(download.get("id"));check(Main.obj(Main.arr(download.get("routes")).get(0)).get("site_profile_id").equals(profileId),"Download routes retain selected profile identity");
            api("GET","/api/v1/jobs/"+downloadId,null,200);
            Path file=own.resolve("Отправить_日本語_🚀.txt");Files.writeString(file,"Собственный UTF-8 текст\n",StandardCharsets.UTF_8);
            Map<String,Object> upload=api("POST","/api/v1/uploads",Map.of("local_path",file.toString(),"target_url","файл.txt","site_profile_id",profileId),201);String id=Main.str(upload.get("id"));check(upload.get("local_path").equals(file.toString()),"Upload API preserves full Unicode local path");check(Main.str(upload.get("target_url")).startsWith("ftp://owned-api.invalid/"),"Upload API resolves relative profile target URL");check(Boolean.FALSE.equals(upload.get("overwrite")),"Upload API defaults to no overwrite");check(Main.obj(Main.arr(upload.get("routes")).get(0)).get("site_profile_id").equals(profileId),"Upload selected gateway has profile auth identity");
            api("POST","/api/v1/uploads/"+id+"/pause",Map.of(),200);api("GET","/api/v1/uploads/"+id,null,200);check(Main.arr(api("GET","/api/v1/uploads",null,200).get("uploads")).size()==1,"Uploads collection contains new job");api("POST","/api/v1/uploads/"+id+"/resume",Map.of(),200);api("POST","/api/v1/uploads/"+id+"/pause",Map.of(),200);
            Map<String,Object> removed=api("DELETE","/api/v1/uploads/"+id,null,200);check(Boolean.TRUE.equals(removed.get("local_files_kept"))&&Files.readString(file,StandardCharsets.UTF_8).equals("Собственный UTF-8 текст\n"),"Removing upload keeps exact local source bytes");check(Main.arr(api("GET","/api/v1/uploads",null,200).get("uploads")).isEmpty(),"Upload removal persists in collection");
            api("POST","/api/v1/archives/preview",Map.of("url","http://owned-api.invalid/test.zip","route",Map.of("type","direct")),400);api("POST","/api/v1/remote/list",Map.of("url","ftp://owned-api.invalid/","route",Map.of("type","direct")),400);
            for(String path:List.of("/api/v1/site-profiles","/api/v1/uploads","/api/v1/archives/preview","/api/v1/remote/list"))check(send("POST",path,Map.of(),"https://foreign.invalid").statusCode()==403,"Foreign browser origin rejected before new API action "+path);
            api("DELETE","/api/v1/site-profiles/"+profileId,null,200);check(Main.arr(api("GET","/api/v1/site-profiles",null,200).get("profiles")).isEmpty(),"Profile removed from list");check(Files.isRegularFile(file),"Profile deletion keeps user transfer files");
            // Display replacement applies only to known templates, not filename/path arguments.
            String oldPath="C:/MultiRoute Resume/Файл.txt";String hover=I18n.tFor("en","Файл: %s",oldPath);check(hover.contains(oldPath),"Branding leaves a user-controlled old-name path intact");check(Brand.applicationJar("MultiRoute-Downloader.jar")&&Brand.applicationJar("MultiRoute-Resume.jar")&&!Brand.applicationJar("foreign.jar"),"Previous JAR compatibility remains narrowly bounded");
            Path legacy=own.resolve("legacy"),current=own.resolve("current");Files.createDirectories(legacy.resolve("data"));Files.writeString(legacy.resolve("data/own.config"),"Сохранённая настройка",StandardCharsets.UTF_8);check(Brand.savedProfile(current,legacy).equals(legacy),"Upgrade reuses an existing legacy profile in place");Files.createDirectories(current.resolve("data"));check(Brand.savedProfile(current,legacy).equals(current),"Existing current profile takes priority");check(Files.readString(legacy.resolve("data/own.config"),StandardCharsets.UTF_8).equals("Сохранённая настройка"),"Brand change preserves exact prior UTF-8 settings");
            Path relocated=own.resolve("relocated-legacy"),fresh=own.resolve("fresh-current");Files.createDirectories(relocated);Files.writeString(relocated.resolve("data-location.config"),"Сохранённые данные",StandardCharsets.UTF_8);check(Brand.savedProfile(fresh,relocated).equals(relocated),"Upgrade preserves a legacy relocated data pointer without local data directory");Files.createDirectories(fresh);Files.writeString(fresh.resolve("data-location.config"),"Current pointer",StandardCharsets.UTF_8);check(Brand.savedProfile(fresh,relocated).equals(fresh),"A current relocated data pointer takes priority");
        }finally{manager.shutdown();server.stop(0);threads.shutdownNow();SourceTransfer.endpoints=previous;Transport.resetGatewayClients();}
        Map<String,Object> report=Map.of("status","PASS","checks",checks,"cases",cases,"actual_controller_handler",true,"external_network",false,"user_profile_read",false,"boundary","API wiring and UTF-8 verified here; real file transfers verified separately by protocol fixtures");Files.writeString(own.resolve("TRANSFERS-API-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println("PASS "+checks+" transfer API and upgrade checks");
    }
}
