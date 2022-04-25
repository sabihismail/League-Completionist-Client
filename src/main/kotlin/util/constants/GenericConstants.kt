package util.constants

import league.models.enums.GameMode


object GenericConstants {
    private val ROLE_SPECIFIC_MODES = listOf(
        GameMode.DRAFT_PICK,
        GameMode.RANKED_SOLO,
        GameMode.RANKED_FLEX,
        GameMode.CLASH,
    )

    val ACCEPTABLE_GAME_MODES = ROLE_SPECIFIC_MODES + listOf(
        GameMode.ARAM,
        GameMode.BLIND_PICK,
    )
}
