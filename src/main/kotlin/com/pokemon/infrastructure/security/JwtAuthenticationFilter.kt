package com.pokemon.infrastructure.security

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()

        // Skip authentication for login endpoint and Swagger/OpenAPI endpoints
        if (path.startsWith("/api/login") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/api-docs") ||
                        path.startsWith("/graphql-playground")
        ) {
            return chain.filter(exchange)
        }

        // Skip GraphQL endpoints - no authentication required
        if (path == "/graphql") {
            return chain.filter(exchange)
        }

        return chain.filter(exchange)
    }
}
