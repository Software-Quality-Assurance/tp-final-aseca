import os

import pytest

from yahoo_finance.postgres_repository import PostgresPriceRepository
from yahoo_finance.service import PriceUpdateService
from yahoo_finance.yahoo_provider import YahooFinanceProvider


pytestmark = [pytest.mark.integration, pytest.mark.live]


def test_real_yahoo_price_is_persisted_and_audited_in_postgres() -> None:
    database_url = os.getenv("TEST_DATABASE_URL")
    if os.getenv("RUN_LIVE_YAHOO_TESTS") != "1" or not database_url:
        pytest.skip("Live Yahoo and PostgreSQL integration is not enabled")

    import psycopg

    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            cursor.execute("DROP TABLE IF EXISTS price_update_failures, price_update_runs, prices, watchlist, history, users, companies CASCADE")
            cursor.execute(
                """
                CREATE TABLE companies (
                    id BIGSERIAL PRIMARY KEY,
                    ticker VARCHAR(32) NOT NULL,
                    company_name VARCHAR(255) NOT NULL,
                    active BOOLEAN NOT NULL
                );
                CREATE TABLE users (
                    id BIGSERIAL PRIMARY KEY,
                    mail VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL
                );
                CREATE TABLE history (
                    id BIGSERIAL PRIMARY KEY,
                    number_of_stocks INTEGER NOT NULL,
                    transaction_value NUMERIC(19, 2) NOT NULL,
                    transaction_type_enum VARCHAR(16) NOT NULL,
                    timestamp TIMESTAMPTZ NOT NULL,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    company_id BIGINT NOT NULL REFERENCES companies(id)
                );
                CREATE TABLE watchlist (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    company_id BIGINT NOT NULL REFERENCES companies(id)
                );
                CREATE TABLE prices (
                    id BIGSERIAL PRIMARY KEY,
                    ticker VARCHAR(32) NOT NULL,
                    unity_price NUMERIC(19, 2) NOT NULL,
                    timestamp TIMESTAMPTZ NOT NULL,
                    company_id BIGINT NOT NULL REFERENCES companies(id)
                );
                INSERT INTO users (mail, password) VALUES ('live@example.com', 'secret');
                INSERT INTO companies (ticker, company_name, active) VALUES ('AAPL', 'Apple Inc.', TRUE);
                INSERT INTO history (number_of_stocks, transaction_value, transaction_type_enum, timestamp, user_id, company_id)
                VALUES (1, 100, 'BUY', NOW(), 1, 1);
                """
            )
        connection.commit()

    result = PriceUpdateService(
        repository=PostgresPriceRepository(database_url),
        provider=YahooFinanceProvider(timeout_seconds=15),
        trigger="TEST_LIVE",
    ).run()

    assert result.status == "SUCCESS"
    assert result.successful_tickers == 1

    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT ticker, unity_price, source FROM prices")
            ticker, value, source = cursor.fetchone()
            assert ticker == "AAPL"
            assert value > 0
            assert source == "YAHOO_FINANCE"

            cursor.execute("SELECT status, trigger, successful_tickers, failed_tickers FROM price_update_runs")
            assert cursor.fetchone() == ("SUCCESS", "TEST_LIVE", 1, 0)
