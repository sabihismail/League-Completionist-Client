package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.CacheType
import tornadofx.*
import ui.views.util.blackLabel
import util.constants.ViewConstants.IMAGE_WIDTH

class ChampionFragment : Fragment() {
    val champion: ChampionInfo by param(ChampionInfo(name = "None"))
    val showTokens: Boolean by param(true)
    val showEternals: Boolean by param(true)
    val showYou: Boolean by param(false)
    val showMaxEternal: Boolean by param(false)

    override val root = stackpane {
        alignment = Pos.TOP_CENTER

        blackLabel("You", textAlignment = TextAlignment.LEFT, fontSize = 11.0) {
            isVisible = champion.isSummonerSelectedChamp && showYou
        }

        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, champion.id)) {
            fitWidth = IMAGE_WIDTH
            fitHeight = IMAGE_WIDTH

            effect = Blend().apply {
                mode = BlendMode.SRC_OVER
                opacity = 0.7
                topInput = ColorInput().apply {
                    width = IMAGE_WIDTH
                    height = IMAGE_WIDTH

                    paint = Color.GREEN
                }
            } //LeagueCommunityDragonApi.getChampionImageEffect(champion)
        }

        borderpane {
            left = stackpane {
                alignment = Pos.TOP_LEFT

                vbox {
//                    val extraInfoStr = if (showTokens && setOf(5, 6).any { champion.level == it })
//                        " (T: ${champion.tokens}/${champion.level / 2})"
//                    else
//                        champion.percentageUntilNextLevel

                    var text = "Lvl ${champion.level}${champion.percentageUntilNextLevel}" // $extraInfoStr"

                    if (showMaxEternal) {
                        text += champion.maxEternalStr
                    }

//                    if (champion.idealChampionToMasterEntry != -1) {
//                        text += " (Rec: ${champion.idealChampionToMasterEntry})"
//                    }

                    blackLabel(text, fontSize = 9.6)

                    blackLabel("${champion.roles?.sortedBy { it.name }?.joinToString(", ") { it.name.lowercase() }}", fontSize = 9.6)

//                    blackLabel(champion.masteryBoxRewards, textAlignment = TextAlignment.LEFT)
                }
            }

            bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to champion.getEternals(showEternals), EternalsFragment::fontSizeIn to 9.0)).root
        }
    }
}