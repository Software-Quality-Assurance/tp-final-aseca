import os

import pytest

from yahoo_finance.yahoo_provider import YahooFinanceProvider


pytestmark = pytest.mark.live


def test_downloads_real_prices_from_yahoo_finance() -> None:
    if os.getenv("RUN_LIVE_YAHOO_TESTS") != "1":
        pytest.skip("RUN_LIVE_YAHOO_TESTS is not enabled")

    result = YahooFinanceProvider(timeout_seconds=15).fetch_latest_prices(["AAPL", "MSFT"])

    assert set(result.prices) == {"AAPL", "MSFT"}
    assert result.errors == {}
    assert all(price.value > 0 for price in result.prices.values())
    assert all(price.source == "YAHOO_FINANCE" for price in result.prices.values())
