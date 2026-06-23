import logging
from collections.abc import Callable
from datetime import UTC, datetime
from typing import Protocol

from yahoo_finance.models import BatchResult, FetchResult, MarketPrice, UsedTicker


logger = logging.getLogger(__name__)


class PriceRepository(Protocol):
    def ensure_audit_schema(self) -> None: ...

    def get_active_tickers(self) -> list[UsedTicker]: ...

    def start_run(self, requested_tickers: int, started_at: datetime, trigger: str) -> int: ...

    def save_price(self, company: UsedTicker, price: MarketPrice) -> None: ...

    def record_failure(self, run_id: int, ticker: str, reason: str) -> None: ...

    def finish_run(
        self,
        run_id: int,
        status: str,
        successful_tickers: int,
        failed_tickers: int,
        finished_at: datetime,
    ) -> None: ...


class MarketPriceProvider(Protocol):
    def fetch_latest_prices(self, tickers: list[str]) -> FetchResult: ...


class PriceUpdateService:
    def __init__(
        self,
        repository: PriceRepository,
        provider: MarketPriceProvider,
        clock: Callable[[], datetime] | None = None,
        trigger: str = "MANUAL",
    ) -> None:
        self.repository = repository
        self.provider = provider
        self.clock = clock or (lambda: datetime.now(UTC))
        self.trigger = trigger.strip().upper() or "MANUAL"

    def run(self) -> BatchResult:
        self.repository.ensure_audit_schema()
        companies = self._deduplicate(self.repository.get_active_tickers())
        run_id = self.repository.start_run(len(companies), self.clock(), self.trigger)

        if not companies:
            self.repository.finish_run(run_id, "SUCCESS", 0, 0, self.clock())
            logger.info("Price update run %s completed: no tickers in use", run_id)
            return BatchResult(run_id, "SUCCESS", 0, 0, 0, {}, 0)

        tickers = list(companies)
        logger.info("Price update run %s started for %s tickers", run_id, len(tickers))

        try:
            fetch_result = self.provider.fetch_latest_prices(tickers)
        except Exception as error:
            reason = f"Yahoo Finance request failed: {error}"
            errors = {ticker: reason for ticker in tickers}
            for ticker in tickers:
                self.repository.record_failure(run_id, ticker, reason)
                logger.error("Ticker %s failed: %s", ticker, reason)
            self.repository.finish_run(run_id, "FAILED", 0, len(tickers), self.clock())
            return BatchResult(run_id, "FAILED", len(tickers), 0, len(tickers), errors, 1)

        errors = dict(fetch_result.errors)
        successful = 0

        for ticker, company in companies.items():
            price = fetch_result.prices.get(ticker)
            if price is None:
                errors.setdefault(ticker, "Yahoo Finance returned no closing price")
                continue
            try:
                self.repository.save_price(company, price)
                successful += 1
                logger.info("Stored %s price %s from %s", ticker, price.value, price.source)
            except Exception as error:
                errors[ticker] = f"Could not persist price: {error}"

        for ticker, reason in errors.items():
            self.repository.record_failure(run_id, ticker, reason)
            logger.error("Ticker %s failed: %s", ticker, reason)

        failed = len(tickers) - successful
        status = self._status(successful, failed)
        exit_code = 1 if status == "FAILED" else 0
        self.repository.finish_run(run_id, status, successful, failed, self.clock())
        logger.info(
            "Price update run %s completed with status %s: %s successes, %s failures",
            run_id,
            status,
            successful,
            failed,
        )
        return BatchResult(run_id, status, len(tickers), successful, failed, errors, exit_code)

    @staticmethod
    def _deduplicate(companies: list[UsedTicker]) -> dict[str, UsedTicker]:
        unique: dict[str, UsedTicker] = {}
        for company in companies:
            ticker = company.ticker.strip().upper()
            if ticker:
                unique.setdefault(ticker, UsedTicker(company.company_id, ticker))
        return dict(sorted(unique.items()))

    @staticmethod
    def _status(successful: int, failed: int) -> str:
        if failed == 0:
            return "SUCCESS"
        if successful == 0:
            return "FAILED"
        return "PARTIAL"
