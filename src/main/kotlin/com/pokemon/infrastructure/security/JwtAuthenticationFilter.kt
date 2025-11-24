package com.pokemon.infrastructure.security

import com.pokemon.domain.service.JwtTokenService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        
        // Skip authentication for login endpoint and Swagger/OpenAPI endpoints
        if (path.startsWith("/api/login") || 
            path.startsWith("/swagger-ui") || 
            path.startsWith("/api-docs") ||
            path.startsWith("/graphql-playground")) {
            return chain.filter(exchange)
        }
        
        // Only protect GraphQL Pokemon endpoints
        if (path == "/graphql") {
            val token = extractToken(exchange)
            
            if (token == null || !jwtTokenService.validateToken(token)) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                exchange.response.headers.add("Content-Type", "application/json")
                val errorResponse = """
                    {
                        "errors": [{
                            "message": "Unauthorized: Invalid or missing token",
                            "extensions": {
                                "code": "UNAUTHENTICATED",
                                "httpStatus": 401
                            }
                        }]
                    }
                """.trimIndent()
                val buffer = exchange.response.bufferFactory().wrap(errorResponse.toByteArray())
                return exchange.response.writeWith(Mono.just(buffer))
            }
            
            val username = jwtTokenService.getUsernameFromToken(token)
            if (username != null) {
                val authentication = UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            }
        }
        
        return chain.filter(exchange)
    }
    
    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }
        
        // Also check for token in query parameter (for GraphQL Playground)
        return exchange.request.queryParams.getFirst("token")
    }
}

