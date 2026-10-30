# Proxy Pat · Backend

Proxy genérico con logging para tres microservicios de un e-commerce (inventario,
pedidos y pagos). Cada llamada pasa por `LoggingProxy`, que mide y registra sin
tocar el código de los servicios.

Java 17 · Spring Boot 4 · Maven Wrapper · sin base de datos (buffer en memoria).

## Correr

```bash
./mvnw spring-boot:run
# http://localhost:8080
```

Docker:

```bash
docker build -t proxy-pat-backend .
docker run -p 8080:8080 proxy-pat-backend
```

## El patrón

```
POST /api/services/{servicio}/{operacion}
        │
        ▼
ServiceRegistry ──► LoggingProxy<T> (MicroserviceProxy<T>)
                        │  ├─ requestId = UUID
                        │  ├─ mide con System.nanoTime()
                        │  ├─ invoca la operación por reflexión
                        │  └─ registra en LogStore (SUCCESS/ERROR, ±stack resumido)
                        ▼
              InventoryService · OrderService · PaymentService
```

`MicroserviceProxy<T>` es la interfaz del taller:

```java
public interface MicroserviceProxy<T> {
    T execute(String operation, Object... params);
}
```

`T` es el tipo del resultado; el proxy se instancia como `LoggingProxy<Object>`
porque cada operación devuelve una forma distinta (`Map`, `List`, primitivo).
La operación se resuelve por nombre sobre el objeto envuelto y los argumentos
se ajustan al tipo declarado (`Integer` → `int`, etc.). Así el mismo proxy
sirve para cualquier servicio, sin `switch` por servicio.

## Endpoints

| Método | Ruta | Qué hace |
| --- | --- | --- |
| POST | `/api/services/inventory/{operation}` | `listItems`, `checkStock`, `reserveStock`, `releaseStock` |
| POST | `/api/services/orders/{operation}` | `createOrder`, `getOrder`, `cancelOrder`, `listOrders` |
| POST | `/api/services/payments/{operation}` | `charge`, `refund`, `listPayments` |
| GET | `/api/metrics/summary` | llamadas, tasa de error y duración media por servicio |
| GET | `/api/metrics/logs` | `?service=&status=&from=&to=&page=&size=` |
| POST | `/api/metrics/simulate-load` | 50 llamadas aleatorias |

El body es la lista de parámetros: `["SKU-A1", 2]`. Sin body, la operación va
sin argumentos.

Ejemplo:

```bash
curl -X POST localhost:8080/api/services/orders/createOrder \
  -H 'Content-Type: application/json' -d '["SKU-A1", 2]'

curl -X POST localhost:8080/api/metrics/simulate-load
curl "localhost:8080/api/metrics/logs?service=payments&status=ERROR&size=5"
```

## Log

```json
{
  "requestId": "f5847a53-…",
  "serviceId": "payments",
  "operation": "charge",
  "durationMs": 18.19,
  "status": "ERROR",
  "timestamp": "2026-10-30T20:20:16.722Z",
  "params": ["ORD-1157", 187.5],
  "result": null,
  "error": "IllegalStateException: Pasarela no responde … ← PaymentService.charge:24 …"
}
```

- Buffer circular de 5.000 entradas, el más reciente primero.
- Pagos falla a propósito en el 10 % de las llamadas (`ThreadLocalRandom`).
- Los servicios simulan entre 1 y 18 ms de trabajo para que la latencia medida
  sea significativa.

## Configuración

| Variable | Default | Uso |
| --- | --- | --- |
| `PORT` | `8080` | puerto HTTP (Railway lo inyecta) |
| `CORS_ORIGINS` | `*` | orígenes permitidos, separados por coma |

## Despliegue

Railway detecta el `Dockerfile` (multi-stage, `eclipse-temurin:17`). El
servicio escucha en `$PORT`.
