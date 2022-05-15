package league.models.enums

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName

enum class GameModeChallenge {
    ALL,
    @SerializedName("CLASSIC")
    CLASSIC,
    @SerializedName("ARAM")
    ARAM,
}