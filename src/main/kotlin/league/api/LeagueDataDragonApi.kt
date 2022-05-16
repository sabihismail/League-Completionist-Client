@file:Suppress("SameParameterValue")

package league.api

import league.models.json.ApiLeagueVersion
import util.StringUtil
import java.net.URL

object LeagueDataDragonApi {
    private const val VERSION_ENDPOINT = "https://ddragon.leagueoflegends.com/realms/na.json"

    val VERSION by lazy {
        val jsonStr = sendRequest(VERSION_ENDPOINT)
        val json = StringUtil.extractJSONFromString<ApiLeagueVersion>(jsonStr)

        if (json.v != null) {
            return@lazy if (json.v!!.count { it == '.' } == 2) json.v!!.substringBeforeLast(".") else json.v!!
        }

        ApiLeagueVersion.DEFAULT
    }

    private fun sendRequest(url: String): String {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

        return connection.getInputStream().bufferedReader().use { it.readText() }
    }
}