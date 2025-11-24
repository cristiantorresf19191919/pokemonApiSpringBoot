package com.pokemon.presentation.graphql.resolver

import com.pokemon.domain.service.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AuthMutationResolverTest {

    private val authenticationService = mockk<AuthenticationService>()
    private val resolver = AuthMutationResolver(authenticationService)

    @Test
    fun `should return success for valid credentials`() {
        every { authenticationService.authenticate("admin", "admin") } returns true
        every { authenticationService.generateToken("admin") } returns "test-token"

        val result = resolver.login("admin", "admin")

        assertTrue(result.success)
        assertNotNull(result.token)
        assertEquals("test-token", result.token)
        assertEquals("Login successful", result.message)
    }

    @Test
    fun `should return failure for invalid credentials`() {
        every { authenticationService.authenticate("wrong", "wrong") } returns false

        val result = resolver.login("wrong", "wrong")

        assertFalse(result.success)
        assertNull(result.token)
        assertEquals("Invalid credentials", result.message)
    }
}
