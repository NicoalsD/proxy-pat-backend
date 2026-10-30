package co.edu.proxypat.proxy;

import co.edu.proxypat.logging.LogEntry;
import co.edu.proxypat.logging.LogStatus;
import co.edu.proxypat.logging.LogStore;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Envuelve cualquier servicio: mide, registra y deja pasar el resultado.
 * La operación se resuelve por nombre sobre el objeto envuelto, así el mismo
 * proxy sirve para inventario, pedidos y pagos sin tocar su código.
 */
public final class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final int STACK_FRAMES = 5;

    private final String serviceId;
    private final Object target;
    private final LogStore logs;

    public LoggingProxy(String serviceId, Object target, LogStore logs) {
        this.serviceId = serviceId;
        this.target = target;
        this.logs = logs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Object[] args = params == null ? new Object[0] : params;
        long start = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = invoke(operation, args);
            return (T) result;
        } catch (InvocationTargetException e) {
            failure = e.getCause();
            throw new ServiceException(String.valueOf(failure.getMessage()), requestId, failure);
        } catch (ReflectiveOperationException e) {
            failure = e;
            throw new ServiceException("Operación no disponible: " + operation, requestId, e);
        } finally {
            double durationMs = Math.round((System.nanoTime() - start) / 10_000.0) / 100.0;
            logs.record(new LogEntry(
                    requestId,
                    serviceId,
                    operation,
                    durationMs,
                    failure == null ? LogStatus.SUCCESS : LogStatus.ERROR,
                    Instant.now(),
                    List.copyOf(Arrays.asList(args)),
                    result,
                    failure == null ? null : summarize(failure)));
        }
    }

    private Object invoke(String operation, Object[] args) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(operation) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                return method.invoke(target, coerce(method.getParameterTypes(), args));
            } catch (IllegalArgumentException wrongOverload) {
                // Otro método con el mismo nombre y distinta firma: se sigue buscando.
            }
        }
        throw new NoSuchMethodException(operation);
    }

    /**
     * El JSON llega como Integer/Double aunque la firma pida int o long: se ajusta
     * al tipo del método para que el proxy sirva con cualquier servicio.
     */
    private static Object[] coerce(Class<?>[] types, Object[] args) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = coerce(types[i], args[i]);
        }
        return out;
    }

    private static Object coerce(Class<?> type, Object value) {
        if (value == null || type.isInstance(value)) {
            return value;
        }
        if (value instanceof Number number) {
            if (type == int.class || type == Integer.class) {
                return number.intValue();
            }
            if (type == long.class || type == Long.class) {
                return number.longValue();
            }
            if (type == double.class || type == Double.class) {
                return number.doubleValue();
            }
        }
        return type == String.class ? String.valueOf(value) : value;
    }

    /** Una línea con el tipo de error y las primeras frames, suficiente para el detalle del log. */
    private static String summarize(Throwable failure) {
        StringBuilder out = new StringBuilder(failure.getClass().getSimpleName());
        if (failure.getMessage() != null) {
            out.append(": ").append(failure.getMessage());
        }
        StackTraceElement[] trace = failure.getStackTrace();
        for (int i = 0; i < Math.min(STACK_FRAMES, trace.length); i++) {
            out.append(" ← ").append(trace[i].getClassName())
                    .append('.').append(trace[i].getMethodName())
                    .append(':').append(trace[i].getLineNumber());
        }
        return out.toString();
    }
}
