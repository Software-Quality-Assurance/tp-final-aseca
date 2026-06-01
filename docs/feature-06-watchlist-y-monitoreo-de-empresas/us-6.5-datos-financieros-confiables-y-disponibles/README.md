# User Story 6.5 - Datos financieros confiables y disponibles

**Feature:** 6 - Watchlist y monitoreo de empresas

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** recibir datos financieros normalizados, claros y tolerantes a fallas del proveedor externo

**Para** poder comparar compañías sin interpretar estructuras técnicas ni confundir datos faltantes con valores reales.

## Criterios de aceptación

1. Dado una respuesta XBRL de EDGAR, cuando se procesa, entonces se convierte a DTO simple.
2. Dado campos irrelevantes de EDGAR, cuando se transforma, entonces no se exponen innecesariamente al frontend.
3. Dado datos faltantes, cuando se transforma, entonces se representan como null o “no disponible”.
4. Dado el DTO final, cuando lo consume el frontend, entonces no necesita conocer la estructura interna de EDGAR.
5. Dado múltiples requests a EDGAR, cuando se ejecutan, entonces no superan 10 requests por segundo.
6. Dado que se alcanza el límite, cuando hay más requests, entonces se encolan o esperan.
7. Dado una prueba automatizada, cuando simula muchas requests, entonces verifica que no se excede el límite.
8. Dado un error de rate limit externo, cuando ocurre, entonces el sistema responde de forma controlada.
9. Dado una request a EDGAR, cuando se envía, entonces incluye un User-Agent descriptivo.
10. Dado configuración por entorno, cuando cambia el contacto o nombre del proyecto, entonces puede actualizarse sin tocar lógica de negocio.
11. Dado ausencia de User-Agent, cuando se valida configuración, entonces se considera error de configuración.
12. Dado un ticker inexistente, cuando se busca, entonces el sistema responde 404 o lista vacía según endpoint.
13. Dado una búsqueda sin resultados, cuando se muestra en frontend, entonces aparece mensaje claro.
14. Dado una empresa inexistente, cuando se intenta consultar métricas o agregar a watchlist, entonces se rechaza la operación sin llamar innecesariamente a endpoints dependientes.
15. Dado una métrica faltante, cuando se compara o muestra en frontend, entonces se diferencia de un valor real y no se toma como 0.
16. Dado una respuesta parcial, cuando se devuelve, entonces el resto de métricas disponibles sí se muestran.
17. Dado EDGAR no disponible o timeout, cuando se consulta, entonces el backend responde con error controlado y mensaje claro.
18. Dado un error externo, cuando se registra en logs, entonces contiene información útil para debug sin exponer secretos.
