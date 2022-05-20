package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonApi
import league.models.enums.CacheType
import league.models.enums.ChallengeLevel
import league.models.json.ChallengeInfo
import tornadofx.*
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH

class ChallengeFragment : Fragment() {
    val challenge: ChallengeInfo by param()

    override val root = stackpane {
        alignment = Pos.TOP_CENTER
        maxHeight = CHALLENGE_IMAGE_WIDTH

        imageview {
            fitWidth = CHALLENGE_IMAGE_WIDTH
            fitHeight = CHALLENGE_IMAGE_WIDTH

            val currentLevel = if (challenge.currentLevel == ChallengeLevel.NONE)
                ChallengeLevel.IRON.name.lowercase()
            else
                challenge.currentLevel!!.name.lowercase()

            image = LeagueCommunityDragonApi.getImage(CacheType.CHALLENGE, challenge.id!!, currentLevel).apply {
                effect = LeagueCommunityDragonApi.getChallengeImageEffect(challenge)
            }
        }

        label(challenge.description!!) {
            textFill = Color.WHITE
            textAlignment = TextAlignment.CENTER
            isWrapText = true
            paddingHorizontal = 8
            font = Font.font(9.0)

            style {
                backgroundColor += Color.BLACK
            }
        }

        stackpane {
            vbox {
                alignment = Pos.BOTTOM_CENTER

                if (challenge.hasRewardTitle) {
                    label("Title: ${challenge.rewardTitle}" + if (challenge.rewardObtained) " ✓" else " (${challenge.rewardLevel.toString()[0]})") {
                        textFill = Color.WHITE
                        textAlignment = TextAlignment.CENTER
                        isWrapText = true
                        paddingHorizontal = 8
                        font = Font.font(9.0)

                        style {
                            backgroundColor += Color.BLACK
                        }
                    }
                }

                label("${challenge.currentLevel} (${challenge.thresholds!!.keys.sorted().indexOf(challenge.currentLevel) + 1}/${challenge.thresholds!!.count()})") {
                    textFill = Color.WHITE
                    textAlignment = TextAlignment.CENTER
                    isWrapText = true
                    paddingHorizontal = 8
                    font = Font.font(9.0)

                    style {
                        backgroundColor += Color.BLACK
                    }
                }

                label("${challenge.currentValue!!.toInt()}/${challenge.nextThreshold!!.toInt()} (+${challenge.nextLevelPoints})") {
                    textFill = Color.WHITE
                    textAlignment = TextAlignment.CENTER
                    isWrapText = true
                    paddingHorizontal = 8
                    font = Font.font(9.0)

                    tooltip(challenge.thresholdSummary) {
                        style {
                            font = Font.font(9.0)
                        }
                    }

                    style {
                        backgroundColor += Color.BLACK
                    }
                }
            }
        }
    }
}