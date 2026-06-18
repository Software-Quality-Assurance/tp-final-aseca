import json
import logging
from pathlib import Path

from locust import LoadTestShape

from common import (
    CAPACITY_RESULT_PATH,
    EdgarApiUser,
    StressPortfolioApiUser,
    active_user_classes,
    env_float,
    env_int,
    set_active_profile,
)


set_active_profile("stress")


class StressShape(LoadTestShape):
    """Increase concurrency until the current stage violates the stress SLO."""

    stage_seconds = env_int("STRESS_STAGE_SECONDS", 60)
    start_users = env_int("STRESS_START_USERS", 120)
    user_multiplier = env_float("STRESS_USER_MULTIPLIER", 1.5)
    stages = env_int("STRESS_STAGES", 9)
    spawn_rate = env_int("STRESS_SPAWN_RATE", 20)
    max_failure_ratio = env_float("STRESS_MAX_FAILURE_RATIO", 0.01)
    max_p95_ms = env_int("STRESS_MAX_P95_MS", 500)
    min_stage_requests = env_int("STRESS_MIN_STAGE_REQUESTS", 100)

    def __init__(self):
        super().__init__()
        self._observed_stage = 0
        self._snapshot_requests = 0
        self._snapshot_failures = 0
        self._snapshot_response_times: dict[int, int] = {}
        self._last_stable_users = 0
        self._capacity_recorded = False

    def stage_users(self, stage: int) -> int:
        return max(self.start_users, int(round(self.start_users * (self.user_multiplier**stage))))

    @staticmethod
    def percentile(response_times: dict[int, int], percentile: float) -> int:
        total = sum(response_times.values())
        if total <= 0:
            return 0
        threshold = max(1, int(total * percentile + 0.999999))
        seen = 0
        for response_time, count in sorted(response_times.items()):
            seen += count
            if seen >= threshold:
                return response_time
        return max(response_times)

    def _stage_metrics(self) -> tuple[int, int, float, int]:
        stats = self.runner.environment.stats.total
        requests = max(0, stats.num_requests - self._snapshot_requests)
        failures = max(0, stats.num_failures - self._snapshot_failures)
        response_times = {
            response_time: max(0, count - self._snapshot_response_times.get(response_time, 0))
            for response_time, count in stats.response_times.items()
        }
        response_times = {key: value for key, value in response_times.items() if value > 0}
        failure_ratio = failures / requests if requests else 0.0
        return requests, failures, failure_ratio, self.percentile(response_times, 0.95)

    def _take_snapshot(self) -> None:
        stats = self.runner.environment.stats.total
        self._snapshot_requests = stats.num_requests
        self._snapshot_failures = stats.num_failures
        self._snapshot_response_times = dict(stats.response_times)

    def _record_capacity(
        self,
        failed_users: int,
        requests: int,
        failures: int,
        failure_ratio: float,
        p95: int,
        reason: str,
    ) -> None:
        result = {
            "last_stable_users": self._last_stable_users,
            "failed_users": failed_users,
            "stage_requests": requests,
            "stage_failures": failures,
            "stage_failure_ratio": failure_ratio,
            "stage_p95_ms": p95,
            "reason": reason,
            "recommended_load_users": max(
                1,
                int(self._last_stable_users * env_float("LOAD_CAPACITY_FACTOR", 0.8)),
            ),
        }
        path = Path(CAPACITY_RESULT_PATH)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(result, indent=2), encoding="utf-8")
        logging.error("Stress capacity exceeded: %s", result)

        self.runner.environment.events.request.fire(
            request_type="SLO",
            name="[stress] capacity exceeded",
            response_time=p95,
            response_length=0,
            exception=RuntimeError(reason),
            context=result,
        )
        self.runner.environment.process_exit_code = 1
        self._capacity_recorded = True

    def _evaluate_completed_stage(self, stage: int) -> bool:
        requests, failures, failure_ratio, p95 = self._stage_metrics()
        users = self.stage_users(stage)
        if requests < self.min_stage_requests:
            return False

        reasons = []
        if failure_ratio > self.max_failure_ratio:
            reasons.append(f"failure ratio {failure_ratio:.2%} > {self.max_failure_ratio:.2%}")
        if p95 > self.max_p95_ms:
            reasons.append(f"p95 {p95} ms > {self.max_p95_ms} ms")

        if reasons:
            self._record_capacity(users, requests, failures, failure_ratio, p95, "; ".join(reasons))
            return True

        self._last_stable_users = users
        return False

    def tick(self):
        stage = int(self.get_run_time() // max(self.stage_seconds, 1))
        if self.runner is None:
            if stage >= self.stages:
                return None
            return self.stage_users(stage), self.spawn_rate, active_user_classes()

        while self._observed_stage < min(stage, self.stages):
            if self._evaluate_completed_stage(self._observed_stage):
                return None
            self._observed_stage += 1
            self._take_snapshot()

        if stage >= self.stages:
            if not self._capacity_recorded:
                requests, failures, failure_ratio, p95 = self._stage_metrics()
                users = self.stage_users(self.stages - 1)
                reason = f"capacity ceiling reached at {users} users without violating the configured SLO"
                self._record_capacity(users, requests, failures, failure_ratio, p95, reason)
            return None
        return self.stage_users(stage), self.spawn_rate, active_user_classes()
