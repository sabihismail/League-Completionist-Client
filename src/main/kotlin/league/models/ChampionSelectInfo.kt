package league.models

import league.models.enums.Role

data class ChampionSelectInfo(val teamChampions: List<ChampionInfo> = listOf(), val benchedChampions: List<ChampionInfo> = listOf(), val assignedRole: Role = Role.ANY) {
    override fun toString(): String {
        return "ChampionSelectInfo(teamChampions=$teamChampions, benchedChampions=$benchedChampions, assignedRole=$assignedRole)"
    }
}
