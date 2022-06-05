package ui.views.fragments

import generated.LolStatstonesStatstone
import generated.LolStatstonesStatstoneSet
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.text.Font
import league.api.LeagueCommunityDragonApi
import tornadofx.*
import ui.views.util.blackLabel
import util.StringUtil
import util.constants.GenericConstants.ETERNALS_DESCRIPTION_REGEX
import java.text.NumberFormat

class EternalsFragment : Fragment() {
    val eternal: SimpleObjectProperty<LolStatstonesStatstoneSet>? by param()
    val fontSizeIn: Double by param()

    override var root = stackpane {
        alignment = Pos.BOTTOM_LEFT
    }

    init {
        if (eternal != null) {
            set(eternal!!)
        }
    }

    fun set(newEternal: SimpleObjectProperty<LolStatstonesStatstoneSet>) {
        if (newEternal.value == null) return

        val vBox = vbox {
            newEternal.value.statstones.forEach {
                val regex = StringUtil.getSafeRegex(ETERNALS_DESCRIPTION_REGEX, it.description)
                blackLabel(regex + "Lvl ${it.formattedMilestoneLevel} - ${it.formattedValue}/${it.nextMilestone}", fontSize = fontSizeIn, isWrapText = false) {
                    tooltip("${it.description} (${getEternalsThreshold(it)})") {
                        style {
                            font = Font.font(fontSizeIn)
                        }
                    }
                }
            }
        }

        root.children.clear()
        root.add(vBox)
    }

    private fun parseNextMilestone(parsed: String, nextMilestone: String): Int {
        val timeMapping = hashMapOf('h' to 60 * 60, 'm' to 60, 's' to 1)
        val distanceMapping = hashMapOf("km" to 1000, "m" to 1)

        return if (timeMapping.keys.any { parsed.contains(it) } && !parsed.contains(".")) {
            nextMilestone.split(" ").sumOf { it.substring(0, it.length - 1).toInt() * timeMapping[it.last()]!! }
        } else if (distanceMapping.keys.any { parsed.contains(it) } && parsed.contains(".")) {
            var numberOfStrOnly = nextMilestone
            var key = ""

            while (numberOfStrOnly.last().isLetter()) {
                key = numberOfStrOnly.last() + key
                numberOfStrOnly = numberOfStrOnly.substring(0, numberOfStrOnly.length - 1)
            }

            return (numberOfStrOnly.toDouble() * distanceMapping[key]!!).toInt()
        } else {
            return NumberFormat.getNumberInstance().parse(nextMilestone).toInt()
        }
    }

    private fun getEternalsThreshold(currentEternal: LolStatstonesStatstone): String {
        return LeagueCommunityDragonApi.getEternal(currentEternal.statstoneId)
            .dropLast(1)
            .filter { it.first > parseNextMilestone(it.second, currentEternal.nextMilestone) }
            .joinToString(" > ") { it.second }
    }
}