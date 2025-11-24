package com.pokemon.infrastructure.service

import com.pokemon.domain.service.JwtTokenService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AuthenticationServiceImplTest {

    private val jwtTokenService = mockk<JwtTokenService>()
    private val authenticationService = AuthenticationServiceImpl(jwtTokenService)

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

    @Test
    fun `should generate token`() {
        every { jwtTokenService.generateToken("admin") } returns "test-token"

        val token = authenticationService.generateToken("admin")
        assertEquals("test-token", token)
    }
}
