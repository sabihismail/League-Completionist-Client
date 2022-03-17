package league

import league.models.Role
import org.junit.jupiter.api.Test

internal class LeagueRoleAPITest {
    @Test
    fun checkLeagueAPIJSONObject() {
        assert(LeagueRoleAPI.ROLE_MAPPING.size == 0)
        assert(LeagueRoleAPI.getChampionsByRole(Role.TOP).isNotEmpty())
        assert(LeagueRoleAPI.ROLE_MAPPING.size == 5)
    }
}