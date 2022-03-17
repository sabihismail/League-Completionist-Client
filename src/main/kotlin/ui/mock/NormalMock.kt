package ui.mock

import javafx.collections.FXCollections
import league.*
import tornadofx.runLater
import ui.AramGridView
import ui.MainView
import ui.MainViewController
import ui.NormalGridView
import java.util.*


class NormalMainViewControllerSimulated : MainViewController() {
    private val view: MainView by inject()
    private val regularView: NormalGridView by inject()

    init {
        // onSummonerChange
        val summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)

        runLater { view.summonerProperty.set("Logged in as: ${summonerInfo.displayName} (Level ${summonerInfo.summonerLevel})") }

        // onMasteryChestChange
        val masteryChestInfo = MasteryChestInfo(Calendar.getInstance().apply { add(Calendar.DATE, 1) }.time, 3)

        val remaining = (masteryChestInfo.nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
        val remainingStr = String.format("%.2f", remaining)

        runLater { view.chestProperty.set("Available chests: ${masteryChestInfo.chestCount} (next one in $remainingStr days)") }

        // onChampionSelectChange
        val championSelectInfo = ChampionSelectInfo(
            GameMode.RANKED_FLEX,
            listOf(),
            listOf(),
            Role.TOP
        )

        val championInfo = listOf(
            ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10463),
            ChampionInfo(432, "Bard", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10421),
            ChampionInfo(58, "Renekton", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 107547),
            ChampionInfo(48, "Trundle", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 106372),
            ChampionInfo(45, "Veigar", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1025),
            ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10621),
            ChampionInfo(13, "Ryze", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 105216),
            ChampionInfo(16, "Soraka", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10251),
            ChampionInfo(22, "Ashe", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 102),
            ChampionInfo(875, "Sett", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1035),
            ChampionInfo(711, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 5215),
            ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 62621),
            ChampionInfo(114, "Fiora", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1525120),
            ChampionInfo(134, "Syndra", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 162610),
            ChampionInfo(106, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52110),
            ChampionInfo(1, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 52155),
            ChampionInfo(2, "Yuumi", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 626271),
            ChampionInfo(4, "Fiora", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 15256120),
            ChampionInfo(7, "Syndra", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 1626110),
            ChampionInfo(8, "Volibear", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 522110),
        )

        val sortedChampionInfo = championInfo.filter { it.ownershipStatus == ChampionOwnershipStatus.BOX_NOT_ATTAINED }
            .sortedBy { it.masteryPoints }

        val root = find<AramGridView>().root
        root.children.clear()
        root.children.add(regularView.root)

        runLater { view.gameModeProperty.set("Game Mode: ${championSelectInfo.gameMode}") }

        runLater {
            regularView.championListProperty.set(FXCollections.observableList(sortedChampionInfo))
        }
    }

    override fun updateChestInfo() {}
    override fun updateChampionMasteryInfo() {}
}
