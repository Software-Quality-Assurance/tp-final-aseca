import logging
import json
import os
import time
from collections.abc import Callable
from html import escape
from pathlib import Path

from gevent import sleep
from gevent.lock import Semaphore
from locust import HttpUser, between, events, task


def env_bool(name: str, default: bool = False) -> bool:
    return os.getenv(name, str(default)).strip().lower() in {"1", "true", "yes", "on"}


def env_float(name: str, default: float) -> float:
    return float(os.getenv(name, str(default)))


def env_int(name: str, default: int) -> int:
    return int(os.getenv(name, str(default)))


def env_str(name: str, default: str) -> str:
    return os.getenv(name, default).strip()


def request_timeout_seconds() -> float:
    if active_profile() == "stress":
        return env_float("STRESS_REQUEST_TIMEOUT_SECONDS", 2.0)
    return env_float("LOAD_REQUEST_TIMEOUT_SECONDS", 5.0)


def set_active_profile(name: str) -> None:
    os.environ["LOCUST_PROFILE"] = name


def active_profile() -> str:
    return os.getenv("LOCUST_PROFILE", "load").strip().lower()


CAPACITY_RESULT_PATH = Path(os.getenv("LOCUST_CAPACITY_RESULT_PATH", "/load-tests/results/capacity.json"))


def discovered_load_users(default: int = 150) -> int:
    configured = os.getenv("LOAD_USERS", "").strip()
    if configured:
        return int(configured)

    try:
        result = json.loads(CAPACITY_RESULT_PATH.read_text(encoding="utf-8"))
        stable_users = int(result["last_stable_users"])
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError):
        return default

    factor = env_float("LOAD_CAPACITY_FACTOR", 0.8)
    return max(1, int(stable_users * factor))


def load_profile_settings() -> dict[str, int]:
    return {
        "max_users": discovered_load_users(),
        "spawn_rate": env_int("LOAD_SPAWN_RATE", 3),
        "ramp_seconds": env_int("LOAD_RAMP_SECONDS", 50),
        "steady_seconds": env_int("LOAD_STEADY_SECONDS", 120),
        "ramp_down_seconds": env_int("LOAD_RAMP_DOWN_SECONDS", 30),
    }


def stress_profile_settings() -> dict[str, int]:
    start_users = env_int("STRESS_START_USERS", 120)
    multiplier = env_float("STRESS_USER_MULTIPLIER", 1.5)
    stages = env_int("STRESS_STAGES", 9)
    targets = [max(start_users, int(round(start_users * (multiplier**stage)))) for stage in range(stages)]
    return {
        "start_users": start_users,
        "stages": stages,
        "max_users": targets[-1],
        "spawn_rate": env_int("STRESS_SPAWN_RATE", 20),
        "stage_seconds": env_int("STRESS_STAGE_SECONDS", 60),
    }


def service_capacities() -> list[str]:
    return [
        env_str("LOCUST_CAPACITY_API", "1 GB RAM, 1 core para API"),
        env_str("LOCUST_CAPACITY_DB", "512 MB RAM, 0.5 core para DB"),
        env_str("LOCUST_CAPACITY_EXTERNAL", "512 MB RAM, 0.5 core para API externa"),
    ]


def profile_comparison_rows() -> list[dict[str, str]]:
    load_settings = load_profile_settings()
    stress_settings = stress_profile_settings()
    return [
        {
            "metric": "Objetivo",
            "load": "Validar carga esperada sostenible",
            "stress": "Encontrar el primer punto de degradacion",
        },
        {
            "metric": "Patron de usuarios",
            "load": "Ramp-up, meseta estable, ramp-down",
            "stress": "Escalones crecientes de concurrencia",
        },
        {
            "metric": "Usuarios maximos",
            "load": str(load_settings["max_users"]),
            "stress": str(stress_settings["max_users"]),
        },
        {
            "metric": "Creacion de usuarios/seg",
            "load": str(load_settings["spawn_rate"]),
            "stress": str(stress_settings["spawn_rate"]),
        },
        {
            "metric": "Duracion principal",
            "load": f'{load_settings["steady_seconds"]} s de meseta',
            "stress": f'{stress_settings["stages"]} x {stress_settings["stage_seconds"]} s',
        },
        {
            "metric": "Criterio de lectura",
            "load": "Sostiene umbrales durante la meseta",
            "stress": "Marca el escalon donde fallan los umbrales",
        },
    ]


