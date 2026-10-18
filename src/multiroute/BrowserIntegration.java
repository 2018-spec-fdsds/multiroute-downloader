package multiroute;

import com.sun.net.httpserver.HttpExchange;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Explicit opt-in bridge for this project's fixed-ID Chrome/Edge extension. */
public final class BrowserIntegration {
    // Generated from the public key committed in browser-extension/manifest.json.
    static final String EXTENSION_ID="ljdklefabmaecjalfhjoicmgedmjjkbl";
    static final String ORIGIN="chrome-extension://"+EXTENSION_ID;
    private final Path settings;
    private volatile boolean enabled;
    BrowserIntegration(Path directory)throws IOException {
        settings=directory.resolve("browser-integration.config");
        if(Files.exists(settings,LinkOption.NOFOLLOW_LINKS)){
            Main.safePath(settings);if(!Files.isRegularFile(settings,LinkOption.NOFOLLOW_LINKS)||Files.size(settings)>8192)throw new IOException("Invalid site transfer request");
            Object saved=Main.obj(Json.parse(Files.readString(settings,StandardCharsets.UTF_8))).get("enabled");
            if(!(saved instanceof Boolean))throw new IOException("Invalid site transfer request");enabled=Boolean.TRUE.equals(saved);
        }
    }
    synchronized void enable(boolean value)throws IOException {Main.safePath(settings);Main.save(settings,Map.of("enabled",value));enabled=value;}
    boolean enabled(){return enabled;}
    static Path extensionDirectory()throws Exception {
        Path location=Path.of(BrowserIntegration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path candidate=(Files.isRegularFile(location)?location.getParent():Path.of("").toAbsolutePath()).resolve("browser-extension");
        if(!Files.isRegularFile(candidate.resolve("manifest.json")))throw new IOException("The browser extension folder is missing");
        return candidate;
    }
    public void open(JFrame owner){
        JPanel panel=new JPanel(new BorderLayout(8,12));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JTextArea notes=new JTextArea(8,65);notes.setEditable(false);notes.setLineWrap(true);notes.setWrapStyleWord(true);notes.setOpaque(false);notes.setFont(UIManager.getFont("Label.font"));
        I18n.text(notes,"В Chrome откройте chrome://extensions, в Edge — edge://extensions. Включите режим разработчика, нажмите «Загрузить распакованное» и выберите папку расширения. Это нужно один раз. Затем нажимайте «Скачать через MultiRoute» в меню ссылки или кнопку расширения. Приложение должно быть запущено, в том числе в трее. Расширение не читает cookies и не перехватывает остальные загрузки браузера.");
        JScrollPane scroll=new JScrollPane(notes);scroll.setPreferredSize(new Dimension(760,190));panel.add(scroll,BorderLayout.CENTER);
        JCheckBox allow=new JCheckBox();I18n.text(allow,"Разрешить расширению Chrome / Edge передавать ссылки");allow.setSelected(enabled);
        JButton folder=new JButton();I18n.text(folder,"Открыть папку расширения");folder.addActionListener(event->{try{Desktop.getDesktop().open(extensionDirectory().toFile());}catch(Exception error){JOptionPane.showMessageDialog(owner,DiagnosticText.render(error.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}});
        JPanel bottom=new JPanel(new BorderLayout(8,8));bottom.add(allow,BorderLayout.NORTH);bottom.add(folder,BorderLayout.SOUTH);panel.add(bottom,BorderLayout.SOUTH);ClipboardSupport.install(panel);I18n.refresh(panel);
        if(JOptionPane.showConfirmDialog(owner,panel,I18n.t("Интеграция с Chrome / Edge"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)try{enable(allow.isSelected());}catch(IOException error){JOptionPane.showMessageDialog(owner,DiagnosticText.render(error.getMessage()),I18n.t("Ошибка"),JOptionPane.ERROR_MESSAGE);}
    }
    void handle(HttpExchange exchange,Main.Manager manager)throws IOException {
        String origin=exchange.getRequestHeaders().getFirst("Origin"),path=exchange.getRequestURI().getPath(),method=exchange.getRequestMethod();
        if(!enabled||!ORIGIN.equals(origin)){Main.json(exchange,403,Map.of("error","Browser integration is disabled or the extension origin is not allowed"));return;}
        if(!Set.of("/api/v1/browser/status","/api/v1/browser/download").contains(path)){Main.json(exchange,404,Map.of("error","Not found"));return;}
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",ORIGIN);exchange.getResponseHeaders().set("Vary","Origin");
        if(method.equals("OPTIONS")){
            String requested=Objects.toString(exchange.getRequestHeaders().getFirst("Access-Control-Request-Method"),"");
            String headers=Objects.toString(exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers"),"");
            if(!Set.of("GET","POST").contains(requested)||Arrays.stream(headers.split(",")).map(String::trim).anyMatch(value->!value.isEmpty()&&!value.equalsIgnoreCase("content-type"))){Main.json(exchange,403,Map.of("error","Browser request is not allowed"));return;}
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods","GET, POST");exchange.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
            if("true".equals(exchange.getRequestHeaders().getFirst("Access-Control-Request-Private-Network")))exchange.getResponseHeaders().set("Access-Control-Allow-Private-Network","true");
            exchange.sendResponseHeaders(204,-1);exchange.close();return;
        }
        if(path.endsWith("/status")&&method.equals("GET")){Main.json(exchange,200,Map.of("application",Brand.NAME,"version",Main.VERSION,"ready",manager.browserDownload!=null));return;}
        if(!path.endsWith("/download")||!method.equals("POST")){Main.json(exchange,405,Map.of("error","Method not allowed"));return;}
        Map<String,Object> input=Main.request(exchange);
        if(!input.keySet().equals(Set.of("url"))||!(input.get("url") instanceof String text)||text.length()>16384)throw new IOException("Invalid source URL");
        String url=text.strip();URI parsed;try{parsed=URI.create(url);}catch(RuntimeException invalid){throw new IOException("Invalid source URL");}
        if(!"magnet".equalsIgnoreCase(parsed.getScheme()))SourceTransfer.validate(url);
        else if(!url.startsWith("magnet:?")||parsed.getRawFragment()!=null||url.chars().anyMatch(value->value<32||value==127))throw new IOException("Invalid source URL");
        java.util.function.Consumer<String> receiver=manager.browserDownload;
        if(receiver==null||manager.closing){Main.json(exchange,503,Map.of("error","Open the MultiRoute desktop application first"));return;}
        receiver.accept(url);Main.json(exchange,202,Map.of("accepted",true,"status","sent_to_desktop"));
    }
}
