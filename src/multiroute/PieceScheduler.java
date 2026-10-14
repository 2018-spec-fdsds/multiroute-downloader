package multiroute;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** Coordinator-only leases over the torrent's original, hash-checked pieces. */
final class PieceScheduler {
    static final long WINDOW_BYTES = 8L * 1024 * 1024;
    static final int MAX_WINDOW = 4;
    static final long HELP_AFTER = TimeUnit.SECONDS.toNanos(4);
    static final long STALL_AFTER = TimeUnit.SECONDS.toNanos(60);
    static final long RETRY_AFTER = TimeUnit.SECONDS.toNanos(2);
    final int window;
    private final boolean[] wanted, complete, enabled, dirty, queued, stalled;
    private final long[] leases;
    private int[] priorities;
    private final long[] firstAssigned, payload, lastActivity, retryAt;
    private final String[] routeOwner,avoidOwner;
    private final List<LinkedHashSet<Integer>> assigned = new ArrayList<>();
    private final List<Map<Integer,long[]>> pieceActivity = new ArrayList<>();
    private final ArrayDeque<Integer> pending = new ArrayDeque<>();
    private long rescued, expired;

    PieceScheduler(boolean[] wanted, boolean[] complete, int[] sizes, int routes, long now) {
        this(wanted,complete,sizes,initialEnabled(routes),now);
    }

    private static boolean[] initialEnabled(int routes) {
        if(routes<1 || routes>32)throw new IllegalArgumentException("Invalid piece queue");
        boolean[] result=new boolean[routes];Arrays.fill(result,true);return result;
    }

    PieceScheduler(boolean[] wanted, boolean[] complete, int[] sizes, boolean[] initialEnabled, long now) {
        int routes=initialEnabled.length;
        if(routes<1 || routes>32 || wanted.length!=complete.length || wanted.length!=sizes.length)
            throw new IllegalArgumentException("Invalid piece queue");
        this.wanted=wanted.clone();this.complete=complete.clone();leases=new long[wanted.length];
        firstAssigned=new long[wanted.length];queued=new boolean[wanted.length];
        enabled=initialEnabled.clone();dirty=new boolean[routes];Arrays.fill(dirty,true);stalled=new boolean[routes];
        payload=new long[routes];lastActivity=new long[routes];Arrays.fill(lastActivity,now);retryAt=new long[routes];
        routeOwner=new String[routes];for(int r=0;r<routes;r++)routeOwner[r]=Integer.toString(r);avoidOwner=new String[wanted.length];
        int largest=1;
        for(int p=0;p<wanted.length;p++)if(wanted[p]) {
            if(sizes[p]<1)throw new IllegalArgumentException("Invalid piece size");
            largest=Math.max(largest,sizes[p]);if(!complete[p])enqueue(p);
        }
        window=(int)Math.max(1,Math.min(MAX_WINDOW,WINDOW_BYTES/largest));
        for(int r=0;r<routes;r++){assigned.add(new LinkedHashSet<>());pieceActivity.add(new HashMap<>());}
        refill(now);
    }

    void observe(int route,long received,long now) {
        received=Math.max(0,received);
        if(received>payload[route])lastActivity[route]=now;
        payload[route]=received;
    }
    /** Actual bytes of this piece, including the peer's partially received native block. */
    void observePiece(int route,int piece,long received,long now) {
        long[] clock=pieceActivity.get(route).get(piece);if(clock==null)return;
        received=Math.max(0,received);if(received>clock[0])clock[1]=now;clock[0]=received;
    }

    /** Called only after the piece bytes and their proof have entered the shared store. */
    void verified(int piece,int route,long now) {
        if(!wanted[piece] || complete[piece])return;
        complete[piece]=true;lastActivity[route]=now;avoidOwner[piece]=null;
        for(int r=0;r<enabled.length;r++)if((leases[piece]&(1L<<r))!=0) {
            assigned.get(r).remove(piece);pieceActivity.get(r).remove(piece);dirty[r]=true;
        }
        leases[piece]=0;
    }

    void disable(int route) {
        if(!enabled[route])return;
        enabled[route]=false;release(route);dirty[route]=true;
    }
    /** Reusing a slot starts a fresh native session, so its cumulative counter needs a baseline. */
    void enable(int route,long received,long now) {
        enable(route,received,now,routeOwner[route]);
    }
    void enable(int route,long received,long now,String owner) {
        if(enabled[route])return;
        routeOwner[route]=Objects.requireNonNull(owner);
        enabled[route]=true;payload[route]=Math.max(0,received);lastActivity[route]=now;
        retryAt[route]=0;stalled[route]=false;dirty[route]=true;
    }
    boolean enabled(int route){return enabled[route];}
    boolean anyEnabled(){for(boolean value:enabled)if(value)return true;return false;}
    boolean takeStalled(int route){boolean value=stalled[route];stalled[route]=false;return value;}

