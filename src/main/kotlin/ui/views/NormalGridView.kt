package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.LeagueCommunityDragonAPI
import league.models.ChampionInfo
import tornadofx.*
import ui.ViewConstants


class NormalGridView: View() {
    val championListProperty = SimpleListProperty<ChampionInfo>()
    val selectionState = SimpleBooleanProperty(true)

    override val root = borderpane {
        prefHeight = 1000.0

        center = vbox {
            alignment = Pos.CENTER

            label("Available Champions:")
            datagrid(championListProperty) {
                alignment = Pos.CENTER
                prefHeight = 600.0
                paddingBottom = 16.0

                maxRows = 32
                maxCellsInRow = 5
                cellWidth = ViewConstants.IMAGE_WIDTH
                cellHeight = ViewConstants.IMAGE_WIDTH

                cellCache {
                    stackpane {
                        alignment = Pos.TOP_CENTER

                        imageview(LeagueCommunityDragonAPI.getChampionImage(it.id)) { effect = LeagueCommunityDragonAPI.getChampionImageEffect(it) }

                        borderpane {
                            left = stackpane {
                                alignment = Pos.TOP_LEFT

                                label("Lvl ${it.level}") {
                                    textFill = Color.WHITE
                                    paddingHorizontal = 8

                                    style {
                                        backgroundColor += Color.BLACK
                                    }
                                }
                            }

                            right = stackpane {
                                alignment = Pos.TOP_RIGHT

                                label(
                                    when (it.level) {
                                        6 -> "${it.tokens}/3"
                                        5 -> "${it.tokens}/2"
                                        else -> ""
                                    }
                                ) {
                                    isVisible = listOf(5, 6).contains(it.level)
                                    textFill = Color.WHITE
                                    paddingHorizontal = 8

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

        bottom = vbox {
            alignment = Pos.BOTTOM_RIGHT
            paddingAll = 24.0

            checkbox("Sort by Role", selectionState)
        }
    }
}
