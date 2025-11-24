package com.pokemon.application.service

import com.pokemon.domain.model.*
import com.pokemon.domain.repository.PokemonRepository
import com.pokemon.domain.service.PaginationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PokemonServiceTest {

    private val pokemonRepository = mockk<PokemonRepository>()
    private val paginationService = mockk<PaginationService>()
    private val pokemonService = PokemonService(pokemonRepository, paginationService)

    @Test
    fun `should get pokemon by id`() {
        val pokemon = createTestPokemon(1, "pikachu")
        
        every { pokemonRepository.findById(1) } returns Mono.just(pokemon)

        StepVerifier.create(pokemonService.getPokemonById(1))
            .assertNext { dto ->
                assertEquals(1, dto.id)
                assertEquals("pikachu", dto.name)
                assertEquals(1, dto.number)
            }
            .verifyComplete()
    }

    @Test
    fun `should get paginated pokemons with default values`() {
        val pokemons = listOf(
            createTestPokemon(1, "pikachu"),
            createTestPokemon(2, "charizard")
        )
        
        every { pokemonRepository.findAll(20, 0) } returns Mono.just(pokemons)
        every { pokemonRepository.getTotalCount() } returns Mono.just(100)
        every { paginationService.decodeCursor(null) } returns null
        every { paginationService.encodeCursor(0) } returns "cursor0"
        every { paginationService.encodeCursor(1) } returns "cursor1"

        StepVerifier.create(pokemonService.getPokemons(null, null, null))
            .assertNext { page ->
                assertEquals(2, page.edges.size)
                assertEquals(100, page.totalCount)
                assertTrue(page.pageInfo.hasNextPage)
                assertFalse(page.pageInfo.hasPreviousPage)
            }
            .verifyComplete()
    }

    @Test
    fun `should sort pokemons by name`() {
        val pokemons = listOf(
            createTestPokemon(2, "charizard"),
            createTestPokemon(1, "pikachu")
        )
        
        every { pokemonRepository.findAll(20, 0) } returns Mono.just(pokemons)
        every { pokemonRepository.getTotalCount() } returns Mono.just(100)
        every { paginationService.decodeCursor(null) } returns null
        every { paginationService.encodeCursor(0) } returns "cursor0"
        every { paginationService.encodeCursor(1) } returns "cursor1"

        StepVerifier.create(pokemonService.getPokemons(null, null, "name"))
            .assertNext { page ->
                assertEquals("charizard", page.edges[0].node.name)
                assertEquals("pikachu", page.edges[1].node.name)
            }
            .verifyComplete()
    }

    private fun createTestPokemon(id: Int, name: String): Pokemon {
        return Pokemon(
            id = id,
            name = name,
            number = id,
            imageUrl = "https://example.com/$id.png",
            abilities = emptyList(),
            moves = emptyList(),
            forms = emptyList()
        )
    }
}

