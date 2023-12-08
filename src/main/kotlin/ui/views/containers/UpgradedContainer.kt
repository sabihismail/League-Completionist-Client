package ui.views.containers

import league.models.json.Challenge

data class UpgradedChallengesContainer(val upgraded: List<Pair<Challenge, Challenge>>, val progressed: List<Pair<Challenge, Challenge>>,
                                       val completed: List<Pair<Challenge, Challenge>>)
