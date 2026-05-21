# User Story 3.1 - Administración y búsqueda de compañías

**Feature:** 3 - Gestión de compañías

**Tipo:** Backend

## Historia

**Como** usuario o desarrollador

**Quiero** crear, buscar, listar, actualizar y eliminar compañías

**Para** mantener un catálogo confiable de empresas utilizables por el resto del sistema.

## Criterios de aceptación

1. Dado datos válidos de compañía, cuando se registra, entonces el sistema responde 201 Created.
2. Dado un ticker ya registrado, cuando se intenta crear o actualizar otra compañía con el mismo ticker, entonces responde 409 Conflict.
3. Dado campos obligatorios faltantes o inválidos, cuando se intenta crear o actualizar una compañía, entonces responde 400 Bad Request.
4. Dado una compañía existente, cuando se busca por nombre completo, nombre parcial o ticker, entonces el sistema devuelve resultados relacionados.
5. Dado una búsqueda con mayúsculas o minúsculas distintas, cuando se ejecuta, entonces el sistema normaliza el criterio y encuentra la compañía correspondiente.
6. Dado una búsqueda sin resultados, cuando no hay coincidencias, entonces devuelve lista vacía con 200 OK o 404 Not Found según el endpoint definido.
7. Dado compañías cargadas, cuando se consulta el listado, entonces el sistema devuelve todas las compañías disponibles paginadas.
8. Dado que no hay compañías cargadas, cuando se consulta el listado, entonces devuelve lista vacía con 200 OK.
9. Dado muchas compañías, cuando se consulta el listado, entonces la respuesta es paginable.
10. Dado una compañía existente, cuando se actualizan datos válidos, entonces el sistema responde 200 OK y refleja los cambios.
11. Dado una compañía existente, cuando se elimina, entonces deja de estar disponible y no aparece en búsquedas o listados.
12. Dado una compañía asociada a portfolios, watchlists o historial , cuando se intenta eliminar, entonces el sistema evita inconsistencias.
13. Whitelist: Generar companias seedeadas
