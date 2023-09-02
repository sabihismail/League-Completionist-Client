package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import league.models.ChampionInfo
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_HORIZONTAL_COUNT
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
                maxCellsInRow = IMAGE_HORIZONTAL_COUNT
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellCache {
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showTokens to false)).root
                }
            }

            boldLabel("Your Team:")
            datagrid(teamChampionListProperty) {
                alignment = Pos.CENTER

                maxRows = 1
                maxCellsInRow = IMAGE_HORIZONTAL_COUNT
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH
                horizontalCellSpacing = IMAGE_SPACING_WIDTH

                cellCache {
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showTokens to false, ChampionFragment::showYou to true)).root
                }
            }
        }
    }
}
