# User Story 6.2 - Comparación de métricas financieras

**Feature:** 6 - Watchlist y monitoreo de empresas

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** comparar métricas financieras entre empresas de mi watchlist y mi portfolio

**Para** decidir qué compañías parecen más atractivas para invertir o mantener.

## Criterios de aceptación

1. Dado dos o más empresas en watchlist, cuando se solicita comparación, entonces se muestra una tabla comparativa.
2. Dado una sola empresa, cuando se solicita comparación, entonces se muestra la empresa y una advertencia de que la comparación requiere al menos dos.
3. Dado métricas disponibles, cuando se comparan, entonces cada empresa aparece como columna o fila clara.
4. Dado una empresa con datos disponibles, cuando se consulta, entonces se muestran Revenue, Net Income, EPS, Total Assets y Total Liabilities.
5. Dado la respuesta del endpoint, cuando se consume desde frontend, entonces los campos están transformados a DTOs simples.
6. Dado una métrica financiera, cuando se muestra, entonces incluye la fecha del último reporte.
7. Dado diferentes fechas por métrica, cuando se muestran, entonces cada una conserva su fecha correspondiente.
8. Dado una empresa con suficientes datos, cuando se solicita evolución histórica, entonces se devuelven entre 4 y 8 quarters.
9. Dado una métrica seleccionada, cuando se lista, entonces cada punto incluye período, año fiscal y valor.
10. Dado una empresa del portfolio y empresas en watchlist, cuando se solicita comparación, entonces aparecen todas en la misma tabla.
11. Dado una empresa repetida en portfolio y watchlist, cuando se compara, entonces no se duplica innecesariamente.
12. Dado la comparación generada, cuando el usuario la revisa, entonces puede distinguir cuáles empresas pertenecen al portfolio y cuáles a la watchlist.
13. Dado una tabla comparativa, cuando se muestran métricas, entonces se resaltan los mejores valores relativos.
14. Dado una métrica donde “mayor” no necesariamente es mejor, cuando se resalta, entonces el criterio está documentado.
15. Dado valores no disponibles, cuando se calcula el mejor valor, entonces no se consideran como ganadores.
16. Dado empates, cuando existen, entonces se resaltan todas las empresas empatadas o se indica empate.
17. Dado una métrica faltante, cuando se muestra la tabla, entonces aparece como “No disponible” y no se interpreta como 0.
18. Dado muchas métricas faltantes, cuando se muestra la empresa, entonces se informa que la información es parcial.
19. Dado un error externo de EDGAR, cuando se intenta comparar, entonces se informa que el servicio externo no está disponible.
