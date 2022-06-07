@file:Suppress("DuplicatedCode")

package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.scene.control.ScrollPane
import league.models.json.ChallengeInfo
import tornadofx.*
import ui.views.fragments.ChallengeFragment
import ui.views.util.blackLabel
import ui.views.util.blackLabelObs
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.DEFAULT_SPACING
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH

class ChallengesUpdatedView : View("LoL Challenges - Updated") {
    val challengesProgressedProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()
    val challengesUpgradedProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()

    private fun getBracketTest(it: Pair<ChallengeInfo, ChallengeInfo>): String {
        return "${it.second.pointsDifference}) (+${(it.second - it.first)}"
    }

    override val root = borderpane {
        prefWidth = WINDOW_WIDTH
        prefHeight = IMAGE_WIDTH * ROWS + IMAGE_SPACING_WIDTH * (ROWS + 2) + 4.0 + (DEFAULT_SPACING * 4) + (LABEL_HEIGHT * 2)

        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            gridpane {
                fitToParentWidth()

                constraintsForRow(0).prefHeight = LABEL_HEIGHT
                constraintsForRow(1).prefHeight = IMAGE_WIDTH * (ROWS - 1) + IMAGE_SPACING_WIDTH * (ROWS + 2 - 1) + 4.0
                constraintsForRow(2).prefHeight = LABEL_HEIGHT
                constraintsForRow(3).prefHeight = IMAGE_WIDTH * 1 + IMAGE_SPACING_WIDTH * (1 + 2) + 4.0

                constraintsForColumn(0).prefWidth = WINDOW_WIDTH

                row {
                    fitToParentWidth()
                    blackLabel("Progressed", fontSize = 14.0)
                }
                row {
                    fitToParentWidth()
                    datagrid(challengesProgressedProperty) {
                        cellWidth = CHALLENGE_IMAGE_WIDTH
                        cellHeight = CHALLENGE_IMAGE_WIDTH

                        cellFormat {
                            graphic = find<ChallengeFragment>(
                                mapOf(ChallengeFragment::challenge to it.second, ChallengeFragment::bracketText to getBracketTest(it))
                            ).root
                        }
                    }
                }

                row {
                    fitToParentWidth()
                    blackLabelObs(challengesUpgradedProperty.select { "Upgraded - (+${it.sumOf { fragPair -> 
                        if (fragPair.first.currentLevel != fragPair.second.currentLevel) {
                            fragPair.first.pointsDifference
                        } else {
                            0
                        }
                    }})".toProperty() }, fontSize = 14.0)
                }
                row {
                    fitToParentWidth()
                    datagrid(challengesUpgradedProperty) {
                        cellWidth = CHALLENGE_IMAGE_WIDTH
                        cellHeight = CHALLENGE_IMAGE_WIDTH
                        maxRows = 1

                        cellFormat {
                            graphic = find<ChallengeFragment>(
                                mapOf(ChallengeFragment::challenge to it.second, ChallengeFragment::bracketText to getBracketTest(it))
                            ).root
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ELEMENTS_PER_ROW = 8
        const val ROWS = 7
        const val WINDOW_WIDTH = IMAGE_WIDTH * ELEMENTS_PER_ROW + IMAGE_SPACING_WIDTH * (ELEMENTS_PER_ROW + 2) + 4.0

        const val LABEL_HEIGHT = 30.0
    }
}