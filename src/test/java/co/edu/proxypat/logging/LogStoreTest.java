package co.edu.proxypat.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogStoreTest {

    private static LogEntry entry(String service, LogStatus status, double ms, Instant at) {
        return new LogEntry("req-" + at, service, "op", ms, status, at, List.of(), null, null);
    }

    @Test
    void summarizesCallsErrorsAndAveragePerService() {
        LogStore store = new LogStore();
        Instant now = Instant.parse("2026-10-30T12:00:00Z");
        store.record(entry("payments", LogStatus.SUCCESS, 10, now));
        store.record(entry("payments", LogStatus.ERROR, 20, now.plusSeconds(1)));
        store.record(entry("orders", LogStatus.SUCCESS, 4, now.plusSeconds(2)));

        List<ServiceSummary> summary = store.summary(List.of("payments", "orders"));

        assertThat(summary).hasSize(2);
        assertThat(summary.get(0).totalCalls()).isEqualTo(2);
        assertThat(summary.get(0).errorRate()).isEqualTo(0.5);
        assertThat(summary.get(0).avgDurationMs()).isEqualTo(15.0);
        assertThat(summary.get(1).errorRate()).isZero();
    }

    @Test
    void filtersByServiceStatusAndWindow() {
        LogStore store = new LogStore();
        Instant base = Instant.parse("2026-10-30T12:00:00Z");
        store.record(entry("payments", LogStatus.ERROR, 9, base.plusSeconds(5)));
        store.record(entry("payments", LogStatus.SUCCESS, 9, base.plusSeconds(10)));
        store.record(entry("orders", LogStatus.ERROR, 9, base.plusSeconds(20)));

        assertThat(store.filter("payments", "ERROR", null, null)).hasSize(1);
        assertThat(store.filter(null, null, base.plusSeconds(6), null)).hasSize(2);
        assertThat(store.filter(null, null, null, base.plusSeconds(6))).hasSize(1);
    }

    @Test
    void pagesTheFilteredResultNewestFirst() {
        LogStore store = new LogStore();
        Instant base = Instant.parse("2026-10-30T12:00:00Z");
        for (int i = 0; i < 5; i++) {
            store.record(entry("inventory", LogStatus.SUCCESS, 2, base.plusSeconds(i)));
        }

        LogPage first = store.page(null, null, null, null, 0, 2);
        LogPage last = store.page(null, null, null, null, 2, 2);

        assertThat(first.total()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.items()).hasSize(2);
        assertThat(first.items().get(0).timestamp()).isEqualTo(base.plusSeconds(4));
        assertThat(last.items()).hasSize(1);
    }

    @Test
    void keepsTheBufferBounded() {
        LogStore store = new LogStore();
        Instant base = Instant.parse("2026-10-30T12:00:00Z");
        for (int i = 0; i < 5_100; i++) {
            store.record(entry("inventory", LogStatus.SUCCESS, 1, base.plusMillis(i)));
        }

        assertThat(store.recent()).hasSize(5_000);
        assertThat(store.recent().get(0).timestamp()).isEqualTo(base.plusMillis(5_099));
    }
}
