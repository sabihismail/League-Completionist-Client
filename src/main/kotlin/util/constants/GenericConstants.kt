package util.constants

import com.stirante.lolclient.libs.com.google.gson.Gson
import com.stirante.lolclient.libs.com.google.gson.GsonBuilder

object GenericConstants {
    val ETERNALS_DESCRIPTION_REGEX = Regex(""".*(\([A-Z][\dA-Z]?\)).*""")
    val DISTANCE_CONSTANT = 152
    val GSON: Gson = GsonBuilder().create()
}