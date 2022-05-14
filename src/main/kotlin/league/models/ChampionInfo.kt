package league.models

import generated.LolStatstonesStatstoneSet
import league.models.enums.ChampionOwnershipStatus

data class ChampionInfo(val id: Int, val name: String, val ownershipStatus: ChampionOwnershipStatus, val masteryPoints: Int, val level: Int = 0, val tokens: Int = 0,
                        var isSummonerSelectedChamp: Boolean = false, var eternal: LolStatstonesStatstoneSet? = null) {
    override fun toString(): String {
        return "ChampionInfo(id=$id, name='$name', ownershipStatus=$ownershipStatus, masteryPoints=$masteryPoints, level=$level, tokens=$tokens, " +
                "isSummonerSelectedChamp=$isSummonerSelectedChamp, eternal=$eternal)"
    }
}