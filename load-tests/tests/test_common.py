import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock
from unittest.mock import patch

from common import (
    AuthenticatedApiUser,
    RequestBudget,
    configured_tickers,
    discovered_load_users,
    edgar_budget_rps,
    request_timeout_seconds,
)


class RequestBudgetTests(unittest.TestCase):
    def test_spaces_requests_according_to_cost(self) -> None:
        now = 0.0
        sleeps = []

        def clock() -> float:
            return now

        def sleeper(seconds: float) -> None:
            nonlocal now
            sleeps.append(seconds)
            now += seconds

        budget = RequestBudget(8.0, clock=clock, sleeper=sleeper)

        budget.acquire(cost=2)
        budget.acquire(cost=1)
        budget.acquire(cost=1)

        self.assertEqual([0.25, 0.125], sleeps)
        self.assertAlmostEqual(0.375, sum(sleeps))

    def test_divides_budget_across_workers(self) -> None:
        now = 0.0
        sleeps = []

        def clock() -> float:
            return now

        def sleeper(seconds: float) -> None:
            nonlocal now
            sleeps.append(seconds)
            now += seconds

        budget = RequestBudget(8.0, worker_count=2, clock=clock, sleeper=sleeper)

        budget.acquire()
        budget.acquire()

        self.assertEqual([0.25], sleeps)

    def test_rejects_invalid_configuration(self) -> None:
        with self.assertRaises(ValueError):
            RequestBudget(0)
        with self.assertRaises(ValueError):
            RequestBudget(8, worker_count=0)
        with self.assertRaises(ValueError):
            RequestBudget(8).acquire(cost=0)

    def test_rejects_edgar_budget_above_official_limit(self) -> None:
        with patch.dict("os.environ", {"EDGAR_MAX_REQUESTS_PER_SECOND": "10"}):
            self.assertEqual(10.0, edgar_budget_rps())
        with patch.dict("os.environ", {"EDGAR_MAX_REQUESTS_PER_SECOND": "10.1"}):
            with self.assertRaises(ValueError):
                edgar_budget_rps()

    def test_requires_exactly_two_configured_tickers(self) -> None:
        with patch.dict("os.environ", {"LOCUST_TICKERS": "ACLS, ACU"}):
            self.assertEqual(("ACLS", "ACU"), configured_tickers())
        with patch.dict("os.environ", {"LOCUST_TICKERS": "ACLS"}):
            with self.assertRaises(ValueError):
                configured_tickers()


class AccountSetupTests(unittest.TestCase):
    def setUp(self) -> None:
        AuthenticatedApiUser._registered_accounts.clear()
        AuthenticatedApiUser._prepared_accounts.clear()

    def test_account_setup_state_is_shared_between_user_classes(self) -> None:
        AuthenticatedApiUser._registered_accounts.add("load@example.com")
        AuthenticatedApiUser._prepared_accounts.add("load@example.com")

        self.assertIn("load@example.com", AuthenticatedApiUser._registered_accounts)
        self.assertIn("load@example.com", AuthenticatedApiUser._prepared_accounts)

    def test_uses_profile_specific_request_timeouts(self) -> None:
        with patch.dict("os.environ", {"LOCUST_PROFILE": "load", "LOAD_REQUEST_TIMEOUT_SECONDS": "5"}, clear=False):
            self.assertEqual(5.0, request_timeout_seconds())
        with patch.dict("os.environ", {"LOCUST_PROFILE": "stress", "STRESS_REQUEST_TIMEOUT_SECONDS": "2"}, clear=False):
            self.assertEqual(2.0, request_timeout_seconds())

    def test_api_helpers_forward_timeout(self) -> None:
        user = object.__new__(AuthenticatedApiUser)
        user.client = Mock()

        with patch.dict("os.environ", {"LOCUST_PROFILE": "stress", "STRESS_REQUEST_TIMEOUT_SECONDS": "2"}, clear=False):
            user.api_get("/api/portfolio", name="/api/portfolio")
            user.api_post("/api/auth/login", json={"email": "a", "password": "b"})

        user.client.get.assert_called_once_with("/api/portfolio", name="/api/portfolio", timeout=2.0)
        user.client.post.assert_called_once_with("/api/auth/login", json={"email": "a", "password": "b"}, timeout=2.0)

    def test_load_users_are_derived_from_discovered_stable_capacity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result_path = Path(directory) / "capacity.json"
            result_path.write_text(json.dumps({"last_stable_users": 500}), encoding="utf-8")
            with (
                patch("common.CAPACITY_RESULT_PATH", result_path),
                patch.dict(
                    "os.environ",
                    {"LOAD_USERS": "", "LOAD_CAPACITY_FACTOR": "0.8"},
                    clear=False,
                ),
            ):
                self.assertEqual(400, discovered_load_users())


if __name__ == "__main__":
    unittest.main()
