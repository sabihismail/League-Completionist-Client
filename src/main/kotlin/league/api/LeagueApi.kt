package league.api

import db.DatabaseImpl
import db.GenericKeyValueKey
import league.LeagueConnection
import league.models.enums.CacheType
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
import kotlin.reflect.KMutableProperty0


object LeagueApi {
    var WIN_SUMMONERS_RIFT_MAPPING = hashMapOf<Int, Boolean>()
    var WIN_BOT_GAMES_MAPPING = hashMapOf<Int, Boolean>()
    var PENTAKILL_MAPPING = hashMapOf<Int, Boolean>()
    var WIN_WITHOUT_DYING_MAPPING = hashMapOf<Int, Boolean>()

    fun getChampionWinInSummonersRift(championId: Int): Boolean {
        if (LeagueConnection.summonerInfo.uniqueId == 0L) return false
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_SUMMONERS_RIFT_MAPPING, ::updateMatchHistory, append = getAppend())

        return getOrPut(::WIN_SUMMONERS_RIFT_MAPPING, championId)
    }

    fun getChampionWinInBotGames(championId: Int): Boolean {
        if (LeagueConnection.summonerInfo.uniqueId == 0L) return false
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_BOT_GAMES_MAPPING, ::updateMatchHistory, append = getAppend())

        return getOrPut(::WIN_BOT_GAMES_MAPPING, championId)
    }

    fun getChampionGotPentakill(championId: Int): Boolean {
        if (LeagueConnection.summonerInfo.uniqueId == 0L) return false
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::PENTAKILL_MAPPING, ::updateMatchHistory, append = getAppend())

        return getOrPut(::PENTAKILL_MAPPING, championId)
    }

    fun getChampionWonWithoutDying(championId: Int): Boolean {
        if (LeagueConnection.summonerInfo.uniqueId == 0L) return false
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_WITHOUT_DYING_MAPPING, ::updateMatchHistory, append = getAppend())

        return getOrPut(::WIN_WITHOUT_DYING_MAPPING, championId)
    }

    fun updateMatchHistory() {
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
            val matches = matchesLst.mapIndexed { i, matchId ->
                Logging.log("Loaded Match: ${i + 1}/${matchesLst.size}", LogType.INFO, carriageReturn = (i + 1) / matchesLst.size)
                matchBuilder.withId(matchId).match
            }

            val participantList = matches.map { it to it.participants?.firstOrNull { participant -> participant.summonerId == summoner.summonerId } }
                .filter { it.second != null }
            participantList.asSequence()
                .filter { !isBot(it) }
                .filter { !isAram(it) }
                .filter { it.second?.didWin() ?: false }
                .map { it.second?.championId!! to true }
                .distinctBy { it.first }.toList()
                .forEach { safeSet(WIN_SUMMONERS_RIFT_MAPPING, it) }

            participantList.asSequence()
                .filter { isBot(it) }
                .filter { !isAram(it) }
                .filter { it.second?.didWin() ?: false }
                .map { it.second?.championId!! to true }
                .distinctBy { it.first }
                .forEach { safeSet(WIN_BOT_GAMES_MAPPING, it) }

            participantList.asSequence()
                .filter { !isBot(it) }
                .filter { !isAram(it) }
                .filter { (it.second?.pentaKills!! > 0) }
                .map { it.second?.championId!! to (it.second?.pentaKills!! > 0) }
                .distinctBy { it.first }
                .forEach { safeSet(PENTAKILL_MAPPING, it) }

            participantList.asSequence().filter { !isBot(it) }
                .filter { !isAram(it) }
                .filter { it.second?.didWin() ?: false }
                .filter { it.second?.deaths == 0 }
                .map { it.second?.championId!! to (it.second?.deaths == 0) }
                .distinctBy { it.first }.toList()
                .forEach { safeSet(WIN_WITHOUT_DYING_MAPPING, it) }

            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_SUMMONERS_RIFT_MAPPING, append = getAppend())
            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_BOT_GAMES_MAPPING, append = getAppend())
            CacheUtil.addJsonCache(CacheType.API_JSON, ::PENTAKILL_MAPPING, append = getAppend())
            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_WITHOUT_DYING_MAPPING, append = getAppend())
        }

        DatabaseImpl.setValue(GenericKeyValueKey.CHALLENGES_MATCH_HISTORY_END_DATE, endTimeEpoch)
    }

    private fun getOrPut(mapping: KMutableProperty0<HashMap<Int, Boolean>>, key: Int, default: Boolean = false): Boolean {
        return if (mapping.get().containsKey(key)) {
            mapping.get()[key]!!
        } else {
            mapping.getter.call()[key] = default
            CacheUtil.addJsonCache(CacheType.API_JSON, mapping, append = getAppend())

            default
        }
    }

    private fun isAram(it: Pair<LOLMatch, MatchParticipant?>): Boolean {
        return it.first.gameMode == GameModeType.ARAM
    }

    private fun isBot(it: Pair<LOLMatch, MatchParticipant?>): Boolean {
        return it.first.gameMode == GameModeType.CLASSIC && it.first.queue.name.contains("BOT")
    }

    private fun safeSet(mapping: MutableMap<Int, Boolean>, it: Pair<Int, Boolean>) {
        mapping[it.first] = it.second || (mapping[it.first] ?: false) == true
    }

    private fun getAppend(): String {
        return "/${LeagueConnection.summonerInfo.uniqueId}"
    }
}