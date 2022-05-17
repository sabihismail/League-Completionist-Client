package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiChallengeResponse(val challenges: Map<Int, ChallengeInfo>) {
    override fun toString(): String {
        return "ApiChallengeInfo(challenges=$challenges)"
    }
}