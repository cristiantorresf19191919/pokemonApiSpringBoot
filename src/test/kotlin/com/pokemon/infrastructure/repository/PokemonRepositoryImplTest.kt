package com.pokemon.infrastructure.repository

import com.pokemon.infrastructure.client.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PokemonRepositoryImplTest {

    private val pokeApiClient = mockk<PokeApiClient>()
    private val repository = PokemonRepositoryImpl(pokeApiClient)

    @Test
    fun `should find pokemon by id`() {
        val response = createPokeApiResponse(1, "pikachu")
        
        every { pokeApiClient.getPokemonById(1) } returns Mono.just(response)

        StepVerifier.create(repository.findById(1))
            .assertNext { pokemon ->
                assertEquals(1, pokemon.id)
                assertEquals("pikachu", pokemon.name)
                assertEquals(1, pokemon.number)
            }
            .verifyComplete()
    }

    @Test
    fun `should get total count`() {
        val listResponse = PokeApiListResponse(
            count = 100,
            next = null,
            previous = null,
            results = emptyList()
        )
        
        every { pokeApiClient.getPokemonList(1, 0) } returns Mono.just(listResponse)

        StepVerifier.create(repository.getTotalCount())
            .assertNext { count ->
                assertEquals(100, count)
            }
            .verifyComplete()
    }

    private fun createPokeApiResponse(id: Int, name: String): PokeApiPokemonResponse {
        return PokeApiPokemonResponse(
            id = id,
            name = name,
            sprites = Sprites("https://example.com/$id.png"),
            abilities = emptyList(),
            moves = emptyList(),
            forms = emptyList()
        )
    }
}

