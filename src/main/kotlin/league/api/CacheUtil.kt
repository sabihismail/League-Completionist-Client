package league.api

import com.stirante.lolclient.libs.com.google.gson.reflect.TypeToken
import league.models.CacheInfo
import league.models.enums.CacheType
import league.models.enums.ChallengeCategory
import league.models.json.ChallengeInfo
import util.LogType
import util.Logging
import util.constants.GenericConstants
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.reflect.KMutableProperty0

object CacheUtil {
    val CACHE_MAPPING = mutableMapOf(
        CacheType.API to CacheInfo("api"),
        CacheType.API_JSON to CacheInfo("api_json/"),

        CacheType.CHAMPION to CacheInfo("champion") { LeagueCommunityDragonApi.CHAMPION_PORTRAIT_ENDPOINT },
        CacheType.CHALLENGE to CacheInfo("challenge") { LeagueCommunityDragonApi.CHALLENGE_IMAGE_ENDPOINT },
        CacheType.JSON to CacheInfo("json/")
    )

    fun getPath(cacheType: CacheType, append: String = ""): Path {
        val info = CACHE_MAPPING[cacheType]!!
        val path = Paths.get(Paths.get("").toAbsolutePath().toString(), "/cache/${info.folder.replace(".", "_")}$append")
        path.createDirectories()

        return path
    }

    fun getEndpoint(cacheType: CacheType): String? {
        return CACHE_MAPPING[cacheType]!!.endpoint()
    }

    fun preloadChallengesCache(challengeInfo: Map<ChallengeCategory, MutableList<ChallengeInfo>>) {
        thread {
            val elements = challengeInfo.values
                .flatMap { challengeInfos -> challengeInfos.flatMap { challengeInfo -> challengeInfo.thresholds!!.keys.map { rank -> Pair(challengeInfo.id, rank) } } }
                .toList()

            val maxCount = elements.count()
            val fileWalk = Files.walk(getPath(CacheType.CHALLENGE)).count()
            if (fileWalk < maxCount) {
                thread {
                    Logging.log("Challenges - Starting Cache Download...", LogType.INFO)

                    val num = AtomicInteger(0)
                    elements.parallelStream()
                        .forEach {
                            LeagueCommunityDragonApi.getImagePath(CacheType.CHALLENGE, it.first.toString().lowercase(), it.second)

                            num.incrementAndGet()
                        }

                    while (num.get() != maxCount) {
                        Thread.sleep(1000)
                    }

                    Logging.log("Challenges - Finished Cache Download.", LogType.INFO)
                }
            }
        }
    }

    fun <T1, T2> addJsonCache(cacheType: CacheType, data: KMutableProperty0<HashMap<T1, T2>>, append: String = "") {
        val json = GenericConstants.GSON.toJson(data.get())

        val path = getPath(cacheType, append = append).resolve(data.name + ".json")
        path.deleteIfExists()
        path.createFile()
        path.writeText(json)
    }

    inline fun <reified T> checkIfJsonCached(cacheType: CacheType, data: KMutableProperty0<T>, runnable: () -> Unit, append: String = "") {
        val path = getPath(cacheType, append = append).resolve(data.name + ".json")
        if (!path.exists()) {
            runnable()
            return
        }

        val jsonStr = path.readText()
        val json: T = GenericConstants.GSON.fromJson(jsonStr, object: TypeToken<T>(){}.type)

        data.set(json)
    }
}