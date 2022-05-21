package ui.views.fragments

import generated.LolStatstonesStatstone
import generated.LolStatstonesStatstoneSet
import javafx.geometry.Pos
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import tornadofx.*
import ui.views.fragments.util.blackLabel
import util.constants.GenericConstants.ETERNALS_DESCRIPTION_REGEX

class EternalsFragment : Fragment() {
    val eternal: LolStatstonesStatstoneSet by param()
    val fontSizeIn: Double by param()

    private fun getEternalsThreshold(currentEternal: LolStatstonesStatstone): String {
        return LeagueCommunityDragonApi.getEternal(currentEternal.statstoneId)
            .dropLast(1)
            .filter { it > currentEternal.playerRecord.value }
            .joinToString(", ")
    }

    override val root = stackpane {
        alignment = Pos.BOTTOM_LEFT

        vbox {
            eternal.statstones.forEach {
                val regexVal = if (ETERNALS_DESCRIPTION_REGEX.matches(it.description))
                    ETERNALS_DESCRIPTION_REGEX.find(it.description)!!.groups[1]!!.value + " "
                else
                    ""

                blackLabel(regexVal + "Lvl ${it.formattedMilestoneLevel} - ${it.formattedValue}/${it.nextMilestone}", fontSize = fontSizeIn, isWrapText = false) {
                    tooltip("${it.description} (${getEternalsThreshold(it)})") {
                        style {
                            font = Font.font(fontSizeIn)
                        }
                    }
                }
            }
        }
    }
}