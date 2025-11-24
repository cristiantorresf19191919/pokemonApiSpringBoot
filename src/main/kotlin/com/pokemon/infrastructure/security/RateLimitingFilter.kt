package com.pokemon.infrastructure.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingFilter : WebFilter {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        
        // Only apply rate limiting to GraphQL endpoint
        if (path != "/graphql") {
            return chain.filter(exchange)
        }
        
        val clientIp = getClientIp(exchange)
        val bucket = buckets.computeIfAbsent(clientIp) {
            createBucket()
        }
        
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        
        if (probe.isConsumed) {
            exchange.response.headers.add("X-RateLimit-Remaining", probe.remainingTokens.toString())
            return chain.filter(exchange)
        } else {
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.headers.add("Content-Type", "application/json")
            val retryAfterSeconds = (probe.nanosToWaitForRefill / 1_000_000_000).toInt()
            exchange.response.headers.add("X-RateLimit-Retry-After", retryAfterSeconds.toString())
            val errorResponse = """
                {
                    "errors": [{
                        "message": "Rate limit exceeded. Please try again later.",
                        "extensions": {
                            "code": "RATE_LIMIT_EXCEEDED",
                            "httpStatus": 429
                        }
                    }]
                }
            """.trimIndent()
            val buffer = exchange.response.bufferFactory().wrap(errorResponse.toByteArray())
            return exchange.response.writeWith(Mono.just(buffer))
        }
    }
    
    private fun createBucket(): Bucket {
        // 100 requests per minute per IP
        val refill = Refill.intervally(100, Duration.ofMinutes(1))
        val limit = Bandwidth.classic(100, refill)
        return Bucket.builder().addLimit(limit).build()
    }
    
    private fun getClientIp(exchange: ServerWebExchange): String {
        val xForwardedFor = exchange.request.headers.getFirst("X-Forwarded-For")
        if (xForwardedFor != null && xForwardedFor.isNotEmpty()) {
            return xForwardedFor.split(",")[0].trim()
        }
        val remoteAddress = exchange.request.remoteAddress
        return remoteAddress?.address?.hostAddress ?: "unknown"
    }
}

