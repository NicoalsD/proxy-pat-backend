package co.edu.proxypat.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.proxypat.logging.LogEntry;
import co.edu.proxypat.logging.LogStatus;
import co.edu.proxypat.logging.LogStore;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoggingProxyTest {

    static class DummyService {
        public Map<String, Object> echo(String value, int times) {
            return Map.of("value", value, "times", times);
        }

        public String boom() {
            throw new IllegalStateException("puerta cerrada");
        }
    }

    @Test
    void logsTheCallAndReturnsTheResult() {
        LogStore store = new LogStore();
        MicroserviceProxy<Object> proxy = new LoggingProxy<>("dummy", new DummyService(), store);

        Object result = proxy.execute("echo", "hola", 3);

        assertThat(result).isEqualTo(Map.of("value", "hola", "times", 3));
        LogEntry log = store.recent().get(0);
        assertThat(log.status()).isEqualTo(LogStatus.SUCCESS);
        assertThat(log.serviceId()).isEqualTo("dummy");
        assertThat(log.operation()).isEqualTo("echo");
        assertThat(log.params()).containsExactly("hola", 3);
        assertThat(log.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(UUID.fromString(log.requestId())).isNotNull();
    }

    @Test
    void logsTheFailureWithAShortStackTrace() {
        LogStore store = new LogStore();
        MicroserviceProxy<Object> proxy = new LoggingProxy<>("dummy", new DummyService(), store);

        assertThatThrownBy(() -> proxy.execute("boom"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("puerta cerrada");

        LogEntry log = store.recent().get(0);
        assertThat(log.status()).isEqualTo(LogStatus.ERROR);
        assertThat(log.result()).isNull();
        assertThat(log.error()).startsWith("IllegalStateException: puerta cerrada").contains("DummyService.boom");
        assertThat(List.of(log.error().split("←"))).hasSizeLessThanOrEqualTo(6);
    }

    @Test
    void unknownOperationIsLoggedAsError() {
        LogStore store = new LogStore();
        MicroserviceProxy<Object> proxy = new LoggingProxy<>("dummy", new DummyService(), store);

        assertThatThrownBy(() -> proxy.execute("noExiste")).hasMessageContaining("Operación no disponible");

        assertThat(store.recent().get(0).status()).isEqualTo(LogStatus.ERROR);
    }
}
