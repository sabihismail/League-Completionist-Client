package league.api

import com.merakianalytics.orianna.Orianna
import com.merakianalytics.orianna.types.common.Region
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object LeagueApi {
    data class LeagueApiObj(val winMapping: Map<Int, Boolean>, val winWithoutDyingList: List<Int>, val uniquePentakillMapping: Map<Int, Int>)

    fun getData(
        summonerName: String,
        region: Region = Region.NORTH_AMERICA,
        startTime: DateTime = DateTime.parse("01/06/2021", DateTimeFormat.forPattern("dd/MM/yyyy")),
        endTime: DateTime = DateTime.now()
    ): LeagueApiObj {
        val summoner = Orianna.summonerNamed(summonerName).withRegion(region).get()
        val matchHistory = summoner.matchHistory().withStartTime(startTime).withEndTime(endTime).get()

        val participantList = matchHistory.map { it?.participants?.first { participant -> participant.summoner.id == summoner.id } }
        val winMapping = participantList.filter { it?.stats?.isWinner ?: false }
            .map { it?.champion?.id!! to true }
            .distinctBy { it.first }
            .toMap()

        val winWithoutDyingList = participantList.filter { it?.stats?.isWinner ?: false }
            .filter { it?.stats?.deaths == 0 }
            .map { it?.champion?.id!! }
            .distinct()

        val pentakillMapping = participantList.map { it?.champion?.id!! to it.stats?.pentaKills!! }
            .distinctBy { it.first }
            .filter { it.second > 0 }
            .toMap()

        return LeagueApiObj(winMapping, winWithoutDyingList, pentakillMapping)
    }
}