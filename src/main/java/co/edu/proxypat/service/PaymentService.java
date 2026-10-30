package co.edu.proxypat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Pagos reales, con la falla deliberada del taller: 10% de las llamadas revienta. */
public class PaymentService {

    private static final int FAILURE_PERCENT = 10;

    private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(5100);

    public Map<String, Object> charge(String orderId, Double amountCop) {
        Latency.of(6, 18);
        if (ThreadLocalRandom.current().nextInt(100) < FAILURE_PERCENT) {
            throw new IllegalStateException("Pasarela no responde (timeout 504, proveedor=payco)");
        }
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("paymentId", "PAY-" + sequence.incrementAndGet());
        payment.put("orderId", orderId);
        payment.put("amountCop", Math.round(amountCop));
        payment.put("status", "APPROVED");
        payment.put("chargedAt", Instant.now().toString());
        payments.put((String) payment.get("paymentId"), payment);
        return new LinkedHashMap<>(payment);
    }

    public Map<String, Object> refund(String paymentId) {
        Latency.of(5, 12);
        Map<String, Object> payment = require(paymentId);
        synchronized (payment) {
            if (!"APPROVED".equals(payment.get("status"))) {
                throw new IllegalStateException("El pago " + paymentId + " no está aprobado");
            }
            payment.put("status", "REFUNDED");
            payment.put("refundedAt", Instant.now().toString());
            return new LinkedHashMap<>(payment);
        }
    }

    public List<Map<String, Object>> listPayments() {
        Latency.of(4, 10);
        List<Map<String, Object>> out = new LinkedList<>();
        for (Map<String, Object> payment : payments.values()) {
            out.add(new LinkedHashMap<>(payment));
        }
        return new ArrayList<>(out);
    }

    private Map<String, Object> require(String paymentId) {
        Map<String, Object> payment = payments.get(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Pago no encontrado: " + paymentId);
        }
        return payment;
    }
}
