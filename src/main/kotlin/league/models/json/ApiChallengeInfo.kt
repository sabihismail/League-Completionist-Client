package league.models.json

@kotlinx.serialization.Serializable
data class ApiChallengeInfo(val challenges: Map<Int, ChallengeInfo>) {
    override fun toString(): String {
        return "ApiChallengeInfo(challenges=$challenges)"
    }
}