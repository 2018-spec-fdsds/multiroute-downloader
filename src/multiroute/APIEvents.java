package multiroute;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded, loss-tolerant SSE snapshots. Publishers never wait for a client or hold an output lock. */
final class APIEvents implements AutoCloseable {
    private final Set<ArrayBlockingQueue<String>> subscribers=ConcurrentHashMap.newKeySet();
    private final AtomicLong sequence=new AtomicLong();
    private volatile boolean closed;
    void publish(String type,Object data) {
        if(closed||subscribers.isEmpty())return;
        String event="id: "+sequence.incrementAndGet()+"\nevent: "+type+"\ndata: "+Json.stringify(data)+"\n\n";
        if(event.length()>262144)return;
        for(var queue:subscribers)if(!queue.offer(event)){queue.clear();queue.offer("event: reset\ndata: {\"reason\":\"slow_consumer\"}\n\n");}
    }
    void serve(HttpExchange exchange,Object initial) throws IOException {
        ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<>(64);
        synchronized(subscribers){if(closed||subscribers.size()>=32){Main.json(exchange,503,Map.of("error","Event client limit reached"));return;}subscribers.add(queue);}
        try {
            exchange.getResponseHeaders().set("Content-Type","text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control","no-store"); exchange.getResponseHeaders().set("X-Accel-Buffering","no");
            exchange.sendResponseHeaders(200,0);
            try(OutputStream out=exchange.getResponseBody()) {
            out.write(("event: snapshot\ndata: "+Json.stringify(initial)+"\n\n").getBytes(StandardCharsets.UTF_8));out.flush();
            while(!closed) {
                String event;
                try{event=queue.poll(15,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
                out.write((event==null?": heartbeat\n\n":event).getBytes(StandardCharsets.UTF_8));out.flush();
            }
            }
        }finally{subscribers.remove(queue);exchange.close();}
    }
    int subscribers(){return subscribers.size();}
    @Override public void close(){closed=true;for(var queue:subscribers)queue.offer(": closing\n\n");}
}
