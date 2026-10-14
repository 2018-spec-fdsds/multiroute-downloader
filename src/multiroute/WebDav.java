package multiroute;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.w3c.dom.*;

/** RFC 4918 HTTPS WebDAV depth-one file listing; file GETs use the usual range transport. */
public final class WebDav {
    private static final String NS="DAV:";
    private WebDav(){}
    public static boolean isWebDav(String url){try{return "webdavs".equalsIgnoreCase(URI.create(url).getScheme());}catch(RuntimeException error){return false;}}
    public static Map<String,Object> inspect(Map<String,Object> route,String url)throws IOException,InterruptedException {
        String http=SourceTransfer.httpUrl(url);URI source=Transport.uri(http);if(!source.getScheme().equalsIgnoreCase("https"))throw new Transport.NonRetryableException("WebDAV metadata requires HTTPS");
        String body="<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:displayname/><d:getcontentlength/><d:getetag/><d:getlastmodified/><d:resourcetype/></d:prop></d:propfind>";
        return parse(SecureXml.read(Transport.request(route,http,"PROPFIND",Map.of("Depth","1","Content-Type","application/xml; charset=utf-8"),body),207),source);
    }
    static Map<String,Object> parse(byte[] bytes,URI source)throws IOException {
        Document document=SecureXml.parse(bytes);Element root=document.getDocumentElement();if(!NS.equals(root.getNamespaceURI())||!"multistatus".equals(root.getLocalName()))throw new IOException("Expected WebDAV multistatus XML");
        List<Map<String,Object>> files=new ArrayList<>();Set<String> urls=new HashSet<>();String basePath=source.getPath();boolean collection=basePath.endsWith("/");
        for(Element response:children(root,"response")){
            if(files.size()>=10000)throw new IOException("WebDAV listing contains too many files");List<Element> hrefs=children(response,"href");if(hrefs.size()!=1)throw new IOException("Invalid WebDAV resource URL");URI target;
            try{target=source.resolve(hrefs.get(0).getTextContent().trim()).normalize();}catch(RuntimeException error){throw new IOException("Invalid WebDAV resource URL");}
            target=Transport.uri(target.toString());if(!Transport.sameOrigin(source,target))throw new IOException("WebDAV resource left the selected source origin");
            String path=target.getPath();if(Arrays.stream(path.split("/",-1)).anyMatch(part->part.equals("..")||part.equals(".")))throw new IOException("Unsafe WebDAV resource path");
            if(collection&&!path.startsWith(basePath))throw new IOException("WebDAV resource is outside the selected folder");
            if(!collection&&!path.equals(basePath))throw new IOException("WebDAV resource differs from selected file");
            Element props=null;for(Element propstat:children(response,"propstat")){List<Element> statuses=children(propstat,"status");if(statuses.size()!=1)throw new IOException("Invalid WebDAV property status");if(statuses.get(0).getTextContent().trim().matches("HTTP/1\\.[01] 200(?: .*)?")){List<Element> values=children(propstat,"prop");if(values.size()!=1)throw new IOException("Invalid WebDAV properties");props=values.get(0);break;}}
            if(props==null)continue;boolean isCollection=false;for(Element resource:children(props,"resourcetype"))if(!children(resource,"collection").isEmpty())isCollection=true;if(isCollection)continue;
            String relative=collection?path.substring(basePath.length()):path.substring(path.lastIndexOf('/')+1);Metalink.validatePath(relative);if(collection&&relative.contains("/"))throw new IOException("WebDAV depth-one listing contains a nested file");
            List<Element> lengths=children(props,"getcontentlength");if(lengths.size()!=1)throw new IOException("WebDAV file size is missing");long size;try{String value=lengths.get(0).getTextContent().trim();if(!value.matches("[0-9]+"))throw new NumberFormatException();size=Long.parseLong(value);}catch(NumberFormatException error){throw new IOException("Invalid WebDAV file size");}
            if(!urls.add(target.toString()))throw new IOException("Duplicate WebDAV resource");Map<String,Object> file=new LinkedHashMap<>();file.put("path",relative);file.put("filename",relative);file.put("url",target.toString());file.put("size",size);file.put("sha256","");file.put("checksum_source","");file.put("etag",first(props,"getetag"));file.put("modified",first(props,"getlastmodified"));files.add(file);
        }
        if(!collection&&files.size()!=1)throw new IOException("WebDAV file is missing");Map<String,Object> result=new LinkedHashMap<>();result.put("kind",collection?"webdav":"file");result.put("source_url",source.toString());result.put("files",files);return result;
    }
    private static String first(Element parent,String name)throws IOException{List<Element> values=children(parent,name);if(values.size()>1)throw new IOException("Duplicate WebDAV file metadata");return values.isEmpty()?"":values.get(0).getTextContent().trim();}
    private static List<Element> children(Element parent,String name){List<Element> result=new ArrayList<>();for(Node child=parent.getFirstChild();child!=null;child=child.getNextSibling())if(child instanceof Element element&&NS.equals(element.getNamespaceURI())&&name.equals(element.getLocalName()))result.add(element);return result;}
}
