package league.models

import com.stirante.lolclient.ClientApi
import db.DatabaseImpl
import generated.LolStatstonesStatstone
import generated.LolStatstonesStatstoneSet
import league.models.enums.ChallengeMappingEnum
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.ChampionRole

data class ChampionInfo(val id: Int = -1, val name: String = "None", val ownershipStatus: ChampionOwnershipStatus = ChampionOwnershipStatus.NOT_OWNED,
                        val masteryPoints: Int = 1, val currentMasteryPoints: Int = 1, val nextLevelMasteryPoints: Int = 2, val level: Int = 0, val tokens: Int = 0,
                        var isSummonerSelectedChamp: Boolean = false, var eternalInfo: Map<Int, Boolean> = mapOf(), var roles: Set<ChampionRole>? = null,
                        var idealChampionToMasterEntry: Int = -1, val clientApi: ClientApi? = null) {
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

    fun getEternals(showEternals: Boolean): List<LolStatstonesStatstone> {
        return if (eternalInfo.any { it.value } && showEternals) {
            clientApi?.executeGet("/lol-statstones/v2/player-statstones-self/${id}", Array<LolStatstonesStatstoneSet>::class.java)?.responseObject
                ?.filter { set -> set.name != "Starter Series" && set.stonesOwned > 0 }
                ?.flatMap { set -> set.statstones } ?: listOf()
        } else {
            listOf()
        }
    }

    override fun toString(): String {
        return "ChampionInfo(id=$id, name='$name', ownershipStatus=$ownershipStatus, masteryPoints=$masteryPoints, currentMasteryPoints=$currentMasteryPoints, " +
                "nextLevelMasteryPoints=$nextLevelMasteryPoints, level=$level, tokens=$tokens, isSummonerSelectedChamp=$isSummonerSelectedChamp, hasEternal=$eternalInfo, " +
                "roles=$roles, idealChampionToMasterEntry=$idealChampionToMasterEntry, clientApi=$clientApi, nameLower='$nameLower', " +
                "percentageUntilNextLevel='$percentageUntilNextLevel', challengesMapping=$challengesMapping, differentChallenges='$differentChallenges')"
    }
}