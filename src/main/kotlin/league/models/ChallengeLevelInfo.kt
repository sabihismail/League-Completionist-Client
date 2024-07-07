package league.models

import league.models.enums.ChallengeLevel

data class ChallengeLevelInfo(val id: Long, val description: String, val rewardLevel: ChallengeLevel, val rewardTitle: String, val rewardValue: Double,
                              val currentLevel: ChallengeLevel, val currentLevelImage: String, val currentValue: Double, val anyImageId: Int) {
    val rewardObtained get() = rewardLevel <= currentLevel
}
