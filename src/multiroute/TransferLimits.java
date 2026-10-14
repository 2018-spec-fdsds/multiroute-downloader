package multiroute;

import java.io.InterruptedIOException;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared payload budget for every job. Native torrent reservations reduce the HTTP budget. */
final class TransferLimits {
    private volatile long global;
    private volatile Map<String,Long> gateways = Map.of();
    private final Map<String,Long> clocks = new HashMap<>();
    private final Map<String,Reservation> nativeUsers = new HashMap<>();
    private record Reservation(String gateway) { }
    synchronized void configure(long global, Map<String,Long> gateways) {
        this.global = global; this.gateways = Map.copyOf(gateways); clocks.clear(); notifyAll();
    }
    synchronized void registerNative(String owner, String gateway) { nativeUsers.put(owner,new Reservation(gateway)); }
    synchronized void unregisterNative(String owner) { nativeUsers.remove(owner); notifyAll(); }
    synchronized int nativeLimit(String owner) {
        Reservation current = nativeUsers.get(owner); if(current == null) return 0;
        int users = Math.max(1,nativeUsers.size()+1);
        long limit = global == 0 ? 0 : Math.max(1,global/users);
        long gateway = gateways.getOrDefault(current.gateway,0L);
        long same = nativeUsers.values().stream().filter(r->r.gateway.equals(current.gateway)).count()+1;
        if(gateway>0) limit=limit==0?Math.max(1,gateway/same):Math.min(limit,Math.max(1,gateway/same));
        return (int)Math.min(Integer.MAX_VALUE,limit);
    }
    private synchronized long httpLimit(String gateway) {
        long limit=global;
        if(limit>0 && !nativeUsers.isEmpty()) limit=Math.max(1,limit/(nativeUsers.size()+1));
        return limit;
    }
    /** Reservations are made before read; no more than 32 KiB can be in flight per HTTP stream. */
    int acquire(String gateway,int requested,BooleanSupplier cancelled,Runnable paced) throws InterruptedIOException {
        int grant=Math.min(32768,requested); long deadline;
        synchronized(this) {
            long all=httpLimit(gateway), per=gateways.getOrDefault(gateway,0L);
            long nativeCount=nativeUsers.values().stream().filter(r->r.gateway.equals(gateway)).count();
            if(per>0&&nativeCount>0)per=Math.max(1,per/(nativeCount+1));
            if(all==0&&per==0)return grant;
            long effective=all==0?per:per==0?all:Math.min(all,per);
            grant=(int)Math.max(1,Math.min(grant,effective/10));
            long now=System.nanoTime(); deadline=now;
            if(all>0)deadline=Math.max(deadline,clocks.getOrDefault("*",now));
            if(per>0)deadline=Math.max(deadline,clocks.getOrDefault(gateway,now));
            if(all>0)clocks.put("*",deadline+duration(grant,all));
            if(per>0)clocks.put(gateway,deadline+duration(grant,per));
        }
        while(System.nanoTime()<deadline) {
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Transfer cancelled");
            paced.run(); long remaining=deadline-System.nanoTime();
            try{Thread.sleep(Math.max(1,Math.min(100,remaining/1_000_000)));}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new InterruptedIOException("Transfer cancelled");}
        }
        if(cancelled.getAsBoolean())throw new InterruptedIOException("Transfer cancelled");
        return grant;
    }
    private static long duration(int bytes,long rate){return Math.max(1,(long)Math.ceil(bytes*1_000_000_000d/rate));}
}
