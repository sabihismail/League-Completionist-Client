package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.CacheType
import tornadofx.*
import ui.views.fragments.EternalsFragment
import ui.views.fragments.util.blackLabel
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH


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
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellCache {
                    stackpane {
                        alignment = Pos.TOP_LEFT

                        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, it.id)) { effect = LeagueCommunityDragonApi.getChampionImageEffect(it) }

                        blackLabel("Lvl ${it.level}", textAlignment = TextAlignment.LEFT)
                    }
                }
            }

            label("Your Team:")
            datagrid(teamChampionListProperty) {
                alignment = Pos.CENTER

                maxRows = 1
                maxCellsInRow = 5
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH
                horizontalCellSpacing = IMAGE_SPACING_WIDTH

                cellCache {
                    stackpane {
                        stackpane {
                            alignment = Pos.BOTTOM_CENTER

                            imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, it.id)) {
                                effect = LeagueCommunityDragonApi.getChampionImageEffect(it)
                            }

                            blackLabel("You", textAlignment = TextAlignment.LEFT) {
                                isVisible = it.isSummonerSelectedChamp
                            }
                        }

                        stackpane {
                            alignment = Pos.TOP_LEFT

                            blackLabel("Lvl ${it.level}", textAlignment = TextAlignment.LEFT)
                        }

                        if (it.eternal != null) {
                            alignment = Pos.BOTTOM_LEFT

                            bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to it.eternal, EternalsFragment::fontSizeIn to 12.0)).root
                        }
                    }
                }
            }
        }
    }
}
