# ADR-002: Arquitectura de microservicios del workshop

## Estado

Aceptado

## Contexto y problema

El workshop busca enseñar los fundamentos de microservicios, comunicación
asíncrona con Kafka y consistencia eventual. La pregunta clave es: ¿cuántos
servicios necesitamos para practicar estos conceptos sin caer en una
arquitectura sobredimensionada?

## Requisitos y restricciones

-   El alcance funcional es una plataforma de gestión de pedidos.
-   La prioridad es aprender, no construir un sistema productivo completo.
-   Cada servicio debe tener una responsabilidad clara y justificada.
-   Debe haber comunicación entre servicios que justifique el uso de Kafka.
-   El diseño debe permitir extender el sistema en el futuro.

## Alternativas consideradas

### Opción A: Cuatro servicios + Gateway

-   **Servicios:** Catalog, Order, Inventory, Notification + API Gateway.
-   **Ventajas:** más realista, cubre más casos de uso.
-   **Inconvenientes:** mayor complejidad operativa, más contenedores, más
    configuración. Gran parte del código se repetiría siguiendo el mismo
    patrón.

### Opción B: Tres servicios esenciales

-   **Servicios:** Catalog, Order, Inventory.
-   **Ventajas:** reduce la complejidad respecto a la Opción A.
-   **Inconvenientes:** el catálogo no aporta al problema central de
    consistencia eventual entre pedido e inventario.

### Opción C: Dos servicios

-   **Servicios:** Order e Inventory.
-   **Ventajas:** enfoca el aprendizaje en el problema esencial (pedido +
    reserva + eventos + consistencia eventual). Menor overhead operativo.
    Facilita experimentar con duplicados, reintentos y fallos.
-   **Inconvenientes:** menos representativo de un sistema real grande.
    Catalog y Notification se dejan para una fase posterior.

## Decisión tomada

Se elige la **Opción C**: implementar **Order Service** e **Inventory
Service** como microservicios independientes.

Catalog Service, Notification Service y API Gateway se añadirán solo si el
flujo principal lo justifica o si se necesitan para completar un objetivo
de aprendizaje específico.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** aprendizaje enfocado, menos código repetitivo, despliegue
    más ligero, posibilidad de profundizar en los patrones distribuidos.
-   **Inconvenientes:** el workshop no cubrirá ciertos aspectos de un
    ecosistema con muchos servicios (gateway, descubrimiento, etc.) hasta
    que se añadan explícitamente.
-   **Consecuencias:** cada servicio tendrá su propia base de datos. Order
    publicará eventos de pedido creado. Inventory consumirá esos eventos,
    intentará reservar stock y publicará el resultado. Order actualizará el
    estado del pedido en base a esos eventos.

## Fuentes

-   `PLAN.md`, sección 2: alcance funcional y servicios candidatos.
