package league.models.enums

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName

@kotlinx.serialization.Serializable
@Suppress("unused", "UNUSED_PARAMETER")
enum class EternalTrackingType(i: Int) {
    @SerializedName("0")
    COUNT(0),
    @SerializedName("1")
    TIME(1),
    @SerializedName("2")
    DISTANCE(2),
}