package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import league.models.enums.CacheType
import league.models.json.ChallengeInfo
import tornadofx.*
import ui.views.util.blackLabel
import util.KotlinExtensionUtil.toCommaSeparatedNumber
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH

class ChallengeFragment : Fragment() {
    val challenge: ChallengeInfo by param()
    val bracketText: String by param()

    override val root = stackpane {
        alignment = Pos.TOP_CENTER
        maxHeight = CHALLENGE_IMAGE_WIDTH

        imageview {
            fitWidth = CHALLENGE_IMAGE_WIDTH
            fitHeight = CHALLENGE_IMAGE_WIDTH

            image = LeagueCommunityDragonApi.getImage(CacheType.CHALLENGE, challenge.id?.toInt()!!, challenge.currentLevelImage).apply {
                effect = LeagueCommunityDragonApi.getChallengeImageEffect(challenge.currentLevel)
            }
        }

        blackLabel(challenge.descriptiveDescription)

        stackpane {
            vbox {
                alignment = Pos.BOTTOM_CENTER

                if (challenge.hasRewardTitle) {
                    blackLabel("Title: ${challenge.rewardTitle}" + if (challenge.rewardObtained) " ✓" else " (${challenge.rewardLevel.toString()[0]})")
                }

                blackLabel("${challenge.currentLevel} (${challenge.levelByThreshold}/${challenge.thresholds!!.count()})")

                blackLabel(
                    "${challenge.currentValue!!.toInt().toCommaSeparatedNumber()}/${challenge.nextThreshold!!.toInt().toCommaSeparatedNumber()} (+$bracketText)"
                ) {
                    tooltip(challenge.thresholdSummaryAnyTier) {
                        style {
                            font = Font.font(9.0)
                        }
                    }
                }

                if (challenge.thresholdSummaryOneLiner.isNotEmpty()) {
                    blackLabel("(${challenge.thresholdSummaryOneLiner})") {
                        tooltip(challenge.thresholdSummaryAnyTier) {
                            style {
                                font = Font.font(9.0)
                            }
                        }
                    }
                }
            }
        }
    }
}