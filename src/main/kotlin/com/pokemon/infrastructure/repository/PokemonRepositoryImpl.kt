package com.pokemon.infrastructure.repository

import com.pokemon.domain.model.Pokemon
import com.pokemon.domain.repository.PokemonRepository
import com.pokemon.infrastructure.client.PokeApiClient
import com.pokemon.infrastructure.mapper.PokemonDomainMapper.toDomain
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class PokemonRepositoryImpl(private val pokeApiClient: PokeApiClient) : PokemonRepository {

    override fun findById(id: Int): Mono<Pokemon> {
        return pokeApiClient.getPokemonById(id).map { it.toDomain() }
    }

    override fun findAll(limit: Int, offset: Int): Mono<List<Pokemon>> {
        return pokeApiClient
                .getPokemonList(limit, offset)
                .flatMapMany { response ->
                    Flux.fromIterable(response.results).flatMap { item ->
                        val pokemonId = extractIdFromUrl(item.url)
                        pokeApiClient.getPokemonById(pokemonId).map { it.toDomain() }
                    }
                }
                .collectList()
    }

    override fun getTotalCount(): Mono<Int> {
        return pokeApiClient.getPokemonList(1, 0).map { it.count }
    }

    private fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/').substringAfterLast('/').toInt()
    }
}
