package league

import com.stirante.lolclient.*
import com.stirante.lolclient.libs.com.google.gson.GsonBuilder
import com.stirante.lolclient.libs.org.apache.http.HttpException
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException
import generated.*
import league.api.LeagueCommunityDragonApi
import league.models.*
import league.models.enums.*
import league.models.enums.Role
import league.models.json.ChallengeInfo
import league.models.json.ChallengeSummary
import league.util.LeagueConnectionUtil
import tornadofx.*
import util.LogType
import util.Logging
import util.ProcessExecutor
import java.io.*
import java.net.ConnectException
import java.util.*
import kotlin.concurrent.thread

class LeagueConnection {
    var clientApi: ClientApi? = null
    var socket: ClientWebSocket? = null

    var gameId = -1L
    var gameMode = GameMode.NONE
    var role = Role.ANY

    var summonerInfo = SummonerInfo()
    var masteryChestInfo = MasteryChestInfo()
    var championSelectInfo = ChampionSelectInfo()
    var championInfo = mapOf<Int, ChampionInfo>()
    var challengeInfo = mapOf<ChallengeCategory, MutableList<ChallengeInfo>>()
    var challengeInfoSummary = ChallengeSummary()
    var eternalsValidQueues = setOf<Int>()

    private var clientApiListener: ClientConnectionListener? = null

    private var clientState = LolGameflowGameflowPhase.NONE

    private val onSummonerChangeList = ArrayList<(SummonerInfo) -> Unit>()
    private val onMasteryChestChangeList = ArrayList<(MasteryChestInfo) -> Unit>()
    private val onChampionSelectChangeList = ArrayList<(ChampionSelectInfo) -> Unit>()
    private val onClientStateChangeList = ArrayList<(LolGameflowGameflowPhase) -> Unit>()
    private val onChallengesChangedList = ArrayList<() -> Unit>()
    private val onLoggedInList = ArrayList<() -> Unit>()

    private var isConnected = false

    private val eventListenerMapping = mapOf(
        "/lol-champ-select/v1/session.*".toRegex() to { data: Any ->
            handleChampionSelectChange(data as LolChampSelectChampSelectSession)
        },
        "/lol-gameflow/v1/gameflow-phase.*".toRegex() to { data ->
            handleClientStateChange(data as LolGameflowGameflowPhase)
        },
        "/lol-login/v1/session.*".toRegex() to { data ->
            handleSignOnStateChange(data as LolLoginLoginSession)
        },
        "/lol-collections/v1/inventories/chest-eligibility.*".toRegex() to { data ->
            handleMasteryChestChange(data as LolCollectionsCollectionsChestEligibility)
        },
        "/lol-challenges/v1/my-updated-challenges/.*".toRegex() to { data ->
            handleChallengesChange((data as Array<*>).map {
                val gson = GsonBuilder().create()
                val obj = gson.toJson(it)
                gson.fromJson(obj, ChallengeInfo::class.java)
            })
        }
    )

    fun start() {
        thread {
            lateinit var console: ProcessExecutor

            while (true) {
                console = ProcessExecutor(PowershellProcessWatcher.EXECUTABLE, "--end-marker--")
                console.addFinalOutputListener { s: String ->
                    val isAlive = s.contains("LeagueClientUx.exe") && s.contains("--install-directory=")

                    if (isConnected != isAlive) {
                        isConnected = isAlive

                        Logging.log(if (isConnected) "Client Process Detected" else "Client Process Vanished", LogType.INFO)
                        if (isConnected) {
                            startClientAPI()
                        } else {
                            stopClientAPI()
                        }
                    }
                }
                console.writeCommand(PowershellProcessWatcher.COMMAND)
                console.run()

                Thread.sleep(1000)
            }
        }
    }

    private fun startClientAPI() {
        thread {
            waitForLcuServerStart()

            Logging.log("Client Server running! Starting ClientAPI...", LogType.INFO)
            setupClientAPI()

            while (summonerInfo.status == SummonerStatus.NOT_CHECKED) {
                Thread.sleep(1000)
            }
        }
    }

