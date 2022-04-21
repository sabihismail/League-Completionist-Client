package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.LeagueCommunityDragonAPI
import league.models.ChampionInfo
import tornadofx.*
import ui.ViewConstants


class AramGridView: View() {
    val benchedChampionListProperty = SimpleListProperty<ChampionInfo>()
    val teamChampionListProperty = SimpleListProperty<ChampionInfo>()

    override val root = borderpane {
        prefHeight = 1000.0

        center = vbox {
            alignment = Pos.CENTER

            label("Available Champions:")
            datagrid(benchedChampionListProperty) {
                alignment = Pos.CENTER
                paddingBottom = 16.0

                maxRows = 2
                maxCellsInRow = 5
                cellWidth = ViewConstants.IMAGE_WIDTH
                cellHeight = ViewConstants.IMAGE_WIDTH

                cellCache {
                    stackpane {
                        alignment = Pos.TOP_LEFT

                        imageview(LeagueCommunityDragonAPI.getChampionImage(it.id)) { effect = LeagueCommunityDragonAPI.getChampionImageEffect(it) }

                        label("Lvl ${it.level}") {
                            textFill = Color.WHITE
                            paddingHorizontal = 8

                            style {
                                backgroundColor += Color.BLACK
                            }
                        }
                    }
                }
            }

            label("Your Team:")
            datagrid(teamChampionListProperty) {
                alignment = Pos.CENTER

                maxRows = 1
                maxCellsInRow = 5
                cellWidth = ViewConstants.IMAGE_WIDTH
                cellHeight = ViewConstants.IMAGE_WIDTH
                horizontalCellSpacing = ViewConstants.IMAGE_SPACING_WIDTH

                cellCache {
                    stackpane {
                        stackpane {
                            alignment = Pos.BOTTOM_CENTER

                            imageview(LeagueCommunityDragonAPI.getChampionImage(it.id)) {
                                effect = LeagueCommunityDragonAPI.getChampionImageEffect(it)
                            }

                            label("You") {
                                isVisible = it.isSummonerSelectedChamp
                                textFill = Color.WHITE
                                paddingHorizontal = 8

                                style {
                                    backgroundColor += Color.BLACK
                                }
                            }
                        }

                        stackpane {
                            alignment = Pos.TOP_LEFT

                            label("Lvl ${it.level}") {
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
}
