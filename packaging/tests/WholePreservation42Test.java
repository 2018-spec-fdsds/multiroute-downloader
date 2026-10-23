package multiroute;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Owned UTF-8 payloads and loopback proxy fixtures; never contacts a publisher or native helper. */
public final class WholePreservation42Test {
    static final int MIB=1024*1024;
    static final byte[] DATA=new byte[8*MIB+17];
    static final String ETAG="\"owned-preservation42\"", HASH;
    static Path root; static int checks, ids;
    static {new Random(42).nextBytes(DATA);HASH=Main.hex(Main.sha().digest(DATA));}
    static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    static void waitFor(java.util.function.BooleanSupplier value,long ms,String text)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(ms);
        while(!value.getAsBoolean()&&System.nanoTime()<deadline)Thread.sleep(20);
        check(value.getAsBoolean(),text);
    }
    static final class Origin implements AutoCloseable {
        final HttpServer server; final ExecutorService workers;
        final String mode; final AtomicInteger wholeGets=new AtomicInteger(),nonzeroProbes=new AtomicInteger();
        final AtomicInteger ignoredPositiveResponses=new AtomicInteger(),successfulPositiveResponses=new AtomicInteger();
        final AtomicLong ignoredAt=new AtomicLong(),recoveredAt=new AtomicLong();
        final List<Long> ignoredStarts=new CopyOnWriteArrayList<>();
        final List<Long> segmentStarts=new CopyOnWriteArrayList<>();
        Origin(String mode)throws Exception {
            this.mode=mode;server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            workers=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"owned-preservation42");t.setDaemon(true);return t;});
            server.setExecutor(workers);server.createContext("/",this::send);server.start();
        }
        void send(HttpExchange exchange)throws IOException {
            try {
                if(!exchange.getRequestURI().getPath().equals("/file.bin")){exchange.sendResponseHeaders(404,-1);return;}
                String range=exchange.getRequestHeaders().getFirst("Range");long start=0,end=DATA.length-1;
                if(range!=null){String[] bounds=range.substring(6).split("-");start=Long.parseLong(bounds[0]);end=Long.parseLong(bounds[1]);if(start==1&&end==1)nonzeroProbes.incrementAndGet();}
                else wholeGets.incrementAndGet();
                boolean rangeMode=Set.of("range","slow-range","transient-range").contains(mode);
                boolean ignorePositive=mode.equals("transient-range")&&range!=null&&start>0&&end-start>1&&ignoredPositiveResponses.compareAndSet(0,1);
                if(ignorePositive){ignoredStarts.add(start);ignoredAt.compareAndSet(0,System.nanoTime());}
                boolean ranged=rangeMode&&range!=null&&start>0&&!ignorePositive;
                if(!rangeMode)exchange.getResponseHeaders().set("ETag",ETAG);
                exchange.getResponseHeaders().set("Content-Type",mode.equals("bad-headers")&&range==null?"text/html":"application/octet-stream");
                exchange.getResponseHeaders().set("Repr-Digest","sha-256=:"+Base64.getEncoder().encodeToString(HexFormat.of().parseHex(HASH))+":");
                if(ranged){exchange.getResponseHeaders().set("Content-Range","bytes "+start+"-"+end+"/"+DATA.length);if(end-start>1){segmentStarts.add(start);successfulPositiveResponses.incrementAndGet();if(ignoredAt.get()>0)recoveredAt.compareAndSet(0,System.nanoTime());}}
                else {start=0;end=DATA.length-1;}
                exchange.sendResponseHeaders(ranged?206:200,end-start+1);
                try(OutputStream out=exchange.getResponseBody()) {
                    long limit=mode.equals("cut")&&range==null?170123:end+1;
                    for(long pos=start;pos<=end&&pos<limit;pos+=32768){int count=(int)Math.min(Math.min(32768,end-pos+1),limit-pos);out.write(DATA,(int)pos,count);out.flush();if(ranged&&end-start>1)Thread.sleep(mode.equals("slow-range")?160:2);}
                }
            } catch(InterruptedException stopped){Thread.currentThread().interrupt();}
            catch(IOException clientClosed){/* The client closes an ignored-range metadata body deliberately. */}
            finally{exchange.close();}
        }
        URI endpoint(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort());}
        public void close(){server.stop(0);workers.shutdownNow();}
    }
    static List<Object> routes(int count){List<Object> routes=new ArrayList<>();for(int i=1;i<=count;i++)routes.add(new LinkedHashMap<>(Map.of("type","gateway","gateway","192.168.1."+i,"name","Gateway "+i,"enabled",true,"bytes",0L,"chunks",0L,"last_error","")));return routes;}
    static Main.Manager manager(String test)throws Exception{return new Main.Manager(root.resolve(test+"-data"),(u,r)->Map.of(),p->Long.MAX_VALUE,(r,t)->{});}
    static Main.Job job(Main.Manager manager,String name,List<Object> routes)throws Exception {
        String id=String.format("%012x",++ids);Path dir=root.resolve(name);Files.createDirectories(dir);
        Map<String,Object> state=new LinkedHashMap<>();state.put("id",id);state.put("url","http://owned-preservation.invalid/file.bin");state.put("filename","Проверка_日本語_é.bin");state.put("directory",dir.toString());state.put("part_path",dir.resolve(".Проверка_日本語_é.part").toString());state.put("routes",routes);state.put("expected_sha256",HASH);state.put("size",0L);state.put("chunk_size",(long)MIB);state.put("chunks",new LinkedHashMap<>());state.put("downloaded",0L);state.put("status","paused");
        Main.Job job=new Main.Job(state,manager.dataDir,SourceTransfer::open,TimeUnit.SECONDS.toNanos(2));manager.jobs.put(id,job);job.persist();return job;
    }
    static void completed(Main.Job job)throws Exception {
        waitFor(()->Set.of("complete","failed","paused").contains(Main.str(job.snapshot().get("status"))),20000,"transfer reached terminal state");
        Map<String,Object> value=job.snapshot();check("complete".equals(value.get("status")),"transfer completed: "+value.get("error"));
        Path result=Path.of(Main.str(value.get("directory"))).resolve(Main.str(value.get("filename")));
        check(Arrays.equals(DATA,Files.readAllBytes(result)),"all output bytes preserved");check(HASH.equals(value.get("sha256")),"publisher checksum verified");check(!result.toString().contains("\uFFFD"),"UTF-8 filename intact");
    }
    static void distribute()throws Exception {
        List<Origin> origins=new ArrayList<>();for(int i=0;i<4;i++)origins.add(new Origin("range"));
        SourceTransfer.endpoints=route->origins.get(Integer.parseInt(Main.str(route.get("gateway")).substring(10))-1).endpoint();Transport.resetGatewayClients();
        Main.Manager manager=manager("distribution");
        try {
            Main.Job job=job(manager,"distribution",routes(4));job.launch();completed(job);
            check(Boolean.TRUE.equals(job.snapshot().get("distribution_supported")),"nonzero probe enables distribution");
            check(origins.get(0).nonzeroProbes.get()>0,"initial HTTP200 is rechecked at nonzero offset");
            for(int i=0;i<4;i++)check(!origins.get(i).segmentStarts.isEmpty(),"gateway "+(i+1)+" received actual segments");
        }finally{manager.shutdown();for(Origin origin:origins)origin.close();}
    }
    static void preserveAndResume()throws Exception {
        try(Origin origin=new Origin("cut")) {
            SourceTransfer.endpoints=route->origin.endpoint();Transport.resetGatewayClients();Main.Manager manager=manager("preserve");
            try {
                Main.Job job=job(manager,"preserve",routes(4));job.launch();
                waitFor(()->"paused".equals(job.snapshot().get("status")),15000,"broken whole transfer pauses");
                byte[] prefix=Files.readAllBytes(job.part());check(prefix.length>0&&prefix.length<DATA.length,"partial bytes remain on disk");
                check(Arrays.equals(prefix,Arrays.copyOf(DATA,prefix.length)),"retained prefix matches owned source");
                check(Boolean.TRUE.equals(job.snapshot().get("restart_required")),"explicit restart requirement visible");
                check(origin.wholeGets.get()==1,"network error never retries from zero through another gateway");
                job.launch();waitFor(()->"paused".equals(job.snapshot().get("status")),15000,"resume of non-range source pauses safely");
                check(Arrays.equals(prefix,Files.readAllBytes(job.part())),"resume never truncates saved prefix");
                check(origin.wholeGets.get()==1,"resume never issues destructive whole request");
                check(Main.num(job.snapshot().get("downloaded"),0)==prefix.length,"saved progress remains visible");
            }finally{manager.shutdown();}
        }
    }
    static void recoverPrefix()throws Exception {
        List<Origin> origins=new ArrayList<>();for(int i=0;i<4;i++)origins.add(new Origin("range"));
        SourceTransfer.endpoints=route->origins.get(Integer.parseInt(Main.str(route.get("gateway")).substring(10))-1).endpoint();Transport.resetGatewayClients();Main.Manager manager=manager("recover");
        try {
            Main.Job job=job(manager,"recover",routes(4));int prefix=2*MIB+123;Files.write(job.part(),Arrays.copyOf(DATA,prefix));
            job.state.put("whole_file",true);job.state.put("size",(long)DATA.length);job.state.put("downloaded",(long)prefix);job.state.put("chunk_size",(long)DATA.length);job.state.put("segment_size",(long)MIB);job.state.put("etag","");job.state.put("modified","");job.persist();
            job.launch();completed(job);
            check(Main.num(job.snapshot().get("chunk_size"),0)==MIB,"original configured segment size restored");
            for(Origin origin:origins){check(!origin.segmentStarts.contains(0L),"saved first segment not downloaded again");check(!origin.segmentStarts.contains((long)MIB),"saved second segment not downloaded again");}
            check(Boolean.TRUE.equals(job.snapshot().get("distribution_supported")),"recovered whole download is distributed");
        }finally{manager.shutdown();for(Origin origin:origins)origin.close();}
    }
    static void rejectBeforeWrite()throws Exception {
        try(Origin origin=new Origin("bad-headers")) {
            SourceTransfer.endpoints=route->origin.endpoint();Transport.resetGatewayClients();Main.Manager manager=manager("headers");
            try {
                Main.Job job=job(manager,"headers",routes(1));job.launch();
                waitFor(()->"failed".equals(job.snapshot().get("status")),15000,"invalid whole response is rejected");
                check(!Files.exists(job.part()),"invalid headers never create or open partial output");
            }finally{manager.shutdown();}
        }
    }
    static void preserveSparseSegments()throws Exception {
        try(Origin origin=new Origin("whole")) {
            SourceTransfer.endpoints=route->origin.endpoint();Transport.resetGatewayClients();Main.Manager manager=manager("sparse");
            try {
                Main.Job job=job(manager,"sparse",routes(4));byte[] sparse=new byte[DATA.length];System.arraycopy(DATA,0,sparse,0,MIB);Files.write(job.part(),sparse);
                job.state.put("whole_file",false);job.state.put("size",(long)DATA.length);job.state.put("downloaded",(long)MIB);job.state.put("etag",ETAG);job.state.put("modified","");job.state.put("chunks",new LinkedHashMap<>(Map.of("0",Main.hex(Main.sha().digest(Arrays.copyOf(DATA,MIB))))));job.persist();
                job.launch();waitFor(()->"paused".equals(job.snapshot().get("status")),15000,"range support loss pauses segmented job");
                check(Arrays.equals(sparse,Files.readAllBytes(job.part())),"sparse saved segments never mistaken for a complete whole prefix");
                check(Boolean.FALSE.equals(job.snapshot().get("whole_file")),"unsupported source does not relabel sparse file as whole prefix");
                check(origin.wholeGets.get()==0,"range support loss never performs whole restart");
            }finally{manager.shutdown();}
        }
    }
    static void transientRangeRecovery()throws Exception {
        List<Origin> origins=new ArrayList<>();origins.add(new Origin("transient-range"));for(int i=1;i<4;i++)origins.add(new Origin("slow-range"));
        SourceTransfer.endpoints=route->origins.get(Integer.parseInt(Main.str(route.get("gateway")).substring(10))-1).endpoint();Transport.resetGatewayClients();Main.Manager manager=manager("transient");
        try {
            Main.Job job=job(manager,"transient",routes(4));job.launch();completed(job);
            Origin first=origins.get(0);check(first.ignoredPositiveResponses.get()==1,"one nonzero range deliberately returned HTTP200");
            check(first.successfulPositiveResponses.get()>0,"gateway automatically recovered after ignored range");
            check(first.recoveredAt.get()-first.ignoredAt.get()>=TimeUnit.MILLISECONDS.toNanos(1800),"gateway stayed selected and resumed after the two-second retry backoff");
            check(origins.stream().anyMatch(origin->origin.segmentStarts.contains(first.ignoredStarts.get(0))),"shared pool delivered the rejected range through a valid206 before completing");
            for(int i=0;i<4;i++)check(Main.num(Main.obj(Main.arr(job.snapshot().get("routes")).get(i)).get("bytes"),0)>0,"transient fixture gateway "+(i+1)+" completed useful bytes");
        }finally{manager.shutdown();for(Origin origin:origins)origin.close();}
    }
    public static void main(String[] args)throws Exception {
        root=Path.of(args[0]).toAbsolutePath();Files.createDirectories(root);SourceAuth.configure(root.resolve("sources"));AppLog.initialize(root.resolve("log-profile"));
        SourceTransfer.EndpointResolver previous=SourceTransfer.endpoints;
        try{
            distribute();preserveAndResume();recoverPrefix();rejectBeforeWrite();preserveSparseSegments();transientRangeRecovery();AppLog.flush();
            List<Map<String,Object>> events=new ArrayList<>();String journal=Files.readString(root.resolve("log-profile/logs/download-transfers.jsonl"),StandardCharsets.UTF_8);
            for(String line:journal.split("\\R"))if(!line.isBlank())events.add(Main.obj(Json.parse(line)));
            check(events.stream().allMatch(event->event.get("time") instanceof String),"every transfer journal entry retains a timestamp");
            check(events.stream().anyMatch(event->"prefix_preserved".equals(event.get("event"))&&Main.num(event.get("discarded_bytes"),-1)==0),"preservation events record zero discarded bytes");
            check(events.stream().noneMatch(event->"prefix_restarted".equals(event.get("event"))),"retry and resume never log a zero restart");
            for(int i=1;i<=4;i++){String gateway="192.168.1."+i;check(events.stream().anyMatch(event->gateway.equals(event.get("gateway"))&&"attempt_completed".equals(event.get("event"))&&Main.num(event.get("attempt_bytes"),0)>0),"journal records real completed bytes and range for gateway "+i);}
            check(!journal.contains("\uFFFD")&&!journal.contains("owned-preservation.invalid"),"journal is UTF-8 and contains no source URL");
        }
        finally{SourceTransfer.endpoints=previous;Transport.resetGatewayClients();AppLog.close();}
        Files.writeString(root.resolve("report.json"),Json.stringify(Map.of("status","PASS","checks",checks,"native_network_used",false,"payload_sha256",HASH)),StandardCharsets.UTF_8);
        System.out.println("PASS "+checks+" checks");
    }
}
