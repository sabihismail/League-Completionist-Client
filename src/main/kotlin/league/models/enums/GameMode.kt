package league.models.enums

import com.google.gson.annotations.SerializedName
import league.api.LeagueCommunityDragonApi


enum class GameMode {
    @SerializedName("CLASSIC")
    CLASSIC,
    @SerializedName("Blind Pick")
    BLIND_PICK,
    @SerializedName("Draft Pick")
    DRAFT_PICK,
    @SerializedName("RANKED_SOLO_5x5", alternate = ["Ranked Solo/Duo"])
    RANKED_SOLO,
    @SerializedName("RANKED_FLEX_SR", alternate = ["Ranked Flex"])
    RANKED_FLEX,
    @SerializedName("CLASH", alternate = ["Clash"])
    CLASH,
    @SerializedName("ARAM")
    ARAM,
    @SerializedName("HEXAKILL")
    HEXAKILL,
    @SerializedName("ONEFORALL")
    ONE_FOR_ALL,
    @SerializedName("URF")
    URF,
    @SerializedName("CHERRY")
    CHERRY,
    @SerializedName("TUTORIAL")
    TUTORIAL,
    @SerializedName("BOT", alternate = ["Beginner", "Intermediate", "Co-op vs. AI", "Intro"])
    BOT,
    @SerializedName("PRACTICETOOL")
    PRACTICE_TOOL,
    @SerializedName("STRAWBERRY")
    STRAWBERRY,
    @SerializedName("SWIFTPLAY")
    SWIFTPLAY,

    NONE,
    ANY,
    UNKNOWN;

    val serializedName: SerializedName by lazy { this.declaringJavaClass.getField(this.name).getAnnotation(SerializedName::class.java) }

    val gameModeGeneric by lazy { GAME_MODE_MAPPING[this] }

    override fun toString(): String {
        return STRING_MAPPING.getOrDefault(this, this.name)
    }

    companion object {
        private val CLASSIC_MODES = setOf(CLASSIC, BLIND_PICK, DRAFT_PICK, RANKED_SOLO, RANKED_FLEX, CLASH, BOT)
        private val GAME_MODE_MAPPING = CLASSIC_MODES.associateWith { CLASSIC } + mapOf(ARAM to ARAM, CHERRY to CHERRY)

        val STRING_MAPPING = mapOf(ANY to "All", CHERRY to "Arena", CLASSIC to "Summoner's Rift")

        fun fromGameMode(str: String, queueId: Int): GameMode {
            val tmp = entries.firstOrNull { it.name == str } ?: UNKNOWN
            if (tmp != CLASSIC) return tmp

            val gameType = LeagueCommunityDragonApi.getQueueMapping(queueId)
            return entries.firstOrNull {
                val serializedName = it.serializedName

                serializedName.value == gameType.description || serializedName.alternate.contains(gameType.description)
            } ?: UNKNOWN
        }
    }
}