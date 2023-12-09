package league.models.json

import generated.*

data class GameDataTeam(
    var championId: Int,
    var lastSelectedSkinIndex: Int,
    var profileIconId: Int,
    var puuid: String,
    var selectedPosition: String,
    var selectedRole: String,
    var summonerId: Long,
    var summonerInternalName: String,
    var teamOwner: Boolean,
    var teamParticipantId: Int,
)

data class GameDataPlayerSelections(
    var championId: Int,
    var selectedSkinIndex: Int,
    var spell1Id: Int,
    var spell2Id: Int,
    var summonerInternalName: String,
)

data class GameData(
    var gameId: Long,
    var gameName: String,
    var isCustomGame: Boolean,
    var password: String,
    var playerChampionSelections: List<GameDataPlayerSelections>,
    var queue: LolGameflowQueue,
    var spectatorsAllowed: Boolean,
    var teamOne: List<GameDataTeam>,
    var teamTwo: List<GameDataTeam>,
) {
    fun getCurrentChampionId(name: String): Int {
        val nameToChampionIdMapping = playerChampionSelections.associate { it.summonerInternalName to it.championId }

        return nameToChampionIdMapping[name]!!
    }
}

data class GameflowSession(
    var gameClient: LolGameflowGameflowGameClient,
    var gameData: GameData,
    var gameDodge: LolGameflowGameflowGameDodge,
    var map: LolGameflowGameflowGameMap,
    var phase: LolGameflowGameflowPhase,
)
