package ui.views.util

import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import javafx.util.StringConverter
import tornadofx.*

inline fun <reified T> EventTarget.blackLabelObs(observable: ObservableValue<T>, graphicProperty: ObservableValue<Node>? = null, converter: StringConverter<in T>? = null,
    textFill: Color? = Color.WHITE, graphic: Node? = null, textAlignment: TextAlignment = TextAlignment.CENTER, isWrapText: Boolean = true, fontSize: Double = 9.0,
    backgroundColorVal: Paint = Color.BLACK, paddingHorizontal: Int = 8, fontWeight: FontWeight = FontWeight.BOLD, noinline op: Label.() -> Unit = {}) = label().apply {
    if (converter == null) {
        if (T::class == String::class) {
            @Suppress("UNCHECKED_CAST")
            textProperty().bind(observable as ObservableValue<String>)
        } else {
            textProperty().bind(observable.stringBinding { it?.toString() })
        }
    } else {
        textProperty().bind(observable.stringBinding { converter.toString(it) })
    }
    if (graphic != null) graphicProperty().bind(graphicProperty)

    this.textFill = textFill
    this.textAlignment = textAlignment
    this.isWrapText = isWrapText
    this.paddingHorizontal = paddingHorizontal
    this.font = Font.font(Font.getDefault().family, fontWeight, fontSize)

    this.style {
        backgroundColor += backgroundColorVal
    }

    op(this)
}

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
