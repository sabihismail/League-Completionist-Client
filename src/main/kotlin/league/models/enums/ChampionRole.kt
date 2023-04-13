package league.models.enums

enum class ChampionRole {
    ANY,
    MAGE,
    FIGHTER,
    TANK,
    ASSASSIN,
    SUPPORT,
    MARKSMAN;

    companion object {
        fun fromString(str: String): ChampionRole {
            val upper = str.uppercase()
            val r = values().firstOrNull { it.name == upper }

            return r ?: ANY
        }
    }
}
