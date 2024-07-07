package ui.views

import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import league.api.LeagueCommunityDragonApi
import league.models.ChallengeLevelInfo
import league.models.enums.CacheType
import tornadofx.*
import ui.views.util.blackLabel
import util.KotlinExtensionUtil.toCommaSeparatedNumber
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.DEFAULT_SPACING
import util.constants.ViewConstants.SCROLLBAR_HEIGHT
import kotlin.math.ceil


class ChallengesLevelView : View("LoL Level Challenges") {
    private val challengeLevelsProperty = SimpleListProperty<ChallengeLevelInfo>()

    private lateinit var verticalRow: ScrollPane
    private lateinit var grid: DataGrid<ChallengeLevelInfo>

    fun setChallenges(challengeLevels: List<ChallengeLevelInfo> = challengeLevelsProperty.value) {
        runAsync {
            challengeLevels.sortedWith(
                compareBy<ChallengeLevelInfo> { CRINGE_MISSIONS.any { x -> it.description.contains(x) } }
                    .thenByDescending { it.currentValue / it.rewardValue }
                    .thenBy { it.description }
            )
            .filter { !CRINGE_MISSIONS.any { x -> it.description.contains(x) } }
            .filter { !it.rewardObtained }
        } ui {
            challengeLevelsProperty.value = FXCollections.observableList(it)

            val x = ceil(it.size.toDouble() / CHALLENGES_PER_ROW) * (CHALLENGE_IMAGE_WIDTH + (DEFAULT_SPACING * 2)) + SCROLLBAR_HEIGHT
            verticalRow.minHeight = x - (SCROLLBAR_HEIGHT / 2)
            grid.minHeight = x

            currentWindow!!.sizeToScene()
            currentWindow!!.centerOnScreen()
        }
    }

    override val root = vbox {
        alignment = Pos.CENTER
        minWidth = WIDTH
        maxWidth = WIDTH

        verticalRow = scrollpane(fitToWidth = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            grid = datagrid(challengeLevelsProperty) {
                alignment = Pos.CENTER
                cellWidth = CHALLENGE_IMAGE_WIDTH
                cellHeight = CHALLENGE_IMAGE_WIDTH

                cellFormat {
                    alignment = Pos.TOP_CENTER
                    maxHeight = CHALLENGE_IMAGE_WIDTH

                    graphic = stackpane {
                        imageview {
                            fitWidth = CHALLENGE_IMAGE_WIDTH
                            fitHeight = CHALLENGE_IMAGE_WIDTH

                            image = LeagueCommunityDragonApi.getImage(CacheType.CHALLENGE, it.anyImageId, it.currentLevelImage).apply {
                                effect = LeagueCommunityDragonApi.getChallengeImageEffect(it.currentLevel)
                            }
                        }

                        blackLabel(it.description)

                        stackpane {
                            vbox {
                                alignment = Pos.BOTTOM_CENTER

                                blackLabel("Title: ${it.rewardTitle}" + if (it.rewardObtained) " ✓" else " (${it.rewardLevel.toString()[0]})")

                                blackLabel("${it.currentValue.toInt().toCommaSeparatedNumber()}/${it.rewardValue.toInt().toCommaSeparatedNumber()}")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val WIDTH = 1040.0

        private var CHALLENGES_PER_ROW = (WIDTH / (CHALLENGE_IMAGE_WIDTH + (DEFAULT_SPACING * 2))).toInt()

        val CRINGE_MISSIONS = setOf(
            "Earn points from challenges in the ",
            "Earn rank in Ranked ",
        )
    }
}