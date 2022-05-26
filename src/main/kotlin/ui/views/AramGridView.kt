package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import league.models.ChampionInfo
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.fragments.util.boldLabel
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH


class AramGridView: View() {
    val benchedChampionListProperty = SimpleListProperty<ChampionInfo>()
    val teamChampionListProperty = SimpleListProperty<ChampionInfo>()

    override val root = borderpane {
        prefHeight = 1000.0

        center = vbox {
            alignment = Pos.CENTER

            boldLabel("Available Champions:")
            datagrid(benchedChampionListProperty) {
                alignment = Pos.CENTER
                paddingBottom = 16.0

                maxRows = 2
                maxCellsInRow = 5
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellCache {
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showTokens to false)).root
                    /*
                    stackpane {
                        alignment = Pos.TOP_LEFT

                        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, it.id)) { effect = LeagueCommunityDragonApi.getChampionImageEffect(it) }

                        blackLabel("Lvl ${it.level}", textAlignment = TextAlignment.LEFT, fontSize = 11.0)

                        if (it.eternal != null) {
                            find<EternalsFragment>(mapOf(EternalsFragment::eternal to it.eternal.toProperty(), EternalsFragment::fontSizeIn to 12.0)).root
                        }
                    }
                     */
                }
            }

            boldLabel("Your Team:")
            datagrid(teamChampionListProperty) {
                alignment = Pos.CENTER

                maxRows = 1
                maxCellsInRow = 5
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH
                horizontalCellSpacing = IMAGE_SPACING_WIDTH

                cellCache {
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showTokens to false, ChampionFragment::showYou to true))
                        .root
                    /*
                    stackpane {
                        stackpane {
                            alignment = Pos.BOTTOM_CENTER

                            imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, it.id)) {
                                effect = LeagueCommunityDragonApi.getChampionImageEffect(it)
                            }

                            blackLabel("You", textAlignment = TextAlignment.LEFT, fontSize = 11.0) {
                                isVisible = it.isSummonerSelectedChamp
                            }
                        }

                        stackpane {
                            alignment = Pos.TOP_LEFT

                            blackLabel("Lvl ${it.level}", textAlignment = TextAlignment.LEFT, fontSize = 11.0)

                            if (it.eternal != null) {
                                find<EternalsFragment>(mapOf(EternalsFragment::eternal to it.eternal.toProperty(), EternalsFragment::fontSizeIn to 12.0)).root
                            }
                        }
                    }
                     */
                }
            }
        }
    }
}
