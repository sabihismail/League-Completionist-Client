package league.models.json

import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel
import league.models.enums.ChallengeThresholdRewardCategory
import league.models.enums.GameMode


@kotlinx.serialization.Serializable
@Suppress("unused")
class ChallengeInfo {
    var id: Long? = null
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
    var gameModes: List<String>? = null
    var hasLeaderboard: Boolean? = null
    var iconPath: String? = null
    var isApex: Boolean? = null
    var isCapstone: Boolean? = null
    var isReverseDirection: Boolean? = null
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

    var thresholds: Map<ChallengeLevel, ChallengeThreshold>? = null
    var category: ChallengeCategory? = null
    var currentLevel: ChallengeLevel? = null

    val isComplete get() = currentLevel == thresholds!!.keys.maxOf { x -> x }
    var rewardTitle = ""
    var rewardLevel = ChallengeLevel.NONE
    val rewardObtained get() = rewardLevel <= currentLevel!!
    var hasRewardTitle = false
    var gameModeSet = setOf<GameMode>()

    fun init() {
        initGameMode()
        initRewardTitle()
    }

    private fun initGameMode() {
        gameModeSet = gameModes!!.map { GameMode.valueOf(it) }.toSet()
    }

    private fun initRewardTitle() {
        val rewardCategory = thresholds!!.map { it.key to it.value.rewards!!.firstOrNull { reward -> reward.category == ChallengeThresholdRewardCategory.TITLE } }
            .firstOrNull { it.second != null }
        if (rewardCategory != null) {
            rewardTitle = rewardCategory.second!!.name.toString()
            rewardLevel = rewardCategory.first
            hasRewardTitle = true
            return
        }

        hasRewardTitle = false
    }

    override fun toString(): String {
        return "ChallengeInfo(id=$id, capstoneGroupId=$capstoneGroupId, capstoneGroupName=$capstoneGroupName, capstoneId=$capstoneId, " +
                "currentLevelAchievedTime=$currentLevelAchievedTime, currentThreshold=$currentThreshold, currentValue=$currentValue, description=$description, " +
                "descriptionShort=$descriptionShort, gameModes=$gameModes, hasLeaderboard=$hasLeaderboard, iconPath=$iconPath, " +
                "isApex=$isApex, isCapstone=$isCapstone, isReverseDirection=$isReverseDirection, name=$name, nextLevel=$nextLevel, " +
                "nextLevelIconPath=$nextLevelIconPath, nextThreshold=$nextThreshold, percentile=$percentile, pointsAwarded=$pointsAwarded, position=$position, " +
                "previousLevel=$previousLevel, previousValue=$previousValue, source=$source, valueMapping=$valueMapping, thresholds=$thresholds, category=$category, " +
                "currentLevel=$currentLevel, rewardTitle='$rewardTitle', rewardLevel=$rewardLevel, hasRewardTitle=$hasRewardTitle)"
    }
}