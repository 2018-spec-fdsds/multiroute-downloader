package multiroute;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Independently authenticated sites; secrets never enter job manifests or public metadata. */
public final class SiteProfiles {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_PROFILES = 128, MAX_VAULT = 4 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("none", "basic", "bearer", "cookie", "headers", "ftp", "sftp");
    private static final List<String> CREDENTIAL_FIELDS = List.of("username", "password", "token", "cookie", "headers", "host_key_sha256");
    private static Path directory;
    private static boolean loaded;
    private static final Map<String,Map<String,Object>> profiles = new LinkedHashMap<>();
    private SiteProfiles() { }

    /** Called after the instance guard. No file or key is created until the first edit. */
    public static synchronized void configure(Path dataDirectory) throws IOException {
        if (dataDirectory == null) throw new IOException("Site profile storage is not configured");
        Path next = dataDirectory.toAbsolutePath().normalize().resolve("site-profiles");
        if (next.equals(directory)) return;
        directory = next; profiles.clear(); loaded = false;
    }

    public static synchronized List<Map<String,Object>> listPublic() throws IOException {
        load(); List<Map<String,Object>> result = new ArrayList<>();
        for (Map<String,Object> profile : profiles.values()) result.add(publicProfile(profile));
        return result;
    }

    /** Empty/omitted credentials keep existing values only for an unchanged origin and type.
     * replace_credentials=true explicitly replaces the full credential set, allowing an empty password. */
    public static synchronized Map<String,Object> save(Map<String,Object> input) throws IOException {
        if (input == null) throw new IOException("Invalid site profile");
        load(); String id = text(input.get("id"), "id", 64, false);
        if (id.isEmpty()) id = UUID.randomUUID().toString(); else id = id(id);
        Map<String,Object> prior = profiles.get(id);
        if (prior == null && profiles.size() >= MAX_PROFILES) throw new IOException("Too many saved site profiles");
        String name = text(field(input,prior,"name"), "name", 200, true);
        String base = baseUrl(text(field(input,prior,"base_url"), "base_url", 4096, true));
        String origin = SourceAuth.origin(base);
        String type = text(input.containsKey("type") ? input.get("type") : input.containsKey("auth_type")
                ? input.get("auth_type") : prior == null ? "none" : prior.get("type"), "type", 32, true).toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IOException("Unsupported site authentication type");
        String download = text(field(input,prior,"download_directory"), "download_directory", 8192, false);
        if (!download.isEmpty()) {
            try {
                Path destination = Path.of(download);
                if (!destination.isAbsolute()) throw new IOException("Site download directory must be absolute");
                download = destination.normalize().toString();
            } catch (IllegalArgumentException invalid) { throw new IOException("Invalid site download directory"); }
        }
        String remote = text(field(input,prior,"remote_directory"), "remote_directory", 8192, false);
        List<String> gateways = gateways(field(input,prior,"gateways"));
        boolean replace = flag(input,"replace_credentials");
        boolean keep = !replace && prior != null && origin.equals(prior.get("origin")) && type.equals(prior.get("type"));
        Map<String,Object> credentials = new LinkedHashMap<>();
        if (keep && prior.get("credentials") instanceof Map<?,?> saved) credentials.putAll(cast(saved));
        credentials.put("origin",origin); credentials.put("type",type); credentials.put("name",name);
        for (String key : CREDENTIAL_FIELDS) if (input.containsKey(key)) {
            Object value = input.get(key);
            boolean blank = value == null || value instanceof String string && string.isBlank()
                    || value instanceof Map<?,?> map && map.isEmpty();
            if (!blank || !keep) credentials.put(key,value);
        }
        if (type.equals("none")) credentials = Map.of();
        else credentials = SourceAuth.validateCredentials(credentials);
        Map<String,Object> profile = new LinkedHashMap<>();
        profile.put("id",id); profile.put("name",name); profile.put("base_url",base); profile.put("origin",origin);
        profile.put("download_directory",download); profile.put("remote_directory",remote);
        profile.put("gateways",gateways); profile.put("type",type); profile.put("credentials",credentials);
        Map<String,Map<String,Object>> updated = new LinkedHashMap<>(profiles); updated.put(id,profile);
        persist(updated); profiles.clear(); profiles.putAll(updated); return publicProfile(profile);
    }

