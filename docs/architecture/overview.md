# Arquitectura del workshop

> Documento de diseño de la Fase 2. Decisiones: ADR-001, ADR-002, ADR-003
> y ADR-004.

## Servicios

| Servicio | Responsabilidad | Base de datos |
|----------|-----------------|---------------|
| Order Service | Crear pedidos, gestionar su ciclo de vida y exponer su estado. | `workshop_orders` |
| Inventory Service | Gestionar stock, reservar líneas y publicar el resultado. | `workshop_inventory` |

Catalog Service, Notification Service y API Gateway no forman parte del
diseño actual (ADR-002).

## Diagrama

```mermaid
flowchart LR
    Cliente -->|POST /orders/drafts| Order[Order Service]
    Cliente -->|PUT /orders/{orderId}: reemplaza líneas| Order
    Cliente -->|POST /orders/{orderId}/submit| Order
    Order --> OrdersDB[(workshop_orders)]
    Order -.->|OrderCreated, Fase 4| T1[(orders.created)]
    T1 --> Inventory[Inventory Service]
    Inventory --> InvDB[(workshop_inventory)]
    Inventory -->|InventoryReserved| T2[(inventory.reserved)]
    Inventory -->|InventoryRejected| T3[(inventory.rejected)]
    T2 --> Order
    T3 --> Order
    Cliente -.->|GET /orders/{id}, propuesto| Order
```

## Flujo de negocio

1. El cliente envía `POST /orders/drafts`. Order crea la orden con id
   generado por la base de datos y estado `DRAFT` (ADR-004).
2. El cliente envía `PUT /orders/{orderId}` con la lista completa de
   líneas. El `customerEmail` ya quedó registrado en el borrador. Order
   sustituye las líneas actuales por las recibidas, en una transacción, y
   mantiene el estado `DRAFT`. Así se añaden, modifican y borran líneas
   con la misma operación (ADR-005).
3. El cliente envía `POST /orders/{orderId}/submit` para pasar a
   `PENDING`. Cuando se introduzca Kafka en la Fase 4, ese será el momento
   de publicar `OrderCreated`. Un reenvío del `PUT` o del `submit` sobre
   una orden que ya no está en `DRAFT` devuelve la existente sin cambios
   ni eventos.
4. Inventory consume `OrderCreated` y, en una única transacción local,
   comprueba el stock de **todas** las líneas:
   - Si hay stock suficiente, crea una reserva por línea y publica
     `InventoryReserved`.
   - Si falta alguna línea, no crea ninguna reserva y publica
     `InventoryRejected`.
5. Order consume el resultado y cambia el estado a `CONFIRMED` o
   `REJECTED`.

Un pedido se cumple **completo o no se cumple** (ADR-003). El cliente no
recibe reservas parciales. El `submit` devuelve la orden en `PENDING`
porque la confirmación es eventual. Un SKU repetido en el mismo `PUT` se
rechaza con `400` y se nombran los SKUs: el servidor no suma cantidades,
porque eso inventaría la intención del cliente. Se propone exponer
`GET /orders/{id}` para consultar el estado final; aún no está aprobado ni
implementado.

## Contratos de eventos

Los tres topics usan `orderId` como clave de mensaje. Así, todos los
eventos de un pedido caen en la misma partición y se consumen en orden.

### `orders.created`

```json
{
  "orderId": 42,
  "createdAt": "2026-11-26T14:31:52Z",
  "lines": [
    { "productId": "SKU-1", "quantity": 2 },
    { "productId": "SKU-2", "quantity": 1 }
  ]
}
```

Publicado por Order una vez por pedido.

### `inventory.reserved`

```json
{
  "orderId": 42,
  "reservedAt": "2026-11-26T14:31:53Z"
}
```

Publicado por Inventory cuando todas las líneas se reservaron.

### `inventory.rejected`

```json
{
  "orderId": 42,
  "reason": "INSUFFICIENT_STOCK",
  "rejectedAt": "2026-11-26T14:31:53Z"
}
```

Publicado por Inventory cuando alguna línea no se pudo reservar. Los
motivos previstos son `INSUFFICIENT_STOCK` y `UNKNOWN_PRODUCT`.

## Idempotencia

El `orderId` lo genera la base de datos al crear el borrador. Repetir el
`PUT` sobre un `DRAFT` deja exactamente las mismas líneas, porque el cuerpo
describe el estado completo deseado. Reenviar el `PUT` o el `submit` sobre
una orden que ya pasó de `DRAFT` no produce cambios ni eventos. Inventory
guarda cada reserva con `orderId` único: si Kafka reentrega un
`OrderCreated` ya atendido, confirma el offset y no crea nuevas reservas.
Order puede aplicar el mismo resultado varias veces sin cambiar el estado
final.

## Limitaciones conocidas

- La publicación de `OrderCreated` se implementará en la Fase 4. Hasta
  entonces el `PUT` solo sustituye líneas del borrador y el `submit` solo
  cambia el estado a `PENDING`.
- Cuando se publique directamente tras el commit, un fallo de publicación
  dejará el pedido en `PENDING`. Se sustituirá por el patrón Outbox en la
  Fase 5.
- Los borradores `DRAFT` abandonados necesitarán una política de limpieza.
- Order no comprueba que el SKU exista al crear el pedido: `product_id` es
  un valor opaco sin FK. Inventory, que es dueño del stock, rechazará el
  SKU desconocido con `UNKNOWN_PRODUCT`. El cliente ve primero `PENDING` y
  después `REJECTED`.
- La autenticación HTTP Basic protege a los clientes de Order. La
  identidad entre servicios no se ha decidido todavía.
- El `PUT` y el `submit` solo son efectivos mientras la orden está en
  `DRAFT`; en otros estados devuelven la orden existente sin cambios.
- La cancelación o eliminación debe permitirse solo sobre `DRAFT` y solo
  al propietario o a un admin. Requiere vincular la orden con la identidad
  autenticada; todavía no está implementado.

## Evolución prevista: Catalog Service

Si se necesita rechazar un SKU inexistente de forma inmediata (`400`),
hará falta un servicio dueño del catálogo que Order consulte antes de
persistir. Se añadirá solo cuando ese beneficio compense el coste de un
tercer servicio (ADR-002). No se creará una tabla `products` dentro de
Order: duplicaría un dominio ajeno y exigiría sincronización.
