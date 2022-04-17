package ui.mock

import javafx.collections.FXCollections
import league.LeagueConnection
import league.models.*
import tornadofx.runLater
import ui.MainView
import ui.MainViewController
import ui.views.DefaultGridView
import ui.views.NormalGridView
import util.KotlinExtensionUtil.getPrivateProperty
import java.util.*


class NormalMockController : MainViewController() {
    private val roleFilter = false

    private val view: MainView by inject()
    private val regularView: NormalGridView by inject()

    init {
        // onSummonerChange
        val summonerInfo = SummonerInfo(SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)

        runLater { view.summonerProperty.set("Logged in as: ${summonerInfo.displayName} (Level ${summonerInfo.summonerLevel})") }

        // onMasteryChestChange
        val masteryChestInfo = MasteryChestInfo(Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time, 3)

        val remaining = (masteryChestInfo.nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
        val remainingStr = String.format("%.2f", remaining)

        runLater { view.chestProperty.set("Available chests: ${masteryChestInfo.chestCount} (next one in $remainingStr days)") }

        val controller = MainViewController()
        val leagueConnection = controller.getPrivateProperty("leagueConnection") as LeagueConnection

        leagueConnection.gameMode = GameMode.RANKED_FLEX
        leagueConnection.championInfo = mapOf(
            127 to ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.BOX_ATTAINED, 10463, level = 7),
            432 to ChampionInfo(432, "Bard", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10421, level = 6),
            58 to ChampionInfo(58, "Renekton", ChampionOwnershipStatus.FREE_TO_PLAY, 107547, level = 5),
            48 to ChampionInfo(48, "Trundle", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 106372, level = 6),
            45 to ChampionInfo(45, "Veigar", ChampionOwnershipStatus.BOX_ATTAINED, 1025, level = 4),
            28 to ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10621, level = 7),
            13 to ChampionInfo(13, "Ryze", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 105216, level = 2),
            16 to ChampionInfo(16, "Soraka", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10251, level = 6),
            22 to ChampionInfo(22, "Ashe", ChampionOwnershipStatus.BOX_ATTAINED, 102, level = 4),
            875 to ChampionInfo(875, "Sett", ChampionOwnershipStatus.FREE_TO_PLAY, 1035, level = 2),
            711 to ChampionInfo(711, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 5215, level = 6),
            350 to ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 62621, level = 3),
            114 to ChampionInfo(114, "Fiora", ChampionOwnershipStatus.BOX_ATTAINED, 1525120, level = 4),
            134 to ChampionInfo(134, "Syndra", ChampionOwnershipStatus.FREE_TO_PLAY, 162610, level = 2),
            106 to ChampionInfo(106, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52110, level = 5),
            1 to ChampionInfo(1, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52155, level = 3),
            2 to ChampionInfo(2, "Yuumi", ChampionOwnershipStatus.BOX_ATTAINED, 626271, level = 5),
            4 to ChampionInfo(4, "Fiora", ChampionOwnershipStatus.FREE_TO_PLAY, 15256120, level = 7),
            7 to ChampionInfo(7, "Syndra", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1626110, level = 2),
            8 to ChampionInfo(8, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 522110, level = 0),
        )

        leagueConnection.role = if (roleFilter) Role.TOP else Role.ANY
        leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)
        leagueConnection.gameMode = GameMode.RANKED_FLEX
        val sortedChampionInfo = controller.getChampionMasteryInfo()

        val root = find<DefaultGridView>().root
        root.children.clear()
        root.children.add(regularView.root)

        runLater { view.gameModeProperty.set("Game Mode: ${leagueConnection.gameMode}") }

        runLater {
            regularView.championListProperty.set(FXCollections.observableList(sortedChampionInfo))
        }

        regularView.currentRole.addListener { _, _, newValue ->
            leagueConnection.role = Role.valueOf(newValue.toString())
            leagueConnection.gameMode = GameMode.RANKED_FLEX
            leagueConnection.championSelectInfo = ChampionSelectInfo(assignedRole = leagueConnection.role)
            val newSortedChampionInfo = controller.getChampionMasteryInfo()

            regularView.championListProperty.set(FXCollections.observableList(newSortedChampionInfo))
        }
    }
}
