from datetime import datetime

import psycopg

from yahoo_finance.models import MarketPrice, UsedTicker


class PostgresPriceRepository:
    def __init__(self, database_url: str) -> None:
        self.database_url = database_url

    def ensure_audit_schema(self) -> None:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS source VARCHAR(32);
                    UPDATE prices SET source = 'UNKNOWN' WHERE source IS NULL;
                    ALTER TABLE prices ALTER COLUMN source SET DEFAULT 'UNKNOWN';
                    ALTER TABLE prices ALTER COLUMN source SET NOT NULL;

                    CREATE TABLE IF NOT EXISTS price_update_runs (
                        id BIGSERIAL PRIMARY KEY,
                        started_at TIMESTAMPTZ NOT NULL,
                        finished_at TIMESTAMPTZ,
                        status VARCHAR(16) NOT NULL,
                        trigger VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
                        requested_tickers INTEGER NOT NULL,
                        successful_tickers INTEGER NOT NULL DEFAULT 0,
                        failed_tickers INTEGER NOT NULL DEFAULT 0
                    );

                    ALTER TABLE price_update_runs
                        ADD COLUMN IF NOT EXISTS trigger VARCHAR(32) NOT NULL DEFAULT 'MANUAL';

                    CREATE TABLE IF NOT EXISTS price_update_failures (
                        id BIGSERIAL PRIMARY KEY,
                        run_id BIGINT NOT NULL REFERENCES price_update_runs(id) ON DELETE CASCADE,
                        ticker VARCHAR(32) NOT NULL,
                        reason TEXT NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    """
                )
            connection.commit()

    def get_used_tickers(self) -> list[UsedTicker]:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT DISTINCT ON (UPPER(c.ticker))
                        c.id,
                        UPPER(c.ticker)
                    FROM companies c
                    WHERE c.active = TRUE
                      AND (
                          EXISTS (
                              SELECT 1
                              FROM watchlist w
                              WHERE w.company_id = c.id
                          )
                          OR COALESCE(
                              (
                                  SELECT SUM(
                                      CASE
                                          WHEN h.transaction_type_enum = 'BUY' THEN h.number_of_stocks
                                          WHEN h.transaction_type_enum = 'SELL' THEN -h.number_of_stocks
                                          ELSE 0
                                      END
                                  )
                                  FROM history h
                                  WHERE h.company_id = c.id
                              ),
                              0
                          ) > 0
                      )
                    ORDER BY UPPER(c.ticker), c.id
                    """
                )
                return [UsedTicker(company_id=row[0], ticker=row[1]) for row in cursor.fetchall()]

    def start_run(self, requested_tickers: int, started_at: datetime, trigger: str) -> int:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO price_update_runs (started_at, status, trigger, requested_tickers)
                    VALUES (%s, 'RUNNING', %s, %s)
                    RETURNING id
                    """,
                    (started_at, trigger, requested_tickers),
                )
                run_id = cursor.fetchone()[0]
            connection.commit()
        return run_id

    def save_price(self, company: UsedTicker, price: MarketPrice) -> None:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO prices (ticker, unity_price, timestamp, source, company_id)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    (company.ticker, price.value, price.timestamp, price.source, company.company_id),
                )
            connection.commit()

    def record_failure(self, run_id: int, ticker: str, reason: str) -> None:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO price_update_failures (run_id, ticker, reason)
                    VALUES (%s, %s, %s)
                    """,
                    (run_id, ticker, reason[:4000]),
                )
            connection.commit()

    def finish_run(
        self,
        run_id: int,
        status: str,
        successful_tickers: int,
        failed_tickers: int,
        finished_at: datetime,
    ) -> None:
        with psycopg.connect(self.database_url) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE price_update_runs
                    SET finished_at = %s,
                        status = %s,
                        successful_tickers = %s,
                        failed_tickers = %s
                    WHERE id = %s
                    """,
                    (finished_at, status, successful_tickers, failed_tickers, run_id),
                )
            connection.commit()
