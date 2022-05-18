package ui.controllers

import db.DatabaseImpl
import generated.LolGameflowGameflowPhase
import javafx.collections.FXCollections
import league.LeagueConnection
import league.api.LeagueCommunityDragonApi
import league.models.enums.*
import tornadofx.Controller
import tornadofx.runLater
import ui.views.AramGridView
import ui.views.ChallengesView
import ui.views.MainView
import ui.views.NormalGridView
import util.LogType
import util.Logging
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


open class MainViewController : Controller() {
    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val normalView: NormalGridView by inject()

    private var activeView = ActiveView.NORMAL
    private var manualRoleSelect = false
    private var manualGameModeSelect = false

    val leagueConnection = LeagueConnection()

    init {
        runLater { view.defaultGridView.setRoot(normalView) }

        leagueConnection.start()

        normalView.currentRole.addListener { _, _, newValue ->
            manualRoleSelect = true

            leagueConnection.role = Role.valueOf(newValue.toString())

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.championListProperty.set(FXCollections.observableList(newSortedChampionInfo))
        }

        normalView.find<ChallengesView>().currentGameModeProperty.addListener { _, _, _ ->
            manualGameModeSelect = true
        }

        leagueConnection.onLoggedIn {
            leagueConnection.updateChallengesInfo()
            updateChallengesView()

            val elements = leagueConnection.challengeInfo.values
                .flatMap { challengeInfos ->
                    challengeInfos.flatMap {
                        challengeInfo -> challengeInfo.thresholds!!.keys.map { rank -> Pair(challengeInfo.id, rank) }
                    }
                }
                .toList()

            val maxCount = elements.count()
            val fileWalk = Files.walk(LeagueCommunityDragonApi.getPath(ImageCacheType.CHALLENGE)).count()
            if (fileWalk < maxCount) {
                thread {
                    Logging.log("Challenges - Starting Cache Download...", LogType.INFO)

                    val num = AtomicInteger(0)
                    elements.parallelStream()
                        .forEach {
                            LeagueCommunityDragonApi.getImagePath(ImageCacheType.CHALLENGE, it.first.toString().lowercase(), it.second)

                            num.incrementAndGet()
                        }

                    while (num.get() != maxCount) {
                        Thread.sleep(1000)
                    }

                    Logging.log("Challenges - Finished Cache Download.", LogType.INFO)
                }
            }
        }

        leagueConnection.onSummonerChange {
            runLater { view.summonerProperty.set(it) }

            if (it.status != SummonerStatus.LOGGED_IN_AUTHORIZED) return@onSummonerChange

            leagueConnection.updateMasteryChestInfo()
            leagueConnection.updateChampionMasteryInfo()
            leagueConnection.updateClientState()

            updateChampionList()
        }

        leagueConnection.onMasteryChestChange {
            if (it.nextChestDate == null) return@onMasteryChestChange

            runLater { view.chestProperty.set(it) }

            DatabaseImpl.setMasteryInfo(leagueConnection.summonerInfo, leagueConnection.masteryChestInfo, it.remainingTime)

            runLater { view.masteryAccountView.run() }
        }

        leagueConnection.onChampionSelectChange {
            runLater { view.gameModeProperty.set(leagueConnection.gameMode) }

            if (!ACCEPTABLE_GAME_MODES.contains(leagueConnection.gameMode)) return@onChampionSelectChange

            replaceDisplay()

            if (!manualGameModeSelect) {
                runLater {
                    view.find<ChallengesView>().currentGameModeProperty.set(
                        if (leagueConnection.gameMode.isClassic) {
                            GameMode.CLASSIC
                        } else if (leagueConnection.gameMode == GameMode.ARAM) {
                            GameMode.ARAM
                        } else {
                            throw IllegalArgumentException("onChampionSelectChange - Invalid GameMode - " + leagueConnection.gameMode)
                        }
                    )
                }
            }
        }

        leagueConnection.onChallengesChange {
            updateChallengesView()
        }

        leagueConnection.onClientStateChange {
            if (it == LolGameflowGameflowPhase.CHAMPSELECT) {
                manualRoleSelect = false
                manualGameModeSelect = false
            }

            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                updateChampionList()
            }

            if (STATES_TO_REFRESH_DISPLAY.contains(it)) {
                while (leagueConnection.championInfo.isEmpty()) {
                    leagueConnection.updateChampionMasteryInfo()
                }

                replaceDisplay()
            }

            runLater {
                view.masteryAccountView.run()
                view.clientStateProperty.set(it)
                view.gameModeProperty.set(leagueConnection.gameMode)
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
            runLater {
                normalView.currentRole.set(leagueConnection.championSelectInfo.assignedRole.toString())
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

                    normalView.championListProperty.set(FXCollections.observableList(championList))
                }
            }
        }
    }

    fun updateChallengesView() {
        runLater {
            view.find<ChallengesView>().setChallenges(leagueConnection.challengeInfoSummary, leagueConnection.challengeInfo,
                leagueConnection.challengeInfo.keys.sortedBy { it })
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
