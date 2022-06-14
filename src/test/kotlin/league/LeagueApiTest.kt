package league

import league.api.LeagueApi
import league.models.SummonerInfo
import kotlin.test.Test

internal class LeagueApiTest {
    @Test
    fun checkWinsSummonersRiftApi() {
        LeagueConnection.summonerInfo = SummonerInfo(displayName = "")

        // val champion = 55 // Katarina

        assert(LeagueApi.WIN_SUMMONERS_RIFT_MAPPING.size == 0)
        // assert(LeagueApi.getChampionWinInSummonersRift(champion))
        // assert(LeagueApi.WIN_SUMMONERS_RIFT_MAPPING.size > 0)
    }

    @Test
    fun checkWinWithoutDyingApi() {
        LeagueConnection.summonerInfo = SummonerInfo(displayName = "")

        // val champion = 236 // Lucian

        assert(LeagueApi.WIN_WITHOUT_DYING_MAPPING.size == 0)
        // assert(LeagueApi.getChampionWonWithoutDying(champion))
        // assert(LeagueApi.WIN_WITHOUT_DYING_MAPPING.size > 0)
    }

    @Test
    fun checkWinBotGamesApi() {
        LeagueConnection.summonerInfo = SummonerInfo(displayName = "")

        // val champion = 23 // Tryndamere

        assert(LeagueApi.WIN_BOT_GAMES_MAPPING.size == 0)
        // assert(LeagueApi.getChampionWinInBotGames(champion))
        // assert(LeagueApi.WIN_BOT_GAMES_MAPPING.size > 0)
    }

    @Test
    fun checkPentakillsApi() {
        LeagueConnection.summonerInfo = SummonerInfo(displayName = "")

        // val champion = 236 // Lucian

        assert(LeagueApi.PENTAKILL_MAPPING.size == 0)
        // assert(LeagueApi.getChampionGotPentakill(champion))
        // assert(LeagueApi.PENTAKILL_MAPPING.size > 0)
    }
}