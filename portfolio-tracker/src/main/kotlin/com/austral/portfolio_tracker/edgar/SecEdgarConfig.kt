package com.austral.portfolio_tracker.edgar

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class SecEdgarConfig {
    @Bean
    fun secRateLimiter(): SecRateLimiter = SecRateLimiter(requestsPerSecond = 10)

    @Bean
    fun systemClock(): Clock = Clock.systemUTC()
}
