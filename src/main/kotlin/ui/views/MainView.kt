package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import db.DatabaseImpl
import db.models.MasteryChestTable
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.*
import ui.controllers.MainViewController
import ui.controllers.MainViewController.Companion.CHEST_MAX_COUNT
import ui.controllers.MainViewController.Companion.CHEST_WAIT_TIME
import ui.mock.AramMockController
import ui.mock.NormalMockController
import util.constants.ViewConstants
import java.time.LocalDateTime
import java.time.ZoneId


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
                            fill = ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
                        }
                        label("Available")
                    }
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
                        }
                        label("Already Obtained")
                    }
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = ViewConstants.CHAMPION_STATUS_NOT_OWNED_COLOR
                        }
                        label("Not Owned/Free to Play")
                    }
                }

                separator {
                    paddingTop = 10.0
                    paddingBottom = 6.0
                }

                hbox {
                    paddingBottom = 6.0
                    paddingHorizontal = 4.0

                    for (i in DatabaseImpl.getMasteryChestEntryCount()) {
                        label(getMasteryChestString(i)) {
                            paddingHorizontal = 8.0
                        }
                    }
                }
            }
        }
    }

    private fun getMasteryChestString(id: Int): String {
        lateinit var entry: ResultRow
        transaction {
            entry = MasteryChestTable.select { MasteryChestTable.id eq id }
                .single()
        }

        val zoneId = ZoneId.systemDefault()
        val diff = (entry[MasteryChestTable.lastBoxDate].atZone(zoneId).toEpochSecond() - LocalDateTime.now().atZone(zoneId).toEpochSecond()) / (24 * 60 * 60.0)
        val currentChestCount = (CHEST_MAX_COUNT - (diff / CHEST_WAIT_TIME)).toInt()
        val nextChestDays = diff % CHEST_WAIT_TIME

        return "${entry[MasteryChestTable.name]} - $currentChestCount (next in ${String.format("%.2f", nextChestDays)} days)"
    }
}
