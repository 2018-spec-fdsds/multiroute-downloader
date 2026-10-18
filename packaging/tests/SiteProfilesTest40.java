package multiroute;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Own encrypted fixtures only: no network, real credentials, user profile, or remote file access. */
public final class SiteProfilesTest40 {
    private static int checks;
    private static final String PASSWORD="own-secret-Пароль_日本語_🚀-first", SECOND="own-secret-second-user-isolated";
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);checks++;}
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Action action,String message)throws Exception{try{action.run();throw new AssertionError(message);}catch(IOException expected){checks++;}}
    private static Map<String,Object> profile(Path own,String name,String user,String password){
        Map<String,Object> value=new LinkedHashMap<>();value.put("name",name);value.put("base_url","https://profiles.example.invalid/files");
        value.put("download_directory",own.resolve("Файлы_日本語_हिन्दी_🚀").toString());value.put("remote_directory","Папка/上传");
        value.put("gateways",List.of("192.168.1.1","192.168.1.2","192.168.1.3","192.168.1.4"));
        value.put("type","basic");value.put("username",user);value.put("password",password);return value;
    }
    private static String id(Map<String,Object> profile){return (String)profile.get("id");}
    private static Map<String,Object> route(String id){return Map.of("type","gateway","gateway","192.168.1.1","site_profile_id",id);}
    private static Map<String,Object> auth(String id)throws Exception{return SourceAuth.forUri(URI.create("https://profiles.example.invalid/another/path"),route(id));}
    private static void noSecrets(Object metadata){String text=Json.stringify(metadata);check(!text.contains(PASSWORD)&&!text.contains(SECOND)&&!text.contains("own-header-secret-long-value")&&!text.contains("own-bearer-global-secret"),"Public metadata contains a credential");}
    public static void main(String[] args)throws Exception{
        Path own=Path.of(args[0]).toAbsolutePath().normalize();Files.createDirectories(own);
        Path data=own.resolve("профиль_日本語_🚀"), other=own.resolve("other-profile");
        SourceAuth.configure(data);SiteProfiles.configure(data);
        check(SiteProfiles.listPublic().isEmpty()&&!Files.exists(data),"Read-only configuration created files");
        SourceAuth.validateCredentials(Map.of("origin","https://profiles.example.invalid","type","basic","username","Пользователь","password",PASSWORD));
        check(!Files.exists(data),"Pure source credential validation wrote a global store");
        SourceAuth.save(Map.of("origin","https://profiles.example.invalid","type","bearer","token","own-bearer-global-secret"));
        SourceAuth.save(Map.of("origin","https://other.example.invalid","type","bearer","token","own-other-global-secret"));
        List<Map<String,Object>> globals=SourceAuth.listPublic();
        Map<String,Object> first=SiteProfiles.save(profile(own,"Сайт_日本語_हिन्दी_🚀","первый",PASSWORD));
        Map<String,Object> second=SiteProfiles.save(profile(own,"Second account","second",SECOND));
        String a=id(first),b=id(second);URI source=URI.create("https://profiles.example.invalid/file.bin");
        check(!a.equals(b)&&SiteProfiles.listPublic().size()==2,"Same-origin accounts replaced one another");
        check(auth(a).get("username").equals("первый")&&auth(a).get("password").equals(PASSWORD),"First account credentials lost");
        check(auth(b).get("username").equals("second")&&auth(b).get("password").equals(SECOND),"Second account credentials lost");
        check(SourceAuth.forUri(source).get("token").equals("own-bearer-global-secret"),"Profile save overwrote the origin-global credentials");
        check(SourceAuth.listPublic().equals(globals),"Independent profiles changed the global vault");
        noSecrets(first);noSecrets(second);noSecrets(SiteProfiles.listPublic());
        for(String field:List.of("username","password","token","cookie","headers","credentials"))check(!first.containsKey(field),"A credential field is public");
        Map<String,Object> edited=new LinkedHashMap<>();edited.put("id",a);edited.put("name","Изменено_日本語_🚀");edited.put("username","");edited.put("password","");
        SiteProfiles.save(edited);check(auth(a).get("password").equals(PASSWORD),"Blank edit erased the existing password");
        check(auth(a).get("username").equals("первый"),"Blank edit erased the existing username");
        check(auth(b).get("password").equals(SECOND),"Editing one profile changed the other account");
        HttpRequest.Builder basic=HttpRequest.newBuilder(source);SourceAuth.apply(basic,source,route(a));
        String header=basic.build().headers().firstValue("Authorization").orElseThrow();
        check(new String(Base64.getDecoder().decode(header.substring(6)),StandardCharsets.UTF_8).equals("первый:"+PASSWORD),"HTTP Basic did not preserve UTF-8 credentials");
        for(String foreign:List.of("https://other.example.invalid/file","http://profiles.example.invalid/file","https://profiles.example.invalid:444/file")){
            URI target=URI.create(foreign);check(SourceAuth.forUri(target,route(a)).isEmpty(),"Profile credentials escaped their exact origin");
            HttpRequest.Builder builder=HttpRequest.newBuilder(target);SourceAuth.apply(builder,target,route(a));check(builder.build().headers().map().isEmpty(),"A foreign redirect received profile/global credentials");
        }
        check(!SourceAuth.forUri(URI.create("https://other.example.invalid/file")).isEmpty(),"Unprofiled global authentication no longer works");
        rejects(()->SiteProfiles.authFor(a,URI.create("https://user:pass@profiles.example.invalid/file")),"Userinfo was accepted");
        Map<String,Object> request=new LinkedHashMap<>();request.put("site_profile_id",a);request.put("url","модель_日本語.bin");
        request.put("route",new LinkedHashMap<>(Map.of("type","direct")));request.put("routes",List.of(new LinkedHashMap<>(Map.of("type","gateway","gateway","192.168.1.4"))));
        Map<String,Object> applied=SiteProfiles.apply(request);
        check(applied.get("url").equals("https://profiles.example.invalid/files/модель_日本語.bin"),"Relative download URL did not use the profile base directory");
        check(Main.obj(applied.get("route")).get("site_profile_id").equals(a),"Single archive/inspection route lost the profile binding");
        check(Main.obj(Main.arr(applied.get("routes")).get(0)).get("site_profile_id").equals(a),"Route list lost the profile binding");
        check(applied.get("directory").equals(first.get("download_directory"))&&applied.get("remote_directory").equals("Папка/上传"),"Profile directory defaults are missing");
        check(!Main.obj(request.get("route")).containsKey("site_profile_id")&&!request.containsKey("directory"),"Applying a profile mutated the caller's request");
        check(!Json.stringify(applied).contains(PASSWORD)&&!Json.stringify(applied).contains("username"),"Credentials leaked into a job request");
        Map<String,Object> defaults=SiteProfiles.apply(Map.of("site_profile_id",a,"target_url","upload/файл.bin"));
        check(Main.arr(defaults.get("routes")).size()==4&&((String)defaults.get("target_url")).endsWith("/files/upload/файл.bin"),"Upload/default gateway resolution failed");
        Map<String,Object> explicit=SiteProfiles.apply(Map.of("site_profile_id",a,"url","file","directory",own.resolve("manual").toString()));
        check(explicit.get("directory").equals(own.resolve("manual").toString()),"Explicit download destination was overwritten");
        rejects(()->SiteProfiles.apply(Map.of("site_profile_id",a,"target_url","https://other.example.invalid/file")),"An initial URL on another site was accepted");
        rejects(()->SiteProfiles.apply(Map.of("site_profile_id",a,"route","invalid")),"Invalid route shape accepted");
        Map<String,Object> clear=new LinkedHashMap<>();clear.put("id",a);clear.put("replace_credentials",true);clear.put("username","первый");clear.put("password","");
        SiteProfiles.save(clear);check(auth(a).get("password").equals(""),"Explicit password replacement could not clear an empty password");
        SiteProfiles.save(Map.of("id",a,"password",PASSWORD));
        rejects(()->SiteProfiles.save(Map.of("id",a,"base_url","https://different.example.invalid")),"Credentials crossed a changed origin without re-entry");
        rejects(()->SiteProfiles.save(Map.of("id",a,"type","bearer")),"Credentials crossed a changed authentication type");
        check(auth(a).get("password").equals(PASSWORD),"Rejected edit changed saved credentials");
        Map<String,Object> anonymous=SiteProfiles.save(Map.of("name","Anonymous","base_url","https://profiles.example.invalid","type","none"));
        check(SourceAuth.forUri(source,route(id(anonymous))).isEmpty(),"Anonymous profile unexpectedly used global authentication");
        Map<String,Object> defaultRoute=SiteProfiles.apply(Map.of("site_profile_id",id(anonymous),"url","file.bin"));
        check(Main.obj(Main.arr(defaultRoute.get("routes")).get(0)).get("site_profile_id").equals(id(anonymous)),"Default transport lost the selected profile credential binding");
        Map<String,Object> singleRoute=SiteProfiles.apply(Map.of("site_profile_id",a,"route",Map.of("type","direct"),"url","file.bin"));
        check(Main.obj(Main.arr(singleRoute.get("routes")).get(0)).get("site_profile_id").equals(a),"Single-route request lost credentials when used as a download job");
        Map<String,Object> headers=SiteProfiles.save(Map.of("name","Headers","base_url","https://profiles.example.invalid","type","headers","headers",Map.of("X-Api-Key","own-header-secret-long-value")));
        Map<String,Object> copy=auth(id(headers));Main.obj(copy.get("headers")).put("x-api-key","changed-only-copy");
        check(Main.obj(auth(id(headers)).get("headers")).get("x-api-key").equals("own-header-secret-long-value"),"Credential copies expose mutable stored headers");
        noSecrets(headers);
        String fingerprint="SHA256:"+Base64.getEncoder().withoutPadding().encodeToString(new byte[32]);
        Map<String,Object> sftp=SiteProfiles.save(Map.of("name","SFTP","base_url","sftp://sftp.example.invalid/base","type","sftp","username","user","password",SECOND,"host_key_sha256",fingerprint));
        check(sftp.get("host_key_sha256").equals(fingerprint),"Verified SFTP host key did not survive public metadata");
        check(SiteProfiles.authFor(id(sftp),URI.create("sftp://sftp.example.invalid/file")).get("password").equals(SECOND),"SFTP profile credentials unavailable");
        rejects(()->SiteProfiles.save(Map.of("name","Wrong SFTP","base_url","sftp://sftp.example.invalid","type","sftp","username","user","host_key_sha256","unknown")),"Unverified SFTP host key accepted");
        for(String base:List.of("https://user:pass@profiles.example.invalid","https://profiles.example.invalid/?token=secret","https://profiles.example.invalid/#fragment","file:///tmp/private"))
            rejects(()->SiteProfiles.save(Map.of("name","Bad","base_url",base,"type","none")),"Invalid or credential-bearing base URL accepted");
        rejects(()->SiteProfiles.save(Map.of("name","Bad","base_url","https://profiles.example.invalid","gateways",List.of("192.168.1.1","192.168.1.1"))),"Duplicate gateways accepted");
        rejects(()->SiteProfiles.save(Map.of("name","Bad","base_url","https://profiles.example.invalid","gateways",List.of("999.1.1.1"))),"Malformed gateway accepted");
        rejects(()->SiteProfiles.save(Map.of("name","Bad\nName","base_url","https://profiles.example.invalid")),"Control characters accepted");
        rejects(()->SiteProfiles.save(Map.of("name","Bad","base_url","https://profiles.example.invalid","type","headers","headers",Map.of("X-Api-Key","one\r\nInjected: yes"))),"Header injection accepted");
        List<Map<String,Object>> publicList=SiteProfiles.listPublic();Main.arr(publicList.get(0).get("gateways")).clear();
        check(Main.arr(SiteProfiles.listPublic().get(0).get("gateways")).size()==4,"Public metadata exposes mutable stored gateways");
        Path vault=data.resolve("site-profiles/vault.bin"),key=data.resolve("site-profiles/key.bin");
        byte[] ciphertext=Files.readAllBytes(vault),keyBefore=Files.readAllBytes(key);
        String raw=new String(ciphertext,StandardCharsets.ISO_8859_1);
        check(!raw.contains(SECOND)&&!raw.contains("own-header-secret-long-value"),"Protected profile store contains plaintext credentials");
        check(ciphertext[0]==1&&keyBefore.length==32&&ciphertext.length>29,"AES-GCM envelope or key size is invalid");
        SiteProfiles.save(Map.of("id",b,"name","Changed second"));byte[] changed=Files.readAllBytes(vault);
        check(!Arrays.equals(Arrays.copyOfRange(ciphertext,1,13),Arrays.copyOfRange(changed,1,13)),"Profile persistence reused a GCM nonce");
        check(Arrays.equals(keyBefore,Files.readAllBytes(key)),"Ordinary metadata edit unexpectedly replaced the encryption key");
        try(var files=Files.list(vault.getParent())){check(files.noneMatch(path->path.getFileName().toString().endsWith(".tmp")),"Atomic write left a temporary vault");}
        SiteProfiles.configure(other);SiteProfiles.configure(data);
        check(SiteProfiles.listPublic().stream().anyMatch(value->"Изменено_日本語_🚀".equals(value.get("name"))),"UTF-8 profile name did not survive restart");
        check(auth(a).get("password").equals(PASSWORD)&&auth(b).get("password").equals(SECOND),"Same-origin credentials were mixed after reopening");
        var executor=Executors.newFixedThreadPool(4);
        try{List<java.util.concurrent.Future<Boolean>> calls=new ArrayList<>();for(int n=0;n<20;n++){String selected=n%2==0?a:b;String expected=n%2==0?PASSWORD:SECOND;calls.add(executor.submit(()->auth(selected).get("password").equals(expected)));}for(var result:calls)check(result.get(5,TimeUnit.SECONDS),"Concurrent accounts mixed credentials");}
        finally{executor.shutdownNow();check(executor.awaitTermination(5,TimeUnit.SECONDS),"Owned credential readers did not stop");}
        byte[] valid=Files.readAllBytes(vault),corrupt=valid.clone();corrupt[corrupt.length-1]^=1;Files.write(vault,corrupt);
        SiteProfiles.configure(other);SiteProfiles.configure(data);rejects(SiteProfiles::listPublic,"Tampered GCM vault was accepted");
        Files.write(vault,valid);SiteProfiles.configure(other);SiteProfiles.configure(data);check(auth(b).get("password").equals(SECOND),"Restored owned fixture did not load");
        Path keep=own.resolve("owned-downloaded-file.bin");Files.writeString(keep,"own file сохраняется",StandardCharsets.UTF_8);
        SiteProfiles.delete(a);check(Files.readString(keep,StandardCharsets.UTF_8).equals("own file сохраняется"),"Deleting profile deleted a local file");
        check(auth(b).get("password").equals(SECOND)&&SourceAuth.listPublic().equals(globals),"Deleting one profile changed another account or global vault");
        rejects(()->SiteProfiles.authFor(a,source),"Deleted profile silently fell back to global credentials");
        String rendered=Json.stringify(SiteProfiles.listPublic());Files.writeString(own.resolve("public-profiles-utf8.json"),rendered,StandardCharsets.UTF_8);
        check(Files.readString(own.resolve("public-profiles-utf8.json"),StandardCharsets.UTF_8).equals(rendered)&&!rendered.contains("\ufffd"),"UTF-8 output roundtrip changed metadata");
        noSecrets(SiteProfiles.listPublic());
        Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASS");report.put("checks",checks);report.put("version",Main.VERSION);
        report.put("same_origin_accounts_isolated",true);report.put("foreign_redirect_has_no_global_fallback",true);report.put("encrypted_atomic_vault_and_nonce_rotation",true);
        report.put("utf8_credentials_metadata_output_roundtrip",true);report.put("network_calls",0);report.put("user_data_read",false);report.put("remote_files_modified",false);
        Files.writeString(own.resolve("SITE-PROFILES-TESTS.json"),Json.stringify(report),StandardCharsets.UTF_8);System.out.println("PASS "+checks+" site profile checks");
    }
}
