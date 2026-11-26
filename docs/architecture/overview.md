# Arquitectura del workshop

> Documento de diseño de la Fase 2. Decisiones: ADR-001, ADR-002 y ADR-003.

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
    Cliente -->|POST /orders| Order[Order Service]
    Order --> OrdersDB[(workshop_orders)]
    Order -->|OrderCreated| T1[(orders.created)]
    T1 --> Inventory[Inventory Service]
    Inventory --> InvDB[(workshop_inventory)]
    Inventory -->|InventoryReserved| T2[(inventory.reserved)]
    Inventory -->|InventoryRejected| T3[(inventory.rejected)]
    T2 --> Order
    T3 --> Order
    Cliente -.->|GET /orders/{id}, propuesto| Order
```

## Flujo de negocio

1. El cliente envía `POST /orders` con una o más líneas
   (`productId`, `quantity`).
2. Order guarda el pedido con estado `PENDING` y publica `OrderCreated`.
3. Inventory consume `OrderCreated` y, en una única transacción local,
   comprueba el stock de **todas** las líneas:
   - Si hay stock suficiente, crea una reserva por línea y publica
     `InventoryReserved`.
   - Si falta alguna línea, no crea ninguna reserva y publica
     `InventoryRejected`.
4. Order consume el resultado y cambia el estado a `CONFIRMED` o
   `REJECTED`.

Un pedido se cumple **completo o no se cumple** (ADR-003). El cliente no
recibe reservas parciales. La respuesta inicial es `201` con estado
`PENDING` porque la confirmación es eventual. Se propone exponer
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

Publicado por Inventory cuando alguna línea no se pudo reservar.

## Idempotencia

Inventory guarda cada reserva con `orderId` único. Si Kafka reentrega un
`OrderCreated` ya atendido, el consumidor confirma el offset y no crea
nuevas reservas. Order puede aplicar el mismo resultado varias veces sin
cambiar el estado final.

## Limitaciones conocidas

- Order publica el evento directamente tras el commit de la base de datos.
  Si la publicación falla, el pedido se queda `PENDING`. Se sustituirá por
  el patrón Outbox en la Fase 5.
- La autenticación HTTP Basic protege a los clientes de Order. La
  identidad entre servicios no se ha decidido todavía.
