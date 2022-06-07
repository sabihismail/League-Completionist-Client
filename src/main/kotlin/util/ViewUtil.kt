package util

import javafx.stage.Screen
import javafx.stage.Stage

object ViewUtil {
    fun moveToScreen(stage: Stage?) {
        if (stage == null) return

        val bounds = Screen.getScreens().first { it.visualBounds.minX > 0 }.visualBounds

        stage.x = bounds.minX
        stage.y = bounds.minY

        stage.centerOnScreen()
    }
}