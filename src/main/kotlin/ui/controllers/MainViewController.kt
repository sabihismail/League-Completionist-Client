package ui.controllers

import db.DatabaseImpl
import generated.LolGameflowGameflowPhase
import javafx.collections.FXCollections
import league.LeagueConnection
import league.api.LeagueCommunityDragonAPI
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
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


open class MainViewController : Controller() {
    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val normalView: NormalGridView by inject()

    private var activeView = ActiveView.NORMAL
    private var manualRoleSelect = false

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

        leagueConnection.onLoggedIn {
            leagueConnection.updateChallengesInfo()
            setChallengesView()

            val elements = leagueConnection.challengeInfo.values
                .flatMap { challengeInfos ->
                    challengeInfos.flatMap {
                        challengeInfo -> challengeInfo.thresholds!!.keys.map { rank -> Pair(challengeInfo.id, rank) }
                    }
                }
                .toList()

            val maxCount = elements.count()
            val fileWalk = Files.walk(LeagueCommunityDragonAPI.getPath(ImageCacheType.CHALLENGE)).count()
            if (fileWalk < maxCount) {
                thread {
                    Logging.log("Challenges - Starting Cache Download...", LogType.INFO)

                    val num = AtomicInteger(0)
                    elements.parallelStream()
                        .forEach {
                            LeagueCommunityDragonAPI.getImagePath(ImageCacheType.CHALLENGE, it.first.toString().lowercase(), it.second)

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
            val str = when (it.status) {
                SummonerStatus.NOT_LOGGED_IN, SummonerStatus.NOT_CHECKED -> "Not logged in."
                SummonerStatus.LOGGED_IN_UNAUTHORIZED -> "Unauthorized Login."
                SummonerStatus.LOGGED_IN_AUTHORIZED -> "Logged in as: ${it.displayName} (Level ${it.summonerLevel})"
            }

            runLater {
                view.summonerProperty.set(str)
                view.isLoggedInProperty.set(it.status == SummonerStatus.LOGGED_IN_AUTHORIZED)
            }

            if (it.status != SummonerStatus.LOGGED_IN_AUTHORIZED) return@onSummonerChange

            leagueConnection.updateMasteryChestInfo()
            leagueConnection.updateChampionMasteryInfo()
            leagueConnection.updateClientState()

            updateChampionList()
        }

        leagueConnection.onMasteryChestChange {
            if (it.nextChestDate == null) return@onMasteryChestChange

            val remaining = (it.nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
            val remainingStr = String.format("%.2f", remaining)

            runLater { view.chestProperty.set("Available chests: ${it.chestCount} (next one in $remainingStr days)") }

            DatabaseImpl.setMasteryInfo(leagueConnection.summonerInfo, leagueConnection.masteryChestInfo, remaining)

            runLater { view.masteryAccountView.run() }
        }

        leagueConnection.onChampionSelectChange {
            runLater { view.gameModeProperty.set("Game Mode: ${leagueConnection.gameMode}") }

            if (!ACCEPTABLE_GAME_MODES.contains(leagueConnection.gameMode)) return@onChampionSelectChange

            replaceDisplay()
        }

        leagueConnection.onClientStateChange {
            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                leagueConnection.updateChampionMasteryInfo()

                updateChampionList()
            }

            if (it == LolGameflowGameflowPhase.CHAMPSELECT) {
                manualRoleSelect = false
            }

            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                leagueConnection.updateChallengesInfo()

                if (view.find<ChallengesView>().primaryStage.isShowing) {
                    setChallengesView()
                }
            }

            if (STATES_TO_REFRESH_DISPLAY.contains(it)) {
                while (leagueConnection.championInfo.isEmpty()) {
                    leagueConnection.updateChampionMasteryInfo()
                }

                replaceDisplay()
            }

            runLater { view.masteryAccountView.run() }
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

    fun setChallengesView() {
        view.find<ChallengesView>().setChallenges(leagueConnection.challengeInfo, leagueConnection.challengeInfo.keys.sortedBy { it })
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
