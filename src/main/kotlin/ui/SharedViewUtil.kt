package ui

import league.models.json.Challenge

object SharedViewUtil {
    private const val EMPTY_CHALLENGE_NAME = "All Challenges"

    fun getEmptyChallenge(): Challenge {
        return Challenge().apply { name = EMPTY_CHALLENGE_NAME }
    }

    fun Challenge.isEmptyChallenge(): Boolean {
        return name == EMPTY_CHALLENGE_NAME
    }
}