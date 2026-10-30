package co.edu.proxypat.web;

import co.edu.proxypat.logging.LogPage;
import co.edu.proxypat.logging.LogStore;
import co.edu.proxypat.proxy.ServiceRegistry;
import co.edu.proxypat.simulation.LoadSimulator;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final LogStore logs;
    private final ServiceRegistry registry;
    private final LoadSimulator simulator;

    public MetricsController(LogStore logs, ServiceRegistry registry, LoadSimulator simulator) {
        this.logs = logs;
        this.registry = registry;
        this.simulator = simulator;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
                "generatedAt", Instant.now(),
                "services", logs.summary(registry.serviceIds()));
    }

    @GetMapping("/logs")
    public LogPage logs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return logs.page(
                blank(service),
                blank(status),
                instant(from),
                instant(to),
                Math.max(0, page),
                Math.min(100, Math.max(1, size)));
    }

    @PostMapping("/simulate-load")
    public Map<String, Object> simulateLoad() {
        return Map.of("generated", simulator.run(50));
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Instant instant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value.trim());
    }
}
