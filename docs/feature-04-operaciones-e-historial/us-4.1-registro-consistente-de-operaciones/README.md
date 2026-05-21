# User Story 4.1 - Registro consistente de operaciones

**Feature:** 4 - Operaciones e historial

**Tipo:** Backend

## Historia

**Como** usuario

**Quiero** registrar compras y ventas de acciones usando el precio vigente almacenado

**Para** que mi portfolio e historial reflejen correctamente mis operaciones de inversión.

## Criterios de aceptación

1. Dado un usuario autenticado y un precio almacenado, cuando compra acciones, entonces se registra una operación BUY.
2. Dado una compra exitosa, cuando se consulta el portfolio, entonces aumenta o crea la posición correspondiente.
3. Dado una compra exitosa, cuando se consulta el historial, entonces aparece la transacción.
4. Dado una compañía sin precio almacenado, cuando se intenta comprar, entonces el sistema responde 422 Unprocessable Entity.
5. Dado un usuario con acciones disponibles, cuando vende una cantidad válida, entonces se registra una operación SELL.
6. Dado una venta parcial, cuando se completa, entonces la posición queda reducida.
7. Dado una venta total, cuando se completa, entonces la posición se elimina o queda en 0 según diseño definido.
8. Dado una venta mayor a la tenencia, cuando se intenta ejecutar, entonces responde 422 y no modifica portfolio ni historial.
9. Dado una cantidad 0 o negativa, cuando se intenta comprar o vender, entonces responde 400 Bad Request.
10. Dado una empresa inexistente, cuando se intenta operar, entonces responde 404 Not Found. (whitelist)
11. Dado una operación inválida o fallida, cuando se consulta el portfolio y el historial, entonces no se registran cambios parciales.
12. Dado múltiples operaciones sobre el mismo ticker, cuando se consulta el portfolio, entonces refleja la posición agregada correctamente.
