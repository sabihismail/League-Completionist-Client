package league.models

enum class ChampionOwnershipStatus {
    NOT_OWNED,
    FREE_TO_PLAY,
    RENTAL,
    BOX_NOT_ATTAINED,
    BOX_ATTAINED
}

enum class SummonerStatus {
    NOT_LOGGED_IN,
    LOGGED_IN_UNAUTHORIZED,
    LOGGED_IN_AUTHORIZED,
    NOT_CHECKED,
}

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
    UNKNOWN
}

enum class Role {
    ANY,
    TOP,
    JUNGLE,
    MIDDLE,
    BOTTOM,
    SUPPORT
}
