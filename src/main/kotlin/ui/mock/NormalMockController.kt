package ui.mock

import javafx.collections.FXCollections
import league.models.ChampionInfo
import league.models.ChampionSelectInfo
import league.models.SummonerInfo
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.GameMode
import league.models.enums.Role
import league.models.enums.SummonerStatus
import tornadofx.runLater
import ui.controllers.MainViewController


class NormalMockController : MainViewController() {
    private val roleFilter = false

    init {
        val summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)
        runLater { view.summonerProperty.set(summonerInfo) }

        leagueConnection.gameMode = GameMode.RANKED_FLEX
        leagueConnection.championInfo = mapOf(
            127 to ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.RENTAL, 10463, 0, 1, level = 7),
            432 to ChampionInfo(432, "Bard", ChampionOwnershipStatus.RENTAL, 10421, 0, 1, level = 6),
            58 to ChampionInfo(58, "Renekton", ChampionOwnershipStatus.FREE_TO_PLAY, 107547, 0, 1, level = 5),
            48 to ChampionInfo(48, "Trundle", ChampionOwnershipStatus.RENTAL, 106372, 0, 1, level = 6),
            45 to ChampionInfo(45, "Veigar", ChampionOwnershipStatus.RENTAL, 1025, 0, 1, level = 4),
            28 to ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.RENTAL, 10621, 0, 1, level = 7),
            13 to ChampionInfo(13, "Ryze", ChampionOwnershipStatus.RENTAL, 105216, 0, 1, level = 2),
            16 to ChampionInfo(16, "Soraka", ChampionOwnershipStatus.RENTAL, 10251, 0, 1, level = 6),
            22 to ChampionInfo(22, "Ashe", ChampionOwnershipStatus.RENTAL, 102, 0, 1, level = 4),
            875 to ChampionInfo(875, "Sett", ChampionOwnershipStatus.FREE_TO_PLAY, 1035, 0, 1, level = 2),
            711 to ChampionInfo(711, "Vex", ChampionOwnershipStatus.RENTAL, 5215, 0, 1, level = 6),
            350 to ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.RENTAL, 62621, 0, 1, level = 3),
            114 to ChampionInfo(114, "Fiora", ChampionOwnershipStatus.RENTAL, 1525120, 0, 1, level = 4),
            134 to ChampionInfo(134, "Syndra", ChampionOwnershipStatus.FREE_TO_PLAY, 162610, 0, 1, level = 2),
            106 to ChampionInfo(106, "Volibear", ChampionOwnershipStatus.RENTAL, 52110, 0, 1, level = 5),
            1 to ChampionInfo(1, "Vex", ChampionOwnershipStatus.RENTAL, 52155, 0, 1, level = 3),
            2 to ChampionInfo(2, "Yuumi", ChampionOwnershipStatus.RENTAL, 626271, 0, 1, level = 5),
            4 to ChampionInfo(4, "Fiora", ChampionOwnershipStatus.FREE_TO_PLAY, 15256120, 0, 1, level = 7),
            39 to ChampionInfo(39, "Irelia", ChampionOwnershipStatus.RENTAL, 1626110, 0, 1, level = 2, isSummonerSelectedChamp = true,
                eternalInfo = mapOf()
            ),
            8 to ChampionInfo(8, "Volibear", ChampionOwnershipStatus.RENTAL, 522110, 0, 1, level = 0),
        )

        leagueConnection.role = if (roleFilter) Role.TOP else Role.ANY
        leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)
        leagueConnection.gameMode = GameMode.RANKED_FLEX

        runLater {
            view.gameModeProperty.set(leagueConnection.gameMode)
            view.defaultGridView.setRoot(normalView)

            val sortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.setChampions(FXCollections.observableList(sortedChampionInfo))
        }

        normalView.currentLaneProperty.addListener { _, _, newValue ->
            leagueConnection.role = newValue
            leagueConnection.gameMode = GameMode.RANKED_FLEX
            leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.setChampions(FXCollections.observableList(newSortedChampionInfo))
        }
    }
}
