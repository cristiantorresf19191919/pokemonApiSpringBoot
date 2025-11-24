package com.pokemon.domain.model

data class Pokemon(
    val id: Int,
    val name: String,
    val number: Int,
    val imageUrl: String,
    val abilities: List<Ability>,
    val moves: List<Move>,
    val forms: List<Form>
)

data class Ability(
    val name: String,
    val isHidden: Boolean
)

data class Move(
    val name: String,
    val levelLearnedAt: Int?
)

data class Form(
    val name: String,
    val url: String
)

