package com.pokemon.presentation.dto

data class AuthPayload(
    val success: Boolean,
    val token: String?,
    val message: String?
)

