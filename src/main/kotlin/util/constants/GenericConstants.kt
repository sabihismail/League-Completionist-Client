package util.constants

import com.stirante.lolclient.libs.com.google.gson.GsonBuilder

object GenericConstants {
    val ETERNALS_DESCRIPTION_REGEX = Regex(""".*(\([A-Z][\dA-Z]?\)).*""")
    val GSON = GsonBuilder().create()
}