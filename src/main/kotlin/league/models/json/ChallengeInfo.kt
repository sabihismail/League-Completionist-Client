package league.models.json

import generated.LolChallengesFriendLevelsData
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeInfoRank


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