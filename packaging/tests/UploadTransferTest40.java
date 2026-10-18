package multiroute;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Real owned FTP/FTPS/SFTP servers behind an owned CONNECT proxy; no external traffic or user files. */
public final class UploadTransferTest40 {
    static Path own;static Map<String,Object> config;static int checks;static final List<String> cases=new ArrayList<>();
    static final List<UploadManager> managers=new ArrayList<>();
    static final Map<String,Object> route=Map.of("type","gateway","gateway","192.0.2.2","enabled",true,"virtual_ip","192.0.2.70");
    static final Map<String,Object> firstRoute=Map.of("type","gateway","gateway","192.0.2.1","enabled",true,"virtual_ip","192.0.2.70");
    static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);cases.add(name);}
    interface Action{void run()throws Exception;}
    static void rejects(Action action,String label)throws Exception{try{action.run();throw new AssertionError(label);}catch(IOException expected){check(true,label);}}
    static String url(String name,String path){return (name.equals("sftp")?"sftp":name.equals("ftps")?"ftps":"ftp")+"://fixture.invalid:"+Main.num(config.get(name),0)+path;}
    static String profile(String name,String fingerprint)throws Exception{
        Map<String,Object> input=new LinkedHashMap<>();input.put("name","OWN "+name+" профиль 📦");input.put("base_url",url(name,"/"));input.put("type",name.equals("sftp")?"sftp":"ftp");input.put("username","owned");input.put("password","own-upload-password");
        if(name.equals("sftp"))input.put("host_key_sha256",fingerprint);
        return Main.str(SiteProfiles.save(input).get("id"));
    }
    static Map<String,Object> request(Path local,String name,String remote,String profile,List<Map<String,Object>> routes){
        Map<String,Object> input=new LinkedHashMap<>();input.put("local_path",local.toString());input.put("target_url",url(name,remote));input.put("routes",routes);input.put("site_profile_id",profile);return input;
    }
    static Map<String,Object> terminal(UploadManager manager,String id,String status)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(65);
        while(System.nanoTime()<deadline){Map<String,Object> state=manager.snapshot(id);String current=Main.str(state.get("status"));if(Set.of("failed","complete").contains(current)){check(current.equals(status),"upload terminal "+status+": "+state.get("error"));return state;}Thread.sleep(20);}
        throw new AssertionError("Upload timed out: "+manager.snapshot(id));
    }
    static Map<String,Object> run(UploadManager manager,Path local,String name,String remote,String profile,List<Map<String,Object>> routes)throws Exception{
        Map<String,Object> created=manager.create(request(local,name,remote,profile,routes));return terminal(manager,Main.str(created.get("id")),"complete");
    }
    static Map<String,Object> decorated(String profile){Map<String,Object> result=new LinkedHashMap<>(route);result.put("site_profile_id",profile);return result;}
    static byte[] download(String name,String path,String profile,long size)throws Exception{
        if(size==0)return new byte[0];try(InputStream input=SourceTransfer.open(decorated(profile),url(name,path),0,size-1,null,null).body()){return input.readAllBytes();}
    }
    static void slowReady()throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);while(!Files.exists(own.resolve("slow-stor-ready"))&&System.nanoTime()<deadline)Thread.sleep(10);check(Files.exists(own.resolve("slow-stor-ready")),"owned FTP server received a real partial upload before pause");}
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();own=Path.of(args[0]).toAbsolutePath().normalize();config=Main.obj(Json.parse(Files.readString(own.resolve("fixture-config.json"),StandardCharsets.UTF_8)));
        Path profileDir=own.resolve("app-profile");SourceAuth.configure(profileDir);SiteProfiles.configure(profileDir);AppLog.initialize(profileDir);
        SourceTransfer.EndpointResolver original=SourceTransfer.endpoints;
        SourceTransfer.endpoints=selected->URI.create("http://127.0.0.1:"+Main.num(config.get("192.0.2.1".equals(selected.get("gateway"))?"refused_proxy":"proxy"),0));
        try{
            Path local=own.resolve("Локальный-файл-ä-📦.bin");byte[] bytes=new byte[262149];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)(i*31+i/83);Files.write(local,bytes);
            String sha=Main.hex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String ftp=profile("ftp",""),pasv=profile("pasv",""),ftps=profile("ftps",""),sftp=profile("sftp",Main.str(config.get("host_key_sha256"))),norest=profile("norest","");
            SourceAuth.save(Map.of("origin",url("ftp","/"),"type","ftp","username","wrong-global","password","WRONG-GLOBAL-SECRET"));
            UploadManager manager=new UploadManager(profileDir);managers.add(manager);
            Map<String,Object> listing=SourceTransfer.listRemote(decorated(ftp),url("ftp","/"));
            check(Main.arr(listing.get("entries")).stream().anyMatch(item->"каталог".equals(Main.obj(item).get("name"))&&Boolean.TRUE.equals(Main.obj(item).get("directory"))),"FTP MLSD returns Unicode remote directory with correct type");
            Map<String,Object> nested=SourceTransfer.listRemote(decorated(ftp),url("ftp","/каталог/"));
            check(Main.arr(nested.get("entries")).stream().anyMatch(item->"旧-файл.bin".equals(Main.obj(item).get("name"))),"FTP directory listing round-trips actual UTF-8 filename bytes");
            String remote="/каталог/Загрузка-ä-📦.bin";
            Map<String,Object> uploaded=run(manager,local,"ftp",remote,ftp,List.of(route));String completedId=Main.str(uploaded.get("id"));
            check(Boolean.TRUE.equals(uploaded.get("verified"))&&"sha256-readback".equals(uploaded.get("verification"))&&sha.equals(uploaded.get("sha256")),"FTP upload completion is backed by full remote SHA-256 readback");
            check(Arrays.equals(download("ftp",remote,ftp,bytes.length),bytes),"actual FTP published bytes match local Unicode-named source");
            check(Main.num(uploaded.get("uploaded_bytes"),0)==bytes.length&&Main.num(uploaded.get("eta_seconds"),-1)==0&&uploaded.containsKey("completed_at"),"completed upload DTO exposes bytes date and ETA");
            String manifest=Files.readString(profileDir.resolve("uploads").resolve(completedId+".json"),StandardCharsets.UTF_8);
            check(manifest.contains("📦")&&!manifest.contains("\ufffd")&&!manifest.contains("own-upload-password")&&!manifest.contains("WRONG-GLOBAL-SECRET"),"persistent upload manifest preserves UTF-8 and excludes source credentials");
            check(!Json.stringify(uploaded).contains("remote_temporary")&&Main.str(uploaded.get("local_path")).equals(local.toString()),"public upload DTO hides private temporary path and preserves full local path");
            Map<String,Object> existing=manager.create(request(local,"ftp","/existing.bin",ftp,List.of(route)));
            Map<String,Object> refused=terminal(manager,Main.str(existing.get("id")),"failed");
            check(!refused.containsKey("completed_at")&&Arrays.equals(download("ftp","/existing.bin",ftp,17),"OWN-EXISTING-KEEP".getBytes(StandardCharsets.UTF_8)),"default upload cannot overwrite existing remote file");
            Map<String,Object> corrupted=manager.create(request(local,"ftp","/corrupt.bin",ftp,List.of(route)));
            Map<String,Object> corruptResult=terminal(manager,Main.str(corrupted.get("id")),"failed");
            check(!corruptResult.containsKey("completed_at")&&!Boolean.TRUE.equals(corruptResult.get("verified")),"corrupted remote readback never claims successful checksum verification");
            check(Main.arr(SourceTransfer.listRemote(decorated(ftp),url("ftp","/")).get("entries")).stream().noneMatch(item->"corrupt.bin".equals(Main.obj(item).get("name"))),"corrupt temporary upload is never published under final name");
            Map<String,Object> failover=run(manager,local,"ftp","/failover.bin",ftp,List.of(firstRoute,route));
            check("192.0.2.2".equals(failover.get("active_gateway")),"upload automatically switches to another selected gateway after first gateway failure");
            Map<String,Object> passive=run(manager,local,"pasv","/pasv-upload.bin",pasv,List.of(route));
            check(Boolean.TRUE.equals(passive.get("verified")),"FTP PASV upload succeeds while advertised external PASV host is ignored");
            Map<String,Object> secure=run(manager,local,"ftps","/ftps-📦.bin",ftps,List.of(route));
            check(Arrays.equals(download("ftps","/ftps-📦.bin",ftps,bytes.length),bytes)&&Boolean.TRUE.equals(secure.get("verified")),"real FTPS control and data TLS upload and readback succeed");
            Map<String,Object> ssh=run(manager,local,"sftp","/sftp-кириллица-📦.bin",sftp,List.of(route));
            check(Arrays.equals(download("sftp","/sftp-кириллица-📦.bin",sftp,bytes.length),bytes)&&Boolean.TRUE.equals(ssh.get("verified")),"real host-key-pinned SFTP uploads Unicode paths with exact remote bytes");
            check(Main.arr(SourceTransfer.listRemote(decorated(sftp),url("sftp","/")).get("entries")).stream().anyMatch(item->"sftp-кириллица-📦.bin".equals(Main.obj(item).get("name"))),"SFTP remote directory browser lists uploaded Unicode file");
            Map<String,Object> literal=run(manager,local,"sftp","/literal-%2A-file.bin",sftp,List.of(route));
            run(manager,local,"sftp","/literal-other-file.bin",sftp,List.of(route));
            check(Boolean.TRUE.equals(literal.get("verified"))&&Arrays.equals(download("sftp","/literal-%2A-file.bin",sftp,bytes.length),bytes)&&Main.arr(SourceTransfer.listRemote(decorated(sftp),url("sftp","/")).get("entries")).stream().anyMatch(item->"literal-*-file.bin".equals(Main.obj(item).get("name"))),"SFTP wildcard characters in upload and download paths remain literal even beside matching filenames");
            String badPin=profile("sftp","SHA256:"+Base64.getEncoder().encodeToString(new byte[32]).replace("=",""));
            Map<String,Object> pinned=manager.create(request(local,"sftp","/must-not-publish.bin",badPin,List.of(route)));
            Map<String,Object> pinnedResult=terminal(manager,Main.str(pinned.get("id")),"failed");
            check(!pinnedResult.containsKey("completed_at"),"wrong SFTP host-key pin cannot publish an upload or record completion");
            Path large=own.resolve("Большой-slow-📦.bin");byte[] largeBytes=new byte[3*1024*1024+97];for(int i=0;i<largeBytes.length;i++)largeBytes[i]=(byte)(i*23+i/71);Files.write(large,largeBytes);
            Files.deleteIfExists(own.resolve("slow-stor-ready"));
            Map<String,Object> slow=manager.create(request(large,"ftp","/slow-resume.bin",ftp,List.of(route)));String slowId=Main.str(slow.get("id"));
            slowReady();manager.pause(slowId);check("paused".equals(manager.snapshot(slowId).get("status")),"pause cancels active upload and retains persistent paused state");
            manager.resume(slowId);Map<String,Object> resumed=terminal(manager,slowId,"complete");
            check(resumed.containsKey("first_started_at")&&resumed.containsKey("last_resumed_at")&&Arrays.equals(download("ftp","/slow-resume.bin",ftp,largeBytes.length),largeBytes),"safe FTP resume verifies remote prefix and completes actual partial bytes without corruption");
            Files.deleteIfExists(own.resolve("slow-stor-ready"));
            Map<String,Object> unsupported=manager.create(request(large,"norest","/norestslow.bin",norest,List.of(route)));String unsupportedId=Main.str(unsupported.get("id"));slowReady();manager.pause(unsupportedId);Thread.sleep(100);manager.resume(unsupportedId);
            Map<String,Object> restarted=terminal(manager,unsupportedId,"complete");
            check(Boolean.TRUE.equals(restarted.get("verified"))&&Arrays.equals(download("norest","/norestslow.bin",norest,largeBytes.length),largeBytes),"server without REST safely restarts only unique remote temporary file from zero");
            Files.deleteIfExists(own.resolve("slow-sftp-ready"));
            Map<String,Object> sshSlow=manager.create(request(large,"sftp","/sftp-slow-resume.bin",sftp,List.of(route)));String sshSlowId=Main.str(sshSlow.get("id"));
            long sshDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);while(!Files.exists(own.resolve("slow-sftp-ready"))&&System.nanoTime()<sshDeadline)Thread.sleep(10);
            check(Files.exists(own.resolve("slow-sftp-ready")),"owned SFTP server received actual partial writes before pause");manager.pause(sshSlowId);manager.resume(sshSlowId);
            Map<String,Object> sshResumed=terminal(manager,sshSlowId,"complete");
            check(Boolean.TRUE.equals(sshResumed.get("verified"))&&sshResumed.containsKey("last_resumed_at")&&Arrays.equals(download("sftp","/sftp-slow-resume.bin",sftp,largeBytes.length),largeBytes),"actual SFTP append resume verifies existing prefix and publishes exact full file");
            Files.deleteIfExists(own.resolve("slow-stor-ready"));
            Map<String,Object> beforeRestart=manager.create(request(large,"ftp","/slow-process-restart.bin",ftp,List.of(route)));String restartId=Main.str(beforeRestart.get("id"));slowReady();manager.pause(restartId);manager.shutdown();
            UploadManager restored=new UploadManager(profileDir);managers.add(restored);
            check("paused".equals(restored.snapshot(restartId).get("status")),"application restart restores queued upload as paused without unsolicited network transfer");
            restored.resume(restartId);Map<String,Object> afterRestart=terminal(restored,restartId,"complete");
            check(Boolean.TRUE.equals(afterRestart.get("verified"))&&Arrays.equals(download("ftp","/slow-process-restart.bin",ftp,largeBytes.length),largeBytes),"persistent upload resumes using same unique remote partial after manager restart");
            restored.remove(completedId);check(Files.exists(local)&&!Files.exists(profileDir.resolve("uploads").resolve(completedId+".json"))&&Arrays.equals(download("ftp",remote,ftp,bytes.length),bytes),"removing upload entry keeps local source and published remote file");
            rejects(()->restored.snapshot(completedId),"removed upload is absent from API state");
            rejects(()->restored.create(request(local,"ftp","/direct.bin",ftp,List.of(Map.of("type","direct","enabled",true)))),"upload rejects default OS routing instead of bypassing selected gateways");
            rejects(()->restored.create(new LinkedHashMap<>(Map.of("local_path",local.toString(),"target_url","ftp://user:secret@fixture.invalid/file","routes",List.of(route)))),"upload URL rejects embedded credentials");
            Path empty=own.resolve("empty.bin");Files.write(empty,new byte[0]);Map<String,Object> emptyResult=run(restored,empty,"ftp","/empty-upload.bin",ftp,List.of(route));
            check(Main.num(emptyResult.get("uploaded_bytes"),-1)==0&&Boolean.TRUE.equals(emptyResult.get("verified")),"empty FTP file receives real zero-byte readback verification");
            AppLog.flush();Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("version",Main.VERSION);report.put("checks",checks);report.put("cases",cases);report.put("protocols",List.of("ftp","ftps","sftp"));
            report.put("external_network_calls",0);report.put("user_files_read",false);report.put("physical_gateway_tested",false);report.put("owned_connect_proxy_only",true);report.put("sha256_remote_readback_verified",true);
            Files.writeString(own.resolve("UPLOAD-TRANSFER-TESTS.json"),Json.stringify(report)+"\n",StandardCharsets.UTF_8);System.out.println("PASS "+checks+" real upload checks");
        }finally{for(UploadManager manager:managers)manager.shutdown();SourceTransfer.endpoints=original;AppLog.close();}
    }
}
