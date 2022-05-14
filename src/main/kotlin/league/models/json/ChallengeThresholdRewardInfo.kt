package league.models.json

@Suppress("unused")
class ChallengeThresholdRewardInfo {
    var asset: String? = null
    var category: String? = null
    var name: String? = null
    var quantity: Double? = null

    override fun toString(): String {
        return "ChallengeThresholdRewardInfo(asset=$asset, category=$category, name=$name, quantity=$quantity)"
    }
}