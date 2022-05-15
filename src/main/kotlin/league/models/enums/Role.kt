package league.models.enums

enum class Role {
    ANY,
    TOP,
    JUNGLE,
    MIDDLE,
    BOTTOM,
    SUPPORT;

    companion object {
        fun fromString(str: String): Role {
            val upper = str.uppercase()
            val r = values().firstOrNull { it.name == upper }

            return r ?: ANY
        }
    }
}
