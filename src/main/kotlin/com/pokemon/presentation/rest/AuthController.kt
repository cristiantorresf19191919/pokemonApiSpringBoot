package com.pokemon.presentation.rest

import com.pokemon.domain.service.AuthenticationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Authentication endpoints for user login")
class AuthController(private val authenticationService: AuthenticationService) {

        @Operation(
                summary = "User login",
                description =
                        "Authenticate user with username and password. Returns a token on success."
        )
        @ApiResponses(
                value =
                        [
                                ApiResponse(
                                        responseCode = "200",
                                        description = "Login successful",
                                        content =
                                                [
                                                        Content(
                                                                schema =
                                                                        Schema(
                                                                                implementation =
                                                                                        LoginResponse::class
                                                                        )
                                                        )]
                                ),
                                ApiResponse(
                                        responseCode = "401",
                                        description = "Invalid credentials",
                                        content =
                                                [
                                                        Content(
                                                                schema =
                                                                        Schema(
                                                                                implementation =
                                                                                        LoginResponse::class
                                                                        )
                                                        )]
                                )]
        )
        @PostMapping("/login")
        fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
                val isAuthenticated =
                        authenticationService.authenticate(request.username, request.password)

                return if (isAuthenticated) {
                        val token = authenticationService.generateToken(request.username)
                        ResponseEntity.ok(
                                LoginResponse(
                                        success = true,
                                        token = token,
                                        message = "Login successful"
                                )
                        )
                } else {
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(
                                        LoginResponse(
                                                success = false,
                                                token = null,
                                                message = "Invalid credentials"
                                        )
                                )
                }
        }
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val success: Boolean, val token: String?, val message: String?)
