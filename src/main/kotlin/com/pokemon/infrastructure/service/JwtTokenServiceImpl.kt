package com.pokemon.infrastructure.service

import com.pokemon.domain.service.JwtTokenService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtTokenServiceImpl(
    @Value("\${jwt.secret:PokemonBackendSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong}")
    private val secret: String,
    @Value("\${jwt.expiration:86400000}") // 24 hours default
    private val expiration: Long
) : JwtTokenService {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    override fun generateToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    override fun getUsernameFromToken(token: String): String? {
        return try {
            val claims = getClaimsFromToken(token)
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}

