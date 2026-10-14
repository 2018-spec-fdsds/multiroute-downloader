package multiroute;

import java.io.*;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** Bounded publisher metadata parser: no DTD, external entities or external schema access. */
final class SecureXml {
    private static final int MAX_BYTES=2*1024*1024;
    private static final ScheduledExecutorService WATCH=Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"source-metadata-timeout");t.setDaemon(true);return t;});
    static byte[] read(HttpResponse<InputStream> response,int status)throws IOException {
        try(InputStream body=response.body()){
            if(response.statusCode()!=status)throw new Transport.NonRetryableException("Source metadata request failed: HTTP "+response.statusCode());
            String encoding=response.headers().firstValue("Content-Encoding").orElse("identity");if(!encoding.equalsIgnoreCase("identity"))throw new IOException("Unexpected metadata Content-Encoding");
            var timeout=WATCH.schedule(()->{try{body.close();}catch(IOException ignored){}},20,TimeUnit.SECONDS);
            byte[] bytes;try{bytes=body.readNBytes(MAX_BYTES+1);}finally{timeout.cancel(false);}
            if(bytes.length>MAX_BYTES)throw new IOException("Source metadata exceeds 2 MiB");return bytes;
        }
    }
    static Document parse(byte[] bytes)throws IOException {
        if(bytes.length>MAX_BYTES)throw new IOException("Source metadata exceeds 2 MiB");
        try {
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var builder=factory.newDocumentBuilder();builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){public void error(org.xml.sax.SAXParseException error)throws org.xml.sax.SAXException{throw error;}public void fatalError(org.xml.sax.SAXParseException error)throws org.xml.sax.SAXException{throw error;}});
            return builder.parse(new ByteArrayInputStream(bytes));
        }catch(Exception error){throw new IOException("Invalid or unsafe source XML metadata");}
    }
}
