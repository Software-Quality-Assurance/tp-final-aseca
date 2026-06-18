# Load y stress testing

Esta carpeta implementa los ensayos de rendimiento requeridos por el TP con Locust. Los tests ejercitan la API Spring Boot real, su autenticación JWT y PostgreSQL. No llaman directamente a SEC EDGAR: cualquier acceso externo pasa por el backend y por su rate limiter.

## Diferencia entre load y stress

### Load test

`load.py` verifica el comportamiento bajo la carga esperada. Si existe
`results/capacity.json`, usa por default el 80% del último escalón estable
descubierto por stress; si todavía no existe, usa 150 usuarios como fallback.

| Etapa | Duración default | Usuarios |
| --- | ---: | ---: |
| Ramp-up | 50 s | 1 a 150 |
| Meseta | 120 s | 150 |
| Ramp-down | 30 s | 150 a 1 |

El objetivo es comprobar estabilidad durante una meseta representativa: tasa de errores menor o igual al 1% y p95 menor o igual a 1000 ms.

### Stress test

`stress.py` incrementa concurrencia hasta encontrar el primer escalón que
incumple el SLO (`p95 > 500 ms` o errores `> 1%`):

| Etapa | Duración default | Usuarios |
| --- | ---: | ---: |
| 1 | 60 s | 120 |
| 2 | 60 s | 180 |
| 3 | 60 s | 270 |
| 4 | 60 s | 405 |
| 5 | 60 s | 608 |
| 6 | 60 s | 911 |
| 7 | 60 s | 1367 |
| 8 | 60 s | 2050 |
| 9 | 60 s | 3075 |

Al incumplir el SLO, Locust agrega el fallo visible
`[stress] capacity exceeded`, termina con código `1` y guarda en
`results/capacity.json` el escalón fallido, el último estable y la cantidad
recomendada para load.

En `stress`, los usuarios usan un `think time` corto pero no nulo por default (`0.2` a `0.6 s`) para empujar RPS altos sin romper demasiado temprano.
Ademas, `stress` usa un timeout corto por request (`2 s` por default) para que las caidas del backend se reflejen rapido como fallos en Locust y no queden ocultas como requests colgadas durante demasiado tiempo.

En Docker Compose, la capacidad objetivo actual queda acotada a `1 CPU / 1 GB`
para `portfolio-api` y `0.5 CPU / 512 MB` para `db`.

Los valores son una línea base para un TP y deben ajustarse después de medir el entorno de defensa. Las curvas son configurables por variables de entorno sin modificar código.

## Mix de tráfico

`PortfolioApiUser` representa navegación autenticada habitual:

- portfolio;
- valuación;
- profit/loss;
- historial;
- watchlist;
- listado y búsqueda local de compañías.

Las lecturas principales tienen mayor peso que búsquedas auxiliares. Se evita modificar posiciones durante la medición para no introducir conflictos entre usuarios que comparten la cuenta de prueba.

Antes de medir, Locust registra o reutiliza una cuenta y prepara idempotentemente una watchlist con los dos valores de `LOCUST_TICKERS`. Los defaults `ACLS,ACU` existen en la whitelist actual. Esto evita medir únicamente colecciones vacías y permite comparar compañías reales sin modificar posiciones durante la ejecución.

`EdgarApiUser` es opt-in y mantiene una sola instancia. Recorre búsqueda, métricas, filings, histórico y comparación. Cada operación reserva un costo estimado en `RequestBudget`.

## Yahoo Finance

El proceso Yahoo Finance no forma parte del tráfico continuo de Locust porque el TP lo define como un batch independiente que se ejecuta una única vez por invocación. Incluirlo como tarea repetitiva modelaría un comportamiento que la aplicación no tiene y distorsionaría el load test.

La integración y tolerancia a fallos de ese batch se validan en su suite y workflow propios. Locust mide el API que consumen las aplicaciones web/mobile y verifica que la valuación utilice los precios ya persistidos.

## Protección de SEC EDGAR

SEC limita el consumo a 10 requests por segundo. Los ensayos usan estas defensas:

1. `INCLUDE_EDGAR=false` por default, por lo que load y stress normales sólo prueban componentes internos.
2. Cuando se habilita EDGAR, existe un solo `EdgarApiUser`.
3. `RequestBudget` usa por default 8 requests externas estimadas por segundo, dejando 20% de margen.
4. Búsqueda reserva costo 2 porque en frío puede consultar el directorio de compañías y EFTS.
5. Comparación reserva costo 2 porque la watchlist preparada contiene dos compañías.
6. El backend mantiene además su limitador global de 10 requests por segundo.
7. En ejecución distribuida debe definirse `LOCUST_WORKER_COUNT` con el total de workers. El presupuesto se divide entre ellos.

No ejecutar varios procesos independientes con EDGAR habilitado sin repartir el presupuesto. Para dos workers:

```text
LOCUST_WORKER_COUNT=2
EDGAR_MAX_REQUESTS_PER_SECOND=8
```

## Ejecución con Docker

Levantar la API:

```powershell
$env:SPRING_PROFILES_ACTIVE="seed"
docker compose --profile api up -d --build
```

El perfil `seed` carga la whitelist de `companies.json` en una base nueva. Puede omitirse únicamente si la base ya contiene las compañías.

Primero ejecutar stress y observarlo en `http://127.0.0.1:8089`:

```powershell
docker compose --profile api --profile load-tests up --build --force-recreate locust-stress
```

Cuando termine por incumplir el SLO, ejecutar load con la capacidad descubierta:

```powershell
docker compose --profile api --profile load-report run --rm locust-load
```

