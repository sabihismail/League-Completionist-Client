package league.models

data class ImageCacheInfo(val folder: String, val endpoint: String) {
    override fun toString(): String {
        return "ImageCacheInfo(folder='$folder', endpoint='$endpoint')"
    }
}