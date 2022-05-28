package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import league.models.ChampionInfo
import league.models.enums.Role
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.fragments.util.boldLabel
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    val currentRole = SimpleStringProperty(Role.ANY.name)

    private val allChampions = SimpleListProperty<ChampionInfo>()
    private val championListProperty = SimpleListProperty<ChampionInfo>()
    private val eternalsOnlyProperty = SimpleBooleanProperty(false)

    fun setChampions(lst: List<ChampionInfo>) {
        allChampions.value = FXCollections.observableList(lst)

        handleEternalsOnlyProperty(eternalsOnlyProperty.value)
    }

    private fun handleEternalsOnlyProperty(value: Boolean) {
        if (value) {
            championListProperty.value = FXCollections.observableList(allChampions.value.filter { championInfo -> championInfo.eternal != null })
        } else {
            championListProperty.value = FXCollections.observableList(allChampions.value.toList())
        }
    }

    override val root = borderpane {
        prefHeight = 1000.0

        center = vbox {
            alignment = Pos.CENTER

            stackpane {
                alignment = Pos.CENTER_LEFT
                paddingHorizontal = 16.0

                boldLabel("Available Champions:")
            }

            datagrid(championListProperty) {
                alignment = Pos.CENTER
                prefHeight = 600.0
                paddingBottom = 8.0

                maxRows = 32
                maxCellsInRow = 5
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellCache {
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it)).root
                }
            }
        }

        bottom = borderpane {
            right = vbox {
                alignment = Pos.BOTTOM_RIGHT
                paddingBottom = 24.0
                paddingRight = 24.0

                vbox {
                    alignment = Pos.BOTTOM_LEFT
                    spacing = 6.0

                    combobox<String>(currentRole, Role.values().map { it.name })
                    checkbox("Eternals Only", eternalsOnlyProperty).apply {
                        eternalsOnlyProperty.onChange { handleEternalsOnlyProperty(it) }
                    }
                }
            }
        }
    }
}
