import logging
from datetime import UTC, datetime
from decimal import Decimal, ROUND_HALF_UP

import pandas as pd
import yfinance as yf

from yahoo_finance.models import FetchResult, MarketPrice


logger = logging.getLogger(__name__)


class YahooFinanceProvider:
    def __init__(self, timeout_seconds: int = 15) -> None:
        self.timeout_seconds = timeout_seconds

    def fetch_latest_prices(self, tickers: list[str]) -> FetchResult:
        normalized = sorted({ticker.strip().upper() for ticker in tickers if ticker.strip()})
        if not normalized:
            return FetchResult(prices={}, errors={})

        data = yf.download(
            tickers=normalized,
            period="5d",
            interval="1d",
            group_by="ticker",
            auto_adjust=False,
            progress=False,
            threads=True,
            timeout=self.timeout_seconds,
            multi_level_index=True,
        )

        fetched_at = datetime.now(UTC)
        prices: dict[str, MarketPrice] = {}
        errors: dict[str, str] = {}

        for ticker in normalized:
            try:
                close = self._latest_close(data, ticker, len(normalized))
                if close is None or close <= 0:
                    raise ValueError("Yahoo Finance returned no positive closing price")
                prices[ticker] = MarketPrice(
                    ticker=ticker,
                    value=Decimal(str(close)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP),
                    timestamp=fetched_at,
                    source="YAHOO_FINANCE",
                )
            except Exception as error:
                reason = str(error)
                errors[ticker] = reason
                logger.warning("Could not obtain price for %s: %s", ticker, reason)

        return FetchResult(prices=prices, errors=errors)

    @staticmethod
    def _latest_close(data: pd.DataFrame, ticker: str, ticker_count: int) -> float | None:
        if data.empty:
            return None

        if isinstance(data.columns, pd.MultiIndex):
            level_zero = set(map(str, data.columns.get_level_values(0)))
            if ticker in level_zero:
                ticker_data = data[ticker]
                close_series = ticker_data["Close"]
            elif ticker_count == 1 and "Close" in level_zero:
                close_data = data["Close"]
                close_series = close_data.iloc[:, 0] if isinstance(close_data, pd.DataFrame) else close_data
            else:
                return None
        else:
            if "Close" not in data.columns:
                return None
            close_series = data["Close"]

        valid = close_series.dropna()
        if valid.empty:
            return None
        return float(valid.iloc[-1])
