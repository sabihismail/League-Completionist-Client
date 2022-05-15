package league.models.json

import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel

@Suppress("unused")
class ChallengeSummaryCategoryProgress {
    var category: ChallengeCategory? = null
    var current: Int? = null
    var level: ChallengeLevel? = null
    var max: Int? = null
    var positionPercentile: Double? = null

    override fun toString(): String {
        return "ChallengeSummaryCategoryProgress(category=$category, current=$current, level=$level, max=$max, positionPercentile=$positionPercentile)"
    }
}