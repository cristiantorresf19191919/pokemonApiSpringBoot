package com.pokemon.infrastructure.mapper

import com.pokemon.domain.model.*
import com.pokemon.infrastructure.client.PokeApiPokemonResponse

object PokemonDomainMapper {
    fun PokeApiPokemonResponse.toDomain(): Pokemon {
        return Pokemon(
            id = this.id,
            name = this.name,
            number = this.id,
            imageUrl = this.sprites.frontDefault ?: "",
            abilities = this.abilities.map { ability ->
                Ability(
                    name = ability.ability.name,
                    isHidden = ability.isHidden
                )
            },
            moves = this.moves.map { move ->
                Move(
                    name = move.move.name,
                    levelLearnedAt = move.versionGroupDetails.firstOrNull()?.levelLearnedAt
                )
            },
            forms = this.forms.map { form ->
                Form(
                    name = form.name,
                    url = form.url
                )
            }
        )
    }
}

