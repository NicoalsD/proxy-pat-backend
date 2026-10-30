package co.edu.proxypat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Inventario real: catálogo con stock y reservas en memoria. */
public class InventoryService {

    private final Map<String, Map<String, Object>> items = new ConcurrentHashMap<>();

    public InventoryService() {
        add("SKU-A1", "Camisa oxford", 42, 89_000);
        add("SKU-B2", "Jean recto", 28, 139_000);
        add("SKU-C3", "Chaqueta de lona", 12, 259_000);
        add("SKU-D4", "Gorra de sarga", 64, 45_000);
    }

    private void add(String sku, String name, int stock, int unitPriceCop) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sku", sku);
        item.put("name", name);
        item.put("stock", stock);
        item.put("unitPriceCop", unitPriceCop);
        item.put("warehouse", "Bogotá · Calle 72");
        items.put(sku, item);
    }

    public List<Map<String, Object>> listItems() {
        Latency.of(1, 5);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : items.values()) {
            out.add(new LinkedHashMap<>(item));
        }
        return out;
    }

    public Map<String, Object> checkStock(String sku) {
        Latency.of(1, 5);
        return new LinkedHashMap<>(require(sku));
    }

    public Map<String, Object> reserveStock(String sku, Integer qty) {
        Latency.of(2, 6);
        Map<String, Object> item = require(sku);
        synchronized (item) {
            int stock = (int) item.get("stock");
            if (stock < qty) {
                throw new IllegalStateException("Stock insuficiente de " + sku + ": quedan " + stock + " unidades");
            }
            item.put("stock", stock - qty);
            return result("reserved", sku, qty, stock - qty);
        }
    }

    public Map<String, Object> releaseStock(String sku, Integer qty) {
        Latency.of(2, 6);
        Map<String, Object> item = require(sku);
        synchronized (item) {
            int stock = (int) item.get("stock") + qty;
            item.put("stock", stock);
            return result("released", sku, qty, stock);
        }
    }

    private Map<String, Object> result(String action, String sku, int qty, int remaining) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", action);
        out.put("sku", sku);
        out.put("qty", qty);
        out.put("remaining", remaining);
        return out;
    }

    private Map<String, Object> require(String sku) {
        Map<String, Object> item = items.get(sku);
        if (item == null) {
            throw new IllegalArgumentException("SKU desconocido: " + sku);
        }
        return item;
    }
}
