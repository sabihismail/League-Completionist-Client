package ui

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.LeagueCommunityDragonAPI
import league.LeagueConnection
import league.models.*
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
    REGULAR
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
    val ACCEPTABLE_GAME_MODES = listOf(
        GameMode.ARAM,
        GameMode.SUMMONERS_RIFT,
        GameMode.CLASH,
        GameMode.RANKED_SOLO,
        GameMode.RANKED_FLEX,
    )

    val ROLE_SPECIFIC_MODES = listOf(
        GameMode.CLASH,
        GameMode.RANKED_SOLO,
        GameMode.RANKED_FLEX,
    )

    val NON_ROLE_SPECIFIC_MODES = listOf(
        GameMode.SUMMONERS_RIFT,
    )
}

open class MainViewController : Controller() {
    private val view: MainView by inject()
    private val aramView: AramGridView by inject()
    private val regularView: NormalGridView by inject()
    private val leagueConnection = LeagueConnection()
    private var activeView = ActiveView.ARAM

    init {
        leagueConnection.start()

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

            if (leagueConnection.gameMode == GameMode.ARAM) {
                runLater {
                    if (activeView != ActiveView.ARAM) {
                        val root = find<NormalGridView>().root
                        root.children.clear()
                        root.children.add(aramView.root)

                        activeView = ActiveView.ARAM
                    }

                    aramView.benchedChampionListProperty.set(FXCollections.observableList(it.benchedChampions))
                    aramView.teamChampionListProperty.set(FXCollections.observableList(it.teamChampions))
                }
            } else {
                runLater {
                    if (activeView != ActiveView.REGULAR) {
                        val root = find<AramGridView>().root
                        root.children.clear()
                        root.children.add(regularView.root)

                        activeView = ActiveView.REGULAR
                    }

                    regularView.championListProperty.set(FXCollections.observableList(getChampionMasteryInfo(byRole=true)))
                }
            }
        }

        leagueConnection.onClientStateChange {
            runLater { view.clientStateProperty.set("Client State: ${it.name}") }
        }
    }

    private fun getChampionMasteryInfo(byRole: Boolean = false): List<ChampionInfo> {
        var info = leagueConnection.championInfo.map { champion -> champion.value }
            .filter { champion -> champion.ownershipStatus == ChampionOwnershipStatus.BOX_NOT_ATTAINED }
            .sortedByDescending { champion -> champion.masteryPoints }

        if (byRole && leagueConnection.championSelectInfo.assignedRole != Role.ANY) {
            val championsByRole = LeagueCommunityDragonAPI.getChampionsByRole(leagueConnection.championSelectInfo.assignedRole)

            info = info.filter { championsByRole.contains(it.id) }
        }

        return info
    }

    open fun updateChestInfo() {
        view.chestProperty.set("Querying...")

        leagueConnection.updateMasteryChestInfo()
    }

    open fun updateChampionMasteryInfo() {
        leagueConnection.updateChampionMasteryInfo()
    }
}



class MainView: View() {
    val summonerProperty = SimpleStringProperty()
    val chestProperty = SimpleStringProperty()

    val clientStateProperty = SimpleStringProperty()
    val gameModeProperty = SimpleStringProperty()

    private val controller = find(
        if (DEBUG_FAKE_UI_DATA_ARAM) AramMockController::class
        else if (DEBUG_FAKE_UI_DATA_NORMAL) NormalMockController::class
        else MainViewController::class)

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

            center = borderpane {
                center<AramGridView>()
            }

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
                hbox {
                    alignment = Pos.CENTER
                    spacing = 6.0

                    button("Refresh Chest Data").setOnAction { controller.updateChestInfo() }
                    button("Refresh Champion Mastery Data").setOnAction { controller.updateChampionMasteryInfo() }
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