def load_profile_summary() -> str:
    settings = load_profile_settings()
    return (
        f"Load Testing ({settings['max_users']} usuarios, {settings['spawn_rate']}/s). "
        "El objetivo es validar que el sistema funcione correctamente bajo una carga "
        "ligeramente superior a la esperada en produccion. "
        f"Con {settings['max_users']} usuarios concurrentes durante {settings['steady_seconds']} segundos, "
        "se simula un escenario realista para una aplicacion nueva o con crecimiento controlado. "
        f"La tasa de {settings['spawn_rate']} usuarios/segundo permite un crecimiento gradual "
        "sin sobrecargar inmediatamente el sistema."
    )


def stress_profile_summary() -> str:
    settings = stress_profile_settings()
    first_stage = settings["start_users"]
    last_stage = settings["max_users"]
    return (
        f"Stress Testing ({first_stage} a {last_stage} usuarios, {settings['spawn_rate']}/s). "
        "El objetivo es forzar al sistema mas alla de la carga esperada para detectar "
        "el primer escalon donde aumentan errores o se degradan los tiempos. "
        f"Se avanza en {settings['stages']} etapas de {settings['stage_seconds']} segundos "
        "con crecimiento multiplicativo de usuarios, para identificar con evidencia "
        "el punto de saturacion observable."
    )


def capacity_comparison_html() -> str:
    current = active_profile()
    include_edgar = "si" if env_bool("INCLUDE_EDGAR", False) else "no"
    capacity_items = "".join(f"<li>{escape(item)}</li>" for item in service_capacities())
    rows = "".join(
        (
            "<tr>"
            f"<th>{escape(row['metric'])}</th>"
            f"<td>{escape(row['load'])}</td>"
            f"<td>{escape(row['stress'])}</td>"
            "</tr>"
        )
        for row in profile_comparison_rows()
    )
    return f"""<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Locust Capacity Comparison</title>
  <style>
    body {{
      font-family: Arial, sans-serif;
      margin: 24px auto 48px;
      max-width: 1100px;
      line-height: 1.5;
      color: #14213d;
      padding: 0 16px;
      background: #ffffff;
    }}
    h1, h2 {{
      margin-bottom: 12px;
    }}
    .hero {{
      background: linear-gradient(90deg, #d6ebff 0%, #b8d8ff 55%, #d6ebff 100%);
      padding: 18px 20px;
      font-size: 2.2rem;
      font-weight: 700;
      margin-bottom: 18px;
    }}
    .meta {{
      background: #f4f9ff;
      border: 1px solid #bfd8f3;
      border-radius: 10px;
      padding: 16px;
      margin-bottom: 24px;
    }}
    .section {{
      background: linear-gradient(90deg, #e8f3ff 0%, #cfe5ff 58%, #edf6ff 100%);
      padding: 10px 14px;
      font-size: 1.4rem;
      font-weight: 700;
      margin-bottom: 16px;
    }}
    .capacity-box {{
      background: #f4f9ff;
      border: 1px solid #bfd8f3;
      border-radius: 10px;
      padding: 18px 22px;
      margin-bottom: 24px;
    }}
    .capacity-box ul {{
      margin: 10px 0 0 20px;
    }}
    .story {{
      background: linear-gradient(90deg, #eef7ff 0%, #dcecff 100%);
      border-left: 6px solid #7bb0ea;
      padding: 16px 18px;
      margin-bottom: 20px;
      font-size: 1.15rem;
    }}
    .story strong {{
      font-size: 1.2rem;
    }}
    table {{
      border-collapse: collapse;
      width: 100%;
      margin-bottom: 24px;
    }}
    th, td {{
      border: 1px solid #d1d5db;
      padding: 10px 12px;
      text-align: left;
      vertical-align: top;
    }}
    thead th {{
      background: #111827;
      color: #f9fafb;
    }}
    tbody th {{
      background: #f9fafb;
      width: 28%;
    }}
    .active {{
      color: #065f46;
      font-weight: 700;
    }}
    .muted {{
      color: #6b7280;
    }}
    a {{
      color: #2563eb;
    }}
    code {{
      background: #eef2f7;
      padding: 2px 4px;
      border-radius: 4px;
    }}
  </style>
</head>
<body>
  <div class="hero">Diferenciacion entre Load Testing y Stress Testing</div>
  <div class="meta">
    <p><strong>Perfil activo:</strong> <span class="active">{escape(current)}</span></p>
    <p><strong>EDGAR incluido:</strong> {include_edgar}</p>
    <p><strong>Umbrales:</strong> fail ratio &lt;= {env_float("LOCUST_MAX_FAILURE_RATIO", 0.01):.2%}, p95 &lt;= {env_int("LOCUST_MAX_P95_MS", 1000)} ms</p>
    <p class="muted">Abrir esta vista en <code>/capacity-comparison</code> mientras corre Locust en modo web.</p>
  </div>
  <div class="section">Capacidades de los servicios</div>
  <div class="capacity-box">
    <ul>
      {capacity_items}
    </ul>
  </div>
  <div class="story"><strong>{escape(load_profile_summary().split('. ')[0])}.</strong> {escape('. '.join(load_profile_summary().split('. ')[1:]))}</div>
  <div class="story"><strong>{escape(stress_profile_summary().split('. ')[0])}.</strong> {escape('. '.join(stress_profile_summary().split('. ')[1:]))}</div>
  <h2>Cuadro comparativo</h2>
  <table>
    <thead>
      <tr>
        <th>Metrica</th>
        <th>Load</th>
        <th>Stress</th>
      </tr>
    </thead>
    <tbody>
      {rows}
    </tbody>
  </table>
</body>
</html>"""


