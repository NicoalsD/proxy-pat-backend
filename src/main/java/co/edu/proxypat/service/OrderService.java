package co.edu.proxypat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Pedidos reales: creación, consulta y cancelación sobre memoria. */
public class OrderService {

    private static final Map<String, Integer> PRICES = Map.of(
            "SKU-A1", 89_000,
            "SKU-B2", 139_000,
            "SKU-C3", 259_000,
            "SKU-D4", 45_000);

    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1040);

    public Map<String, Object> createOrder(String sku, Integer qty) {
        Latency.of(3, 9);
        int unitPrice = PRICES.getOrDefault(sku, 0);
        if (unitPrice == 0) {
            throw new IllegalArgumentException("SKU sin precio en catálogo: " + sku);
        }
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", "ORD-" + sequence.incrementAndGet());
        order.put("sku", sku);
        order.put("qty", qty);
        order.put("unitPriceCop", unitPrice);
        order.put("totalCop", unitPrice * qty);
        order.put("status", "CREATED");
        order.put("createdAt", Instant.now().toString());
        orders.put((String) order.get("orderId"), order);
        return new LinkedHashMap<>(order);
    }

    public Map<String, Object> getOrder(String orderId) {
        Latency.of(2, 6);
        return new LinkedHashMap<>(require(orderId));
    }

    public Map<String, Object> cancelOrder(String orderId) {
        Latency.of(3, 7);
        Map<String, Object> order = require(orderId);
        synchronized (order) {
            if (!"CREATED".equals(order.get("status"))) {
                throw new IllegalStateException("El pedido " + orderId + " ya está " + order.get("status"));
            }
            order.put("status", "CANCELLED");
            order.put("cancelledAt", Instant.now().toString());
            return new LinkedHashMap<>(order);
        }
    }

    public List<Map<String, Object>> listOrders() {
        Latency.of(3, 9);
        List<Map<String, Object>> out = new LinkedList<>();
        for (Map<String, Object> order : orders.values()) {
            out.add(new LinkedHashMap<>(order));
        }
        return new ArrayList<>(out);
    }

    private Map<String, Object> require(String orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Pedido no encontrado: " + orderId);
        }
        return order;
    }
}
