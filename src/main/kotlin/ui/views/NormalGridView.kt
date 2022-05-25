package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.CacheType
import league.models.enums.Role
import tornadofx.*
import ui.views.fragments.EternalsFragment
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    private val allChampions = SimpleListProperty<ChampionInfo>()
    private val championListProperty = SimpleListProperty<ChampionInfo>()
    private val eternalsOnlyProperty = SimpleBooleanProperty(false)

    val currentRole = SimpleStringProperty(Role.ANY.name)

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

            label("Available Champions:")
            datagrid(championListProperty) {
                alignment = Pos.CENTER
                prefHeight = 600.0
                paddingBottom = 16.0

                maxRows = 32
                maxCellsInRow = 5
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellCache {
                    stackpane {
                        alignment = Pos.TOP_CENTER

                        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, it.id)) { effect = LeagueCommunityDragonApi.getChampionImageEffect(it) }

                        borderpane {
                            left = stackpane {
                                alignment = Pos.TOP_LEFT

                                val txt = if (it.level in 1..4)
                                    " (${"%.2f".format((it.currentMasteryPoints.toDouble()/(it.nextLevelMasteryPoints + it.currentMasteryPoints)) * 100)}%)"
                                else
                                    ""
                                label("Lvl ${it.level}" + txt) {
                                    textFill = Color.WHITE
                                    paddingHorizontal = 8
                                    font = Font.font(11.6)

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
                                    font = Font.font(11.6)

                                    style {
                                        backgroundColor += Color.BLACK
                                    }
                                }
                            }

                            if (it.eternal != null) {
                                bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to it.eternal.toProperty(), EternalsFragment::fontSizeIn to 9.0)).root
                            }
                        }
                    }
                }
            }
        }

        bottom = borderpane {
            right = vbox {
                alignment = Pos.BOTTOM_RIGHT
                paddingBottom = 24.0
                paddingRight = 24.0

                combobox<String>(currentRole, Role.values().map { it.name })
                checkbox("Eternals Only", eternalsOnlyProperty).apply {
                    eternalsOnlyProperty.onChange { handleEternalsOnlyProperty(it) }
                }
            }
        }
    }
}
