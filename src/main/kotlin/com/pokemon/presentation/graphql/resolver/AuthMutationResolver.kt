package com.pokemon.presentation.graphql.resolver

import com.pokemon.domain.service.AuthenticationService
import com.pokemon.presentation.dto.AuthPayload
import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class AuthMutationResolver(private val authenticationService: AuthenticationService) :
        GraphQLMutationResolver {

    fun login(username: String, password: String): AuthPayload {
        val isAuthenticated = authenticationService.authenticate(username, password)

        return if (isAuthenticated) {
            val token = authenticationService.generateToken(username)
            AuthPayload(success = true, token = token, message = "Login successful")
        } else {
            AuthPayload(success = false, token = null, message = "Invalid credentials")
        }
    }
}