    private fun stopClientAPI() {
        clientApi?.stop()
        clientApi?.removeClientConnectionListener(clientApiListener)
        socket?.close()

        socket = null
        clientApi = null
        clientApiListener = null

        gameMode = GameMode.NONE
        summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
        championSelectInfo = ChampionSelectInfo()
        championInfo = mapOf()

        clientState = LolGameflowGameflowPhase.NONE
        masteryChestInfo = MasteryChestInfo()

        summonerChanged()
    }

    private fun waitForLcuServerStart() {
        var success = false
        while (!success) {
            try {
                LeagueConnectionUtil.tryLcuRequest {
                    Logging.log(it, LogType.DEBUG)
                }

                success = true
            } catch (e: Exception) {
                when(e) {
                    is HttpException, is ConnectException -> {
                        Logging.log("Client Server not yet running...", LogType.INFO, ignorableDuplicate = true)
                    }
                }
            }
        }
    }

    fun getChampionMasteryInfo(): List<ChampionInfo> {
        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        var info = championInfo.map { champion -> champion.value }
            .sortedWith(
                compareByDescending<ChampionInfo> { it.level }
                    .thenByDescending { it.tokens }
                    .thenByDescending { it.currentMasteryPoints }
                    .thenByDescending { it.ownershipStatus }
                    .thenByDescending { it.eternal != null }
                    .thenByDescending { it.name }
            )

        if (role != Role.ANY) {
            val championsByRole = LeagueCommunityDragonApi.getChampionsByRole(role)

            info = info.filter { championsByRole.contains(it.id) }
        }

        return info
    }

    fun updateClientState() {
        val newClientState = clientApi!!.executeGet("/lol-gameflow/v1/gameflow-phase", LolGameflowGameflowPhase::class.java).responseObject
        if (newClientState == clientState) return

        clientState = newClientState
        Logging.log(clientState, LogType.DEBUG)

        handleClientStateChange(clientState)

        when (clientState) {
            LolGameflowGameflowPhase.CHAMPSELECT -> {
                val championSelectSession = clientApi!!.executeGet("/lol-champ-select/v1/session", LolChampSelectChampSelectSession::class.java).responseObject

                handleChampionSelectChange(championSelectSession)
            }
            else -> return
        }
    }

    fun updateChampionMasteryInfo() {
        val champions = clientApi!!.executeGet("/lol-champions/v1/inventories/${summonerInfo.summonerID}/champions",
            Array<LolChampionsCollectionsChampion>::class.java).responseObject ?: return
        Logging.log(champions, LogType.VERBOSE)

        val championMasteryList = clientApi!!.executeGet("/lol-collections/v1/inventories/${summonerInfo.summonerID}/champion-mastery",
            Array<LolCollectionsCollectionsChampionMastery>::class.java).responseObject ?: return
        Logging.log(championMasteryList, LogType.VERBOSE)

        val eternalSummary = clientApi!!.executeGet("/lol-statstones/v2/player-summary-self", Array<LolStatstonesChampionStatstoneSummary>::class.java)
                .responseObject
                .associate { it.championId to (it.sets.first { set -> set.name == "Series 1" }.stonesOwned > 0) }
        Logging.log(eternalSummary, LogType.VERBOSE)

        val masteryPairing = champions.filter { it.id != -1 }
            .map {
                var championPoints = 0
                var currentMasteryPoints = 0
                var nextLevelMasteryPoints = 0
                var championLevel = 0
                var tokens = 0

                lateinit var championOwnershipStatus: ChampionOwnershipStatus
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
                        championLevel = championMastery.championLevel
                        currentMasteryPoints = championMastery.championPointsSinceLastLevel
                        nextLevelMasteryPoints = championMastery.championPointsUntilNextLevel
                        tokens = championMastery.tokensEarned
                    }
                }

                var eternal: LolStatstonesStatstoneSet? = null
                if (eternalSummary[it.id] == true) {
                    eternal = clientApi!!.executeGet("/lol-statstones/v2/player-statstones-self/${it.id}", Array<LolStatstonesStatstoneSet>::class.java)
                        .responseObject
                        .first { set -> set.name == "Series 1" && set.stonesOwned > 0 }
                }

