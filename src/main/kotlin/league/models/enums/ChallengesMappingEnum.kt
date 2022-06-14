package league.models.enums

enum class ChallengesMappingEnum {
    @Suppress("unused")
    NONE,

    WIN_SUMMONERS_RIFT,
    PENTA_IN_SUMMONERS_RIFT,
    NO_DEATHS_SUMMONERS_RIFT,
    WIN_BOTS_GAME;

    companion object {
        val mapping = mapOf(
            NONE to "None",

            WIN_SUMMONERS_RIFT to "SR",
            PENTA_IN_SUMMONERS_RIFT to "P",
            NO_DEATHS_SUMMONERS_RIFT to "0d",
            WIN_BOTS_GAME to "BOT",
        )
    }
}