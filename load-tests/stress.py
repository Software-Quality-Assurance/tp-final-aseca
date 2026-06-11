from locust import LoadTestShape

from common import EdgarApiUser, PortfolioApiUser, active_user_classes, env_int


class StressShape(LoadTestShape):
    """Increase concurrency in steps until the configured ceiling is reached."""

    stage_seconds = env_int("STRESS_STAGE_SECONDS", 120)
    start_users = env_int("STRESS_START_USERS", 10)
    step_users = env_int("STRESS_STEP_USERS", 20)
    stages = env_int("STRESS_STAGES", 5)
    spawn_rate = env_int("STRESS_SPAWN_RATE", 10)

    def tick(self):
        stage = int(self.get_run_time() // max(self.stage_seconds, 1))
        if stage >= self.stages:
            return None
        users = self.start_users + (stage * self.step_users)
        return users, self.spawn_rate, active_user_classes()
