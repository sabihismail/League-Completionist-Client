package util

import javafx.scene.control.Alert
import tornadofx.runLater
import util.constants.GenericConstants
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.reflect.full.memberProperties
import kotlin.system.exitProcess

class Settings {
    var mainId = 0L
    var disenchantIds = ""
    var developerIds = ""

    companion object {
        val INSTANCE: Settings by lazy {
            val configFile = Paths.get("config.json")

            if (!configFile.exists()) {
                val settings = Settings()
                val json = GenericConstants.GSON_PRETTY.toJson(settings)

                configFile.createFile()
                configFile.writeText(json)

                invalidConfigurationData()
            }

            val dataStr = configFile.readText()
            val data = GenericConstants.GSON.fromJson(dataStr, Settings::class.java)

            for (property in Settings::class.memberProperties) {
                val propertyValue = property.get(data)

                if (propertyValue.toString().isBlank()) {
                    invalidConfigurationData()
                }
            }

            data
        }

        private fun invalidConfigurationData() {
            runLater {
                val alertDialog = Alert(Alert.AlertType.ERROR)
                alertDialog.title = "Fatal Error"
                alertDialog.contentText = "Invalid 'config.json' values."
                alertDialog.showAndWait()
                alertDialog.setOnCloseRequest {
                    exitProcess(-1)
                }
            }

            while (true) {
                Thread.sleep(1000)
            }
        }
    }
}