    public static synchronized void delete(String identifier) throws IOException {
        load(); String id = id(identifier);
        if (!profiles.containsKey(id)) throw new IOException("Site profile not found");
        Map<String,Map<String,Object>> updated = new LinkedHashMap<>(profiles); updated.remove(id);
        persist(updated); profiles.clear(); profiles.putAll(updated);
    }

    /** Deliberately no global-origin fallback when an explicitly chosen profile does not match. */
    static synchronized Map<String,Object> authFor(String identifier, URI target) throws IOException {
        load(); Map<String,Object> profile = profiles.get(id(identifier));
        if (profile == null) throw new IOException("Site profile not found");
        if (target == null || !profile.get("origin").equals(SourceAuth.origin(target.toString()))) return Map.of();
        return SourceAuth.credentialCopy(cast((Map<?,?>)profile.get("credentials")));
    }

    /** Deep-copy a transfer/inspection request, add defaults and bind credentials by profile ID only. */
    public static synchronized Map<String,Object> apply(Map<String,Object> input) throws IOException {
        if (input == null) throw new IOException("Invalid site profile request");
        Map<String,Object> result = cloneMap(input);
        Object rawId = input.get("site_profile_id");
        // A route-only API binding must survive later reconstruction and gateway changes.
        // One transfer has one site account; mixing accounts can combine different remote files.
        Set<String> bindings=new LinkedHashSet<>();
        if(rawId!=null){if(!(rawId instanceof String text))throw new IOException("Invalid site profile identifier");if(!text.isBlank())bindings.add(text);}
        List<Object> supplied=new ArrayList<>();
        if(result.get("route") instanceof Map<?,?> route)supplied.add(route);
        if(result.get("routes") instanceof List<?> routes)supplied.addAll(routes);
        for(Object value:supplied)if(value instanceof Map<?,?> route&&route.containsKey("site_profile_id")){
            if(!(route.get("site_profile_id") instanceof String text))throw new IOException("Invalid site profile identifier");
            if(!text.isBlank())bindings.add(text);
        }
        if(bindings.size()>1)throw new IOException("Invalid site profile identifier");
        if(!bindings.isEmpty()){rawId=bindings.iterator().next();result.put("site_profile_id",rawId);}
        if (rawId == null || rawId instanceof String value && value.isBlank()) return result;
        String id = id(text(rawId,"site_profile_id",64,true)); load();
        Map<String,Object> profile = profiles.get(id);
        if (profile == null) throw new IOException("Site profile not found");
        URI base = URI.create((String)profile.get("base_url"));
        if (!base.toString().endsWith("/")) base = URI.create(base.toString()+"/");
        for (String key : List.of("url","target_url")) if (result.containsKey(key)) {
            String value = text(result.get(key),key,16384,false);
            if (value.isEmpty()) continue;
            URI target;
            try { target = base.resolve(URI.create(value)); }
            catch (IllegalArgumentException invalid) { throw new IOException("Invalid site transfer URL"); }
            if (!profile.get("origin").equals(SourceAuth.origin(target.toString())))
                throw new IOException("Transfer URL does not match the selected site profile");
            result.put(key,target.toString());
        }
        if (blank(result.get("directory")) && !blank(profile.get("download_directory"))) result.put("directory",profile.get("download_directory"));
        if (blank(result.get("remote_directory")) && !blank(profile.get("remote_directory"))) result.put("remote_directory",profile.get("remote_directory"));
        result.put("site_profile_id",id);
        if (result.containsKey("route")) {
            if (!(result.get("route") instanceof Map<?,?> route)) throw new IOException("Invalid site transfer route");
            Map<String,Object> decorated = cast(route); decorated.put("site_profile_id",id); result.put("route",decorated);
        }
        List<Object> routes = new ArrayList<>();
        if (result.containsKey("routes")) {
            if (!(result.get("routes") instanceof List<?> list)) throw new IOException("Invalid site transfer routes");
            for (Object value : list) {
                if (!(value instanceof Map<?,?> route)) throw new IOException("Invalid site transfer route");
                Map<String,Object> decorated = cast(route); decorated.put("site_profile_id",id); routes.add(decorated);
            }
            result.put("routes",routes);
        } else if (!result.containsKey("route") && profile.get("gateways") instanceof List<?> list && !list.isEmpty()) {
            for (Object gateway : list) routes.add(new LinkedHashMap<>(Map.of("type","gateway","gateway",gateway,"enabled",true,"site_profile_id",id)));
            result.put("routes",routes);
        }
        // Keep the credential binding even when the caller relies on the API's ordinary direct route.
        // A selected profile must never fall through to unrelated global-origin credentials.
        if (!result.containsKey("routes") || result.get("routes") instanceof List<?> list && list.isEmpty()) {
            Map<String,Object> fallback = result.get("route") instanceof Map<?,?> route ? cast(route)
                    : new LinkedHashMap<>(Map.of("type","direct","enabled",true));
            fallback.put("site_profile_id",id); result.put("routes",List.of(fallback));
        }
        return result;
    }

