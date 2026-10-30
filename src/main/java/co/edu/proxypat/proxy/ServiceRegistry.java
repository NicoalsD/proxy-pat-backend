package co.edu.proxypat.proxy;

import co.edu.proxypat.logging.LogStore;
import co.edu.proxypat.service.InventoryService;
import co.edu.proxypat.service.OrderService;
import co.edu.proxypat.service.PaymentService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Los tres servicios reales, cada uno detrás de su propio LoggingProxy. */
@Component
public class ServiceRegistry {

    private final Map<String, MicroserviceProxy<?>> proxies = new LinkedHashMap<>();

    public ServiceRegistry(LogStore logs) {
        register("inventory", new InventoryService(), logs);
        register("orders", new OrderService(), logs);
        register("payments", new PaymentService(), logs);
    }

    private void register(String serviceId, Object service, LogStore logs) {
        proxies.put(serviceId, new LoggingProxy<>(serviceId, service, logs));
    }

    public Object execute(String serviceId, String operation, Object... params) {
        MicroserviceProxy<?> proxy = proxies.get(serviceId);
        if (proxy == null) {
            throw new IllegalArgumentException("Servicio desconocido: " + serviceId);
        }
        return proxy.execute(operation, params);
    }

    public List<String> serviceIds() {
        return List.copyOf(proxies.keySet());
    }
}
