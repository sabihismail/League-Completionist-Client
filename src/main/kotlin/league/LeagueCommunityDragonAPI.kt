package league

import javafx.scene.effect.*
import javafx.scene.image.Image
import league.models.ChampionInfo
import league.models.QueueInfo
import league.models.RoleMapping
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.Role
import util.StringUtil
import util.constants.ViewConstants
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.notExists

object LeagueCommunityDragonAPI {
    private const val IMAGE_ENDPOINT = "https://cdn.communitydragon.org/latest/champion/%d/square"
    private const val CHAMPION_ROLE_ENDPOINT = "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-champion-statistics/global/default/rcp-fe-lol-champion-statistics.js"
    private const val QUEUE_TYPE_ENDPOINT = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/queues.json"
    private val IMAGE_CACHE_FOLDER = Paths.get(Paths.get("").toAbsolutePath().toString(), "/cache")

    var ROLE_MAPPING = hashMapOf<Role, HashMap<Int, Float>>()
    var QUEUE_MAPPING = hashMapOf<Int, QueueInfo>()

    fun getQueueMapping(id: Int): QueueInfo {
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

    fun getChampionImage(id: Int): Image {
        val path = getChampionImagePath(id)

        return Image(path.toUri().toString())
    }

    fun getChampionImagePath(id: Int): Path {
        if (IMAGE_CACHE_FOLDER.notExists()) {
            IMAGE_CACHE_FOLDER.createDirectory()
        }

        val imagePath = IMAGE_CACHE_FOLDER.resolve("$id.png")

        if (!imagePath.exists()) {
            val urlStr = IMAGE_ENDPOINT.format(id)

            val connection = URL(urlStr).openConnection()
            connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

            val readableByteChannel = Channels.newChannel(connection.getInputStream())
            val fileOutputStream = FileOutputStream(imagePath.toFile())

            fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
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

    private fun populateQueueMapping() {
        QUEUE_MAPPING.clear()

        val connection = URL(QUEUE_TYPE_ENDPOINT).openConnection()
        connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

        val jsonStr = connection.getInputStream().bufferedReader().use { it.readText() }
        val json = StringUtil.extractJSONMapFromString<QueueInfo>(jsonStr)

        QUEUE_MAPPING = HashMap(json.mapKeys { it.key.toInt() })
    }

    private fun populateRoleMapping() {
        ROLE_MAPPING.clear()

        val connection = URL(CHAMPION_ROLE_ENDPOINT).openConnection()
        connection.setRequestProperty("User-Agent", "LoL-Mastery-Box-Client")

        val jsonStr = connection.getInputStream().bufferedReader().use { it.readText() }
        val json = StringUtil.extractJSONFromString<RoleMapping>(jsonStr, "a.exports=")

        ROLE_MAPPING[Role.TOP] = json.top
        ROLE_MAPPING[Role.JUNGLE] = json.jungle
        ROLE_MAPPING[Role.MIDDLE] = json.middle
        ROLE_MAPPING[Role.BOTTOM] = json.bottom
        ROLE_MAPPING[Role.SUPPORT] = if (json.support.isNullOrEmpty()) json.utility!! else json.support
    }
}