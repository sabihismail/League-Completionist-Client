package league.models.enums

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName

@Suppress("unused", "UNUSED_PARAMETER")
enum class ChallengeInfoRank(i: Int) {
    @SerializedName("NONE")
    NONE(0),
    @SerializedName("IRON")
    IRON(1),
    @SerializedName("BRONZE")
    BRONZE(2),
    @SerializedName("SILVER")
    SILVER(3),
    @SerializedName("GOLD")
    GOLD(4),
    @SerializedName("PLATINUM")
    PLATINUM(5),
    @SerializedName("DIAMOND")
    DIAMOND(6),
    @SerializedName("MASTER")
    MASTER(7),
    @SerializedName("GRANDMASTER")
    GRANDMASTER(8),
    @SerializedName("CHALLENGER")
    CHALLENGER(9),
}