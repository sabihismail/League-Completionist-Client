package ui

import tornadofx.App
import ui.views.MainView
import kotlin.system.exitProcess

class MainApp: App(MainView::class) {
    override fun stop() {
        super.stop()

        exitProcess(0)
    }
}
