# User Story 4.4 - Análisis de ganancia y pérdida

**Feature:** 4 - Operaciones e historial

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** ver la ganancia, pérdida y rendimiento porcentual de mis posiciones y del portfolio completo

**Para** entender qué inversiones rinden mejor o peor.

## Criterios de aceptación

1. Dado una posición con precio de compra y precio actual mayor, cuando se calcula P&L, entonces el resultado es positivo.
2. Dado una posición con precio actual menor, cuando se calcula P&L, entonces el resultado es negativo.
3. Dado una posición con precio actual igual al de compra, cuando se calcula P&L, entonces el resultado es 0.
4. Dado múltiples lotes de compra, cuando se calcula P&L, entonces se respeta el precio original de cada operación o se define un promedio ponderado.
5. Dado una posición con precio de compra y precio actual, cuando se calcula rendimiento, entonces se devuelve porcentaje de variación.
6. Dado una ganancia o pérdida, cuando se muestra el rendimiento, entonces el porcentaje conserva el signo correspondiente.
7. Dado datos insuficientes, cuando falta precio de compra o precio actual, entonces se muestra advertencia en lugar de cálculo incorrecto.
8. Dado varias posiciones, cuando se consulta el análisis, entonces se devuelve ganancia/pérdida total.
9. Dado varias posiciones, cuando se calcula rendimiento agregado, entonces se usa el costo total invertido como base.
10. Dado posiciones con datos incompletos, cuando se calcula el agregado, entonces se informa qué posiciones no pudieron calcularse.
11. Dado un portfolio vacío, cuando se consulta análisis, entonces devuelve valores 0 o mensaje de estado vacío.
