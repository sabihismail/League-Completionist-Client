package ui

import DEBUG_FAKE_UI_DATA
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.*
import tornadofx.*
import ui.ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
import ui.ViewConstants.CHAMPION_STATUS_NOT_OWNED_COLOR
import ui.ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
import ui.mock.MainViewControllerSimulated
import java.util.*
import kotlin.system.exitProcess

object ViewConstants {
    const val IMAGE_WIDTH = 120.0
    const val IMAGE_SPACING_WIDTH = 8.0

    const val APP_WIDTH = IMAGE_WIDTH * 5 + IMAGE_SPACING_WIDTH * (5 + 2) + 40.0
    const val APP_HEIGHT = 800.0

    val CHAMPION_STATUS_AVAILABLE_CHEST_COLOR: Color = Color.GREEN
    val CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR: Color = Color.RED
    val CHAMPION_STATUS_NOT_OWNED_COLOR: Color = Color.BLACK
}

open class MainViewController : Controller() {
    private val view: MainView by inject()
    private val leagueConnection = LeagueConnection()

    init {
        leagueConnection.start()

        leagueConnection.onSummonerChange {
            val str = when (it.status) {
                SummonerStatus.NOT_LOGGED_IN, SummonerStatus.NOT_CHECKED -> "Not logged in."
                SummonerStatus.LOGGED_IN_UNAUTHORIZED -> "Unauthorized Login."
                SummonerStatus.LOGGED_IN_AUTHORIZED -> "Logged in as: ${it.displayName} (Level: ${it.summonerLevel})"
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
            runLater { view.gameModeProperty.set("Game Mode: ${it.gameMode}") }

            if (it.gameMode != GameMode.ARAM) return@onChampionSelectChange

            runLater {
                view.benchedChampionListProperty.set(FXCollections.observableList(it.benchedChampions))
                view.teamChampionListProperty.set(FXCollections.observableList(it.teamChampions))
            }
        }

        leagueConnection.onClientStateChange {
            runLater { view.clientStateProperty.set("Client State: ${it.name}") }
        }
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
    val benchedChampionListProperty = SimpleListProperty<ChampionInfo>()
    val teamChampionListProperty = SimpleListProperty<ChampionInfo>()

    private val controller = find(if (DEBUG_FAKE_UI_DATA) MainViewControllerSimulated::class else MainViewController::class)

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

            center = vbox {
                alignment = Pos.CENTER

                label("Available Champions:")
                datagrid(benchedChampionListProperty) {
                    alignment = Pos.CENTER
                    paddingBottom = 16.0

                    maxRows = 2
                    maxCellsInRow = 5
                    cellWidth = 120.0
                    cellHeight = 120.0

                    cellCache {
                        imageview(LeagueImageAPI.getChampionImage(it.id))  { effect = LeagueImageAPI.getChampionImageEffect(it) }
                    }
                }

                label("Your Team:")
                datagrid(teamChampionListProperty) {
                    alignment = Pos.CENTER

                    maxRows = 1
                    maxCellsInRow = 5
                    cellWidth = ViewConstants.IMAGE_WIDTH
                    cellHeight = ViewConstants.IMAGE_WIDTH
                    horizontalCellSpacing = ViewConstants.IMAGE_SPACING_WIDTH

                    cellCache {
                        stackpane {
                            imageview(LeagueImageAPI.getChampionImage(it.id)) { effect = LeagueImageAPI.getChampionImageEffect(it) }

                            label(if (it.isSummonerSelectedChamp) "You" else "") {
                                textFill = Color.WHITE

                                style {
                                    backgroundColor += Color.BLACK
                                }
                            }
                        }
                    }
                }
            }

            bottom = vbox {
                vbox {
                    hbox {
                        spacing = 6.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
                        }
                        label("Available")
                    }
                    hbox {
                        spacing = 6.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
                        }
                        label("Already Obtained")
                    }
                    hbox {
                        spacing = 6.0
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
