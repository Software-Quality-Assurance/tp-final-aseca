# Feature 6 - Watchlist y monitoreo de empresas

Permite que el usuario siga empresas de interés, compare métricas financieras oficiales y evalúe oportunidades de inversión sin agregarlas necesariamente a su portfolio.

## User Stories

- [User Story 6.1 - Gestión de watchlist](us-6.1-gestion-de-watchlist/README.md) - Mixta
- [User Story 6.2 - Comparación de métricas financieras](us-6.2-comparacion-de-metricas-financieras/README.md) - Mixta
- [User Story 6.3 - Búsqueda de empresas para análisis](us-6.3-busqueda-de-empresas-para-analisis/README.md) - Mixta
- [User Story 6.4 - Consulta de información financiera](us-6.4-consulta-de-informacion-financiera/README.md) - Mixta
- [User Story 6.5 - Datos financieros confiables y disponibles](us-6.5-datos-financieros-confiables-y-disponibles/README.md) - Mixta

## Integración SEC EDGAR

La integración se ejecuta desde el backend Spring porque `data.sec.gov` no soporta CORS y el frontend debe recibir DTOs simples.

Endpoints:

- `GET /api/edgar/search?query={texto}`: combina el mapa oficial de tickers/CIK con EDGAR Full-Text Search y filtra por compañías activas de la whitelist.
- `GET /api/edgar/companies/{ticker}/metrics`: Revenue, Net Income, EPS, Total Assets y Total Liabilities.
- `GET /api/edgar/companies/{ticker}/filings`: últimos formularios 10-K y 10-Q con URL oficial.
- `GET /api/edgar/companies/{ticker}/history?metric={metrica}&quarters={4..8}`: evolución trimestral.
- `GET /api/edgar/comparison`: comparación deduplicada de compañías del portfolio y watchlist.

Fuentes oficiales utilizadas:

- `company_tickers.json` para ticker, nombre y CIK.
- `Company Facts` para métricas e históricos XBRL.
- `Submissions` para filings.
- `EDGAR Full-Text Search` para búsquedas por texto.

La comparación considera mayor como mejor para Revenue, Net Income, EPS y Total Assets. Para Total Liabilities considera menor como mejor. Los valores faltantes no participan y los empates marcan a todas las compañías correspondientes.

Todas las llamadas incluyen el User-Agent `tp-final-aseca fmanfredi@mail.austral.edu.ar`, timeout configurable y un rate limiter global de 10 requests por segundo.
