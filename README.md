# RapiPedido: monolito modular con eventos

Implementación del ADR-001 de la presentación *RapiPedido diseño de arquitectura*: un monolito modular
en Spring Boot que se comunica por eventos asíncronos (RabbitMQ), con PostgreSQL como base principal y
Redis como caché y almacén de ubicaciones.

**Documentación**
- [Diseño de arquitectura](docs/RapiPedido%20-%20Dise%C3%B1o%20de%20arquitectura.pdf): actores, atributos de calidad, C4 y ADR-001.
- [Cómo funciona el código](docs/RapiPedido%20-%20C%C3%B3mo%20funciona%20el%20c%C3%B3digo.pdf): recorrido de un pedido por el código, con capturas.
- [Presentación: cómo funciona el código](docs/RapiPedido%20-%20Presentaci%C3%B3n%20c%C3%B3mo%20funciona%20el%20c%C3%B3digo.pdf): versión en diapositivas (16) para la exposición.

## Cómo correrlo

Solo hace falta Docker; Java y Maven se usan dentro de los contenedores.

```bash
docker compose up -d --build        # front React + 2 réplicas de la API + PostgreSQL + Redis + RabbitMQ
open http://localhost:8080          # la aplicación web
./scripts/demo-flujo.sh             # pedido completo: pago → restaurante → repartidor → entrega
./scripts/demo-falla-pasarela.sh    # escenario de disponibilidad
docker compose logs -f app | grep -E 'PUSH|SMS'   # notificaciones simuladas
```

| Qué | Dónde |
|---|---|
| Aplicación web (cliente, restaurante, repartidor, admin) | http://localhost:8080 |
| API (a través de nginx) | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola de RabbitMQ (guest/guest) | http://localhost:15672 |
| PostgreSQL / Redis / RabbitMQ desde el host | puertos 5433 / 6380 / 5673 |

Pruebas (incluye la verificación de módulos):

```bash
docker run --rm -v "$PWD":/app -v rapipedido-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

## Front (aplicación web)

`frontend/` es una sola app React (Vite + TypeScript) con una vista por actor del PDF. En el diagrama el
cliente y el repartidor son apps Flutter; aquí se simulan como vistas web que usan la misma API, y la del
repartidor tiene formato de teléfono.

| Vista | Qué hace | Cómo habla con el backend |
|---|---|---|
| **Cliente** `/cliente` | Restaurantes, menú, carrito, pago y seguimiento con línea de tiempo y mapa en vivo | REST + WebSocket (`/topic/pedidos/{id}/ubicacion`) |
| **Restaurante** `/restaurante` | Tablero de comandas (nuevos → aceptados → en preparación → despacho), menú y apertura | REST, actualización cada 3 s |
| **Repartidor** `/repartidor` | Conexión, entrega asignada, recoger y entregar; envía su GPS cada 5 s (ruta simulada o GPS real) | WebSocket (`/app/ubicacion`), con HTTP de respaldo |
| **Administración** `/admin` | Pedidos por estado, repartidores, soporte de pagos y botón para **tumbar la pasarela** | REST |

Para la demo, abran cada vista en una pestaña distinta: el pedido se mueve entre ellas en tiempo real.
En Docker, nginx (`frontend/nginx.conf`) sirve el front y balancea `/api` y `/ws` entre las réplicas.
Para trabajar en el front con recarga en caliente:

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173, usa la API de Docker
```

## Del PDF al código

### Contenedores (C4 nivel 2)

| Contenedor del diagrama | Implementación |
|---|---|
| API Monolito modular (Spring Boot) | `src/main/java/com/rapipedido`, 2 réplicas en `docker-compose.yml` |
| Servicio de tracking (WebSocket, GPS cada 5 s) | módulo `tracking`: STOMP en `/ws`, fanout en RabbitMQ entre réplicas |
| Cola de eventos (RabbitMQ) | módulo `eventos`: exchange `rapipedido.eventos`, una cola por módulo y DLQ |
| PostgreSQL | migraciones Flyway en `src/main/resources/db/migration` |
| Redis | caché del catálogo, índice GEO de repartidores, estado de la pasarela simulada |
| App Cliente, App Repartidor, Panel Restaurante, Panel Admin | `frontend/`: una app React con una vista por actor |
| Pasarela de pagos / Notificaciones | simuladas: `PasarelaPagosSimulada`, `PushSimulado`, `SmsSimulado` |

### Módulos

Cada módulo expone su API en la raíz del paquete y oculta todo lo demás en `interno`.
`ModularidadTest` (Spring Modulith) hace fallar el build si alguien rompe esa regla o crea un ciclo, y
genera diagramas C4 de componentes en `target/spring-modulith-docs`.

```
catalogo        restaurantes, menú (con caché)            API: CatalogoApi
pedidos         carrito, pedido, máquina de estados       → catalogo, pagos (solo la API), eventos
pagos           cobro, reintentos, métodos de pago        API: CatalogoMetodosPago
asignacion      repartidor más cercano, recogida, entrega → tracking (API), eventos
tracking        GPS por WebSocket, Redis GEO              API: UbicacionRepartidores
notificaciones  push y SMS al cambiar de estado           → eventos
eventos         contratos de eventos y outbox             (kernel compartido)
```

### Flujo de eventos

