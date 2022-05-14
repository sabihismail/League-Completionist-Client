package ui.mock

import javafx.collections.FXCollections
import league.models.ChampionInfo
import league.models.ChampionSelectInfo
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.GameMode
import league.models.enums.SummonerStatus
import tornadofx.runLater
import ui.controllers.MainViewController
import ui.views.AramGridView
import ui.views.MainView
import java.util.*


class AramMockController : MainViewController() {
    private val view: MainView by inject()
    private val aramView: AramGridView by inject()

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
            listOf(
                ChampionInfo(711, "Vex", ChampionOwnershipStatus.BOX_NOT_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(114, "Fiora", ChampionOwnershipStatus.FREE_TO_PLAY, 10, isSummonerSelectedChamp = false),
                ChampionInfo(134, "Syndra", ChampionOwnershipStatus.RENTAL, 10, isSummonerSelectedChamp = true),
                ChampionInfo(106, "Volibear", ChampionOwnershipStatus.NOT_OWNED, 10, isSummonerSelectedChamp = false),
            ),
            listOf(
                ChampionInfo(432, "Bard", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(58, "Renekton", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(48, "Trundle", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(45, "Veigar", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(13, "Ryze", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(16, "Soraka", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(22, "Ashe", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
                ChampionInfo(875, "Sett", ChampionOwnershipStatus.BOX_ATTAINED, 10, isSummonerSelectedChamp = false),
            )
        )

        runLater {
            view.defaultGridView.setRoot(aramView)

            view.gameModeProperty.set("Game Mode: ${GameMode.ARAM}")

            aramView.benchedChampionListProperty.set(FXCollections.observableList(championSelectInfo.benchedChampions))
            aramView.teamChampionListProperty.set(FXCollections.observableList(championSelectInfo.teamChampions))
        }
    }
}
