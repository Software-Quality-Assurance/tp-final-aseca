package com.austral.portfolio_tracker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // Keep CSRF enabled globally, but allow unauthenticated registration
        // without requiring a CSRF token.
        http.csrf { csrf ->
            csrf.ignoringRequestMatchers("/api/auth/register")
        }

        http.authorizeHttpRequests { authz ->
            authz.requestMatchers("/api/auth/register").permitAll()
            authz.anyRequest().authenticated()
        }

        return http.build()
    }
}
