package league

import db.DatabaseImpl
import league.models.SummonerInfo
import league.models.enums.ChallengeMappingEnum
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LeagueApiTest {
    @Test
    fun checkWinsSummonersRiftApi() {
        LeagueConnection.summonerInfo = SummonerInfo(summonerId = 2549404233031175, accountID = 0)

        val champion = 55 // Katarina

        assertEquals(true, DatabaseImpl.getChallengeComplete(ChallengeMappingEnum.WIN_SUMMONERS_RIFT, champion))
    }

    @Test
    fun checkWinWithoutDyingApi() {
        LeagueConnection.summonerInfo = SummonerInfo(summonerId = 2549404233031175, accountID = 0)

        val champion = 236 // Lucian

        assertEquals(true, DatabaseImpl.getChallengeComplete(ChallengeMappingEnum.WIN_NO_DEATHS_SUMMONERS_RIFT, champion))
    }

    @Test
    fun checkWinBotGamesApi() {
        LeagueConnection.summonerInfo = SummonerInfo(summonerId = 2549404233031175, accountID = 0)

        val champion = 23 // Tryndamere

        assertEquals(true, DatabaseImpl.getChallengeComplete(ChallengeMappingEnum.WIN_BOTS_GAME, champion))
    }

    @Test
    fun checkPentakillsApi() {
        LeagueConnection.summonerInfo = SummonerInfo(summonerId = 2549404233031175, accountID = 0)

        val champion = 236 // Lucian

        assertEquals(true, DatabaseImpl.getChallengeComplete(ChallengeMappingEnum.PENTA_IN_SUMMONERS_RIFT, champion))
    }
}