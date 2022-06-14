package util.constants

import com.stirante.lolclient.libs.com.google.gson.Gson
import com.stirante.lolclient.libs.com.google.gson.GsonBuilder

object GenericConstants {
    const val DISTANCE_CONSTANT = 152

    val ETERNALS_DESCRIPTION_REGEX = Regex(""".*(\([A-Z][\dA-Z]?\)).*""")
    val GSON: Gson = GsonBuilder().create()
    val GSON_PRETTY: Gson = GsonBuilder().setPrettyPrinting().create()
}