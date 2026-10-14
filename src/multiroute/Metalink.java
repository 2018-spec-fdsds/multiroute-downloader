package multiroute;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.w3c.dom.*;

/** RFC 5854 meta4: publisher-pinned SHA-256, alternative URLs and optional ordered piece hashes. */
public final class Metalink {
    private static final String NS="urn:ietf:params:xml:ns:metalink";
    private Metalink(){}
    public static boolean isMetalink(String url){try{String path=URI.create(url).getPath().toLowerCase(Locale.ROOT);return path.endsWith(".meta4")||path.endsWith(".metalink");}catch(RuntimeException error){return false;}}
    public static Map<String,Object> inspect(Map<String,Object> route,String url)throws IOException,InterruptedException {
        URI source=Transport.uri(url);if(!source.getScheme().equalsIgnoreCase("https"))throw new Transport.NonRetryableException("Trusted Metalink metadata requires HTTPS");
        return parse(SecureXml.read(Transport.request(route,url,"GET",Map.of(),null),200),source);
    }
    static Map<String,Object> parse(byte[] bytes,URI source)throws IOException {
        Document document=SecureXml.parse(bytes);Element root=document.getDocumentElement();
        if(!NS.equals(root.getNamespaceURI())||!"metalink".equals(root.getLocalName()))throw new IOException("Expected RFC 5854 meta4 metadata");
        List<Map<String,Object>> files=new ArrayList<>();Set<String> names=new HashSet<>();
        for(Element file:children(root,"file")){
            if(files.size()>=10000)throw new IOException("Metalink contains too many files");String name=file.getAttribute("name");validatePath(name);if(!names.add(name))throw new IOException("Duplicate Metalink file path");
            List<Element> sizes=children(file,"size");if(sizes.size()!=1)throw new IOException("Metalink requires one exact file size");long size=integer(sizes.get(0).getTextContent().trim(),false);
            Map<String,String> whole=new LinkedHashMap<>(),sources=new LinkedHashMap<>();
            for(Element hash:children(file,"hash")){
                String algorithm=switch(hash.getAttribute("type").toLowerCase(Locale.ROOT)){case "md5"->"md5";case "sha-256"->"sha256";case "sha-512"->"sha512";default->"";};
                if(algorithm.isEmpty())continue;String next;
                try{next=Checksums.normalize(algorithm,hash.getTextContent());}catch(IllegalArgumentException error){throw new IOException("Invalid publisher "+Checksums.label(algorithm),error);}
                if(next.isEmpty())throw new IOException("Empty publisher "+Checksums.label(algorithm));
                if(whole.containsKey(algorithm)&&!whole.get(algorithm).equals(next))throw new Checksum.ConflictException("Conflicting Metalink whole-file "+Checksums.label(algorithm));
                whole.put(algorithm,next);sources.put(algorithm,"metalink-publisher-"+algorithm);
            }
            String sha=whole.getOrDefault("sha256","");
            if(sha.isEmpty())throw new IOException("Trusted mirrors require the publisher's whole-file SHA-256");
            List<Element> urls=children(file,"url");if(urls.isEmpty()||urls.size()>32)throw new IOException("Metalink requires 1 to 32 mirror URLs");
            urls.sort(Comparator.comparingLong(value->{try{return integer(value.getAttribute("priority").isEmpty()?"999999":value.getAttribute("priority"),true);}catch(IOException error){return Long.MAX_VALUE;}}));
            List<String> mirrors=new ArrayList<>();for(Element element:urls){String text=element.getTextContent().trim();URI target;try{target=source.resolve(text);}catch(RuntimeException error){throw new IOException("Invalid Metalink mirror URL");}String mirror=SourceTransfer.validate(target.toString()).toString();if(!mirrors.contains(mirror))mirrors.add(mirror);}
            Map<String,Object> value=new LinkedHashMap<>();value.put("path",name);value.put("filename",name.substring(name.lastIndexOf('/')+1));value.put("url",mirrors.get(0));value.put("mirrors",mirrors);value.put("size",size);value.putAll(whole);value.put("checksum_sources",sources);value.put("checksum_source","metalink-publisher-sha256");value.put("metalink_url",source.toString());
            List<Element> pieces=children(file,"pieces");if(pieces.size()>8)throw new IOException("Too many Metalink piece hash sets");boolean hadPieces=false;
            for(Element piece:pieces)if(piece.getAttribute("type").equalsIgnoreCase("sha-256")){
                if(hadPieces)throw new IOException("Duplicate Metalink SHA-256 piece set");hadPieces=true;long length=integer(piece.getAttribute("length"),true);
                if(length>Transport.MAX_SEGMENT)throw new IOException("Metalink piece size exceeds 64 MiB");List<String> hashes=new ArrayList<>();for(Element h:children(piece,"hash")){if(!h.getAttribute("type").isEmpty())throw new IOException("Invalid Metalink piece hash type");hashes.add(hash(h.getTextContent()));if(hashes.size()>100000)throw new IOException("Too many Metalink piece hashes");}
                if(hashes.size()!=(size==0?0:(size-1)/length+1))throw new IOException("Metalink piece hash count does not match file size");value.put("piece_length",length);value.put("piece_hashes",hashes);
            }
            files.add(value);
        }
        if(files.isEmpty())throw new IOException("Metalink contains no downloadable files");Map<String,Object> result=new LinkedHashMap<>();result.put("kind","metalink");result.put("source_url",source.toString());result.put("files",files);return result;
    }
    /** Validate each assembled publisher piece before the final file is accepted. */
    public static void verifyPieces(Path file,Map<String,Object> metadata)throws IOException {
        if(!(metadata.get("piece_hashes") instanceof List<?> hashes))return;
        long length=number(metadata.get("piece_length"));if(length<1||length>Transport.MAX_SEGMENT)throw new IOException("Invalid publisher piece length");long size=Files.size(file);if(hashes.size()!=(size==0?0:(size-1)/length+1))throw new Checksum.ConflictException("Publisher piece count does not match assembled size");
        try(InputStream input=Files.newInputStream(file)){
            byte[] buffer=new byte[64*1024];for(int index=0;index<hashes.size();index++){MessageDigest digest=digest();long remaining=Math.min(length,size-(long)index*length);while(remaining>0){int count=input.read(buffer,0,(int)Math.min(buffer.length,remaining));if(count<0)throw new EOFException("Incomplete publisher piece");digest.update(buffer,0,count);remaining-=count;}String actual=HexFormat.of().formatHex(digest.digest());if(!actual.equals(hashes.get(index)))throw new Checksum.ConflictException("Publisher SHA-256 piece verification failed at piece "+index);}
        }
    }
    static void validatePath(String path)throws IOException {if(path.isEmpty()||path.length()>4096||path.startsWith("/")||path.contains("\\")||path.contains(":")||path.chars().anyMatch(c->c<32||c==127)||Arrays.stream(path.split("/",-1)).anyMatch(part->part.isEmpty()||part.equals(".")||part.equals("..")||part.endsWith(".")||part.endsWith(" ")))throw new IOException("Unsafe source file path");}
    private static String hash(String text)throws IOException{String value=text.trim().toLowerCase(Locale.ROOT);if(!value.matches("[0-9a-f]{64}"))throw new IOException("Invalid publisher SHA-256");return value;}
    private static long number(Object value)throws IOException{if(!(value instanceof Number number))throw new IOException("Invalid publisher number");return integer(number.toString(),true);}
    private static long integer(String text,boolean positive)throws IOException{try{if(!text.matches("[0-9]+"))throw new NumberFormatException();long value=Long.parseLong(text);if(positive&&value==0)throw new NumberFormatException();return value;}catch(NumberFormatException error){throw new IOException("Invalid publisher metadata number");}}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception error){throw new AssertionError(error);}}
    static List<Element> children(Element parent,String name){List<Element> result=new ArrayList<>();for(Node child=parent.getFirstChild();child!=null;child=child.getNextSibling())if(child instanceof Element element&&NS.equals(element.getNamespaceURI())&&name.equals(element.getLocalName()))result.add(element);return result;}
}
