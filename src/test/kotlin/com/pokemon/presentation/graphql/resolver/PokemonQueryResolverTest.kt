package com.pokemon.presentation.graphql.resolver

import com.pokemon.application.dto.PokemonDTO
import com.pokemon.application.service.PokemonService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

class PokemonQueryResolverTest {

    private val pokemonService = mockk<PokemonService>()
    private val resolver = PokemonQueryResolver(pokemonService)

    @Test
    fun `should resolve pokemon query`() {
        val pokemonDTO = PokemonDTO(
            id = 1,
            name = "pikachu",
            number = 1,
            imageUrl = "https://example.com/1.png",
            abilities = emptyList(),
            moves = emptyList(),
            forms = emptyList()
        )
        
        every { pokemonService.getPokemonById(1) } returns Mono.just(pokemonDTO)

        val result = resolver.pokemon(1).get()
        
        assertEquals(1, result.id)
        assertEquals("pikachu", result.name)
    }
}

