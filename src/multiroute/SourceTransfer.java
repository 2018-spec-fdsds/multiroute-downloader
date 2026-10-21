package multiroute;

import com.jcraft.jsch.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.net.ssl.*;

/** HTTP downloads and FTP/FTPS/SFTP file operations through selected application gateways. */
public final class SourceTransfer {
    private SourceTransfer() { }
    /** Cancellable upload resource scope. Absolute progress includes a verified remote prefix. */
    interface UploadControl {
        boolean cancelled();
        void attach(AutoCloseable resource);
        void detach(AutoCloseable resource);
        void progress(long bytes) throws IOException;
        void stage(String status) throws IOException;
        default void check() throws IOException {
            if (cancelled() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Transfer cancelled");
        }
    }
    public static Map<String,Object> listRemote(Map<String,Object> route, String url) throws IOException, InterruptedException {
        return RemoteFiles.list(route, url);
    }
    static Map<String,Object> upload(Map<String,Object> route, String url, java.nio.file.Path local,
            String temporary, long size, String sha256, boolean overwrite, UploadControl control) throws IOException, InterruptedException {
        return RemoteFiles.upload(route, url, local, temporary, size, sha256, overwrite, control);
    }
    interface EndpointResolver {URI endpoint(Map<String,Object> route)throws IOException,InterruptedException;}
    static volatile EndpointResolver endpoints=GatewayNetwork::endpoint; // Package-only owned-fixture seam.
    static final class MissingSourceException extends Transport.NonRetryableException {MissingSourceException(){super("Source file not found");}}
    public static boolean handles(String url){try{return Set.of("ftp","ftps","sftp","webdavs").contains(validate(url).getScheme().toLowerCase(Locale.ROOT));}catch(IOException error){return false;}}
    public static URI validate(String text)throws IOException {
        URI uri;try{uri=URI.create(text.trim());}catch(RuntimeException error){throw new IOException("Invalid source URL");}
        String scheme=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);
        if(!Set.of("http","https","ftp","ftps","sftp","webdavs").contains(scheme)||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null||uri.getPort()==0||uri.getPort()>65535)
            throw new IOException("Expected HTTP, HTTPS, FTP, FTPS, SFTP or secure WebDAV URL without embedded credentials");
        if(uri.getPath()==null||uri.getPath().chars().anyMatch(c->c<32||c==127)||uri.getPath().contains("\\"))throw new IOException("Invalid source path");
        if(Set.of("ftp","ftps","sftp").contains(scheme)&&(uri.getRawQuery()!=null||uri.getPath().isEmpty()||uri.getPath().endsWith("/")))
            throw new IOException("Use a source URL pointing to a file");
        return uri;
    }
    static String httpUrl(String url)throws IOException {
        URI uri=validate(url);
        if(uri.getScheme().equalsIgnoreCase("webdavs"))return "https"+uri.toString().substring(uri.toString().indexOf(':'));
        return url;
    }
    public static HttpResponse<InputStream> open(Map<String,Object> route,String url,long start,long end,String etag,String modified)throws IOException,InterruptedException {
        URI uri=validate(url);String scheme=uri.getScheme().toLowerCase(Locale.ROOT);
        if(start<0||end<start||end-start>=Transport.MAX_SEGMENT)throw new IOException("Invalid or oversized byte range");
        if(Set.of("http","https","webdavs").contains(scheme))return Transport.open(route,httpUrl(url),start,end,etag,modified);
        requireGateway(route);
        return scheme.equals("sftp")?sftp(route,uri,start,end,modified):ftp(route,uri,start,end,modified);
    }
    public static HttpResponse<InputStream> openWhole(Map<String,Object> route,String url)throws IOException,InterruptedException {
        URI uri=validate(url);requireGateway(route);
        if(Set.of("http","https","webdavs").contains(uri.getScheme().toLowerCase(Locale.ROOT)))return Transport.whole(route,httpUrl(url));
        HttpResponse<InputStream> probe=open(route,url,0,0,null,null);long size;String modified;
        try(InputStream ignored=probe.body()){if(probe.statusCode()==200)return empty(uri,uri.getScheme());size=Transport.validate(probe,0,0,null,null,null);modified=probe.headers().firstValue("Last-Modified").orElse("");}
        HttpResponse<InputStream> response=uri.getScheme().equalsIgnoreCase("sftp")?sftp(route,uri,0,size-1,modified):ftp(route,uri,0,size-1,modified);
        Map<String,List<String>> headers=new LinkedHashMap<>(response.headers().map());headers.keySet().removeIf(key->key.equalsIgnoreCase("Content-Range"));
        return new Response(200,uri,response.body(),HttpHeaders.of(headers,(a,b)->true));
    }
    /** Optional publisher checksum files use the same gateway, TLS/SSH checks and origin login. */
    static HttpResponse<InputStream> small(Map<String,Object> route,String url)throws IOException,InterruptedException {
        HttpResponse<InputStream> probe=open(route,url,0,0,null,null);long size;String modified;
        try(InputStream ignored=probe.body()){if(probe.statusCode()==200)return probe;size=Transport.validate(probe,0,0,null,null,null);modified=probe.headers().firstValue("Last-Modified").orElse("");}
        if(size>1024*1024)throw new IOException("Checksum sidecar exceeds 1 MiB");return open(route,url,0,size-1,null,modified);
    }
    public static Map<String,Object> inspect(Map<String,Object> route,String url)throws IOException,InterruptedException {
        URI uri=validate(url);requireGateway(route);HttpResponse<InputStream> response=open(route,url,0,0,null,null);
        try(InputStream body=response.body()) {
            long size=-1;boolean range=response.statusCode()==206;
            if(range)size=Transport.validate(response,0,0,null,null,null);
            else if(response.statusCode()==200){try{size=Long.parseLong(response.headers().firstValue("Content-Length").orElse("-1"));}catch(NumberFormatException ignored){}}
            else throw new Transport.NonRetryableException("Cannot inspect source: status "+response.statusCode());
            String sha=Checksum.fromHeaders(response).sha256();
            Map<String,Object> result=new LinkedHashMap<>();result.put("kind","file");result.put("source_url",url);result.put("size",size);result.put("range_support",range);result.put("etag",response.headers().firstValue("ETag").orElse(""));result.put("modified",response.headers().firstValue("Last-Modified").orElse(""));result.put("sha256",sha);result.put("checksum_source",sha.isEmpty()?"":"Repr-Digest");result.put("protocol",uri.getScheme().toLowerCase(Locale.ROOT));return result;
        }
    }
    private static void requireGateway(Map<String,Object> route)throws IOException {if(route==null||!"gateway".equals(route.get("type")))throw new Transport.NonRetryableException("This source requires a selected application gateway");}
    /** The only actual TCP connect is to the selected helper's loopback HTTP CONNECT listener. */
    static Socket connect(Map<String,Object> route,String host,int port)throws IOException,InterruptedException {
        requireGateway(route);if(host==null||!host.matches("[A-Za-z0-9.\\-:\\[\\]]+")||port<1||port>65535)throw new IOException("Invalid source endpoint");
        URI endpoint=endpoints.endpoint(route);
        if(!"http".equals(endpoint.getScheme())||!"127.0.0.1".equals(endpoint.getHost())||endpoint.getPort()<1)throw new IOException("Gateway proxy must be loopback HTTP");
        Socket socket=new Socket(java.net.Proxy.NO_PROXY);long started=System.nanoTime();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1",endpoint.getPort()),15000);socket.setSoTimeout(1000);socket.setTcpNoDelay(true);
            String authority=host.contains(":")?"["+host.replace("[","").replace("]","")+"]:"+port:host+":"+port;
            socket.getOutputStream().write(("CONNECT "+authority+" HTTP/1.1\r\nHost: "+authority+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
            ByteArrayOutputStream header=new ByteArrayOutputStream();int state=0;long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
            while(state!=4){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(System.nanoTime()>deadline)throw new SocketTimeoutException("Gateway CONNECT timed out");int b;try{b=socket.getInputStream().read();}catch(SocketTimeoutException waiting){continue;}if(b<0||header.size()>=16384)throw new IOException("Invalid gateway CONNECT response");header.write(b);state=switch(state){case 0->b==13?1:0;case 1->b==10?2:b==13?1:0;case 2->b==13?3:0;default->b==10?4:0;};}
            String line=header.toString(StandardCharsets.US_ASCII).split("\r\n",2)[0];
            if(!line.matches("HTTP/1\\.[01] 200(?: .*)?")){
                java.util.regex.Matcher status=java.util.regex.Pattern.compile("HTTP/1\\.[01] ([1-5][0-9]{2})(?: .*)?").matcher(line);
                throw new IOException(status.matches()?"Gateway refused source tunnel (HTTP "+status.group(1)+")":"Gateway refused source tunnel");
            }
            socket.setSoTimeout(60000);return socket;
        }catch(IOException|InterruptedException error){
            if(error instanceof IOException)NetworkDiagnostics.requestFailure(route,NetworkDiagnostics.uri("tcp://"+(host.contains(":")?"["+host.replace("[","").replace("]","")+"]":host)+":"+port),"Source tunnel",started,error);
            try{socket.close();}catch(IOException ignored){}throw error;
        }
    }
    static SSLSocket tls(Socket raw,String host,int port)throws IOException {
        SSLSocket secure=(SSLSocket)TlsTrust.context().getSocketFactory().createSocket(raw,host,port,true);
        SSLParameters parameters=secure.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");secure.setSSLParameters(parameters);secure.setSoTimeout(15000);
        try{secure.startHandshake();secure.setSoTimeout(60000);return secure;}catch(IOException error){try{secure.close();}catch(IOException ignored){}throw error;}
    }
    private static HttpResponse<InputStream> ftp(Map<String,Object> route,URI uri,long start,long end,String modified)throws IOException,InterruptedException {
        Ftp control=new Ftp(route,uri);
        try {
            Map<String,Object> auth=SourceAuth.forUri(uri,route);String user=(String)auth.getOrDefault("username","anonymous"),pass=(String)auth.getOrDefault("password","MultiRoute-Downloader@");
            control.login(user,pass);control.expect("TYPE I",200);
            String path=uri.getPath();Reply size=control.command("SIZE "+path);if(size.code==550)throw new MissingSourceException();if(size.code!=213)throw new Transport.NonRetryableException("FTP server did not provide the file size");
            long total;try{total=Long.parseLong(size.text.trim());}catch(NumberFormatException error){throw new IOException("Invalid FTP file size");}
            if(total==0){control.close();return empty(uri,"ftp");}if(total<=end)throw new IOException("Source range exceeds file size");
            Reply mdtm=control.command("MDTM "+path);String timestamp=mdtm.code==213?mdtm.text.trim():"";
            if(!timestamp.isEmpty()&&!timestamp.matches("[0-9]{14}(?:\\.[0-9]+)?"))throw new IOException("Invalid FTP modification time");
            if(modified!=null&&!modified.isBlank()&&!modified.equals(timestamp))throw new IOException("Remote Last-Modified changed or is missing");
            int dataPort=control.passivePort();Socket data=connect(route,uri.getHost(),dataPort);
            try {
                if(start>0)control.expect("REST "+start,350);
                Reply retr=control.command("RETR "+path);if(retr.code!=125&&retr.code!=150)throw new IOException("FTP server refused the file transfer");
                if(control.secure)data=tls(data,uri.getHost(),dataPort);
                Socket finalData=data;InputStream stream=new Limited(data.getInputStream(),end-start+1,()->{try{finalData.close();}finally{control.close();}});
                return response(uri,stream,start,end,total,timestamp,"ftp");
            }catch(IOException error){try{data.close();}catch(IOException ignored){}throw error;}
        }catch(IOException|InterruptedException error){control.close();throw error;}
    }
    record Reply(int code,String text) { }
    static final class Ftp implements AutoCloseable {
        Socket socket;BufferedReader reader;BufferedWriter writer;final boolean secure;final URI uri;
        Ftp(Map<String,Object> route,URI uri)throws IOException,InterruptedException {
            this.uri=uri;secure=uri.getScheme().equalsIgnoreCase("ftps");socket=connect(route,uri.getHost(),uri.getPort()<0?21:uri.getPort());
            try {streams();Reply greeting=read();if(greeting.code!=220)throw new IOException("FTP server refused connection");
                if(secure){expect("AUTH TLS",234);socket=tls(socket,uri.getHost(),uri.getPort()<0?21:uri.getPort());streams();expect("PBSZ 0",200);expect("PROT P",200);}
            }catch(IOException error){close();throw error;}
        }
        void streams()throws IOException{socket.setSoTimeout(1000);reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));}
        void login(String user,String password)throws IOException {
            Reply login=command("USER "+user);if(login.code==331)login=command("PASS "+password);
            if(login.code!=230)throw new Transport.NonRetryableException("FTP authentication failed");
        }
        void expect(String text,int code)throws IOException{if(command(text).code!=code)throw new IOException("FTP server rejected "+text.split(" ",2)[0]);}
        Reply command(String text)throws IOException{if(text.length()>8192||text.chars().anyMatch(c->c<32||c==127))throw new IOException("Invalid FTP command");writer.write(text+"\r\n");writer.flush();return read();}
        Reply read()throws IOException {
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            String first=line(deadline);if(!first.matches("[0-9]{3}[ -].*"))throw new IOException("Invalid FTP reply");int code=Integer.parseInt(first.substring(0,3));String text=first.substring(4);
            if(first.charAt(3)=='-'){for(int count=0;;count++){if(count>=100)throw new IOException("FTP reply too large");String next=line(deadline);if(next.startsWith(first.substring(0,3)+" ")){text=next.substring(4);break;}}}
            return new Reply(code,text);
        }
        String line(long deadline)throws IOException{StringBuilder line=new StringBuilder();while(line.length()<8192){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Source connection interrupted");if(System.nanoTime()>deadline)throw new SocketTimeoutException("FTP reply timed out");int c;try{c=reader.read();}catch(SocketTimeoutException waiting){continue;}if(c<0)throw new EOFException("FTP connection closed");if(c=='\n')return line.toString().replaceAll("\r$","");line.append((char)c);}throw new IOException("FTP reply too large");}
        int passivePort()throws IOException {
            Reply reply=command("EPSV");if(reply.code==229){var match=java.util.regex.Pattern.compile("\\((.)(?:\\1){2}([0-9]+)\\1\\)").matcher(reply.text);if(!match.find())throw new IOException("Invalid FTP EPSV reply");return checkedPort(match.group(2));}
            reply=command("PASV");if(reply.code!=227)throw new IOException("FTP server requires passive mode");
            var match=java.util.regex.Pattern.compile("\\(([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3})\\)").matcher(reply.text);if(!match.find())throw new IOException("Invalid FTP PASV reply");
            for(int i=1;i<=6;i++)if(Integer.parseInt(match.group(i))>255)throw new IOException("Invalid FTP PASV reply");
            // PASV addresses are never used: the data connection retains the control source host.
            return checkedPort(Integer.toString(Integer.parseInt(match.group(5))*256+Integer.parseInt(match.group(6))));
        }
        int checkedPort(String value)throws IOException{try{int port=Integer.parseInt(value);if(port<1||port>65535)throw new NumberFormatException();return port;}catch(NumberFormatException error){throw new IOException("Invalid FTP passive port");}}
        public void close(){try{socket.close();}catch(IOException ignored){}}
    }
    private static HttpResponse<InputStream> sftp(Map<String,Object> route,URI uri,long start,long end,String modified)throws IOException,InterruptedException {
        Map<String,Object> auth=SourceAuth.forUri(uri,route);String fingerprint=(String)auth.getOrDefault("host_key_sha256","");
        if(fingerprint.isEmpty()||!(auth.get("username") instanceof String username))throw new Transport.NonRetryableException("Configure SFTP credentials and the verified server host-key fingerprint first");
        JSch client=new JSch();client.setHostKeyRepository(new PinnedKey(fingerprint));Session session=null;ChannelSftp channel=null;
        try {
            session=client.getSession(username,uri.getHost(),uri.getPort()<0?22:uri.getPort());
            session.setConfig("StrictHostKeyChecking","yes");session.setConfig("PreferredAuthentications","password,keyboard-interactive");
            session.setPassword(((String)auth.getOrDefault("password","")).getBytes(StandardCharsets.UTF_8));
            session.setTimeout(60000);session.setSocketFactory(new com.jcraft.jsch.SocketFactory(){public Socket createSocket(String host,int port)throws IOException{try{return connect(route,host,port);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IOException("Source connection interrupted");}}public InputStream getInputStream(Socket socket)throws IOException{return socket.getInputStream();}public OutputStream getOutputStream(Socket socket)throws IOException{return socket.getOutputStream();}});
            session.connect(15000);channel=(ChannelSftp)session.openChannel("sftp");channel.connect(15000);
            SftpATTRS attrs=channel.stat(RemoteFiles.literal(uri.getPath()));if(attrs.isDir()||!attrs.isReg())throw new IOException("SFTP source is not a regular file");long total=attrs.getSize();if(total==0){channel.disconnect();session.disconnect();return empty(uri,"sftp");}if(total<=end)throw new IOException("Source range exceeds file size");
            String timestamp=Long.toString(Integer.toUnsignedLong(attrs.getMTime()));if(modified!=null&&!modified.isBlank()&&!modified.equals(timestamp))throw new IOException("Remote Last-Modified changed or is missing");
            ChannelSftp finalChannel=channel;Session finalSession=session;InputStream raw=channel.get(RemoteFiles.literal(uri.getPath()),null,start);
            InputStream stream=new Limited(raw,end-start+1,()->{try{raw.close();}finally{finalChannel.disconnect();finalSession.disconnect();}});
            return response(uri,stream,start,end,total,timestamp,"sftp");
        }catch(JSchException|SftpException error){if(channel!=null)channel.disconnect();if(session!=null)session.disconnect();if(error instanceof SftpException missing&&missing.id==ChannelSftp.SSH_FX_NO_SUCH_FILE)throw new MissingSourceException();throw new Transport.NonRetryableException("SFTP connection, host-key verification or authentication failed",error);}
        catch(IOException error){if(channel!=null)channel.disconnect();if(session!=null)session.disconnect();throw error;}
    }
    static final class PinnedKey implements HostKeyRepository {
        final byte[] expected;PinnedKey(String fingerprint){expected=Base64.getDecoder().decode(fingerprint.substring(7));}
        public int check(String host,byte[] key){try{return MessageDigest.isEqual(expected,MessageDigest.getInstance("SHA-256").digest(key))?OK:CHANGED;}catch(Exception error){return CHANGED;}}
        public void add(HostKey key,UserInfo info){}public void remove(String host,String type){}public void remove(String host,String type,byte[]key){}public String getKnownHostsRepositoryID(){return "application-origin-pinned-sha256";}public HostKey[] getHostKey(){return new HostKey[0];}public HostKey[] getHostKey(String host,String type){return new HostKey[0];}
    }
    interface CloseAction {void close()throws IOException;}
    private static final class Limited extends FilterInputStream {
        long remaining;boolean closed;final CloseAction action;
        Limited(InputStream input,long length,CloseAction action){super(input);remaining=length;this.action=action;}
        public int read()throws IOException{if(remaining==0)return -1;int b=super.read();if(b<0)throw new EOFException("Source returned an incomplete range");remaining--;return b;}
        public int read(byte[]bytes,int offset,int length)throws IOException{Objects.checkFromIndexSize(offset,length,bytes.length);if(length==0)return 0;if(remaining==0)return -1;int count=in.read(bytes,offset,(int)Math.min(length,remaining));if(count<0)throw new EOFException("Source returned an incomplete range");remaining-=count;return count;}
        public void close()throws IOException{if(!closed){closed=true;action.close();}}
    }
    private static HttpResponse<InputStream> response(URI uri,InputStream stream,long start,long end,long total,String modified,String protocol){
        Map<String,List<String>> headers=new LinkedHashMap<>();headers.put("Content-Range",List.of("bytes "+start+"-"+end+"/"+total));headers.put("Content-Length",List.of(Long.toString(end-start+1)));headers.put("Content-Type",List.of("application/octet-stream"));headers.put("X-MultiRoute-Source-Protocol",List.of(protocol));if(!modified.isBlank())headers.put("Last-Modified",List.of(modified));
        return new Response(206,uri,stream,HttpHeaders.of(headers,(a,b)->true));
    }
    private static HttpResponse<InputStream> empty(URI uri,String protocol){return new Response(200,uri,InputStream.nullInputStream(),HttpHeaders.of(Map.of("Content-Length",List.of("0"),"Content-Type",List.of("application/octet-stream"),"X-MultiRoute-Source-Protocol",List.of(protocol)),(a,b)->true));}
    private record Response(int statusCode,URI uri,InputStream body,HttpHeaders headers)implements HttpResponse<InputStream>{public HttpRequest request(){return HttpRequest.newBuilder(URI.create("https://source.invalid/")).build();}public Optional<HttpResponse<InputStream>> previousResponse(){return Optional.empty();}public Optional<SSLSession> sslSession(){return Optional.empty();}public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}}
}
