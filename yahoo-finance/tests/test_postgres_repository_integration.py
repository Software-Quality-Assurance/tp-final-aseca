import os
from datetime import UTC, datetime
from decimal import Decimal

import pytest

from yahoo_finance.models import MarketPrice
from yahoo_finance.postgres_repository import PostgresPriceRepository


pytestmark = pytest.mark.integration


@pytest.fixture()
def database_url() -> str:
    value = os.getenv("TEST_DATABASE_URL")
    if not value:
        pytest.skip("TEST_DATABASE_URL is not configured")
    return value


def test_finds_all_unique_active_company_tickers_and_persists_price(database_url: str) -> None:
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
                    source VARCHAR(32) NOT NULL,
                    company_id BIGINT NOT NULL REFERENCES companies(id)
                );
                """
            )
            cursor.execute(
                """
                INSERT INTO users (mail, password) VALUES ('test@example.com', 'secret');
                INSERT INTO companies (ticker, company_name, active) VALUES
                    ('AAPL', 'Apple Inc.', TRUE),
                    ('MSFT', 'Microsoft Corporation', TRUE),
                    ('OLD', 'Inactive Corp', FALSE),
                    ('SOLD', 'Sold Corp', TRUE);
                INSERT INTO history (number_of_stocks, transaction_value, transaction_type_enum, timestamp, user_id, company_id)
                SELECT 2, 200, 'BUY', NOW(), 1, id FROM companies WHERE ticker = 'AAPL';
                INSERT INTO history (number_of_stocks, transaction_value, transaction_type_enum, timestamp, user_id, company_id)
                SELECT 1, 100, 'BUY', NOW(), 1, id FROM companies WHERE ticker = 'SOLD';
                INSERT INTO history (number_of_stocks, transaction_value, transaction_type_enum, timestamp, user_id, company_id)
                SELECT 1, 100, 'SELL', NOW(), 1, id FROM companies WHERE ticker = 'SOLD';
                INSERT INTO watchlist (user_id, company_id)
                SELECT 1, id FROM companies WHERE ticker IN ('AAPL', 'MSFT', 'OLD');
                """
            )
        connection.commit()

    repository = PostgresPriceRepository(database_url)
    repository.ensure_audit_schema()
    tickers = repository.get_active_tickers()

    assert [(item.company_id, item.ticker) for item in tickers] == [
        (1, "AAPL"),
        (2, "MSFT"),
        (4, "SOLD"),
    ]

    repository.save_price(
        tickers[0],
        MarketPrice(
            ticker="AAPL",
            value=Decimal("199.99"),
            timestamp=datetime(2026, 6, 10, 15, 30, tzinfo=UTC),
            source="YAHOO_FINANCE",
        ),
    )

    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT ticker, unity_price, source FROM prices")
            assert cursor.fetchone() == ("AAPL", Decimal("199.99"), "YAHOO_FINANCE")
