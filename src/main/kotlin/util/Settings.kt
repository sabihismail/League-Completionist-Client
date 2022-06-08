package util

import util.constants.GenericConstants
import java.nio.file.Paths
import kotlin.io.path.readText

class Settings {
    lateinit var riotApiKey: String

    companion object {
        fun get(): Settings {
            val data = Paths.get("config.json").readText()

            return GenericConstants.GSON.fromJson(data, Settings::class.java)
        }
    }
}