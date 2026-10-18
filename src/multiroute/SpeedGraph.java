package multiroute;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/** Bounded graph of observed rates. It never invents points between API snapshots. */
final class SpeedGraph extends JPanel {
    private static final int MAX=300;
    private final ArrayDeque<Map<String,Long>> samples=new ArrayDeque<>();
    private final LinkedHashMap<String,Integer> colors=new LinkedHashMap<>();
    private long lastObserved;
    SpeedGraph(){setPreferredSize(new Dimension(450,75));setMaximumSize(new Dimension(Integer.MAX_VALUE,85));setMinimumSize(new Dimension(180,65));setOpaque(false);}
    void clear(){samples.clear();colors.clear();lastObserved=0;repaint();}
    void observe(Map<String,Object> job){
        long now=System.nanoTime();if(now-lastObserved<700_000_000L)return;lastObserved=now;
        LinkedHashMap<String,Long> point=new LinkedHashMap<>();point.put("total",Math.max(0,Main.num(job.get("speed_bps"),0)));
        for(Object value:Main.arr(job.get("routes"))){Map<String,Object> route=Main.obj(value);String ip=Main.str(route.get("gateway"));
            if(!ip.isBlank()&&Main.yes(route.get("enabled")))point.put(ip,Math.max(0,Main.num(route.get("speed_bps"),0)));}
        for(String key:point.keySet())colors.computeIfAbsent(key,k->colors.size());
        samples.addLast(Map.copyOf(point));while(samples.size()>MAX)samples.removeFirst();colors.keySet().removeIf(key->samples.stream().noneMatch(row->row.containsKey(key)));repaint();
    }
    int sampleCount(){return samples.size();}
    @Override protected void paintComponent(Graphics original){
        super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int left=45,top=22,right=Math.max(left+1,getWidth()-10),bottom=Math.max(top+1,getHeight()-20);
            g.setColor(DesktopTheme.muted());g.drawRect(left,top,right-left,bottom-top);
            List<Map<String,Long>> rows=new ArrayList<>(samples);long max=1;for(Map<String,Long> row:rows)for(long value:row.values())max=Math.max(max,value);
            g.drawString(I18n.t("История скорости"),left,15);
            g.drawString(String.format(java.util.Locale.ROOT,"%.1f",max*8.0/1_000_000),2,top+10);
            g.drawString("Mbit/s",2,bottom);g.drawString("0",left,bottom+15);
            for(var entry:colors.entrySet()){
                g.setColor(color(entry.getKey(),entry.getValue()));int previousX=-1,previousY=-1;
                for(int i=0;i<rows.size();i++){
                    Long value=rows.get(i).get(entry.getKey());if(value==null){previousX=-1;continue;}
                    int x=left+(right-left)*i/Math.max(MAX-1,1),y=bottom-(int)((bottom-top)*(double)value/max);
                    if(previousX>=0)g.drawLine(previousX,previousY,x,y);previousX=x;previousY=y;
                }
            }
            g.setColor(DesktopTheme.accent());g.drawString(I18n.t("Общая скорость"),Math.max(left,right-125),bottom+15);
            StringBuilder legend=new StringBuilder("<html>");
            legend.append(I18n.t("Общая скорость")).append(" · ");
            for(var entry:colors.entrySet())if(!entry.getKey().equals("total"))legend.append("<span style='color:#")
                    .append(String.format(java.util.Locale.ROOT,"%06x",color(entry.getKey(),entry.getValue()).getRGB()&0xffffff)).append("'>")
                    .append(entry.getKey()).append("</span> · ");
            setToolTipText(legend.append("</html>").toString());
        }finally{g.dispose();}
    }
    private static Color color(String key,int index){return "total".equals(key)?DesktopTheme.accent():DesktopTheme.graphColor(index);}
}
