package multiroute;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.sevenz.*;

/** Real HTTP range/proxy requests to owned generated archives. No user files or external servers. */
public final class ArchivePreviewTest40 {
    static int checks;static final List<String> cases=new ArrayList<>();
    static final String NAME="Каталог/Файл_日本語_é_🚀.txt";
    static final Map<String,Object> ROUTE=Map.of("type","gateway","gateway","192.0.2.10","enabled",true);
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);cases.add(name);}
    interface Task{void run()throws Exception;}
    static void rejects(Task task,String text)throws Exception{try{task.run();throw new AssertionError("Expected rejection: "+text);}catch(IOException expected){check(expected.getMessage().contains(text),"Rejected "+text+": "+expected.getMessage());}}
    static byte[] zip()throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream out=new ZipOutputStream(bytes,StandardCharsets.UTF_8)){ZipEntry entry=new ZipEntry(NAME);out.putNextEntry(entry);out.write("Собственный UTF-8 текст 🚀".getBytes(StandardCharsets.UTF_8));out.closeEntry();out.putNextEntry(new ZipEntry("../literal-not-extracted.txt"));out.write(new byte[]{42});out.closeEntry();}return bytes.toByteArray();}
    static byte[] tar(long size)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(TarArchiveOutputStream out=new TarArchiveOutputStream(bytes,StandardCharsets.UTF_8.name())){out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);out.setAddPaxHeadersForNonAsciiNames(true);TarArchiveEntry entry=new TarArchiveEntry(NAME);entry.setSize(size);out.putArchiveEntry(entry);byte[] block=new byte[65536];while(size>0){int count=(int)Math.min(size,block.length);out.write(block,0,count);size-=count;}out.closeArchiveEntry();}return bytes.toByteArray();}
    static byte[] seven(Path own)throws Exception{Path file=own.resolve("owned.7z");try(SevenZOutputFile out=new SevenZOutputFile(file.toFile())){SevenZArchiveEntry entry=new SevenZArchiveEntry();entry.setName(NAME);entry.setSize(3);out.putArchiveEntry(entry);out.write(new byte[]{1,2,3});out.closeArchiveEntry();}return Files.readAllBytes(file);}
    static void le(ByteArrayOutputStream out,long value,int count){for(int i=0;i<count;i++)out.write((int)(value>>(8*i))&255);}
    static void vint(ByteArrayOutputStream out,long value){while(value>=128){out.write((int)(value&127)|128);value>>>=7;}out.write((int)value);}
    static byte[] block5(int type,int flags,byte[] body){ByteArrayOutputStream content=new ByteArrayOutputStream();vint(content,type);vint(content,flags);content.writeBytes(body);ByteArrayOutputStream sized=new ByteArrayOutputStream();vint(sized,content.size());sized.writeBytes(content.toByteArray());CRC32 crc=new CRC32();crc.update(sized.toByteArray());ByteArrayOutputStream block=new ByteArrayOutputStream();le(block,crc.getValue(),4);block.writeBytes(sized.toByteArray());return block.toByteArray();}
    static byte[] rar5(boolean encrypted){ByteArrayOutputStream out=new ByteArrayOutputStream();out.writeBytes(new byte[]{'R','a','r','!',26,7,1,0});out.writeBytes(block5(1,0,new byte[]{0}));if(encrypted){out.writeBytes(block5(4,0,new byte[]{0,0}));return out.toByteArray();}ByteArrayOutputStream file=new ByteArrayOutputStream();vint(file,0);vint(file,7);vint(file,0);vint(file,0);vint(file,1);byte[] name=NAME.getBytes(StandardCharsets.UTF_8);vint(file,name.length);file.writeBytes(name);out.writeBytes(block5(2,0,file.toByteArray()));out.writeBytes(block5(5,0,new byte[]{0}));return out.toByteArray();}
    static byte[] block4(int type,int flags,byte[] body){ByteArrayOutputStream header=new ByteArrayOutputStream();header.write(type);le(header,flags,2);le(header,7+body.length,2);header.writeBytes(body);CRC32 crc=new CRC32();crc.update(header.toByteArray());ByteArrayOutputStream out=new ByteArrayOutputStream();le(out,crc.getValue()&65535,2);out.writeBytes(header.toByteArray());return out.toByteArray();}
    static byte[] rar4(){ByteArrayOutputStream out=new ByteArrayOutputStream();out.writeBytes(new byte[]{'R','a','r','!',26,7,0});out.writeBytes(block4(0x73,0,new byte[6]));ByteArrayOutputStream body=new ByteArrayOutputStream();le(body,0,4);le(body,7,4);body.write(3);le(body,0,4);le(body,0,4);body.write(20);body.write(0x30);byte[] name=NAME.getBytes(StandardCharsets.UTF_8);le(body,name.length,2);le(body,0,4);body.writeBytes(name);out.writeBytes(block4(0x74,0x8000,body.toByteArray()));out.writeBytes(block4(0x7b,0,new byte[0]));return out.toByteArray();}
    record Response(URI uri,InputStream body,java.net.http.HttpHeaders headers) implements java.net.http.HttpResponse<InputStream>{public int statusCode(){return 206;}public java.net.http.HttpRequest request(){return java.net.http.HttpRequest.newBuilder(uri).build();}public Optional<java.net.http.HttpResponse<InputStream>> previousResponse(){return Optional.empty();}public Optional<javax.net.ssl.SSLSession> sslSession(){return Optional.empty();}public java.net.http.HttpClient.Version version(){return java.net.http.HttpClient.Version.HTTP_1_1;}}
    static void budget()throws Exception{
        ArchivePreview.Opener previous=ArchivePreview.opener;AtomicInteger opened=new AtomicInteger(),closed=new AtomicInteger();AtomicLong actual=new AtomicLong();
        ArchivePreview.opener=(route,url,from,to,etag,modified)->{opened.incrementAndGet();InputStream body=new ByteArrayInputStream(new byte[(int)(to-from+1)]){public synchronized int read(byte[] bytes,int off,int len){int n=super.read(bytes,off,len);if(n>0)actual.addAndGet(n);return n;}public synchronized int read(){int value=super.read();if(value>=0)actual.incrementAndGet();return value;}public void close(){closed.incrementAndGet();}};return new Response(URI.create(url),body,java.net.http.HttpHeaders.of(Map.of("ETag",List.of("\"own-budget\""),"Content-Range",List.of("bytes "+from+"-"+to+"/33554432"),"Content-Length",List.of(Long.toString(to-from+1))),(a,b)->true));};
        try(ArchivePreview.RemoteChannel channel=new ArchivePreview.RemoteChannel(ROUTE,"http://owned-budget.invalid/large.bin")){
            java.nio.ByteBuffer bytes=java.nio.ByteBuffer.allocate(65536);boolean stopped=false;try{for(int i=0;i<600;i++){bytes.clear();channel.read(bytes);}}catch(ArchivePreview.Limit limit){stopped=true;}
            check(stopped&&actual.get()<=ArchivePreview.BUDGET&&actual.get()>ArchivePreview.BUDGET-ArchivePreview.BLOCK,"Actual range bytes stop at the 16 MiB budget");check(channel.fetched==actual.get(),"Budget accounts for probe and every body byte");
            Thread.currentThread().interrupt();try{channel.position(0);throw new AssertionError("Interrupted preview continued");}catch(InterruptedIOException expected){check(Thread.currentThread().isInterrupted(),"Cancellation preserves thread interruption");}finally{Thread.interrupted();}
        }finally{ArchivePreview.opener=previous;}
        check(opened.get()==closed.get(),"All preview range bodies close on budget and cancellation");
    }
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);SourceAuth.configure(own);SiteProfiles.configure(own);
        byte[] zip=zip(),tar=tar(1024*1024),seven=seven(own);Map<String,byte[]> files=new ConcurrentHashMap<>();files.put("/own.zip",zip);files.put("/own.tar",tar);files.put("/own.7z",seven);files.put("/own.rar4",rar4());files.put("/own.rar5",rar5(false));files.put("/encrypted.rar",rar5(true));files.put("/bad.rar",rar5(false).clone());files.get("/bad.rar")[12]^=1;
        AtomicLong fetched=new AtomicLong();AtomicInteger requests=new AtomicInteger();List<String> authorizations=new CopyOnWriteArrayList<>();
        HttpServer proxy=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);ExecutorService serverThreads=Executors.newCachedThreadPool();proxy.setExecutor(serverThreads);
        proxy.createContext("/",exchange->{try{
            String path=exchange.getRequestURI().getPath();byte[] data=files.get(path);if(data==null)data=zip;authorizations.add(Objects.toString(exchange.getRequestHeaders().getFirst("Authorization"),""));
            String range=exchange.getRequestHeaders().getFirst("Range");if(range==null)throw new IOException("No range");String[] parts=range.substring(6).split("-");int from=Integer.parseInt(parts[0]),to=Integer.parseInt(parts[1]);requests.incrementAndGet();
            if(!path.equals("/no-identity.zip"))exchange.getResponseHeaders().set("ETag",path.equals("/changed.zip")&&from!=0?"\"changed\"":"\"owned-stable\"");
            exchange.getResponseHeaders().set("Content-Type","application/octet-stream");exchange.getResponseHeaders().set("Content-Range","bytes "+from+"-"+to+"/"+data.length);
            exchange.sendResponseHeaders(path.equals("/no-range.zip")?200:206,to-from+1);exchange.getResponseBody().write(data,from,to-from+1);fetched.addAndGet(to-from+1);
        }catch(Exception failure){/* A rejected metadata request may close the body immediately. */}finally{exchange.close();}});proxy.start();
        SourceTransfer.EndpointResolver previous=SourceTransfer.endpoints;SourceTransfer.endpoints=route->{check(route.get("gateway").equals("192.0.2.10"),"Selected gateway retained for every real archive request");return URI.create("http://127.0.0.1:"+proxy.getAddress().getPort());};Transport.resetGatewayClients();
        try{
            for(String suffix:List.of("zip","tar","7z","rar4","rar5")){
                long before=fetched.get();Map<String,Object> result=ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/own."+suffix,"route",ROUTE));List<Object> entries=Main.arr(result.get("entries"));
                check(!Boolean.TRUE.equals(result.get("truncated")),"Complete "+suffix+" catalog");check(Main.obj(entries.get(0)).get("name").equals(NAME),"Unicode filename roundtrip "+suffix);check(result.get("format").toString().toLowerCase(Locale.ROOT).startsWith(suffix.equals("7z")?"7z":suffix),"Detected format "+suffix);check(Main.num(result.get("bytes_fetched"),0)==fetched.get()-before,"Actual transferred byte counter "+suffix);check(!Files.exists(own.resolve("literal-not-extracted.txt")),"No extracted paths "+suffix);
                if(suffix.equals("tar"))check(Main.num(result.get("bytes_fetched"),0)<100000,"TAR payload skipped through seek rather than downloading 1 MiB body");
                Files.writeString(own.resolve("listing-"+suffix+".json"),Json.stringify(result),StandardCharsets.UTF_8);
            }
            Map<String,Object> encrypted=ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/encrypted.rar","route",ROUTE));check(Boolean.TRUE.equals(encrypted.get("password_required"))&&Boolean.TRUE.equals(encrypted.get("truncated")),"Encrypted RAR explicitly reports unsupported header listing");
            char[] transientPassword="Собственный пароль 🚀".toCharArray();Map<String,Object> withPassword=ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/own.zip","route",ROUTE,"password",transientPassword));check(!Main.arr(withPassword.get("entries")).isEmpty(),"Desktop char-array password accepted without JSON serialization");check(Arrays.equals(transientPassword,"Собственный пароль 🚀".toCharArray())&&!Json.stringify(withPassword).contains("пароль"),"Preview clears its own password copy and never returns secret");Arrays.fill(transientPassword,'\0');
            rejects(()->ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/bad.rar","route",ROUTE)),"checksum");
            rejects(()->ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/no-range.zip","route",ROUTE)),"206");
            rejects(()->ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/no-identity.zip","route",ROUTE)),"ETag or Last-Modified");
            rejects(()->ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/own.zip","route",Map.of("type","direct"))),"selected application gateway");
            Map<String,Object> profile=SiteProfiles.save(Map.of("name","Сайт 🚀","base_url","http://archive-owned.invalid/","type","basic","username","тест","password","пароль"));
            authorizations.clear();Map<String,Object> profiled=ArchivePreview.inspect(Map.of("url","own.zip","route",ROUTE,"site_profile_id",profile.get("id")));String expected="Basic "+Base64.getEncoder().encodeToString("тест:пароль".getBytes(StandardCharsets.UTF_8));check(!authorizations.isEmpty()&&authorizations.stream().allMatch(expected::equals),"Profile credentials reach all real proxy ranges as UTF-8 Basic");check(!Json.stringify(profiled).contains("пароль"),"Archive response contains no secret");
            // A large advertised directory is rejected before object allocation or entry payload requests.
            byte[] count=zip.clone();for(int i=count.length-22;i>=0;i--)if((count[i]&255)==0x50&&(count[i+1]&255)==0x4b&&(count[i+2]&255)==5&&(count[i+3]&255)==6){count[i+10]=0x11;count[i+11]=0x27;break;}files.put("/many.zip",count);Map<String,Object> limited=ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/many.zip","route",ROUTE));check(Boolean.TRUE.equals(limited.get("truncated"))&&Main.arr(limited.get("entries")).isEmpty(),"ZIP object allocation bounded before parsing excessive entries");
            // Representation changes must abort even if lengths still match.
            byte[] largeZip=new byte[zip.length+50000];System.arraycopy(zip,0,largeZip,0,zip.length);files.put("/changed.zip",largeZip);rejects(()->ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid/changed.zip","route",ROUTE)),"ETag");
            Path json=own.resolve("listing-zip.json");check(Files.readString(json,StandardCharsets.UTF_8).contains(NAME),"UTF-8 output file preserves literal Cyrillic and Unicode");
            if(args.length>1){Path fixtures=Path.of(args[1]);for(Object raw:Main.arr(Json.parse(Files.readString(fixtures.resolve("sources.json"),StandardCharsets.UTF_8)))){Map<String,Object> source=Main.obj(raw);String name=Main.str(source.get("file"));byte[] data=Files.readAllBytes(fixtures.resolve(name));check(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)).equals(source.get("sha256")),"Pinned upstream fixture "+name);String path="/upstream-"+files.size()+".rar";files.put(path,data);Map<String,Object> result=ArchivePreview.inspect(Map.of("url","http://archive-owned.invalid"+path,"route",ROUTE));boolean encryptedHeaders=name.contains("encrypted-junrar");check(encryptedHeaders?Boolean.TRUE.equals(result.get("password_required")):!Main.arr(result.get("entries")).isEmpty(),"Real upstream RAR directory "+name);Files.writeString(own.resolve("upstream-"+path.substring(1)+".json"),Json.stringify(result),StandardCharsets.UTF_8);}}
        }finally{SourceTransfer.endpoints=previous;Transport.resetGatewayClients();proxy.stop(0);serverThreads.shutdownNow();}
        budget();Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("checks",checks);report.put("cases",cases);report.put("real_proxy_requests",requests.get());report.put("user_files_read",false);report.put("external_network",false);report.put("boundaries",List.of("No physical router path validation","No archive extraction","No encrypted RAR header decryption"));Files.writeString(own.resolve("ARCHIVE-PREVIEW-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println("PASS "+checks+" archive preview checks");
    }
}
