package ui

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import generated.LolGameflowGameflowPhase
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.LeagueConnection
import league.models.GameMode
import league.models.Role
import league.models.SummonerStatus
import tornadofx.*
import ui.GenericConstants.ACCEPTABLE_GAME_MODES
import ui.ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
import ui.ViewConstants.CHAMPION_STATUS_NOT_OWNED_COLOR
import ui.ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
import ui.mock.AramMockController
import ui.mock.NormalMockController
import ui.views.AramGridView
import ui.views.NormalGridView
import java.util.*
import kotlin.system.exitProcess

enum class ActiveView {
    ARAM,
    NORMAL,
}

object ViewConstants {
    const val IMAGE_WIDTH = 120.0
    const val IMAGE_SPACING_WIDTH = 8.0

    const val APP_WIDTH = IMAGE_WIDTH * 5 + IMAGE_SPACING_WIDTH * (5 + 2) + 40.0
    const val APP_HEIGHT = 800.0

    val CHAMPION_STATUS_AVAILABLE_CHEST_COLOR: Color = Color.GREEN
    val CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR: Color = Color.RED
    val CHAMPION_STATUS_NOT_OWNED_COLOR: Color = Color.BLACK
}

object GenericConstants {
    val ROLE_SPECIFIC_MODES = listOf(
        GameMode.DRAFT_PICK,
        GameMode.RANKED_SOLO,
        GameMode.RANKED_FLEX,
        GameMode.CLASH,
    )

    val ACCEPTABLE_GAME_MODES = ROLE_SPECIFIC_MODES + listOf(
        GameMode.ARAM,
        GameMode.BLIND_PICK,
    )
}

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

            if (!ACCEPTABLE_GAME_MODES.contains(leagueConnection.gameMode)) return@onChampionSelectChange

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

class DefaultGridView: View() {
    private var currentView = object: View() {
        override val root = vbox {  }
    }

    override val root = borderpane {
        center = currentView.root
    }

    fun setRoot(view: View) {
        if (isAlreadyBound(view)) return

        root.center = view.root
    }

    private fun isAlreadyBound(view: View): Boolean {
        return currentView == view
    }
}

class MainView: View() {
    val defaultGridView = find(DefaultGridView::class)

    val summonerProperty = SimpleStringProperty()
    val chestProperty = SimpleStringProperty()

    val clientStateProperty = SimpleStringProperty()
    val gameModeProperty = SimpleStringProperty()

    @Suppress("unused")
    private val controller = find(
        if (DEBUG_FAKE_UI_DATA_ARAM) AramMockController::class
        else if (DEBUG_FAKE_UI_DATA_NORMAL) NormalMockController::class
        else MainViewController::class
    )

    override val root = vbox {
        prefWidth = ViewConstants.APP_WIDTH
        prefHeight = ViewConstants.APP_HEIGHT

        borderpane {
            top = vbox {
                alignment = Pos.CENTER
                paddingBottom = 16.0

                label(summonerProperty)
                label(chestProperty)
                label(clientStateProperty)
                label(gameModeProperty)
            }

            center = defaultGridView.root

            bottom = vbox {
                vbox {
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
                        }
                        label("Available")
                    }
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
                        }
                        label("Already Obtained")
                    }
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_NOT_OWNED_COLOR
                        }
                        label("Not Owned/Free to Play")
                    }
                }
            }
        }
    }
}

class MainApp: App(MainView::class) {
    override fun stop() {
        super.stop()

        exitProcess(0)
    }
}
