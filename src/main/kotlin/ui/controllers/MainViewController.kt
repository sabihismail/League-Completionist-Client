package ui.controllers

import generated.LolGameflowGameflowPhase
import javafx.collections.FXCollections
import league.LeagueConnection
import league.models.ChallengeLevelInfo
import league.models.enums.*
import league.models.json.ChallengeInfo
import tornadofx.Controller
import tornadofx.runLater
import ui.views.*
import ui.views.ChallengesView.Companion.CRINGE_MISSIONS
import ui.views.containers.UpgradedChallengesContainer
import ui.views.fragments.ChampionFragment


open class MainViewController : Controller() {
    val leagueConnection = LeagueConnection()

    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val normalView: NormalGridView by inject()
    private val challengesView: ChallengesView by inject()
    private val challengesLevelView: ChallengesLevelView by inject()
    private val challengesUpdatedView: ChallengesUpdatedView by inject()
    private val debugView: DebugView by inject()

    private var activeView = ActiveView.NORMAL
    private var manualRoleSelect = false
    private var manualGameModeSelect = false
    private var championFragmentSet = false

    init {
        runLater { view.defaultGridView.setRoot(normalView) }

        leagueConnection.start()

        normalView.currentLane.addListener { _, _, newValue ->
            manualRoleSelect = true

            leagueConnection.role = newValue

            val newSortedChampionInfo = leagueConnection.getChampionMasteryInfo()
            normalView.setCompletableChallenges(leagueConnection.completableChallenges)
            normalView.setChampions(newSortedChampionInfo)
        }

        normalView.currentChampionRole.addListener { _, _, _ ->
            normalView.setChampions(leagueConnection.getChampionMasteryInfo())
        }

        normalView.currentChallenge.addListener { _, _, _ ->
            normalView.setChampions(leagueConnection.getChampionMasteryInfo())
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
                    runLater {
                        val newFragment = view.find<ChampionFragment>()
                        view.currentChampionView.replaceWith(newFragment)
                        view.currentChampionView = newFragment
                    }
                    runLater { normalView.currentLane.set(Role.ANY) }
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
            if (view.currentChampionView.champion.id != it.teamChampions.firstOrNull { championInfo -> championInfo?.isSummonerSelectedChamp == true }?.id) {
                championFragmentSet = false
            }

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
                } ui { gameMode ->
                    challengesView.currentGameModeProperty.set(gameMode)
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
                championFragmentSet = false
            }

            if (it == LolGameflowGameflowPhase.INPROGRESS) {
                updateCurrentChampion()
            }

            if (it == LolGameflowGameflowPhase.ENDOFGAME) {
                updateChampionList()
                updateCurrentChampion()
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

        leagueConnection.onLcuEvent { event ->
            runAsync {
                val newValue = debugView.events.value ?: return@runAsync listOf(event)

                newValue.toMutableList().apply { add(event) }
            } ui {
                debugView.events.set(FXCollections.observableList(it))
            }
        }
    }

    private fun updateCurrentChampion() {
        if (championFragmentSet) return
        if (!leagueConnection.championSelectInfo.teamChampions.any { championInfo -> championInfo?.isSummonerSelectedChamp == true }) return

        runAsync {
            leagueConnection.championSelectInfo.teamChampions.firstOrNull { championInfo -> championInfo?.isSummonerSelectedChamp == true }
                ?.apply { eternalInfo = leagueConnection.championInfo[id]?.eternalInfo!! }
        } ui {
            if (it == null) return@ui

            val newFragment = view.find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showEternals to true))
            view.currentChampionView.replaceWith(newFragment)
            view.currentChampionView = newFragment

            championFragmentSet = true
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
                runLater { normalView.currentLane.set(leagueConnection.championSelectInfo.assignedRole) }
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

                    normalView.setCompletableChallenges(leagueConnection.completableChallenges)
                    normalView.setChampions(championList)
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

    fun updatedChallengeLevelsView() {
        runAsync {
            leagueConnection.challengeInfo.flatMap {
                it.value.map { x ->
                    x.init()
                    val rewardValue = x.allThresholds.associate { y -> y.first to y.second }[x.rewardLevel]?.value ?: 0.0

                    ChallengeLevelInfo(x.id?.toLong() ?: 0L, x.description ?: "", x.rewardLevel, x.rewardTitle, rewardValue,
                        x.currentLevel ?: ChallengeLevel.NONE, x.currentLevelImage, x.currentValue ?: 0.0)
                } }
                .filter { it.rewardLevel != ChallengeLevel.NONE }
        } ui {
            challengesLevelView.setChallenges(it)
        }
    }

    fun updateChallengesUpdatedView() {
        runAsync {
            val upgraded = leagueConnection.challengesUpdatedInfo.filter { it.first.currentLevel != it.second.currentLevel }
                .sortedWith(
                    compareByDescending<Pair<ChallengeInfo, ChallengeInfo>> { it.second.category != ChallengeCategory.LEGACY }
                        .thenByDescending { it.second.currentLevel }
                        .thenByDescending { it.second.percentage }
                )

            val compareBy = compareByDescending<Pair<ChallengeInfo, ChallengeInfo>> { !CRINGE_MISSIONS.any { x -> it.second.name!!.contains(x) } }
                    .thenByDescending { it.second.category != ChallengeCategory.LEGACY }
                    .thenByDescending { it.first.pointsDifference > 0 }
                    .thenByDescending { it.second.currentLevel }
                    .thenByDescending { it.second.percentage }
            val progressed = leagueConnection.challengesUpdatedInfo.filter { it.first.currentLevel == it.second.currentLevel }
                .filter { it.first.currentLevel!! <= ChallengeLevel.DIAMOND && !it.first.maxThresholdReached }
                .sortedWith(compareBy)

            val completed = leagueConnection.challengesUpdatedInfo.filter { it.first.currentLevel == it.second.currentLevel }
                .filter { it.first.currentLevel!! > ChallengeLevel.DIAMOND && (!it.first.maxThresholdReached || it.first.hasLeaderboard) }
                .sortedWith(compareBy)

            val upgradedSet = upgraded.map { it.second.descriptiveDescription }.toHashSet()
            val progressedSet = progressed.map { it.second.descriptiveDescription }.toHashSet()
            val completedSet = completed.map { it.second.descriptiveDescription }.toHashSet()
            val allSet = leagueConnection.challengesUpdatedInfo
                .filter { it.first.currentLevel != it.second.currentLevel || (!it.first.maxThresholdReached || it.first.hasLeaderboard) }
                .map { it.first.descriptiveDescription }
                .toHashSet()
            val intersections = listOf(upgradedSet.intersect(progressedSet), progressedSet.intersect(completedSet), completedSet.intersect(upgradedSet))
            if (intersections.any { it.isNotEmpty() }) {
                println("Set failure 1: ${intersections.first { it.isNotEmpty() }.joinToString("\n")}")
            }

            val combined = upgradedSet.union(progressedSet.union(completedSet))
            if (allSet != combined) {
                val difference = combined.subtract(allSet).plus(allSet.subtract(combined))
                val d = combined.subtract(allSet)
                val a = allSet.subtract(combined)

                println("Set failure 2: ${difference.joinToString("\n")}")
                println(d.joinToString("\n"))
                println(a.joinToString("\n"))
            }

            UpgradedChallengesContainer(upgraded, progressed, completed)
        } ui {
            challengesUpdatedView.challengesUpgradedProperty.set(FXCollections.observableList(it.upgraded))
            challengesUpdatedView.challengesProgressedProperty.set(FXCollections.observableList(it.progressed))
            challengesUpdatedView.challengesCompletedProperty.set(FXCollections.observableList(it.completed))
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
