package ui.mock

import javafx.collections.FXCollections
import league.models.ChampionInfo
import league.models.ChampionSelectInfo
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.GameMode
import league.models.enums.Role
import league.models.enums.SummonerStatus
import tornadofx.runLater
import ui.controllers.MainViewController
import ui.views.MainView
import ui.views.NormalGridView
import java.util.*


class NormalMockController : MainViewController() {
    private val roleFilter = false

    private val view: MainView by inject()
    private val regularView: NormalGridView by inject()

    init {
        val summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)
        runLater { view.summonerProperty.set(summonerInfo) }

        val masteryChestInfo = MasteryChestInfo(Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time, 3)
        runLater { view.chestProperty.set(masteryChestInfo) }

        leagueConnection.gameMode = GameMode.RANKED_FLEX
        leagueConnection.championInfo = mapOf(
            127 to ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.BOX_ATTAINED, 10463, 0, 1, level = 7),
            432 to ChampionInfo(432, "Bard", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10421, 0, 1, level = 6),
            58 to ChampionInfo(58, "Renekton", ChampionOwnershipStatus.FREE_TO_PLAY, 107547, 0, 1, level = 5),
            48 to ChampionInfo(48, "Trundle", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 106372, 0, 1, level = 6),
            45 to ChampionInfo(45, "Veigar", ChampionOwnershipStatus.BOX_ATTAINED, 1025, 0, 1, level = 4),
            28 to ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10621, 0, 1, level = 7),
            13 to ChampionInfo(13, "Ryze", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 105216, 0, 1, level = 2),
            16 to ChampionInfo(16, "Soraka", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10251, 0, 1, level = 6),
            22 to ChampionInfo(22, "Ashe", ChampionOwnershipStatus.BOX_ATTAINED, 102, 0, 1, level = 4),
            875 to ChampionInfo(875, "Sett", ChampionOwnershipStatus.FREE_TO_PLAY, 1035, 0, 1, level = 2),
            711 to ChampionInfo(711, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 5215, 0, 1, level = 6),
            350 to ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 62621, 0, 1, level = 3),
            114 to ChampionInfo(114, "Fiora", ChampionOwnershipStatus.BOX_ATTAINED, 1525120, 0, 1, level = 4),
            134 to ChampionInfo(134, "Syndra", ChampionOwnershipStatus.FREE_TO_PLAY, 162610, 0, 1, level = 2),
            106 to ChampionInfo(106, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52110, 0, 1, level = 5),
            1 to ChampionInfo(1, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52155, 0, 1, level = 3),
            2 to ChampionInfo(2, "Yuumi", ChampionOwnershipStatus.BOX_ATTAINED, 626271, 0, 1, level = 5),
            4 to ChampionInfo(4, "Fiora", ChampionOwnershipStatus.FREE_TO_PLAY, 15256120, 0, 1, level = 7),
            39 to ChampionInfo(39, "Irelia", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1626110, 0, 1, level = 2, isSummonerSelectedChamp = true,
                hasEternal = false
            ),
            8 to ChampionInfo(8, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 522110, 0, 1, level = 0),
        )

        leagueConnection.role = if (roleFilter) Role.TOP else Role.ANY
        leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)
        leagueConnection.gameMode = GameMode.RANKED_FLEX

        runLater {
            view.gameModeProperty.set(leagueConnection.gameMode)
            view.defaultGridView.setRoot(regularView)

            val sortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            regularView.setChampions(FXCollections.observableList(sortedChampionInfo))
        }

        regularView.currentRole.addListener { _, _, newValue ->
            leagueConnection.role = newValue
            leagueConnection.gameMode = GameMode.RANKED_FLEX
            leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            regularView.setChampions(FXCollections.observableList(newSortedChampionInfo))
        }
    }
}
