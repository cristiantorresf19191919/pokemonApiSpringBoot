package com.pokemon.application.dto

data class PageDTO<T>(
    val edges: List<EdgeDTO<T>>,
    val pageInfo: PageInfoDTO,
    val totalCount: Int
)

data class EdgeDTO<T>(
    val node: T,
    val cursor: String
)

data class PageInfoDTO(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: String?,
    val endCursor: String?
)

