# Feature 4 - Operaciones e historial

Permite registrar compras y ventas de acciones, mantener consistencia del portfolio, auditar transacciones y calcular el valor actualizado de las inversiones con precios de mercado almacenados.

## User Stories

- [User Story 4.1 - Registro consistente de operaciones](us-4.1-registro-consistente-de-operaciones/README.md) - Backend
- [User Story 4.2 - Historial de operaciones](us-4.2-historial-de-operaciones/README.md) - Mixta
- [User Story 4.3 - Cálculo del valor actual del portfolio](us-4.3-calculo-del-valor-actual-del-portfolio/README.md) - Mixta
- [User Story 4.4 - Análisis de ganancia y pérdida](us-4.4-analisis-de-ganancia-y-perdida/README.md) - Mixta
- [User Story 4.5 - Valuación con precios de mercado](us-4.5-valuacion-con-precios-de-mercado/README.md) - Mixta
- [User Story 4.6 - Actualización auditable de precios](us-4.6-actualizacion-auditable-de-precios/README.md) - Mixta

## Seed temporal de precios

Feature 4 necesita precios almacenados para registrar operaciones, calcular valor actual y calcular P&L. Hasta que la actualización real de precios quede resuelta por las historias `US 4.5 - Valuación con precios de mercado` y `US 4.6 - Actualización auditable de precios`, se mantiene un seed temporal en `portfolio-tracker/src/main/resources/companies.json`.

El objetivo del seed no es reemplazar la integración con Yahoo Finance, sino permitir que los flujos de compra, venta, valor actual y P&L puedan desarrollarse y probarse con precios almacenados desde el arranque de la aplicación.

El `DataLoader` lee el campo opcional `price` de cada compañía. Si `price` tiene valor, crea una fila inicial en `prices` asociada a esa compañía. Si `price` es `null`, solo crea la compañía.

Compañías con precio fijo temporal:

| Ticker | Compañía | Precio |
| --- | --- | ---: |
| TRWD | Tradewinds Universal | 24.50 |
| NTCS | Natics Corp. | 18.75 |
| LMMY | Exousia Bio, Inc. | 12.30 |
| NEWH | NewHydrogen, Inc. | 9.95 |
| SMNR | Semnur Pharmaceuticals, Inc. | 31.40 |

Este seed existe solo para poder desarrollar y testear Feature 4 sin depender todavía del proceso real de actualización de precios. Cuando `US 4.5` y `US 4.6` estén implementadas con datos reales/auditables, se debe borrar este workaround:

- Remover los valores fijos de `price` en `companies.json` o volverlos `null`.
- Quitar del `DataLoader` la creación automática de `Price` desde el JSON si ya no se necesita.
- Ajustar los tests que dependan de estos precios para que preparen sus propios datos o usen el proceso real de actualización de precios.
