package league.models.enums

import com.stirante.lolclient.libs.com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class ChallengeThresholdRewardCategory {
    @SerializedName("CHALLENGE_POINTS")
    CHALLENGE_POINTS,
    @SerializedName("TITLE")
    TITLE,
}