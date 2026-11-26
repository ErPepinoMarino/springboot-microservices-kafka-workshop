# ADR-004: Creación de pedidos en dos fases con id generado por la base de datos

## Estado

Sustituido por ADR-005. La creación del borrador con id de base de datos
sigue vigente; el envío del pedido ya no ocurre en el `PUT` de líneas.

## Contexto y problema

El cliente necesita poder reenviar el mismo pedido tantas veces como haga
falta sin que se creen pedidos duplicados. Al mismo tiempo, el
identificador del pedido debe ser generado por la base de datos, que es
la fuente de verdad; el cliente no debe inventarlo ni colisionar con
otros.

Un `POST /orders` único que genera el id en el servidor no puede
distinguir un primer envío de un reintento: si la respuesta se pierde, el
reintento crea otro pedido.

## Requisitos y restricciones

-   La base de datos genera `order.id`.
-   Reenviar el mismo pedido no debe insertar otra orden ni publicar otro
    evento.
-   El cliente solo crea un pedido nuevo cuando inicia voluntariamente
    otra operación.
-   El mismo `orderId` debe viajar en `OrderCreated` para que Inventory
    sea idempotente.
-   Se mantiene el alcance de dos servicios (ADR-002).

## Alternativas consideradas

### Opción A: El cliente envía `orderId` en el POST

-   **Ventajas:** una sola llamada, idempotencia inmediata por PK duplicada.
-   **Inconvenientes:** el cliente puede colisionar o falsificar ids. La
    base de datos deja de ser la fuente de verdad del identificador.

### Opción B: `Idempotency-Key` generada por el cliente

Patrón documentado por Stripe y Square. El servidor genera el id de la
orden y deduplica por la clave del cliente.

-   **Ventajas:** una sola llamada, muy probado en APIs de pago.
-   **Inconvenientes:** exige que el cliente persista una clave y la reenvíe;
    añade una tabla de deduplicación y una ventana de expiración. Choca
    con el requisito de que la BD genere la identidad.

### Opción C: Borrador creado por el servidor y envío idempotente

 El servidor crea una orden en estado `DRAFT` con id de base de datos; el
 cliente añade las líneas con una segunda llamada idempotente. Es el
 patrón de *draft order* de Sibi y GoDaddy Commerce.

-   **Ventajas:** la BD genera el id; el cliente no inventa nada; el doble
    clic se detecta por el estado de la orden; permite carritos
    abandonados.
-   **Inconvenientes:** dos llamadas y un estado adicional; hay que limpiar
    borradores abandonados.

## Decisión tomada

Se elige la **Opción C**:

1.  `POST /orders/drafts` crea una orden con `status = DRAFT` e id
    generado por la base de datos.
2.  `PUT /orders/{orderId}` recibe el `customerEmail` y las líneas, guarda
    las líneas, cambia el estado a `PENDING` y, cuando se introduzca Kafka
    en la Fase 4, publica `OrderCreated`.
3.  Si el `PUT` se repite sobre una orden que ya no está en `DRAFT`, se
    devuelve la orden existente sin guardar líneas ni publicar nada.

Se descarta usar `GET /orders/drafts` para crear el borrador: `GET` es
seguro e idempotente por definición HTTP y no debe tener efectos laterales.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** la base de datos sigue siendo la fuente de verdad; el
    reintento es inocuo; el flujo distingue claramente "crear carrito" de
    "enviar pedido".
-   **Inconvenientes:** dos peticiones HTTP y un estado `DRAFT` más.
-   **Consecuencias:** el test actual de `POST /orders` debe reescribirse;
    la publicación del evento llega en la Fase 4; habrá que decidir cómo
    limpiar borradores abandonados; Inventory sigue usando `orderId` como
    clave de idempotencia.

## Fuentes

-   Decisión del desarrollador, 2026-11-26.
-   [Stripe: idempotent requests](https://docs.stripe.com/api/idempotent_requests)
-   [Square: idempotency](https://developer.squareup.com/docs/build-basics/common-api-patterns/idempotency)
-   [Sibi: create order from draft](https://docs.sibipro.com/api-reference/draft-orders/create-order-from-draft)
-   [GoDaddy Commerce: DRAFT a OPEN](https://developer.godaddy.com/en/docs/api-users/commerce/manage-orders-and-customers)
