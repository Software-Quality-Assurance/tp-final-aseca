import logging
import os
import time
from collections.abc import Callable

from gevent import sleep
from gevent.lock import Semaphore
from locust import HttpUser, between, events, task


def env_bool(name: str, default: bool = False) -> bool:
    return os.getenv(name, str(default)).strip().lower() in {"1", "true", "yes", "on"}


def env_float(name: str, default: float) -> float:
    return float(os.getenv(name, str(default)))


def env_int(name: str, default: int) -> int:
    return int(os.getenv(name, str(default)))


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

        with self.client.post(
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

            with self.client.post(
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
                with self.client.post(
                    f"/api/watchlist/{ticker}",
                    name="/api/watchlist/:ticker [setup]",
                    catch_response=True,
                ) as response:
                    if response.status_code not in {201, 409}:
                        response.failure(f"Watchlist setup failed for {ticker} with {response.status_code}")
                        return
                    response.success()

            self._prepared_accounts.add(email)


class PortfolioApiUser(AuthenticatedApiUser):
    weight = 9

    @task(4)
    def portfolio(self) -> None:
        self.client.get("/api/portfolio", name="/api/portfolio")

    @task(3)
    def portfolio_value(self) -> None:
        self.client.get("/api/portfolio/value", name="/api/portfolio/value")

    @task(3)
    def profit_loss(self) -> None:
        self.client.get("/api/portfolio/profit-loss", name="/api/portfolio/profit-loss")

    @task(2)
    def history(self) -> None:
        self.client.get("/api/portfolio/history", name="/api/portfolio/history")

    @task(2)
    def watchlist(self) -> None:
        self.client.get("/api/watchlist", name="/api/watchlist")

    @task
    def companies(self) -> None:
        self.client.get("/api/company?page=1", name="/api/company")

    @task
    def company_search(self) -> None:
        self.client.get(
            f"/api/company/search?ticker={PRIMARY_TICKER}",
            name="/api/company/search",
        )


class EdgarApiUser(AuthenticatedApiUser):
    fixed_count = 1
    wait_time = between(1, 2)

    @task(3)
    def metrics(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.client.get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/metrics",
            name="/api/edgar/companies/:ticker/metrics",
        )

    @task(2)
    def filings(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.client.get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/filings",
            name="/api/edgar/companies/:ticker/filings",
        )

    @task(2)
    def history(self) -> None:
        EDGAR_BUDGET.acquire(cost=1)
        self.client.get(
            f"/api/edgar/companies/{PRIMARY_TICKER}/history?metric=REVENUE&quarters=8",
            name="/api/edgar/companies/:ticker/history",
        )

    @task
    def search(self) -> None:
        # A cold search may request both company_tickers.json and EFTS.
        EDGAR_BUDGET.acquire(cost=2)
        self.client.get(
            f"/api/edgar/search?query={PRIMARY_TICKER}",
            name="/api/edgar/search",
        )

    @task
    def comparison(self) -> None:
        # The prepared watchlist contains two companies, each requiring Company Facts.
        EDGAR_BUDGET.acquire(cost=2)
        self.client.get("/api/edgar/comparison", name="/api/edgar/comparison")


def active_user_classes() -> list[type[HttpUser]]:
    classes: list[type[HttpUser]] = [PortfolioApiUser]
    if env_bool("INCLUDE_EDGAR", False):
        classes.append(EdgarApiUser)
    return classes


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
    else:
        environment.process_exit_code = 0
