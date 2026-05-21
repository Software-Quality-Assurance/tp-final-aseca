package com.austral.portfolio_tracker.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        // Only skip OPTIONS and the public auth endpoints (register, login).
        // We previously skipped all /api/auth/* which prevented adding an authenticated logout endpoint.
        return request.method == "OPTIONS" || path == "/api/auth/register" || path == "/api/auth/login"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authorization.removePrefix("Bearer ").trim()
        try {
            val principal = jwtTokenService.authenticate(token)
            val authentication = UsernamePasswordAuthenticationToken(principal, token, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
        } catch (_: JwtValidationException) {
            SecurityContextHolder.clearContext()
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            response.writer.write("""{"error":"Unauthorized"}""")
        }
    }
}
