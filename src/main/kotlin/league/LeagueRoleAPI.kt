package league

import league.models.Role
import league.models.RoleMapping
import util.StringUtil
import java.net.URL

object LeagueRoleAPI {
    private const val FILTER_FILE = "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-champion-statistics/global/default/rcp-fe-lol-champion-statistics.js"

    var ROLE_MAPPING = hashMapOf<Role, HashMap<Int, Float>>()

    fun getChampionsByRole(role: Role): List<Int> {
        if (ROLE_MAPPING.isEmpty()) {
            populateRoleMapping()
        }

        val sorted = ROLE_MAPPING[role]?.map { it.key }

        return sorted!!
    }

    fun populateRoleMapping() {
        ROLE_MAPPING.clear()

        val connection = URL(FILTER_FILE).openConnection()
        connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

        val jsonStr = connection.getInputStream().bufferedReader().use { it.readText() }
        val json = StringUtil.extractJSONFromString<RoleMapping>(jsonStr, "a.exports=")

        ROLE_MAPPING[Role.TOP] = json.top
        ROLE_MAPPING[Role.JUNGLE] = json.jungle
        ROLE_MAPPING[Role.MIDDLE] = json.middle
        ROLE_MAPPING[Role.BOTTOM] = json.bottom
        ROLE_MAPPING[Role.SUPPORT] = json.support
    }
}