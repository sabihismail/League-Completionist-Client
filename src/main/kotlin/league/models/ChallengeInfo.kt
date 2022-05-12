package league.models

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName
import generated.LolChallengesFriendLevelsData


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

@Suppress("unused", "UNUSED_PARAMETER")
enum class ChallengeCategory(i: Int) {
    @SerializedName("EXPERTISE")
    EXPERTISE(1),
    @SerializedName("TEAMWORK")
    TEAMWORK(2),
    @SerializedName("IMAGINATION")
    IMAGINATION(3),
    @SerializedName("VETERANCY")
    VETERANCY(4),
    @SerializedName("COLLECTION")
    COLLECTION(5),
    @SerializedName("LEGACY")
    LEGACY(6),
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
    var rewards: List<ChallengeThresholdRewardInfo>? = null
    var value: Double? = null
}

@Suppress("unused")
class ChallengeInfo {
    var capstoneGroupId: Long? = null
    var capstoneGroupName: String? = null
    var capstoneId: Long? = null
    //var category: String? = null
    //var currentLevel: String? = null
    var currentLevelAchievedTime: Long? = null
    var currentThreshold: Double? = null
    var currentValue: Double? = null
    var description: String? = null
    var descriptionShort: String? = null
    var friendsAtLevels: List<LolChallengesFriendLevelsData>? = null
    var gameModes: List<String>? = null
    var hasLeaderboard: Boolean? = null
    var iconPath: String? = null
    var id: Long? = null
    var isApex: Boolean? = null
    var isCapstone: Boolean? = null
    var isReverseDirection: Boolean? = null
    var levelToIconPath: Any? = null
    var name: String? = null
    var nextLevel: String? = null
    var nextLevelIconPath: String? = null
    var nextThreshold: Double? = null
    var percentile: Double? = null
    var pointsAwarded: Long? = null
    var position: Int? = null
    var previousLevel: String? = null
    var previousValue: Double? = null
    var source: String? = null
    //var thresholds: Any? = null
    var valueMapping: String? = null

    var thresholds: Map<ChallengeInfoRank, ChallengeThresholdInfo>? = null
    var category: ChallengeCategory? = null
    var currentLevel: ChallengeInfoRank? = null

    val isComplete get() = currentLevel == thresholds!!.keys.maxOf { x -> x }
}