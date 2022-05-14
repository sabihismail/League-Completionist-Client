package league.models.json

@Suppress("unused")
class ChallengeThresholdInfo {
    var rewards: List<ChallengeThresholdRewardInfo>? = null
    var value: Double? = null

    override fun toString(): String {
        return "ChallengeThresholdInfo(rewards=$rewards, value=$value)"
    }
}