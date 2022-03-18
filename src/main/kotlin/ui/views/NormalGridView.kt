package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.geometry.Pos
import league.LeagueCommunityDragonAPI
import league.models.ChampionInfo
import tornadofx.*
import ui.ViewConstants


class NormalGridView: View() {
    val championListProperty = SimpleListProperty<ChampionInfo>()

    override val root = vbox {
        alignment = Pos.CENTER

        label("Available Champions:")
        datagrid(championListProperty) {
            alignment = Pos.CENTER
            paddingBottom = 16.0
            prefHeight = 1000.0

            maxRows = 32
            maxCellsInRow = 5
            cellWidth = ViewConstants.IMAGE_WIDTH
            cellHeight = ViewConstants.IMAGE_WIDTH

            cellCache {
                imageview(LeagueCommunityDragonAPI.getChampionImage(it.id))  { effect = LeagueCommunityDragonAPI.getChampionImageEffect(it) }
            }
        }
    }
}