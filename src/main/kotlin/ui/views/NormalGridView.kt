package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import league.models.ChampionInfo
import league.models.enums.ChallengeMappingEnum
import league.models.enums.Role
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    val currentRole = SimpleObjectProperty(Role.ANY)
    val currentChallenge = SimpleObjectProperty(ChallengeMappingEnum.NONE)

    private val allChampions = SimpleListProperty<ChampionInfo>()
    private val championListProperty = SimpleListProperty<ChampionInfo>()
    private val eternalsOnlyProperty = SimpleBooleanProperty(false)
    private val championSearchProperty = SimpleStringProperty("")

    fun setChampions(lst: List<ChampionInfo>) {
        allChampions.value = FXCollections.observableList(lst)

        setActiveChampions()
    }

    private fun setActiveChampions() {
        championListProperty.value = FXCollections.observableList(
            allChampions.value.filter { !eternalsOnlyProperty.value || it.eternalInfo.any { eternal -> eternal.value } }
                .filter { it.nameLower.contains(championSearchProperty.value.lowercase()) }
                .filter { currentChallenge.value == ChallengeMappingEnum.NONE || !it.challengesMapping[currentChallenge.value]!! }
        )
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
                    find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showEternals to false)).root
                }
            }

            textfield(championSearchProperty) {
                paddingRight = 16
                paddingBottom = 4

                textProperty().addListener { _, _, _ ->
                    setActiveChampions()
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
                        combobox(currentChallenge, ChallengeMappingEnum.values().toList())
                    }

                    checkbox("Eternals Only", eternalsOnlyProperty).apply {
                        eternalsOnlyProperty.onChange { setActiveChampions() }
                    }
                }
            }
        }
    }
}
