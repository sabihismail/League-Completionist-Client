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
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_SUMMONERS_RIFT_MAPPING, ::populateData)

        return getOrPut(::WIN_SUMMONERS_RIFT_MAPPING, championId)
    }

    fun getChampionWinInBotGames(championId: Int): Boolean {
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_BOT_GAMES_MAPPING, ::populateData)

        return getOrPut(::WIN_BOT_GAMES_MAPPING, championId)
    }

    fun getChampionGotPentakill(championId: Int): Boolean {
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::PENTAKILL_MAPPING, ::populateData)

        return getOrPut(::PENTAKILL_MAPPING, championId)
    }

    fun getChampionWonWithoutDying(championId: Int): Boolean {
        CacheUtil.checkIfJsonCached(CacheType.API_JSON, ::WIN_WITHOUT_DYING_MAPPING, ::populateData)

        return getOrPut(::WIN_WITHOUT_DYING_MAPPING, championId)
    }

    private fun populateData() {
        val settings = Settings.get()

        DataCall.setCredentials(APICredentials(settings.riotApiKey, "", "", "", ""))
        DataCall.setCacheProvider(FileSystemCacheProvider(CacheUtil.getPath(CacheType.API)))

        val lastEndDate = DatabaseImpl.getValue(GenericKeyValueKey.CHALLENGES_MATCH_HISTORY_END_DATE)
        val startTime = if (!lastEndDate.isNullOrEmpty())
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastEndDate.toLong()), ZoneId.of("UTC"))
        else
            ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC")) // Technically June 16th but whatever

        val startTimeEpoch = startTime.toEpochSecond()
        val endTimeEpoch = ZonedDateTime.now().toEpochSecond()

        val summoner = Summoner.byName(LeagueConnection.summonerInfo.region, LeagueConnection.summonerInfo.displayName)
        val matchBuilder = MatchBuilder(summoner.platform)
        val matchesLst = summoner.leagueGames.withStartTime(startTimeEpoch).withEndTime(endTimeEpoch).lazy
        matchesLst.loadFully()
        val matches = matchesLst.map { matchBuilder.withId(it).match }

        if (matches.isNotEmpty()) {
            val participantList = matches.map { it to it.participants?.first { participant -> participant.summonerId == summoner.summonerId } }
            participantList.filter { !isBot(it) }
                .filter { it.second?.didWin() ?: false }
                .map { it.second?.championId!! to true }
                .distinctBy { it.first }
                .forEach { safeSet(WIN_SUMMONERS_RIFT_MAPPING, it) }

            participantList.filter { isBot(it) }
                .filter { it.second?.didWin() ?: false }
                .map { it.second?.championId!! to true }
                .distinctBy { it.first }
                .forEach { safeSet(WIN_BOT_GAMES_MAPPING, it) }

            participantList.filter { !isBot(it) }
                .filter { (it.second?.pentaKills!! > 0) }
                .map { it.second?.championId!! to (it.second?.pentaKills!! > 0) }
                .distinctBy { it.first }
                .forEach { safeSet(PENTAKILL_MAPPING, it) }

            participantList.filter { !isBot(it) }
                .filter { it.second?.didWin() ?: false }
                .map { it.second?.championId!! to (it.second?.deaths == 0) }
                .distinctBy { it.first }
                .forEach { safeSet(WIN_WITHOUT_DYING_MAPPING, it) }

            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_SUMMONERS_RIFT_MAPPING)
            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_BOT_GAMES_MAPPING)
            CacheUtil.addJsonCache(CacheType.API_JSON, ::PENTAKILL_MAPPING)
            CacheUtil.addJsonCache(CacheType.API_JSON, ::WIN_WITHOUT_DYING_MAPPING)
        }

        DatabaseImpl.setValue(GenericKeyValueKey.CHALLENGES_MATCH_HISTORY_END_DATE, endTimeEpoch)
    }

    private fun getOrPut(mapping: KMutableProperty0<HashMap<Int, Boolean>>, key: Int, default: Boolean = false): Boolean {
        return if (mapping.get().containsKey(key)) {
            mapping.get()[key]!!
        } else {
            mapping.get()[key] = default
            CacheUtil.addJsonCache(CacheType.API_JSON, mapping)

            default
        }
    }

    private fun isBot(it: Pair<LOLMatch, MatchParticipant?>): Boolean {
        return it.first.gameMode == GameModeType.CLASSIC && it.first.queue.name.contains("BOT")
    }

    private fun safeSet(mapping: MutableMap<Int, Boolean>, it: Pair<Int, Boolean>) {
        mapping[it.first] = it.second || (mapping[it.first] ?: false) == true
    }
}