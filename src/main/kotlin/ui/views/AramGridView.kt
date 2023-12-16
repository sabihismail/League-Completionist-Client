package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import league.models.ChampionInfo
import league.models.ChampionSelectInfo
import league.models.enums.GameMode
import league.models.json.Challenge
import tornadofx.*
import ui.SharedViewUtil
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_HORIZONTAL_COUNT
import util.constants.ViewConstants.IMAGE_SPACING_WIDTH
import util.constants.ViewConstants.IMAGE_WIDTH


class AramGridView: View() {
    val currentChallengeProperty = SimpleObjectProperty<Challenge>(null)
    val benchedChampionListProperty = SimpleListProperty<ChampionInfo>()
    val teamChampionListProperty = SimpleListProperty<ChampionInfo>()

    private val allChallengesProperty = SimpleListProperty<Challenge>()
    private val completableChallengesProperty = SimpleListProperty<Challenge>()
    private val skipCompleteChallengesProperty = SimpleBooleanProperty(true)

    fun setChampions(championSelectInfo: ChampionSelectInfo) {
        runAsync {
            Pair(
                SharedViewUtil.getActiveChampions(championSelectInfo.benchedChampions, challenges = currentChallengeProperty),
                SharedViewUtil.getActiveChampions(championSelectInfo.teamChampions, challenges = currentChallengeProperty),
            )
        } ui {
            benchedChampionListProperty.value = it.first
            teamChampionListProperty.value = it.second
        }
    }

    fun setChallenges(lst: List<Challenge>) {
        if (allChallengesProperty.isNotEmpty()) return

        allChallengesProperty.value = SharedViewUtil.addEmptyChallenge(lst)

        setActiveChallenges()
    }

    private fun setActiveChallenges() {
        runAsync {
            SharedViewUtil.getActiveChallenges(allChallengesProperty.value, gameMode = GameMode.ARAM, skip = skipCompleteChallengesProperty)
        } ui {
            completableChallengesProperty.value = it
            currentChallengeProperty.value = it?.first()
        }
    }

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

                        label("Challenges (completed champs hidden): ")
                        combobox(currentChallengeProperty, completableChallengesProperty)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT
                        spacing = 10.0

                        checkbox("Skip Completed Challenges", skipCompleteChallengesProperty).apply {
                            skipCompleteChallengesProperty.onChange { setActiveChallenges() }
                        }
                    }
                }
            }
        }
    }
}
