# ADR-007: Idempotencia por pedido en Inventory Service

## Estado

Aceptado

## Contexto y problema

ADR-006 propuso `registro_ordenes.source_event_id` como `UNIQUE` para deduplicar eventos de Kafka. ADR-003 define pedidos multilinea: un mismo pedido genera varias líneas de movimiento. Si todas comparten el mismo identificador de evento, la restricción `UNIQUE` rechazaría la segunda línea del pedido.

Además, el desarrollador decidió que no se escribirán campos a medias: si una reserva falla, se hace rollback de todo y el pedido se reenvía completo. No se reparan escrituras parciales.

Este ADR sustituye parcialmente ADR-006: sus puntos 4 y 68 quedan sin efecto. El modelo de tablas y el `CHECK` de `tipo_de_movimiento` se mantienen.

## Requisitos y restricciones

- Un pedido multilinea se aplica una sola vez, o no se aplica.
- Una reentrega es un no-op; no descuenta stock dos veces.
- No puede existir stock descontado sin su movimiento, ni movimiento sin su marker de pedido procesado.
- Order e Inventory tienen bases distintas (ADR-002): `order_id` es un ID lógico heredado, no una foreign key física.
- Un SKU repetido en el evento es error: se nombra el SKU y se deja log para futura monitorización. No se suman cantidades.
- Una reentrega concurrente debe fallar rápido por clave primaria duplicada.

## Alternativas consideradas

### Opción A: mantener `source_event_id` `UNIQUE` por movimiento

- **Ventajas:** deduplicación por evento.
- **Inconvenientes:** incompatible con pedidos multilinea (ADR-003). Descartada.

### Opción B: `UNIQUE (order_id, product_id)` en `registro_ordenes`

- **Ventajas:** deduplica cada línea.
- **Inconvenientes:** sigue siendo idempotencia por línea, no por pedido; un SKU repetido ya se trata como error, por lo que no aporta. Descartada.

### Opción C: tabla `processed_order(order_id PK)`

- **Ventajas:** idempotencia por pedido, compatible con multilinea; la PK detecta reentregas concurrentes en el `flush`.
- **Inconvenientes:** añade una tabla; el marker solo es fiable si todas las escrituras pasan por el servicio.

### Opción D: reparar escrituras parciales

- **Ventajas:** permitiría reanudar un pedido a medias.
- **Inconvenientes:** añade estado inconsistente que hay que reconciliar. Descartada: se prefiere rollback completo y reenvío del pedido entero.

## Decisión tomada

Se elige la **Opción C**:

1. La migración V2 elimina `registro_ordenes.source_event_id` y crea `processed_order(order_id BIGINT PRIMARY KEY, processed_at TIMESTAMPTZ)`.
2. `InventoryService.reserveOrder(...)` es una única transacción local:
   - valida pedido vacío, SKU vacío, cantidad no positiva y SKUs repetidos;
   - si `processed_order` ya contiene `orderId`, termina sin efectos;
   - inserta el marker con `saveAndFlush` antes de tocar el stock;
   - por cada línea descuenta `stock_disponible` y escribe un movimiento `salida`;
   - cualquier error lanza una excepción y hace rollback de todo.
3. `existsById(orderId)` basta para detectar una reentrega porque `orderId` es la clave primaria del marker.
4. No hay foreign key a Order: el `orderId` se hereda lógicamente del pedido.

## Ventajas, inconvenientes y consecuencias

- **Ventajas:** pedidos multilinea idempotentes; estado siempre todo-o-nada; esquema más simple que `source_event_id`; causa de fallo trazable por logs.
- **Inconvenientes:** una reentrega concurrente puede terminar en error de PK; el consumidor de Kafka deberá reintentar hasta ver el marker. Una edición manual en base de datos puede romper la garantía.
- **Consecuencias:** ADR-006 queda parcialmente sustituido. No se implementa reposición de stock ni reconciliación de estados parciales.

## Fuentes

- Decisión del desarrollador, 2026-11-27.
- ADR-003: pedido multilinea y reserva todo-o-nada.
- ADR-006: modelo de stock y movimientos de Inventory Service.