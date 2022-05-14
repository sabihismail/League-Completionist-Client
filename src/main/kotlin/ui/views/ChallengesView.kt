package ui.views

import DEBUG_FAKE_UI_DATA_ARAM
import DEBUG_FAKE_UI_DATA_NORMAL
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonAPI
import league.models.ChallengeFilter
import league.models.ChallengeUiRefreshData
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeInfoRank
import league.models.enums.ImageCacheType
import league.models.json.ChallengeInfo
import tornadofx.*
import ui.controllers.MainViewController
import ui.mock.AramMockController
import ui.mock.NormalMockController
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.DEFAULT_SPACING
import util.constants.ViewConstants.SCROLLBAR_HEIGHT
import kotlin.math.roundToInt


class ChallengesView : View("LoL Challenges") {
    private val categoriesProperty = SimpleListProperty<ChallengeCategory>()
    private val allChallengeInfoMapProperty = SimpleMapProperty<ChallengeCategory, List<ChallengeInfo>>()
    private val sortedChallengeInfoMapProperty = SimpleMapProperty<ChallengeCategory, List<ChallengeInfo>>()

    private val hideEarnPointChallengesProperty = SimpleBooleanProperty(true)
    private val hideCompletedChallengesProperty = SimpleBooleanProperty(true)
    private val currentSearchTextProperty = SimpleStringProperty("")

    private lateinit var grid: DataGrid<ChallengeCategory>

    fun setChallenges(challengeInfo: Map<ChallengeCategory, List<ChallengeInfo>> = allChallengeInfoMapProperty.value,
                      categories: List<ChallengeCategory> = categoriesProperty.value) {
        runAsync {
            val filters = listOf(
                ChallengeFilter(hideEarnPointChallengesProperty.get()) { challengeInfo ->
                    !CRINGE_MISSIONS.any { x -> challengeInfo.description!!.contains(x) }
                },

                ChallengeFilter(hideCompletedChallengesProperty.get()) { challengeInfo -> !challengeInfo.isComplete },

                ChallengeFilter(currentSearchTextProperty.value.isNotEmpty()) { challengeInfo ->
                    challengeInfo.description!!.lowercase().contains(currentSearchTextProperty.value.lowercase())
                },
            )

            val sortedMap = challengeInfo.toList().associate { (k, v) -> k to v.filter { challengeInfo -> filters.filter { it.isSet }.all { it.action(challengeInfo) } } }

            ChallengeUiRefreshData(FXCollections.observableMap(challengeInfo), FXCollections.observableMap(sortedMap), FXCollections.observableList(categories))
        } ui {
            categoriesProperty.value = it.categories
            allChallengeInfoMapProperty.value = it.allChallengeInfo
            sortedChallengeInfoMapProperty.value = it.sortedChallengeInfo

            grid.cellWidth = (CHALLENGE_IMAGE_WIDTH + DEFAULT_SPACING * 2) *
                    (categoriesProperty.maxOfOrNull { key -> sortedChallengeInfoMapProperty[key]!!.size } ?: 1)
        }
    }

    @Suppress("unused")
    private val controller = find(
        if (DEBUG_FAKE_UI_DATA_ARAM) AramMockController::class
        else if (DEBUG_FAKE_UI_DATA_NORMAL) NormalMockController::class
        else MainViewController::class
    )

    init {
        ROW_COUNT = controller.leagueConnection.challengeInfo.keys.size
        OUTER_GRID_PANE_HEIGHT = (INNER_CELL_HEIGHT + SPACING_BETWEEN_ROW * 2) * ROW_COUNT + DEFAULT_SPACING * 2

        hideEarnPointChallengesProperty.onChange { setChallenges() }
        hideCompletedChallengesProperty.onChange { setChallenges() }
        currentSearchTextProperty.onChange { setChallenges() }
    }

