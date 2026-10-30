package co.edu.proxypat.logging;

public record ServiceSummary(
        String serviceId,
        long totalCalls,
        double errorRate,
        double avgDurationMs) {
}
