package com.pokemon.domain.model

data class PageInfo(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: String?,
    val endCursor: String?
)

data class Page<T>(
    val edges: List<Edge<T>>,
    val pageInfo: PageInfo,
    val totalCount: Int
)

data class Edge<T>(
    val node: T,
    val cursor: String
)

