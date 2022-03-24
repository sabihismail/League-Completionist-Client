package league

import league.models.Role
import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.test.Test

internal class LeagueCommunityDragonAPITest {
    @Test
    fun checkLeagueQueueMappingAPI() {
        assert(LeagueCommunityDragonAPI.QUEUE_MAPPING.size == 0)
        assert(LeagueCommunityDragonAPI.getQueueMapping(0).name == "Custom")
        assert(LeagueCommunityDragonAPI.QUEUE_MAPPING.size > 0)
    }

    @Test
    fun checkLeagueRoleMappingAPI() {
        assert(LeagueCommunityDragonAPI.ROLE_MAPPING.size == 0)
        assert(LeagueCommunityDragonAPI.getChampionsByRole(Role.TOP).isNotEmpty())
        assert(LeagueCommunityDragonAPI.ROLE_MAPPING.size == 5)
    }

    @Test
    fun getChampionImagePath() {
        val imageID = 412 // thresh

        val image = LeagueCommunityDragonAPI.getChampionImagePath(imageID)

        assert(Files.exists(image))
        assert(image.fileSize() > 0)

        val image2 = LeagueCommunityDragonAPI.getChampionImagePath(imageID)

        assert(Files.exists(image2))
        assert(image2.fileSize() > 0)
    }
}