package com.pokemon.infrastructure.service

import com.pokemon.domain.service.AuthenticationService
import com.pokemon.domain.service.JwtTokenService
import org.springframework.stereotype.Service

@Service
class AuthenticationServiceImpl(
    private val jwtTokenService: JwtTokenService
) : AuthenticationService {
    
    override fun authenticate(username: String, password: String): Boolean {
        // Only admin/admin is valid, anything else is incorrect
        return username == "admin" && password == "admin"
    }
    
    override fun generateToken(username: String): String {
        return jwtTokenService.generateToken(username)
    }
}

