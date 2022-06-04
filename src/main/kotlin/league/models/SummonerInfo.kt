package league.models

import league.models.enums.SummonerStatus

data class SummonerInfo(var status: SummonerStatus = SummonerStatus.NOT_CHECKED, val accountID: Long = 0, val summonerId: Long = 0, val displayName: String = "",
                        val internalName: String = "", val percentCompleteForNextLevel: Int = 0, val summonerLevel: Int = 0, val xpUntilNextLevel: Long = 0) {
    val uniqueId by lazy { accountID.xor(summonerId) }

    fun toDisplayString(): String {
        return when (status) {
            SummonerStatus.NOT_LOGGED_IN, SummonerStatus.NOT_CHECKED -> "Not logged in."
            SummonerStatus.LOGGED_IN_UNAUTHORIZED -> "Unauthorized Login."
            SummonerStatus.LOGGED_IN_AUTHORIZED -> "Logged in as: $displayName (Lvl $summonerLevel) ($uniqueId)"
        }
    }

    override fun toString(): String {
        return "SummonerInfo(status=$status, accountID=$accountID, summonerID=$summonerId, displayName='$displayName', internalName='$internalName', " +
                "percentCompleteForNextLevel=$percentCompleteForNextLevel, summonerLevel=$summonerLevel, xpUntilNextLevel=$xpUntilNextLevel)"
    }
}