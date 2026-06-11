FROM python:3.12-slim

WORKDIR /app

COPY yahoo-finance/pyproject.toml ./pyproject.toml
COPY yahoo-finance/src ./src

RUN pip install --no-cache-dir .

CMD ["python", "-m", "yahoo_finance"]
