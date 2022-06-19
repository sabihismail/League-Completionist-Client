package league.api

import db.DatabaseImpl
import db.GenericKeyValueKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import league.LeagueConnection
import league.models.enums.CacheType
import league.models.enums.ChallengeMappingEnum
import no.stelar7.api.r4j.basic.APICredentials
import no.stelar7.api.r4j.basic.cache.impl.FileSystemCacheProvider
import no.stelar7.api.r4j.basic.calling.DataCall
import no.stelar7.api.r4j.basic.constants.types.lol.GameModeType
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner
import util.LogType
import util.Logging
import util.Settings
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


object LeagueApi {
    private val mutex = Mutex()

    fun updateMatchHistory() {
        runBlocking {
            mutex.withLock {
                val settings = Settings.get()

                DataCall.setCredentials(APICredentials(settings.riotApiKey, "", "", "", ""))
                DataCall.setCacheProvider(FileSystemCacheProvider(CacheUtil.getPath(CacheType.API)))

                val lastEndDate = DatabaseImpl.getValue(GenericKeyValueKey.CHALLENGES_MATCH_HISTORY_END_DATE)
                val startTime = if (!lastEndDate.isNullOrEmpty())
                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(lastEndDate.toLong()), ZoneId.of("UTC"))
                else
                    ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC")) // Technically June 16th but whatever

                val startTimeEpoch = startTime.toEpochSecond()
                val endTimeEpoch = ZonedDateTime.now(ZoneId.of("UTC")).toEpochSecond()

                val summoner = Summoner.byName(LeagueConnection.summonerInfo.region, LeagueConnection.summonerInfo.displayName)
                val matchBuilder = MatchBuilder(summoner.platform)
                val matchesLst = summoner.leagueGames.withStartTime(startTimeEpoch).withEndTime(endTimeEpoch).lazy
                matchesLst.loadFully()

                if (matchesLst.isNotEmpty()) {
                    var counter = 0
                    val matches = matchesLst.map { matchId ->
                        counter++

                        Logging.log("Loaded Match: ${counter}/${matchesLst.size}", LogType.INFO, carriageReturn = counter / matchesLst.size)
                        matchBuilder.withId(matchId).match
                    }

                    val participantList = matches.map { it to it.participants?.firstOrNull { participant -> participant.summonerId == summoner.summonerId } }
                        .filter { it.second != null }
                    participantList.asSequence()
                        .filter { !isBot(it) }
                        .filter { !isAram(it) }
                        .filter { it.second?.didWin() ?: false }
                        .map { it.second?.championId!! to true }
                        .distinctBy { it.first }
                        .filter { it.second }
                        .forEach { DatabaseImpl.setChallengeComplete(ChallengeMappingEnum.WIN_SUMMONERS_RIFT, it.first) }

                    participantList.asSequence()
                        .filter { isBot(it) }
                        .filter { !isAram(it) }
                        .filter { it.second?.didWin() ?: false }
                        .map { it.second?.championId!! to true }
                        .distinctBy { it.first }
                        .filter { it.second }
                        .forEach { DatabaseImpl.setChallengeComplete(ChallengeMappingEnum.WIN_BOTS_GAME, it.first) }

                    participantList.asSequence()
                        .filter { !isBot(it) }
                        .filter { !isAram(it) }
                        .filter { (it.second?.pentaKills!! > 0) }
                        .map { it.second?.championId!! to (it.second?.pentaKills!! > 0) }
                        .distinctBy { it.first }
                        .filter { it.second }
                        .forEach { DatabaseImpl.setChallengeComplete(ChallengeMappingEnum.PENTA_IN_SUMMONERS_RIFT, it.first) }

                    participantList.asSequence().filter { !isBot(it) }
                        .filter { !isAram(it) }
                        .filter { it.second?.didWin() ?: false }
                        .filter { it.second?.deaths == 0 }
                        .map { it.second?.championId!! to (it.second?.deaths == 0) }
                        .distinctBy { it.first }.toList()
                        .forEach { DatabaseImpl.setChallengeComplete(ChallengeMappingEnum.WIN_NO_DEATHS_SUMMONERS_RIFT, it.first) }
                }

                DatabaseImpl.setValue(GenericKeyValueKey.CHALLENGES_MATCH_HISTORY_END_DATE, endTimeEpoch)
            }
        }
    }

    private fun isAram(it: Pair<LOLMatch, MatchParticipant?>): Boolean {
        return it.first.gameMode == GameModeType.ARAM
    }

    private fun isBot(it: Pair<LOLMatch, MatchParticipant?>): Boolean {
        return it.first.gameMode == GameModeType.CLASSIC && it.first.queue.name.contains("BOT")
    }
}