import os
import unittest
from unittest.mock import patch

from load import ExpectedLoadShape
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
        shape.step_users = 20
        shape.stages = 3

        with patch.object(shape, "get_run_time", return_value=0):
            self.assertEqual(10, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=10):
            self.assertEqual(30, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=20):
            self.assertEqual(50, shape.tick()[0])
        with patch.object(shape, "get_run_time", return_value=30):
            self.assertIsNone(shape.tick())

    def test_edgar_user_is_opt_in(self) -> None:
        with patch.dict(os.environ, {"INCLUDE_EDGAR": "false"}):
            self.assertEqual(1, len(__import__("common").active_user_classes()))
        with patch.dict(os.environ, {"INCLUDE_EDGAR": "true"}):
            self.assertEqual(2, len(__import__("common").active_user_classes()))


if __name__ == "__main__":
    unittest.main()
