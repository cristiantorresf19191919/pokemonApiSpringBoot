package com.pokemon.presentation.rest

import com.pokemon.domain.service.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthControllerTest {

    private val authenticationService = mockk<AuthenticationService>()
    private val controller = AuthController(authenticationService)

    @Test
    fun `should return success for valid credentials`() {
        every { authenticationService.authenticate("admin", "admin") } returns true

        val request = LoginRequest("admin", "admin")
        val response = controller.login(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body?.success == true)
        assertNotNull(response.body?.token)
    }

    @Test
    fun `should return unauthorized for invalid credentials`() {
        every { authenticationService.authenticate("wrong", "wrong") } returns false

        val request = LoginRequest("wrong", "wrong")
        val response = controller.login(request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertFalse(response.body?.success == true)
        assertNull(response.body?.token)
    }
}

