package com.pokemon.infrastructure.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaginationServiceImplTest {

    private val paginationService = PaginationServiceImpl()

    @Test
    fun `should encode and decode cursor correctly`() {
        val offset = 10
        val cursor = paginationService.encodeCursor(offset)
        val decoded = paginationService.decodeCursor(cursor)
        
        assertEquals(offset, decoded)
    }

    @Test
    fun `should return null for invalid cursor`() {
        val decoded = paginationService.decodeCursor("invalid-cursor")
        assertNull(decoded)
    }

    @Test
    fun `should return null for null cursor`() {
        val decoded = paginationService.decodeCursor(null)
        assertNull(decoded)
    }

    @Test
    fun `should handle zero offset`() {
        val cursor = paginationService.encodeCursor(0)
        val decoded = paginationService.decodeCursor(cursor)
        assertEquals(0, decoded)
    }
}

