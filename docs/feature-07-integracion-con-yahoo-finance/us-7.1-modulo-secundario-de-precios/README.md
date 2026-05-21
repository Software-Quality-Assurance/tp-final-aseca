# User Story 7.1 - Módulo secundario de precios

**Feature:** 7 - Integración con Yahoo Finance

**Tipo:** Backend Python

## Historia

**Como** desarrollador

**Quiero** implementar un módulo batch en Python conectado a PostgreSQL

**Para** actualizar y persistir precios de mercado sin acoplar esa responsabilidad al backend principal.

## Criterios de aceptación

1. Dado el módulo Python, cuando se ejecuta manualmente, entonces inicia el proceso batch.
2. Dado una ejecución exitosa, cuando termina, entonces informa cantidad de tickers procesados.
3. Dado errores parciales, cuando ocurren, entonces se registran sin interrumpir todo el proceso.
4. Dado el diseño del sistema, cuando se revisa la arquitectura, entonces el batch está desacoplado del backend principal.
5. Dado la configuración de entorno, cuando corre el batch, entonces se conecta a PostgreSQL.
6. Dado tickers en portfolio o watchlist, cuando el batch consulta la base, entonces los obtiene correctamente.
7. Dado una conexión fallida, cuando se ejecuta el batch, entonces termina con error controlado.
8. Dado el backend principal, cuando consulta precios luego del batch, entonces ve los precios insertados por Python.
9. Dado un ticker válido y precio obtenido, cuando corre el batch, entonces inserta una entrada en price.
10. Dado una entrada de precio, cuando se guarda, entonces queda asociada a una compañía.
11. Dado una actualización nueva, cuando ya existía un precio anterior, entonces se conserva el histórico.
12. Dado la entrada guardada, cuando se consulta, entonces incluye precio, timestamp y fuente.