                ChampionInfo(it.id, it.name, championOwnershipStatus, championPoints, currentMasteryPoints, nextLevelMasteryPoints, championLevel, tokens,
                    eternal=eternal)
            }

        championInfo = masteryPairing.associateBy({ it.id }, { it })
    }

    fun updateMasteryChestInfo(force: Boolean = false) {
        if (!force && masteryChestInfo.nextChestDate != null && Calendar.getInstance().time.before(masteryChestInfo.nextChestDate)) {
            masteryChestChanged()
            return
        }

        val chestEligibility = clientApi!!.executeGet("/lol-collections/v1/inventories/chest-eligibility",
            LolCollectionsCollectionsChestEligibility::class.java).responseObject ?: return

        handleMasteryChestChange(chestEligibility)
    }

    fun updateChallengesInfo() {
        val challenges = clientApi!!.executeGet("/lol-challenges/v1/challenges/local-player", Array<ChallengeInfo>::class.java).responseObject

        val sections = challenges.groupBy { it.category!! }
            .map { entry ->
                Pair(entry.key, entry.value.sortedWith(
                    compareBy<ChallengeInfo> { it.isComplete }
                        .thenByDescending { it.currentLevel }
                        .thenByDescending { it.hasRewardTitle }
                        .thenBy { !it.rewardObtained }
                        .thenByDescending { it.nextLevelPoints }
                        .thenByDescending { it.percentage }
                ).toMutableList())
            }
            .toMap()

        sections.values.forEach { value -> value.forEach { it.init() } }

        challengeInfo = sections
        challengeInfoSummary = clientApi!!.executeGet("/lol-challenges/v1/summary-player-data/local-player", ChallengeSummary::class.java).responseObject
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

    fun onClientStateChange(callable: (LolGameflowGameflowPhase) -> Unit) {
        onClientStateChangeList.add(callable)
    }

    fun onChallengesChange(callable: () -> Unit) {
        onChallengesChangedList.add(callable)
    }

    fun onLoggedIn(callable: () -> Unit) {
        onLoggedInList.add(callable)
    }

    private fun setupClientAPI() {
        clientApi = ClientApi()

        clientApiListener = object : ClientConnectionListener {
            override fun onClientConnected() {
                var success = handleClientConnection()
                if (!success) {
                    while (summonerInfo.status == SummonerStatus.LOGGED_IN_UNAUTHORIZED) {
                        Thread.sleep(100)
                        success = handleClientConnection()
                    }

                    if (!success) return
                }

                updateChampionMasteryInfo()

                socket = clientApi?.openWebSocket()
                socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                    override fun onEvent(event: ClientWebSocket.Event?) {
                        if (event == null || event.uri == null || event.data == null) return

                        val mappedRegex = eventListenerMapping.keys.firstOrNull { event.uri.matches(it) }
                        if (mappedRegex == null) {
                            Logging.log(event.data, LogType.VERBOSE, "ClientAPI WebSocket: " + event.uri + " - " + event.eventType)
                            return
                        }

                        eventListenerMapping[mappedRegex]?.invoke(event.data)
                    }

                    override fun onClose(code: Int, reason: String?) {
                        println("ClientAPI WebSocket: Closed - $code")

                        summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
                    }
                })

                loggedIn()
            }

            override fun onClientDisconnected() {
                summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)

                socket?.close()
            }
        }

        clientApi?.addClientConnectionListener(clientApiListener)
    }

    private fun handleChallengesChange(challengeInfoList: List<ChallengeInfo>) {
        challengeInfoList.forEach {
            val index = challengeInfo[it.category]!!.indexOfFirst { old -> old.id == it.id }
            it.init()

            challengeInfo[it.category]!![index] = it
        }

        challengeInfoSummary = clientApi!!.executeGet("/lol-challenges/v1/summary-player-data/local-player", ChallengeSummary::class.java).responseObject
        challengesChanged()
    }

    private fun handleMasteryChestChange(chestEligibility: LolCollectionsCollectionsChestEligibility) {
        Logging.log(chestEligibility, LogType.DEBUG)

        val nextChestDate = Date(chestEligibility.nextChestRechargeTime)
        val chestCount = chestEligibility.earnableChests

        masteryChestInfo = MasteryChestInfo(nextChestDate, chestCount)
        masteryChestChanged()
    }

    private fun handleSignOnStateChange(loginSession: LolLoginLoginSession) {
        Logging.log(loginSession, LogType.DEBUG)

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
        Logging.log(gameFlowPhase, LogType.DEBUG)

        gameMode = when (gameFlowPhase) {
            LolGameflowGameflowPhase.CHAMPSELECT,
            LolGameflowGameflowPhase.INPROGRESS -> {
                val gameFlow = clientApi!!.executeGet("/lol-gameflow/v1/session", LolGameflowGameflowSession::class.java).responseObject ?: return
                Logging.log(gameFlow, LogType.DEBUG)

                gameId = gameFlow.gameData.gameId
                GameMode.fromGameMode(gameFlow.gameData.queue.gameMode, gameFlow.gameData.queue.id)
            }
            else -> {
                GameMode.NONE
            }
        }

        if (clientState == LolGameflowGameflowPhase.ENDOFGAME) {
            updateChampionMasteryInfo()
        }

        clientState = gameFlowPhase
        clientStateChanged()
    }

    private fun handleChampionSelectChange(champSelectSession: LolChampSelectChampSelectSession) {
        Logging.log(champSelectSession, LogType.DEBUG)

        if (gameMode == GameMode.NONE) return
        if (champSelectSession.myTeam.isEmpty()) return

        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        val selectedChamp = champSelectSession.myTeam.find { it.summonerId == summonerInfo.summonerID }!!

        val benchedChampions = champSelectSession.benchChampionIds.map { championInfo[it]!! }
        val teamChampions = champSelectSession.myTeam.sortedBy { it.cellId }
            .map {
                if (championInfo.contains(it.championId)) {
                    championInfo[it.championId]
                } else {
                    null
                }
            }

        teamChampions.filterNotNull().forEach {
            it.isSummonerSelectedChamp = it.id == selectedChamp.championId
        }

        val assignedRole = Role.fromString(selectedChamp.assignedPosition)

        championSelectInfo = ChampionSelectInfo(teamChampions, benchedChampions, assignedRole)
        championSelectChanged()

        role = championSelectInfo.assignedRole
    }

    private fun handleClientConnection(): Boolean {
        var str = "ClientConnection: isConnected=${clientApi?.isConnected}"

        if (!clientApi!!.isConnected) {
            Logging.log("Login - " + SummonerStatus.NOT_LOGGED_IN, LogType.INFO, ignorableDuplicate = true)
            return false
        }

        try {
            str += " - isAuthorized=${clientApi?.isAuthorized}"

            if (!clientApi!!.isAuthorized) {
                Logging.log("Login - " + SummonerStatus.LOGGED_IN_UNAUTHORIZED, LogType.INFO, ignorableDuplicate = true)

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

        Logging.log("Login - " + SummonerStatus.LOGGED_IN_AUTHORIZED, LogType.INFO, ignorableDuplicate = true)

        val summoner = clientApi!!.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner::class.java).responseObject
        Logging.log(summoner, LogType.DEBUG)

        summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, summoner.accountId, summoner.summonerId, summoner.displayName, summoner.internalName,
            summoner.percentCompleteForNextLevel, summoner.summonerLevel, summoner.xpUntilNextLevel)
        summonerChanged()

        getEternalsQueueIds()

        return true
    }

    private fun getEternalsQueueIds() {
        val queues = clientApi!!.executeGet("/lol-statstones/v1/statstones-enabled-queue-ids", Array<Int>::class.java).responseObject
        eternalsValidQueues = queues.toSet()
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

    private fun clientStateChanged() {
        onClientStateChangeList.forEach { it(clientState) }
    }

    private fun challengesChanged() {
        onChallengesChangedList.forEach { it() }
    }

    private fun loggedIn() {
        onLoggedInList.forEach { it() }
    }
}
