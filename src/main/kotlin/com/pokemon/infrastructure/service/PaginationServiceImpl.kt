package com.pokemon.infrastructure.service

import com.pokemon.domain.service.PaginationService
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class PaginationServiceImpl : PaginationService {
    override fun encodeCursor(offset: Int): String {
        return Base64.getEncoder().encodeToString(offset.toString().toByteArray())
    }

    override fun decodeCursor(cursor: String?): Int? {
        return cursor?.let {
            try {
                String(Base64.getDecoder().decode(it)).toInt()
            } catch (e: Exception) {
                null
            }
        }
    }
}

