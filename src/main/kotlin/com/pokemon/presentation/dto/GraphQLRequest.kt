package com.pokemon.presentation.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any>? = null,
    @JsonProperty("operationName")
    val operationName: String? = null
)

