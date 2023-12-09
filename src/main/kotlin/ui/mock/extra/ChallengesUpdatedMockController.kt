package ui.mock.extra

import league.models.SummonerInfo
import league.models.enums.GameMode
import league.models.enums.SummonerStatus
import tornadofx.runLater
import ui.controllers.MainViewController
import ui.views.ChallengesUpdatedView

class ChallengesUpdatedMockController : MainViewController() {
    init {
        // onSummonerChange
        val summonerInfo = SummonerInfo(SummonerStatus.LOGGED_IN_AUTHORIZED, -1, -1, "TestName", "TestName",
            1, 12, 1)

        runLater { view.summonerProperty.set(summonerInfo) }

        runLater {
            view.gameModeProperty.set(GameMode.BLIND_PICK)

            val challengesView = view.find<ChallengesUpdatedView>()
            challengesView.openWindow(owner = null)
        }
    }
}