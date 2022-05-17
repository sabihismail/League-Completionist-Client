package league.api

import javafx.scene.effect.*
import javafx.scene.image.Image
import league.models.ChampionInfo
import league.models.ImageCacheInfo
import league.models.enums.ChallengeLevel
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.ImageCacheType
import league.models.enums.Role
import league.models.json.*
import util.LogType
import util.Logging
import util.StringUtil
import util.constants.ViewConstants
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.notExists


object LeagueCommunityDragonApi {
    private val CHAMPION_ROLE_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/plugins/rcp-fe-lol-champion-statistics/global/default/rcp-fe-lol-champion-statistics.js"
    }
    private val QUEUE_TYPE_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/plugins/rcp-be-lol-game-data/global/default/v1/queues.json"
    }
    private val CHALLENGES_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/plugins/rcp-be-lol-game-data/global/default/v1/challenges.json"
    }
    private val CHAMPION_PORTRAIT_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/%s.png"
    }
    private val ETERNALS_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/plugins/rcp-be-lol-game-data/global/default/v1/statstones.json"
    }
    private val CHALLENGE_IMAGE_ENDPOINT by lazy {
        "https://raw.communitydragon.org/${LeagueDataDragonApi.VERSION}/game/assets/challenges/config/%s/tokens/%s.png"
    }

    private val CACHE_MAPPING by lazy {
        mapOf(
            ImageCacheType.CHAMPION to ImageCacheInfo("champion", CHAMPION_PORTRAIT_ENDPOINT),
            ImageCacheType.CHALLENGE to ImageCacheInfo("challenge", CHALLENGE_IMAGE_ENDPOINT)
        )
    }

    var ROLE_MAPPING = hashMapOf<Role, HashMap<Int, Float>>()
    var QUEUE_MAPPING = hashMapOf<Int, ApiQueueInfoResponse>()
    var CHALLENGE_MAPPING = hashMapOf<String, Long>()
    var ETERNALS_MAPPING = hashMapOf<String, List<Int>>()

    fun getQueueMapping(id: Int): ApiQueueInfoResponse {
        if (QUEUE_MAPPING.isEmpty()) {
            populateQueueMapping()
        }

        return QUEUE_MAPPING[id]!!
    }

    fun getChampionsByRole(role: Role): List<Int> {
        if (ROLE_MAPPING.isEmpty()) {
            populateRoleMapping()
        }

        val sorted = ROLE_MAPPING[role]?.map { it.key }

        return sorted!!
    }

    fun getChallenge(id: String, challengeLevel: ChallengeLevel): Long {
        if (CHALLENGE_MAPPING.isEmpty()) {
            populateChallengeMapping()
        }

        return CHALLENGE_MAPPING[id + challengeLevel.name]!!
    }

    fun getEternal(contentId: String): List<Int> {
        if (ETERNALS_MAPPING.isEmpty()) {
            populateEternalsMapping()
        }

        return ETERNALS_MAPPING[contentId]!!
    }

    fun getImage(t: ImageCacheType, vararg params: Any): Image {
        val path = getImagePath(t, *params)

        return Image(path!!.toUri().toString())
    }

    fun getPath(t: ImageCacheType): Path {
        val info = CACHE_MAPPING[t]!!
        return Paths.get(Paths.get("").toAbsolutePath().toString(), "/cache/${info.folder}")
    }

    fun getImagePath(t: ImageCacheType, vararg params: Any): Path? {
        val path = getPath(t)

        if (path.notExists()) {
            path.createDirectory()
        }

        val imagePath = path.resolve(params.joinToString("-") + ".png")
        if (!imagePath.exists()) {
            val urlStr = CACHE_MAPPING[t]!!.endpoint.format(*params)

            val connection = URL(urlStr).openConnection()
            connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

            try {
                val readableByteChannel = Channels.newChannel(connection.getInputStream())
                val fileOutputStream = FileOutputStream(imagePath.toFile())

                fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
            } catch (e: FileNotFoundException) {
                if (t == ImageCacheType.CHALLENGE) return null

                throw e
            }

            Logging.log("Image Download: '$imagePath'", LogType.INFO)
        }

        return imagePath
    }

    fun getChampionImageEffect(championInfo: ChampionInfo): Effect {
        if (championInfo.ownershipStatus == ChampionOwnershipStatus.NOT_OWNED || championInfo.ownershipStatus == ChampionOwnershipStatus.RENTAL ||
            championInfo.ownershipStatus == ChampionOwnershipStatus.FREE_TO_PLAY) {
            return ColorAdjust(0.0, -1.0, -0.7, -0.1)
        }

        val colorInput = ColorInput().apply {
            width = ViewConstants.IMAGE_WIDTH
            height = ViewConstants.IMAGE_WIDTH

            paint = if (championInfo.ownershipStatus == ChampionOwnershipStatus.BOX_ATTAINED)
                ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
            else
                ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
        }

        val blend = Blend().apply {
            mode = BlendMode.SRC_OVER
            opacity = 0.7
            topInput = colorInput
        }

        return blend
    }

    fun getChallengeImageEffect(challengeInfo: ChallengeInfo): Effect? {
        if (challengeInfo.currentLevel != ChallengeLevel.NONE) return null

        return ColorAdjust(0.0, -1.0, -0.7, -0.1)
    }

    private fun populateQueueMapping() {
        QUEUE_MAPPING.clear()

        val jsonStr = sendRequest(QUEUE_TYPE_ENDPOINT)
        val json = StringUtil.extractJSONMapFromString<ApiQueueInfoResponse>(jsonStr)

        QUEUE_MAPPING = HashMap(json.mapKeys { it.key.toInt() })
    }

    private fun populateRoleMapping() {
        ROLE_MAPPING.clear()

        val jsonStr = sendRequest(CHAMPION_ROLE_ENDPOINT)
        val json = StringUtil.extractJSONFromString<RoleMapping>(jsonStr, "a.exports=")

        ROLE_MAPPING[Role.TOP] = json.top
        ROLE_MAPPING[Role.JUNGLE] = json.jungle
        ROLE_MAPPING[Role.MIDDLE] = json.middle
        ROLE_MAPPING[Role.BOTTOM] = json.bottom
        ROLE_MAPPING[Role.SUPPORT] = if (json.support.isNullOrEmpty()) json.utility!! else json.support
    }

    private fun populateChallengeMapping() {
        CHALLENGE_MAPPING.clear()

        val jsonStr = sendRequest(CHALLENGES_ENDPOINT)
        val json = StringUtil.extractJSONFromString<ApiChallengeResponse>(jsonStr)

        CHALLENGE_MAPPING = HashMap(json.challenges.values.flatMap { c -> c.thresholds!!.map { (k, v) -> (c.name!! + k.name) to v.value!!.toLong() } }
            .toMap())
    }

    private fun populateEternalsMapping() {
        ETERNALS_MAPPING.clear()

        val jsonStr = sendRequest(ETERNALS_ENDPOINT)
        val json = StringUtil.extractJSONFromString<ApiEternalsResponse>(jsonStr)

        ETERNALS_MAPPING = HashMap(json.statstoneData.flatMap { data -> data.statstones.map { it.contentId to it.milestones } }.toMap())
    }

    private fun sendRequest(url: String): String {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

        return connection.getInputStream().bufferedReader().use { it.readText() }
    }
}