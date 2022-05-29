package ui.controllers

import generated.LolGameflowGameflowPhase
import javafx.collections.FXCollections
import league.LeagueConnection
import league.models.ChampionInfo
import league.models.enums.ActiveView
import league.models.enums.GameMode
import league.models.enums.Role
import league.models.enums.SummonerStatus
import league.models.json.ChallengeInfo
import tornadofx.Controller
import tornadofx.runLater
import ui.views.*
import ui.views.ChallengesView.Companion.CRINGE_MISSIONS
import ui.views.fragments.ChampionFragment


open class MainViewController : Controller() {
    val leagueConnection = LeagueConnection()

    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val normalView: NormalGridView by inject()
    private val challengesView: ChallengesView by inject()
    private val challengesUpdatedView: ChallengesUpdatedView by inject()

    private var activeView = ActiveView.NORMAL
    private var manualRoleSelect = false
    private var manualGameModeSelect = false

    init {
        runLater { view.defaultGridView.setRoot(normalView) }

        leagueConnection.start()

        normalView.currentRole.addListener { _, _, newValue ->
            manualRoleSelect = true

            leagueConnection.role = newValue

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.setChampions(FXCollections.observableList(newSortedChampionInfo))
        }

        challengesView.currentGameModeProperty.addListener { _, _, _ ->
            manualGameModeSelect = true
        }

        leagueConnection.onLoggedIn {
            updateChallengesView()
        }

        leagueConnection.onSummonerChange {
            runLater { view.summonerProperty.set(it) }

            when (it.status) {
                SummonerStatus.LOGGED_IN_AUTHORIZED -> {
                    updateChampionList()
                }
                else -> {
                    runLater { view.currentChampionView.replaceWith(view.find<ChampionFragment>(ChampionFragment::champion to ChampionInfo())) }
                    runLater { normalView.currentRole.set(Role.ANY) }
                }
            }
        }

        leagueConnection.onMasteryChestChange {
            if (it.nextChestDate == null) return@onMasteryChestChange

            runLater { view.chestProperty.set(it) }
            runLater { view.masteryAccountView.run() }
        }

        leagueConnection.onChampionSelectChange {
            runLater { view.gameModeProperty.set(leagueConnection.gameMode) }

            if (!ACCEPTABLE_GAME_MODES.contains(leagueConnection.gameMode)) return@onChampionSelectChange

            replaceDisplay()
            updateCurrentChampion()

            if (!manualGameModeSelect) {
                runAsync {
                    if (leagueConnection.gameMode.isClassic) {
                        GameMode.CLASSIC
                    } else if (leagueConnection.gameMode == GameMode.ARAM) {
                        GameMode.ARAM
                    } else {
                        throw IllegalArgumentException("onChampionSelectChange - Invalid GameMode - " + leagueConnection.gameMode)
                    }
                } ui {
                    challengesView.currentGameModeProperty.set(it)
                }
            }
        }

        leagueConnection.onChallengesChange {
            updateChallengesView()
            updateChallengesUpdatedView()
        }

        leagueConnection.onClientStateChange {
            if (it == LolGameflowGameflowPhase.CHAMPSELECT) {
                manualRoleSelect = false
                manualGameModeSelect = false
            }

            if (it == LolGameflowGameflowPhase.INPROGRESS) {
                updateCurrentChampion()
            }

            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                updateChampionList()
            }

            if (STATES_TO_REFRESH_DISPLAY.contains(it)) {
                replaceDisplay()
            }

            runLater {
                view.masteryAccountView.run()
                view.clientStateProperty.set(it)
                view.gameModeProperty.set(leagueConnection.gameMode)
            }
        }
    }

    private fun updateCurrentChampion() {
        if (!leagueConnection.championSelectInfo.teamChampions.any { championInfo -> championInfo?.isSummonerSelectedChamp == true }) return

        runAsync {
            leagueConnection.championSelectInfo.teamChampions.firstOrNull { championInfo -> championInfo?.isSummonerSelectedChamp == true }
        } ui {
            if (it != null) {
                view.currentChampionView.replaceWith(view.find<ChampionFragment>(mapOf(ChampionFragment::champion to it)))
            }
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

        if (ROLE_SPECIFIC_MODES.contains(leagueConnection.gameMode) && !manualRoleSelect) {
            if (!leagueConnection.isSmurf) {
                runLater { normalView.currentRole.set(leagueConnection.championSelectInfo.assignedRole) }
            }
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

                    normalView.setChampions(FXCollections.observableList(championList))
                }
            }
        }
    }

    fun updateChallengesView() {
        runAsync {
            leagueConnection.challengeInfo.keys.sortedBy { it }
        } ui {
            challengesView.setChallenges(leagueConnection.challengeInfoSummary, leagueConnection.challengeInfo, it)
        }
    }

    fun updateChallengesUpdatedView() {
        runAsync {
            leagueConnection.challengesUpdatedInfo.sortedWith(
                compareByDescending<Pair<ChallengeInfo, ChallengeInfo>> { !CRINGE_MISSIONS.any { x -> it.second.description!!.contains(x) } }
                    .thenByDescending { it.second.currentLevel }
                    .thenByDescending { it.second.percentage }
            )
        } ui {
            challengesUpdatedView.challengesProperty.set(FXCollections.observableList(it))
        }
    }

    companion object {
        const val CHEST_MAX_COUNT = 4
        const val CHEST_WAIT_TIME = 7.0

        private val STATES_TO_REFRESH_DISPLAY = setOf(LolGameflowGameflowPhase.NONE, LolGameflowGameflowPhase.LOBBY, LolGameflowGameflowPhase.CHAMPSELECT,
            LolGameflowGameflowPhase.ENDOFGAME)

        private val ROLE_SPECIFIC_MODES = setOf(
            GameMode.DRAFT_PICK,
            GameMode.RANKED_SOLO,
            GameMode.RANKED_FLEX,
            GameMode.CLASH,
        )

        private val ACCEPTABLE_GAME_MODES = ROLE_SPECIFIC_MODES + setOf(
            GameMode.ARAM,
            GameMode.BLIND_PICK,
        )
    }
}
