# User Story 7.2 - Actualización de precios con Yahoo Finance

**Feature:** 7 - Integración con Yahoo Finance

**Tipo:** Backend Python / Infraestructura

## Historia

**Como** desarrollador

**Quiero** actualizar precios desde Yahoo Finance leyendo tickers en uso, manejando errores y permitiendo ejecución manual o desde CI

**Para** mantener valuaciones actualizadas y auditables.

## Criterios de aceptación

1. Dado portfolios y watchlists con tickers, cuando corre el batch, entonces obtiene todos los tickers únicos.
2. Dado tickers repetidos en varios usuarios, cuando se procesan, entonces se consulta una sola vez por ticker.
3. Dado que no hay tickers en uso, cuando corre el batch, entonces no consulta a Yahoo Finance.
4. Dado tickers inactivos o eliminados, cuando corre el batch, entonces no los incluye.
5. Dado un ticker válido, cuando se consulta Yahoo Finance con yfinance, entonces se obtiene el último precio disponible.
6. Dado un ticker inválido o una respuesta sin precio, cuando se consulta, entonces se registra error y no se persiste precio inválido (El sistema debe únicamente informar el error, sigue analizando de todas formas).
7. Dado una consulta exitosa, cuando termina, entonces el precio puede ser usado por el backend principal.
8. Dado un precio persistido, cuando se guarda, entonces incluye fecha/hora de actualización y fuente Yahoo Finance.
9. Dado múltiples ejecuciones, cuando se revisa la tabla, entonces cada entrada conserva su timestamp.
10. Dado varios tickers, cuando falla uno, entonces el batch continúa con los demás.
11. Dado una falla parcial, cuando termina el batch, entonces informa éxitos y fallos.
12. Dado un ticker fallido, cuando se revisan logs, entonces aparece el motivo del error.
13. Dado un comando CLI o endpoint dedicado, cuando se ejecuta, entonces dispara el batch.
14. Dado una ejecución exitosa, cuando termina, entonces devuelve código de salida 0 o respuesta exitosa.
15. Dado una falla total, cuando termina, entonces devuelve código o respuesta de error.
16. Dado un workflow configurado, cuando se ejecuta manualmente o por schedule, entonces dispara el batch.
17. Dado una ejecución desde CI, cuando termina, entonces queda log visible en GitHub Actions.
18. Dado configuración opcional, cuando el equipo no quiere actualizar precios en cada build, entonces puede desactivar ese paso.
