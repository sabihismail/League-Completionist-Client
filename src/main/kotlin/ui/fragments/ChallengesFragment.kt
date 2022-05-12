package ui.fragments

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import league.LeagueCommunityDragonAPI
import league.models.ChallengeInfo
import league.models.enums.ImageCacheType
import tornadofx.*
import util.constants.ViewConstants

class ChallengesFragment : Fragment("LoL Challenges") {
    val challengeKeys = SimpleListProperty<String>()
    val challengesMap = SimpleMapProperty<String, List<ChallengeInfo>>()

    override val root = vbox {
        alignment = Pos.CENTER
        minWidth = 800.0
        minHeight = 800.0

        datagrid(challengeKeys) {
            maxCellsInRow = 1
            cellWidth = 800.0
            minHeight = 800.0

            cellCache {
                label("$it:") {
                    paddingHorizontal = 8
                    textFill = Color.WHITE
                    font = Font.font("Arial", FontWeight.BOLD, 16.0)
                    textAlignment = TextAlignment.LEFT

                    style {
                        backgroundColor += Color.BLACK
                    }
                }

                datagrid(challengesMap[it]) {
                    alignment = Pos.CENTER
                    paddingBottom = 16.0
                    minWidth = (ViewConstants.IMAGE_WIDTH + this.horizontalCellSpacing) * challengesMap[it]!!.size

                    maxCellsInRow = challengesMap[it]!!.count()
                    cellWidth = ViewConstants.IMAGE_WIDTH
                    cellHeight = ViewConstants.IMAGE_WIDTH

                    cellCache {
                        stackpane {
                            alignment = Pos.TOP_CENTER

                            imageview {
                                val currentLevel = if (it.currentLevel.lowercase() == "none") "iron" else it.currentLevel.lowercase()
                                val img = LeagueCommunityDragonAPI.getImage(ImageCacheType.CHALLENGE, it.id, currentLevel).apply {
                                    effect = LeagueCommunityDragonAPI.getChallengeImageEffect(it)
                                }

                                image = img
                                fitWidth = ViewConstants.IMAGE_WIDTH
                                fitHeight = ViewConstants.IMAGE_WIDTH
                            }

                            label(it.description!!) {
                                paddingHorizontal = 8
                                textFill = Color.WHITE
                                font = Font.font("Arial", FontWeight.BOLD, 10.0)
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