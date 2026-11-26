# ADR-003: Pedido con múltiples líneas y reserva todo-o-nada

## Estado

Aceptado

## Contexto y problema

El flujo del workshop necesita que Inventory Reserve stock a partir de un
pedido. La pregunta es si un pedido contiene un solo producto o varios,
y qué ocurre cuando una parte del carrito no se puede reservar.

## Requisitos y restricciones

-   Un pedido debe reflejar un carrito completo, no una línea aislada.
-   El cliente no debe recibir resultados parciales: o se cumple todo el
    pedido o no se cumple nada.
-   Order e Inventory tienen bases de datos separadas, por lo que no
    pueden compartir una transacción.
-   Los eventos pueden duplicarse, así que el consumo debe ser idempotente.

## Alternativas consideradas

### Opción A: Un producto por pedido

Un carrito con varios productos se divide en varios pedidos. Cada uno se
reserve por separado.

-   **Ventajas:** modelo mínimo, sin tabla de líneas.
-   **Inconvenientes:** obliga al cliente a gestionar varios pedidos y
    permite estados mixtos (unos confirmados, otros rechazados). Es un
    comportamiento deshonesto con el cliente.

### Opción B: Producto fijo simulado en el evento

El pedido no modela productos; el evento inventa un producto y una
cantidad fijos.

-   **Ventajas:** cero cambios en Order.
-   **Inconvenientes:** el contrato de eventos no representa el dominio
    real. Enseña una mentira que habría que corregir después.

### Opción C: Múltiples líneas y reserva atómica en Inventory

Order agrega varias líneas (`productId`, `quantity`). `OrderCreated`
incluye todas. Inventory comprueba y reserva todas las líneas en una
única transacción local sobre su propia base de datos.

-   **Ventajas:** el pedido es una unidad coherente; el cliente recibe un
    único resultado; la atomicidad se logra con una transacción local,
    sin transacciones distribuidas.
-   **Inconvenientes:** más código en Order (tabla de líneas, DTO anidado
    y tests) y obliga a diseñar idempotencia desde el principio.

## Decisión tomada

Se elige la **Opción C**. Cada pedido contiene una o más líneas y se
gestiona como una unidad: todas se reservan o ninguna se reserva.

Order publica `OrderCreated` con el identificador del pedido y todas sus
líneas. Inventory publica `InventoryReserved` o `InventoryRejected`.
Order actualiza el estado del pedido en consecuencia.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** honestidad con el cliente, dominio realista, atomicidad
    garantizada por una transacción local en Inventory.
-   **Inconvenientes:** hay que añadir la tabla de líneas de pedido, el
    DTO anidado y sus pruebas antes de publicar el primer evento.
-   **Consecuencias:** el consumidor de Inventory debe ser idempotente
    para no duplicar reservas ante reentregas de Kafka. Si en el futuro el
    stock se reparte entre varios servicios, la reserva atómica exigirá
    una Saga con compensación (reservar una línea, liberarla si falla
    otra).

## Fuentes

-   Decisión del desarrollador, 2026-11-26.
-   `PLAN.md`, sección 2: flujo principal y problemas a estudiar.
