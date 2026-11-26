# ADR-005: Edición del borrador por reemplazo completo y envío explícito

## Estado

Aceptado. Sustituye a ADR-004.

## Contexto y problema

ADR-004 hacía que el primer `PUT /orders/{orderId}` guardara las líneas y
cambiara el estado a `PENDING`. Eso congela el borrador: el cliente ya no
puede corregir una cantidad, añadir un producto ni quitar una línea sin
empezar otro pedido.

Un pedido en `DRAFT` necesita poder editarse las veces que haga falta.
Además, las correcciones no deben dejar estados parciales ni líneas
huérfanas.

## Requisitos y restricciones

-   Un borrador debe poder editarse mientras siga en `DRAFT`.
-   Una sola petición debe describir el estado completo deseado de las
    líneas.
-   Añadir, modificar y borrar una línea deben expresarse con la misma
    operación.
-   Enviar el pedido a procesar es un cambio de ciclo de vida, distinto de
    editarlo.
-   Reenviar el envío no debe producir cambios ni eventos duplicados.
-   La base de datos sigue generando `orderId` (ADR-004).
-   Un SKU aparece como máximo una vez por pedido (ADR-003).

## Alternativas consideradas

### Opción A: `PUT` que inserta líneas y envía el pedido

Es el comportamiento actual de ADR-004.

-   **Ventajas:** una sola llamada para completar y enviar.
-   **Inconvenientes:** el primer `PUT` sentencia el pedido; no permite
    corregirlo. Descartada por error grave de diseño.

### Opción B: `PUT` de reemplazo completo y envío explícito

El cuerpo del `PUT` contiene todas las líneas deseadas. El servidor sustituye
las actuales por las recibidas. Un endpoint independiente cambia el estado a
`PENDING`.

-   **Ventajas:** añadir, modificar y borrar son la misma operación; el
    estado es siempre completo; el reemplazo es idempotente.
-   **Inconvenientes:** una petición adicional para enviar; hay que borrar
    líneas dentro de la misma transacción.

### Opción C: Endpoints por línea

`POST`, `PATCH` y `DELETE` sobre `/orders/{orderId}/lines/{productId}`.

-   **Ventajas:** granular; permite operaciones independientes.
-   **Inconvenientes:** más endpoints, más estados intermedios y más difícil
    de hacer idempotente.

### Opción D: `PUT` con una lista de operaciones

El cliente envía `add`, `update` y `delete` explícitamente.

-   **Ventajas:** intención explícita.
-   **Inconvenientes:** más complejo; el cliente y el servidor deben
    sincronizar el estado previo.

## Decisión tomada

Se elige la **Opción B**:

1.  `POST /orders/drafts` crea una orden en `DRAFT` con id generado por la
    base de datos.
2.  `PUT /orders/{orderId}` acepta la lista completa de líneas. Solo es
    efectivo mientras la orden está en `DRAFT`: valida que no haya SKUs
    repetidos, borra las líneas actuales de ese pedido y guarda exactamente
    las recibidas. La orden **sigue en `DRAFT`**.
3.  `POST /orders/{orderId}/submit` cambia el estado a `PENDING`. Si la orden
    ya no está en `DRAFT`, devuelve la existente sin cambios ni eventos.
4.  La publicación de `OrderCreated` se hará desde `submit` en la Fase 4.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** el borrador es editable; no hay líneas parciales; el
    reenvío de cualquiera de las dos operaciones es inocuo.
-   **Inconvenientes:** tres endpoints en lugar de dos; `OrderItemRepository`
    necesita una operación de borrado por `orderId`.
-   **Consecuencias:** ADR-004 queda sustituido; el test de integración debe
    reescribirse; el borrado y la inserción de líneas deben ocurrir en una
    sola transacción.

## Fuentes

-   Decisión del desarrollador, 2026-11-26.
-   [RFC 9110: PUT](https://www.rfc-editor.org/rfc/rfc9110#name-put)
