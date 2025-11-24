package com.pokemon.application.service

import com.pokemon.domain.model.Pokemon
import com.pokemon.infrastructure.client.PokeApiClient
import com.pokemon.infrastructure.mapper.PokemonDomainMapper.toDomain
import jakarta.annotation.PostConstruct
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Service
class PokemonCacheService(private val pokeApiClient: PokeApiClient) {
    // This holds the "Master List" (lightweight)
    private var allPokemonIndex: List<PokemonIndexItem> = emptyList()

    // This holds detailed data we have already seen (Detailed Cache)
    private val detailsCache = ConcurrentHashMap<Int, Pokemon>()

    @PostConstruct
    fun init() {
        // Fetch ALL pokemons (limit=10000) just to get names and IDs for sorting
        println("Initializing Pokemon Index...")
        pokeApiClient
                .getPokemonList(10000, 0)
                .doOnNext { response ->
                    allPokemonIndex =
                            response.results.map { item ->
                                val id = extractIdFromUrl(item.url)
                                PokemonIndexItem(id, item.name, item.url)
                            }
                    println("Loaded ${allPokemonIndex.size} pokemons into memory.")
                }
                .doOnError { error ->
                    println("Error initializing Pokemon Index: ${error.message}")
                }
                .subscribe()
    }

    // This is purely synchronous in-memory logic! Fast!
    fun getPage(limit: Int, offset: Int, sortBy: String): PageResult {
        // Safety check: if index hasn't loaded yet, return empty result
        if (allPokemonIndex.isEmpty()) {
            return PageResult(emptyList(), 0)
        }

        // 1. Sort globally
        val sortedList =
                when (sortBy) {
                    "name" -> allPokemonIndex.sortedBy { it.name }
                    else -> allPokemonIndex.sortedBy { it.id } // Default by Number
                }

        // 2. Paginate (Slice)
        val totalCount = sortedList.size
        val safeOffset = if (offset >= totalCount) totalCount else offset
        val safeLimit = if (offset + limit > totalCount) totalCount - offset else limit

        val slicedList =
                if (safeLimit > 0 && safeOffset < totalCount) {
                    sortedList.subList(safeOffset, safeOffset + safeLimit)
                } else {
                    emptyList()
                }

        return PageResult(slicedList, totalCount)
    }

    // Fetch details: Check RAM first, then API
    fun getDetails(id: Int): Mono<Pokemon> {
        return if (detailsCache.containsKey(id)) {
            Mono.just(detailsCache[id]!!)
        } else {
            pokeApiClient
                    .getPokemonById(id)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofMillis(500)).filter { throwable ->
                                // Retry on 5xx errors or network issues
                                if (throwable is
                                                org.springframework.web.reactive.function.client.WebClientResponseException
                                ) {
                                    val statusCode = throwable.statusCode
                                    statusCode.is5xxServerError
                                } else {
                                    false
                                }
                            }
                    )
                    .map { it.toDomain() }
                    .doOnNext { detailsCache[id] = it }
                    .onErrorResume { error ->
                        // Fallback: try using URL from index if available
                        println(
                                "Error fetching Pokemon $id by ID, trying fallback with URL: ${error.message}"
                        )
                        val indexItem = allPokemonIndex.firstOrNull { it.id == id }
                        if (indexItem != null) {
                            println("Found Pokemon $id in index, trying URL: ${indexItem.url}")
                            pokeApiClient
                                    .getPokemonByUrl(indexItem.url)
                                    .map { it.toDomain() }
                                    .doOnNext { detailsCache[id] = it }
                                    .doOnError { fallbackError ->
                                        println(
                                                "Fallback also failed for Pokemon $id: ${fallbackError.message}"
                                        )
                                    }
                        } else {
                            println("Pokemon $id not found in index, cannot use fallback")
                            Mono.error(error)
                        }
                    }
        }
    }

    private fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/').substringAfterLast('/').toInt()
    }

    fun search(query: String): List<PokemonIndexItem> {
        if (allPokemonIndex.isEmpty()) return emptyList()

        val sanitizedQuery = query.trim().lowercase()
        if (sanitizedQuery.isEmpty()) return emptyList()
        return allPokemonIndex
                .filter { it.name.contains(sanitizedQuery) }
                .take(10) // Limit to 10 results
    }
}

data class PokemonIndexItem(val id: Int, val name: String, val url: String)

data class PageResult(val items: List<PokemonIndexItem>, val totalCount: Int)
