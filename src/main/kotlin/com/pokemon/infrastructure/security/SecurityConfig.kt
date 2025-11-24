package com.pokemon.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitingFilter: RateLimitingFilter
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/login", "/swagger-ui/**", "/api-docs/**", "/graphql-playground")
                    .permitAll()
                    .pathMatchers("/graphql")
                    .authenticated()
                    .anyExchange()
                    .permitAll()
            }
            .addFilterBefore(rateLimitingFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}

