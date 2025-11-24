package com.pokemon.domain.service

interface PaginationService {
    fun encodeCursor(offset: Int): String
    fun decodeCursor(cursor: String?): Int?
}

