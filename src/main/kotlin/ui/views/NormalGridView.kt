package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import league.models.ChampionInfo
import league.models.enums.ChampionRole
import league.models.enums.Role
import league.models.json.Challenge
import tornadofx.*
import ui.SharedViewUtil
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_HORIZONTAL_COUNT
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    val currentLaneProperty = SimpleObjectProperty(Role.ANY)
    val currentChampionRoleProperty = SimpleObjectProperty(ChampionRole.ANY)
    val currentChallengeProperty = SimpleObjectProperty<Challenge>(null)

    private val allChampionsProperty = SimpleListProperty<ChampionInfo>()
    private val championListProperty = SimpleListProperty<ChampionInfo>()
    private val championSearchProperty = SimpleStringProperty("")

    private val allChallengesProperty = SimpleListProperty<Challenge>()
    private val challengesProperty = SimpleListProperty<Challenge>()
    private val skipCompleteChallengesProperty = SimpleBooleanProperty(true)

    private val eternalsOnlyProperty = SimpleBooleanProperty(false)
    private val loadEternalsProperty = SimpleBooleanProperty(false)
    private val sortByMaxEternalsProperty = SimpleBooleanProperty(false)

    fun setChampions(lst: List<ChampionInfo>) {
        allChampionsProperty.value = FXCollections.observableList(lst)

        setActiveChampions()
    }

    fun setChallenges(lst: List<Challenge>) {
        if (allChallengesProperty.isNotEmpty()) return

        allChallengesProperty.value = SharedViewUtil.addEmptyChallenge(lst)

        setActiveChallenges()
    }

    private fun setActiveChampions() {
        runAsync {
            SharedViewUtil.getActiveChampions(allChampionsProperty.value, role = currentChampionRoleProperty, search = championSearchProperty,
                eternalsOnly = eternalsOnlyProperty, challenges = currentChallengeProperty, sortByMaxEternals = sortByMaxEternalsProperty)
        } ui {
            championListProperty.value = it
        }
    }

    private fun setActiveChallenges() {
        runAsync {
            SharedViewUtil.getActiveChallenges(allChallengesProperty.value, skip = skipCompleteChallengesProperty)
        } ui {
            challengesProperty.value = it
            currentChallengeProperty.value = it?.first()
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
                maxCellsInRow = IMAGE_HORIZONTAL_COUNT
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellFormat {
                    graphic = find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showEternals to loadEternalsProperty.value,
                        ChampionFragment::showMaxEternal to sortByMaxEternalsProperty.value)).root
                }
            }

            hbox {
                hgrow = Priority.ALWAYS
                alignment = Pos.CENTER_LEFT
                paddingLeft = 10.0
                paddingRight = 24
                paddingBottom = 6
                spacing = 10.0

                label("Search by Name: ")

                textfield(championSearchProperty) {
                    hgrow = Priority.ALWAYS

                    textProperty().addListener { _, _, _ ->
                        setActiveChampions()
                    }
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

                        label("Lane: ")
                        combobox(currentLaneProperty, Role.entries)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Champion Role: ")
                        combobox(currentChampionRoleProperty, ChampionRole.entries)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Challenges (completed champs hidden): ")
                        combobox(currentChallengeProperty, challengesProperty)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT
                        spacing = 10.0

                        checkbox("Sort: Max Eternal", sortByMaxEternalsProperty).apply {
                            sortByMaxEternalsProperty.onChange { setActiveChampions() }
                        }

                        checkbox("Display Eternals", loadEternalsProperty).apply {
                            loadEternalsProperty.onChange { setActiveChampions() }
                        }

                        checkbox("Skip Completed Challenges", skipCompleteChallengesProperty).apply {
                            skipCompleteChallengesProperty.onChange { setActiveChallenges() }
                        }
                    }
                }
            }
        }
    }
}
