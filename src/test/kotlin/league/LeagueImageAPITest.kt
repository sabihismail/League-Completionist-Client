package league

import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.test.Test

internal class LeagueImageAPITest {
    @Test
    fun getChampionImagePath() {
        val imageID = 412 // thresh

        val image = LeagueImageAPI.getChampionImagePath(imageID)

        assert(Files.exists(image))
        assert(image.fileSize() > 0)

        val image2 = LeagueImageAPI.getChampionImagePath(imageID)

        assert(Files.exists(image2))
        assert(image2.fileSize() > 0)
    }
}