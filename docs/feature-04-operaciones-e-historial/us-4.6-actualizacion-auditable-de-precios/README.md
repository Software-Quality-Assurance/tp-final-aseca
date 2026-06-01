# User Story 4.6 - Actualización auditable de precios

**Feature:** 4 - Operaciones e historial

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** que los precios usados para valorar mi portfolio tengan fuente, fecha de actualización y resultado de procesamiento visibles o auditables

**Para** confiar en los valores mostrados y entender cuándo la información puede estar desactualizada.

## Criterios de aceptación

1. Dado una ejecución exitosa de actualización, cuando termina, entonces queda registrada la cantidad de tickers procesados.
2. Dado un precio persistido, cuando se guarda, entonces incluye fecha/hora de actualización y fuente Yahoo Finance.
3. Dado múltiples ejecuciones, cuando se revisa la tabla de precios, entonces cada entrada conserva su timestamp.
4. Dado varios tickers, cuando falla uno, entonces la actualización continúa con los demás.
5. Dado una falla parcial, cuando termina la actualización, entonces informa éxitos y fallos.
6. Dado un ticker fallido, cuando se revisan logs, entonces aparece el motivo del error.
7. Dado un ticker inválido o una respuesta sin precio, cuando se consulta, entonces se registra error y no se persiste precio inválido.
8. Dado una ejecución manual o programada, cuando se dispara la actualización, entonces el resultado queda trazable.
9. Dado una ejecución exitosa, cuando termina, entonces devuelve código de salida 0 o respuesta exitosa.
10. Dado una falla total, cuando termina, entonces devuelve código o respuesta de error.
11. Dado una ejecución desde CI, cuando termina, entonces queda log visible en GitHub Actions.
12. Dado configuración opcional, cuando el equipo no quiere actualizar precios en cada build, entonces puede desactivar ese paso.
