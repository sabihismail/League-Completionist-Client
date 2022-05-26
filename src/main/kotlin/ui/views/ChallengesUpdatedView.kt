package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import league.models.json.ChallengeInfo
import tornadofx.View
import tornadofx.datagrid
import tornadofx.scrollpane
import tornadofx.vbox
import ui.views.fragments.ChallengeUpdateFragment
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH

class ChallengesUpdatedView : View("LoL Updated Challenges") {
    val challengesProperty = SimpleListProperty<Pair<ChallengeInfo, ChallengeInfo>>()

    override val root = vbox {
        scrollpane(fitToWidth = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            datagrid(challengesProperty) {
                alignment = Pos.CENTER
                cellWidth = CHALLENGE_IMAGE_WIDTH
                cellHeight = CHALLENGE_IMAGE_WIDTH
                cellFormat {
                    graphic = find<ChallengeUpdateFragment>(mapOf(ChallengeUpdateFragment::challenge to it.first, ChallengeUpdateFragment::challengeUpdate to it.second))
                        .root
                }
            }
        }
    }
}