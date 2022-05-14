package league.models.json

import generated.LolChallengesFriendLevelsData
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeRank
import league.models.enums.ChallengeThresholdRewardCategory


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

    var thresholds: Map<ChallengeRank, ChallengeThreshold>? = null
    var category: ChallengeCategory? = null
    var currentLevel: ChallengeRank? = null

    val isComplete get() = currentLevel == thresholds!!.keys.maxOf { x -> x }
    var rewardTitle = ""
    var hasRewardTitle = false

    fun getRewardTitle() {
        val rewards = thresholds!!.values.sortedByDescending { it.value }[0].rewards!!
        val category = rewards.firstOrNull { it.category == ChallengeThresholdRewardCategory.TITLE }

        if (category != null) {
            rewardTitle = category.name!!
            hasRewardTitle = true
            return
        }

        hasRewardTitle = false
    }

    override fun toString(): String {
        return "ChallengeInfo(capstoneGroupId=$capstoneGroupId, capstoneGroupName=$capstoneGroupName, capstoneId=$capstoneId, " +
                "currentLevelAchievedTime=$currentLevelAchievedTime, currentThreshold=$currentThreshold, currentValue=$currentValue, " +
                "description=$description, descriptionShort=$descriptionShort, friendsAtLevels=$friendsAtLevels, gameModes=$gameModes, " +
                "hasLeaderboard=$hasLeaderboard, iconPath=$iconPath, id=$id, isApex=$isApex, isCapstone=$isCapstone, isReverseDirection=$isReverseDirection, " +
                "levelToIconPath=$levelToIconPath, name=$name, nextLevel=$nextLevel, nextLevelIconPath=$nextLevelIconPath, nextThreshold=$nextThreshold, " +
                "percentile=$percentile, pointsAwarded=$pointsAwarded, position=$position, previousLevel=$previousLevel, previousValue=$previousValue, source=$source, " +
                "valueMapping=$valueMapping, thresholds=$thresholds, category=$category, currentLevel=$currentLevel)"
    }
}