def configured_tickers() -> tuple[str, str]:
    tickers = tuple(
        ticker.strip().upper()
        for ticker in os.getenv("LOCUST_TICKERS", "ACLS,ACU").split(",")
        if ticker.strip()
    )
    if len(tickers) != 2:
        raise ValueError("LOCUST_TICKERS must contain exactly two comma-separated tickers")
    return tickers


def edgar_budget_rps() -> float:
    requests_per_second = env_float("EDGAR_MAX_REQUESTS_PER_SECOND", 8.0)
    if requests_per_second > 10:
        raise ValueError("EDGAR_MAX_REQUESTS_PER_SECOND cannot exceed SEC's 10 requests/second limit")
    return requests_per_second


class RequestBudget:
    """Cooperatively paces estimated outbound requests across one Locust process."""

    def __init__(
        self,
        requests_per_second: float,
        worker_count: int = 1,
        clock: Callable[[], float] = time.monotonic,
        sleeper: Callable[[float], None] = sleep,
    ) -> None:
        if requests_per_second <= 0:
            raise ValueError("requests_per_second must be positive")
        if worker_count <= 0:
            raise ValueError("worker_count must be positive")

        self._interval = worker_count / requests_per_second
        self._clock = clock
        self._sleeper = sleeper
        self._lock = Semaphore()
        self._next_slot = 0.0

    def acquire(self, cost: int = 1) -> None:
        if cost <= 0:
            raise ValueError("cost must be positive")

        with self._lock:
            now = self._clock()
            if self._next_slot > now:
                self._sleeper(self._next_slot - now)
            self._next_slot = max(self._next_slot, self._clock()) + (self._interval * cost)


EDGAR_BUDGET = RequestBudget(
    requests_per_second=edgar_budget_rps(),
    worker_count=env_int("LOCUST_WORKER_COUNT", 1),
)
PRIMARY_TICKER, SECONDARY_TICKER = configured_tickers()


