# User Story 4.5 - Valuación con precios de mercado

**Feature:** 4 - Operaciones e historial

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** que mis posiciones y empresas seguidas se valúen con precios de mercado actualizados y almacenados

**Para** conocer el valor de mis inversiones sin depender de consultas en tiempo real al usar la aplicación.

## Criterios de aceptación

1. Dado un catálogo con compañías activas, cuando se actualizan precios, entonces el sistema considera todos sus tickers únicos.
2. Dado tickers repetidos en el catálogo, cuando se procesan, entonces se consulta una sola vez por ticker.
3. Dado que no hay compañías activas, cuando se ejecuta la actualización, entonces no se consulta al proveedor externo.
4. Dado tickers inactivos o eliminados, cuando se actualizan precios, entonces no se incluyen en el cálculo.
5. Dado un ticker válido, cuando se consulta la fuente de mercado, entonces se obtiene el último precio disponible.
6. Dado un precio obtenido correctamente, cuando se guarda, entonces queda asociado a la compañía correspondiente.
7. Dado una valuación de portfolio, cuando el backend consulta precios, entonces usa los últimos precios almacenados.
8. Dado que Yahoo Finance no está disponible, cuando el usuario consulta su valuación, entonces la vista sigue funcionando con precios previamente almacenados.
9. Dado una compañía sin precio almacenado, cuando se intenta calcular valuación o P&L, entonces se informa que faltan datos suficientes.
10. Dado una actualización nueva, cuando ya existía un precio anterior, entonces se conserva el histórico.
11. Dado la entrada guardada, cuando se consulta, entonces incluye precio, timestamp y fuente.
12. Dado errores parciales al actualizar precios, cuando ocurren, entonces se registran sin interrumpir todo el proceso.
