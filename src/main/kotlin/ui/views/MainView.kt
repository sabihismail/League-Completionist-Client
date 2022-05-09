package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import tornadofx.*
import ui.controllers.MainViewController
import ui.mock.AramMockController
import ui.mock.NormalMockController
import util.constants.ViewConstants


class MainView: View() {
    val defaultGridView = find(DefaultGridView::class)

    val summonerProperty = SimpleStringProperty()
    val chestProperty = SimpleStringProperty()

    val clientStateProperty = SimpleStringProperty()
    val gameModeProperty = SimpleStringProperty()

    val masteryAccountView = find(MasteryAccountView::class)

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

                scrollpane(fitToHeight = true) {
                    vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                    style = "-fx-background-color:transparent;"

                    borderpane {
                        center = masteryAccountView.root
                    }
                }
            }
        }
    }
}
