package com.pokemon.infrastructure.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AuthenticationServiceImplTest {

    private val authenticationService = AuthenticationServiceImpl()

    @Test
    fun `should authenticate with correct credentials`() {
        val result = authenticationService.authenticate("admin", "admin")
        assertTrue(result)
    }

    @Test
    fun `should not authenticate with incorrect username`() {
        val result = authenticationService.authenticate("wrong", "admin")
        assertFalse(result)
    }

    @Test
    fun `should not authenticate with incorrect password`() {
        val result = authenticationService.authenticate("admin", "wrong")
        assertFalse(result)
    }

    @Test
    fun `should not authenticate with both incorrect credentials`() {
        val result = authenticationService.authenticate("user", "pass")
        assertFalse(result)
    }
}

