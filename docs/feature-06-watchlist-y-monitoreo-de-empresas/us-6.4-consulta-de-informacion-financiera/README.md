# User Story 6.4 - Consulta de información financiera

**Feature:** 6 - Watchlist y monitoreo de empresas

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** consultar métricas financieras, filings recientes y evolución histórica de una compañía

**Para** evaluar empresas con información oficial y actualizada.

## Criterios de aceptación

1. Dado una compañía con datos disponibles, cuando se consultan métricas, entonces devuelve Revenue, Net Income, EPS, Total Assets y Total Liabilities.
2. Dado una métrica no disponible, cuando se devuelve la respuesta, entonces se marca como faltante.
3. Dado respuesta XBRL compleja, cuando llega al frontend, entonces se transforma a DTO simple.
4. Dado un error de SEC EDGAR, cuando ocurre, entonces el sistema devuelve mensaje controlado.
5. Dado una compañía con filings, cuando se consultan filings recientes, entonces devuelve 10-K y 10-Q.
6. Dado cada filing, cuando se muestra, entonces incluye tipo, fecha y URL o referencia al documento.
7. Dado una empresa sin filings relevantes, cuando se consulta, entonces devuelve lista vacía con mensaje claro.
8. Dado EDGAR caído, cuando se consulta, entonces se informa indisponibilidad temporal.
9. Dado una métrica con datos históricos, cuando se consulta evolución, entonces devuelve entre 4 y 8 quarters.
10. Dado menos de 4 quarters disponibles, cuando se consulta, entonces devuelve los disponibles.
11. Dado cada punto histórico, cuando se devuelve, entonces incluye período, año fiscal y valor.
12. Dado datos incompletos, cuando se arma la serie, entonces se indica que la evolución es parcial.
