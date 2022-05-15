package league.models.json

@kotlinx.serialization.Serializable
@Suppress("unused")
class ChallengeThreshold {
    var rewards: List<ChallengeThresholdReward>? = null
    var value: Double? = null

    override fun toString(): String {
        return "ChallengeThresholdInfo(rewards=$rewards, value=$value)"
    }
}