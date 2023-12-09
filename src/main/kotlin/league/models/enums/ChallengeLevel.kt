package league.models.enums

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@Suppress("unused", "UNUSED_PARAMETER")
enum class ChallengeLevel(i: Int) {
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("NONE", "")
    @SerializedName("NONE", alternate = [""])
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