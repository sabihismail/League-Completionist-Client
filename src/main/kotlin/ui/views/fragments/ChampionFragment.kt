package ui.views.fragments

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.CacheType
import tornadofx.*
import ui.views.fragments.util.blackLabel
import util.constants.ViewConstants.IMAGE_WIDTH

class ChampionFragment : Fragment() {
    val champion: SimpleObjectProperty<ChampionInfo> by param(ChampionInfo().toProperty())
    val showTokens: Boolean by param(true)
    val showYou: Boolean by param(false)

    override val root = stackpane {
        alignment = Pos.TOP_CENTER

        blackLabel("You", textAlignment = TextAlignment.LEFT, fontSize = 11.0) {
            isVisible = champion.value.isSummonerSelectedChamp && showYou
        }

        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, champion.value.id)) {
            fitWidth = IMAGE_WIDTH
            fitHeight = IMAGE_WIDTH

            effect = LeagueCommunityDragonApi.getChampionImageEffect(champion.value)
        }

        borderpane {
            left = stackpane {
                alignment = Pos.TOP_LEFT

                label("Lvl ${champion.value.level}${champion.value.percentageUntilNextLevel}") {
                    textFill = Color.WHITE
                    paddingHorizontal = 8
                    font = Font.font(11.0)

                    style {
                        backgroundColor += Color.BLACK
                    }
                }
            }

            if (showTokens) {
                right = stackpane {
                    alignment = Pos.TOP_RIGHT

                    label(
                        when (champion.value.level) {
                            6 -> "${champion.value.tokens}/3"
                            5 -> "${champion.value.tokens}/2"
                            else -> ""
                        }
                    ) {
                        isVisible = listOf(5, 6).contains(champion.value.level)
                        textFill = Color.WHITE
                        paddingHorizontal = 8
                        font = Font.font(11.6)

                        style {
                            backgroundColor += Color.BLACK
                        }
                    }
                }
            }

            if (champion.value.eternal != null) {
                bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to champion.value.eternal.toProperty(), EternalsFragment::fontSizeIn to 9.0)).root
            }
        }
    }
}