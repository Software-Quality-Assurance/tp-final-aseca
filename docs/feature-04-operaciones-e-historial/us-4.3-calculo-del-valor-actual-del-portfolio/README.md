# User Story 4.3 - Cálculo del valor actual del portfolio

**Feature:** 4 - Operaciones e historial

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** ver el valor actual de mi portfolio usando precios almacenados y conocer la fecha de última actualización

**Para** evaluar mi situación patrimonial sin depender de consultas en tiempo real.

## Criterios de aceptación

1. Dado un portfolio con posiciones, cuando se calcula el valor actual, entonces se multiplica cantidad por último precio almacenado.
2. Dado múltiples posiciones, cuando se calcula el total, entonces se suman los valores actuales de todas.
3. Dado un portfolio vacío, cuando se calcula, entonces el total es 0.
4. Dado una posición sin precio disponible, cuando se calcula, entonces se informa que falta precio o se excluye con advertencia explícita.
5. Dado una consulta de valor actual, cuando se ejecuta, entonces el backend principal no consulta Yahoo Finance en tiempo real.
6. Dado precios existentes en la tabla price, cuando se calcula valor actual o P&L, entonces se usa el último precio almacenado.
7. Dado que Yahoo Finance está caído, cuando se consulta el valor actual, entonces la vista sigue funcionando con precios ya almacenados.
8. Dado que no hay precio almacenado, cuando se intenta calcular, entonces el sistema informa que no hay datos suficientes.
9. Dado precios almacenados, cuando se muestra el valor actual, entonces se muestra la fecha y hora de última actualización.
10. Dado múltiples precios, cuando se muestra la valuación, entonces se informa la actualización relevante según criterio definido.
11. Dado una actualización nueva del batch, cuando se vuelve a consultar, entonces cambia el timestamp mostrado.
