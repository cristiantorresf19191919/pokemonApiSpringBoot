package com.pokemon.presentation.graphql.resolver

import graphql.kickstart.tools.GraphQLQueryResolver
import com.pokemon.application.dto.PokemonDTO
import com.pokemon.application.dto.PokemonPreviewDTO
import com.pokemon.application.service.PokemonService
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class PokemonQueryResolver(
    private val pokemonService: PokemonService
) : GraphQLQueryResolver {

    fun pokemon(id: Int): CompletableFuture<PokemonDTO> {
        return pokemonService.getPokemonById(id)
            .toFuture()
    }

    fun pokemons(first: Int?, after: String?, sortBy: String?): CompletableFuture<com.pokemon.application.dto.PageDTO<PokemonDTO>> {
        return pokemonService.getPokemons(first, after, sortBy)
            .toFuture()
    }

    fun searchPokemon(query: String): CompletableFuture<List<PokemonPreviewDTO>> {
        return pokemonService.searchPokemon(query)
            .collectList()
            .toFuture()
    }
}

