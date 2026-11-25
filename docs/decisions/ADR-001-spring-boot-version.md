# ADR-001: Versión base de Spring Boot

## Estado

Aceptado

## Contexto y problema

El workshop requiere una versión de Spring Boot que:

-   Sea compatible con Java 21.
-   Tenga soporte activo y parches de seguridad disponibles.
-   Permita usar Spring Cloud y Spring Kafka en versiones estables.
-   Sea reconocible y defendible en una entrevista técnica.

La duda inicial surgió porque Spring Boot ha evolucionado rápidamente y
existía confusión sobre si la línea 4.x estaba disponible como GA o solo
como versión futura.

## Requisitos y restricciones

-   Usar una versión GA, no snapshot ni milestone.
-   Preferir estabilidad y seguridad actualizada sobre novedades.
-   Mantener compatibilidad con el resto del stack previsto (Spring
    Cloud, Spring Kafka, Java 21).
-   Documentar la fuente de la decisión.

## Alternativas consideradas

### Opción A: Spring Boot 4.2.0 (última GA)

-   **Ventajas:** versión más reciente, acceso a las últimas mejoras,
    usa Spring Framework 7.1, alineada con Spring Cloud 2026.0.x.
-   **Inconvenientes:** lanzada el 19 de noviembre de 2026, con menor
    maduración en producción real; puede tener regresiones aún no
    detectadas por la comunidad.

### Opción B: Spring Boot 4.1.3 (rama anterior estable)

-   **Ventajas:** última versión de la rama 4.1, con parches de
    seguridad recientes; construida sobre Spring Framework 7.0.10;
    considerada más segura y estable para producción actual; soporte de
    parches de código abierto hasta el 31 de julio de 2027.
-   **Inconvenientes:** no incluye las novedades más recientes de la
    rama 4.2; requiere Spring Cloud 2025.1.x en lugar de 2026.0.x.

### Opción C: Spring Boot 3.4.x (línea anterior)

-   **Ventajas:** muy madura, amplia documentación y comunidad.
-   **Inconvenientes:** no aprovecha las mejoras de Spring Boot 4.x,
    menos relevante para una entrevista actual; soporte más cercano al
    final de su ciclo.

## Decisión tomada

Se elige **Spring Boot 4.1.3** como versión base del workshop.

La decisión prioriza la estabilidad y la seguridad actualizada sobre las
últimas novedades. Si durante el workshop surge alguna necesidad
concreta de la rama 4.2, se evaluará un cambio controlado con su
propio ADR.

## Ventajas, inconvenientes y consecuencias

-   **Ventajas:** base estable, soporte confirmado, compatible con Java
    21, alineada con las recomendaciones de producción actuales.
-   **Inconvenientes:** ligeramente por detrás de la última GA; algunas
    dependencias del ecosistema pueden requerir versiones específicas
    del release train 2025.1.x.
-   **Consecuencias:** todas las aplicaciones del workshop usarán Spring
    Boot 4.1.3 salvo que se decida lo contrario mediante un nuevo ADR.
    La versión 4.2.0 queda documentada como alternativa válida.

## Fuentes

-   [Spring Boot project page](https://spring.io/projects/spring-boot#support)
-   [Spring Cloud release train compatibility](https://spring.io/projects/spring-cloud/)
-   [Spring Cloud Release Reference](https://docs.spring.io/spring-cloud-release/reference/index.html)
