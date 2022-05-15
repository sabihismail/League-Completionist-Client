package league.models.json

import generated.LolChallengesUITitle
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ChallengeSummary {
    var apexLadderUpdateTime: Long? = null
    var categoryProgress: List<ChallengeSummaryCategoryProgress>? = null
    var overallChallengeLevel: ChallengeLevel? = null
    var pointsUntilNextRank: Long? = null
    var positionPercentile: Double? = null
    var title: LolChallengesUITitle? = null
    var topChallenges: List<ChallengeInfo>? = null
    var totalChallengeScore: Long? = null

    fun getLevelByCategory(it: ChallengeCategory): ChallengeLevel {
        val elem = categoryProgress!!.firstOrNull { progress -> progress.category == it }

        return elem?.level ?: ChallengeLevel.NONE
    }

    override fun toString(): String {
        return "ChallengeSummary(apexLadderUpdateTime=$apexLadderUpdateTime, categoryProgress=$categoryProgress, overallChallengeLevel=$overallChallengeLevel, pointsUntilNextRank=$pointsUntilNextRank, positionPercentile=$positionPercentile, title=$title, topChallenges=$topChallenges, totalChallengeScore=$totalChallengeScore)"
    }
}