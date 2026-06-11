# User Story 6.3 - Búsqueda de empresas para análisis

**Feature:** 6 - Watchlist y monitoreo de empresas

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** buscar empresas por nombre o ticker y seleccionar la compañía correcta para analizar

**Para** acceder a información financiera oficial antes de decidir si invertir, mantener o seguir una empresa.

## Criterios de aceptación

1. Dado un usuario autenticado, cuando busca por ticker, entonces el sistema consulta o usa datos de SEC EDGAR.
2. Dado una búsqueda por nombre parcial, cuando hay resultados, entonces devuelve empresas relacionadas.
3. Dado una búsqueda sin resultados, cuando no existe la empresa en la whitelist, entonces devuelve lista vacía con 200 OK.
4. Dado SEC EDGAR no disponible, cuando se busca, entonces responde con error controlado, por ejemplo 503 Service Unavailable.
5. Dado un ticker válido, cuando se consulta el identificador oficial, entonces el sistema devuelve el CIK correcto.
6. Dado un ticker inexistente, cuando se consulta, entonces responde 404 Not Found.
7. Dado un CIK obtenido, cuando se consulta Company Facts o Submissions, entonces se usa ese CIK.
