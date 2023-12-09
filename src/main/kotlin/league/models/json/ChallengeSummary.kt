package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel


@Serializable
@Suppress("unused")
class ChallengeSummaryCategoryProgress {
    var category: ChallengeCategory? = null
    var current: Int? = null
    var level: ChallengeLevel? = null
    private var max: Int? = null
    var positionPercentile: Double? = null

    override fun toString(): String {
        return "ChallengeSummaryCategoryProgress(category=$category, current=$current, level=$level, max=$max, positionPercentile=$positionPercentile)"
    }
}

@Serializable
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ChallengeSummary {
    var apexLadderUpdateTime: Long? = null
    var categoryProgress: List<ChallengeSummaryCategoryProgress>? = null
    var overallChallengeLevel: ChallengeLevel? = null
    var pointsUntilNextRank: Long? = null
    var positionPercentile: Double? = null
    var topChallenges: List<Challenge>? = null
    var totalChallengeScore: Long? = null

    fun getLevelByCategory(it: ChallengeCategory): ChallengeLevel {
        val elem = categoryProgress!!.firstOrNull { progress -> progress.category == it }

        return elem?.level ?: ChallengeLevel.NONE
    }

    override fun toString(): String {
        return "ChallengeSummary(apexLadderUpdateTime=$apexLadderUpdateTime, categoryProgress=$categoryProgress, overallChallengeLevel=$overallChallengeLevel, " +
                "pointsUntilNextRank=$pointsUntilNextRank, positionPercentile=$positionPercentile, topChallenges=$topChallenges, " +
                "totalChallengeScore=$totalChallengeScore)"
    }
}