    private static boolean blank(Object value) { return value == null || value instanceof String text && text.isBlank(); }
    private static Object field(Map<String,Object> input,Map<String,Object> prior,String key) {
        return input.containsKey(key) ? input.get(key) : prior == null ? null : prior.get(key);
    }
    private static boolean flag(Map<String,Object> input,String key) throws IOException {
        Object value=input.get(key); if(value==null)return false;
        if(!(value instanceof Boolean result))throw new IOException("Invalid site profile option"); return result;
    }
    private static String text(Object raw,String field,int limit,boolean required) throws IOException {
        if(raw==null)raw="";
        if(!(raw instanceof String value) || value.length()>limit || value.chars().anyMatch(c->c<32||c==127) || required&&value.isBlank())
            throw new IOException("Invalid site profile field: "+field);
        return value;
    }
    private static String id(String value) throws IOException {
        try { String normalized=UUID.fromString(value).toString();if(!normalized.equalsIgnoreCase(value))throw new IllegalArgumentException();return normalized; }
        catch(RuntimeException invalid){throw new IOException("Invalid site profile identifier");}
    }
    private static String baseUrl(String value) throws IOException {
        try {
            URI uri=URI.create(value.trim());
            if(uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)throw new IllegalArgumentException();
            SourceAuth.origin(uri.toString()); return uri.normalize().toString();
        } catch(RuntimeException invalid){throw new IOException("Invalid site base URL");}
    }
    private static List<String> gateways(Object raw) throws IOException {
        if(raw==null)return List.of();
        if(!(raw instanceof List<?> list)||list.size()>32)throw new IOException("Invalid site gateway list");
        Set<String> values=new LinkedHashSet<>();
        for(Object item:list){
            String value=text(item,"gateways",15,true);String[] parts=value.split("\\.",-1);
            if(parts.length!=4)throw new IOException("Invalid site gateway IPv4 address");
            for(String part:parts)try{if(!part.matches("0|[1-9][0-9]{0,2}")||Integer.parseInt(part)>255)throw new NumberFormatException();}catch(NumberFormatException invalid){throw new IOException("Invalid site gateway IPv4 address");}
            int first=Integer.parseInt(parts[0]);if(first==0||first==127||first>=224||!values.add(value))throw new IOException("Invalid or duplicate site gateway IPv4 address");
        }
        return List.copyOf(values);
    }
    private static Map<String,Object> publicProfile(Map<String,Object> value) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(String key:List.of("id","name","base_url","origin","download_directory","remote_directory","type"))result.put(key,value.get(key));
        result.put("auth_type",value.get("type"));result.put("gateways",new ArrayList<>((List<?>)value.get("gateways")));
        Map<String,Object> credentials=cast((Map<?,?>)value.get("credentials"));
        if(credentials.containsKey("host_key_sha256"))result.put("host_key_sha256",credentials.get("host_key_sha256"));
        result.put("credentials_configured",!credentials.isEmpty());return result;
    }
    private static Map<String,Object> cast(Map<?,?> raw) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(var entry:raw.entrySet())if(entry.getKey() instanceof String key)result.put(key,entry.getValue());
        return result;
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> cloneMap(Map<String,Object> value) throws IOException {
        try{return (Map<String,Object>)Json.parse(Json.stringify(value));}
        catch(RuntimeException invalid){throw new IOException("Invalid site transfer request");}
    }
    private static void load() throws IOException {
        if(loaded)return;
        if(directory==null){loaded=true;return;}
        Main.safePath(directory);Path vault=directory.resolve("vault.bin"),key=directory.resolve("key.bin");
        if(!Files.exists(vault,LinkOption.NOFOLLOW_LINKS)){loaded=true;return;}
        byte[] secret=null;
        try{
            SourceAuth.checkFile(vault,MAX_VAULT);SourceAuth.checkFile(key,32);
            byte[] encrypted=Files.readAllBytes(vault);secret=Files.readAllBytes(key);
            if(encrypted.length<29||encrypted[0]!=1||secret.length!=32)throw new IOException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(secret,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(encrypted,1,13)));
            String plaintext=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(cipher.doFinal(Arrays.copyOfRange(encrypted,13,encrypted.length)))).toString();
            Object parsed=Json.parse(plaintext);
            if(!(parsed instanceof List<?> list)||list.size()>MAX_PROFILES)throw new IOException();
            Map<String,Map<String,Object>> restored=new LinkedHashMap<>();
            for(Object item:list){
                if(!(item instanceof Map<?,?> raw))throw new IOException();Map<String,Object> value=cast(raw);
                String id=id(text(value.get("id"),"id",64,true));String base=baseUrl(text(value.get("base_url"),"base_url",4096,true));
                if(!SourceAuth.origin(base).equals(value.get("origin"))||!TYPES.contains(value.get("type")))throw new IOException();
                text(value.get("name"),"name",200,true);text(value.get("download_directory"),"download_directory",8192,false);text(value.get("remote_directory"),"remote_directory",8192,false);
                value.put("gateways",gateways(value.get("gateways")));
                if(!(value.get("credentials") instanceof Map<?,?> credentials))throw new IOException();
                Map<String,Object> auth="none".equals(value.get("type"))?Map.of():SourceAuth.validateCredentials(cast(credentials));
                if(!auth.isEmpty()&&(!auth.get("origin").equals(value.get("origin"))||!auth.get("type").equals(value.get("type"))))throw new IOException();
                value.put("credentials",auth);if(restored.putIfAbsent(id,value)!=null)throw new IOException();
            }
            profiles.clear();profiles.putAll(restored);loaded=true;
        }catch(Exception invalid){profiles.clear();throw new IOException("Cannot read the protected site profile store");}
        finally{if(secret!=null)Arrays.fill(secret,(byte)0);}
    }
    private static void persist(Map<String,Map<String,Object>> updated) throws IOException {
        if(directory==null)throw new IOException("Site profile storage is not configured");
        byte[] secret=null;Path pending=null;
        try{
            Main.safePath(directory);Files.createDirectories(directory);Main.safePath(directory);SourceAuth.privateAccess(directory,true);
            Path key=directory.resolve("key.bin");
            if(Files.exists(key,LinkOption.NOFOLLOW_LINKS)){SourceAuth.checkFile(key,32);secret=Files.readAllBytes(key);if(secret.length!=32)throw new IOException();}
            else{secret=new byte[32];RANDOM.nextBytes(secret);SourceAuth.writePrivate(key,secret,false);}
            byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(secret,"AES"),new GCMParameterSpec(128,nonce));
            byte[] ciphertext=cipher.doFinal(Json.stringify(new ArrayList<>(updated.values())).getBytes(StandardCharsets.UTF_8));
            if(ciphertext.length+13>MAX_VAULT)throw new IOException();
            byte[] bytes=new byte[13+ciphertext.length];bytes[0]=1;System.arraycopy(nonce,0,bytes,1,12);System.arraycopy(ciphertext,0,bytes,13,ciphertext.length);
            pending=directory.resolve("vault-"+UUID.randomUUID()+".tmp");SourceAuth.writePrivate(pending,bytes,false);
            Path vault=directory.resolve("vault.bin");if(Files.exists(vault,LinkOption.NOFOLLOW_LINKS))SourceAuth.checkFile(vault,MAX_VAULT);
            try{Files.move(pending,vault,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(pending,vault,StandardCopyOption.REPLACE_EXISTING);}
            SourceAuth.privateAccess(vault,false);
        }catch(Exception invalid){throw new IOException("Cannot save the protected site profile store");}
        finally{if(secret!=null)Arrays.fill(secret,(byte)0);if(pending!=null)Files.deleteIfExists(pending);}
    }
}
