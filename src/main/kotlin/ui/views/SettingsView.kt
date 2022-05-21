package ui.views

import javafx.beans.property.SimpleListProperty
import org.jetbrains.exposed.sql.ResultRow
import tornadofx.View
import tornadofx.listview
import tornadofx.vbox

class SettingsView : View() {
    val accountList: SimpleListProperty<ResultRow> by param()

    override val root = vbox {
        listview(accountList)
    }
}