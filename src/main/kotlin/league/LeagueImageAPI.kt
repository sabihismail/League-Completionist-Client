package league

import javafx.scene.image.Image
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
            connection.setRequestProperty("User-Agent", "LoLcuBoxes")

            val readableByteChannel = Channels.newChannel(connection.getInputStream())
            val fileOutputStream = FileOutputStream(imagePath.toFile())

            fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
        }

        return imagePath
    }
}