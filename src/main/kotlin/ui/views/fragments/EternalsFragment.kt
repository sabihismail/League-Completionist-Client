package ui.views.fragments

import generated.LolStatstonesStatstone
import generated.LolStatstonesStatstoneSet
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import tornadofx.*
import ui.views.fragments.util.blackLabel
import util.constants.GenericConstants.ETERNALS_DESCRIPTION_REGEX
import java.text.NumberFormat

class EternalsFragment : Fragment() {
    val eternal: SimpleObjectProperty<LolStatstonesStatstoneSet>? by param()
    val fontSizeIn: Double by param()

    override var root = stackpane {  }

    init {
        if (eternal != null) {
            set(eternal!!)
        }
    }

    fun set(newEternal: SimpleObjectProperty<LolStatstonesStatstoneSet>) {
        root = stackpane {
            alignment = Pos.BOTTOM_LEFT

            vbox {
                newEternal.value.statstones.forEach {
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

    private fun getEternalsThreshold(currentEternal: LolStatstonesStatstone): String {
        return LeagueCommunityDragonApi.getEternal(currentEternal.statstoneId)
            .dropLast(1)
            .filter { it > NumberFormat.getNumberInstance().parse(currentEternal.nextMilestone).toInt() }
            .joinToString(", ")
    }
}