package league.models

import com.google.gson.annotations.SerializedName


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
    var puuid: String? = null
    var tokensEarned: Int = 0
}

class ChampionMasteryNextSeasonMilestone {
    var bonus: Boolean? = null
    var requireGradeCounts: ChampionMasteryRequireGradeCounts? = null
    var rewardConfig: ChampionMasteryRewardConfig? = null
    var rewardMarks: Int? = null
}

class ChampionMasteryRequireGradeCounts {
    @SerializedName("B-")
    var bMinus: Int = 0
    @SerializedName("C-")
    var cMinus: Int = 0
}

class ChampionMasteryRewardConfig {
    var maximumReward: Int? = null
    var rewardValue: String? = null
}