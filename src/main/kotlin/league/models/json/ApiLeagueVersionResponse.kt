package league.models.json

import kotlinx.serialization.Serializable

@Serializable
class ApiLeagueVersionResponse {
    var v: String? = "latest"

    companion object {
        const val DEFAULT = "latest"
    }
}