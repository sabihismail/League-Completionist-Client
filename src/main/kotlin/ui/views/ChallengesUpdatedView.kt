package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.scene.control.ScrollPane
import league.models.json.ChallengeInfo
import tornadofx.View
import tornadofx.borderpane
import tornadofx.datagrid
import tornadofx.scrollpane
import ui.views.fragments.ChallengeFragment
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH

class ChallengesUpdatedView : View("LoL Challenges - Updated") {
    val challengesProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()

    override val root = borderpane {
        prefWidth = IMAGE_WIDTH * ELEMENTS_PER_ROW + IMAGE_SPACING_WIDTH * (ELEMENTS_PER_ROW + 2) + 4.0
        prefHeight = IMAGE_WIDTH * ROWS + IMAGE_SPACING_WIDTH * (ROWS + 2) + 4.0

        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            datagrid(challengesProperty) {
                cellWidth = CHALLENGE_IMAGE_WIDTH
                cellHeight = CHALLENGE_IMAGE_WIDTH

                cellFormat {
                    graphic = find<ChallengeFragment>(mapOf(
                        ChallengeFragment::challenge to it.second,
                        ChallengeFragment::bracketText to (it.second.currentValue!! - it.first.currentValue!!).toInt().toString()
                    )).root
                }
            }
        }
    }

    companion object {
        const val ELEMENTS_PER_ROW = 8
        const val ROWS = 7
    }
}