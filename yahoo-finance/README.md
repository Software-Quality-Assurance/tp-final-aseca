# Yahoo Finance price updater

Modulo batch independiente que implementa `US 4.5` y `US 4.6`.

## Responsabilidad

- Obtiene los tickers unicos que estan en una watchlist o tienen una posicion neta positiva.
- Excluye companias inactivas y posiciones totalmente vendidas.
- Descarga los precios en una sola invocacion de `yfinance.download`.
- Inserta una nueva fila en `prices` por cada precio valido, preservando el historico.
- Guarda `timestamp` y `source = YAHOO_FINANCE`.
- Registra cada ejecucion en `price_update_runs`.
- Registra fallos por ticker en `price_update_failures`.
- Continua ante errores parciales y falla con codigo distinto de cero si ningun ticker pudo actualizarse.

El batch no crea companias ni consulta todos los tickers del catalogo. Solo procesa companias activas actualmente utilizadas.

## Configuracion

| Variable | Default | Uso |
| --- | --- | --- |
| `DATABASE_URL` | `postgresql://portfolio_user:portfolio_pass@localhost:5433/portfolio_tracker` | Conexion a la misma PostgreSQL del backend |
| `YAHOO_TIMEOUT_SECONDS` | `15` | Timeout de descarga |
| `LOG_LEVEL` | `INFO` | Nivel de logs |
| `PRICE_UPDATE_TRIGGER` | `MANUAL` | Origen auditable de la ejecucion, por ejemplo `MANUAL`, `CI` o `SCHEDULED` |

La base debe tener previamente las tablas del backend (`companies`, `history`, `watchlist` y `prices`).

## Ejecucion local

```powershell
cd yahoo-finance
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e ".[test]"
.\.venv\Scripts\python.exe -m yahoo_finance
```

La ultima linea de salida es JSON:

```json
{"exit_code": 0, "failed_tickers": 0, "requested_tickers": 2, "run_id": 1, "status": "SUCCESS", "successful_tickers": 2}
```

## Docker

Con la base y el backend inicializados:

```powershell
docker compose --profile prices run --rm yahoo-finance
```

## Tests

Tests de dominio, sin red ni base:

```powershell
.\.venv\Scripts\python.exe -m pytest -m "not integration and not live"
```

Integracion PostgreSQL:

```powershell
$env:TEST_DATABASE_URL="postgresql://portfolio_user:portfolio_pass@localhost:5433/portfolio_tracker"
.\.venv\Scripts\python.exe -m pytest -m integration
```

Integracion real con Yahoo Finance:

```powershell
$env:RUN_LIVE_YAHOO_TESTS="1"
.\.venv\Scripts\python.exe -m pytest -m live
```

El test live se ejecuta solo de manera explicita para evitar depender de la disponibilidad externa en cada build.
Cuando `RUN_LIVE_YAHOO_TESTS` y `TEST_DATABASE_URL` estan definidos, tambien se ejecuta el recorrido end-to-end contra Yahoo y PostgreSQL.
