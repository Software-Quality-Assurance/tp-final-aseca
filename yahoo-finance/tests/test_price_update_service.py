from datetime import UTC, datetime
from decimal import Decimal

from yahoo_finance.models import FetchResult, MarketPrice, UsedTicker
from yahoo_finance.service import PriceUpdateService


FIXED_NOW = datetime(2026, 6, 10, 15, 30, tzinfo=UTC)


class FakeRepository:
    def __init__(self, tickers: list[UsedTicker]) -> None:
        self.tickers = tickers
        self.started_runs: list[tuple[int, str]] = []
        self.saved_prices: list[tuple[UsedTicker, MarketPrice]] = []
        self.failures: list[tuple[int, str, str]] = []
        self.finished_runs: list[tuple[int, str, int, int]] = []

    def ensure_audit_schema(self) -> None:
        pass

    def get_used_tickers(self) -> list[UsedTicker]:
        return self.tickers

    def start_run(self, requested_tickers: int, started_at: datetime, trigger: str) -> int:
        self.started_runs.append((requested_tickers, trigger))
        return 42

    def save_price(self, company: UsedTicker, price: MarketPrice) -> None:
        self.saved_prices.append((company, price))

    def record_failure(self, run_id: int, ticker: str, reason: str) -> None:
        self.failures.append((run_id, ticker, reason))

    def finish_run(
        self,
        run_id: int,
        status: str,
        successful_tickers: int,
        failed_tickers: int,
        finished_at: datetime,
    ) -> None:
        self.finished_runs.append((run_id, status, successful_tickers, failed_tickers))


class FakeProvider:
    def __init__(self, result: FetchResult) -> None:
        self.result = result
        self.calls: list[list[str]] = []

    def fetch_latest_prices(self, tickers: list[str]) -> FetchResult:
        self.calls.append(tickers)
        return self.result


def test_no_tickers_finishes_without_calling_yahoo() -> None:
    repository = FakeRepository([])
    provider = FakeProvider(FetchResult(prices={}, errors={}))

    result = PriceUpdateService(repository, provider, clock=lambda: FIXED_NOW, trigger="TEST").run()

    assert provider.calls == []
    assert repository.started_runs == [(0, "TEST")]
    assert result.status == "SUCCESS"
    assert result.requested_tickers == 0
    assert result.exit_code == 0
    assert repository.finished_runs == [(42, "SUCCESS", 0, 0)]


def test_deduplicates_tickers_and_persists_successful_prices() -> None:
    repository = FakeRepository(
        [
            UsedTicker(company_id=1, ticker="AAPL"),
            UsedTicker(company_id=1, ticker="aapl"),
            UsedTicker(company_id=2, ticker="MSFT"),
        ]
    )
    provider = FakeProvider(
        FetchResult(
            prices={
                "AAPL": MarketPrice("AAPL", Decimal("201.25"), FIXED_NOW, "YAHOO_FINANCE"),
                "MSFT": MarketPrice("MSFT", Decimal("450.10"), FIXED_NOW, "YAHOO_FINANCE"),
            },
            errors={},
        )
    )

    result = PriceUpdateService(repository, provider, clock=lambda: FIXED_NOW).run()

    assert provider.calls == [["AAPL", "MSFT"]]
    assert [company.ticker for company, _ in repository.saved_prices] == ["AAPL", "MSFT"]
    assert result.status == "SUCCESS"
    assert result.successful_tickers == 2
    assert result.failed_tickers == 0


def test_partial_yahoo_failure_is_recorded_without_stopping_other_tickers() -> None:
    repository = FakeRepository(
        [
            UsedTicker(company_id=1, ticker="AAPL"),
            UsedTicker(company_id=2, ticker="INVALID"),
        ]
    )
    provider = FakeProvider(
        FetchResult(
            prices={
                "AAPL": MarketPrice("AAPL", Decimal("201.25"), FIXED_NOW, "YAHOO_FINANCE"),
            },
            errors={"INVALID": "Yahoo Finance returned no closing price"},
        )
    )

    result = PriceUpdateService(repository, provider, clock=lambda: FIXED_NOW).run()

    assert len(repository.saved_prices) == 1
    assert repository.failures == [(42, "INVALID", "Yahoo Finance returned no closing price")]
    assert result.status == "PARTIAL"
    assert result.successful_tickers == 1
    assert result.failed_tickers == 1
    assert result.exit_code == 0


def test_total_failure_returns_non_zero_exit_code() -> None:
    repository = FakeRepository([UsedTicker(company_id=1, ticker="INVALID")])
    provider = FakeProvider(
        FetchResult(
            prices={},
            errors={"INVALID": "Yahoo Finance returned no closing price"},
        )
    )

    result = PriceUpdateService(repository, provider, clock=lambda: FIXED_NOW).run()

    assert result.status == "FAILED"
    assert result.exit_code == 1
    assert repository.finished_runs == [(42, "FAILED", 0, 1)]
