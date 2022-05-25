package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import generated.LolGameflowGameflowPhase
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.text.Font
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import league.models.enums.GameMode
import league.models.enums.SummonerStatus
import tornadofx.*
import ui.controllers.MainViewController
import ui.mock.AramMockController
import ui.mock.NormalMockController
import ui.views.fragments.EternalsFragment
import util.constants.ViewConstants
import util.constants.ViewConstants.DEFAULT_SPACING


class MainView: View("LoL Mastery Box Client") {
    val defaultGridView = find(DefaultGridView::class)
    val masteryAccountView = find(MasteryAccountView::class)
    val currentEternalView = find<EternalsFragment>(mapOf(EternalsFragment::eternal to null, EternalsFragment::fontSizeIn to 12.0))

    val summonerProperty = SimpleObjectProperty(SummonerInfo())
    val chestProperty = SimpleObjectProperty(MasteryChestInfo())
    val clientStateProperty = SimpleObjectProperty(LolGameflowGameflowPhase.NONE)
    val gameModeProperty = SimpleObjectProperty(GameMode.NONE)

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

                label(summonerProperty.select { it.toDisplayString().toProperty() })
                label(chestProperty.select { "Available chests: ${it.chestCount} (next one in ${it.remainingStr} days)".toProperty() })
                label(clientStateProperty.select { "Client State: ${it.name}".toProperty() })
                label(gameModeProperty.select { "Game Mode: $it".toProperty() })
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
                    currentEternalView.root
                }

                separator {
                    paddingBottom = 6.0
                }

                scrollpane(fitToHeight = true) {
                    style = "-fx-background-color:transparent;"
                    minHeight = Font.getDefault().size + DEFAULT_SPACING * 2 + DEFAULT_SPACING * 2

                    borderpane {
                        center = masteryAccountView.root
                    }
                }

                hbox {
                    alignment = Pos.BOTTOM_CENTER
                    paddingHorizontal = 8.0
                    spacing = 8.0

                    button("View Challenges").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            controller.leagueConnection.updateChallengesInfo()
                            controller.updateChallengesView()

                            find<ChallengesView>().openWindow(owner = null)
                        }
                    }

                    button("View Last Game's Challenges").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            controller.leagueConnection.updateChallengesInfo()
                            controller.updateChallengesUpdatedView()

                            find<ChallengesUpdatedView>().openWindow(owner = null)
                        }
                    }

                    /*
                    button("Settings").apply {
                        action {
                            find<SettingsView>(mapOf(SettingsView::accountList to FXCollections.observableList(DatabaseImpl.getHextechAccounts())))
                                .openWindow(owner = null)
                        }
                    }
                    */
                }
            }
        }
    }
}
