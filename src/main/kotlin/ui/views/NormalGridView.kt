package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import league.models.ChampionInfo
import league.models.enums.ChallengesMappingEnum
import league.models.enums.Role
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    val currentRole = SimpleObjectProperty(Role.ANY)
    val currentChallenge = SimpleObjectProperty(ChallengesMappingEnum.NONE)

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
                    alignment = Pos.BOTTOM_RIGHT
                    spacing = 6.0

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Role: ")
                        combobox(currentRole, Role.values().toList())
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Challenge: ")
                        combobox(currentChallenge, ChallengesMappingEnum.values().toList())
                    }

                    checkbox("Eternals Only", eternalsOnlyProperty).apply {
                        eternalsOnlyProperty.onChange { handleEternalsOnlyProperty(it) }
                    }
                }
            }
        }
    }
}
