package co.edu.proxypat.logging;

import java.time.Instant;
import java.util.List;

public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        double durationMs,
        LogStatus status,
        Instant timestamp,
        List<Object> params,
        Object result,
        String error) {
}
