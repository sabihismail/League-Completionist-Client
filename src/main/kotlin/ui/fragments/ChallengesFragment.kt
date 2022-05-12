package ui.fragments

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import league.LeagueCommunityDragonAPI
import league.models.ChallengeCategory
import league.models.ChallengeInfo
import league.models.ChallengeInfoRank
import league.models.enums.ImageCacheType
import tornadofx.*
import util.constants.ViewConstants
import kotlin.math.roundToInt

class ChallengesFragment : Fragment("LoL Challenges") {
    val challengeKeys = SimpleListProperty<ChallengeCategory>()
    val challengesMap = SimpleMapProperty<ChallengeCategory, List<ChallengeInfo>>()

    override val root = vbox {
        alignment = Pos.CENTER
        minWidth = 820.0
        minHeight = 972.0

        datagrid(challengeKeys) {
            maxCellsInRow = 1
            cellWidth = 800.0
            cellHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH + verticalCellSpacing + 16.0 + 30.0
            minHeight = 980.0
            verticalCellSpacing = 2.0

            cellCache {
                vbox {
                    alignment = Pos.CENTER_LEFT
                    maxHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH + verticalCellSpacing + 16.0 + 30.0
                    minHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH + verticalCellSpacing + 16.0 + 30.0

                    label("$it:") {
                        textFill = Color.WHITE
                        font = Font.font(15.0)
                        textAlignment = TextAlignment.LEFT

                        fitToParentWidth()
                        style {
                            backgroundColor += Color.BLACK
                        }
                    }

                    datagrid(challengesMap[it]) {
                        alignment = Pos.CENTER
                        maxRows = 1

                        cellWidth = ViewConstants.CHALLENGE_IMAGE_WIDTH
                        cellHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH

                        cellCache {
                            stackpane {
                                alignment = Pos.TOP_CENTER
                                maxHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH

                                imageview {
                                    val currentLevel = if (it.currentLevel == ChallengeInfoRank.NONE) "iron" else it.currentLevel!!.name.lowercase()
                                    val img = LeagueCommunityDragonAPI.getImage(ImageCacheType.CHALLENGE, it.id!!, currentLevel).apply {
                                        effect = LeagueCommunityDragonAPI.getChallengeImageEffect(it)
                                    }

                                    image = img
                                    fitWidth = ViewConstants.CHALLENGE_IMAGE_WIDTH
                                    fitHeight = ViewConstants.CHALLENGE_IMAGE_WIDTH
                                }

                                label(it.description!!) {
                                    paddingHorizontal = 8
                                    textFill = Color.WHITE
                                    font = Font.font(9.0)
                                    textAlignment = TextAlignment.CENTER
                                    isWrapText = true

                                    style {
                                        backgroundColor += Color.BLACK
                                    }
                                }

                                stackpane {
                                    alignment = Pos.BOTTOM_CENTER

                                    label("${it.currentLevel} (${it.currentThreshold!!.roundToInt()}/${it.nextThreshold!!.roundToInt()})") {
                                        paddingHorizontal = 8
                                        textFill = Color.WHITE
                                        font = Font.font(9.0)
                                        textAlignment = TextAlignment.CENTER
                                        isWrapText = true

                                        style {
                                            backgroundColor += Color.BLACK
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        hbox {
            checkbox("Hide")
        }
    }
}