```
POST /api/pedidos ──► PedidoCreado ──► pagos ──► PagoAprobado / PagoFallido ──► pedidos (PAGADO)
PATCH estado LISTO ─► PedidoListo ──► asignacion ──► RepartidorAsignado ──► pedidos (ASIGNADO)
POST /entregas/{id}/recoger ──► PedidoRecogido ──► pedidos (EN_CAMINO)
POST /entregas/{id}/confirmar ─► EntregaConfirmada ─► pedidos (ENTREGADO)
cada cambio de estado ──► EstadoPedidoCambiado ──► notificaciones
```

Estados: `PAGO_PENDIENTE → PAGADO → ACEPTADO → EN_PREPARACION → LISTO → ASIGNADO → EN_CAMINO → ENTREGADO`,
además de `PAGO_FALLIDO` y `CANCELADO`.

## Cómo se cumple cada atributo de calidad

### 1. Rendimiento: menos de 2 s para el 95 % de los pedidos

- `POST /api/pedidos` **no espera a la pasarela**: guarda el pedido y su evento en una transacción y
  responde `202 Accepted`. El cobro ocurre después.
- El catálogo se sirve desde **Redis** (`@Cacheable` en `CatalogoServicio`).
- La API es **sin estado** y corre en varias réplicas detrás de nginx.
- Medido con k6 (`carga/hora-pico.js`) a 10 pedidos/s, 12 veces el pico del enunciado:
  **p95 = 13 ms**, 0 % de errores.

```bash
docker run --rm -i --network rapipedido_default -e BASE_URL=http://web grafana/k6 run - < carga/hora-pico.js
```

### 2. Disponibilidad: 99,9 % sin pedidos perdidos

- **Outbox transaccional** (`PublicadorEventos`, `OutboxRelay`): el evento se guarda en PostgreSQL
  junto con el pedido. Si RabbitMQ se cae, los eventos esperan y se envían cuando vuelve.
- **Reintentos con espera creciente**: si la pasarela falla, RabbitMQ reintenta a los 1, 2, 4 y 8 s.
  Después el mensaje pasa a la **DLQ** y el `ReconciliadorPagos` sigue intentando cada 15 s, hasta 10
  intentos en total.
- **Idempotencia** en todos los puntos: cabecera `Idempotency-Key` al crear el pedido, clave de
  idempotencia ante la pasarela y consumidores que ignoran mensajes repetidos. Un reintento nunca
  cobra dos veces.
- **Redundancia**: 2 réplicas; `FOR UPDATE SKIP LOCKED` y el bloqueo optimista (`@Version`) evitan
  que dos réplicas procesen lo mismo.
- Para la demo, la caída se provoca en vivo:
  `PUT /api/admin/pasarela {"caida":true,"tasaFallo":0,"tasaRechazo":0,"latenciaMs":200}`.

### 3. Modificabilidad: nuevo método de pago en menos de 3 días-persona

Agregar un método de pago es **crear una sola clase** en `pagos/interno/metodos`:

```java
@Component
public class NequiPago implements MetodoPago {
    public String codigo() { return "NEQUI"; }
    public String descripcion() { return "Billetera Nequi"; }
    public ResultadoCobro cobrar(SolicitudCobro s) { /* llamar al proveedor */ }
}
```

`RegistroMetodosPago` lo descubre solo, `GET /api/pagos/metodos` lo lista y `pedidos` lo acepta, sin
tocar ningún otro módulo. `EfectivoContraEntregaPago` es un ejemplo real. Lo mismo vale para las
comisiones: la regla vive en `PoliticaComision`.

## Endpoints principales

| Actor | Método y ruta |
|---|---|
| Cliente | `GET /api/restaurantes`, `GET /api/restaurantes/{id}/menu` |
| Cliente | `POST /api/pedidos` (cabecera opcional `Idempotency-Key`), `GET /api/pedidos/{id}`, `GET /api/pedidos?clienteId=` |
| Cliente | WebSocket `/ws`, suscripción a `/topic/pedidos/{id}/ubicacion`; `GET /api/tracking/pedidos/{id}/ubicacion` |
| Restaurante | `GET /api/pedidos?restauranteId=`, `PATCH /api/pedidos/{id}/estado` (`ACEPTADO`, `EN_PREPARACION`, `LISTO`, `CANCELADO`) |
| Restaurante | `POST /api/restaurantes/{id}/productos`, `PATCH /api/productos/{id}`, `PATCH /api/restaurantes/{id}/apertura` |
| Repartidor | WebSocket: envío a `/app/ubicacion` cada 5 s (o `POST /api/tracking/ubicacion`) |
| Repartidor | `POST /api/repartidores/{id}/disponibilidad`, `GET /api/repartidores/{id}/entrega-actual` |
| Repartidor | `POST /api/entregas/{pedidoId}/recoger`, `POST /api/entregas/{pedidoId}/confirmar` |
| Administración | `GET /api/pedidos/resumen`, `GET /api/pagos/pedido/{id}`, `GET/PUT /api/admin/pasarela` |

## Fuera del alcance (para mencionar en la defensa)

- Autenticación y autorización: hoy los IDs de cliente, restaurante y repartidor llegan en la petición.
- Reembolsos al cancelar un pedido ya pagado.
- Réplicas de lectura de PostgreSQL: la arquitectura las contempla, pero `docker-compose` usa una sola base.
- Apps móviles nativas en Flutter: el cliente y el repartidor se implementaron como vistas web responsivas.
