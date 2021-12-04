package ui

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.*
import tornadofx.*
import java.util.*

class MainViewController : Controller() {
    private val view: MainView by inject()
    private val leagueConnection = LeagueConnection()

    init {
        leagueConnection.start()

        leagueConnection.onSummonerChange {
            val str = when (it.status) {
                SummonerStatus.NOT_LOGGED_IN -> "Not logged in."
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
    }

    fun updateChestInfo() {
        view.chestProperty.set("Querying...")

        leagueConnection.updateMasteryChestInfo()
    }

    fun updateChampionMasteryInfo() {
        leagueConnection.updateChampionMasteryInfo()
    }
}

class MainView: View() {
    val summonerProperty = SimpleStringProperty()
    val chestProperty = SimpleStringProperty()

    val gameModeProperty = SimpleStringProperty()
    val benchedChampionListProperty = SimpleListProperty<ChampionInfo>()
    val teamChampionListProperty = SimpleListProperty<ChampionInfo>()

    private val controller = find(MainViewController::class)

    override val root = vbox {
        prefWidth = 600.0
        prefHeight = 600.0

        borderpane {
            top = vbox {
                alignment = Pos.CENTER

                label(summonerProperty)
                label(chestProperty)
                label(gameModeProperty)
            }

            center = vbox {
                alignment = Pos.CENTER
                paddingTop = 16.0

                label("Available Champions:")
                datagrid(benchedChampionListProperty) {
                    alignment = Pos.CENTER

                    maxRows = 2
                    maxCellsInRow = 5
                    cellWidth = 120.0
                    cellHeight = 120.0

                    cellCache {
                        imageview(LeagueImageAPI.getChampionImage(it.id))
                    }
                }

                label("Your Team:")
                datagrid(teamChampionListProperty) {
                    alignment = Pos.CENTER

                    maxRows = 1
                    maxCellsInRow = 5
                    cellWidth = 120.0
                    cellHeight = 120.0

                    cellCache {
                        stackpane {
                            alignment = Pos.TOP_CENTER

                            imageview(LeagueImageAPI.getChampionImage(it.id))
                            label(if (it.isSummonerSelectedChamp) "You" else "") {
                                alignment = Pos.TOP_CENTER

                                textFill = Color.WHITE
                                style {
                                    backgroundColor += Color.BLACK
                                }
                            }
                        }
                    }
                }
            }

            bottom = hbox {
                alignment = Pos.CENTER
                spacing = 6.0

                button("Refresh Chest Data").setOnAction { controller.updateChestInfo() }
                button("Refresh Champion Mastery Data").setOnAction { controller.updateChampionMasteryInfo() }
            }
        }
    }
}

class MainApp: App(MainView::class)
