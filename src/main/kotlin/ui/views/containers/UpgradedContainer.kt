package ui.views.containers

import league.models.json.ChallengeInfo

data class UpgradedChallengesContainer(val upgraded: List<Pair<ChallengeInfo, ChallengeInfo>>, val progressed: List<Pair<ChallengeInfo, ChallengeInfo>>,
                                       val completed: List<Pair<ChallengeInfo, ChallengeInfo>>)
