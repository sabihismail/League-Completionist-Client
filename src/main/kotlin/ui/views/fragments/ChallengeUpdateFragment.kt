package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import league.models.enums.CacheType
import league.models.json.ChallengeInfo
import tornadofx.*
import ui.views.fragments.util.blackLabel
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH

class ChallengeUpdateFragment : Fragment() {
    val challenge: ChallengeInfo by param()
    val challengeUpdate: ChallengeInfo by param()

    override val root = stackpane {
        alignment = Pos.TOP_CENTER
        maxHeight = CHALLENGE_IMAGE_WIDTH

        imageview {
            fitWidth = CHALLENGE_IMAGE_WIDTH
            fitHeight = CHALLENGE_IMAGE_WIDTH

            image = LeagueCommunityDragonApi.getImage(CacheType.CHALLENGE, challengeUpdate.id!!, challengeUpdate.currentLevelImage).apply {
                effect = LeagueCommunityDragonApi.getChallengeImageEffect(challengeUpdate)
            }
        }

        blackLabel(challengeUpdate.description!!)

        stackpane {
            vbox {
                alignment = Pos.BOTTOM_CENTER

                if (challengeUpdate.hasRewardTitle) {
                    blackLabel("Title: ${challengeUpdate.rewardTitle}" + if (challengeUpdate.rewardObtained) " ✓" else " (${challengeUpdate.rewardLevel.toString()[0]})")
                }

                blackLabel("${challengeUpdate.currentLevel} (${challengeUpdate.levelByThreshold}/${challengeUpdate.thresholds!!.count()})")

                blackLabel("${challengeUpdate.currentValue!!.toInt()}/${challengeUpdate.nextThreshold!!.toInt()} " +
                        "(+${challengeUpdate.currentValue!! - challenge.currentValue!!})") {
                    tooltip(challengeUpdate.thresholdSummary) {
                        style {
                            font = Font.font(9.0)
                        }
                    }
                }
            }
        }
    }
}