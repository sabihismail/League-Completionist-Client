package ui

import javafx.stage.Stage
import tornadofx.App
import tornadofx.importStylesheet
import tornadofx.reloadStylesheetsOnFocus
import ui.views.MainView
import util.ResourceUtils
import util.ViewUtil
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess


class MainApp: App(MainView::class) {
    override val configPath: Path = Paths.get("./config.properties")

    init {
        reloadStylesheetsOnFocus()

        val path = ResourceUtils.getResource(this.javaClass.classLoader, "nord-dark.css")
        importStylesheet(path)
    }

    override fun start(stage: Stage) {
        super.start(stage)

        ViewUtil.moveToScreen(stage)
    }

    override fun stop() {
        super.stop()

        exitProcess(0)
    }
}
