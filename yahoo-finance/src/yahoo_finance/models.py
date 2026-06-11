from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal


@dataclass(frozen=True)
class UsedTicker:
    company_id: int
    ticker: str


@dataclass(frozen=True)
class MarketPrice:
    ticker: str
    value: Decimal
    timestamp: datetime
    source: str


@dataclass(frozen=True)
class FetchResult:
    prices: dict[str, MarketPrice]
    errors: dict[str, str]


@dataclass(frozen=True)
class BatchResult:
    run_id: int
    status: str
    requested_tickers: int
    successful_tickers: int
    failed_tickers: int
    errors: dict[str, str]
    exit_code: int
