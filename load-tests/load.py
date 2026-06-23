from locust import LoadTestShape

from common import (
    EdgarApiUser,
    PortfolioApiUser,
    active_user_classes,
    discovered_load_users,
    env_int,
    set_active_profile,
)


set_active_profile("load")


class ExpectedLoadShape(LoadTestShape):
    """Ramp to expected traffic, hold a stable plateau, then ramp down."""

    ramp_seconds = env_int("LOAD_RAMP_SECONDS", 50)
    steady_seconds = env_int("LOAD_STEADY_SECONDS", 120)
    ramp_down_seconds = env_int("LOAD_RAMP_DOWN_SECONDS", 30)
    users = discovered_load_users()
    spawn_rate = env_int("LOAD_SPAWN_RATE", 3)

    def tick(self):
        run_time = self.get_run_time()
        classes = active_user_classes()

        if run_time < self.ramp_seconds:
            target = max(1, int(self.users * run_time / max(self.ramp_seconds, 1)))
            return target, self.spawn_rate, classes
        if run_time < self.ramp_seconds + self.steady_seconds:
            return self.users, self.spawn_rate, classes
        if run_time < self.ramp_seconds + self.steady_seconds + self.ramp_down_seconds:
            elapsed = run_time - self.ramp_seconds - self.steady_seconds
            target = max(1, int(self.users * (1 - elapsed / max(self.ramp_down_seconds, 1))))
            return target, self.spawn_rate, classes
        return None
