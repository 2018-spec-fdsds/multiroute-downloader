package multiroute;

import java.io.*;
import java.net.http.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.sevenz.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.zip.*;

/** Bounded remote directory listing. Never extracts entries, creates files or executes content. */
public final class ArchivePreview {
    static final long BUDGET=16L*1024*1024;
    static final int MAX_ENTRIES=10000, BLOCK=16*1024;
    private static final ScheduledExecutorService TIMEOUTS=Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"archive-preview-timeouts");t.setDaemon(true);return t;});
    @FunctionalInterface interface Opener {HttpResponse<InputStream> open(Map<String,Object> route,String url,long from,long to,String etag,String modified)throws IOException,InterruptedException;}
    static volatile Opener opener=SourceTransfer::open; // Package-only owned-fixture seam.
    private ArchivePreview() { }

    public static Map<String,Object> inspect(Map<String,Object> request)throws IOException,InterruptedException {
        Map<String,Object> clean=new LinkedHashMap<>(request);Object suppliedPassword=clean.remove("password");
        if(suppliedPassword!=null&&!(suppliedPassword instanceof char[])&&!(suppliedPassword instanceof String))throw new IOException("Invalid site transfer request");
        Map<String,Object> input=SiteProfiles.apply(clean);
        String url=SourceTransfer.validate(Main.str(input.get("url"))).toString();
        Map<String,Object> route=input.get("route") instanceof Map<?,?>?Main.obj(input.get("route")):Main.arr(input.get("routes")).stream().filter(value->!Boolean.FALSE.equals(Main.obj(value).get("enabled"))).map(Main::obj).findFirst().orElse(Map.of());
        if(!"gateway".equals(route.get("type")))throw new IOException("Archive preview requires a selected application gateway");
        if(input.get("site_profile_id") instanceof String id&&!id.isBlank()){route=new LinkedHashMap<>(route);route.put("site_profile_id",id);}
        Charset charset;
        try{charset=input.get("charset") instanceof String value&&!value.isBlank()?Charset.forName(value):StandardCharsets.UTF_8;}catch(IllegalArgumentException error){throw new IOException("Invalid archive filename charset");}
        char[] password=suppliedPassword instanceof char[] value?value.clone():suppliedPassword instanceof String value?value.toCharArray():null;
        if(password!=null&&password.length>1024){Arrays.fill(password,'\0');throw new IOException("Archive password is too long");}
        List<Map<String,Object>> entries=new ArrayList<>();Map<String,Object> result=new LinkedHashMap<>();
        result.put("url",url);result.put("entries",entries);result.put("metadata_budget_bytes",BUDGET);result.put("truncated",false);result.put("range_supported",true);result.put("format","");
        try(RemoteChannel channel=new RemoteChannel(route,url)) {
            try {
                byte[] signature=read(channel,0,(int)Math.min(8,channel.size()));String format;
                if(starts(signature,new byte[]{'P','K',3,4})||starts(signature,new byte[]{'P','K',5,6})||starts(signature,new byte[]{'P','K',7,8}))format="ZIP";
                else if(starts(signature,new byte[]{0x37,0x7a,(byte)0xbc,(byte)0xaf,0x27,0x1c}))format="7Z";
                else if(starts(signature,new byte[]{'R','a','r','!',0x1a,7,1,0}))format="RAR5";
                else if(starts(signature,new byte[]{'R','a','r','!',0x1a,7,0}))format="RAR4";
                else if(channel.size()>=512&&TarArchiveInputStream.matches(read(channel,0,512),512))format="TAR";
                else throw new IOException("Expected a ZIP, RAR, TAR or 7z archive with readable headers");
                result.put("format",format);channel.position(0);
                switch(format){
                    case "ZIP"->zip(channel,charset,entries);
                    case "7Z"->seven(channel,password,entries);
                    case "TAR"->tar(channel,charset,entries);
                    default->rar(channel,format.equals("RAR5"),charset,entries,result);
                }
                result.putIfAbsent("message","Archive directory read without extracting files");
            }catch(Limit limit){result.put("truncated",true);result.put("message",limit.getMessage());}
            catch(PasswordRequiredException encrypted){result.put("truncated",true);result.put("password_required",true);result.put("message","Archive headers are encrypted; enter the archive password");}
            finally{result.put("bytes_fetched",channel.fetched);}
        }finally{if(password!=null)Arrays.fill(password,'\0');}
        return result;
    }
    private static void add(List<Map<String,Object>> entries,String name,long size,long compressed,boolean directory,boolean encrypted)throws IOException {
        if(entries.size()>=MAX_ENTRIES)throw new Limit("Archive preview is limited to 10000 entries");
        if(name==null||name.length()>65536||name.indexOf('\ufffd')>=0)throw new IOException("Archive filename could not be decoded; select its original charset");
        Map<String,Object> entry=new LinkedHashMap<>();entry.put("name",name);entry.put("path",name);entry.put("size",size);entry.put("compressed_size",compressed);entry.put("directory",directory);entry.put("encrypted",encrypted);entries.add(entry);
    }
    private static void zip(RemoteChannel channel,Charset charset,List<Map<String,Object>> entries)throws IOException {
        // Bound library object allocation before it builds the central-directory index.
        byte[] tail=read(channel,Math.max(0,channel.size()-65557),(int)Math.min(65557,channel.size()));int end=-1;
        for(int i=tail.length-22;i>=0;i--)if(u32(tail,i)==0x06054b50L&&i+22+u16(tail,i+20)==tail.length){end=i;break;}
        if(end<0)throw new IOException("Invalid ZIP end of directory");
        if(u16(tail,end+4)!=0||u16(tail,end+6)!=0)throw new IOException("Preview of multi-volume ZIP archives is not supported");
        long count=u16(tail,end+10);
        if(count==65535){long offset=channel.size()-tail.length+end;byte[] locator=read(channel,offset-20,20);if(u32(locator,0)!=0x07064b50L||u32(locator,4)!=0||u32(locator,16)!=1)throw new IOException("Invalid ZIP64 locator");byte[] record=read(channel,u64(locator,8),56);if(u32(record,0)!=0x06064b50L)throw new IOException("Invalid ZIP64 directory");count=u64(record,32);}
        if(count>MAX_ENTRIES)throw new Limit("Archive preview is limited to 10000 entries");
        channel.position(0);
        try(ZipFile archive=ZipFile.builder().setSeekableByteChannel(channel.nonClosing()).setCharset(charset).setUseUnicodeExtraFields(true).setIgnoreLocalFileHeader(true).get()){
            Enumeration<ZipArchiveEntry> directory=archive.getEntries();while(directory.hasMoreElements()){ZipArchiveEntry entry=directory.nextElement();add(entries,entry.getName(),entry.getSize(),entry.getCompressedSize(),entry.isDirectory(),entry.getGeneralPurposeBit().usesEncryption());}
        }
    }
    private static void seven(RemoteChannel channel,char[] password,List<Map<String,Object>> entries)throws IOException {
        var builder=SevenZFile.builder().setSeekableByteChannel(channel.nonClosing()).setMaxMemoryLimitKb(65536);if(password!=null)builder.setPassword(password);
        try(SevenZFile archive=builder.get()){
            // getEntries reads the index only. getNextEntry would also skip/decode preceding payloads.
            for(SevenZArchiveEntry entry:archive.getEntries()){
                boolean encrypted=false;if(entry.getContentMethods()!=null)for(SevenZMethodConfiguration method:entry.getContentMethods())if(method.getMethod()==SevenZMethod.AES256SHA256)encrypted=true;
                add(entries,entry.getName(),entry.getSize(),-1,entry.isDirectory(),encrypted);
            }
        }
    }
    private static void tar(RemoteChannel channel,Charset charset,List<Map<String,Object>> entries)throws IOException {
        // TarFile seeks over file bodies; TarArchiveInputStream deliberately reads skipped bytes.
        try(TarFile archive=new TarFile(channel.nonClosing(),512,512,charset.name(),false)){
            for(TarArchiveEntry entry:archive.getEntries()){if(!entry.isCheckSumOK())throw new IOException("Invalid TAR header checksum");add(entries,entry.getName(),entry.getSize(),entry.getSize(),entry.isDirectory(),false);}
        }
    }

    private static void rar(RemoteChannel channel,boolean v5,Charset charset,List<Map<String,Object>> entries,Map<String,Object> result)throws IOException {
        long offset=v5?8:7;boolean ended=false;
        while(offset<channel.size()){
            if(entries.size()>=MAX_ENTRIES)throw new Limit("Archive preview is limited to 10000 entries");
            byte[] header;long packed=0;int type,flags;
            if(v5){
                byte[] start=read(channel,offset,(int)Math.min(8,channel.size()-offset));Cursor cursor=new Cursor(start,4);long length=cursor.vint();int sizeBytes=cursor.index-4;if(length<2||length>2*1024*1024||sizeBytes>3)throw new IOException("Invalid RAR5 header size");
                header=read(channel,offset,(int)(4+sizeBytes+length));CRC32 crc=new CRC32();crc.update(header,4,header.length-4);if(crc.getValue()!=u32(header,0))throw new IOException("Invalid RAR header checksum");
                cursor=new Cursor(header,4+sizeBytes);type=cursor.integer();flags=cursor.integer();long extra=(flags&1)!=0?cursor.vint():0;packed=(flags&2)!=0?cursor.vint():0;
                if(extra>header.length-cursor.index)throw new IOException("Invalid RAR extra area");int extraStart=header.length-(int)extra;
                if(type==4){result.put("password_required",true);throw new Limit("Preview of encrypted RAR headers is not supported");}
                if(type==1){if((cursor.integer()&1)!=0){result.put("truncated",true);result.put("message","Only the selected RAR volume is listed");}}
                if(type==2){int fileFlags=cursor.integer();long size=cursor.vint();cursor.vint();if((fileFlags&2)!=0)cursor.skip(4);if((fileFlags&4)!=0)cursor.skip(4);cursor.vint();cursor.vint();int nameLength=cursor.integer();if(nameLength>extraStart-cursor.index)throw new IOException("Invalid RAR filename length");String name=decode(cursor.bytes(nameLength),StandardCharsets.UTF_8);boolean encrypted=false;
                    Cursor extras=new Cursor(header,extraStart);while(extras.index<header.length){long record=extras.vint();if(record<1||record>header.length-extras.index)throw new IOException("Invalid RAR extra record");int next=extras.index+(int)record;int recordType=extras.integer();if(recordType==1)encrypted=true;extras.index=next;}
                    add(entries,name,(fileFlags&8)!=0?-1:size,packed,(fileFlags&1)!=0,encrypted);
                }
                if((flags&24)!=0){result.put("truncated",true);result.put("message","Only the selected RAR volume is listed");}
                if(type==5){ended=true;break;}
            }else{
                byte[] base=read(channel,offset,7);type=base[2]&255;flags=u16(base,3);int length=u16(base,5);if(length<7)throw new IOException("Invalid RAR4 header size");header=read(channel,offset,length);CRC32 crc=new CRC32();crc.update(header,2,header.length-2);if((crc.getValue()&65535)!=u16(header,0))throw new IOException("Invalid RAR header checksum");
                if((flags&0x8000)!=0){if(length<11)throw new IOException("Invalid RAR data area");packed=u32(header,7);}
                if(type==0x73&&(flags&0x80)!=0){result.put("password_required",true);throw new Limit("Preview of encrypted RAR headers is not supported");}
                if(type==0x73&&(flags&1)!=0){result.put("truncated",true);result.put("message","Only the selected RAR volume is listed");}
                if(type==0x74){if(length<32)throw new IOException("Invalid RAR4 file header");long size=u32(header,11);int nameLength=u16(header,26),nameOffset=32;if((flags&0x100)!=0){if(length<40)throw new IOException("Invalid RAR4 large file header");long high=u32(header,32);if(high>Integer.MAX_VALUE)throw new IOException("RAR size overflow");packed|=high<<32;high=u32(header,36);if(high>Integer.MAX_VALUE)throw new IOException("RAR size overflow");size|=high<<32;nameOffset=40;}
                    if(nameLength>length-nameOffset)throw new IOException("Invalid RAR filename length");byte[] name=Arrays.copyOfRange(header,nameOffset,nameOffset+nameLength);String text=(flags&0x200)!=0?rarUnicode(name,charset):decode(name,charset);add(entries,text,size,packed,(flags&0xe0)==0xe0,(flags&4)!=0);
                    if((flags&3)!=0){result.put("truncated",true);result.put("message","Only the selected RAR volume is listed");}
                }
                if(type==0x7b){ended=true;break;}
            }
            long next=offset+header.length;if(packed<0||packed>channel.size()-next)throw new IOException("RAR data area exceeds source size");offset=next+packed;
        }
        if(!ended&&v5)throw new IOException("RAR archive has no end header");
    }
    /** RAR4's stored Unicode name stream; ANSI fallback is caller-selected, UTF-8 by default. */
    private static String rarUnicode(byte[] input,Charset charset)throws IOException {
        int zero=0;while(zero<input.length&&input[zero]!=0)zero++;if(zero==input.length)return decode(input,charset);if(zero+1==input.length)return decode(Arrays.copyOf(input,zero),charset);
        Cursor encoded=new Cursor(input,zero+1);int high=encoded.integerByte(),bits=0,flags=0;StringBuilder name=new StringBuilder();
        while(encoded.index<input.length){if(bits==0){flags=encoded.integerByte();bits=8;}int mode=(flags>>6)&3;flags<<=2;bits-=2;
            switch(mode){case 0->name.append((char)encoded.integerByte());case 1->name.append((char)((high<<8)|encoded.integerByte()));case 2->name.append((char)(encoded.integerByte()|(encoded.integerByte()<<8)));default->{int count=encoded.integerByte();boolean correction=(count&128)!=0;int adjust=correction?encoded.integerByte():0;count=(count&127)+2;for(int i=0;i<count;i++){int index=name.length();if(index>=zero)throw new IOException("Invalid RAR Unicode filename");name.append((char)(correction?(high<<8)|(((input[index]&255)+adjust)&255):(input[index]&255)));}}}
            if(name.length()>65536)throw new IOException("Oversized RAR filename");
        }return name.toString();
    }
    private static String decode(byte[] bytes,Charset charset)throws IOException{return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();}
    private static boolean starts(byte[] source,byte[] expected){return source.length>=expected.length&&Arrays.equals(Arrays.copyOf(source,expected.length),expected);}
    private static int u16(byte[] bytes,int off)throws IOException{if(off<0||off+2>bytes.length)throw new EOFException("Truncated archive header");return (bytes[off]&255)|((bytes[off+1]&255)<<8);}
    private static long u32(byte[] bytes,int off)throws IOException{return Integer.toUnsignedLong(u16(bytes,off)|(u16(bytes,off+2)<<16));}
    private static long u64(byte[] bytes,int off)throws IOException{long high=u32(bytes,off+4);if(high>Integer.MAX_VALUE)throw new IOException("Archive size exceeds supported range");return u32(bytes,off)|(high<<32);}
    private static byte[] read(RemoteChannel channel,long position,int length)throws IOException{if(position<0||length<0||position>channel.size()-length)throw new EOFException("Truncated archive header");channel.position(position);byte[] bytes=new byte[length];ByteBuffer out=ByteBuffer.wrap(bytes);while(out.hasRemaining())if(channel.read(out)<0)throw new EOFException("Truncated archive header");return bytes;}
    static final class Limit extends IOException{Limit(String message){super(message);}}
    static final class Cursor{final byte[] bytes;int index;Cursor(byte[] bytes,int index){this.bytes=bytes;this.index=index;}int integerByte()throws IOException{if(index>=bytes.length)throw new EOFException("Truncated archive header");return bytes[index++]&255;}void skip(int n)throws IOException{if(n<0||n>bytes.length-index)throw new EOFException("Truncated archive header");index+=n;}byte[] bytes(int n)throws IOException{int start=index;skip(n);return Arrays.copyOfRange(bytes,start,index);}long vint()throws IOException{long result=0;for(int shift=0;shift<63;shift+=7){int next=integerByte();if(shift==56&&(next&128)!=0)throw new IOException("Archive integer overflow");result|=(long)(next&127)<<shift;if((next&128)==0)return result;}throw new IOException("Archive integer overflow");}int integer()throws IOException{long value=vint();if(value>Integer.MAX_VALUE)throw new IOException("Archive integer overflow");return (int)value;}}

    static final class RemoteChannel implements SeekableByteChannel {
        final Map<String,Object> route;final String url;final long size;final String etag,modified;long position,fetched;boolean open=true;
        final long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        final Map<Long,byte[]> cache=new LinkedHashMap<>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<Long,byte[]> entry){return size()>32;}};
        RemoteChannel(Map<String,Object> route,String url)throws IOException,InterruptedException{
            this.route=Map.copyOf(route);this.url=url;HttpResponse<InputStream> response=opener.open(route,url,0,0,null,null);
            try(InputStream body=response.body()){
                size=Transport.validate(response,0,0,null,null,null);etag=response.headers().firstValue("ETag").filter(value->!value.startsWith("W/")).orElse("");modified=response.headers().firstValue("Last-Modified").orElse("");
                if(etag.isEmpty()&&modified.isEmpty())throw new IOException("Archive preview needs an ETag or Last-Modified to combine byte ranges safely");
                ScheduledFuture<?> timeout=TIMEOUTS.schedule(()->{try{body.close();}catch(IOException ignored){}},15,TimeUnit.SECONDS);try{if(body.read()<0)throw new EOFException("Truncated source range");fetched=1;}finally{timeout.cancel(false);}
            }
        }
        private void check()throws IOException{if(!open)throw new ClosedChannelException();if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Archive preview cancelled");if(System.nanoTime()>deadline)throw new Limit("Archive preview reached its time limit");}
        public int read(ByteBuffer target)throws IOException{
            check();if(!target.hasRemaining())return 0;if(position>=size)return -1;int total=0;
            while(target.hasRemaining()&&position<size){check();long block=position/BLOCK;byte[] bytes=cache.get(block);
                if(bytes==null){long from=block*BLOCK,to=Math.min(size-1,from+BLOCK-1);int length=(int)(to-from+1);if(length>BUDGET-fetched)throw new Limit("Archive preview reached its 16 MiB metadata limit");
                    HttpResponse<InputStream> response;try{response=opener.open(route,url,from,to,etag,modified);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new InterruptedIOException("Archive preview cancelled");}
                    try(InputStream body=response.body()){
                        Transport.validate(response,from,to,size,etag,modified);ScheduledFuture<?> timeout=TIMEOUTS.schedule(()->{try{body.close();}catch(IOException ignored){}},Math.max(1,Math.min(15,TimeUnit.NANOSECONDS.toSeconds(deadline-System.nanoTime()))),TimeUnit.SECONDS);
                        try{bytes=new byte[length];int got=0;while(got<length){check();int n=body.read(bytes,got,length-got);if(n<0)throw new EOFException("Truncated source range");got+=n;fetched+=n;}if(body.read()!=-1)throw new IOException("Source exceeded requested range");}finally{timeout.cancel(false);}
                    }cache.put(block,bytes);
                }
                int offset=(int)(position%BLOCK),n=Math.min(target.remaining(),bytes.length-offset);target.put(bytes,offset,n);position+=n;total+=n;
            }return total;
        }
        SeekableByteChannel nonClosing(){return new SeekableByteChannel(){public int read(ByteBuffer bytes)throws IOException{return RemoteChannel.this.read(bytes);}public long position()throws IOException{return RemoteChannel.this.position();}public SeekableByteChannel position(long value)throws IOException{RemoteChannel.this.position(value);return this;}public long size(){return size;}public boolean isOpen(){return open;}public void close(){}public int write(ByteBuffer bytes)throws IOException{throw new NonWritableChannelException();}public SeekableByteChannel truncate(long size)throws IOException{throw new NonWritableChannelException();}};}
        public long position()throws IOException{check();return position;}public SeekableByteChannel position(long value)throws IOException{check();if(value<0||value>size)throw new IOException("Invalid archive seek");position=value;return this;}public long size(){return size;}public boolean isOpen(){return open;}public void close(){open=false;cache.clear();}public int write(ByteBuffer bytes)throws IOException{throw new NonWritableChannelException();}public SeekableByteChannel truncate(long length)throws IOException{throw new NonWritableChannelException();}
    }
}
