package ui.controllers

import generated.LolGameflowGameflowPhase
import javafx.collections.FXCollections
import league.LeagueConnection
import league.models.enums.ActiveView
import league.models.enums.GameMode
import league.models.enums.Role
import league.models.enums.SummonerStatus
import tornadofx.Controller
import tornadofx.runLater
import ui.views.AramGridView
import ui.views.MainView
import ui.views.NormalGridView
import util.constants.GenericConstants
import java.util.*


open class MainViewController : Controller() {
    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val normalView: NormalGridView by inject()
    private var activeView = ActiveView.NORMAL

    protected val leagueConnection = LeagueConnection()

    init {
        runLater { view.defaultGridView.setRoot(normalView) }

        leagueConnection.start()

        normalView.currentRole.addListener { _, _, newValue ->
            leagueConnection.role = Role.valueOf(newValue.toString())

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.championListProperty.set(FXCollections.observableList(newSortedChampionInfo))
        }

        leagueConnection.onSummonerChange {
            val str = when (it.status) {
                SummonerStatus.NOT_LOGGED_IN, SummonerStatus.NOT_CHECKED -> "Not logged in."
                SummonerStatus.LOGGED_IN_UNAUTHORIZED -> "Unauthorized Login."
                SummonerStatus.LOGGED_IN_AUTHORIZED -> "Logged in as: ${it.displayName} (Level ${it.summonerLevel})"
            }

            runLater { view.summonerProperty.set(str) }

            if (it.status != SummonerStatus.LOGGED_IN_AUTHORIZED) return@onSummonerChange

            leagueConnection.updateMasteryChestInfo()
            leagueConnection.updateClientState()

            updateChampionList()
        }

        leagueConnection.onMasteryChestChange {
            if (it.nextChestDate == null) return@onMasteryChestChange

            val remaining = (it.nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
            val remainingStr = String.format("%.2f", remaining)

            runLater { view.chestProperty.set("Available chests: ${it.chestCount} (next one in $remainingStr days)") }
        }

        leagueConnection.onChampionSelectChange {
            runLater { view.gameModeProperty.set("Game Mode: ${leagueConnection.gameMode}") }

            if (!GenericConstants.ACCEPTABLE_GAME_MODES.contains(leagueConnection.gameMode)) return@onChampionSelectChange

            replaceDisplay()
        }

        leagueConnection.onClientStateChange {
            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                leagueConnection.updateChampionMasteryInfo()

                updateChampionList()
            }

            if (it == LolGameflowGameflowPhase.LOBBY) {
                if (leagueConnection.championInfo.isEmpty()) {
                    leagueConnection.updateChampionMasteryInfo()
                }

                replaceDisplay()
            }

            runLater { view.clientStateProperty.set("Client State: ${it.name}") }
        }
    }

    private fun replaceDisplay() {
        activeView = when (leagueConnection.gameMode) {
            GameMode.ARAM -> ActiveView.ARAM
            GameMode.BLIND_PICK,
            GameMode.DRAFT_PICK,
            GameMode.RANKED_SOLO,
            GameMode.RANKED_FLEX,
            GameMode.CLASH -> ActiveView.NORMAL
            else -> ActiveView.NORMAL
        }

        val replacementView = when (activeView) {
            ActiveView.ARAM -> aramView
            ActiveView.NORMAL -> normalView
        }

        runLater {
            view.defaultGridView.setRoot(replacementView)

            updateChampionList()
        }
    }

    private fun updateChampionList() {
        runLater {
            when (activeView) {
                ActiveView.ARAM -> {
                    aramView.benchedChampionListProperty.set(FXCollections.observableList(leagueConnection.championSelectInfo.benchedChampions))
                    aramView.teamChampionListProperty.set(FXCollections.observableList(leagueConnection.championSelectInfo.teamChampions))
                }
                ActiveView.NORMAL -> {
                    val championList = leagueConnection.getChampionMasteryInfo()

                    normalView.championListProperty.set(FXCollections.observableList(championList))
                }
            }
        }
    }
}
