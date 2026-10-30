package co.edu.proxypat.logging;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Buffer circular en memoria: la demo no necesita persistencia y así el
 * dashboard responde sin tocar base de datos. 5.000 entradas son suficientes
 * para varias sesiones de "Simular carga".
 */
@Component
public class LogStore {

    private static final int CAPACITY = 5000;

    private final Deque<LogEntry> entries = new ArrayDeque<>();

    public synchronized void record(LogEntry entry) {
        entries.addFirst(entry);
        if (entries.size() > CAPACITY) {
            entries.removeLast();
        }
    }

    public synchronized List<LogEntry> recent() {
        return new ArrayList<>(entries);
    }

    public List<LogEntry> filter(String serviceId, String status, Instant from, Instant to) {
        return recent().stream()
                .filter(e -> serviceId == null || e.serviceId().equals(serviceId))
                .filter(e -> status == null || e.status().name().equals(status))
                .filter(e -> from == null || !e.timestamp().isBefore(from))
                .filter(e -> to == null || !e.timestamp().isAfter(to))
                .toList();
    }

    public LogPage page(String serviceId, String status, Instant from, Instant to, int page, int size) {
        List<LogEntry> filtered = filter(serviceId, status, from, to);
        int totalPages = (int) Math.ceil(filtered.size() / (double) size);
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        return new LogPage(filtered.subList(start, end), page, size, filtered.size(), totalPages);
    }

    public List<ServiceSummary> summary(Collection<String> serviceIds) {
        List<LogEntry> snapshot = recent();
        List<ServiceSummary> out = new ArrayList<>();
        for (String serviceId : serviceIds) {
            List<LogEntry> calls = snapshot.stream()
                    .filter(e -> e.serviceId().equals(serviceId))
                    .toList();
            long errors = calls.stream().filter(e -> e.status() == LogStatus.ERROR).count();
            double avg = calls.stream().mapToDouble(LogEntry::durationMs).average().orElse(0);
            double errorRate = calls.isEmpty() ? 0 : errors / (double) calls.size();
            out.add(new ServiceSummary(serviceId, calls.size(), round(errorRate), round(avg)));
        }
        return out;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
