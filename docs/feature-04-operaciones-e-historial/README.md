# Feature 4 - Operaciones e historial

Permite registrar compras y ventas de acciones, mantener consistencia del portfolio y auditar todas las transacciones realizadas por el usuario.

## User Stories

- [User Story 4.1 - Registro consistente de operaciones](us-4.1-registro-consistente-de-operaciones/README.md) - Backend
- [User Story 4.2 - Historial de operaciones](us-4.2-historial-de-operaciones/README.md) - Mixta
- [User Story 4.3 - Cálculo del valor actual del portfolio](us-4.3-calculo-del-valor-actual-del-portfolio/README.md) - Mixta
- [User Story 4.4 - Análisis de ganancia y pérdida](us-4.4-analisis-de-ganancia-y-perdida/README.md) - Mixta

## Seed temporal de precios

Feature 4 necesita precios almacenados para registrar operaciones, calcular valor actual y calcular P&L. Como la integracion real de precios corresponde a Feature 7, se agrego un seed temporal en `portfolio-tracker/src/main/resources/companies.json`.

El `DataLoader` lee el campo opcional `price` de cada compania. Si `price` tiene valor, crea una fila inicial en `prices` asociada a esa compania. Si `price` es `null`, solo crea la compania.

Companias con precio fijo temporal:

| Ticker | Compania | Precio |
| --- | --- | ---: |
| TRWD | Tradewinds Universal | 24.50 |
| NTCS | Natics Corp. | 18.75 |
| LMMY | Exousia Bio, Inc. | 12.30 |
| NEWH | NewHydrogen, Inc. | 9.95 |
| SMNR | Semnur Pharmaceuticals, Inc. | 31.40 |

Este seed existe solo para poder desarrollar y testear Feature 4 sin depender todavia del batch de Yahoo Finance. Cuando Feature 7 este implementada, se debe borrar este workaround:

- Remover los valores fijos de `price` en `companies.json` o volverlos `null`.
- Quitar del `DataLoader` la creacion automatica de `Price` desde el JSON si ya no se necesita.
- Ajustar los tests que dependan de estos precios para que preparen sus propios datos o usen el proceso real de actualizacion de precios.
