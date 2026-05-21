# User Story 4.2 - Historial de operaciones

**Feature:** 4 - Operaciones e historial

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** ver, editar y eliminar entradas de mi historial de operaciones

**Para** auditar mis movimientos y corregir errores de carga cuando sea necesario.

## Criterios de aceptación

1. Dado un usuario con operaciones, cuando consulta el historial, entonces ve todas sus compras y ventas.
2. Dado un usuario sin operaciones, cuando consulta el historial, entonces recibe lista vacía con 200 OK.
3. Dado múltiples operaciones, cuando se muestran, entonces aparecen ordenadas por fecha descendente.
4. Dado una operación registrada, cuando se muestra en historial, entonces incluye fecha, tipo, compañía, ticker, cantidad, precio unitario y precio total.
5. Dado una operación BUY o SELL, cuando se visualiza, entonces se identifica claramente como compra o venta.
6. Dado una operación con precio almacenado, cuando se muestra, entonces mantiene el precio usado en el momento de la operación.
7. Dado una entrada existente, cuando se editan campos válidos, entonces se actualiza correctamente.
8. Dado una edición que afecta cantidad o tipo, cuando se guarda, entonces el portfolio mantiene consistencia.
9. Dado una entrada inexistente, cuando se intenta editar o eliminar, entonces responde 404 Not Found.
10. Dado una edición inválida, cuando se intenta guardar, entonces responde 400 Bad Request.
11. Dado una entrada existente, cuando se elimina, entonces deja de aparecer en el historial.
12. Dado una eliminación que afecta el portfolio, cuando se confirma, entonces el sistema recalcula o mantiene consistencia según regla definida.
13. Dado un usuario autenticado, cuando consulta historial ajeno, entonces el sistema rechaza el acceso.
