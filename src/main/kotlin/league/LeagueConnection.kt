package league

import DEBUG_LOG_ENDPOINTS
import com.stirante.lolclient.ClientApi
import com.stirante.lolclient.ClientConnectionListener
import com.stirante.lolclient.ClientWebSocket
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException
import generated.*
import tornadofx.*
import java.util.*


enum class ChampionOwnershipStatus {
    NOT_OWNED,
    FREE_TO_PLAY,
    RENTAL,
    BOX_NOT_ATTAINED,
    BOX_ATTAINED
}

enum class SummonerStatus {
    NOT_LOGGED_IN,
    LOGGED_IN_UNAUTHORIZED,
    LOGGED_IN_AUTHORIZED
}

enum class GameMode {
    NONE,
    SUMMONERS_RIFT,
    RANKED_SOLO,
    RANKED_FLEX,
    CLASH,
    ARAM,
    HEXAKILL,
    ONE_FOR_ALL,
    URF,
    TUTORIAL,
    BOT,
    PRACTICE_TOOL,
    UNKNOWN
}

data class SummonerInfo(val status: SummonerStatus = SummonerStatus.NOT_LOGGED_IN,
                        val accountID: Long = 0,
                        val summonerID: Long = 0,
                        val displayName: String = "",
                        val internalName: String = "",
                        val percentCompleteForNextLevel: Int = 0,
                        val summonerLevel: Int = 0,
                        val xpUntilNextLevel: Long = 0)

data class MasteryChestInfo(var nextChestDate: Date? = null, var chestCount: Int = 0)

data class ChampionInfo(val id: Int, val name: String, val ownershipStatus: ChampionOwnershipStatus, val masteryPoints: Int,
                        var isSummonerSelectedChamp: Boolean = false)

data class ChampionSelectInfo(val gameMode: GameMode = GameMode.NONE, val teamChampions: List<ChampionInfo> = listOf(),
                              val benchedChampions: List<ChampionInfo> = listOf())


class LeagueConnection {
    val clientAPI = ClientApi()
    var socket: ClientWebSocket? = null

    var clientState = LolGameflowGameflowPhase.NONE
    var gameMode = GameMode.NONE

    var summonerInfo = SummonerInfo()
    var masteryChestInfo = MasteryChestInfo()
    var championSelectInfo = ChampionSelectInfo()
    var championInfo = mapOf<Int, ChampionInfo>()

    private val onSummonerChangeList = ArrayList<(SummonerInfo) -> Unit>()
    private val onMasteryChestChangeList = ArrayList<(MasteryChestInfo) -> Unit>()
    private val onChampionSelectChangeList = ArrayList<(ChampionSelectInfo) -> Unit>()

    fun updateClientState() {
        clientState = clientAPI.executeGet("/lol-gameflow/v1/gameflow-phase", LolGameflowGameflowPhase::class.java).responseObject

        handleClientStateChange(clientState)

        when (clientState) {
            LolGameflowGameflowPhase.CHAMPSELECT -> {
                val championSelectSession = clientAPI.executeGet("/lol-champ-select/v1/session", LolChampSelectChampSelectSession::class.java).responseObject

                handleChampionSelect(championSelectSession)
            }
            else -> return
        }
    }

    fun updateChampionMasteryInfo() {
        val champions = clientAPI.executeGet("/lol-champions/v1/inventories/${summonerInfo.summonerID}/champions",
            Array<LolChampionsCollectionsChampion>::class.java).responseObject

        val championMasteryList = clientAPI.executeGet("/lol-collections/v1/inventories/${summonerInfo.summonerID}/champion-mastery",
            Array<LolCollectionsCollectionsChampionMastery>::class.java).responseObject

        val masteryPairing = champions.map {
            lateinit var championOwnershipStatus: ChampionOwnershipStatus
            var championPoints = 0

            if (!it.ownership.owned) {
                championOwnershipStatus = if (it.ownership.rental.rented) {
                    ChampionOwnershipStatus.RENTAL
                } else if (it.freeToPlay) {
                    ChampionOwnershipStatus.FREE_TO_PLAY
                } else {
                    ChampionOwnershipStatus.NOT_OWNED
                }
            } else {
                val championMastery = championMasteryList.firstOrNull { championMastery -> championMastery.championId == it.id }

                if (championMastery == null) {
                    championOwnershipStatus = ChampionOwnershipStatus.BOX_NOT_ATTAINED
                } else {
                    championOwnershipStatus = if (championMastery.chestGranted) ChampionOwnershipStatus.BOX_ATTAINED else ChampionOwnershipStatus.BOX_NOT_ATTAINED

                    championPoints = championMastery.championPoints
                }
            }

            ChampionInfo(it.id, it.name, championOwnershipStatus, championPoints)
        }

        championInfo = masteryPairing.associateBy({ it.id }, { it })
    }

    fun updateMasteryChestInfo(force: Boolean = false) {
        if (!force && masteryChestInfo.nextChestDate != null && Calendar.getInstance().time.before(masteryChestInfo.nextChestDate)) {
            masteryChestChanged()
            return
        }

        val chestEligibility = clientAPI.executeGet("/lol-collections/v1/inventories/chest-eligibility",
            LolCollectionsCollectionsChestEligibility::class.java).responseObject

        val nextChestDate = Date(chestEligibility.nextChestRechargeTime)
        val chestCount = chestEligibility.earnableChests

        masteryChestInfo = MasteryChestInfo(nextChestDate, chestCount)
        masteryChestChanged()
    }

