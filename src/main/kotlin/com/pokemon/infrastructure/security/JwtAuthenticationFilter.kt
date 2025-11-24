package com.pokemon.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokemon.domain.service.JwtTokenService
import java.nio.charset.StandardCharsets
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(private val jwtTokenService: JwtTokenService) : WebFilter {

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

        // Only protect GraphQL Pokemon endpoints
        if (path == "/graphql") {
            val method = exchange.request.method.name()

            // Check for GET requests with introspection query parameter (Apollo Client schema
            // fetching)
            if (method == "GET") {
                val queryParam = exchange.request.queryParams.getFirst("query") ?: ""
                val isIntrospection = isIntrospectionQuery(queryParam)

                if (isIntrospection) {
                    // Allow introspection queries via GET without authentication
                    return chain.filter(exchange)
                } else {
                    // For non-introspection GET requests, check authentication
                    val token = extractToken(exchange)

                    if (token == null || !jwtTokenService.validateToken(token)) {
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                        exchange.response.headers.add("Content-Type", "application/json")
                        val errorResponse =
                                """
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
                        val buffer =
                                exchange.response
                                        .bufferFactory()
                                        .wrap(errorResponse.toByteArray())
                        return exchange.response.writeWith(Mono.just(buffer)).then()
                    } else {
                        val username = jwtTokenService.getUsernameFromToken(token)
                        if (username != null) {
                            val authentication =
                                    UsernamePasswordAuthenticationToken(
                                            username,
                                            null,
                                            listOf(SimpleGrantedAuthority("ROLE_USER"))
                                    )

                            return chain.filter(exchange)
                                    .contextWrite(
                                            ReactiveSecurityContextHolder
                                                    .withAuthentication(authentication)
                                    )
                        } else {
                            return chain.filter(exchange)
                        }
                    }
                }
            }

            // For POST requests, check the body for introspection queries
            return DataBufferUtils.join(exchange.request.body)
                    .flatMap { dataBuffer ->
                        val bodyBytes = ByteArray(dataBuffer.readableByteCount())
                        dataBuffer.read(bodyBytes)
                        DataBufferUtils.release(dataBuffer)
                        val body = String(bodyBytes, StandardCharsets.UTF_8)

                        // Parse JSON body to extract the query field
                        var queryString = body
                        try {
                            val mapper = ObjectMapper()
                            val jsonNode = mapper.readTree(body)
                            if (jsonNode.has("query") && jsonNode.get("query").isTextual) {
                                queryString = jsonNode.get("query").asText()
                            }
                        } catch (e: Exception) {
                            // If JSON parsing fails, use the raw body string
                        }

                        // Detect introspection queries (used by Apollo Client and other tools)
                        val isIntrospection = isIntrospectionQuery(queryString)

                        // Create a cached body request decorator
                        val cachedRequest =
                                object : ServerHttpRequestDecorator(exchange.request) {
                                    override fun getBody(): Flux<DataBuffer> {
                                        return Flux.just(
                                                exchange.response.bufferFactory().wrap(bodyBytes)
                                        )
                                    }
                                }
                        val cachedExchange = exchange.mutate().request(cachedRequest).build()

                        if (isIntrospection) {
                            // Allow introspection queries to pass through without authentication
                            chain.filter(cachedExchange)
                        } else {
                            // Check authentication for regular queries
                            val token = extractToken(exchange)

                            if (token == null || !jwtTokenService.validateToken(token)) {
                                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                                exchange.response.headers.add("Content-Type", "application/json")
                                val errorResponse =
                                        """
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
                                val buffer =
                                        exchange.response
                                                .bufferFactory()
                                                .wrap(errorResponse.toByteArray())
                                exchange.response.writeWith(Mono.just(buffer))
                            } else {
                                val username = jwtTokenService.getUsernameFromToken(token)
                                if (username != null) {
                                    val authentication =
                                            UsernamePasswordAuthenticationToken(
                                                    username,
                                                    null,
                                                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                                            )

                                    chain.filter(cachedExchange)
                                            .contextWrite(
                                                    ReactiveSecurityContextHolder
                                                            .withAuthentication(authentication)
                                            )
                                } else {
                                    chain.filter(cachedExchange)
                                }
                            }
                        }
                    }
                    .switchIfEmpty(
                            // If body is empty, check token normally
                            Mono.defer {
                                val token = extractToken(exchange)
                                if (token == null || !jwtTokenService.validateToken(token)) {
                                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                                    exchange.response.headers.add(
                                            "Content-Type",
                                            "application/json"
                                    )
                                    val errorResponse =
                                            """
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
                                    val buffer =
                                            exchange.response
                                                    .bufferFactory()
                                                    .wrap(errorResponse.toByteArray())
                                    exchange.response.writeWith(Mono.just(buffer))
                                } else {
                                    val username = jwtTokenService.getUsernameFromToken(token)
                                    if (username != null) {
                                        val authentication =
                                                UsernamePasswordAuthenticationToken(
                                                        username,
                                                        null,
                                                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                                                )
                                        chain.filter(exchange)
                                                .contextWrite(
                                                        ReactiveSecurityContextHolder
                                                                .withAuthentication(authentication)
                                                )
                                    } else {
                                        chain.filter(exchange)
                                    }
                                }
                            }
                    )
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

    /**
     * Detects if a GraphQL query is an introspection query (used by Apollo Client and other tools
     * to fetch the schema).
     *
     * Apollo Client sends queries like:
     * - query IntrospectionQuery { __schema { ... } }
     * - query { __schema { ... } }
     * - query { __type(name: "...") { ... } }
     */
    private fun isIntrospectionQuery(query: String): Boolean {
        if (query.isBlank()) {
            return false
        }

        val normalizedQuery = query.replace("\\s+".toRegex(), " ").trim()

        return normalizedQuery.contains("__schema") ||
                normalizedQuery.contains("__type") ||
                normalizedQuery.contains("IntrospectionQuery") ||
                normalizedQuery.matches(
                        Regex(".*query\\s+.*__schema.*", RegexOption.DOT_MATCHES_ALL)
                ) ||
                normalizedQuery.matches(
                        Regex(".*query\\s+.*__type.*", RegexOption.DOT_MATCHES_ALL)
                ) ||
                normalizedQuery.contains("query IntrospectionQuery") ||
                normalizedQuery.contains("query{__schema") ||
                normalizedQuery.contains("query { __schema")
    }
}
