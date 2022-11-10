package ui.views

import javafx.beans.property.SimpleStringProperty
import tornadofx.View
import tornadofx.borderpane
import tornadofx.textarea

class DebugView: View() {
    val endpointListStr = SimpleStringProperty()

    override val root = borderpane {
        center = textarea(endpointListStr)
    }
}
