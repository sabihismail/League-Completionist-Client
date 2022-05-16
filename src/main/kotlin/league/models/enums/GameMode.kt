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
    @SerializedName("BOT", alternate = ["Beginner", "Intermediate", "Co-op vs. AI"])
    BOT,
    @SerializedName("PRACTICETOOL")
    PRACTICE_TOOL,
    NONE,
    ALL,
    UNKNOWN;

    val isClassic get() = CLASSIC_MODES.contains(this)
    val getSerializedName: SerializedName get() = this.declaringClass.getField(this.name).getAnnotation(SerializedName::class.java)

    companion object {
        private val CLASSIC_MODES = setOf(CLASSIC, BLIND_PICK, DRAFT_PICK, RANKED_SOLO, RANKED_FLEX, CLASH, BOT)

        fun fromGameMode(str: String, queueId: Int): GameMode {
            var tmp = GameMode.values().firstOrNull { it.name == str } ?: UNKNOWN
            /*
            var tmp = when (str) {
                "CLASSIC" -> CLASSIC
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
            }*/

            if (tmp == CLASSIC) {
                val gameType = LeagueCommunityDragonApi.getQueueMapping(queueId)
                tmp = GameMode.values().firstOrNull {
                    val serializedName = it.getSerializedName

                    serializedName.value == gameType.description || serializedName.alternate.contains(gameType.description)
                } ?: UNKNOWN

                /*
                tmp = when (gameType.description) {
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
                 */
            }

            return tmp
        }
    }
}