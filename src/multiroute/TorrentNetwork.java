package multiroute;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

/** Keeps all tracker/webseed TLS in Java; other BitTorrent TCP and UDP still use the chosen gateway. */
final class TorrentNetwork implements AutoCloseable {
    final Map<String,Object> route;final TorrentEngine.Resolver resolver;final InetSocketAddress upstream;
    final HttpServer relay;final ServerSocket proxy;final ExecutorService threads;
    private final Object closeGate=new Object();
    final Set<Socket> sockets=ConcurrentHashMap.newKeySet();final Set<InputStream> bodies=ConcurrentHashMap.newKeySet();
    final Map<String,URI> sources=new ConcurrentHashMap<>();final AtomicInteger sequence=new AtomicInteger();
    final AtomicReference<String> failure=new AtomicReference<>();volatile boolean closed;
    static final Pattern RANGE=Pattern.compile("bytes=([0-9]+)-([0-9]*)");
    TorrentNetwork(Map<String,Object> route,TorrentEngine.Resolver resolver)throws Exception {
        this.route=route;this.resolver=resolver;upstream=resolver.resolve(route);
        if(upstream==null||upstream.isUnresolved()||!upstream.getAddress().isLoopbackAddress())throw new IOException("Некорректный SOCKS5 шлюза");
        threads=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"torrent-gateway-relay");t.setDaemon(true);return t;});
        HttpServer started=null;ServerSocket bound=null;
        try{started=HttpServer.create(new InetSocketAddress("127.0.0.1",0),16);started.setExecutor(threads);started.createContext("/r/",this::http);started.start();bound=new ServerSocket(0,16,InetAddress.getByName("127.0.0.1"));}
        catch(Exception|LinkageError error){if(started!=null)started.stop(0);if(bound!=null)try{bound.close();}catch(IOException ignored){}threads.shutdownNow();throw error;}
        relay=started;proxy=bound;
        threads.submit(()->{while(!closed)try{Socket socket=proxy.accept();register(socket);threads.submit(()->socks(socket));}catch(IOException error){if(!closed)failure.set("Ошибка локального шлюза BitTorrent");}});
    }
    InetSocketAddress endpoint(){return new InetSocketAddress("127.0.0.1",proxy.getLocalPort());}
    String rewrite(String url)throws IOException {
        URI uri;try{uri=URI.create(url);}catch(IllegalArgumentException bad){throw new IOException("Некорректный URL torrent",bad);}
        String scheme=uri.getScheme();if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme))return url;
        Transport.uri(url);String key=Integer.toString(sequence.incrementAndGet());sources.put(key,uri);
        String path=uri.getRawPath();if(path==null||path.isEmpty())path="/";String tail=path.substring(path.lastIndexOf('/')+1);
        return "http://127.0.0.1:"+relay.getAddress().getPort()+"/r/"+key+"/"+tail+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery());
    }
    String restore(String url){
        try{URI uri=URI.create(url);String[] parts=uri.getRawPath().split("/",4);if(uri.getHost().equals("127.0.0.1")&&uri.getPort()==relay.getAddress().getPort()&&parts.length==4&&parts[1].equals("r")){URI original=sources.get(parts[2]);if(original!=null)return original.toString();}}catch(RuntimeException ignored){}return url;
    }
    private void http(HttpExchange exchange)throws IOException {
        HttpResponse<InputStream> response=null;
        try {
            if(closed||!exchange.getRequestMethod().equals("GET"))throw new IOException("Метод relay не поддерживается");
            String request=exchange.getRequestURI().getRawPath();String[] parts=request.split("/",4);if(parts.length<4)throw new IOException("Неизвестный источник relay");
            URI original=sources.get(parts[2]);if(original==null)throw new IOException("Неизвестный источник relay");
            String path=original.getRawPath();if(path==null||path.isEmpty())path="/";
            String parent=path.substring(0,path.lastIndexOf('/')+1),tail=parts[3];
            if(tail.contains("..")||tail.indexOf('\\')>=0)throw new IOException("Небезопасный запрос webseed");
            String query=exchange.getRequestURI().getRawQuery();String target=original.getScheme()+"://"+original.getRawAuthority()+parent+tail+(query==null?"":"?"+query);
            long start=0,end=Transport.MAX_SEGMENT-1;String range=exchange.getRequestHeaders().getFirst("Range");
            if(range!=null){Matcher m=RANGE.matcher(range);if(!m.matches())throw new IOException("Некорректный диапазон webseed");start=Long.parseLong(m.group(1));end=m.group(2).isEmpty()?Math.addExact(start,Transport.MAX_SEGMENT-1):Long.parseLong(m.group(2));if(end<start)throw new IOException("Некорректный диапазон webseed");}
            long requestEnd=Math.min(end,Math.addExact(start,Transport.MAX_SEGMENT-1));requireOpen();response=range==null?resolver.get(route,target):resolver.open(route,target,start,requestEnd);
            InputStream body=response.body();register(body);
            try(body){
                if(closed)throw new IOException("Шлюз закрыт");int status=response.statusCode();
                if(status>=300&&status<400)throw new IOException("Перенаправление relay не обработано");
                String encoding=response.headers().firstValue("Content-Encoding").orElse("identity");if(!encoding.equalsIgnoreCase("identity"))throw new IOException("Сжатие webseed не поддерживается");
                for(String header:List.of("Content-Type","ETag","Last-Modified","Accept-Ranges"))response.headers().firstValue(header).ifPresent(v->exchange.getResponseHeaders().set(header,v));
                long length=response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if(range!=null&&status==206){
                    String contentRange=response.headers().firstValue("Content-Range").orElse("");Matcher parsed=Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matcher(contentRange);
                    if(!parsed.matches())throw new IOException("Нет Content-Range webseed");long actualStart=Long.parseLong(parsed.group(1)),actualEnd=Long.parseLong(parsed.group(2)),total=Long.parseLong(parsed.group(3));
                    if(actualStart!=start||actualEnd>requestEnd||actualEnd<start||total<=actualEnd)throw new IOException("Неверный Content-Range webseed");
                    long finalEnd=Math.min(end,total-1);length=finalEnd-start+1;exchange.getResponseHeaders().set("Content-Range","bytes "+start+"-"+finalEnd+"/"+total);
                    exchange.sendResponseHeaders(206,length);
                    try(OutputStream output=exchange.getResponseBody()){
                        copy(body,output,actualEnd-start+1);long cursor=actualEnd+1;
                        while(cursor<=finalEnd&&!closed){long chunkEnd=Math.min(finalEnd,Math.addExact(cursor,Transport.MAX_SEGMENT-1));requireOpen();HttpResponse<InputStream> chunk=resolver.open(route,target,cursor,chunkEnd);InputStream more=chunk.body();register(more);try(more){Transport.validate(chunk,cursor,chunkEnd,total,null,null);copy(more,output,chunkEnd-cursor+1);}finally{bodies.remove(more);}cursor=chunkEnd+1;}
                    }
                }else {
                    if(length>Transport.MAX_SEGMENT || status==200&&range!=null&&start!=0)throw new IOException("Источник не поддерживает диапазон webseed");
                    exchange.sendResponseHeaders(status,length<0?0:length);
                    try(OutputStream output=exchange.getResponseBody()){copy(body,output,length<0?Transport.MAX_SEGMENT:length);}
                }
            }finally{bodies.remove(body);}
        }catch(Exception error){
            if(error instanceof javax.net.ssl.SSLException)failure.compareAndSet(null,"Не удалось проверить TLS сертификат источника torrent");
            try{byte[] message="Gateway request failed".getBytes(StandardCharsets.US_ASCII);exchange.sendResponseHeaders(502,message.length);try(OutputStream output=exchange.getResponseBody()){output.write(message);}}catch(IOException ignored){}
        }finally{exchange.close();}
    }
    private void copy(InputStream input,OutputStream output,long length)throws IOException {
        byte[] bytes=new byte[32*1024];long remaining=length;while(remaining>0&&!closed){int n=input.read(bytes,0,(int)Math.min(bytes.length,remaining));if(n<0){if(length<Transport.MAX_SEGMENT)throw new EOFException("Обрыв источника torrent");return;}output.write(bytes,0,n);remaining-=n;}
    }
    private void socks(Socket client){
        Socket remote=null;
        try(client){requireOpen();client.setSoTimeout(15000);DataInputStream input=new DataInputStream(client.getInputStream());OutputStream output=client.getOutputStream();
            if(input.readUnsignedByte()!=5)throw new IOException("SOCKS version");int count=input.readUnsignedByte();boolean noAuth=false;for(byte b:input.readNBytes(count))if(b==0)noAuth=true;if(!noAuth){output.write(new byte[]{5,(byte)255});return;}output.write(new byte[]{5,0});output.flush();
            ByteArrayOutputStream request=new ByteArrayOutputStream();int version=input.readUnsignedByte(),command=input.readUnsignedByte(),reserved=input.readUnsignedByte(),type=input.readUnsignedByte();request.write(new byte[]{(byte)version,(byte)command,(byte)reserved,(byte)type});if(version!=5||reserved!=0||(command!=1&&command!=3))throw new IOException("SOCKS request");
            byte[] raw;String host;if(type==1){raw=input.readNBytes(4);host=InetAddress.getByAddress(raw).getHostAddress();request.write(raw);}else if(type==4){raw=input.readNBytes(16);host=InetAddress.getByAddress(raw).getHostAddress();request.write(raw);}else if(type==3){int size=input.readUnsignedByte();request.write(size);raw=input.readNBytes(size);request.write(raw);host=new String(raw,StandardCharsets.UTF_8);}else throw new IOException("SOCKS host");
            int port=input.readUnsignedShort();request.write(port>>8);request.write(port);
            if(command==1&&host.equals("127.0.0.1")&&port==relay.getAddress().getPort()){
                remote=new Socket();register(remote);remote.connect(new InetSocketAddress("127.0.0.1",port),5000);requireOpen();output.write(new byte[]{5,0,0,1,127,0,0,1,0,0});output.flush();
            }else{
                remote=new Socket();register(remote);remote.connect(upstream,5000);requireOpen();remote.setSoTimeout(15000);DataInputStream response=new DataInputStream(remote.getInputStream());OutputStream forwarded=remote.getOutputStream();forwarded.write(new byte[]{5,1,0});forwarded.flush();if(response.readUnsignedByte()!=5||response.readUnsignedByte()!=0)throw new IOException("SOCKS gateway handshake");requireOpen();forwarded.write(request.toByteArray());forwarded.flush();
                ByteArrayOutputStream reply=new ByteArrayOutputStream();int v=response.readUnsignedByte(),result=response.readUnsignedByte(),r=response.readUnsignedByte(),atyp=response.readUnsignedByte();reply.write(new byte[]{(byte)v,(byte)result,(byte)r,(byte)atyp});int bytes=atyp==1?4:atyp==4?16:atyp==3?response.readUnsignedByte():-1;if(bytes<0)throw new IOException("SOCKS gateway reply");if(atyp==3)reply.write(bytes);reply.write(response.readNBytes(bytes+2));output.write(reply.toByteArray());output.flush();if(result!=0)return;
            }
            Socket destination=remote;requireOpen();client.setSoTimeout(0);remote.setSoTimeout(0);
            Future<?> reverse=threads.submit(()->{try{destination.getInputStream().transferTo(output);client.shutdownOutput();}catch(IOException ignored){}});
            try{input.transferTo(remote.getOutputStream());remote.shutdownOutput();}catch(IOException ignored){}
            try{reverse.get(3,TimeUnit.SECONDS);}catch(Exception ignored){}
        }catch(Exception ignored){}finally{if(remote!=null){sockets.remove(remote);try{remote.close();}catch(IOException ignored){}}sockets.remove(client);}
    }
    private void requireOpen()throws IOException {if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Шлюз закрыт");}
    private void register(Socket socket)throws IOException {
        synchronized(closeGate){if(closed){socket.close();throw new InterruptedIOException("Шлюз закрыт");}sockets.add(socket);}
    }
    private void register(InputStream body)throws IOException {
        synchronized(closeGate){if(closed){body.close();throw new InterruptedIOException("Шлюз закрыт");}bodies.add(body);}
    }
    public void close(){
        synchronized(closeGate){closed=true;try{proxy.close();}catch(IOException ignored){}for(Socket socket:sockets)try{socket.close();}catch(IOException ignored){}}
        // Interrupt pending synchronous Java HTTP calls before closing any response obtained already.
        threads.shutdownNow();relay.stop(0);for(InputStream body:bodies)try{body.close();}catch(IOException ignored){}
        boolean interrupted=Thread.interrupted();
        try{threads.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException stopped){interrupted=true;}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
}
