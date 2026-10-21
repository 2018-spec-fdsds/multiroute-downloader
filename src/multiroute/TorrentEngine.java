package multiroute;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/** One native BitTorrent session per selected gateway; verified pieces share a durable job store. */
public final class TorrentEngine {
    @FunctionalInterface interface Resolver {
        InetSocketAddress resolve(Map<String,Object> route) throws Exception;
        default java.net.http.HttpResponse<InputStream> open(Map<String,Object> route,String url,long start,long end)throws Exception {return Transport.open(route,url,start,end,null,null);}
        default java.net.http.HttpResponse<InputStream> get(Map<String,Object> route,String url)throws Exception {return Transport.torrentGet(route,url);}
    }
    private TorrentEngine() { }
    public static InetSocketAddress socksEndpoint(Map<String,Object> route) throws Exception {
        if (!"gateway".equals(route.get("type"))) throw new IOException("Для torrent выберите шлюз");
        try {
            Object endpoint=GatewayNetwork.class.getMethod("socksEndpoint",Map.class).invoke(null,route);
            if (!(endpoint instanceof InetSocketAddress address) || address.isUnresolved() ||
                    !address.getAddress().isLoopbackAddress() || address.getPort()<1)
                throw new IOException("Некорректный локальный SOCKS5 шлюза");
            return address;
        } catch(java.lang.reflect.InvocationTargetException error) {
            if(error.getCause() instanceof Exception cause)throw cause;throw new IOException("Не удалось запустить шлюз",error);
        } catch(ReflectiveOperationException error) {throw new IOException("Транспорт SOCKS5 шлюзов не установлен",error);}
    }
    static SettingsPack settings(Map<String,Object> route, Resolver resolver, boolean discovery) throws Exception {
        InetSocketAddress proxy=resolver.resolve(route);
        if(proxy==null || proxy.isUnresolved() || !proxy.getAddress().isLoopbackAddress())throw new IOException("SOCKS5 шлюза должен быть локальным");
        SettingsPack settings=new SettingsPack();
        settings.setString(settings_pack.string_types.proxy_hostname.swigValue(),proxy.getAddress().getHostAddress());
        // Keep the native connection to our local proxy independent of the OS default interface.
        settings.setString(settings_pack.string_types.outgoing_interfaces.swigValue(),proxy.getAddress().getHostAddress());
        settings.setInteger(settings_pack.int_types.proxy_port.swigValue(),proxy.getPort());
        settings.setInteger(settings_pack.int_types.proxy_type.swigValue(),settings_pack.proxy_type_t.socks5.swigValue());
        settings.setString(settings_pack.string_types.listen_interfaces.swigValue(),"127.0.0.1:0");
        settings.setBoolean(settings_pack.bool_types.proxy_peer_connections.swigValue(),true);
        settings.setBoolean(settings_pack.bool_types.proxy_tracker_connections.swigValue(),true);
        settings.setBoolean(settings_pack.bool_types.proxy_hostnames.swigValue(),true);
        settings.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_lsd.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_incoming_tcp.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_incoming_utp.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_outgoing_utp.swigValue(),false);
        settings.setBoolean(settings_pack.bool_types.enable_outgoing_tcp.swigValue(),discovery);
        settings.setBoolean(settings_pack.bool_types.enable_dht.swigValue(),discovery);
        settings.setBoolean(settings_pack.bool_types.prefer_udp_trackers.swigValue(),false);
        settings.setInteger(settings_pack.int_types.connections_limit.swigValue(),12);
        settings.setInteger(settings_pack.int_types.max_metadata_size.swigValue(),TorrentSource.MAX_METADATA);
        settings.setInteger(settings_pack.int_types.stop_tracker_timeout.swigValue(),1);
        settings.setInteger(settings_pack.int_types.peer_connect_timeout.swigValue(),10);
        settings.setInteger(settings_pack.int_types.min_reconnect_time.swigValue(),2);
        settings.setInteger(settings_pack.int_types.tracker_completion_timeout.swigValue(),15);
        settings.setInteger(settings_pack.int_types.tracker_receive_timeout.swigValue(),10);
        return settings;
    }
    private static void networkParams(AddTorrentParams params,TorrentNetwork network)throws IOException {
        List<String> trackers=new ArrayList<>();for(String url:params.trackers())trackers.add(network.rewrite(url));params.trackers(trackers);
        List<String> seeds=new ArrayList<>();for(String url:params.urlSeeds())seeds.add(network.rewrite(url));params.urlSeeds(seeds);
    }
    static byte[] magnetMetadata(String source,Map<String,Object> route,Resolver resolver) throws Exception {
        SessionManager session=new SessionManager();
        Path temp=Files.createTempDirectory("multiroute-magnet-");TorrentNetwork network=null;
        try {
            network=new TorrentNetwork(route,resolver);final TorrentNetwork activeNetwork=network;
            AtomicReference<byte[]> data=new AtomicReference<>();AtomicReference<String> failure=new AtomicReference<>();CountDownLatch ready=new CountDownLatch(1);
            session.addListener(new AlertListener(){public int[] types(){return new int[]{AlertType.METADATA_RECEIVED.swig(),AlertType.METADATA_FAILED.swig(),AlertType.SAVE_RESUME_DATA.swig(),AlertType.SAVE_RESUME_DATA_FAILED.swig(),AlertType.TORRENT_NEED_CERT.swig()};}public void alert(Alert<?> alert){
                try {
                    if(alert instanceof TorrentNeedCertAlert certificate){certificate.handle().pause();failure.set("SSL-torrent требует клиентский сертификат и пока не поддерживается");ready.countDown();}
                    else if(alert instanceof MetadataReceivedAlert metadata)metadata.handle().saveResumeData(TorrentHandle.SAVE_INFO_DICT);
                    else if(alert instanceof SaveResumeDataAlert saved){
                        data.set(TorrentSource.restoreEndpoints(TorrentSource.torrentFromResume(AddTorrentParams.writeResumeData(saved.params()).bencode()),activeNetwork::restore));ready.countDown();
                    }else{failure.set("Не удалось получить метаданные magnet");ready.countDown();}
                }catch(Throwable error){failure.set("Ошибка метаданных magnet: "+error.getMessage());ready.countDown();}
            }});
            session.start(new SessionParams(settings(route,r->activeNetwork.endpoint(),true)));
            AddTorrentParams params=AddTorrentParams.parseMagnetUri(source);networkParams(params,network);params.savePath(temp.toString());
            params.flags(TorrentFlags.UPLOAD_MODE.or_(TorrentFlags.DISABLE_LSD));
            ErrorCode error=new ErrorCode(new error_code());TorrentHandle handle=new SessionHandle(session.swig()).addTorrent(params,error);
            if(error.value()!=0||!handle.isValid())throw new IOException("Некорректная magnet-ссылка: "+error.message());
            if(!ready.await(45,TimeUnit.SECONDS))throw new IOException("Не удалось получить метаданные magnet через выбранный шлюз за 45 секунд");
            if(failure.get()!=null)throw new IOException(failure.get());if(data.get()==null)throw new IOException("Magnet не предоставил метаданные");return data.get();
        }finally {
            try{if(network!=null)network.close();}finally{session.stop();}
            // Metadata mode never downloads payload, but native storage may create empty paths.
            safeDeleteOwned(temp);
        }
    }
    public static Map<String,Object> run(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update) throws Exception {
        TorrentSource.nativeReady();
        try{return run(state,scratch,stop,update,TorrentEngine::socksEndpoint);}catch(LinkageError unavailable){throw new IOException("Компонент BitTorrent не загружен. Обновите всю папку приложения",unavailable);}
    }
    public static Map<String,Object> run(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update,Supplier<List<Map<String,Object>>> routes) throws Exception {
        TorrentSource.nativeReady();
        try{return run(state,scratch,stop,update,TorrentEngine::socksEndpoint,routes);}catch(LinkageError unavailable){throw new IOException("Компонент BitTorrent не загружен. Обновите всю папку приложения",unavailable);}
    }
    static Map<String,Object> run(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update,Resolver resolver) throws Exception {
        return run(state,scratch,stop,update,resolver,()->enabledRoutes(state));
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> enabledRoutes(Map<String,Object> state) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Object raw:(List<?>)state.getOrDefault("routes",List.of()))if(raw instanceof Map<?,?> map&&!Boolean.FALSE.equals(map.get("enabled")))result.add(new LinkedHashMap<>((Map<String,Object>)map));
        return result;
    }
    static Map<String,Object> runControlled(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update,Supplier<List<Map<String,Object>>> routes,Supplier<Map<String,Object>> options,TransferLimits limits)throws Exception {
        TorrentSource.nativeReady();return run(state,scratch,stop,update,TorrentEngine::socksEndpoint,routes,options,limits);
    }
    static Map<String,Object> run(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update,Resolver resolver,Supplier<List<Map<String,Object>>> routeSupplier)throws Exception {
        return run(state,scratch,stop,update,resolver,routeSupplier,()->state,null);
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> run(Map<String,Object> state,Path scratch,BooleanSupplier stop,Consumer<Map<String,Object>> update,Resolver resolver,Supplier<List<Map<String,Object>>> routeSupplier,Supplier<Map<String,Object>> options,TransferLimits limits) throws Exception {
        String encoded=string(state.get("torrent_metadata"));
        byte[] metadata;
        try {if(encoded.length()>TorrentSource.MAX_METADATA*2)throw new IllegalArgumentException();metadata=Base64.getDecoder().decode(encoded);}
        catch(IllegalArgumentException bad){throw new IOException("Повреждены сохранённые метаданные torrent",bad);}
        Map<String,Object> description=TorrentSource.describe(metadata);
        String identity=description.get("info_hash_v1")+":"+description.get("info_hash_v2");
        TorrentInfo ti=new TorrentInfo(metadata);FileStorage fs=ti.files();
        List<Map<String,Object>> routes=new ArrayList<>();
        for(Object raw:(List<?>)state.getOrDefault("routes",List.of()))if(raw instanceof Map<?,?> map)routes.add(new LinkedHashMap<>((Map<String,Object>)map));
        desiredRoutes(routeSupplier); // Validate before touching storage or native sessions.
        Path root=Path.of(string(state.get("directory"))).toAbsolutePath().normalize();
        safeDirectories(root);scratch=scratch.toAbsolutePath().normalize();safeDirectories(scratch);
        Path pieceDir=scratch.resolve("pieces"), assembled=scratch.resolve("assembled");safeDirectories(pieceDir);safeDirectories(assembled);
        Set<String> requested=new LinkedHashSet<>();
        for(Object path:(List<?>)state.getOrDefault("selected_files",List.of()))requested.add(TorrentSource.normalize(string(path)));
        if(requested.isEmpty()) {
            if(ti.numFiles()!=1 || fs.padFileAt(0))throw new IOException("Выберите файлы torrent");requested.add(TorrentSource.normalize(fs.filePath(0)));
        }
        Map<String,Object> filePriorities=options.get().get("file_priorities") instanceof Map<?,?> priorities?Main.obj(priorities):Map.of();
        Set<Integer> selected=new LinkedHashSet<>();long total=0;
        for(int file=0;file<ti.numFiles();file++)if(requested.remove(TorrentSource.normalize(fs.filePath(file)))) {
            if(fs.padFileAt(file))throw new IOException("Нельзя выбрать служебный pad-файл");if(Main.num(filePriorities.get(TorrentSource.normalize(fs.filePath(file))),4)==0)continue;selected.add(file);total=Math.addExact(total,fs.fileSize(file));
        }
        if(!requested.isEmpty() || selected.isEmpty())throw new IOException("Выбранного файла нет в torrent");
        Map<String,String> expectedHashes=Checksums.expected(state);
        if(!expectedHashes.isEmpty()&&selected.size()!=1)throw new IOException("A global checksum is allowed only for one selected torrent file");
        boolean[] wanted=new boolean[ti.numPieces()];
        for(int file:selected) {
            long begin=fs.fileOffset(file),end=begin+fs.fileSize(file);
            if(end>begin)for(int piece=(int)(begin/ti.pieceLength());piece<ti.numPieces() && (long)piece*ti.pieceLength()<end;piece++)wanted[piece]=true;
        }
        Path checkpoint=scratch.resolve("checkpoint.json");Map<String,Object> saved=new LinkedHashMap<>();
        if(Files.isRegularFile(checkpoint,LinkOption.NOFOLLOW_LINKS)) {
            if(Files.size(checkpoint)>64L*1024*1024)throw new IOException("Слишком большой checkpoint torrent");
            Object raw=Json.parse(Files.readString(checkpoint));if(raw instanceof Map<?,?> map)saved.putAll((Map<String,Object>)map);
            if(!identity.equals(saved.get("metadata_sha256")))throw new IOException("Метаданные torrent изменились");
        }
        if(saved.containsKey("root")&&!root.toString().equals(saved.get("root")))throw new IOException("Папка сохранения torrent изменилась");
        saved.put("metadata_sha256",identity);saved.put("root",root.toString());
        Map<String,Object> pieces=saved.get("pieces") instanceof Map<?,?> m ? new LinkedHashMap<>((Map<String,Object>)m):new LinkedHashMap<>();
        Map<String,Object> published=saved.get("published") instanceof Map<?,?> m ? new LinkedHashMap<>((Map<String,Object>)m):new LinkedHashMap<>();
        saved.put("pieces",pieces);saved.put("published",published);
        boolean[] complete=new boolean[ti.numPieces()];
        for(int piece=0;piece<complete.length;piece++)if(wanted[piece]) {
            Path file=pieceDir.resolve(piece+".bin");Object record=pieces.get(Integer.toString(piece));
            if(record instanceof Map<?,?> proof && Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) && Files.size(file)==ti.pieceSize(piece)) {
                byte[] bytes=Files.readAllBytes(file);
                if(digest("SHA-256",bytes).equals(proof.get("sha256")) && verifyV1(ti,piece,bytes))complete[piece]=true;
                else pieces.remove(Integer.toString(piece));
            }
        }
        for(int file:selected) {
            String path=TorrentSource.normalize(fs.filePath(file));Path destination=safeResolve(root,path);
            if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS) && !ownedPublished(destination,path,published))
                throw new IOException("Файл уже существует: "+path);
        }
        long required=0;for(int piece=0;piece<wanted.length;piece++)if(wanted[piece])required=Math.addExact(required,ti.pieceSize(piece));
        long allowance=16L*1024*1024;boolean sameVolume=Files.getFileStore(root).name().equals(Files.getFileStore(scratch).name());
        if(Files.getFileStore(scratch).getUsableSpace()<Math.addExact(Math.multiplyExact(required,sameVolume?3:2),allowance) ||
                Files.getFileStore(root).getUsableSpace()<Math.addExact(total,allowance))throw new IOException("Недостаточно свободного места для частей torrent и итоговых файлов");
        if(!string(state.get("id")).matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Некорректный идентификатор загрузки");
        int[] pieceSizes=new int[complete.length];for(int p=0;p<pieceSizes.length;p++)pieceSizes[p]=ti.pieceSize(p);
        PieceScheduler queue=new PieceScheduler(wanted,complete,pieceSizes,new boolean[32],System.nanoTime());
        int[] scores=new int[ti.numPieces()];
        for(int file:selected){long begin=fs.fileOffset(file),end=begin+fs.fileSize(file);int score=(int)Main.num(filePriorities.get(TorrentSource.normalize(fs.filePath(file))),4);for(int piece=(int)(begin/ti.pieceLength());piece<ti.numPieces()&&(long)piece*ti.pieceLength()<end;piece++)scores[piece]=Math.max(scores[piece],score);}
        queue.setPriorities(scores);
        List<Channel> channels=new ArrayList<>(Collections.nCopies(32,null));
        Set<Path> ownedStorage=new LinkedHashSet<>();Map<String,Long> retryAt=new HashMap<>();
        try {
            reconcile(channels,routes,queue,desiredRoutes(routeSupplier),retryAt,ownedStorage,scratch,ti,string(state.get("url")),resolver,stop,limits);
            long lastCheckpoint=0;int count=count(complete,wanted);int completedBefore=count;
            update.accept(progress(ti,fs,selected,wanted,complete,pieces,routes,total,"running"));
            while(count<countWanted(wanted)) {
                if(stop.getAsBoolean())break;
                reconcile(channels,routes,queue,desiredRoutes(routeSupplier),retryAt,ownedStorage,scratch,ti,string(state.get("url")),resolver,stop,limits);
                boolean gained=false;
                for(int index=0;index<channels.size();index++) {
                    Channel channel=channels.get(index);
                    if(channel==null||!queue.enabled(index))continue;
                    if(channel.fatal.get()!=null)throw new IOException(channel.fatal.get());
                    if(channel.network!=null&&channel.network.failure.get()!=null) {
                        channel.route.put("error",channel.network.failure.get());
                        channel.route.put("last_error",channel.network.failure.get());
                        retire(channels,index,queue);retryAt.put(routeKey(channel.route),System.nanoTime()+TimeUnit.SECONDS.toNanos(5));
                        continue;
                    }
                    channel.applyLimit();
                    TorrentStatus status=channel.handle.status(TorrentHandle.QUERY_PIECES);PieceIndexBitfield bits=status.pieces();
                    List<Integer> ready=new ArrayList<>();
                    try{
                        channel.recordReceived(status.totalPayloadDownload());channel.recordStatus(status);
                        queue.observe(index,Main.num(channel.route.get("received_bytes"),0),System.nanoTime());
                        channel.observePieces(queue,index,System.nanoTime());
                        for(int piece=0;piece<wanted.length;piece++)if(wanted[piece]&&!complete[piece]&&piece<bits.size()&&bits.getBit(piece))ready.add(piece);
                    }
                    finally{bits.swig().delete();status.swig().delete();}
                    if(!ready.isEmpty()) {
                        channel.flush(stop);
                        for(int piece:ready) {
                            if(stop.getAsBoolean())break;
                            byte[] bytes=readPiece(ti,channel.storage,piece);
                            if(!verifyV1(ti,piece,bytes)) {channel.handle.forceRecheck();continue;}
                            writeAtomic(pieceDir.resolve(piece+".bin"),bytes);
                            pieces.put(Integer.toString(piece),Map.of("sha256",digest("SHA-256",bytes),"gateway",string(channel.route.get("gateway"))));
                            complete[piece]=true;queue.verified(piece,index,System.nanoTime());gained=true;
                        }
                    }
                    channel.persistResume();
                }
                count=count(complete,wanted);
                long now=System.nanoTime();
                queue.refill(now);
                // Close the old session to cancel pending peer/webseed requests before another slot wins.
                for(int index=0;index<channels.size();index++)if(queue.takeStalled(index)&&channels.get(index)!=null) {
                    Channel channel=channels.get(index);channel.route.put("stalled",true);
                    channel.route.put("last_error","Сегмент не получает данные 60 секунд; возвращён в очередь");
                    channel.route.put("stall_releases",Main.num(channel.route.get("stall_releases"),0)+1);
                    if(queue.pieces(index).length==0){retire(channels,index,queue);retryAt.put(routeKey(channel.route),now+PieceScheduler.RETRY_AFTER);}
                }
                queue.refill(now);
                for(int index=0;index<channels.size();index++)if(queue.takeDirty(index)&&channels.get(index)!=null)
                    channels.get(index).handle.prioritizePieces(priorities(queue,index,ti.numPieces()));
                if(gained || now-lastCheckpoint>TimeUnit.SECONDS.toNanos(1)) {
                    writeCheckpoint(checkpoint,saved);lastCheckpoint=now;
                    update.accept(progress(ti,fs,selected,wanted,complete,pieces,routes,total,count==completedBefore?"retrying":"running"));completedBefore=count;
                }
                if(!gained)Thread.sleep(150);
            }
            // Capture the final blocks before pausing resets libtorrent's session payload counter.
            for(Channel channel:channels)if(channel!=null)channel.sampleReceived();
            writeCheckpoint(checkpoint,saved);
            if(stop.getAsBoolean())return progress(ti,fs,selected,wanted,complete,pieces,routes,total,"paused");
            for(Channel channel:channels)if(channel!=null){channel.handle.pause();channel.requestResume();}
            long captureDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
            
            for(Channel channel:channels)if(channel!=null){channel.resumeSignal.await(Math.max(0,captureDeadline-System.nanoTime()),TimeUnit.NANOSECONDS);channel.persistResume();}
            closeChannels(channels);
            for(Path storage:ownedStorage)deleteOwnedStorage(storage,scratch);
            channels.clear();
            update.accept(Map.of("status","verifying","downloaded",total,"error",""));
            for(int file=0;file<ti.numFiles();file++)if(!fs.padFileAt(file)) {
                Path path=safeResolve(assembled,TorrentSource.normalize(fs.filePath(file)));safeDirectories(path.getParent());ensureSparse(path,fs.fileSize(file));
            }
            for(int piece=0;piece<wanted.length;piece++)if(wanted[piece]) {
                if(stop.getAsBoolean())return Map.of("status","paused");
                byte[] bytes=Files.readAllBytes(pieceDir.resolve(piece+".bin"));
                if(!digest("SHA-256",bytes).equals(((Map<?,?>)pieces.get(Integer.toString(piece))).get("sha256")) || !verifyV1(ti,piece,bytes))
                    throw new IOException("Сохранённая часть torrent повреждена: "+piece);
                writePiece(ti,assembled,piece,bytes);
            }
            // Verification is offline and also works if the user deselects every gateway at completion.
            Map<String,Object> verificationRoute=routes.isEmpty()?Map.of("type","gateway","gateway","192.168.1.1"):routes.get(0);
            if(ti.swig().info_hashes().has_v1())verifyAssembled(ti,assembled,wanted,verificationRoute,resolver,stop);
            if(ti.swig().info_hashes().has_v2())verifyV2Files(metadata,ti,assembled,selected,stop);
            if(stop.getAsBoolean())return Map.of("status","paused");
            List<Map<String,Object>> outputs=new ArrayList<>();
            for(int index:selected) {
                if(stop.getAsBoolean())return Map.of("status","paused");
                String path=TorrentSource.normalize(fs.filePath(index));Path source=safeResolve(assembled,path),destination=safeResolve(root,path);
                Map<String,String> hashes=Checksums.file(source);String sha=hashes.get("sha256");
                if(selected.size()==1){update.accept(new LinkedHashMap<>(hashes));Checksums.verify(hashes,expectedHashes);}
                if(!ownedPublished(destination,path,published)) {
                    safeDirectories(destination.getParent());
                    if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS))throw new IOException("Файл уже существует: "+path);
                    Path temporary=destination.resolveSibling("."+destination.getFileName()+".multiroute-"+string(state.get("id"))+".tmp");
                    if(Files.exists(temporary,LinkOption.NOFOLLOW_LINKS))throw new IOException("Временный файл уже существует: "+temporary.getFileName());
                    try {
                        Files.copy(source,temporary);if(!Checksums.file(temporary).equals(hashes))throw new IOException("Ошибка проверки скопированного файла");
                        if(stop.getAsBoolean())return Map.of("status","paused");
                        // Record intent first; recovery only accepts the exact final length and hash.
                        Map<String,Object> proof=new LinkedHashMap<>(hashes);proof.put("size",fs.fileSize(index));published.put(path,proof);writeCheckpoint(checkpoint,saved);
                        Files.move(temporary,destination);
                    }finally {Files.deleteIfExists(temporary);}
                }
                Map<String,Object> output=new LinkedHashMap<>(hashes);output.put("path",path);output.put("size",fs.fileSize(index));output.put("verified",true);
                if(selected.size()==1){for(var entry:expectedHashes.entrySet())output.put("expected_"+entry.getKey(),entry.getValue());output.put("checksum_sources",Checksums.sources(state));}
                outputs.add(output);
            }
            Map<String,Object> result=progress(ti,fs,selected,wanted,complete,pieces,routes,total,"complete");
            result.put("error","");result.put("files",outputs);for(String algorithm:Checksums.ALGORITHMS)result.put(algorithm,outputs.size()==1?outputs.get(0).get(algorithm):"");
            result.put("checksum_sources",Checksums.sources(state));
            result.put("checksum_source","bittorrent-piece-hashes");result.put("verified",true);result.put("pieces_verified",true);
            if(Boolean.TRUE.equals(options.get().get("seeding_enabled")))seedVerified(ti,assembled,scratch,state,stop,update,routeSupplier,options,resolver,limits,result);
            return result;
        }finally {
            boolean interrupted=Thread.interrupted();
            try{
                for(Channel channel:channels)if(channel!=null)channel.sampleReceived();
                if(channels.stream().anyMatch(Objects::nonNull))update.accept(progress(ti,fs,selected,wanted,complete,pieces,routes,total,stop.getAsBoolean()?"paused":"running"));
                for(Channel channel:channels)if(channel!=null&&channel.handle!=null&&channel.handle.isValid())channel.handle.pause();
                for(Channel channel:channels)if(channel!=null)channel.requestResume();
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
                for(Channel channel:channels)if(channel!=null){
                    try{channel.resumeSignal.await(Math.max(0,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
                    catch(InterruptedException paused){interrupted=true;}
                    channel.persistResume();
                }
                writeCheckpoint(checkpoint,saved);
            }finally{try{closeChannels(channels);}finally{if(interrupted)Thread.currentThread().interrupt();}}
        }
    }
    /** Read-only uploads from the independently verified assembled store; no missing piece is requested. */
    private static void seedVerified(TorrentInfo ti,Path assembled,Path scratch,Map<String,Object> state,BooleanSupplier stop,Consumer<Map<String,Object>> update,Supplier<List<Map<String,Object>>> routeSupplier,Supplier<Map<String,Object>> options,Resolver resolver,TransferLimits limits,Map<String,Object> completed)throws Exception {
        Map<String,Channel> seeds=new LinkedHashMap<>();Map<String,Long> uploadBaseline=new HashMap<>();long started=System.nanoTime(),uploaded=0;String outcome="disabled";
        try {
            while(!stop.getAsBoolean()&&Boolean.TRUE.equals(options.get().get("seeding_enabled"))) {
                Map<String,Object> policy=options.get();long elapsed=System.nanoTime()-started;
                long maximum=TimeUnit.MINUTES.toNanos(Main.num(policy.get("seeding_minutes"),30));
                double ratio=policy.get("seeding_ratio") instanceof Number number?number.doubleValue():1.0;
                if(elapsed>=maximum){outcome="time_limit";break;}
                if(uploaded>=Math.max(1,Main.num(completed.get("size"),1))*ratio){outcome="ratio_limit";break;}
                List<Map<String,Object>> routes=desiredRoutes(routeSupplier);Set<String> desired=new HashSet<>();for(var route:routes)desired.add(routeKey(route));
                for(String key:new ArrayList<>(seeds.keySet()))if(!desired.contains(key)){Channel retired=seeds.remove(key);retired.sampleReceived();long current=Main.num(retired.route.get("uploaded_bytes"),0);uploaded+=Math.max(0,current-uploadBaseline.getOrDefault(key,current));uploadBaseline.remove(key);closeChannels(List.of(retired));}
                for(var route:routes)if(!seeds.containsKey(routeKey(route))) {
                    Channel channel=new Channel(new LinkedHashMap<>(route),assembled,scratch.resolve("seed-"+string(route.get("gateway"))+".bin"),resolver,true);
                    try {Priority[] ignored=new Priority[ti.numPieces()];Arrays.fill(ignored,Priority.IGNORE);channel.add(ti,ignored,string(state.get("url")));channel.handle.setFlags(TorrentFlags.UPLOAD_MODE);channel.handle.setUploadLimit(1024*1024);channel.bindLimits(limits);channel.handle.forceRecheck();uploadBaseline.put(routeKey(route),Main.num(channel.route.get("uploaded_bytes"),0));channel.handle.resume();seeds.put(routeKey(route),channel);}
                    catch(Exception error){closeChannels(List.of(channel));throw error;}
                }
                List<Object> summaries=new ArrayList<>();
                for(Channel channel:seeds.values()){channel.applyLimit();channel.sampleReceived();String key=routeKey(channel.route);long current=Main.num(channel.route.get("uploaded_bytes"),0);uploaded+=Math.max(0,current-uploadBaseline.getOrDefault(key,current));uploadBaseline.put(key,current);summaries.add(new LinkedHashMap<>(channel.route));}
                Map<String,Object> progress=new LinkedHashMap<>(completed);progress.put("status","seeding");progress.put("seeding_elapsed_seconds",TimeUnit.NANOSECONDS.toSeconds(elapsed));progress.put("uploaded_bytes",uploaded);progress.put("seeding_ratio_actual",uploaded/(double)Math.max(1,Main.num(completed.get("size"),1)));progress.put("routes",summaries);update.accept(progress);Thread.sleep(250);
            }
            if(stop.getAsBoolean())outcome="paused";else if(!Boolean.TRUE.equals(options.get().get("seeding_enabled")))outcome="disabled";
            completed.put("uploaded_bytes",uploaded);completed.put("seeding_outcome",outcome);completed.put("status",stop.getAsBoolean()?"paused":"complete");
        }finally{closeChannels(new ArrayList<>(seeds.values()));}
    }
    private static Priority[] priorities(PieceScheduler queue,int route,int count) {
        Priority[] result=new Priority[count];Arrays.fill(result,Priority.IGNORE);
        for(int piece:queue.pieces(route))result[piece]=Priority.values()[queue.priority(piece)];
        return result;
    }
    private static String routeKey(Map<String,Object> route){return string(route.get("type"))+":"+string(route.get("gateway"));}
    private static List<Map<String,Object>> desiredRoutes(Supplier<List<Map<String,Object>>> supplier)throws IOException {
        List<Map<String,Object>> supplied=supplier.get();
        if(supplied==null||supplied.size()>32)throw new IOException("Choose between 1 and 32 gateways");
        List<Map<String,Object>> result=new ArrayList<>();Set<String> unique=new HashSet<>();
        for(Map<String,Object> source:supplied) {
            if(source==null||!"gateway".equals(source.get("type")))throw new IOException("Torrent поддерживает только выбранные шлюзы");
            if(Boolean.FALSE.equals(source.get("enabled")))continue;
            Map<String,Object> route=new LinkedHashMap<>(source);String gateway=string(route.get("gateway"));
            String[] octets=gateway.split("\\.",-1);
            if(octets.length!=4)throw new IOException("Некорректный шлюз torrent");
            for(String octet:octets)if(!octet.matches("[0-9]{1,3}")||Integer.parseInt(octet)>255)throw new IOException("Некорректный шлюз torrent");
            if(!unique.add(routeKey(route)))throw new IOException("Некорректный шлюз torrent");
            result.add(route);
        }
        return result;
    }
    private static void reconcile(List<Channel> channels,List<Map<String,Object>> history,PieceScheduler queue,List<Map<String,Object>> desired,Map<String,Long> retryAt,Set<Path> ownedStorage,Path scratch,TorrentInfo ti,String source,Resolver resolver,BooleanSupplier stop,TransferLimits limits)throws Exception {
        Set<String> keys=new HashSet<>();for(Map<String,Object> route:desired)keys.add(routeKey(route));
        for(Map<String,Object> route:history) {
            route.put("enabled",keys.contains(routeKey(route)));
            if(channels.stream().noneMatch(channel->channel!=null&&routeKey(channel.route).equals(routeKey(route))))route.put("active",false);
            if(!keys.contains(routeKey(route))){route.put("stalled",false);retryAt.remove(routeKey(route));}
        }
        for(int index=0;index<channels.size();index++) {
            Channel channel=channels.get(index);
            if(channel!=null&&!keys.contains(routeKey(channel.route)))retire(channels,index,queue);
        }
        for(Map<String,Object> requested:desired) {
            if(stop.getAsBoolean())return;
            String key=routeKey(requested);Map<String,Object> route=null;
            for(Map<String,Object> saved:history)if(key.equals(routeKey(saved))){route=saved;break;}
            if(route==null){route=new LinkedHashMap<>(requested);route.put("active",false);history.add(route);}
            route.put("enabled",true);if(requested.containsKey("name"))route.put("name",requested.get("name"));
            boolean active=false;int free=-1;
            for(int index=0;index<channels.size();index++) {
                if(channels.get(index)==null){if(free<0)free=index;}
                else if(key.equals(routeKey(channels.get(index).route)))active=true;
            }
            if(active||free<0||System.nanoTime()<retryAt.getOrDefault(key,0L))continue;
            // A new generation reuses only its own closed storage; received totals remain historical.
            for(String field:List.of("id","virtual_ip","gateway","type"))if(requested.containsKey(field))route.put(field,requested.get(field));
            Path gatewayRoot=scratch.resolve("gateway-"+string(route.get("gateway"))),storage=gatewayRoot.resolve("files");
            Channel channel=null;
            try {
                safeDirectories(storage);channel=new Channel(route,storage,gatewayRoot.resolve("resume.bin"),resolver,true);
                channel.add(ti,new Priority[0],source);channel.bindLimits(limits); // Replaced by an exact piece mask before resuming.
                queue.enable(free,Main.num(route.get("received_bytes"),0),System.nanoTime(),key);queue.refill(System.nanoTime());
                channel.handle.prioritizePieces(priorities(queue,free,ti.numPieces()));queue.takeDirty(free);
                channels.set(free,channel);ownedStorage.add(storage);route.put("active",true);route.put("stalled",false);route.put("error","");route.put("last_error","");
                channel.handle.resume();retryAt.remove(key);
            }catch(Exception failure) {
                queue.disable(free);
                if(channel!=null)closeChannels(List.of(channel));
                channels.set(free,null);route.put("active",false);route.put("error",Objects.toString(failure.getMessage(),failure.getClass().getSimpleName()));
                route.put("last_error",route.get("error"));
                retryAt.put(key,System.nanoTime()+TimeUnit.SECONDS.toNanos(5));
            }
        }
    }
    private static void retire(List<Channel> channels,int index,PieceScheduler queue)throws Exception {
        Channel channel=channels.get(index);if(channel==null)return;
        queue.disable(index);channel.route.put("active",false);channels.set(index,null);
        try {
            // Close transports first: no tracker, webseed or peer can continue after removal.
            if(channel.network!=null)channel.network.close();
            channel.sampleReceived();
            if(channel.handle!=null&&channel.handle.isValid()) {
                Priority[] ignored=new Priority[channel.ownedInfo.numPieces()];Arrays.fill(ignored,Priority.IGNORE);
                channel.handle.prioritizePieces(ignored);channel.handle.pause();channel.requestResume();
                channel.resumeSignal.await(1,TimeUnit.SECONDS);channel.persistResume();
            }
        }finally{closeChannels(List.of(channel));}
    }
    static final class Channel {
        final Map<String,Object> route;final Path storage,resume;final SessionManager session=new SessionManager();
        TransferLimits limits;final String limitOwner=UUID.randomUUID().toString();
        void bindLimits(TransferLimits value){limits=value;if(value!=null){value.registerNative(limitOwner,string(route.get("gateway")));applyLimit();}}
        void applyLimit(){if(limits!=null)session.downloadRateLimit(limits.nativeLimit(limitOwner));}
        void recordStatus(TorrentStatus status){route.put("peers",status.numPeers());route.put("seeds",status.numSeeds());long current=Math.max(0,status.totalPayloadUpload());long delta=current>=lastUpload?current-lastUpload:current;lastUpload=current;if(delta>0)route.put("uploaded_bytes",Main.num(route.get("uploaded_bytes"),0)+delta);route.put("upload_speed_bps",status.uploadPayloadRate());route.put("tracker_status",status.currentTracker().isBlank()?"waiting":"active");}
        TorrentHandle handle;TorrentInfo ownedInfo;final TorrentNetwork network;final AtomicReference<String> fatal=new AtomicReference<>();
        final AtomicReference<byte[]> resumeData=new AtomicReference<>(),torrentMetadata=new AtomicReference<>();volatile CountDownLatch flushSignal=new CountDownLatch(1),resumeSignal=new CountDownLatch(1);
        long lastPayload,lastUpload;
        Channel(Map<String,Object> route,Path storage,Path resume,Resolver resolver,boolean network)throws Exception {
            this.route=route;this.storage=storage;this.resume=resume;this.network=network?new TorrentNetwork(route,resolver):null;
            session.addListener(new AlertListener(){public int[] types(){return null;}public void alert(Alert<?> alert){
                try {
                    if(alert instanceof TorrentNeedCertAlert certificate){certificate.handle().pause();fatal.set("SSL-torrent требует клиентский сертификат и пока не поддерживается");}
                    else if(alert instanceof CacheFlushedAlert)flushSignal.countDown();
                    else if(alert instanceof SaveResumeDataAlert saved){resumeData.set(AddTorrentParams.writeResumeData(saved.params()).bencode());resumeSignal.countDown();}
                    else if(alert instanceof SaveResumeDataFailedAlert)resumeSignal.countDown();
                    else if(alert instanceof FileErrorAlert || alert instanceof TorrentErrorAlert)fatal.set(alert.message());
                }catch(Throwable failure){fatal.set("Ошибка события BitTorrent: "+failure.getMessage());}
            }});
            try{session.start(new SessionParams(settings(route,this.network==null?r->new InetSocketAddress("127.0.0.1",1):r->this.network.endpoint(),network)));}catch(Exception|LinkageError error){if(this.network!=null)this.network.close();session.stop();throw error;}
        }
        void add(TorrentInfo ti,Priority[] priorities)throws Exception {add(ti,priorities,"");}
        void add(TorrentInfo ti,Priority[] priorities,String source)throws Exception {
            AddTorrentParams params=AddTorrentParams.createInstance();
            if(Files.isRegularFile(resume,LinkOption.NOFOLLOW_LINKS)&&Files.size(resume)<=16L*1024*1024) {
                error_code code=new error_code();add_torrent_params restored=libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(Files.readAllBytes(resume)),code);
                if(code.value()==0)params=new AddTorrentParams(restored);
            }
            ownedInfo=new TorrentInfo(new torrent_info(ti.swig()));
            params.torrentInfo(ownedInfo);params.savePath(storage.toString());params.storageMode(StorageMode.STORAGE_MODE_SPARSE);
            params.flags(TorrentFlags.PAUSED.or_(TorrentFlags.DISABLE_LSD).or_(TorrentFlags.OVERRIDE_TRACKERS).or_(TorrentFlags.OVERRIDE_WEB_SEEDS));
            if(network!=null){
                List<String> trackers=new ArrayList<>();for(AnnounceEntry entry:ti.trackers())trackers.add(entry.url());
                List<String> urls=new ArrayList<>(),httpSeeds=new ArrayList<>();for(WebSeedEntry seed:ti.webSeeds()){if(seed.type()==WebSeedEntry.Type.HTTP_SEED)httpSeeds.add(seed.url());else urls.add(seed.url());}
                if(source.regionMatches(true,0,"magnet:?",0,8)){AddTorrentParams magnet=AddTorrentParams.parseMagnetUri(source);trackers.addAll(magnet.trackers());urls.addAll(magnet.urlSeeds());params.peers(magnet.peers());}
                params.trackers(new ArrayList<>(new LinkedHashSet<>(trackers)));params.urlSeeds(new ArrayList<>(new LinkedHashSet<>(urls)));string_vector vector=new string_vector();for(String seed:httpSeeds)vector.add(network.rewrite(seed));params.swig().set_http_seeds(vector);vector.delete();
                networkParams(params,network);
            }else{params.trackers(List.of());params.urlSeeds(List.of());params.swig().set_http_seeds(new string_vector());}
            ErrorCode error=new ErrorCode(new error_code());handle=new SessionHandle(session.swig()).addTorrent(params,error);
            if(error.value()!=0 || !handle.isValid())throw new IOException("Не удалось добавить torrent: "+error.message());
            handle.prioritizePieces(priorities);
            // The handle is paused here; count only this run, preserving the route's saved historical total.
            route.put("received_bytes",Math.max(0,Main.num(route.get("received_bytes"),Main.num(route.get("bytes"),0))));
            TorrentStatus initial=handle.status(TorrentHandle.QUERY_PIECES);
            try{lastPayload=Math.max(0,initial.totalPayloadDownload());lastUpload=Math.max(0,initial.totalPayloadUpload());}finally{initial.swig().delete();}
        }
        void recordReceived(long current) {
            current=Math.max(0,current);
            long delta=current>=lastPayload?current-lastPayload:current;
            lastPayload=current;
            if(delta>0)route.put("received_bytes",Main.num(route.get("received_bytes"),0)+delta);
        }
        void sampleReceived() {
            if(handle==null||!handle.isValid())return;
            // The no-argument status() is a cached wrapper and must never be manually deleted.
            TorrentStatus status=handle.status(TorrentHandle.QUERY_PIECES);
            try{recordReceived(status.totalPayloadDownload());recordStatus(status);}finally{status.swig().delete();}
        }
        void observePieces(PieceScheduler queue,int route,long now) {
            Map<Integer,Long> bytes=pieceProgress();
            for(int piece:queue.pieces(route))queue.observePiece(route,piece,bytes.getOrDefault(piece,0L),now);
        }
        Map<Integer,Long> pieceProgress() {
            Map<Integer,Long> bytes=new HashMap<>();partial_piece_info_vector partial=handle.swig().get_download_queue();
            try {
                // The wrapper omits native block pointers. Copy aggregate block counts immediately.
                for(int index=0;index<partial.size();index++) {
                    partial_piece_info piece=partial.get(index);
                    bytes.put(piece.getPiece_index(),((long)piece.getFinished()+piece.getWriting())*16384L);
                }
            }finally{partial.delete();}
            peer_info_vector peers=new peer_info_vector();
            try {
                handle.swig().get_peer_info(peers);
                for(int index=0;index<peers.size();index++) {
                    peer_info peer=peers.get(index);int piece=peer.getDownloading_piece_index();
                    if(piece>=0)bytes.merge(piece,(long)Math.max(0,peer.getDownloading_progress()),Long::sum);
                }
            }finally{peers.delete();}
            return bytes;
        }
        void flush(BooleanSupplier stop)throws Exception {
            flushSignal=new CountDownLatch(1);handle.flushCache();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(!stop.getAsBoolean()&&System.nanoTime()<end)if(flushSignal.await(100,TimeUnit.MILLISECONDS))return;
            if(!stop.getAsBoolean())throw new IOException("Не удалось записать части torrent на диск");
        }
        void requestResume(){if(handle!=null&&handle.isValid()){resumeSignal=new CountDownLatch(1);handle.saveResumeData(TorrentHandle.FLUSH_DISK_CACHE.or_(TorrentHandle.SAVE_INFO_DICT));}}
        void persistResume()throws IOException {byte[] data=resumeData.getAndSet(null);if(data!=null)writeAtomic(resume,data);}
    }
    /** BEP 52: SHA-256 of each actual 16 KiB block, short last block, zero hashes for absent leaves. */
    private static void verifyV2Files(byte[] metadata,TorrentInfo ti,Path storage,Set<Integer> selected,BooleanSupplier stop)throws Exception {
        Map<String,byte[]> roots=TorrentSource.v2Roots(metadata,ti);FileStorage fs=ti.files();
        for(int file:selected){if(stop.getAsBoolean())return;if(fs.fileSize(file)==0)continue;
            String path=TorrentSource.normalize(fs.filePath(file));
            if(!Arrays.equals(roots.get(path),merkleRoot(safeResolve(storage,path),stop)))throw new IOException("SHA-256 Merkle root файла не совпадает с torrent: "+path);
        }
    }
    static byte[] merkleRoot(Path file,BooleanSupplier stop)throws Exception {
        long size=Files.size(file);if(size==0)throw new IOException("Пустой файл не имеет Merkle root");
        long leaves=(size+16383)/16384,capacity=1;while(capacity<leaves)capacity=Math.multiplyExact(capacity,2);
        List<byte[]> stack=new ArrayList<>();MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] block=new byte[16384];
        try(InputStream input=Files.newInputStream(file)){
            for(long leaf=0;leaf<capacity;leaf++){
                if(stop.getAsBoolean())throw new InterruptedIOException("Проверка torrent приостановлена");
                byte[] hash;
                if(leaf<leaves){int expected=(int)Math.min(16384,size-leaf*16384);int read=input.readNBytes(block,0,expected);if(read!=expected)throw new IOException("Файл изменился во время проверки torrent");sha.reset();sha.update(block,0,read);hash=sha.digest();}
                else hash=new byte[32];
                int level=0;long index=leaf;while((index&1)!=0){sha.reset();sha.update(stack.get(level));sha.update(hash);hash=sha.digest();stack.set(level,null);level++;index>>=1;}
                while(stack.size()<=level)stack.add(null);stack.set(level,hash);
            }
        }return stack.get(stack.size()-1);
    }
    private static void verifyAssembled(TorrentInfo ti,Path storage,boolean[] wanted,Map<String,Object> route,Resolver resolver,BooleanSupplier stop)throws Exception {
        Channel verifier=new Channel(route,storage,storage.resolveSibling("verify-resume.bin"),resolver,false);
        try {
            Priority[] priority=new Priority[wanted.length];for(int p=0;p<wanted.length;p++)priority[p]=wanted[p]?Priority.NORMAL:Priority.IGNORE;
            verifier.add(ti,priority);
            verifier.handle.setFlags(TorrentFlags.UPLOAD_MODE.or_(TorrentFlags.DISABLE_DHT).or_(TorrentFlags.OVERRIDE_TRACKERS).or_(TorrentFlags.OVERRIDE_WEB_SEEDS));
            verifier.handle.replaceTrackers(List.of());
            for(WebSeedEntry seed:ti.webSeeds()){verifier.handle.removeUrlSeed(seed.url());verifier.handle.removeHttpSeed(seed.url());}
            verifier.handle.forceRecheck();verifier.handle.resume();long end=System.nanoTime()+TimeUnit.MINUTES.toNanos(30);
            while(!stop.getAsBoolean()) {
                if(verifier.fatal.get()!=null)throw new IOException(verifier.fatal.get());
                TorrentStatus status=verifier.handle.status(TorrentHandle.QUERY_PIECES);PieceIndexBitfield bits=status.pieces();boolean verified=true;
                TorrentStatus.State current;
                try{for(int p=0;p<wanted.length;p++)if(wanted[p]&&(p>=bits.size()||!bits.getBit(p)))verified=false;current=status.state();}finally{bits.swig().delete();status.swig().delete();}
                if(verified)return;
                if(current!=TorrentStatus.State.CHECKING_FILES && current!=TorrentStatus.State.CHECKING_RESUME_DATA &&
                        System.nanoTime()>end-TimeUnit.MINUTES.toNanos(30)+TimeUnit.SECONDS.toNanos(3))
                    throw new IOException("Итоговые части не прошли контроль хешей BitTorrent");
                if(System.nanoTime()>end)throw new IOException("Истекло время проверки torrent");Thread.sleep(100);
            }
        }finally{try{if(verifier.handle!=null&&verifier.handle.isValid())verifier.handle.pause();}finally{verifier.session.stop();}}
    }
    private static List<FileSlice> mapBlock(FileStorage files,int piece,int length){
        file_slice_vector vector=files.swig().map_block(piece,0,length);try{List<FileSlice> slices=new ArrayList<>();for(int i=0;i<vector.size();i++)slices.add(new FileSlice(vector.get(i)));return slices;}finally{vector.delete();}
    }
    private static byte[] readPiece(TorrentInfo ti,Path storage,int piece)throws IOException {
        byte[] data=new byte[ti.pieceSize(piece)];int offset=0;FileStorage fs=ti.files();
        for(FileSlice slice:mapBlock(fs,piece,data.length)) {
            int length=Math.toIntExact(slice.size());
            if(!fs.padFileAt(slice.fileIndex())) {
                Path file=safeResolve(storage,TorrentSource.normalize(fs.filePath(slice.fileIndex())));
                if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Отсутствует часть torrent");
                try(RandomAccessFile input=new RandomAccessFile(file.toFile(),"r")){input.seek(slice.offset());input.readFully(data,offset,length);}
            }offset+=length;
        }if(offset!=data.length)throw new IOException("Некорректная карта частей torrent");return data;
    }
    private static void writePiece(TorrentInfo ti,Path storage,int piece,byte[] data)throws IOException {
        int offset=0;FileStorage fs=ti.files();
        for(FileSlice slice:mapBlock(fs,piece,data.length)) {
            int length=Math.toIntExact(slice.size());
            if(!fs.padFileAt(slice.fileIndex())) {
                Path file=safeResolve(storage,TorrentSource.normalize(fs.filePath(slice.fileIndex())));safeDirectories(file.getParent());
                try(RandomAccessFile output=new RandomAccessFile(file.toFile(),"rw")){output.seek(slice.offset());output.write(data,offset,length);}
            }offset+=length;
        }
    }
    private static boolean verifyV1(TorrentInfo ti,int piece,byte[] data)throws IOException {
        if(!ti.swig().info_hashes().has_v1())return true;Sha1Hash hash=ti.hashForPiece(piece);try{return digest("SHA-1",data).equals(hash.toString());}finally{hash.swig().delete();}
    }
    private static Map<String,Object> progress(TorrentInfo ti,FileStorage fs,Set<Integer> selected,boolean[] wanted,boolean[] complete,Map<String,Object> pieces,List<Map<String,Object>> routes,long total,String status) {
        long done=0;Map<String,Long> bytes=new LinkedHashMap<>(),counts=new LinkedHashMap<>();
        for(int piece=0;piece<complete.length;piece++)if(wanted[piece]&&complete[piece]) {
            long begin=(long)piece*ti.pieceLength(),end=begin+ti.pieceSize(piece);
            for(int file:selected){long fileBegin=fs.fileOffset(file),fileEnd=fileBegin+fs.fileSize(file);done+=Math.max(0,Math.min(end,fileEnd)-Math.max(begin,fileBegin));}
            Object record=pieces.get(Integer.toString(piece));if(record instanceof Map<?,?> proof){String gateway=string(proof.get("gateway"));bytes.merge(gateway,(long)ti.pieceSize(piece),Long::sum);counts.merge(gateway,1L,Long::sum);}
        }
        List<Map<String,Object>> summaries=new ArrayList<>();for(Map<String,Object> route:routes){Map<String,Object> copy=new LinkedHashMap<>(route);copy.remove("endpoint");copy.remove("socks_endpoint");copy.remove("token");copy.put("bytes",bytes.getOrDefault(string(route.get("gateway")),0L));copy.put("chunks",counts.getOrDefault(string(route.get("gateway")),0L));summaries.add(copy);}
        Map<String,Object> result=new LinkedHashMap<>();result.put("status",status);result.put("size",total);result.put("downloaded",Math.min(done,total));result.put("routes",summaries);
        result.put("peers",routes.stream().mapToLong(route->Main.num(route.get("peers"),0)).sum());result.put("seeds",routes.stream().mapToLong(route->Main.num(route.get("seeds"),0)).sum());
        result.put("tracker_states",routes.stream().map(route->Map.of("gateway",string(route.get("gateway")),"status",string(route.get("tracker_status")))).toList());
        result.put("pieces_complete",count(complete,wanted));result.put("pieces_total",countWanted(wanted));return result;
    }
    private static int count(boolean[] complete,boolean[] wanted){int n=0;for(int p=0;p<complete.length;p++)if(complete[p]&&wanted[p])n++;return n;}
    private static int countWanted(boolean[] wanted){int n=0;for(boolean p:wanted)if(p)n++;return n;}
    static Path safeResolve(Path root,String relative)throws IOException {Path result=root.resolve(TorrentSource.normalize(relative)).normalize();if(!result.startsWith(root))throw new IOException("Путь вне папки загрузки");checkNoSymlinks(result);return result;}
    private static void checkNoSymlinks(Path path)throws IOException {for(Path current=path.toAbsolutePath();current!=null;current=current.getParent())if(Files.isSymbolicLink(current))throw new IOException("Символическая ссылка в пути загрузки запрещена");}
    private static void safeDirectories(Path path)throws IOException {checkNoSymlinks(path);Files.createDirectories(path);checkNoSymlinks(path);}
    private static void ensureSparse(Path path,long size)throws IOException {checkNoSymlinks(path);if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)){if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Некорректный промежуточный файл");try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")){file.setLength(size);}return;}try(FileChannel file=FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,StandardOpenOption.SPARSE)){if(size>0){file.position(size-1);file.write(java.nio.ByteBuffer.wrap(new byte[]{0}));}}}
    private static void deleteOwnedStorage(Path storage,Path scratch)throws IOException {Path absolute=storage.toAbsolutePath().normalize();if(!absolute.startsWith(scratch)||!absolute.getParent().getFileName().toString().startsWith("gateway-"))throw new IOException("Некорректная папка частей torrent");if(!Files.exists(absolute,LinkOption.NOFOLLOW_LINKS))return;checkNoSymlinks(absolute);try(var paths=Files.walk(absolute)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList()){checkNoSymlinks(path);if(!path.normalize().startsWith(scratch))throw new IOException("Путь частей torrent вне папки задания");Files.delete(path);}}}
    private static boolean ownedPublished(Path path,String key,Map<String,Object> published)throws IOException {
        Object record=published.get(key);if(!(record instanceof Map<?,?> proof)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||!(proof.get("size") instanceof Number size)||Files.size(path)!=size.longValue())return false;
        Map<String,String> expected=Checksums.values(proof);if(!expected.containsKey("sha256"))return false;
        Map<String,String> actual=Checksums.file(path);for(var entry:expected.entrySet())if(!entry.getValue().equals(actual.get(entry.getKey())))return false;return true;
    }
    static String digest(String algorithm,byte[] bytes)throws IOException {try{return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));}catch(GeneralSecurityException error){throw new IOException(error);}}
    private static String digestFile(Path path)throws IOException {try{MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream input=Files.newInputStream(path)){byte[] buffer=new byte[1024*1024];int n;while((n=input.read(buffer))>=0)if(n>0)hash.update(buffer,0,n);}return HexFormat.of().formatHex(hash.digest());}catch(GeneralSecurityException error){throw new IOException(error);}}
    private static void writeCheckpoint(Path file,Map<String,Object> state)throws IOException {writeAtomic(file,Json.stringify(state).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void writeAtomic(Path file,byte[] bytes)throws IOException {
        // Pausing/closing interrupts the coordinator. Its last checkpoint must still be durable.
        boolean interrupted=Thread.interrupted();
        try {
            for(int attempt=0;;attempt++)try {
                safeDirectories(file.getParent());checkNoSymlinks(file);Path temp=file.resolveSibling(file.getFileName()+".tmp");checkNoSymlinks(temp);
                try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
                    java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
                }
                try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
                break;
            }catch(ClosedByInterruptException interruptedWrite) {
                interrupted=true;Thread.interrupted();if(attempt>=4)throw interruptedWrite;
            }
        }finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void closeChannels(List<Channel> channels)throws Exception {
        Exception failure=null;for(Channel channel:channels)if(channel!=null)try{channel.route.put("active",false);try{if(channel.network!=null)channel.network.close();}finally{try{channel.session.stop();}finally{if(channel.limits!=null)channel.limits.unregisterNative(channel.limitOwner);}}}catch(Exception error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw failure;
    }
    static void safeDeleteOwned(Path root)throws IOException {if(!root.toAbsolutePath().getFileName().toString().startsWith("multiroute-magnet-"))throw new IOException("Некорректная папка временных метаданных");try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    private static String string(Object value){return value instanceof String s?s:"";}
}
