# Feature 4 - Operaciones e historial

Permite registrar compras y ventas de acciones, mantener consistencia del portfolio, auditar transacciones y calcular el valor actualizado de las inversiones con precios de mercado almacenados.

## User Stories

- [User Story 4.1 - Registro consistente de operaciones](us-4.1-registro-consistente-de-operaciones/README.md) - Backend
- [User Story 4.2 - Historial de operaciones](us-4.2-historial-de-operaciones/README.md) - Mixta
- [User Story 4.3 - Cálculo del valor actual del portfolio](us-4.3-calculo-del-valor-actual-del-portfolio/README.md) - Mixta
- [User Story 4.4 - Análisis de ganancia y pérdida](us-4.4-analisis-de-ganancia-y-perdida/README.md) - Mixta
- [User Story 4.5 - Valuación con precios de mercado](us-4.5-valuacion-con-precios-de-mercado/README.md) - Mixta
- [User Story 4.6 - Actualización auditable de precios](us-4.6-actualizacion-auditable-de-precios/README.md) - Mixta

## Integración con Yahoo Finance

Las historias `US 4.5` y `US 4.6` se implementan mediante el módulo independiente `yahoo-finance/`.

El batch:

- Busca compañías activas presentes en una watchlist o con una posición neta positiva.
- Deduplica tickers antes de consultar Yahoo Finance.
- Descarga los precios mediante `yfinance`.
- Persiste nuevas filas en `prices` sin eliminar el histórico.
- Guarda timestamp y fuente `YAHOO_FINANCE`.
- Registra ejecuciones y errores parciales para auditoría.
- Puede ejecutarse localmente, con Docker o de forma opcional desde GitHub Actions.

El seed temporal anterior fue retirado. Los valores `price` de `companies.json` volvieron a `null` y `DataLoader` ya no crea precios. Los tests del backend preparan sus propios precios controlados.

La configuración y los comandos de ejecución están en [`yahoo-finance/README.md`](../../yahoo-finance/README.md).
