package com.pokemon.domain.repository

import com.pokemon.domain.model.Pokemon
import reactor.core.publisher.Mono

interface PokemonRepository {
    fun findById(id: Int): Mono<Pokemon>
    fun findAll(limit: Int, offset: Int): Mono<List<Pokemon>>
    fun getTotalCount(): Mono<Int>
}

