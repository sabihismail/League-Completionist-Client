package league

import league.api.LeagueCommunityDragonApi
import league.models.enums.CacheType
import league.models.enums.ChallengeLevel
import league.models.enums.Role
import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.test.Test

internal class LeagueCommunityDragonApiTest {
    @Test
    fun checkLeagueQueueMappingApi() {
        assert(LeagueCommunityDragonApi.QUEUE_MAPPING.size == 0)
        assert(LeagueCommunityDragonApi.getQueueMapping(0).name == "Custom")
        assert(LeagueCommunityDragonApi.QUEUE_MAPPING.size > 0)
    }

    @Test
    fun checkLeagueRoleMappingApi() {
        assert(LeagueCommunityDragonApi.CHAMPION_ROLE_MAPPING.size == 0)
        assert(LeagueCommunityDragonApi.getChampionsByRole(Role.TOP).isNotEmpty())
        assert(LeagueCommunityDragonApi.CHAMPION_ROLE_MAPPING.size == 5)
    }

    @Test
    fun checkLeagueChallengeMappingApi() {
        assert(LeagueCommunityDragonApi.CHALLENGE_THRESHOLD_MAPPING.size == 0)
        assert(LeagueCommunityDragonApi.getChallengeThreshold("CRYSTAL", ChallengeLevel.BRONZE) > 0)
        assert(LeagueCommunityDragonApi.QUEUE_MAPPING.size > 0)
    }

    @Test
    fun checkLeagueEternalsMappingApi() {
        assert(LeagueCommunityDragonApi.ETERNALS_MAPPING.size == 0)
        assert(LeagueCommunityDragonApi.getEternal("bd5ea729-424c-410f-8cf8-864575a08430").isNotEmpty())
        assert(LeagueCommunityDragonApi.ETERNALS_MAPPING.size > 0)
    }

    @Test
    fun getChampionImagePath() {
        val imageID = 412 // thresh

        val image = LeagueCommunityDragonApi.getImagePath(CacheType.CHAMPION, true, imageID)

        assert(Files.exists(image!!))
        assert(image.fileSize() > 0)

        val image2 = LeagueCommunityDragonApi.getImagePath(CacheType.CHAMPION, true, imageID)

        assert(Files.exists(image2!!))
        assert(image2.fileSize() > 0)
    }

    @Test
    fun getChallengeImagePath() {
        val imageId = 2022005 // Co-Op vs AI challenge Id
        val rankId = "grandmaster"

        val image = LeagueCommunityDragonApi.getImagePath(CacheType.CHALLENGE, true, imageId, rankId)

        assert(Files.exists(image!!))
        assert(image.fileSize() > 0)

        val image2 = LeagueCommunityDragonApi.getImagePath(CacheType.CHALLENGE, true, imageId, rankId)

        assert(Files.exists(image2!!))
        assert(image2.fileSize() > 0)
    }
}