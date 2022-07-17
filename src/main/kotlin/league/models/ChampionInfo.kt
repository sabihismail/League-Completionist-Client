package league.models

import db.DatabaseImpl
import generated.LolStatstonesStatstoneSet
import league.models.enums.ChallengeMappingEnum
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.ChampionRole

data class ChampionInfo(val id: Int = -1, val name: String = "None", val ownershipStatus: ChampionOwnershipStatus = ChampionOwnershipStatus.NOT_OWNED,
                        val masteryPoints: Int = 1, val currentMasteryPoints: Int = 1, val nextLevelMasteryPoints: Int = 2, val level: Int = 0, val tokens: Int = 0,
                        var isSummonerSelectedChamp: Boolean = false, var eternal: LolStatstonesStatstoneSet? = null, var roles: Set<ChampionRole>? = null) {
    val nameLower by lazy {
        name.lowercase()
    }

    val percentageUntilNextLevel by lazy {
        if (level in 1..4)
            " (${"%.1f".format((currentMasteryPoints.toDouble()/(nextLevelMasteryPoints + currentMasteryPoints)) * 100)}%)"
        else
            ""
    }

    val challengesMapping by lazy {
        val ignoreSet = setOf(ChallengeMappingEnum.NONE)

        ChallengeMappingEnum.values().filter { !ignoreSet.contains(it) }
            .associateWith { DatabaseImpl.getChallengeComplete(it, id) }
    }

    val differentChallenges by lazy {
        "[" + challengesMapping.toList().filter { !it.second }.joinToString("|") { ChallengeMappingEnum.mapping[it.first]!! } + "]"
    }

    override fun toString(): String {
        return "ChampionInfo(id=$id, name='$name', ownershipStatus=$ownershipStatus, masteryPoints=$masteryPoints, currentMasteryPoints=$currentMasteryPoints, " +
                "nextLevelMasteryPoints=$nextLevelMasteryPoints, level=$level, tokens=$tokens, isSummonerSelectedChamp=$isSummonerSelectedChamp, eternal=$eternal, " +
                "roles=$roles, percentageUntilNextLevel='$percentageUntilNextLevel')"
    }
}