package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonAPI
import league.models.ChampionInfo
import league.models.enums.ImageCacheType
import league.models.enums.Role
import tornadofx.*
import util.constants.ViewConstants


class NormalGridView: View() {
    private val eternalDescriptionRegex = Regex(".*(\\([A-Z]\\)).*")

    val championListProperty = SimpleListProperty<ChampionInfo>()
    val currentRole = SimpleStringProperty(Role.ANY.name)

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
                cellWidth = ViewConstants.IMAGE_WIDTH
                cellHeight = ViewConstants.IMAGE_WIDTH

                cellCache {
                    stackpane {
                        alignment = Pos.TOP_CENTER

                        imageview(LeagueCommunityDragonAPI.getImage(ImageCacheType.CHAMPION, it.id)) { effect = LeagueCommunityDragonAPI.getChampionImageEffect(it) }

                        borderpane {
                            left = stackpane {
                                alignment = Pos.TOP_LEFT

                                label("Lvl ${it.level}") {
                                    textFill = Color.WHITE
                                    paddingHorizontal = 8

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

                                    style {
                                        backgroundColor += Color.BLACK
                                    }
                                }
                            }

                            if (it.eternal != null) {
                                bottom = stackpane {
                                    alignment = Pos.BOTTOM_LEFT

                                    vbox {
                                        it.eternal!!.statstones.forEach {
                                            val regexVal = if (eternalDescriptionRegex.matches(it.description))
                                                eternalDescriptionRegex.find(it.description)!!.groups[1]!!.value + " "
                                            else
                                                ""

                                            label(regexVal + "LVL ${it.formattedMilestoneLevel} ${it.formattedValue}/${it.nextMilestone}") {
                                                tooltip = tooltip(it.description) {
                                                    style {
                                                        font = Font.font(9.0)
                                                    }
                                                }
                                                font = Font.font(9.0)
                                                textFill = Color.WHITE
                                                paddingHorizontal = 8
                                                isWrapText

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
            }
        }

        bottom = vbox {
            alignment = Pos.BOTTOM_RIGHT
            paddingBottom = 24.0
            paddingRight = 24.0

            combobox<String>(currentRole, Role.values().map { it.name })
        }
    }
}
