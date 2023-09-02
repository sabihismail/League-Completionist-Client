package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import DEBUG_FAKE_UI_DATA_UPDATED_CHALLENGES
import generated.LolGameflowGameflowPhase
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.control.TextInputDialog
import javafx.scene.text.Font
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import league.models.enums.GameMode
import league.models.enums.SummonerStatus
import tornadofx.*
import ui.controllers.MainViewController
import ui.mock.AramMockController
import ui.mock.NormalMockController
import ui.mock.extra.ChallengesUpdatedMockController
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.ViewUtil
import util.constants.ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
import util.constants.ViewConstants.CHAMPION_STATUS_NOT_OWNED_COLOR
import util.constants.ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
import util.constants.ViewConstants.DEFAULT_SPACING
import util.constants.ViewConstants.IMAGE_HORIZONTAL_COUNT
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH
import util.constants.ViewConstants.SCROLLBAR_WIDTH


class MainView: View("League Mastery Box Client") {
    val defaultGridView = find(DefaultGridView::class)
    val masteryAccountView = find(MasteryAccountView::class)
    var currentChampionView = find(ChampionFragment::class)

    val summonerProperty = SimpleObjectProperty(SummonerInfo())
    val chestProperty = SimpleObjectProperty(MasteryChestInfo())
    val clientStateProperty = SimpleObjectProperty(LolGameflowGameflowPhase.NONE)
    val gameModeProperty = SimpleObjectProperty(GameMode.NONE)

    @Suppress("unused")
    private val controller = find(
        if (DEBUG_FAKE_UI_DATA_ARAM) AramMockController::class
        else if (DEBUG_FAKE_UI_DATA_NORMAL) NormalMockController::class
        else if (DEBUG_FAKE_UI_DATA_UPDATED_CHALLENGES) ChallengesUpdatedMockController::class
        else MainViewController::class
    )

    override val root = vbox {
        prefWidth = APP_WIDTH
        prefHeight = APP_HEIGHT

        borderpane {
            top = hbox {
                paddingBottom = 16.0
                paddingHorizontal = 16.0

                borderpane {
                    top = boldLabel("You:")
                    left = currentChampionView.root
                }

                vbox {
                    paddingTop = 16.0
                    paddingLeft = 8.0
                    alignment = Pos.TOP_LEFT

                    fitToParentWidth()
                    label(summonerProperty.select { it.toDisplayString().toProperty() })
                    label(chestProperty.select { "Available chests: ${it.chestCount} (next one in ${it.remainingStr} days)".toProperty() })
                    label(clientStateProperty.select { "Client State: ${it.name}".toProperty() })
                    label(gameModeProperty.select { "Game Mode: $it".toProperty() })
                }
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
                        label("Mastery Chest Available")
                    }
                    hbox {
                        spacing = 4.0
                        alignment = Pos.CENTER_LEFT

                        rectangle(0, 0, 20, 20) {
                            fill = CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
                        }
                        label("Mastery Chest Already Obtained")
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

                    button("Friends").apply {
                        action {
                            val stage = find<FriendsView>().openWindow(owner = null)
                            ViewUtil.moveToScreen(stage)
                        }
                    }

                    button("View Challenges").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            controller.leagueConnection.updateChallengesInfo()
                            controller.updateChallengesView()

                            val stage = find<ChallengesView>().openWindow(owner = null)
                            ViewUtil.moveToScreen(stage)
                        }
                    }

                    button("View Levels").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            controller.leagueConnection.updateChallengesInfo()
                            controller.updatedChallengeLevelsView()

                            val stage = find<ChallengesLevelView>().openWindow(owner = null)
                            ViewUtil.moveToScreen(stage)
                        }
                    }

                    button("View Last Game's Challenges").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            controller.leagueConnection.updateChallengesInfo()
                            controller.updateChallengesUpdatedView()

                            val stage = find<ChallengesUpdatedView>().openWindow(owner = null)
                            ViewUtil.moveToScreen(stage)
                        }
                    }

                    button("Execute Endpoint").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            val dialog = TextInputDialog()
                            dialog.title = "Input Command"
                            dialog.headerText = "Input Command"

                            val result = dialog.showAndWait()
                            if (result.isPresent) {
                                controller.leagueConnection.executeCommand(result.get())
                            }
                        }
                    }

                    button("Debug").apply {
                        enableWhen { summonerProperty.select { (it.status == SummonerStatus.LOGGED_IN_AUTHORIZED).toProperty() } }
                        action {
                            val stage = find<DebugView>().openWindow(owner = null)
                            ViewUtil.moveToScreen(stage)
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

    companion object {
        const val APP_WIDTH = IMAGE_WIDTH * IMAGE_HORIZONTAL_COUNT + IMAGE_SPACING_WIDTH * (IMAGE_HORIZONTAL_COUNT * 2) + (IMAGE_HORIZONTAL_COUNT * 2) + SCROLLBAR_WIDTH
        const val APP_HEIGHT = 960.0
    }
}
