package co.edu.proxypat.web;

import co.edu.proxypat.proxy.ServiceRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Un solo endpoint para los tres servicios: el body es la lista de parámetros. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRegistry registry;

    public ServiceController(ServiceRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/{serviceId}/{operation}")
    public Object execute(
            @PathVariable String serviceId,
            @PathVariable String operation,
            @RequestBody(required = false) Object body) {
        return registry.execute(serviceId, operation, params(body));
    }

    /** {@code [a, b]} → dos parámetros; {@code {..}} → uno; sin body → ninguno. */
    private Object[] params(Object body) {
        if (body == null) {
            return new Object[0];
        }
        if (body instanceof List<?> list) {
            return list.toArray();
        }
        return new Object[] {body};
    }
}
