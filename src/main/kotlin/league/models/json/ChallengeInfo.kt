package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel
import league.models.enums.ChallengeThresholdRewardCategory
import league.models.enums.GameMode
import util.KotlinExtensionUtil.toCommaSeparatedNumber
import util.constants.GenericConstants
import kotlin.math.abs


@Serializable
@Suppress("unused")
class ChallengeInfo {
    var availableIds: Array<Double>? = null
    var capstoneGroupId: Double? = null
    var capstoneGroupName: String? = null
    var category: ChallengeCategory? = null
    private var completedIds: Array<Double>? = null
    var currentLevel: ChallengeLevel? = null
    var currentLevelAchievedTime: Double? = null
    var currentThreshold: Double? = null
    var currentValue: Double? = null
    var description: String? = null
    private var descriptionShort: String? = null
    // var friendsAtLevels: List<>
    private var gameModes: Array<String>? = null
    var hasLeaderboard: Boolean = false
    var id: Double? = null
    var idListType: String? = null
    var isApex: Boolean? = null
    var isCapstone: Boolean? = null
    var isReverseDirection: Boolean? = null
    var name: String? = null
    private var nextLevel: ChallengeLevel? = null
    var nextThreshold: Double? = null
    var parentId: Double? = null
    var parentName: String? = null
    var percentile: Double? = null
    var pointsAwarded: Double? = null
    var position: Double? = null
    var previousLevel: ChallengeLevel? = null
    var previousValue: Double? = null
    var priority: Double? = null
    var retireTimestamp: Double? = null
    var source: String? = null
    var thresholds: Map<ChallengeLevel, ChallengeThreshold>? = null
    var valueMapping: String? = null

    val allThresholds by lazy {
        thresholds!!.toList().sortedBy { it.first }
    }

    private val maxThreshold by lazy {
        allThresholds.last().first
    }

    val maxThresholdReached by lazy {
        maxThreshold == currentLevel
    }

    private val thresholdSummaryLst by lazy {
        try {
            allThresholds.filter { it.first > nextLevel!! }
        } catch (_: Exception) {
            listOf()
        }
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

    val pointsDifference by lazy { abs(nextLevelPoints - previousLevelPoints) }

    val descriptiveDescription by lazy {
        description + if (name?.contains(GenericConstants.YEAR) == false) "" else " (${GenericConstants.YEAR})"
    }

    val availableIdsInt by lazy { availableIds?.map { it.toInt() }?.toSet() }
    val completedIdsInt by lazy { completedIds?.map { it.toInt() }?.toSet() }
    val isListingCompletedChampions by lazy {
        val contains = setOf("\u003cem\u003e")
        val ignore = setOf("5-stack", "Mastery 7", "Mastery 5", "Obtain", "premade 5", "mythic items", "champion skins")

        return@lazy contains.all { descriptionShort?.contains(it) == true } && ignore.all { description?.contains(it) == false }
    }

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
        return description!! + (if (isComplete) " (DONE)" else "")
    }

    companion object {
        const val THRESHOLD_SEPARATOR = " > "
    }
}