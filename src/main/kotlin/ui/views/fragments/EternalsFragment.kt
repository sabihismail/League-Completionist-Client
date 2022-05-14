package ui.views.fragments

import generated.LolStatstonesStatstoneSet
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import tornadofx.*
import util.constants.GenericConstants

class EternalsFragment : Fragment() {
    val eternal: LolStatstonesStatstoneSet by param()

    override val root = stackpane {
        alignment = Pos.BOTTOM_LEFT

        vbox {
            eternal.statstones.forEach {
                val regexVal = if (GenericConstants.ETERNALS_DESCRIPTION_REGEX.matches(it.description))
                    GenericConstants.ETERNALS_DESCRIPTION_REGEX.find(it.description)!!.groups[1]!!.value + " "
                else
                    ""

                label(regexVal + "LVL ${it.formattedMilestoneLevel} ${it.formattedValue}/${it.nextMilestone}") {
                    font = Font.font(9.0)
                    textFill = Color.WHITE
                    paddingHorizontal = 8
                    tooltip = tooltip(it.description) {
                        style {
                            font = Font.font(9.0)
                        }
                    }

                    style {
                        backgroundColor += Color.BLACK
                    }
                }
            }
        }
    }
}