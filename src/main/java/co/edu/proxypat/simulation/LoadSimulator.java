package co.edu.proxypat.simulation;

import co.edu.proxypat.proxy.ServiceException;
import co.edu.proxypat.proxy.ServiceRegistry;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Genera llamadas creíbles sobre los tres servicios para poblar el dashboard. */
@Component
public class LoadSimulator {

    private static final String[] SKUS = {"SKU-A1", "SKU-B2", "SKU-C3", "SKU-D4"};

    private final ServiceRegistry registry;

    public LoadSimulator(ServiceRegistry registry) {
        this.registry = registry;
    }

    public int run(int calls) {
        for (int i = 0; i < calls; i++) {
            try {
                fire();
            } catch (ServiceException expected) {
                // El 10% de pagos falla a propósito: también debe quedar en el log.
            }
        }
        return calls;
    }

    private void fire() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        switch (random.nextInt(6)) {
            case 0 -> registry.execute("inventory", "checkStock", sku());
            case 1 -> registry.execute("inventory", "reserveStock", sku(), 1 + random.nextInt(3));
            case 2 -> registry.execute("orders", "createOrder", sku(), 1 + random.nextInt(2));
            case 3 -> registry.execute("orders", "listOrders");
            case 4 -> registry.execute("payments", "charge",
                    "ORD-" + (1000 + random.nextInt(900)), 12.5 + random.nextInt(180));
            default -> registry.execute("payments", "listPayments");
        }
    }

    private static String sku() {
        return SKUS[ThreadLocalRandom.current().nextInt(SKUS.length)];
    }
}
