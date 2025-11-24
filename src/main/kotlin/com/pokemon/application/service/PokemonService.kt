package com.pokemon.application.service

import com.pokemon.application.dto.PageDTO
import com.pokemon.application.dto.PokemonDTO
import com.pokemon.application.mapper.PokemonMapper
import com.pokemon.domain.model.Page
import com.pokemon.domain.model.Pokemon
import com.pokemon.domain.service.PaginationService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class PokemonService(
        private val pokemonCacheService: PokemonCacheService,
        private val paginationService: PaginationService
) {
        fun getPokemonById(id: Int): Mono<PokemonDTO> {
                return pokemonCacheService.getDetails(id).map { PokemonMapper.toDTO(it) }
        }

        fun getPokemons(first: Int?, after: String?, sortBy: String?): Mono<PageDTO<PokemonDTO>> {
                val limit = first ?: 20
                val offset = paginationService.decodeCursor(after) ?: 0
                val sortField = sortBy ?: "number"

                // 1. Get the slice from memory (Synchronous)
                val pageResult = pokemonCacheService.getPage(limit, offset, sortField)

                // 2. Hydrate the data (Fetch details for these items only)
                // This runs the calls in parallel for better performance
                return Flux.fromIterable(pageResult.items.withIndex())
                        .flatMap { indexedValue ->
                                val index = indexedValue.index
                                val indexItem = indexedValue.value
                                pokemonCacheService.getDetails(indexItem.id).map { pokemon: Pokemon
                                        ->
                                        Pair(index, pokemon)
                                }
                        }
                        .collectList()
                        .map { indexedDetailsList: List<Pair<Int, Pokemon>> ->
                                // Sort by original index to preserve order
                                val sortedDetails =
                                        indexedDetailsList.sortedBy { it.first }.map { it.second }
                                // 3. Construct the Cursor-based Page
                                val edges =
                                        sortedDetails.mapIndexed { idx, pokemon ->
                                                com.pokemon.domain.model.Edge(
                                                        node = pokemon,
                                                        cursor =
                                                                paginationService.encodeCursor(
                                                                        offset + idx
                                                                )
                                                )
                                        }

                                val hasNextPage = offset + limit < pageResult.totalCount
                                val hasPreviousPage = offset > 0

                                val page =
                                        Page(
                                                edges = edges,
                                                pageInfo =
                                                        com.pokemon.domain.model.PageInfo(
                                                                hasNextPage = hasNextPage,
                                                                hasPreviousPage = hasPreviousPage,
                                                                startCursor =
                                                                        edges.firstOrNull()?.cursor,
                                                                endCursor =
                                                                        edges.lastOrNull()?.cursor
                                                        ),
                                                totalCount = pageResult.totalCount
                                        )

                                PokemonMapper.toDTO(page)
                        }
        }
}
