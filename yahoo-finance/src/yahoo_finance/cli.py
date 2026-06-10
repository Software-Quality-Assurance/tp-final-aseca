import json
import logging
import os
import sys
from dataclasses import asdict

from yahoo_finance.postgres_repository import PostgresPriceRepository
from yahoo_finance.service import PriceUpdateService
from yahoo_finance.yahoo_provider import YahooFinanceProvider


DEFAULT_DATABASE_URL = "postgresql://portfolio_user:portfolio_pass@localhost:5433/portfolio_tracker"


def main() -> int:
    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )
    database_url = os.getenv("DATABASE_URL", DEFAULT_DATABASE_URL)
    timeout_seconds = int(os.getenv("YAHOO_TIMEOUT_SECONDS", "15"))
    trigger = os.getenv("PRICE_UPDATE_TRIGGER", "MANUAL")

    try:
        result = PriceUpdateService(
            repository=PostgresPriceRepository(database_url),
            provider=YahooFinanceProvider(timeout_seconds=timeout_seconds),
            trigger=trigger,
        ).run()
        print(json.dumps(asdict(result), sort_keys=True))
        return result.exit_code
    except Exception as error:
        logging.getLogger(__name__).exception("Price update batch failed")
        print(json.dumps({"status": "FAILED", "error": str(error)}, sort_keys=True))
        return 1


if __name__ == "__main__":
    sys.exit(main())