    fun onSummonerChange(callable: (SummonerInfo) -> Unit) {
        onSummonerChangeList.add(callable)
    }

    fun onMasteryChestChange(callable: (MasteryChestInfo) -> Unit) {
        onMasteryChestChangeList.add(callable)
    }

    fun onChampionSelectChange(callable: (ChampionSelectInfo) -> Unit) {
        onChampionSelectChangeList.add(callable)
    }

    fun start() {
        clientAPI.addClientConnectionListener(object : ClientConnectionListener {
            override fun onClientConnected() {
                if (!handleClientConnection()) return

                updateChampionMasteryInfo()

                socket = clientAPI.openWebSocket()
                socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                    override fun onEvent(event: ClientWebSocket.Event?) {
                        if (event == null || event.uri == null || event.data == null) return

                        when (event.uri) {
                            "/lol-champ-select/v1/session" -> handleChampionSelect(event.data as LolChampSelectChampSelectSession)
                            "/lol-gameflow/v1/gameflow-phase" -> handleClientStateChange(event.data as LolGameflowGameflowPhase)
                            "/lol-login/v1/session" -> handleSignOnState(event.data as LolLoginLoginSession)
                            else -> {
                                if (!DEBUG_LOG_ENDPOINTS) return

                                println("ClientAPI WebSocket:")
                                println(event.eventType + " - " + event.uri)
                                println(event.data)
                            }
                        }
                    }

                    override fun onClose(code: Int, reason: String?) {
                        println("ClientAPI WebSocket: Closed - $code")
                    }
                })
            }

            override fun onClientDisconnected() {
                summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)

                socket?.close()
            }
        })
    }

    private fun handleSignOnState(loginSession: LolLoginLoginSession) {
        when (loginSession.state) {
            LolLoginLoginSessionStates.LOGGING_OUT -> {
                summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
                summonerChanged()
            }
            LolLoginLoginSessionStates.SUCCEEDED -> {
                handleClientConnection()
            }
            else -> return
        }
    }

    private fun handleClientStateChange(gameFlowPhase: LolGameflowGameflowPhase) {
        when (gameFlowPhase) {
            LolGameflowGameflowPhase.CHAMPSELECT -> {
                val gameFlow = clientAPI.executeGet("/lol-gameflow/v1/session", LolGameflowGameflowSession::class.java).responseObject ?: return

                gameMode = when (gameFlow.gameData.queue.gameMode) {
                    "CLASSIC" -> GameMode.SUMMONERS_RIFT
                    "RANKED_SOLO_5x5" -> GameMode.RANKED_SOLO
                    "RANKED_FLEX_SR" -> GameMode.RANKED_FLEX
                    "CLASH" -> GameMode.CLASH
                    "ARAM" -> GameMode.ARAM
                    "HEXAKILL" -> GameMode.HEXAKILL
                    "ONEFORALL" -> GameMode.ONE_FOR_ALL
                    "URF" -> GameMode.URF
                    "TUTORIAL" -> GameMode.TUTORIAL
                    "BOT" -> GameMode.BOT
                    "PRACTICETOOL" -> GameMode.PRACTICE_TOOL
                    else -> GameMode.UNKNOWN
                }
            }
            else -> {
                gameMode = GameMode.NONE
            }
        }

        clientState = gameFlowPhase
    }

    private fun handleChampionSelect(champSelectSession: LolChampSelectChampSelectSession) {
        if (gameMode == GameMode.NONE) return
        if (champSelectSession.myTeam.isEmpty()) return

        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        val selectedChamp = champSelectSession.myTeam.find { it.summonerId == summonerInfo.summonerID }!!

        val benchedChampions = champSelectSession.benchChampionIds.map { championInfo[it]!! }
        val teamChampions = champSelectSession.myTeam.sortedBy { it.cellId }
            .map { championInfo[it.championId]!! }

        teamChampions.forEach {
            it.isSummonerSelectedChamp = it.id == selectedChamp.championId
        }

        championSelectInfo = ChampionSelectInfo(gameMode, teamChampions, benchedChampions)
        championSelectChanged()
    }

    private fun handleClientConnection(): Boolean {
        if (!clientAPI.isConnected) return false

        try {
            if (!clientAPI.isAuthorized) {
                summonerInfo = SummonerInfo(SummonerStatus.LOGGED_IN_UNAUTHORIZED)
                summonerChanged()

                return false
            }
        } catch (e: HttpHostConnectException) {
            if (e.message?.contains("Connection refused: connect") == true) {
                return false
            }

            e.printStackTrace()
            return false
        }

        val summoner = clientAPI.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner::class.java).responseObject

        summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, summoner.accountId, summoner.summonerId, summoner.displayName, summoner.internalName,
            summoner.percentCompleteForNextLevel, summoner.summonerLevel, summoner.xpUntilNextLevel)
        summonerChanged()

        return true
    }

    private fun summonerChanged() {
        onSummonerChangeList.forEach { it(summonerInfo) }
    }

    private fun masteryChestChanged() {
        onMasteryChestChangeList.forEach { it(masteryChestInfo) }
    }

    private fun championSelectChanged() {
        onChampionSelectChangeList.forEach { it(championSelectInfo) }
    }
}
