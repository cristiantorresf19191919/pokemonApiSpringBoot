package com.pokemon.application.mapper

import com.pokemon.application.dto.*
import com.pokemon.domain.model.*

object PokemonMapper {
    fun toDTO(pokemon: Pokemon): PokemonDTO {
        return PokemonDTO(
            id = pokemon.id,
            name = pokemon.name,
            number = pokemon.number,
            imageUrl = pokemon.imageUrl,
            abilities = pokemon.abilities.map { AbilityDTO(it.name, it.isHidden) },
            moves = pokemon.moves.map { MoveDTO(it.name, it.levelLearnedAt) },
            forms = pokemon.forms.map { FormDTO(it.name, it.url) }
        )
    }

    fun toDTO(page: Page<Pokemon>): PageDTO<PokemonDTO> {
        return PageDTO(
            edges = page.edges.map { EdgeDTO(toDTO(it.node), it.cursor) },
            pageInfo = PageInfoDTO(
                hasNextPage = page.pageInfo.hasNextPage,
                hasPreviousPage = page.pageInfo.hasPreviousPage,
                startCursor = page.pageInfo.startCursor,
                endCursor = page.pageInfo.endCursor
            ),
            totalCount = page.totalCount
        )
    }
}

