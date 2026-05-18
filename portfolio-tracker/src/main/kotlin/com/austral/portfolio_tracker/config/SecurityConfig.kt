package com.austral.portfolio_tracker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // For the purpose of these controller tests we permit anonymous registration
        // and disable CSRF so MockMvc POST requests (without CSRF token) succeed.
        http.csrf { it.disable() }

        http.authorizeHttpRequests { authz ->
            authz.requestMatchers("/api/auth/register").permitAll()
            authz.anyRequest().authenticated()
        }

        return http.build()
    }
}


