import unittest
from unittest.mock import patch

from common import AuthenticatedApiUser, RequestBudget, configured_tickers, edgar_budget_rps


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


if __name__ == "__main__":
    unittest.main()
