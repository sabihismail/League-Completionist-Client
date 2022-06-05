package util

import javafx.stage.Screen
import javafx.stage.Stage
import kotlin.math.min

object ViewUtil {
    fun moveToScreen(stage: Stage?) {
        if (stage == null) return

        val bounds = Screen.getScreens()[min(2, Screen.getScreens().size) - 1].visualBounds

        stage.x = bounds.minX
        stage.y = bounds.minY

        stage.centerOnScreen()
    }
}