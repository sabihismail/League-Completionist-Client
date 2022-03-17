package league

import javafx.scene.effect.*
import javafx.scene.image.Image
import league.models.ChampionInfo
import league.models.ChampionOwnershipStatus
import ui.ViewConstants
import ui.ViewConstants.CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
import ui.ViewConstants.CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.notExists

object LeagueImageAPI {
    private const val IMAGE_ENDPOINT = "https://cdn.communitydragon.org/latest/champion/%d/square"

    private val CACHE_FOLDER = Paths.get(Paths.get("").toAbsolutePath().toString(), "/cache")

    fun getChampionImage(id: Int): Image {
        val path = getChampionImagePath(id)

        return Image(path.toUri().toString())
    }

    fun getChampionImagePath(id: Int): Path {
        if (CACHE_FOLDER.notExists()) {
            CACHE_FOLDER.createDirectory()
        }

        val imagePath = CACHE_FOLDER.resolve("$id.png")

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
                CHAMPION_STATUS_UNAVAILABLE_CHEST_COLOR
            else
                CHAMPION_STATUS_AVAILABLE_CHEST_COLOR
        }

        val blend = Blend().apply {
            mode = BlendMode.SRC_OVER
            opacity = 0.7
            topInput = colorInput
        }

        return blend
    }
}