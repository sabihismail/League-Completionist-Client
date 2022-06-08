package league.models.enums

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName
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
    @SerializedName("TUTORIAL")
    TUTORIAL,
    @SerializedName("BOT", alternate = ["Beginner", "Intermediate", "Co-op vs. AI", "Intro"])
    BOT,
    @SerializedName("PRACTICETOOL")
    PRACTICE_TOOL,
    NONE,
    ANY,
    UNKNOWN;

    val isClassic get() = CLASSIC_MODES.contains(this)
    val getSerializedName: SerializedName get() = this.declaringClass.getField(this.name).getAnnotation(SerializedName::class.java)

    companion object {
        private val CLASSIC_MODES = setOf(CLASSIC, BLIND_PICK, DRAFT_PICK, RANKED_SOLO, RANKED_FLEX, CLASH, BOT)

        fun fromGameMode(str: String, queueId: Int): GameMode {
            val tmp = GameMode.values().firstOrNull { it.name == str } ?: UNKNOWN
            if (tmp != CLASSIC) return tmp

            val gameType = LeagueCommunityDragonApi.getQueueMapping(queueId)
            return GameMode.values().firstOrNull {
                val serializedName = it.getSerializedName

                serializedName.value == gameType.description || serializedName.alternate.contains(gameType.description)
            } ?: UNKNOWN
        }
    }
}