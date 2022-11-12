package ui.views

import com.stirante.lolclient.ClientWebSocket
import com.stirante.lolclient.libs.com.google.gson.JsonArray
import com.stirante.lolclient.libs.com.google.gson.JsonObject
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
        minWidth = 900.0

        center = hbox {
            listview(events) {
                cellFormat {
                    text = it.uri
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

            textarea(currentSelection)
        }
    }
}
