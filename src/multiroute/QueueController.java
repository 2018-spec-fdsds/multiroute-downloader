package multiroute;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Persisted policy; Manager alone decides when queued jobs may start. */
final class QueueController {
    private final Path file;
    private Map<String,Object> settings;
    final TransferLimits limits=new TransferLimits();
    QueueController(Path root)throws IOException {
        file=root.resolve("queue-settings.config");settings=defaults();
        if(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) {
            Main.safePath(file);if(Files.size(file)>65536)throw new IOException("Queue settings are too large");
            try{settings=validated(Main.obj(Json.parse(Files.readString(file))));}
            catch(RuntimeException invalid){throw new IOException("Invalid queue settings",invalid);}
        }
        applyLimits();
    }
    static Map<String,Object> defaults(){Map<String,Object> value=new LinkedHashMap<>();value.put("max_active_jobs",2);value.put("global_limit_bps",0L);value.put("gateway_limit_bps",Map.of());value.put("schedule_enabled",false);value.put("schedule_start","00:00");value.put("schedule_end","00:00");return value;}
    synchronized Map<String,Object> snapshot(){return Main.obj(Json.parse(Json.stringify(settings)));}
    synchronized Map<String,Object> update(Map<String,Object> delta)throws IOException {
        Map<String,Object> next=new LinkedHashMap<>(settings);next.putAll(delta);next=validated(next);
        Main.safePath(file);Main.save(file,next);settings=next;applyLimits();return snapshot();
    }
    synchronized int maximum(){return ((Number)settings.get("max_active_jobs")).intValue();}
    synchronized boolean allowed(Instant now) {
        if(!Boolean.TRUE.equals(settings.get("schedule_enabled")))return true;
        LocalTime from=LocalTime.parse(Main.str(settings.get("schedule_start"))),to=LocalTime.parse(Main.str(settings.get("schedule_end")));
        LocalTime time=now.atZone(ZoneId.systemDefault()).toLocalTime();
        return from.equals(to)||(from.isBefore(to)?!time.isBefore(from)&&time.isBefore(to):!time.isBefore(from)||time.isBefore(to));
    }
    @SuppressWarnings("unchecked") private void applyLimits(){limits.configure(Main.num(settings.get("global_limit_bps"),0),(Map<String,Long>)(Map<?,?>)settings.get("gateway_limit_bps"));}
    private static Map<String,Object> validated(Map<String,Object> input) {
        Set<String> allowed=defaults().keySet();for(String key:input.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown queue option: "+key);
        Map<String,Object> result=defaults();result.putAll(input);
        long maximum=integer(result.get("max_active_jobs"),1,32,"max_active_jobs");result.put("max_active_jobs",(int)maximum);
        result.put("global_limit_bps",integer(result.get("global_limit_bps"),0,1_000_000_000_000L,"global_limit_bps"));
        if(!(result.get("gateway_limit_bps") instanceof Map<?,?> raw)||raw.size()>32)throw new IllegalArgumentException("Invalid gateway speed limits");
        Map<String,Long> rates=new LinkedHashMap<>();for(var entry:raw.entrySet()){String gateway=Main.str(entry.getKey());if(!gateway.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))throw new IllegalArgumentException("Invalid gateway speed limit address");for(String octet:gateway.split("\\."))if(Integer.parseInt(octet)>255)throw new IllegalArgumentException("Invalid gateway speed limit address");rates.put(gateway,integer(entry.getValue(),0,1_000_000_000_000L,"gateway_limit_bps"));}result.put("gateway_limit_bps",rates);
        if(!(result.get("schedule_enabled") instanceof Boolean))throw new IllegalArgumentException("Invalid schedule_enabled");
        for(String field:List.of("schedule_start","schedule_end")){String time=Main.str(result.get(field));if(!time.matches("[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("Schedule requires HH:mm");LocalTime.parse(time);result.put(field,time);}
        return result;
    }
    static long integer(Object value,long minimum,long maximum,String field){if(!(value instanceof Number number)||!Double.isFinite(number.doubleValue())||number.doubleValue()!=number.longValue()||number.longValue()<minimum||number.longValue()>maximum)throw new IllegalArgumentException("Invalid "+field);return number.longValue();}
}
