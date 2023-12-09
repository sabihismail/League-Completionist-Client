package league.api

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import league.models.CacheInfo
import league.models.enums.CacheType
import util.constants.GenericConstants
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.reflect.KMutableProperty0

object CacheUtil {
    val FILE_LOCKS = hashMapOf<Path, Mutex>()

    private val CACHE_MAPPING = mutableMapOf(
        CacheType.API to CacheInfo("api"),
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

    fun <T1, T2> addJsonCache(cacheType: CacheType, data: KMutableProperty0<HashMap<T1, T2>>, append: String = "") {
        val json = GenericConstants.GSON_PRETTY.toJson(data.get())

        val path = getPath(cacheType, append = append).resolve(data.name + ".json")

        val mutex = FILE_LOCKS.getOrDefault(path, Mutex())
        runBlocking {
            mutex.withLock {
                path.deleteIfExists()
                path.createFile()
                path.writeText(json)
            }
        }
    }

    inline fun <reified T> checkIfJsonCached(cacheType: CacheType, data: KMutableProperty0<T>, runnable: () -> Unit, append: String = "") {
        val path = getPath(cacheType, append = append).resolve(data.name + ".json")

        if (!path.exists()) {
            runnable()
            return
        }

        val mutex = FILE_LOCKS.getOrDefault(path, Mutex())
        runBlocking {
            mutex.withLock {
                val jsonStr = path.readText()
                val json: T = GenericConstants.GSON_PRETTY.fromJson(jsonStr, object : TypeToken<T>() {}.type)

                data.set(json)
            }
        }
    }
}