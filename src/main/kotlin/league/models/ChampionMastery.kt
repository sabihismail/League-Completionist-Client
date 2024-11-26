package league.models

import com.google.gson.annotations.SerializedName


@Suppress("unused")
class ChampionMastery {
    var championId: Int = 0
    var championLevel: Int = 0
    var championPoints: Int = 0
    var championPointsSinceLastLevel: Int = 0
    var championPointsUntilNextLevel: Int = 0
    var highestGrade: String? = null
    var lastPlayTime: Long? = null
    var markRequiredForNextLevel: Int? = null
    var nextSeasonMilestone: ChampionMasteryNextSeasonMilestone? = null
    var milestoneGrades: List<String>? = null
    var puuid: String? = null
    var tokensEarned: Int = 0

    val masteryBoxRewards by lazy {
        val orderActual = listOf(
            Pair("S-", nextSeasonMilestone?.requireGradeCounts?.sMinus),
            Pair("A-", nextSeasonMilestone?.requireGradeCounts?.aMinus),
            Pair("B-", nextSeasonMilestone?.requireGradeCounts?.bMinus),
            Pair("C-", nextSeasonMilestone?.requireGradeCounts?.cMinus),
        )

        milestoneGrades?.map { calculateScore(it) }

        orderActual.filter { it.second != 0 }.joinToString(", ") {
            "${it.first}: ${it.second}"
        }

        milestoneGrades?.joinToString(", ")!!
    }

    @Suppress("UNUSED_VARIABLE")
    private fun calculateScore(s: String) {
        val mapping = mapOf(
            "S" to 4,
            "A" to 3,
            "B" to 2,
            "C" to 1,
        )

        val minusMapping = mapOf(
            "+" to 3,
            "" to 2,
            "-" to 1,
        )
    }
}

@Suppress("unused")
class ChampionMasteryNextSeasonMilestone {
    var bonus: Boolean? = null
    var requireGradeCounts: ChampionMasteryRequireGradeCounts? = null
    var rewardConfig: ChampionMasteryRewardConfig? = null
    var rewardMarks: Int? = null
}

class ChampionMasteryRequireGradeCounts {
    @SerializedName("S-")
    var sMinus: Int = 0

    @SerializedName("A-")
    var aMinus: Int = 0

    @SerializedName("B-")
    var bMinus: Int = 0

    @SerializedName("C-")
    var cMinus: Int = 0
}

@Suppress("unused")
class ChampionMasteryRewardConfig {
    var maximumReward: Int? = null
    var rewardValue: String? = null
}