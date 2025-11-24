package com.pokemon.domain.service

interface AuthenticationService {
    fun authenticate(username: String, password: String): Boolean
    fun generateToken(username: String): String
}

