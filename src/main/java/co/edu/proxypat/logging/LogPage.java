package co.edu.proxypat.logging;

import java.util.List;

public record LogPage(
        List<LogEntry> items,
        int page,
        int size,
        long total,
        int totalPages) {
}
