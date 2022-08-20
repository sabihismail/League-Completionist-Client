package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel
import league.models.enums.ChallengeThresholdRewardCategory
import league.models.enums.GameMode
import util.KotlinExtensionUtil.toCommaSeparatedNumber


@Serializable
@Suppress("unused")
class ChallengeInfo {
    var id: Long? = null
    var name: String? = null
    var description: String? = null
    var capstoneGroupId: Long? = null
    var capstoneGroupName: String? = null
    var capstoneId: Long? = null
    //var category: String? = null
    //var currentLevel: String? = null
    var currentLevelAchievedTime: Long? = null
    var currentThreshold: Double? = null
    var currentValue: Double? = null
    var descriptionShort: String? = null
    var gameModes: List<String>? = null
    var hasLeaderboard: Boolean? = null
    var iconPath: String? = null
    var isApex: Boolean? = null
    var isCapstone: Boolean? = null
    var isReverseDirection: Boolean? = null
    var nextLevel: ChallengeLevel? = null
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

    val allThresholds by lazy {
        thresholds!!.toList().sortedBy { it.first }
    }

    val thresholdSummaryLst by lazy {
        try {
            allThresholds.filter { it.first > nextLevel!! }
        } catch (_: Exception) {
            listOf()
        }
    }

    val thresholdSummaryMap by lazy {
        thresholdSummaryLst.associateBy { it.first to it.second }
    }

    private val thresholdSummary by lazy {
        thresholdSummaryLst.filter { it.first <= ChallengeLevel.MASTER }
            .joinToString(THRESHOLD_SEPARATOR) { it.second.value!!.toLong().toCommaSeparatedNumber() }
    }

    val thresholdSummaryAnyTier by lazy {
        thresholdSummaryLst.joinToString(THRESHOLD_SEPARATOR) { it.second.value!!.toLong().toCommaSeparatedNumber() }
    }

    val thresholdSummaryOneLiner by lazy {
        val maxNum = 18

        if (thresholdSummary.length <= maxNum) {
            thresholdSummary
        } else {
            val s = StringBuilder()

            for (value in thresholdSummary.split(THRESHOLD_SEPARATOR)) {
                if (s.length > maxNum - "...".length) break
                s.append(value)
                s.append(THRESHOLD_SEPARATOR)
            }

            val str = s.toString()
            str.substring(0, str.length - THRESHOLD_SEPARATOR.length) + "..."
        }
    }

    val currentLevelImage by lazy {
        if (currentLevel == ChallengeLevel.NONE)
            ChallengeLevel.IRON.name.lowercase()
        else
            currentLevel!!.name.lowercase()
    }

    val isComplete by lazy { currentLevel == thresholds!!.keys.maxOf { x -> x } || pointsDifference == 0 }
    var rewardTitle = ""
    var rewardLevel = ChallengeLevel.NONE
    val rewardObtained get() = rewardLevel <= currentLevel!!
    var hasRewardTitle = false
    var gameModeSet = setOf<GameMode>()
    val levelByThreshold get() = thresholds!!.keys.sorted().indexOf(currentLevel) + 1

    val percentage by lazy { currentValue!!.toDouble() / nextThreshold!! }
    val nextLevelPoints by lazy {
        try {
            thresholds!![nextLevel]!!.rewards!!.firstOrNull { it.category == ChallengeThresholdRewardCategory.CHALLENGE_POINTS }!!.quantity!!.toInt()
        } catch (_: Exception) {
            0
        }
    }
    private val previousLevelPoints by lazy {
        try {
            thresholds!![currentLevel]!!.rewards!!.firstOrNull { it.category == ChallengeThresholdRewardCategory.CHALLENGE_POINTS }!!.quantity!!.toInt()
        } catch (_: Exception) {
            0
        }
    }

    val pointsDifference by lazy { nextLevelPoints - previousLevelPoints }

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

    operator fun minus(other: ChallengeInfo): Int {
        return (currentValue!! - other.currentValue!!).toInt()
    }

    override fun toString(): String {
        return "ChallengeInfo(id=$id, name=$name, description=$description, capstoneGroupId=$capstoneGroupId, capstoneGroupName=$capstoneGroupName, " +
                "capstoneId=$capstoneId, currentLevelAchievedTime=$currentLevelAchievedTime, currentThreshold=$currentThreshold, currentValue=$currentValue, " +
                "descriptionShort=$descriptionShort, gameModes=$gameModes, hasLeaderboard=$hasLeaderboard, iconPath=$iconPath, " +
                "isApex=$isApex, isCapstone=$isCapstone, isReverseDirection=$isReverseDirection, nextLevel=$nextLevel, " +
                "nextLevelIconPath=$nextLevelIconPath, nextThreshold=$nextThreshold, percentile=$percentile, pointsAwarded=$pointsAwarded, position=$position, " +
                "previousLevel=$previousLevel, previousValue=$previousValue, source=$source, valueMapping=$valueMapping, thresholds=$thresholds, category=$category, " +
                "currentLevel=$currentLevel, rewardTitle='$rewardTitle', rewardLevel=$rewardLevel, hasRewardTitle=$hasRewardTitle)"
    }

    companion object {
        const val THRESHOLD_SEPARATOR = " > "
    }
}