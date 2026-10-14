package multiroute;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Editors return validated canonical values; controller owns persistence and scheduling. */
final class DesktopOptionsDialog {
    private DesktopOptionsDialog(){}
    private static JPanel form(){JPanel panel=new JPanel(new GridBagLayout());panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return panel;}
    private static void row(JPanel panel,int y,String source,JComponent control){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=y;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(4,4,4,8);JLabel label=new JLabel();label.putClientProperty("html.disable",true);I18n.text(label,source);panel.add(label,c);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;panel.add(control,c);}
    private static void dataRow(JPanel panel,int y,String value,JComponent control){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=y;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(4,4,4,8);JLabel label=new JLabel(value);label.putClientProperty("html.disable",true);panel.add(label,c);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;panel.add(control,c);}
    private static JSpinner integer(long value,long min,long max){return new JSpinner(new SpinnerNumberModel(Math.max(min,Math.min(max,value)),min,max,1L));}
    private static JCheckBox check(String text,boolean checked){JCheckBox check=new JCheckBox();I18n.text(check,text);check.setSelected(checked);return check;}
    private static JScrollPane bounded(JPanel panel){JScrollPane scroll=new JScrollPane(panel);scroll.setPreferredSize(new java.awt.Dimension(760,Math.min(550,panel.getPreferredSize().height+20)));return scroll;}
    private static void commit(java.awt.Component component)throws java.text.ParseException{if(component instanceof JSpinner spinner)spinner.commitEdit();if(component instanceof java.awt.Container container)for(java.awt.Component child:container.getComponents())commit(child);}
    static Map<String,Object> queue(JFrame owner,Map<String,Object> current,List<String> gateways){
        JPanel panel=form();JSpinner active=integer(Main.num(current.get("max_active_jobs"),2),1,32),global=integer(Main.num(current.get("global_limit_bps"),0)/1024,0,1048576);
        row(panel,0,"Одновременных загрузок",active);row(panel,1,"Общий предел, КиБ/с (0 — без ограничения)",global);
        Map<String,JSpinner> per=new LinkedHashMap<>();Map<String,Object> old=current.get("gateway_limit_bps") instanceof Map<?,?>?Main.obj(current.get("gateway_limit_bps")):Map.of();int index=2;
        for(String ip:gateways){JSpinner cap=integer(Main.num(old.get(ip),0)/1024,0,1048576);per.put(ip,cap);row(panel,index++,I18n.t("Шлюз %s",ip)+" (KiB/s)",cap);}
        JCheckBox schedule=check("Работать по расписанию",Main.yes(current.get("schedule_enabled")));row(panel,index++,"Расписание",schedule);
        JTextField from=new JTextField(Main.str(current.getOrDefault("schedule_start","00:00"))),to=new JTextField(Main.str(current.getOrDefault("schedule_end","23:59")));
        row(panel,index++,"Начало (HH:mm)",from);row(panel,index++,"Конец (HH:mm)",to);
        JLabel timezone=new JLabel();I18n.text(timezone,"Часовой пояс: %s",current.getOrDefault("schedule_timezone",java.time.ZoneId.systemDefault().toString()));row(panel,index,"Время компьютера",timezone);
        ClipboardSupport.install(panel);
        while(JOptionPane.showConfirmDialog(owner,bounded(panel),I18n.t("Очередь и ограничения…"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){try{
            commit(panel);
            java.time.LocalTime.parse(from.getText().strip());java.time.LocalTime.parse(to.getText().strip());Map<String,Object> result=new LinkedHashMap<>();
            result.put("max_active_jobs",((Number)active.getValue()).longValue());result.put("global_limit_bps",((Number)global.getValue()).longValue()*1024);
            Map<String,Object> caps=new LinkedHashMap<>(old);per.forEach((ip,spinner)->caps.put(ip,((Number)spinner.getValue()).longValue()*1024));result.put("gateway_limit_bps",caps);
            result.put("schedule_enabled",schedule.isSelected());result.put("schedule_start",from.getText().strip());result.put("schedule_end",to.getText().strip());return result;
        }catch(Exception error){JOptionPane.showMessageDialog(owner,I18n.t("Введите время в формате HH:mm"),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}}
        return null;
    }
    static Map<String,Object> job(JFrame owner,Map<String,Object> job){
        JPanel panel=form();JSpinner priority=integer(Main.num(job.get("priority"),0),-100,100);row(panel,0,"Приоритет (выше — раньше)",priority);
        long start=Main.num(job.get("start_after_ms"),0);JTextField after=new JTextField(start<=0?"":java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().withNano(0).toString());
        row(panel,1,"Не начинать до (YYYY-MM-DDTHH:mm; пусто — сейчас)",after);JCheckBox seeding=check("Раздавать после завершения",Main.yes(job.get("seeding_enabled")));
        JSpinner minutes=integer(Main.num(job.get("seeding_minutes"),30),1,1440),ratio=new JSpinner(new SpinnerNumberModel(job.get("seeding_ratio") instanceof Number n?n.doubleValue():1.0,0.1,100.0,0.1));
        int index=2;boolean torrent="torrent".equals(job.get("kind"));Map<String,JSpinner> filePriorities=new LinkedHashMap<>();Map<String,Object> old=job.get("file_priorities") instanceof Map<?,?>?Main.obj(job.get("file_priorities")):Map.of();
        List<Object> selected=Main.arr(job.get("selected_files"));
        if(torrent){row(panel,index++,"BitTorrent",seeding);row(panel,index++,"Раздавать не более минут",minutes);row(panel,index++,"Остановить при коэффициенте раздачи",ratio);
            for(Object raw:Main.arr(job.get("files"))){Map<String,Object> file=Main.obj(raw);if(Main.yes(file.get("pad")))continue;String path=Main.str(file.get("path"));if(!selected.isEmpty()&&!selected.contains(path))continue;JSpinner value=integer(Main.num(old.get(path),4),0,7);filePriorities.put(path,value);dataRow(panel,index++,path,value);}
            JLabel hint=new JLabel();I18n.text(hint,"Приоритет файла: 0 — не скачивать, 1–7 — порядок выбора частей.");row(panel,index,"Файлы torrent",hint);}
        ClipboardSupport.install(panel);
        while(JOptionPane.showConfirmDialog(owner,bounded(panel),I18n.t("Параметры загрузки…"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){try{
            commit(panel);
            Map<String,Object> result=new LinkedHashMap<>();result.put("priority",((Number)priority.getValue()).longValue());String text=after.getText().strip();
            result.put("start_after_ms",text.isBlank()?0:java.time.LocalDateTime.parse(text).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            if(torrent){result.put("seeding_enabled",seeding.isSelected());result.put("seeding_minutes",((Number)minutes.getValue()).longValue());result.put("seeding_ratio",((Number)ratio.getValue()).doubleValue());
                Map<String,Object> priorities=new LinkedHashMap<>();filePriorities.forEach((path,value)->priorities.put(path,((Number)value.getValue()).longValue()));if(!priorities.isEmpty()&&priorities.values().stream().allMatch(v->Main.num(v,0)==0))throw new IllegalArgumentException("Выберите хотя бы один файл раздачи");result.put("file_priorities",priorities);}
            return result;
        }catch(Exception error){JOptionPane.showMessageDialog(owner,I18n.t("Проверьте дату и выбранные файлы"),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}}
        return null;
    }
}
