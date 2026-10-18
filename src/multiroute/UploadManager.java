package multiroute;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Durable file uploads. Each file uses one selected gateway; independent files run in parallel. */
public final class UploadManager {
    private final Path directory;
    private final ConcurrentMap<String,Job> jobs = new ConcurrentHashMap<>();
    private final AtomicInteger nextGateway = new AtomicInteger();
    private final ExecutorService transfers = Executors.newFixedThreadPool(4, task -> { Thread thread = new Thread(task,"upload-transfer"); thread.setDaemon(true); return thread; });
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(task -> { Thread thread = new Thread(task,"upload-idle-watchdog"); thread.setDaemon(true); return thread; });
    private volatile boolean closing;

    public UploadManager(Path dataDirectory) throws IOException {
        directory = dataDirectory.toAbsolutePath().normalize().resolve("uploads"); Main.safePath(directory); Files.createDirectories(directory);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory,"*.json")) {
            for (Path manifest : entries) try {
                Main.safePath(manifest);
                if (!Files.isRegularFile(manifest,LinkOption.NOFOLLOW_LINKS) || Files.size(manifest)>4*1024*1024) throw new IOException("Invalid upload manifest");
                Map<String,Object> state = Main.obj(Json.parse(Files.readString(manifest,StandardCharsets.UTF_8)));
                String id = Main.str(state.get("id"));
                if (!id.matches("[a-f0-9]{12}") || !manifest.getFileName().toString().equals(id+".json")) throw new IOException("Invalid upload manifest");
                if (!Set.of("complete","failed","paused").contains(Main.str(state.get("status")))) state.put("status","paused");
                state.put("speed_bps",0L); Job job = new Job(state); jobs.put(id,job); job.persist();
            } catch (Exception malformed) { AppLog.error("Invalid upload manifest %s: %s",manifest,DiagnosticText.arg(malformed.getMessage())); }
        }
        watchdog.scheduleAtFixedRate(() -> {
            long now=System.nanoTime();
            for (Job job:jobs.values()) if(job.thread!=null&&!job.stop&&now-job.lastActivity>TimeUnit.SECONDS.toNanos(60)) {
                job.stalled=true;job.closeResources();
            }
        },1,1,TimeUnit.SECONDS);
    }

    public synchronized Map<String,Object> create(Map<String,Object> input) throws IOException {
        if(closing)throw new IOException("The application is shutting down");
        input=SiteProfiles.apply(input);
        if(jobs.size()>=1000)throw new IOException("Too many upload jobs");
        Path local;
        try{local=Path.of(Main.str(input.get("local_path"))).toAbsolutePath().normalize();}
        catch(RuntimeException malformed){throw new IOException("Invalid local upload file");}
        Main.safePath(local);
        if(!Files.isRegularFile(local,LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid local upload file");
        URI target=RemoteFiles.uri(Main.str(input.get("target_url")),true);
        if(input.containsKey("overwrite")&&!(input.get("overwrite") instanceof Boolean))throw new IOException("Invalid overwrite option");
        String profile=Main.str(input.get("site_profile_id"));
        if(!profile.isEmpty()&&!profile.matches("[A-Za-z0-9-]{1,80}"))throw new IOException("Invalid site profile ID");
        List<Object> routes=new ArrayList<>();Set<String> gateways=new HashSet<>();
        if(!(input.get("routes") instanceof List<?> requested)||requested.isEmpty()||requested.size()>32)throw new IOException("Choose between 1 and 32 gateways");
        for(Object raw:requested){
            if(!(raw instanceof Map<?,?> value))throw new IOException("Invalid gateway list");
            if(!"gateway".equals(value.get("type")))throw new IOException("This source requires a selected application gateway");
            if(Boolean.FALSE.equals(value.get("enabled")))continue;
            String address=Main.str(value.get("gateway"));
            if(!address.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")||Arrays.stream(address.split("\\.")).anyMatch(part->Integer.parseInt(part)>255)||!gateways.add(address))throw new IOException("Invalid gateway list");
            Map<String,Object> route=new LinkedHashMap<>();route.put("type","gateway");route.put("gateway",address);route.put("enabled",true);
            route.put("name",Main.str(value.get("name")).isBlank()?"Gateway "+address:Main.str(value.get("name")));
            for(String field:List.of("virtual_ip","site_profile_id"))if(value.get(field) instanceof String text)route.put(field,text);
            if(!profile.isEmpty())route.put("site_profile_id",profile);
            route.put("uploaded_bytes",0L);route.put("speed_bps",0L);routes.add(route);
        }
        if(routes.isEmpty())throw new IOException("Select at least one gateway");
        for(Job other:jobs.values())synchronized(other){if(!Set.of("complete","failed").contains(Main.str(other.state.get("status")))&&target.toASCIIString().equals(other.state.get("target_url"))&&profile.equals(Main.str(other.state.get("site_profile_id"))))throw new IOException("An upload already targets this remote file");}
        String id=UUID.randomUUID().toString().replace("-","").substring(0,12);
        Map<String,Object> state=new LinkedHashMap<>();
        state.put("id",id);state.put("filename",local.getFileName().toString());state.put("local_path",local.toString());state.put("target_url",target.toASCIIString());
        state.put("remote_filename",RemoteFiles.name(target.getPath()));state.put("remote_temporary",RemoteFiles.parent(target.getPath())+"."+RemoteFiles.name(target.getPath())+".multiroute-upload-"+id+".part");
        state.put("size",Files.size(local));state.put("local_modified_ms",Files.getLastModifiedTime(local,LinkOption.NOFOLLOW_LINKS).toMillis());
        state.put("uploaded_bytes",0L);state.put("speed_bps",0L);state.put("elapsed_ms",0L);state.put("status","queued");state.put("error","");state.put("routes",routes);
        state.put("overwrite",Boolean.TRUE.equals(input.get("overwrite")));state.put("site_profile_id",profile);state.put("created_at",Instant.now().toString());
        state.put("gateway_start_index",Math.floorMod(nextGateway.getAndIncrement(),routes.size()));
        Job job=new Job(state);job.persist();jobs.put(id,job);job.enqueue();return job.snapshot();
    }
    public Map<String,Object> state(){List<Map<String,Object>> result=new ArrayList<>();for(Job job:jobs.values())result.add(job.snapshot());result.sort(Comparator.comparing(value->Main.str(value.get("created_at"))));return Map.of("uploads",result);}
    private Job job(String id)throws IOException{Job job=jobs.get(id);if(job==null)throw new IOException("Upload not found");return job;}
    public Map<String,Object> snapshot(String id)throws IOException{return job(id).snapshot();}
    public Map<String,Object> pause(String id)throws IOException{Job job=job(id);job.pause();return job.snapshot();}
    public Map<String,Object> resume(String id)throws IOException{if(closing)throw new IOException("The application is shutting down");Job job=job(id);job.enqueue();return job.snapshot();}
    public synchronized Map<String,Object> remove(String id)throws IOException{
        Job job=job(id);synchronized(job){job.removed=true;job.restart=false;}job.pause();jobs.remove(id,job);Files.deleteIfExists(directory.resolve(id+".json"));
        return Map.of("id",id,"removed",true,"local_files_kept",true,"remote_files_kept",true);
    }
    public void shutdown(){
        closing=true;watchdog.shutdownNow();for(Job job:jobs.values())try{job.pause();}catch(IOException failure){AppLog.error("Could not save upload: %s",DiagnosticText.arg(failure.getMessage()));}
        transfers.shutdownNow();boolean interrupted=false;
        try{transfers.awaitTermination(10,TimeUnit.SECONDS);}catch(InterruptedException cancelled){interrupted=true;}
        if(interrupted)Thread.currentThread().interrupt();
    }

    private final class Job implements Runnable,SourceTransfer.UploadControl {
        final Map<String,Object> state;final Set<AutoCloseable> resources=ConcurrentHashMap.newKeySet();
        volatile Thread thread;volatile Future<?> future;volatile boolean stop,removed,restart,stalled;
        volatile long lastActivity=System.nanoTime();long elapsed,started,lastSaved,rateAt,rateBytes,attemptPrior=-1;int routeIndex;
        Job(Map<String,Object> state){this.state=state;elapsed=Main.num(state.get("elapsed_ms"),0);}
        synchronized void enqueue()throws IOException{
            if(removed||"complete".equals(state.get("status")))return;
            if(thread!=null){if(stop){restart=true;state.put("status","queued");persist();}return;}
            if(future!=null&&!future.isDone())return;
            stop=false;restart=false;state.put("status","queued");state.put("error","");persist();future=transfers.submit(this);
        }
        void pause()throws IOException{
            boolean active;
            synchronized(this){
                restart=false;if("complete".equals(state.get("status")))return;active=thread!=null&&!stop;stop=true;state.put("status","paused");state.put("speed_bps",0L);state.put("paused_at",Instant.now().toString());persist();
                if(future!=null)future.cancel(true);
            }
            closeResources();
            if(active)AppLog.info("Upload paused: %s; destination: %s",state.get("filename"),state.get("target_url"));
        }
        synchronized long elapsed(){return elapsed+(started==0?0:TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));}
        synchronized void persist()throws IOException{
            if(removed)return;state.put("elapsed_ms",elapsed());state.put("updated_at",Instant.now().toString());boolean interrupted=Thread.interrupted();
            try{for(int attempt=0;;attempt++)try{Main.save(directory.resolve(Main.str(state.get("id"))+".json"),state);break;}
                catch(java.nio.channels.ClosedByInterruptException cancelled){interrupted|=Thread.interrupted();if(attempt>=3)throw cancelled;}}
            finally{if(interrupted)Thread.currentThread().interrupt();}
        }
        synchronized Map<String,Object> snapshot(){
            Map<String,Object> copy=Main.obj(Json.parse(Json.stringify(state)));copy.remove("remote_temporary");copy.remove("local_modified_ms");copy.remove("gateway_start_index");
            copy.put("elapsed_ms",elapsed());long speed=Main.num(copy.get("speed_bps"),0),left=Math.max(0,Main.num(copy.get("size"),0)-Main.num(copy.get("uploaded_bytes"),0));
            copy.put("eta_seconds","complete".equals(copy.get("status"))?0L:speed>0?(long)Math.ceil(left/(double)speed):-1L);
            return copy;
        }
        public boolean cancelled(){return stop||removed||closing;}
        public void attach(AutoCloseable resource){resources.add(resource);if(cancelled())try{resource.close();}catch(Exception ignored){}}
        public void detach(AutoCloseable resource){resources.remove(resource);}
        void closeResources(){for(AutoCloseable resource:resources)try{resource.close();}catch(Exception ignored){}resources.clear();}
        @Override public void check()throws IOException{SourceTransfer.UploadControl.super.check();lastActivity=System.nanoTime();}
        public synchronized void stage(String status)throws IOException{check();state.put("status",status);state.put("speed_bps",0L);persist();}
        public synchronized void progress(long bytes)throws IOException{
            check();long now=System.nanoTime();long bounded=Math.min(Main.num(state.get("size"),0),Math.max(0,bytes));
            if(attemptPrior<0){attemptPrior=bounded;rateBytes=0;rateAt=now;}else{long delta=Math.max(0,bounded-attemptPrior);rateBytes+=delta;attemptPrior=bounded;List<Object> routes=Main.arr(state.get("routes"));Map<String,Object> route=Main.obj(routes.get(routeIndex));route.put("uploaded_bytes",Main.num(route.get("uploaded_bytes"),0)+delta);}
            state.put("uploaded_bytes",bounded);
            if(now-rateAt>=TimeUnit.MILLISECONDS.toNanos(200)){long speed=(long)(rateBytes*1_000_000_000.0/Math.max(1,now-rateAt));state.put("speed_bps",speed);Main.obj(Main.arr(state.get("routes")).get(routeIndex)).put("speed_bps",speed);rateBytes=0;rateAt=now;}
            if(now-lastSaved>=TimeUnit.MILLISECONDS.toNanos(250)){persist();lastSaved=now;}
        }
        private void localUnchanged(Path local)throws IOException{
            Main.safePath(local);
            if(!Files.isRegularFile(local,LinkOption.NOFOLLOW_LINKS)||Files.size(local)!=Main.num(state.get("size"),-1)||Files.getLastModifiedTime(local,LinkOption.NOFOLLOW_LINKS).toMillis()!=Main.num(state.get("local_modified_ms"),-1))throw new Transport.NonRetryableException("Local file changed during upload");
        }
        private String hash(Path local)throws IOException{
            MessageDigest digest=Main.sha();try(InputStream input=Files.newInputStream(local,LinkOption.NOFOLLOW_LINKS)){byte[] buffer=new byte[1024*1024];int read;while((read=input.read(buffer))>=0){check();if(read>0)digest.update(buffer,0,read);}}return Main.hex(digest.digest());
        }
        public void run(){
            synchronized(this){if(cancelled())return;thread=Thread.currentThread();started=System.nanoTime();lastActivity=started;String now=Instant.now().toString();
                if(state.containsKey("first_started_at"))state.put("last_resumed_at",now);else state.put("first_started_at",now);
                state.put("status","running");state.put("error","");}
            try{
                Path local=Path.of(Main.str(state.get("local_path")));localUnchanged(local);String digest=hash(local);localUnchanged(local);
                synchronized(this){String previous=Main.str(state.get("sha256"));if(!previous.isEmpty()&&!previous.equals(digest))throw new Transport.NonRetryableException("Local file changed during upload");state.put("sha256",digest);persist();}
                AppLog.info(state.containsKey("last_resumed_at")?"Upload resumed: %s; destination: %s":"Upload started: %s; destination: %s",state.get("filename"),state.get("target_url"));
                List<Object> routes=Main.arr(state.get("routes"));IOException last=null;
                outer:for(int round=0;round<3;round++)for(int candidate=0;candidate<routes.size();candidate++){
                    check();Thread.interrupted();stalled=false;int index=(int)Math.floorMod(Main.num(state.get("gateway_start_index"),0)+candidate,routes.size());
                    Map<String,Object> route=Main.obj(routes.get(index));
                    synchronized(this){routeIndex=index;attemptPrior=-1;rateAt=System.nanoTime();rateBytes=0;state.put("active_gateway",route.get("gateway"));state.put("status",round==0&&candidate==0?"running":"retrying");state.put("speed_bps",0L);persist();}
                    try{
                        Map<String,Object> verified=SourceTransfer.upload(route,Main.str(state.get("target_url")),local,Main.str(state.get("remote_temporary")),Main.num(state.get("size"),0),digest,Boolean.TRUE.equals(state.get("overwrite")),this);
                        localUnchanged(local);check();synchronized(this){state.putAll(verified);state.put("status","complete");state.put("error","");state.put("speed_bps",0L);state.put("completed_at",Instant.now().toString());persist();}
                        AppLog.info("Upload completed and verified: %s; destination: %s",state.get("filename"),state.get("target_url"));last=null;break outer;
                    }catch(IOException failure){
                        if(cancelled())throw failure;Thread.interrupted();last=failure;synchronized(this){state.put("error",stalled?"The gateway sent no data for 60 seconds":safeError(failure));state.put("status","retrying");state.put("speed_bps",0L);persist();}
                        if(failure instanceof Transport.NonRetryableException&&!safeError(failure).startsWith("SFTP connection"))break outer;
                    }finally{closeResources();}
                    if(candidate==routes.size()-1&&round<2)for(int wait=0;wait<10;wait++){if(cancelled())throw new InterruptedIOException("Transfer cancelled");Thread.sleep(100);}
                }
                if(last!=null)throw last;
            }catch(Exception failure){synchronized(this){if(!cancelled()){state.put("status","failed");state.put("error",safeError(failure));AppLog.error("Upload failed: %s; destination: %s; reason: %s",state.get("filename"),state.get("target_url"),DiagnosticText.arg(safeError(failure)));}}}
            finally{
                closeResources();boolean again;
                synchronized(this){
                    if(started!=0){elapsed+=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);started=0;}
                    state.put("speed_bps",0L);for(Object raw:Main.arr(state.get("routes")))Main.obj(raw).put("speed_bps",0L);
                    thread=null;future=null;again=restart&&!removed&&!closing;restart=false;
                    try{persist();}catch(IOException failure){AppLog.error("Could not save upload: %s",DiagnosticText.arg(failure.getMessage()));}
                }
                if(again)try{enqueue();}catch(IOException failure){AppLog.error("Could not resume upload: %s",DiagnosticText.arg(failure.getMessage()));}
            }
        }
    }
    private static String safeError(Exception error){String text=Main.str(error.getMessage());return text.isBlank()?"Upload failed":text.replace('\r',' ').replace('\n',' ');}
}
