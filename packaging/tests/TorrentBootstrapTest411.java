package multiroute;

import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.swig.settings_pack;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Owned SOCKS RESOLVE servers only. No native session, OS DNS, capture helper or real gateway. */
public final class TorrentBootstrapTest411 {
    private static final List<String> checks = new ArrayList<>();
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks.add(reason);}
    private static final Map<String,Object> ROUTE=Map.of("type","gateway","gateway","192.168.1.2");
    private static final class Server implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,16,InetAddress.getByAddress(new byte[]{127,0,0,1}));
        final ExecutorService workers=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"owned-bootstrap411");t.setDaemon(true);return t;});
        final Set<Socket> sockets=ConcurrentHashMap.newKeySet();final List<String> names=new CopyOnWriteArrayList<>();
        final AtomicInteger accepted=new AtomicInteger(),closedPeers=new AtomicInteger(),active=new AtomicInteger(),maximum=new AtomicInteger();
        final CountDownLatch requests=new CountDownLatch(5);final String mode;final byte[] address;volatile boolean stopped;
        Server(String mode,byte[] address)throws Exception{this.mode=mode;this.address=address;workers.submit(()->{while(!stopped)try{Socket socket=server.accept();sockets.add(socket);accepted.incrementAndGet();workers.submit(()->serve(socket));}catch(IOException done){if(!stopped)throw new UncheckedIOException(done);}});}
        InetSocketAddress endpoint(){return new InetSocketAddress("127.0.0.1",server.getLocalPort());}
        private void serve(Socket socket){
            try(socket){socket.setSoTimeout(3000);DataInputStream in=new DataInputStream(socket.getInputStream());OutputStream out=socket.getOutputStream();
                if(in.readUnsignedByte()!=5||in.readUnsignedByte()!=1||in.readUnsignedByte()!=0)throw new IOException("Owned greeting mismatch");
                if(mode.equals("greeting")){out.write(new byte[]{5,(byte)255});out.flush();return;}out.write(new byte[]{5,0});out.flush();
                if(in.readUnsignedByte()!=5||in.readUnsignedByte()!=0xf0||in.readUnsignedByte()!=0||in.readUnsignedByte()!=3)throw new IOException("Owned command mismatch");
                int n=in.readUnsignedByte();byte[] raw=in.readNBytes(n);if(raw.length!=n||in.readUnsignedShort()!=0)throw new IOException("Owned domain/port mismatch");
                String name=new String(raw,StandardCharsets.US_ASCII);names.add(name);requests.countDown();int current=active.incrementAndGet();maximum.accumulateAndGet(current,Math::max);
                try{
                    if(mode.equals("stall")){if(in.read()==-1)closedPeers.incrementAndGet();return;}
                    if(mode.equals("concurrent"))requests.await(1200,TimeUnit.MILLISECONDS);
                    if(mode.equals("eof")){out.write(new byte[]{5,0,0});out.flush();return;}
                    byte[] response=new byte[]{5,0,0,1,address[0],address[1],address[2],address[3],0,0};
                    if(mode.equals("version"))response[0]=4;if(mode.equals("reserved"))response[2]=1;if(mode.equals("type"))response[3]=4;if(mode.equals("port"))response[9]=1;if(mode.equals("refused"))response[1]=4;
                    out.write(response);out.flush();
                }finally{active.decrementAndGet();}
            }catch(Exception expectedClose){}finally{sockets.remove(socket);}
        }
        public void close()throws Exception{stopped=true;server.close();for(Socket socket:sockets)socket.close();workers.shutdownNow();workers.awaitTermination(2,TimeUnit.SECONDS);}
    }
    private static byte[] ip(int a,int b,int c,int d){return new byte[]{(byte)a,(byte)b,(byte)c,(byte)d};}
    public static void main(String[] args)throws Exception{
        Main.configureUtf8Console();Path own=Path.of(args[0]).toAbsolutePath();Files.createDirectories(own);AppLog.initialize(own.resolve("profile"));
        SourceTransfer.EndpointResolver saved=SourceTransfer.endpoints;AtomicInteger fallback=new AtomicInteger();
        SourceTransfer.endpoints=route->{fallback.incrementAndGet();throw new AssertionError("Real gateway fallback forbidden");};
        try{
            check(TorrentBootstrap.DEFAULT_NODES.equals(List.of("dht.libtorrent.org:25401","router.bittorrent.com:6881","router.utorrent.com:6881","dht.transmissionbt.com:6881","router.silotis.us:6881")),"Exact bundled jlibtorrent wrapper bootstrap defaults preserved");
            try(Server server=new Server("concurrent",ip(93,184,216,34))){
                List<String> nodes=TorrentEngine.DEFAULT_RESOLVER.bootstrap(ROUTE,server.endpoint());
                check(new HashSet<>(nodes).equals(Set.of("93.184.216.34:25401","93.184.216.34:6881")),"Owned RESOLVE returns public numeric addresses with original DHT ports and deduplication");
                check(server.names.size()==5&&server.maximum.get()==5,"All five gateway DNS lookups run concurrently within one route budget");
                check(new HashSet<>(server.names).equals(Set.of("dht.libtorrent.org","router.bittorrent.com","router.utorrent.com","dht.transmissionbt.com","router.silotis.us")),"Wire uses exact ASCII domains, command F0, no destination TCP and port zero");
            }
            try(Server first=new Server("ok",ip(93,184,216,34));Server second=new Server("ok",ip(91,198,174,192))){
                List<String> a=TorrentBootstrap.resolve(ROUTE,first.endpoint(),List.of("owned.invalid:6881"),1000);
                List<String> b=TorrentBootstrap.resolve(Map.of("gateway","192.168.1.3"),second.endpoint(),List.of("owned.invalid:6881"),1000);
                check(a.equals(List.of("93.184.216.34:6881"))&&b.equals(List.of("91.198.174.192:6881")),"Independent selected gateway endpoints retain DNS affinity");
            }
            for(String mode:List.of("greeting","version","reserved","type","port","refused","eof"))try(Server server=new Server(mode,ip(93,184,216,34))){
                check(TorrentBootstrap.resolve(ROUTE,server.endpoint(),List.of("owned.invalid:6881"),1000).isEmpty(),"Reject malformed or unsuccessful native RESOLVE response: "+mode);
            }
            for(byte[] address:List.of(ip(0,1,2,3),ip(10,0,0,1),ip(127,0,0,1),ip(169,254,1,2),ip(172,16,1,2),ip(192,168,1,1),ip(100,64,0,1),ip(224,0,0,1),ip(198,18,0,1),ip(192,0,2,1),ip(198,51,100,1),ip(203,0,113,1)))try(Server server=new Server("ok",address)){
                check(TorrentBootstrap.resolve(ROUTE,server.endpoint(),List.of("owned.invalid:6881"),1000).isEmpty(),"Private special or documentation addresses never become bootstrap destinations: "+Arrays.toString(address));
            }
            try(Server server=new Server("stall",ip(93,184,216,34))){
                long start=System.nanoTime();check(TorrentBootstrap.resolve(ROUTE,server.endpoint(),TorrentBootstrap.DEFAULT_NODES,350).isEmpty(),"Unavailable DNS returns no fallback bootstrap names");
                check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<1500,"Five stalled DNS lookups share one deadline rather than five serial timeouts");
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(1);while(server.closedPeers.get()<5&&System.nanoTime()<end)Thread.sleep(5);
                check(server.closedPeers.get()==5,"Deadline closes every owned pending resolver socket");
                AppLog.flush();String timedJournal=Files.readString(own.resolve("profile/logs/network-errors.log"),StandardCharsets.UTF_8);
                for(String node:TorrentBootstrap.DEFAULT_NODES)check(timedJournal.split(java.util.regex.Pattern.quote("server="+node),-1).length-1==1,"Deadline persists exactly one error for unresolved bootstrap node: "+node);
            }
            AppLog.flush();String beforeCancellation=Files.readString(own.resolve("profile/logs/network-errors.log"),StandardCharsets.UTF_8);
            try(Server server=new Server("stall",ip(93,184,216,34))){
                AtomicBoolean interrupted=new AtomicBoolean();Thread task=new Thread(()->{try{TorrentBootstrap.resolve(ROUTE,server.endpoint());}catch(InterruptedException expected){interrupted.set(true);}catch(IOException error){throw new UncheckedIOException(error);}},"owned-cancel-bootstrap");
                task.start();check(server.requests.await(2,TimeUnit.SECONDS),"Cancellation fixture reaches all five owned requests");task.interrupt();task.join(1500);
                check(interrupted.get()&&!task.isAlive(),"Interrupting bootstrap startup promptly cancels the route operation");
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(1);while(server.closedPeers.get()<5&&System.nanoTime()<end)Thread.sleep(5);
                check(server.closedPeers.get()==5,"Cancellation closes all pending owned resolver sockets");
            }
            AppLog.flush();check(Files.readString(own.resolve("profile/logs/network-errors.log"),StandardCharsets.UTF_8).equals(beforeCancellation),"Intentional cancellation adds no misleading DNS error entries");
            for(InetSocketAddress invalid:List.of(InetSocketAddress.createUnresolved("owned-no-lookup.invalid",1),new InetSocketAddress(InetAddress.getByAddress(ip(192,0,2,1)),1))){
                boolean rejected=false;try{TorrentBootstrap.resolve(ROUTE,invalid);}catch(IOException expected){rejected=true;}check(rejected,"Unresolved or non-loopback proxy rejected without OS DNS");
            }
            SettingsPack empty=TorrentEngine.settings(ROUTE,route->new InetSocketAddress("127.0.0.1",1),true);
            check(empty.hasValue(settings_pack.string_types.dht_bootstrap_nodes.swigValue())&&empty.getString(settings_pack.string_types.dht_bootstrap_nodes.swigValue()).isEmpty(),"Custom Resolver explicitly suppresses wrapper hostname defaults without bootstrap side effects");
            check(empty.getBoolean(settings_pack.bool_types.enable_dht.swigValue()),"DHT remains enabled for network sessions");
            try(Server server=new Server("ok",ip(93,184,216,34))){
                AtomicInteger called=new AtomicInteger();TorrentEngine.Resolver resolver=new TorrentEngine.Resolver(){
                    public InetSocketAddress resolve(Map<String,Object> route){return server.endpoint();}
                    public List<String> bootstrap(Map<String,Object> route,InetSocketAddress endpoint)throws Exception{check(endpoint.equals(server.endpoint())&&route==ROUTE,"Relay adapter preserves selected upstream SOCKS endpoint and original route");called.incrementAndGet();return TorrentBootstrap.resolve(route,endpoint,List.of("owned.invalid:6881"),1000);}
                };
                try(TorrentNetwork network=new TorrentNetwork(ROUTE,resolver)){
                    Method method=TorrentEngine.class.getDeclaredMethod("relayResolver",TorrentNetwork.class,TorrentEngine.Resolver.class);method.setAccessible(true);
                    TorrentEngine.Resolver adapter=(TorrentEngine.Resolver)method.invoke(null,network,resolver);SettingsPack actual=TorrentEngine.settings(ROUTE,adapter,true);
                    check(actual.getString(settings_pack.string_types.proxy_hostname.swigValue()).equals("127.0.0.1")&&actual.getInteger(settings_pack.int_types.proxy_port.swigValue())==network.endpoint().getPort(),"Native torrent connects to the Java relay proxy");
                    check(actual.getString(settings_pack.string_types.dht_bootstrap_nodes.swigValue()).equals("93.184.216.34:6881")&&called.get()==1,"Numeric bootstrap provider survives the relay endpoint adapter");
                    TorrentEngine.settings(ROUTE,adapter,false);check(called.get()==1,"Offline final verifier never resolves bootstrap names");
                }
            }
            for(String value:List.of("router.invalid:6881","127.0.0.1:6881","10.0.0.1:6881","203.0.113.1:6881","93.184.216.34:0","093.184.216.34:6881")){
                boolean rejected=false;try{TorrentBootstrap.numericNodes(List.of(value));}catch(IOException expected){rejected=true;}check(rejected,"No hostname private ambiguous or invalid endpoint enters native settings: "+value);
            }
            AppLog.flush();String journal=Files.readString(own.resolve("profile/logs/network-errors.log"),StandardCharsets.UTF_8);
            check(journal.contains("192.168.1.2")&&journal.contains("owned.invalid:6881"),"Failed bootstrap DNS attempts persist with selected gateway and safe host context");
            check(fallback.get()==0,"No real gateway or system resolver fallback used");
            String unicode="Проверка 日本語 العربية é";Path output=own.resolve("Текст_日本語.txt");Files.writeString(output,unicode,StandardCharsets.UTF_8);check(Files.readString(output,StandardCharsets.UTF_8).equals(unicode)&&!journal.contains("\ufffd"),"Own Unicode paths and diagnostic journal retain UTF-8");
            Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("version",Main.VERSION);report.put("checks",checks.size());report.put("cases",checks);report.put("network_scope","owned loopback SOCKS RESOLVE endpoints only");report.put("native_session_started",false);report.put("native_capture_helper_started",false);report.put("real_gateway_fallback_calls",fallback.get());report.put("user_files_read",false);
            Files.writeString(own.resolve("TORRENT-BOOTSTRAP-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println(Json.stringify(report));
        }finally{SourceTransfer.endpoints=saved;AppLog.close();}
    }
}