class AuthenticatedApiUser(HttpUser):
    abstract = True
    wait_time = between(1, 3)
    _registered_accounts: set[str] = set()
    _prepared_accounts: set[str] = set()
    _setup_lock = Semaphore()

    def on_start(self) -> None:
        run_id = os.getenv("LOCUST_TEST_RUN_ID", "local")
        shared_email = os.getenv("LOCUST_USER_EMAIL") or f"locust-{run_id}@example.com"
        password = os.getenv("LOCUST_USER_PASSWORD") or "Password123!"

        if not self._ensure_registered(shared_email, password):
            return

        with self.api_post(
            "/api/auth/login",
            json={"email": shared_email, "password": password},
            name="/api/auth/login [setup]",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Login failed with {response.status_code}")
                return
            token = response.json().get("token")
            if not token:
                response.failure("Login response did not contain a token")
                return
            response.success()
            self.client.headers.update({"Authorization": f"Bearer {token}"})
            self._prepare_watchlist(shared_email)

    def _ensure_registered(self, email: str, password: str) -> bool:
        with self._setup_lock:
            if email in self._registered_accounts:
                return True

            with self.api_post(
                "/api/auth/register",
                json={"email": email, "password": password},
                name="/api/auth/register [setup]",
                catch_response=True,
            ) as response:
                if response.status_code not in {201, 409}:
                    response.failure(f"Registration failed with {response.status_code}")
                    return False
                response.success()

            self._registered_accounts.add(email)
            return True

    def _prepare_watchlist(self, email: str) -> None:
        with self._setup_lock:
            if email in self._prepared_accounts:
                return

            for ticker in (PRIMARY_TICKER, SECONDARY_TICKER):
                with self.api_post(
                    f"/api/watchlist/{ticker}",
                    name="/api/watchlist/:ticker [setup]",
                    catch_response=True,
                ) as response:
                    if response.status_code not in {201, 409}:
                        response.failure(f"Watchlist setup failed for {ticker} with {response.status_code}")
                        return
                    response.success()

            self._prepared_accounts.add(email)

    def api_get(self, path: str, **kwargs):
        kwargs.setdefault("timeout", request_timeout_seconds())
        return self.client.get(path, **kwargs)

    def api_post(self, path: str, **kwargs):
        kwargs.setdefault("timeout", request_timeout_seconds())
        return self.client.post(path, **kwargs)


class PortfolioApiUser(AuthenticatedApiUser):
    weight = 9

    @task(4)
    def portfolio(self) -> None:
        self.api_get("/api/portfolio", name="/api/portfolio")

    @task(3)
    def portfolio_value(self) -> None:
        self.api_get("/api/portfolio/value", name="/api/portfolio/value")

    @task(3)
    def profit_loss(self) -> None:
        self.api_get("/api/portfolio/profit-loss", name="/api/portfolio/profit-loss")

    @task(2)
    def history(self) -> None:
        self.api_get("/api/portfolio/history", name="/api/portfolio/history")

    @task(2)
    def watchlist(self) -> None:
        self.api_get("/api/watchlist", name="/api/watchlist")

    @task
    def companies(self) -> None:
        self.api_get("/api/company?page=1", name="/api/company")

    @task
    def company_search(self) -> None:
        self.api_get(
            f"/api/company/search?ticker={PRIMARY_TICKER}",
            name="/api/company/search",
        )


class StressPortfolioApiUser(PortfolioApiUser):
    wait_time = between(
        env_float("STRESS_WAIT_MIN_SECONDS", 0.2),
        env_float("STRESS_WAIT_MAX_SECONDS", 0.6),
    )


class EdgarApiUser(AuthenticatedApiUser):
    fixed_count = 1
    wait_time = between(1, 2)

    @task(3)
    def metrics(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.api_get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/metrics",
            name="/api/edgar/companies/:ticker/metrics",
        )

    @task(2)
    def filings(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.api_get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/filings",
            name="/api/edgar/companies/:ticker/filings",
        )

    @task(2)
    def history(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.api_get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/history?metric=REVENUE&quarters=8",
            name="/api/edgar/companies/:ticker/history",
        )

    @task
    def search(self) -> None:
        # A cold search may request both company_tickers.json and EFTS.
        EDGAR_BUDGET.acquire(cost=2)
        self.api_get(
            f"/api/edgar/search?query={PRIMARY_TICKER}",
            name="/api/edgar/search",
        )

    @task
    def comparison(self) -> None:
        # The prepared watchlist contains two companies, each requiring Company Facts.
        EDGAR_BUDGET.acquire(cost=2)
        self.api_get("/api/edgar/comparison", name="/api/edgar/comparison")


def active_user_classes() -> list[type[HttpUser]]:
    classes: list[type[HttpUser]] = [StressPortfolioApiUser if active_profile() == "stress" else PortfolioApiUser]
    if env_bool("INCLUDE_EDGAR", False):
        classes.append(EdgarApiUser)
    return classes


@events.init.add_listener
def register_capacity_comparison(environment, **_kwargs) -> None:
    if not environment.web_ui:
        return

    app = environment.web_ui.app
    if "capacity_comparison" in app.view_functions:
        return

    @app.route("/capacity-comparison")
    def capacity_comparison():
        return capacity_comparison_html()


@events.quitting.add_listener
def enforce_thresholds(environment, **_kwargs) -> None:
    stats = environment.stats.total
    max_failure_ratio = env_float("LOCUST_MAX_FAILURE_RATIO", 0.01)
    max_p95_ms = env_int("LOCUST_MAX_P95_MS", 1000)
    p95 = stats.get_response_time_percentile(0.95) or 0

    failures = []
    if stats.fail_ratio > max_failure_ratio:
        failures.append(f"failure ratio {stats.fail_ratio:.2%} > {max_failure_ratio:.2%}")
    if p95 > max_p95_ms:
        failures.append(f"p95 {p95} ms > {max_p95_ms} ms")

    if failures:
        logging.error("Performance thresholds failed: %s", "; ".join(failures))
        environment.process_exit_code = 1
    elif not environment.process_exit_code:
        environment.process_exit_code = 0
