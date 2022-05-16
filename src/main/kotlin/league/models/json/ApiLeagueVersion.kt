package league.models.json

@kotlinx.serialization.Serializable
class ApiLeagueVersion {
    var v: String? = "latest"

    companion object {
        const val DEFAULT = "latest"
    }
}