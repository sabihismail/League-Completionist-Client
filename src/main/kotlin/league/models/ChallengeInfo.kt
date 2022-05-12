package league.models

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName
import generated.LolChallengesUIChallenge


@Suppress("unused", "UNUSED_PARAMETER")
enum class ChallengeInfoRank(i: Int) {
    @SerializedName("NONE")
    NONE(0),
    @SerializedName("IRON")
    IRON(1),
    @SerializedName("BRONZE")
    BRONZE(2),
    @SerializedName("SILVER")
    SILVER(3),
    @SerializedName("GOLD")
    GOLD(4),
    @SerializedName("PLATINUM")
    PLATINUM(5),
    @SerializedName("DIAMOND")
    DIAMOND(6),
    @SerializedName("MASTER")
    MASTER(7),
    @SerializedName("GRANDMASTER")
    GRANDMASTER(8),
    @SerializedName("CHALLENGER")
    CHALLENGER(9),
}

@Suppress("unused")
class ChallengeThresholdRewardInfo {
    var asset: String? = null
    var category: String? = null
    var name: String? = null
    var quantity: Double? = null
}

@Suppress("unused")
class ChallengeThresholdInfo {
    var rewards: ArrayList<ChallengeThresholdRewardInfo>? = null
    var value: Double? = null
}

@Suppress("unused")
class ChallengeInfo : LolChallengesUIChallenge() {
    @SerializedName("thresholds")
    var thresholds: List<ChallengeThresholdInfo>? = null
}