    void refill(long now) {
        // Every lease has a separate clock: healthy traffic for another piece cannot mask a stall.
        for(int r=0;r<enabled.length;r++)if(enabled[r]) {
            for(int piece:new ArrayList<>(assigned.get(r)))if(now-pieceActivity.get(r).get(piece)[1]>=STALL_AFTER) {
                leases[piece]&=~(1L<<r);assigned.get(r).remove(piece);pieceActivity.get(r).remove(piece);
                if(leases[piece]==0&&!complete[piece])enqueue(piece);
                avoidOwner[piece]=routeOwner[r];expired++;dirty[r]=true;retryAt[r]=now+RETRY_AFTER;stalled[r]=true;
            }
        }
        // Round-robin initial fill also gives every selected gateway a first piece.
        boolean filled;
        do {
            filled=false;
            for(int r=0;r<enabled.length;r++)if(enabled[r]&&now>=retryAt[r]&&assigned.get(r).size()<window) {
                Integer p=nextPending(r);if(p!=null){lease(p,r,now);filled=true;}
            }
        }while(filled&&!pending.isEmpty());
        if(!pending.isEmpty() || enabled.length<2)return;
        // Endgame: one spare gateway races one old piece. The original keeps its progress.
        // At most two independent copies exist; whichever verifies first wins globally.
        for(int r=0;r<enabled.length;r++)if(enabled[r]&&now>=retryAt[r]&&assigned.get(r).isEmpty()) {
            int oldest=-1;
            for(int p=0;p<leases.length;p++)if(wanted[p]&&!complete[p]&&Long.bitCount(leases[p])==1&&
                    (leases[p]&(1L<<r))==0&&now-firstAssigned[p]>=HELP_AFTER&&
                    allowed(p,r)&&
                    (oldest<0 || firstAssigned[p]<firstAssigned[oldest]))oldest=p;
            if(oldest>=0){lease(oldest,r,now);rescued++;}
        }
    }

    void setPriorities(int[] values) {
        if(values.length!=wanted.length)throw new IllegalArgumentException("Invalid piece priorities");
        for(int value:values)if(value<0||value>7)throw new IllegalArgumentException("Invalid piece priority");
        priorities=values.clone();List<Integer> order=new ArrayList<>(pending);order.sort(Comparator.<Integer>comparingInt(p->-priorities[p]).thenComparingInt(p->p));pending.clear();pending.addAll(order);
        Arrays.fill(dirty,true);
    }
    int priority(int piece){return priorities==null?4:priorities[piece];}
    int[] pieces(int route){return assigned.get(route).stream().mapToInt(Integer::intValue).toArray();}
    boolean takeDirty(int route){boolean value=dirty[route];dirty[route]=false;return value;}
    long rescued(){return rescued;}
    long expired(){return expired;}
    private void release(int route) {
        for(int p:assigned.get(route)) {
            leases[p]&=~(1L<<route);if(leases[p]==0&&!complete[p])enqueue(p);
        }
        assigned.get(route).clear();pieceActivity.get(route).clear();dirty[route]=true;
    }
    private void enqueue(int piece){if(!queued[piece]){pending.addLast(piece);queued[piece]=true;}}
    private boolean allowed(int piece,int route) {
        if(!Objects.equals(avoidOwner[piece],routeOwner[route]))return true;
        for(int other=0;other<enabled.length;other++)if(enabled[other]&&!routeOwner[other].equals(routeOwner[route]))return false;
        return true;
    }
    private Integer nextPending(int route) {
        for(int remaining=pending.size();remaining>0;remaining--) {
            int p=pending.removeFirst();queued[p]=false;
            if(wanted[p]&&!complete[p]&&leases[p]==0) {
                if(allowed(p,route))return p;
                pending.addLast(p);queued[p]=true;
            }
        }
        return null;
    }
    private void lease(int piece,int route,long now) {
        if(leases[piece]==0)firstAssigned[piece]=now;
        if(!Objects.equals(avoidOwner[piece],routeOwner[route]))avoidOwner[piece]=null;
        leases[piece]|=1L<<route;assigned.get(route).add(piece);pieceActivity.get(route).put(piece,new long[]{0,now});dirty[route]=true;
        // Starting a fresh window grants this gateway a fresh progress deadline.
        if(assigned.get(route).size()==1)lastActivity[route]=now;
    }
}
