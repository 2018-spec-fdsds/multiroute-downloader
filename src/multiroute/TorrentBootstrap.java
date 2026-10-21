package multiroute;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Resolves native DHT bootstrap names only through the selected gateway's local SOCKS endpoint. */
final class TorrentBootstrap {
    // Preserve the defaults of the bundled jlibtorrent 2.0.12.9 SessionManager.
    static final List<String> DEFAULT_NODES = List.of("dht.libtorrent.org:25401", "router.bittorrent.com:6881",
            "router.utorrent.com:6881", "dht.transmissionbt.com:6881", "router.silotis.us:6881");
    static final long BUDGET_MS = 10_000;
    private TorrentBootstrap() { }

    static void loopback(InetSocketAddress endpoint) throws IOException {
        if (endpoint == null || endpoint.isUnresolved() || !endpoint.getAddress().isLoopbackAddress() || endpoint.getPort() < 1)
            throw new IOException("Invalid local SOCKS5 gateway");
    }

    static List<String> resolve(Map<String,Object> route, InetSocketAddress endpoint) throws InterruptedException, IOException {
        return resolve(route, endpoint, DEFAULT_NODES, BUDGET_MS);
    }

    static List<String> resolve(Map<String,Object> route, InetSocketAddress endpoint, List<String> names, long budgetMs)
            throws InterruptedException, IOException {
        loopback(endpoint);
        if (names.size() > DEFAULT_NODES.size() || budgetMs < 1 || budgetMs > BUDGET_MS) throw new IOException("Invalid torrent bootstrap limits");
        List<Node> nodes = new ArrayList<>(); for (String value : names) nodes.add(node(value));
        if (nodes.isEmpty()) return List.of();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMs);
        Set<Socket> sockets = ConcurrentHashMap.newKeySet(); AtomicBoolean cancelled = new AtomicBoolean();
        ExecutorService threads = Executors.newFixedThreadPool(nodes.size(), task -> { Thread thread = new Thread(task,"torrent-bootstrap-dns"); thread.setDaemon(true); return thread; });
        CompletionService<String> completion = new ExecutorCompletionService<>(threads); List<Future<String>> futures = new ArrayList<>();
        List<AtomicBoolean> reported = new ArrayList<>(), succeeded = new ArrayList<>(); boolean timedOut = false;
        Set<String> resolved = new LinkedHashSet<>();
        try {
            for (Node node : nodes) { AtomicBoolean once = new AtomicBoolean(), success = new AtomicBoolean(); reported.add(once); succeeded.add(success); futures.add(completion.submit(() -> {
                long started = System.nanoTime();
                try { String address=lookup(endpoint,node.host,deadline,sockets,cancelled);success.set(true);return address+":"+node.port; }
                catch (IOException error) {
                    if (!cancelled.get() && !Thread.currentThread().isInterrupted() && once.compareAndSet(false,true))
                        NetworkDiagnostics.requestFailure(route, URI.create("https://"+node.host+":"+node.port), "Resolve torrent bootstrap", started, error);
                    return null;
                }
            })); }
            for (int count = 0; count < nodes.size(); count++) {
                long remaining = deadline - System.nanoTime(); if (remaining <= 0) { timedOut=true; break; }
                Future<String> future = completion.poll(remaining, TimeUnit.NANOSECONDS); if (future == null) { timedOut=true; break; }
                try { String value = future.get(); if (value != null) resolved.add(value); }
                catch (ExecutionException failed) { throw new IOException("Torrent bootstrap resolver failed", failed.getCause()); }
            }
            if (timedOut) {
                cancelled.set(true);
                for (int index=0;index<futures.size();index++) if (!succeeded.get(index).get() && reported.get(index).compareAndSet(false,true)) {
                    Node node=nodes.get(index);
                    NetworkDiagnostics.requestFailure(route,URI.create("https://"+node.host+":"+node.port),"Resolve torrent bootstrap",deadline-TimeUnit.MILLISECONDS.toNanos(budgetMs),new SocketTimeoutException("Torrent bootstrap DNS timed out"));
                }
            }
        } finally {
            cancelled.set(true);
            for (Socket socket : sockets) try { socket.close(); } catch (IOException ignored) { }
            for (Future<String> future : futures) future.cancel(true);
            threads.shutdownNow();
        }
        return List.copyOf(resolved);
    }

    private record Node(String host, int port) { }
    private static Node node(String value) throws IOException {
        int colon = value == null ? -1 : value.lastIndexOf(':');
        if (colon < 1) throw new IOException("Invalid torrent bootstrap endpoint");
        String host = value.substring(0,colon); domain(host); int port;
        try { port = Integer.parseInt(value.substring(colon+1)); } catch (NumberFormatException error) { throw new IOException("Invalid torrent bootstrap endpoint"); }
        if (port < 1 || port > 65535) throw new IOException("Invalid torrent bootstrap endpoint");
        return new Node(host,port);
    }
    private static byte[] domain(String host) throws IOException {
        if (host == null || host.isEmpty() || host.length() > 253 || !host.matches("[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*"))
            throw new IOException("Invalid torrent bootstrap hostname");
        for (String label : host.split("\\.")) if (label.length() > 63 || label.startsWith("-") || label.endsWith("-"))
            throw new IOException("Invalid torrent bootstrap hostname");
        return host.getBytes(StandardCharsets.US_ASCII);
    }
    private static int timeout(long deadline) throws SocketTimeoutException {
        long remaining = deadline-System.nanoTime();
        if (remaining <= 0) throw new SocketTimeoutException("Torrent bootstrap DNS timed out");
        return (int)Math.max(1, Math.min(BUDGET_MS, TimeUnit.NANOSECONDS.toMillis(remaining)));
    }
    static String lookup(InetSocketAddress endpoint, String host, long deadline, Set<Socket> sockets, AtomicBoolean cancelled) throws IOException {
        loopback(endpoint); byte[] domain = domain(host);
        try (Socket socket = new Socket()) {
            sockets.add(socket);
            try {
                if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Torrent bootstrap cancelled");
                socket.connect(endpoint, Math.min(2000,timeout(deadline))); socket.setSoTimeout(timeout(deadline));
                DataInputStream input = new DataInputStream(socket.getInputStream()); OutputStream output = socket.getOutputStream();
                output.write(new byte[]{5,1,0}); output.flush();
                if (input.readUnsignedByte()!=5 || input.readUnsignedByte()!=0) throw new IOException("Invalid SOCKS5 bootstrap greeting");
                socket.setSoTimeout(timeout(deadline));
                ByteArrayOutputStream request = new ByteArrayOutputStream(); request.write(new byte[]{5,(byte)0xf0,0,3,(byte)domain.length}); request.write(domain); request.write(new byte[]{0,0});
                output.write(request.toByteArray()); output.flush();
                int version=input.readUnsignedByte(),status=input.readUnsignedByte(),reserved=input.readUnsignedByte(),type=input.readUnsignedByte();
                if (version!=5 || reserved!=0 || type!=1) throw new IOException("Invalid SOCKS5 bootstrap response");
                byte[] address = new byte[4]; input.readFully(address); int port=input.readUnsignedShort();
                if (status!=0) throw new IOException("Gateway could not resolve torrent bootstrap (SOCKS status "+status+")");
                if (port!=0 || !publicIpv4(address)) throw new IOException("Gateway returned an invalid torrent bootstrap address");
                return (address[0]&255)+"."+(address[1]&255)+"."+(address[2]&255)+"."+(address[3]&255);
            } finally { sockets.remove(socket); }
        }
    }
    static boolean publicIpv4(byte[] bytes) {
        if (bytes.length!=4) return false;
        int a=bytes[0]&255,b=bytes[1]&255,c=bytes[2]&255;
        return a>0 && a<224 && a!=10 && a!=127 && !(a==169&&b==254) && !(a==172&&b>=16&&b<=31) && !(a==192&&b==168) && !(a==100&&b>=64&&b<=127)
                && !(a==192&&b==0&&(c==0||c==2)) && !(a==192&&b==88&&c==99) && !(a==198&&(b==18||b==19)) && !(a==198&&b==51&&c==100) && !(a==203&&b==0&&c==113);
    }
    /** Only canonical public IPv4 endpoints reach libtorrent; hostnames never enter its resolver queue. */
    static String numericNodes(List<String> nodes) throws IOException {
        if (nodes == null || nodes.size()>32) throw new IOException("Invalid torrent bootstrap endpoints");
        Set<String> result = new LinkedHashSet<>();
        for (String node : nodes) {
            int colon=node==null?-1:node.lastIndexOf(':'); if(colon<1)throw new IOException("Invalid torrent bootstrap endpoint");
            String host=node.substring(0,colon); String[] fields=host.split("\\.",-1); if(fields.length!=4)throw new IOException("Torrent bootstrap must use public numeric IPv4");
            byte[] address=new byte[4];
            try { for(int i=0;i<4;i++){int n=Integer.parseInt(fields[i]);if(n<0||n>255||!Integer.toString(n).equals(fields[i]))throw new NumberFormatException();address[i]=(byte)n;} }
            catch(NumberFormatException error){throw new IOException("Torrent bootstrap must use public numeric IPv4");}
            if(!publicIpv4(address))throw new IOException("Torrent bootstrap must use public numeric IPv4");
            int port;try{port=Integer.parseInt(node.substring(colon+1));}catch(NumberFormatException error){throw new IOException("Invalid torrent bootstrap endpoint");}
            if(port<1||port>65535)throw new IOException("Invalid torrent bootstrap endpoint");result.add(host+":"+port);
        }
        return String.join(",",result);
    }
}
