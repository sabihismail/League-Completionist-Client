package league.models.enums

import league.api.LeagueCommunityDragonAPI

enum class GameMode {
    NONE,
    BLIND_PICK,
    DRAFT_PICK,
    RANKED_SOLO,
    RANKED_FLEX,
    CLASH,
    ARAM,
    HEXAKILL,
    ONE_FOR_ALL,
    URF,
    TUTORIAL,
    BOT,
    PRACTICE_TOOL,
    UNKNOWN;

    companion object {
        fun fromGameMode(str: String, queueId: Int): GameMode {
            var ret = when (str) {
                "CLASSIC" -> BLIND_PICK
                "RANKED_SOLO_5x5" -> RANKED_SOLO
                "RANKED_FLEX_SR" -> RANKED_FLEX
                "CLASH" -> CLASH
                "ARAM" -> ARAM
                "HEXAKILL" -> HEXAKILL
                "ONEFORALL" -> ONE_FOR_ALL
                "URF" -> URF
                "TUTORIAL" -> TUTORIAL
                "BOT" -> BOT
                "PRACTICETOOL" -> PRACTICE_TOOL
                else -> UNKNOWN
            }

            if (ret == BLIND_PICK) {
                val gameType = LeagueCommunityDragonAPI.getQueueMapping(queueId)

                ret = when (gameType.description) {
                    "Blind Pick" -> BLIND_PICK
                    "Draft Pick" -> DRAFT_PICK
                    "Ranked Solo/Duo" -> RANKED_SOLO
                    "Ranked Flex" -> RANKED_FLEX
                    "Clash" -> CLASH
                    "Beginner" -> BOT
                    "Intermediate" -> BOT
                    "Co-op vs. AI" -> BOT
                    else -> UNKNOWN
                }
            }

            return ret
        }
    }
}