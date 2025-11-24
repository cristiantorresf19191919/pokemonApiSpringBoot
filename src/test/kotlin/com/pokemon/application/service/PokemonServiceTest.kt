package com.pokemon.application.service

import com.pokemon.domain.model.*
import com.pokemon.domain.service.PaginationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PokemonServiceTest {

    private val pokemonCacheService = mockk<PokemonCacheService>()
    private val paginationService = mockk<PaginationService>()
    private val pokemonService = PokemonService(pokemonCacheService, paginationService)

    @Test
    fun `should get pokemon by id`() {
        val pokemon = createTestPokemon(1, "pikachu")

        every { pokemonCacheService.getDetails(1) } returns Mono.just(pokemon)

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
        val pokemon1 = createTestPokemon(1, "pikachu")
        val pokemon2 = createTestPokemon(2, "charizard")
        val indexItems =
                listOf(
                        PokemonIndexItem(1, "pikachu", "https://pokeapi.co/api/v2/pokemon/1/"),
                        PokemonIndexItem(2, "charizard", "https://pokeapi.co/api/v2/pokemon/2/")
                )
        val pageResult = PageResult(indexItems, 100)

        every { pokemonCacheService.getPage(20, 0, "number") } returns pageResult
        every { pokemonCacheService.getDetails(1) } returns Mono.just(pokemon1)
        every { pokemonCacheService.getDetails(2) } returns Mono.just(pokemon2)
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
        val pokemon1 = createTestPokemon(1, "pikachu")
        val pokemon2 = createTestPokemon(2, "charizard")
        val indexItems =
                listOf(
                        PokemonIndexItem(2, "charizard", "https://pokeapi.co/api/v2/pokemon/2/"),
                        PokemonIndexItem(1, "pikachu", "https://pokeapi.co/api/v2/pokemon/1/")
                )
        val pageResult = PageResult(indexItems, 100)

        every { pokemonCacheService.getPage(20, 0, "name") } returns pageResult
        every { pokemonCacheService.getDetails(2) } returns Mono.just(pokemon2)
        every { pokemonCacheService.getDetails(1) } returns Mono.just(pokemon1)
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
