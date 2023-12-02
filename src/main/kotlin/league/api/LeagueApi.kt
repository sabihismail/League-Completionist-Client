package league.api

import league.LeagueConnection
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner


object LeagueApi {
    fun getCurrentGameChampionId(): Int {
        val summoner = Summoner.byName(LeagueConnection.summonerInfo.region, LeagueConnection.summonerInfo.displayName)

        return summoner.currentGame.participants.first { it.summonerId == summoner.summonerId }.championId
    }
}