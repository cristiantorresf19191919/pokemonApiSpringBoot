package com.pokemon.application.dto

data class PokemonDTO(
    val id: Int,
    val name: String,
    val number: Int,
    val imageUrl: String,
    val abilities: List<AbilityDTO>,
    val moves: List<MoveDTO>,
    val forms: List<FormDTO>
)

data class AbilityDTO(
    val name: String,
    val isHidden: Boolean
)

data class MoveDTO(
    val name: String,
    val levelLearnedAt: Int?
)

data class FormDTO(
    val name: String,
    val url: String
)

