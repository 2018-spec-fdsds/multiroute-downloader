package multiroute;

import java.util.LinkedHashMap;
import java.util.Map;

/** Inspect the application Ethernet path; never substitute the OS default route. */
public final class GatewayStatus {
    private GatewayStatus() { }
    public static Map<String,Object> inspect(String gateway, String url) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("gateway", gateway == null ? "" : gateway);
        result.put("available", false);
        result.put("type", "gateway");
        try {
            if (!GatewayNetwork.validIpv4(gateway)) {
                result.put("status", "Некорректный IP");
                result.put("message", "Укажите IPv4 адрес шлюза");
                return result;
            }
            Transport.uri(url == null || url.isBlank() ? SpeedTest.DEFAULT_URL : url);
            return GatewayNetwork.route(gateway);
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            result.put("status", "Нет подключения");
            result.put("message", Main.str(error.getMessage()));
            return result;
        }
    }
}
