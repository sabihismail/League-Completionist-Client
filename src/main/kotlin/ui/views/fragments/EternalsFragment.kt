package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.text.Font
import league.models.json.LolStatstonesStatstoneInfo
import tornadofx.*
import ui.views.util.blackLabel
import util.StringUtil
import util.constants.GenericConstants.ETERNALS_DESCRIPTION_REGEX

class EternalsFragment : Fragment() {
    val eternal: List<LolStatstonesStatstoneInfo> by param()
    val fontSizeIn: Double by param()

    override var root = stackpane {
        alignment = Pos.BOTTOM_LEFT
    }

    init {
        if (eternal.isNotEmpty()) {
            set(eternal)
        }
    }

    fun set(newEternal: List<LolStatstonesStatstoneInfo>) {
        val vBox = vbox {
            newEternal.sortedByDescending { it.formattedMilestoneLevel }
                .forEach {
                    val regex = StringUtil.getSafeRegex(ETERNALS_DESCRIPTION_REGEX, it.description)
                    blackLabel(regex + "Lvl ${it.formattedMilestoneLevel} - ${it.formattedValue}/${it.nextMilestone}", fontSize = fontSizeIn, isWrapText = false) {
                        tooltip("${it.description} (${it.summaryThreshold})") {
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
}