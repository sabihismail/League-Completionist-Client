package league.models.json.communitydragon

import kotlinx.serialization.Serializable
import league.models.json.Challenge

@Serializable
data class ApiChallengeResponse(val challenges: Map<Int, Challenge>) {
    override fun toString(): String {
        return "ApiChallengeInfo(challenges=$challenges)"
    }
}