package util

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.stage.Screen
import javafx.stage.Stage
import java.util.*

object ViewUtil {
    fun moveToScreen(stage: Stage?) {
        if (stage == null) return

        val bounds = Screen.getScreens().first { it.visualBounds.minX > 0 }.visualBounds

        stage.x = bounds.minX
        stage.y = bounds.minY

        stage.centerOnScreen()
    }

    @Suppress("unused")
    fun showDoubleInputDialog(callable: (Pair<String, String>) -> Unit) {
        val dialog: Dialog<Pair<String, String>> = Dialog()
        dialog.title = "Input"

        val loginButtonType = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.addAll(loginButtonType, ButtonType.CANCEL)

        val gridPane = GridPane()
        gridPane.hgap = 10.0
        gridPane.vgap = 10.0
        gridPane.padding = Insets(20.0, 150.0, 10.0, 10.0)

        val from = TextField()
        from.promptText = "Username"
        val to = TextField()
        to.promptText = "Password"

        gridPane.add(from, 0, 0)
        gridPane.add(to, 0, 1)

        dialog.dialogPane.content = gridPane

        Platform.runLater { from.requestFocus() }

        dialog.setResultConverter { dialogButton ->
            if (dialogButton === loginButtonType) {
                return@setResultConverter Pair(from.text, to.text)
            }
            null
        }

        val result: Optional<Pair<String, String>> = dialog.showAndWait()

        result.ifPresent { pair: Pair<String?, String?> ->
            if (pair.first.isNullOrBlank() || pair.second.isNullOrBlank()) {
                return@ifPresent
            }

            callable(Pair(pair.first!!, pair.second!!))
        }
    }
}