    override val root = vbox {
        alignment = Pos.CENTER
        minWidth = 820.0

        scrollpane(fitToWidth = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            minHeight = OUTER_GRID_PANE_HEIGHT
            maxHeight = OUTER_GRID_PANE_HEIGHT

            grid = datagrid(categoriesProperty) {
                maxCellsInRow = 1
                verticalCellSpacing = SPACING_BETWEEN_ROW
                cellHeight = INNER_CELL_HEIGHT
                minHeight = OUTER_GRID_PANE_HEIGHT

                cellFormat {
                    graphic = vbox {
                        alignment = Pos.CENTER_LEFT
                        maxHeight = INNER_CELL_HEIGHT
                        minHeight = INNER_CELL_HEIGHT

                        label("$it:") {
                            textFill = Color.WHITE
                            font = Font.font(HEADER_FONT_SIZE)
                            textAlignment = TextAlignment.LEFT

                            fitToParentWidth()
                            style {
                                backgroundColor += Color.BLACK
                            }
                        }

                        datagrid(sortedChallengeInfoMapProperty[it]) {
                            alignment = Pos.CENTER
                            maxRows = 1
                            cellWidth = CHALLENGE_IMAGE_WIDTH
                            cellHeight = CHALLENGE_IMAGE_WIDTH

                            cellFormat {
                                graphic = stackpane {
                                    alignment = Pos.TOP_CENTER
                                    maxHeight = CHALLENGE_IMAGE_WIDTH

                                    imageview {
                                        fitWidth = CHALLENGE_IMAGE_WIDTH
                                        fitHeight = CHALLENGE_IMAGE_WIDTH

                                        val currentLevel = if (it.currentLevel == ChallengeInfoRank.NONE)
                                            ChallengeInfoRank.IRON.name.lowercase()
                                        else
                                            it.currentLevel!!.name.lowercase()

                                        image = LeagueCommunityDragonAPI.getImage(ImageCacheType.CHALLENGE, it.id!!, currentLevel).apply {
                                            effect = LeagueCommunityDragonAPI.getChallengeImageEffect(it)
                                        }
                                    }

                                    label(it.description!!) {
                                        textFill = Color.WHITE
                                        textAlignment = TextAlignment.CENTER
                                        isWrapText = true
                                        paddingHorizontal = 8
                                        font = Font.font(9.0)

                                        style {
                                            backgroundColor += Color.BLACK
                                        }
                                    }

                                    stackpane {
                                        vbox {
                                            alignment = Pos.BOTTOM_CENTER

                                            label("${it.currentLevel} (${it.thresholds!!.keys.sorted().indexOf(it.currentLevel) + 1}/${it.thresholds!!.count()})") {
                                                textFill = Color.WHITE
                                                textAlignment = TextAlignment.CENTER
                                                isWrapText = true
                                                paddingHorizontal = 8
                                                font = Font.font(9.0)

                                                style {
                                                    backgroundColor += Color.BLACK
                                                }
                                            }

                                            // val txt = it.thresholds!!.toList().sortedBy { it.first }.map { it.second.value }.joinToString(", ")
                                            label("${it.currentThreshold!!.roundToInt()}/${it.nextThreshold!!.roundToInt()}") {
                                                textFill = Color.WHITE
                                                textAlignment = TextAlignment.CENTER
                                                isWrapText = true
                                                paddingHorizontal = 8
                                                font = Font.font(9.0)

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

        vbox {
            textfield {
                textProperty().addListener { _, _, newValue ->
                    currentSearchTextProperty.set(newValue)
                }
            }

            hbox {
                paddingHorizontal = 10.0
                spacing = 10.0

                checkbox("Hide Grind/Time Missions", hideEarnPointChallengesProperty)
                checkbox("Hide Completed Missions", hideCompletedChallengesProperty)
            }
        }
    }

    companion object {
        private const val HEADER_FONT_SIZE = 14.0
        private const val SPACING_BETWEEN_ROW = 4.0

        // image_height + 2 * verticalCellSpacing + font size of label
        private const val INNER_CELL_HEIGHT = CHALLENGE_IMAGE_WIDTH + (DEFAULT_SPACING * 2) + HEADER_FONT_SIZE

        private var ROW_COUNT = 6
        // cell + row_spacing for 6 rows + vert spacing
        private var OUTER_GRID_PANE_HEIGHT = (INNER_CELL_HEIGHT + SPACING_BETWEEN_ROW * 2) * ROW_COUNT + DEFAULT_SPACING * 2 + SCROLLBAR_HEIGHT

        private val CRINGE_MISSIONS = setOf(
            "Earn points from challenges",
            "Mastery Points",
            "Obtain ",
            "Collect ",
            "Increase your summoner level",
            "Finish any season",
            "Reach ",
            "Achieve milestone",
            "Rekindle an Eternals ",
            "Earn Mastery ",
            "Win games ",
            "Get ",
            "Takedown ",
        )
    }
}