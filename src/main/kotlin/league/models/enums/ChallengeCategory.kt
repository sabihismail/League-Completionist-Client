package league.models.enums

import com.google.gson.annotations.SerializedName

@Suppress("unused", "UNUSED_PARAMETER")
enum class ChallengeCategory(i: Int) {
    @SerializedName("NONE")
    NONE(0),
    @SerializedName("EXPERTISE")
    EXPERTISE(1),
    @SerializedName("TEAMWORK")
    TEAMWORK(2),
    @SerializedName("IMAGINATION")
    IMAGINATION(3),
    @SerializedName("VETERANCY")
    VETERANCY(4),
    @SerializedName("COLLECTION")
    COLLECTION(5),
    @SerializedName("LEGACY")
    LEGACY(6),
}