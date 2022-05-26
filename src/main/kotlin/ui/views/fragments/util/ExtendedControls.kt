package ui.views.fragments.util

import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import tornadofx.attachTo
import tornadofx.paddingHorizontal
import tornadofx.style

fun EventTarget.blackLabel(text: String = "", textFill: Color? = Color.WHITE, graphic: Node? = null, textAlignment: TextAlignment = TextAlignment.CENTER,
                           isWrapText: Boolean = true, fontSize: Double = 9.0, backgroundColorVal: Paint = Color.BLACK, paddingHorizontal: Int = 8,
                           fontWeight: FontWeight = FontWeight.BOLD, op: Label.() -> Unit = {}) = Label(text).attachTo(this, op) {
    if (graphic != null) it.graphic = graphic

    it.textFill = textFill
    it.textAlignment = textAlignment
    it.isWrapText = isWrapText
    it.paddingHorizontal = paddingHorizontal
    it.font = Font.font(Font.getDefault().family, fontWeight, fontSize)

    it.style {
        backgroundColor += backgroundColorVal
    }
}

fun EventTarget.boldLabel(text: String = "", op: Label.() -> Unit = {}) = Label(text).attachTo(this, op) {
    it.text = text
    it.font = Font.font(Font.getDefault().family, FontWeight.BOLD, Font.getDefault().size)
}
