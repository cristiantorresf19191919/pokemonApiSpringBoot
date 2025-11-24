package com.pokemon.application.service

import com.pokemon.application.dto.PageDTO
import com.pokemon.application.dto.PokemonDTO
import com.pokemon.application.mapper.PokemonMapper
import com.pokemon.domain.model.Page
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
        // This runs the calls in parallel
        return Flux.fromIterable(pageResult.items)
                .flatMapSequential { indexItem -> pokemonCacheService.getDetails(indexItem.id) }
                .collectList()
                .map { detailsList ->
                    // 3. Construct the Cursor-based Page
                    val edges =
                            detailsList.mapIndexed { index, pokemon ->
                                com.pokemon.domain.model.Edge(
                                        node = pokemon,
                                        cursor = paginationService.encodeCursor(offset + index)
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
                                                    startCursor = edges.firstOrNull()?.cursor,
                                                    endCursor = edges.lastOrNull()?.cursor
                                            ),
                                    totalCount = pageResult.totalCount
                            )

                    PokemonMapper.toDTO(page)
                }
    }
}
