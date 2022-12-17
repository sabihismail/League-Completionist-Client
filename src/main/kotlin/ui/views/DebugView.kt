package ui.views

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.stirante.lolclient.ClientWebSocket
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import org.apache.commons.lang3.reflect.FieldUtils
import tornadofx.*
import util.constants.GenericConstants.GSON_PRETTY

class DebugView: View() {
    val events = SimpleListProperty<ClientWebSocket.Event>()

    private val currentSelection = SimpleStringProperty("")

    override val root = borderpane {
        minHeight = 1000.0
        minWidth = 1200.0

        center = hbox {
            listview(events) {
                prefWidth = 500.0

                cellFormat {
                    text = it.uri

                    prefWidth = 500.0
                }

                onUserSelect(1) {
                    runAsync {
                        when(val dataJson = FieldUtils.readField(it, "dataJson", true)) {
                            is JsonArray, is JsonObject -> GSON_PRETTY.toJson(dataJson)
                            else -> ""
                        }
                    } ui {
                        currentSelection.value = it
                    }
                }
            }

            textarea(currentSelection) {
                prefWidth = 700.0
            }
        }
    }
}
