package com.pokemon.infrastructure.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class PokeApiClient(
    @Value("\${pokeapi.base-url}") private val baseUrl: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun getPokemonList(limit: Int, offset: Int): Mono<PokeApiListResponse> {
        return webClient.get()
            .uri("/pokemon?limit=$limit&offset=$offset")
            .retrieve()
            .bodyToMono(PokeApiListResponse::class.java)
    }

    fun getPokemonById(id: Int): Mono<PokeApiPokemonResponse> {
        return webClient.get()
            .uri("/pokemon/$id")
            .retrieve()
            .bodyToMono(PokeApiPokemonResponse::class.java)
    }

    fun getPokemonByUrl(url: String): Mono<PokeApiPokemonResponse> {
        return webClient.get()
            .uri(url.replace(baseUrl, ""))
            .retrieve()
            .bodyToMono(PokeApiPokemonResponse::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PokeApiListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokeApiListItem>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PokeApiListItem(
    val name: String,
    val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PokeApiPokemonResponse(
    val id: Int,
    val name: String,
    val sprites: Sprites,
    val abilities: List<AbilityResponse>,
    val moves: List<MoveResponse>,
    val forms: List<FormResponse>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sprites(
    @JsonProperty("front_default")
    val frontDefault: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AbilityResponse(
    val ability: AbilityDetail,
    @JsonProperty("is_hidden")
    val isHidden: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AbilityDetail(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MoveResponse(
    val move: MoveDetail,
    @JsonProperty("version_group_details")
    val versionGroupDetails: List<VersionGroupDetail>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MoveDetail(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionGroupDetail(
    @JsonProperty("level_learned_at")
    val levelLearnedAt: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormResponse(
    val name: String,
    val url: String
)