Los resultados se generan en `load-tests/results/` y no se versionan.

La UI visible queda disponible sólo para `stress` en `http://127.0.0.1:8089`, con `autostart`.

`load` queda configurado para ejecutarse headless y generar artifacts. Se puede
forzar manualmente una cantidad definiendo `LOAD_USERS`; si queda vacío, usa el
80% de `last_stable_users`.

La comparación entre perfiles queda disponible en `http://127.0.0.1:8089/capacity-comparison`. La vista muestra el perfil activo, las capacidades declaradas de los servicios, una explicación estilo defensa para `load` y `stress`, y un cuadro comparativo para usar como evidencia.

Para incluir EDGAR:

```powershell
$env:INCLUDE_EDGAR="true"
$env:EDGAR_MAX_REQUESTS_PER_SECOND="8"
$env:LOCUST_TEST_RUN_ID="edgar-local"
```

## Configuración

| Variable | Default | Uso |
| --- | ---: | --- |
| `INCLUDE_EDGAR` | `false` | Habilita tráfico que puede producir llamadas reales a SEC. |
| `EDGAR_MAX_REQUESTS_PER_SECOND` | `8` | Presupuesto total estimado para SEC. Debe ser menor o igual a 10. |
| `LOCUST_WORKER_COUNT` | `1` | Divide el presupuesto EDGAR en ejecución distribuida. |
| `LOCUST_TICKERS` | `ACLS,ACU` | Exactamente dos tickers activos de `companies.json` usados para setup y EDGAR. |
| `LOCUST_MAX_FAILURE_RATIO` | `0.01` | Máxima proporción de requests fallidas. |
| `LOCUST_MAX_P95_MS` | `1000` | Máximo p95 permitido para que el proceso finalice exitosamente. |
| `LOCUST_CAPACITY_API` | `1 GB RAM, 1 core para API` | Texto mostrado como capacidad declarada del backend. |
| `LOCUST_CAPACITY_DB` | `512 MB RAM, 0.5 core para DB` | Texto mostrado como capacidad declarada de la base. |
| `LOCUST_CAPACITY_EXTERNAL` | `512 MB RAM, 0.5 core para API externa` | Texto mostrado como capacidad declarada de integraciones externas. |
| `LOAD_USERS` | vacío | Fuerza usuarios de load; vacío usa capacidad descubierta o fallback 150. |
| `LOAD_CAPACITY_FACTOR` | `0.8` | Porcentaje del último escalón estable usado por load. |
| `LOAD_RAMP_SECONDS` | `50` | Duración de ramp-up. |
| `LOAD_STEADY_SECONDS` | `120` | Duración de la meseta. |
| `LOAD_RAMP_DOWN_SECONDS` | `30` | Duración de ramp-down. |
| `LOAD_SPAWN_RATE` | `3` | Usuarios creados por segundo. |
| `LOAD_REQUEST_TIMEOUT_SECONDS` | `5` | Timeout por request del perfil load. |
| `STRESS_START_USERS` | `120` | Usuarios del primer escalón. |
| `STRESS_USER_MULTIPLIER` | `1.5` | Multiplicador de usuarios entre escalones. |
| `STRESS_STAGES` | `9` | Máximo de escalones antes de declarar techo no encontrado. |
| `STRESS_STAGE_SECONDS` | `60` | Duración de cada escalón. |
| `STRESS_SPAWN_RATE` | `20` | Usuarios creados por segundo. |
| `STRESS_WAIT_MIN_SECONDS` | `0.2` | Espera mínima entre requests del perfil stress. |
| `STRESS_WAIT_MAX_SECONDS` | `0.6` | Espera máxima entre requests del perfil stress. |
| `STRESS_REQUEST_TIMEOUT_SECONDS` | `2` | Timeout por request del perfil stress. |
| `STRESS_MAX_FAILURE_RATIO` | `0.01` | Tasa de errores que hace fallar el escalón. |
| `STRESS_MAX_P95_MS` | `500` | p95 que hace fallar el escalón. |
| `STRESS_MIN_STAGE_REQUESTS` | `100` | Mínimo de requests para evaluar un escalón. |

## CI

`.github/workflows/locust.yml` realiza dos niveles de validación:

- en pull requests ejecuta los tests unitarios del presupuesto y de las curvas;
- mediante `workflow_dispatch` levanta el stack real, ejecuta `load` o `stress` y publica los reportes como artifact.

La opción EDGAR está deshabilitada por default y debe habilitarse explícitamente al lanzar el workflow manual.

## Verificación de integración

El 11 de junio de 2026 se ejecutaron smoke tests sobre el stack Docker real con PostgreSQL:

| Escenario | Usuarios máximos | Requests | Errores | p95 agregado |
| --- | ---: | ---: | ---: | ---: |
| Load reducido | 5 | 51 | 0 | 50 ms |
| Stress reducido | 6 | 37 | 0 | 49 ms |
| EDGAR controlado a 2 req/s | 2 | 28 | 0 | 800 ms |

Estos resultados prueban la integración, la generación de reportes y los códigos de salida. No representan todavía el dimensionamiento final: para la defensa deben ejecutarse los perfiles completos y conservar sus artifacts.

## Interpretación

Para cada ejecución registrar:

- hardware o runner utilizado;
- perfil y variables;
- cantidad máxima de usuarios;
- requests por segundo;
- p50, p95 y p99;
- porcentaje de errores;
- primer escalón donde se incumplen los umbrales;
- confirmación de que EDGAR estuvo deshabilitado o limitado.

Load test responde si la carga esperada es sostenible. Stress test identifica el punto de degradación y no debe presentarse como una capacidad garantizada de producción.
