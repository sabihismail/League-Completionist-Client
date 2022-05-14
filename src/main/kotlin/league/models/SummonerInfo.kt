package league.models

import league.models.enums.SummonerStatus

data class SummonerInfo(var status: SummonerStatus = SummonerStatus.NOT_CHECKED, val accountID: Long = 0, val summonerID: Long = 0, val displayName: String = "",
    val internalName: String = "", val percentCompleteForNextLevel: Int = 0, val summonerLevel: Int = 0, val xpUntilNextLevel: Long = 0) {
    override fun toString(): String {
        return "SummonerInfo(status=$status, accountID=$accountID, summonerID=$summonerID, displayName='$displayName', internalName='$internalName', " +
                "percentCompleteForNextLevel=$percentCompleteForNextLevel, summonerLevel=$summonerLevel, xpUntilNextLevel=$xpUntilNextLevel)"
    }
}