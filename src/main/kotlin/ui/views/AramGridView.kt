package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.ImageCacheType
import tornadofx.*
import ui.views.fragments.EternalsFragment
import util.constants.ViewConstants


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

                        imageview(LeagueCommunityDragonApi.getImage(ImageCacheType.CHAMPION, it.id)) { effect = LeagueCommunityDragonApi.getChampionImageEffect(it) }

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

                            imageview(LeagueCommunityDragonApi.getImage(ImageCacheType.CHAMPION, it.id)) {
                                effect = LeagueCommunityDragonApi.getChampionImageEffect(it)
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

                        if (it.eternal != null) {
                            alignment = Pos.BOTTOM_LEFT

                            bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to it.eternal)).root
                        }
                    }
                }
            }
        }
    }
}
