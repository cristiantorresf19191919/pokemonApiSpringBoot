package com.pokemon.domain.service

interface JwtTokenService {
    fun generateToken(username: String): String
    fun validateToken(token: String): Boolean
    fun getUsernameFromToken(token: String): String?
}

