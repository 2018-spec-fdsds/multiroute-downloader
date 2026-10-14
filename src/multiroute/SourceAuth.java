package multiroute;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Origin-scoped source credentials. Neither public metadata nor job files contain secrets. */
public final class SourceAuth {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> HEADERS = Set.of("authorization", "cookie", "referer", "x-api-key", "x-auth-token");
    private static Path directory;
    private static final Map<String,Map<String,Object>> entries = new LinkedHashMap<>();
    private static boolean loaded;
    private SourceAuth() { }

    /** Read-only until save/delete: safe to initialize after the application's instance guard. */
    public static synchronized void configure(Path dataDirectory) throws IOException {
        Path next = dataDirectory.toAbsolutePath().normalize().resolve("source-auth");
        if (next.equals(directory)) return;
        directory = next; entries.clear(); loaded = false;
    }
    public static synchronized List<Map<String,Object>> listPublic() throws IOException {
        load(); List<Map<String,Object>> result = new ArrayList<>();
        for (Map<String,Object> value : entries.values()) result.add(publicEntry(value));
        return result;
    }
    public static synchronized Map<String,Object> save(Map<String,Object> input) throws IOException {
        load(); String origin = origin(text(input,"origin",true));
        String type = text(input,"type",true).toLowerCase(Locale.ROOT);
        if (!Set.of("basic","bearer","cookie","headers","sftp","ftp").contains(type))
            throw new IOException("Unsupported source authentication type");
        String scheme = URI.create(origin).getScheme();
        if (("sftp".equals(type) && !"sftp".equals(scheme)) ||
                ("ftp".equals(type) && !Set.of("ftp","ftps").contains(scheme)))
            throw new IOException("Credentials do not match the source protocol");
        if (!Set.of("sftp","ftp").contains(type) && !Set.of("http","https").contains(scheme))
            throw new IOException("HTTP credentials require an HTTP or HTTPS origin");
        Map<String,Object> value = new LinkedHashMap<>();
        Map<String,Object> prior = entries.get(origin);
        value.put("id", prior == null ? UUID.randomUUID().toString() : prior.get("id"));
        value.put("origin",origin); value.put("type",type);
        value.put("name",text(input,"name",false));
        if (Set.of("basic","sftp","ftp").contains(type)) {
            String username=text(input,"username",true), password=text(input,"password",false);
            if (username.contains(":")) throw new IOException("Invalid source username");
            value.put("username",username); value.put("password",password);
        }
        if (type.equals("bearer")) value.put("token",headerText(text(input,"token",true)));
        if (type.equals("cookie")) value.put("cookie",headerText(text(input,"cookie",true)));
        if (type.equals("headers")) {
            if (!(input.get("headers") instanceof Map<?,?> raw) || raw.isEmpty() || raw.size()>5)
                throw new IOException("Source headers must contain 1 to 5 allowed headers");
            Map<String,Object> headers=new LinkedHashMap<>();
            for (var entry:raw.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String content) ||
                        !HEADERS.contains(key.toLowerCase(Locale.ROOT)) || !asciiHeader(content) || content.length()>8192)
                    throw new IOException("Unsupported or invalid source header");
                if (headers.putIfAbsent(key.toLowerCase(Locale.ROOT),content)!=null)
                    throw new IOException("Duplicate source header");
            }
            value.put("headers",headers);
        }
        if (type.equals("sftp")) {
            String fingerprint=text(input,"host_key_sha256",true);
            if (!fingerprint.matches("SHA256:[A-Za-z0-9+/]{43}=?"))
                throw new IOException("SFTP requires the server's verified SHA256 host-key fingerprint");
            try { if (Base64.getDecoder().decode(fingerprint.substring(7)).length!=32) throw new IllegalArgumentException(); }
            catch (IllegalArgumentException error) { throw new IOException("Invalid SSH host-key fingerprint"); }
            value.put("host_key_sha256",fingerprint.replaceAll("=+$",""));
        }
        if (prior==null && entries.size()>=128) throw new IOException("Too many saved source credentials");
        Map<String,Map<String,Object>> updated=new LinkedHashMap<>(entries); updated.put(origin,value);
        persist(updated); entries.clear();entries.putAll(updated); return publicEntry(value);
    }
    public static synchronized void delete(String id) throws IOException {
        load(); Map<String,Map<String,Object>> updated=new LinkedHashMap<>(entries);
        boolean removed=updated.values().removeIf(value -> Objects.equals(value.get("id"),id));
        if (!removed) throw new IOException("Source credentials not found");
        persist(updated); entries.clear();entries.putAll(updated);
    }
    static synchronized Map<String,Object> forUri(URI uri) throws IOException {
        load(); Map<String,Object> value=entries.get(origin(uri.toString()));
        return value==null ? Map.of() : new LinkedHashMap<>(value);
    }
    static void apply(HttpRequest.Builder request, URI uri) throws IOException {
        Map<String,Object> value=forUri(uri); String type=(String)value.getOrDefault("type","");
        switch (type) {
            case "basic" -> request.header("Authorization","Basic "+Base64.getEncoder().encodeToString(
                    (value.get("username")+":"+value.get("password")).getBytes(StandardCharsets.UTF_8)));
            case "bearer" -> request.header("Authorization","Bearer "+headerText((String)value.get("token")));
            case "cookie" -> request.header("Cookie",headerText((String)value.get("cookie")));
            case "headers" -> { for (var entry:((Map<String,Object>)value.get("headers")).entrySet()) request.header(entry.getKey(),headerText((String)entry.getValue())); }
            default -> { }
        }
    }
    public static boolean hasCredentials(String url) throws IOException { return !forUri(URI.create(url)).isEmpty(); }
    public static String origin(String value) throws IOException {
        URI uri;try { uri=URI.create(value.trim()); } catch (RuntimeException error) {throw new IOException("Invalid source origin");}
        String scheme=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("webdavs")) scheme="https";
        if (!Set.of("http","https","sftp","ftp","ftps").contains(scheme) || uri.getHost()==null ||
                uri.getRawUserInfo()!=null || uri.getRawFragment()!=null || uri.getPort()==0 || uri.getPort()>65535)
            throw new IOException("Invalid source origin");
        int port=uri.getPort()<0 ? switch(scheme){case "https"->443;case "sftp"->22;case "ftp","ftps"->21;default->80;} : uri.getPort();
        String host=uri.getHost().toLowerCase(Locale.ROOT);
        if (host.contains(":")) host="["+host.replace("[","").replace("]","")+"]";
        return scheme+"://"+host+":"+port;
    }
    private static Map<String,Object> publicEntry(Map<String,Object> value) {
        Map<String,Object> result=new LinkedHashMap<>();
        for (String key:List.of("id","origin","type","name","host_key_sha256")) if(value.containsKey(key))result.put(key,value.get(key));
        return result;
    }
    private static String text(Map<String,Object> input,String key,boolean required)throws IOException {
        Object raw=input.get(key); String value=raw==null?"":raw instanceof String s?s:null;
        if(value==null || !safe(value) || value.length()>8192 || (required&&value.isBlank()))throw new IOException("Invalid source credential field: "+key);
        return value;
    }
    private static boolean safe(String value){return value.chars().noneMatch(c->c<32||c==127);}
    private static boolean asciiHeader(String value){return value!=null&&value.chars().allMatch(c->c>=32&&c<=126);}
    private static String headerText(String value)throws IOException{if(!asciiHeader(value))throw new IOException("Unsupported or invalid source header");return value;}
    private static synchronized void load()throws IOException {
        if(loaded)return;
        if(directory==null){loaded=true;return;}
        Path vault=directory.resolve("vault.bin"), key=directory.resolve("key.bin");
        if(!Files.exists(vault,LinkOption.NOFOLLOW_LINKS)){loaded=true;return;}
        try {
            checkFile(vault,2*1024*1024);checkFile(key,32);
            byte[] encrypted=Files.readAllBytes(vault), keyBytes=Files.readAllBytes(key);
            if(encrypted.length<29||keyBytes.length!=32||encrypted[0]!=1)throw new IOException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(keyBytes,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(encrypted,1,13)));
            Object parsed=Json.parse(new String(cipher.doFinal(Arrays.copyOfRange(encrypted,13,encrypted.length)),StandardCharsets.UTF_8));
            if(!(parsed instanceof List<?> list)||list.size()>128)throw new IOException();
            for(Object item:list){if(!(item instanceof Map<?,?> raw))throw new IOException();Map<String,Object> value=new LinkedHashMap<>();for(var e:raw.entrySet())value.put((String)e.getKey(),e.getValue());String source=origin((String)value.get("origin"));if(entries.putIfAbsent(source,value)!=null)throw new IOException();}
            loaded=true;
        }catch(Exception error){entries.clear();throw new IOException("Cannot read the protected source credential store");}
    }
    private static void persist(Map<String,Map<String,Object>> updated)throws IOException {
        if(directory==null)throw new IOException("Source credential storage is not configured");
        try {
            Files.createDirectories(directory);if(Files.isSymbolicLink(directory))throw new IOException();privateAccess(directory,true);
            Path key=directory.resolve("key.bin");byte[] keyBytes;
            if(Files.exists(key,LinkOption.NOFOLLOW_LINKS)){checkFile(key,32);keyBytes=Files.readAllBytes(key);if(keyBytes.length!=32)throw new IOException();}
            else {keyBytes=new byte[32];RANDOM.nextBytes(keyBytes);writePrivate(key,keyBytes,false);}
            byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(keyBytes,"AES"),new GCMParameterSpec(128,nonce));
            byte[] encrypted=cipher.doFinal(Json.stringify(new ArrayList<>(updated.values())).getBytes(StandardCharsets.UTF_8));
            byte[] bytes=new byte[13+encrypted.length];bytes[0]=1;System.arraycopy(nonce,0,bytes,1,12);System.arraycopy(encrypted,0,bytes,13,encrypted.length);
            Path pending=directory.resolve("vault-"+UUID.randomUUID()+".tmp");
            try {writePrivate(pending,bytes,false);Path vault=directory.resolve("vault.bin");if(Files.exists(vault,LinkOption.NOFOLLOW_LINKS))checkFile(vault,2*1024*1024);try{Files.move(pending,vault,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException error){Files.move(pending,vault,StandardCopyOption.REPLACE_EXISTING);}privateAccess(vault,false);}finally{Files.deleteIfExists(pending);}
            Arrays.fill(keyBytes,(byte)0);
        }catch(Exception error){throw new IOException("Cannot save the protected source credential store");}
    }
    private static void writePrivate(Path file,byte[] bytes,boolean replace)throws IOException {
        Set<PosixFilePermission> permissions=PosixFilePermissions.fromString("rw-------");
        if(Files.getFileStore(file.getParent()).supportsFileAttributeView("posix"))Files.createFile(file,PosixFilePermissions.asFileAttribute(permissions));
        else Files.createFile(file);
        privateAccess(file,false);Files.write(file,bytes,StandardOpenOption.WRITE);
    }
    private static void checkFile(Path file,long max)throws IOException {
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(file)||Files.size(file)>max)throw new IOException();
        privateAccess(file,false);
    }
    private static void privateAccess(Path path,boolean dir)throws IOException {
        PosixFileAttributeView posix=Files.getFileAttributeView(path,PosixFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
        if(posix!=null){posix.setPermissions(PosixFilePermissions.fromString(dir?"rwx------":"rw-------"));return;}
        AclFileAttributeView acl=Files.getFileAttributeView(path,AclFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
        if(acl!=null){UserPrincipal owner=Files.getOwner(path,LinkOption.NOFOLLOW_LINKS);AclEntry.Builder entry=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner).setPermissions(EnumSet.allOf(AclEntryPermission.class));if(dir)entry.setFlags(AclEntryFlag.DIRECTORY_INHERIT,AclEntryFlag.FILE_INHERIT);acl.setAcl(List.of(entry.build()));return;}
        throw new IOException("Private file permissions are unavailable");
    }
}
