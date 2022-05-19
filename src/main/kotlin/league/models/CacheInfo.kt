package league.models

data class CacheInfo(val folder: String, var endpoint: String? = null) {
    override fun toString(): String {
        return "ImageCacheInfo(folder='$folder', endpoint='$endpoint')"
    }
}