package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.text.Font
import tornadofx.*
import ui.controllers.MainViewController
import ui.mock.AramMockController
import ui.mock.NormalMockController
import util.constants.ViewConstants
import util.constants.ViewConstants.DEFAULT_SPACING


class MainView: View("LoL Mastery Box Client") {
    val defaultGridView = find(DefaultGridView::class)
    val masteryAccountView = find(MasteryAccountView::class)

    val isLoggedInProperty = SimpleBooleanProperty()
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
                    paddingBottom = 6.0
                }

                scrollpane(fitToHeight = true) {
                    style = "-fx-background-color:transparent;"
                    minHeight = Font.getDefault().size + DEFAULT_SPACING * 2 + DEFAULT_SPACING * 2 // default_font_size + hbox_padding + label_padding

                    borderpane {
                        center = masteryAccountView.root
                    }
                }

                hbox {
                    alignment = Pos.BOTTOM_CENTER
                    paddingHorizontal = 8.0
                    spacing = 8.0

                    button("View Challenges").apply {
                        enableWhen { isLoggedInProperty }
                        action {
                            controller.leagueConnection.updateChallengesInfo()

                            val fragment = find<ChallengesView>()
                            fragment.setChallenges(controller.leagueConnection.challengeInfo, controller.leagueConnection.challengeInfo.keys.sortedBy { it })
                            fragment.openWindow()
                        }
                    }
                }
            }
        }
    }
}
