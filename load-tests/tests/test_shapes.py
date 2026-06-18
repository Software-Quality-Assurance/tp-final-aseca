import os
import unittest
from unittest.mock import patch

from load import ExpectedLoadShape
from common import StressPortfolioApiUser, capacity_comparison_html, set_active_profile
from stress import StressPortfolioApiUser as StressModuleUser
from stress import StressShape


class ShapeTests(unittest.TestCase):
    def test_expected_load_has_ramp_plateau_and_stop(self) -> None:
        shape = ExpectedLoadShape()
        shape.ramp_seconds = 10
        shape.steady_seconds = 20
        shape.ramp_down_seconds = 10
        shape.users = 20
        shape.spawn_rate = 2

        with patch.object(shape, "get_run_time", return_value=5):
            self.assertEqual(10, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=15):
            self.assertEqual(20, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=35):
            self.assertEqual(10, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=40):
            self.assertIsNone(shape.tick())

    def test_stress_increases_users_by_stage(self) -> None:
        shape = StressShape()
        shape.stage_seconds = 10
        shape.start_users = 10
        shape.user_multiplier = 2
        shape.stages = 3

        with patch.object(shape, "get_run_time", return_value=0):
            self.assertEqual(10, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=10):
            self.assertEqual(20, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=20):
            self.assertEqual(40, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=30):
            self.assertIsNone(shape.tick())

    def test_stress_percentile_uses_stage_histogram(self) -> None:
        self.assertEqual(500, StressShape.percentile({10: 94, 500: 6}, 0.95))
        self.assertEqual(0, StressShape.percentile({}, 0.95))

    def test_edgar_user_is_opt_in(self) -> None:
        with patch.dict(os.environ, {"INCLUDE_EDGAR": "false"}):
            self.assertEqual(1, len(__import__("common").active_user_classes()))
        with patch.dict(os.environ, {"INCLUDE_EDGAR": "true"}):
            self.assertEqual(2, len(__import__("common").active_user_classes()))

    def test_stress_profile_uses_low_think_time_user_class(self) -> None:
        with patch.dict(os.environ, {"LOCUST_PROFILE": "stress"}, clear=False):
            classes = __import__("common").active_user_classes()

        self.assertIs(classes[0], StressPortfolioApiUser)

    def test_stress_module_exports_stress_user_class_for_locust_discovery(self) -> None:
        self.assertIs(StressModuleUser, StressPortfolioApiUser)

    def test_capacity_comparison_marks_active_profile(self) -> None:
        with patch.dict(
            os.environ,
            {
                "LOAD_USERS": "150",
                "LOAD_STEADY_SECONDS": "120",
                "LOAD_SPAWN_RATE": "3",
                "LOCUST_CAPACITY_API": "1 GB RAM, 1 core para API",
                "STRESS_START_USERS": "120",
                "STRESS_USER_MULTIPLIER": "1.5",
                "STRESS_STAGES": "9",
                "STRESS_STAGE_SECONDS": "60",
                "STRESS_SPAWN_RATE": "20",
            },
            clear=False,
        ):
            set_active_profile("stress")
            html = capacity_comparison_html()

        self.assertIn("Perfil activo:</strong> <span class=\"active\">stress</span>", html)
        self.assertIn("Capacidades de los servicios", html)
        self.assertIn("Load Testing (150 usuarios, 3/s)", html)
        self.assertIn("<td>150</td>", html)
        self.assertIn("<td>3075</td>", html)
        self.assertIn("9 x 60 s", html)


if __name__ == "__main__":
    unittest.main()
