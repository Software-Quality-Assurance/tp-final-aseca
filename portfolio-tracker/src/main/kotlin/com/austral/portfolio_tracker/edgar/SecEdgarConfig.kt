package com.austral.portfolio_tracker.edgar

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecEdgarConfig {
    @Bean
    fun secRateLimiter(): SecRateLimiter = SecRateLimiter(requestsPerSecond = 10)
}
