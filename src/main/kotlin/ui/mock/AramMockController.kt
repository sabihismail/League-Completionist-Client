package ui.mock

import javafx.collections.FXCollections
import league.models.ChampionInfo
import league.models.ChampionSelectInfo
import league.models.SummonerInfo
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.GameMode
import league.models.enums.SummonerStatus
import tornadofx.runLater
import ui.controllers.MainViewController


class AramMockController : MainViewController() {
    init {
        // onSummonerChange
        val summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)

        runLater { view.summonerProperty.set(summonerInfo) }

        // onChampionSelectChange
        val championSelectInfo = ChampionSelectInfo(
            listOf(
                ChampionInfo(711, "Vex", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(350, "Yuumi", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(114, "Fiora", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(134, "Syndra", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = true),
                ChampionInfo(106, "Volibear", ChampionOwnershipStatus.NOT_OWNED, 10, 0, 1, isSummonerSelectedChamp = false),
            ),
            listOf(
                ChampionInfo(432, "Bard", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(127, "Lissandra", ChampionOwnershipStatus.NOT_OWNED, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(58, "Renekton", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(48, "Trundle", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(45, "Veigar", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(28, "Evelynn", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(13, "Ryze", ChampionOwnershipStatus.FREE_TO_PLAY, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(16, "Soraka", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(22, "Ashe", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
                ChampionInfo(875, "Sett", ChampionOwnershipStatus.RENTAL, 10, 0, 1, isSummonerSelectedChamp = false),
            )
        )

        runLater {
            view.defaultGridView.setRoot(aramView)
            view.gameModeProperty.set(GameMode.ARAM)

            aramView.benchedChampionListProperty.set(FXCollections.observableList(championSelectInfo.benchedChampions))
            aramView.teamChampionListProperty.set(FXCollections.observableList(championSelectInfo.teamChampions))
        }
    }
}
