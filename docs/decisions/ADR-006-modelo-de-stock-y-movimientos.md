# ADR-006: Modelo de stock y movimientos de Inventory Service

## Estado

Aceptado

## Contexto y problema

Inventory debe gestionar el stock por SKU y soportar salidas (reservas de
pedidos) y entradas (devoluciones o ajustes manuales). Una tabla que solo
registre reservas no permite explicar cómo se repone el stock ni dejar
constancia de las entradas.

Además, Kafka puede reentregar eventos. Aplicar dos veces una salida
descontaría stock dos veces.

## Requisitos y restricciones

-   Un saldo por SKU con `available_quantity >= 0` (ADR-003).
-   Historial de entradas y salidas con producto, pedido opcional, fecha y
    unidades.
-   Idempotencia: un mismo evento externo no puede aplicarse dos veces.
-   La reserva debe ser todo-o-nada en una transacción local (ADR-003).
-   Base de datos propia `workshop_inventory` (ADR-002).

## Alternativas consideradas

### Opción A: solo tabla `reservations`

Registra únicamente salidas.

-   **Ventajas:** mínima.
-   **Inconvenientes:** no representa entradas ni devoluciones. Descartada.

### Opción B: dos tablas `stock_entries` y `stock_exits`

-   **Ventajas:** explícitas.
-   **Inconvenientes:** duplican columnas, constraints e índices; las
    consultas del histórico requieren `UNION`.

### Opción C: tabla única con enum `tipo_de_movimiento`

-   **Ventajas:** una sola estructura; entrada y salida difieren solo en el
    valor del enum.
-   **Inconvenientes:** el tipo se valida con un `CHECK` en la base de datos.

## Decisión tomada

Se elige la **Opción C** con los nombres acordados por el desarrollador:

1.  `stock_disponible(product_id, available_quantity)` guarda el saldo.
2.  `registro_ordenes(id, product_id, order_id, tipo_de_movimiento,
    quantity, occurred_at, source_event_id)` guarda entradas y salidas.
3.  `tipo_de_movimiento` admite `entrada` o `salida`.
4.  `source_event_id` es `UNIQUE`: si un evento se reentrega, el inserto
    falla y el saldo no se modifica dos veces.
5.  `order_id` es nullable: una devolución lo lleva; un ajuste manual no.
6.  El saldo y el movimiento se escriben en la misma transacción local.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** historial completo; idempotencia en la base de datos;
    saldo siempre coherente con el último movimiento aplicado.
-   **Inconvenientes:** el saldo está desnormalizado respecto al historial;
    hay que actualizar ambas tablas juntas.
-   **Consecuencias:** las entidades JPA y los repositorios de Inventory
    seguirán esta estructura. La publicación/consumo de eventos llega en la
    Fase 4 y usará `source_event_id` como clave de deduplicación.

## Fuentes

-   Decisión del desarrollador, 2026-11-26.
-   ADR-003: pedido multilinea y reserva atomica.
