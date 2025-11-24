package com.pokemon.application.service

import com.pokemon.application.dto.PageDTO
import com.pokemon.application.dto.PokemonDTO
import com.pokemon.application.dto.PokemonPreviewDTO
import com.pokemon.application.mapper.PokemonMapper
import com.pokemon.domain.model.Page
import com.pokemon.domain.model.Pokemon
import com.pokemon.domain.service.PaginationService
import java.time.Duration
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

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
                // When using 'after', we need to start from the next item after the cursor
                val offset =
                        if (after != null) {
                                (paginationService.decodeCursor(after) ?: 0) + 1
                        } else {
                                0
                        }
                val sortField = sortBy ?: "number"

                // 1. Get the slice from memory (Synchronous)
                val pageResult = pokemonCacheService.getPage(limit, offset, sortField)

                // 2. Hydrate the data (Fetch details for these items only)
                // This runs the calls in parallel for better performance
                // Add error handling to prevent one failure from crashing entire pagination
                return Flux.fromIterable(pageResult.items.withIndex())
                        .flatMap { indexedValue ->
                                val index = indexedValue.index
                                val indexItem = indexedValue.value
                                pokemonCacheService
                                        .getDetails(indexItem.id)
                                        .retryWhen(
                                                Retry.backoff(2, Duration.ofMillis(500)).filter {
                                                        throwable ->
                                                        // Retry on 5xx errors or network issues
                                                        if (throwable is
                                                                        org.springframework.web.reactive.function.client.WebClientResponseException
                                                        ) {
                                                                val statusCode =
                                                                        throwable.statusCode
                                                                statusCode.is5xxServerError
                                                        } else {
                                                                false
                                                        }
                                                }
                                        )
                                        .map { pokemon: Pokemon -> Pair(index, pokemon) }
                                        .onErrorResume { error ->
                                                // Log error but continue with other items
                                                println(
                                                        "Warning: Failed to fetch Pokemon ${indexItem.id} (${indexItem.name}): ${error.message}"
                                                )
                                                Mono.empty<Pair<Int, Pokemon>>()
                                        }
                        }
                        .collectList()
                        .map { indexedDetailsList: List<Pair<Int, Pokemon>> ->
                                // Sort by original index to preserve order
                                val sortedDetails = indexedDetailsList.sortedBy { it.first }
                                // 3. Construct the Cursor-based Page
                                // Use original index from pageResult to maintain correct cursor
                                // positions
                                val edges =
                                        sortedDetails.map { (originalIndex, pokemon) ->
                                                com.pokemon.domain.model.Edge(
                                                        node = pokemon,
                                                        cursor =
                                                                paginationService.encodeCursor(
                                                                        offset + originalIndex
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

        fun searchPokemon(query: String): Flux<PokemonPreviewDTO> {
                // 1. Get raw index items from memory
                val results = pokemonCacheService.search(query)
                // 2. Map to DTO and compute Image URL statically (No API call)
                return Flux.fromIterable(results).map { item ->
                        PokemonPreviewDTO(
                                id = item.id,
                                name = item.name,
                                number = item.id,
                                imageUrl =
                                        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${item.id}.png"
                        )
                }
        }
}
