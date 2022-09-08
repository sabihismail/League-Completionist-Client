package league.models.enums

enum class ChallengeMappingEnum {
    @Suppress("unused")
    NONE,

    WIN_SUMMONERS_RIFT,
    PENTA_IN_SUMMONERS_RIFT,
    WIN_NO_DEATHS_SUMMONERS_RIFT,
    WIN_BOTS_GAME,

    S_PLUS_DIFFERENT_CHAMPIONS,
    S_MINUS_DIFFERENT_CHAMPIONS_ARAM;

    companion object {
        val mapping = mapOf(
            NONE to "None",

            WIN_SUMMONERS_RIFT to "SR",
            PENTA_IN_SUMMONERS_RIFT to "P",
            WIN_NO_DEATHS_SUMMONERS_RIFT to "0d",
            WIN_BOTS_GAME to "BOT",

            S_PLUS_DIFFERENT_CHAMPIONS to "S+",
            S_MINUS_DIFFERENT_CHAMPIONS_ARAM to "S- A",
        )
    }
}