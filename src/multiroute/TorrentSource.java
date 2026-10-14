package multiroute;

import com.frostwire.jlibtorrent.*;
import java.io.*;
import java.net.URI;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** Bounded metainfo acquisition and validation before native disk storage is created. */
public final class TorrentSource {
    public static final int MAX_METADATA = 8 * 1024 * 1024;
    private static final Map<String,Map<String,Object>> CACHE = new LinkedHashMap<>();
    private TorrentSource() { }
    public static boolean isTorrentSource(String source) {
        if (source == null) return false;
        if (source.regionMatches(true, 0, "magnet:?", 0, 8)) return true;
        try { return URI.create(source).getPath().toLowerCase(Locale.ROOT).endsWith(".torrent"); }
        catch (RuntimeException error) { return source.toLowerCase(Locale.ROOT).endsWith(".torrent"); }
    }
    public static Map<String,Object> inspect(String source, Map<String,Object> route) throws Exception {
        nativeReady();
        try{return inspect(source, route, TorrentEngine::socksEndpoint);}catch(LinkageError unavailable){throw new IOException("Компонент BitTorrent не загружен. Обновите всю папку приложения",unavailable);}
    }
    static Map<String,Object> inspect(String source, Map<String,Object> route, TorrentEngine.Resolver resolver) throws Exception {
        if (source == null || source.isBlank() || source.length() > 64 * 1024) throw new IOException("Некорректная ссылка torrent");
        String key = source + "\n" + route.get("type") + "\n" + route.get("gateway");
        synchronized (CACHE) {
            var found = CACHE.get(key);
            if (found != null && System.currentTimeMillis() - ((Number)found.get("_cached_at")).longValue() < 120000)
                return copy(found);
        }
        byte[] bytes;
        boolean magnet = source.regionMatches(true, 0, "magnet:?", 0, 8);
        if (magnet) bytes = TorrentEngine.magnetMetadata(source, route, resolver);
        else if (source.regionMatches(true,0,"http://",0,7) || source.regionMatches(true,0,"https://",0,8)) {
            // Regular range transport follows secure redirects through the selected gateway.
            var response = Transport.open(route, source, 0, MAX_METADATA-1, null, null);
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200 && response.statusCode()!=206) throw new IOException("Источник torrent: HTTP " + response.statusCode());
                if (!response.headers().firstValue("Content-Encoding").orElse("identity").equalsIgnoreCase("identity")) throw new IOException("Сжатые метаданные torrent не поддерживаются");
                java.util.concurrent.ScheduledExecutorService guard=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"torrent-metadata-timeout");t.setDaemon(true);return t;});
                var timeout=guard.schedule(()->{try{body.close();}catch(IOException ignored){}},20,java.util.concurrent.TimeUnit.SECONDS);
                try {bytes = body.readNBytes(MAX_METADATA + 1);}finally{timeout.cancel(false);guard.shutdownNow();}
                if(response.statusCode()==206) {
                    String range=response.headers().firstValue("Content-Range").orElse("");
                    if(!range.matches("bytes 0-[0-9]+/[0-9]+"))throw new IOException("Источник вернул неполные метаданные torrent");
                    long total=Long.parseLong(range.substring(range.indexOf('/')+1));
                    if(total!=bytes.length)throw new IOException("Метаданные torrent неполны или превышают лимит");
                }
            }
        } else {
            Path file;
            try { file = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source); }
            catch (RuntimeException bad) { throw new IOException("Некорректный путь torrent", bad); }
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > MAX_METADATA)
                throw new IOException("Файл torrent отсутствует или превышает 8 МиБ");
            bytes = Files.readAllBytes(file);
        }
        Map<String,Object> summary = describe(bytes);
        if (magnet) {
            AddTorrentParams reference = AddTorrentParams.parseMagnetUri(source);
            var expected = reference.swig().getInfo_hashes();
            TorrentInfo actual = new TorrentInfo(bytes);
            var found = actual.swig().info_hashes();
            if (expected.has_v1() && (!found.has_v1() || !expected.getV1().to_hex().equals(found.getV1().to_hex())) ||
                expected.has_v2() && (!found.has_v2() || !expected.getV2().to_hex().equals(found.getV2().to_hex())))
                throw new IOException("Метаданные не соответствуют info-hash magnet");
        }
        summary.put("source", source);
        summary.put("torrent_metadata", Base64.getEncoder().encodeToString(bytes));
        synchronized (CACHE) {
            summary.put("_cached_at", System.currentTimeMillis()); CACHE.put(key, summary);
            while (CACHE.size() > 8) CACHE.remove(CACHE.keySet().iterator().next());
        }
        return copy(summary);
    }
    private static Map<String,Object> copy(Map<String,Object> value) {
        Map<String,Object> result = new LinkedHashMap<>(value); result.remove("_cached_at");
        @SuppressWarnings("unchecked") Map<String,Object> detached=(Map<String,Object>)Json.parse(Json.stringify(result));return detached;
    }
    public static Map<String,Object> describe(byte[] bytes) throws IOException {
        nativeReady();
        if (bytes.length == 0 || bytes.length > MAX_METADATA) throw new IOException("Метаданные torrent превышают 8 МиБ");
        Object decoded = new Bdecode(bytes).parse();
        if (!(decoded instanceof Map<?,?> root) || !(root.get("info") instanceof Map<?,?> info))
            throw new IOException("Файл не содержит метаданные BitTorrent");
        String name = text(info.containsKey("name.utf-8") ? info.get("name.utf-8") : info.get("name"));
        component(name);
        if(info.containsKey("ssl-cert"))throw new IOException("SSL-torrent требует клиентский сертификат и пока не поддерживается");
        if(info.containsKey("symlink path") || info.get("attr") instanceof byte[] attr && new String(attr,StandardCharsets.US_ASCII).contains("l")) throw new IOException("Символические ссылки в torrent запрещены");
        Set<String> rawPaths = new HashSet<>();
        if (info.get("files") instanceof List<?> files) {
            if (files.size() > 10000) throw new IOException("Слишком много файлов torrent");
            for (Object item : files) {
                if (!(item instanceof Map<?,?> file)) throw new IOException("Некорректное описание файла");
                Object paths = file.containsKey("path.utf-8") ? file.get("path.utf-8") : file.get("path");
                if (!(paths instanceof List<?> parts) || parts.isEmpty() || parts.size() > 128) throw new IOException("Некорректный путь torrent");
                List<String> strings = new ArrayList<>(); strings.add(name);
                for (Object part : parts) { String s = text(part); component(s); strings.add(s); }
                unique(rawPaths, String.join("/", strings));
                if (file.get("attr") instanceof byte[] attr && new String(attr,StandardCharsets.US_ASCII).contains("l"))
                    throw new IOException("Символические ссылки в torrent запрещены");
                if (file.containsKey("symlink path")) throw new IOException("Символические ссылки в torrent запрещены");
            }
        }
        if (info.get("file tree") instanceof Map<?,?> tree) validateTree(tree,new ArrayList<>(List.of(name)),new HashSet<>(),0);
        try {
            TorrentInfo ti = new TorrentInfo(bytes);
            if (!ti.isValid() || ti.numFiles() < 1 || ti.numFiles() > 10000 || ti.numPieces() < 1 ||
                    ti.numPieces() > 2000000 || ti.pieceLength() > 64 * 1024 * 1024)
                throw new IOException("Некорректный или слишком большой torrent");
            FileStorage fs = ti.files(); Set<String> nativePaths = new HashSet<>();
            List<Map<String,Object>> files = new ArrayList<>(); long size = 0;
            for (int index=0; index<ti.numFiles(); index++) {
                String path = normalize(fs.filePath(index)); if(!fs.padFileAt(index))unique(nativePaths,path);
                long length=fs.fileSize(index); if (length < 0) throw new IOException("Некорректный размер файла");
                if (!fs.padFileAt(index)) size = Math.addExact(size,length);
                files.add(new LinkedHashMap<>(Map.of("index",index,"path",path,"size",length,"pad",fs.padFileAt(index))));
            }
            List<String> allPaths = new ArrayList<>(nativePaths);
            for (String p : allPaths) for (String q : allPaths) if (!p.equals(q) && q.startsWith(p+"/"))
                throw new IOException("Путь файла совпадает с папкой другого файла");
            var hashes = ti.swig().info_hashes();
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("kind","torrent"); result.put("name",name); result.put("size",size); result.put("pieces",ti.numPieces());
            result.put("info_hash_v1", hashes.has_v1()?hashes.getV1().to_hex():"");
            result.put("info_hash_v2", hashes.has_v2()?hashes.getV2().to_hex():"");
            result.put("files",files); result.put("checksum_source","bittorrent-piece-hashes"); return result;
        } catch (IOException error) { throw error; }
        catch (RuntimeException error) { throw new IOException("Не удалось прочитать torrent",error); }
    }
    /** Resume data contains the original info dictionary, including v2 metadata without piece layers. */
    static byte[] torrentFromResume(byte[] resume) throws IOException {
        Object raw=new Bdecode(resume).parse();
        if(!(raw instanceof Map<?,?> map) || !(map.get("info") instanceof Map<?,?> info))throw new IOException("Magnet не предоставил info dictionary");
        Map<String,Object> torrent=new LinkedHashMap<>();torrent.put("info",info);
        if(map.get("trackers") instanceof List<?> list && !list.isEmpty())torrent.put("announce-list",list);
        if(map.get("url-list") instanceof List<?> list && !list.isEmpty())torrent.put("url-list",list);
        if(map.get("httpseeds") instanceof List<?> list && !list.isEmpty())torrent.put("httpseeds",list);
        ByteArrayOutputStream out=new ByteArrayOutputStream();bencode(out,torrent);return out.toByteArray();
    }
    private static byte[] keyBytes(String key) {
        return key.startsWith("\0bin:")?HexFormat.of().parseHex(key.substring(5)):key.getBytes(StandardCharsets.UTF_8);
    }
    private static void bencode(OutputStream out,Object value) throws IOException {
        if(value instanceof byte[] bytes){out.write((bytes.length+":").getBytes(StandardCharsets.US_ASCII));out.write(bytes);}
        else if(value instanceof Number n)out.write(("i"+n.longValue()+"e").getBytes(StandardCharsets.US_ASCII));
        else if(value instanceof List<?> list){out.write('l');for(Object entry:list)bencode(out,entry);out.write('e');}
        else if(value instanceof Map<?,?> map){out.write('d');List<String> keys=new ArrayList<>();for(Object key:map.keySet())keys.add((String)key);keys.sort((a,b)->Arrays.compareUnsigned(keyBytes(a),keyBytes(b)));for(String key:keys){bencode(out,keyBytes(key));bencode(out,map.get(key));}out.write('e');}
        else throw new IOException("Некорректные метаданные bencode");
    }
    static byte[] restoreEndpoints(byte[] metadata,java.util.function.UnaryOperator<String> restore)throws IOException {
        Object raw=new Bdecode(metadata).parse();Map<String,Object> torrent=(Map<String,Object>)raw;
        for(String key:List.of("announce","announce-list","url-list","httpseeds"))if(torrent.containsKey(key))torrent.put(key,restoreUrlValue(torrent.get(key),restore));
        ByteArrayOutputStream out=new ByteArrayOutputStream();bencode(out,torrent);return out.toByteArray();
    }
    private static Object restoreUrlValue(Object value,java.util.function.UnaryOperator<String> restore)throws IOException {
        if(value instanceof byte[])return restore.apply(text(value)).getBytes(StandardCharsets.UTF_8);
        if(value instanceof List<?> list){List<Object> values=new ArrayList<>();for(Object item:list)values.add(restoreUrlValue(item,restore));return values;}
        throw new IOException("Некорректный список источников torrent");
    }
    /** Trusted v2 roots, in the same paths that libtorrent exposes for saving. */
    static Map<String,byte[]> v2Roots(byte[] metadata,TorrentInfo ti) throws IOException {
        Object raw=new Bdecode(metadata).parse();Map<?,?> info=(Map<?,?>)((Map<?,?>)raw).get("info");
        if(!(info.get("file tree") instanceof Map<?,?> tree))return Map.of();
        Map<String,byte[]> leaves=new LinkedHashMap<>();collectRoots(tree,new ArrayList<>(),leaves);
        FileStorage files=ti.files();Map<String,byte[]> roots=new LinkedHashMap<>();String name=text(info.get("name"));
        for(int file=0;file<ti.numFiles();file++)if(!files.padFileAt(file) && files.fileSize(file)>0){
            String path=normalize(files.filePath(file));byte[] root=leaves.get(path);
            if(root==null && path.startsWith(name+"/"))root=leaves.get(path.substring(name.length()+1));
            if(root==null || root.length!=32)throw new IOException("Нет доверенного Merkle root для "+path);roots.put(path,root);
        }return roots;
    }
    private static void collectRoots(Map<?,?> tree,List<String> prefix,Map<String,byte[]> out) throws IOException {
        for(var entry:tree.entrySet()){
            String key=(String)entry.getKey();if(!(entry.getValue() instanceof Map<?,?> value))throw new IOException("Некорректное дерево v2");
            if(key.isEmpty()){if(value.get("pieces root") instanceof byte[] root)out.put(String.join("/",prefix),root);}
            else {List<String> next=new ArrayList<>(prefix);next.add(key);collectRoots(value,next,out);}
        }
    }
    static void nativeReady() throws IOException {
        try {LibTorrent.version();}
        catch(LinkageError unavailable){throw new IOException("Компонент BitTorrent отсутствует или не подходит для этой системы. Обновите всю папку приложения",unavailable);}
    }
    static String normalize(String path) throws IOException {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.startsWith("\\")) throw new IOException("Абсолютный путь в torrent запрещён");
        String value=path.replace('\\','/'); String[] parts=value.split("/",-1);
        if (parts.length>128 || value.length()>4096) throw new IOException("Слишком длинный путь torrent");
        for(String part:parts) component(part); return value;
    }
    private static void component(String s) throws IOException {
        if (s == null || s.isEmpty() || s.equals(".") || s.equals("..") || s.length()>255 ||
                s.endsWith(".") || s.endsWith(" ") || s.chars().anyMatch(c->c<32 || "<>:\"/\\|?*".indexOf(c)>=0))
            throw new IOException("Небезопасное имя или путь в torrent");
        String stem=s.split("\\.",2)[0].toUpperCase(Locale.ROOT);
        if (Set.of("CON","PRN","AUX","NUL","COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8","COM9","LPT1","LPT2","LPT3","LPT4","LPT5","LPT6","LPT7","LPT8","LPT9").contains(stem))
            throw new IOException("Зарезервированное имя файла");
    }
    private static void unique(Set<String> paths,String value) throws IOException {
        if (!paths.add(value.toLowerCase(Locale.ROOT))) throw new IOException("Совпадающие пути файлов в torrent");
    }
    private static void validateTree(Map<?,?> tree,List<String> prefix,Set<String> paths,int depth) throws IOException {
        if(depth>128 || paths.size()>10000) throw new IOException("Слишком много путей torrent");
        for(var entry:tree.entrySet()) {
            String part=(String)entry.getKey();
            if(part.isEmpty()) {
                if (!(entry.getValue() instanceof Map<?,?> leaf)) throw new IOException("Некорректное дерево файлов");
                if (leaf.containsKey("symlink path") || leaf.get("attr") instanceof byte[] attr && new String(attr,StandardCharsets.US_ASCII).contains("l"))
                    throw new IOException("Символические ссылки в torrent запрещены");
                unique(paths,String.join("/",prefix));
            } else {
                component(part); if (!(entry.getValue() instanceof Map<?,?> child)) throw new IOException("Некорректное дерево файлов");
                List<String> next=new ArrayList<>(prefix);next.add(part);validateTree(child,next,paths,depth+1);
            }
        }
    }
    static String text(Object value) throws IOException {
        if (!(value instanceof byte[] bytes)) throw new IOException("Отсутствует имя torrent");
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException error) { throw new IOException("Имя torrent не в UTF-8",error); }
    }
    /** Small strict parser; byte strings stay bytes so binary hashes are not decoded. */
    static final class Bdecode {
        final byte[] bytes; int pos, tokens;
        Bdecode(byte[] bytes){this.bytes=bytes;}
        Object parse() throws IOException {Object result=value(0);if(pos!=bytes.length)throw new IOException("Лишние данные torrent");return result;}
        Object value(int depth) throws IOException {
            if(depth>150 || ++tokens>200000 || pos>=bytes.length)throw new IOException("Некорректный bencode torrent");
            int c=bytes[pos]&255;
            if(c=='i') {pos++;int begin=pos;while(pos<bytes.length && bytes[pos]!='e')pos++;if(pos==bytes.length)throw new IOException("Обрыв integer");
                String text=new String(bytes,begin,pos++-begin,StandardCharsets.US_ASCII);
                try {if(!text.matches("0|-?[1-9][0-9]*"))throw new NumberFormatException();return Long.parseLong(text);}catch(NumberFormatException e){throw new IOException("Некорректный integer",e);}}
            if(c=='l') {pos++;List<Object> out=new ArrayList<>();while(pos<bytes.length && bytes[pos]!='e')out.add(value(depth+1));if(pos++>=bytes.length)throw new IOException("Обрыв list");return out;}
            if(c=='d') {pos++;Map<String,Object> out=new LinkedHashMap<>();while(pos<bytes.length && bytes[pos]!='e'){
                Object key=value(depth+1);String s;
                try{s=text(key);}catch(IOException invalid){if(!(key instanceof byte[] raw))throw invalid;s="\0bin:"+HexFormat.of().formatHex(raw);}
                if(out.containsKey(s))throw new IOException("Повтор ключа torrent");out.put(s,value(depth+1));}
                if(pos++>=bytes.length)throw new IOException("Обрыв dictionary");return out;}
            int begin=pos;while(pos<bytes.length && bytes[pos]>='0' && bytes[pos]<='9')pos++;
            if(pos==begin || pos>=bytes.length || bytes[pos++]!=':')throw new IOException("Некорректная строка bencode");
            int size;try{size=Integer.parseInt(new String(bytes,begin,pos-begin-1,StandardCharsets.US_ASCII));}catch(NumberFormatException e){throw new IOException("Некорректная длина",e);}
            if(size<0 || size>bytes.length-pos)throw new IOException("Обрыв строки bencode");byte[] out=Arrays.copyOfRange(bytes,pos,pos+size);pos+=size;return out;
        }
    }
}
