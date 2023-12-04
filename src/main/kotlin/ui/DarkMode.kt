package ui

import javafx.scene.paint.Color
import tornadofx.Stylesheet

class DarkMode: Stylesheet() {
    init {
        root {
            backgroundColor += Color.rgb(39, 55, 77)
        }

        label {
            textFill = Color.rgb(82, 109, 130)
        }
    }
}