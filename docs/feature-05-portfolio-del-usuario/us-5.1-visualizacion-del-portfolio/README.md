# User Story 5.1 - Visualización del portfolio

**Feature:** 5 - Portfolio del usuario

**Tipo:** Frontend

## Historia

**Como** usuario

**Quiero** ver la composición actual de mi portfolio, incluyendo estados vacíos

**Para** conocer mis posiciones cargadas y entender el estado inicial de mi cuenta.

## Criterios de aceptación

1. Dado un usuario autenticado con posiciones, cuando consulta su portfolio, entonces ve sus acciones cargadas.
2. Dado cada posición del portfolio, cuando se muestra, entonces incluye compañía, ticker, cantidad y precio de referencia.
3. Dado posiciones existentes, cuando se actualizan operaciones, entonces el portfolio refleja la composición actual.
4. Dado un usuario recién registrado o sin posiciones, cuando consulta su portfolio, entonces recibe una lista vacía con 200 OK.
5. Dado un portfolio vacío, cuando se muestra en frontend, entonces se informa que todavía no hay acciones cargadas.
6. Dado un portfolio vacío, cuando se navega a otras secciones o se calcula valor actual, entonces la aplicación no rompe y el valor total es 0.
7. Dado un usuario autenticado, cuando intenta consultar el portfolio de otro usuario, entonces el sistema rechaza el acceso.
