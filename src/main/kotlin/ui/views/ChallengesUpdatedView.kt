package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.scene.control.ScrollPane
import league.models.json.ChallengeInfo
import tornadofx.*
import ui.views.fragments.ChallengeFragment
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH

class ChallengesUpdatedView : View("LoL Challenges - Updated") {
    val challengesProgressedProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()
    val challengesUpgradedProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()

    private fun getDatagrid(elements: SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>): DataGrid<Pair<ChallengeInfo, ChallengeInfo>> {
        return datagrid(elements) {
            cellWidth = CHALLENGE_IMAGE_WIDTH
            cellHeight = CHALLENGE_IMAGE_WIDTH

            cellFormat {
                graphic = find<ChallengeFragment>(mapOf(
                    ChallengeFragment::challenge to it.second,
                    ChallengeFragment::bracketText to "${(it.second - it.first)}) (${it.second.nextLevelPoints}"
                )).root
            }
        }
    }

    override val root = borderpane {
        prefWidth = IMAGE_WIDTH * ELEMENTS_PER_ROW + IMAGE_SPACING_WIDTH * (ELEMENTS_PER_ROW + 2) + 4.0
        prefHeight = IMAGE_WIDTH * ROWS + IMAGE_SPACING_WIDTH * (ROWS + 2) + 4.0

        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            label("Upgraded")
            getDatagrid(challengesUpgradedProperty)

            label("Progressed")
            getDatagrid(challengesProgressedProperty)
        }
    }

    companion object {
        const val ELEMENTS_PER_ROW = 8
        const val ROWS = 7
    }
}