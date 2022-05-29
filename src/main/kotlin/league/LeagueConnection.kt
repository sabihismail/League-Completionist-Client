package league

import com.stirante.lolclient.*
import com.stirante.lolclient.libs.com.google.gson.internal.LinkedTreeMap
import com.stirante.lolclient.libs.org.apache.http.HttpException
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException
import db.DatabaseImpl
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
import util.constants.GenericConstants.GSON
import java.io.*
import java.net.ConnectException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class LeagueConnection {
    var clientApi: ClientApi? = null
    var socket: ClientWebSocket? = null

    var gameId = -1L
    var gameMode = GameMode.NONE
    var role = Role.ANY
    val isSmurf get() = summonerInfo.uniqueId == 2549404233031175L

    var summonerInfo = SummonerInfo()
    var masteryChestInfo = MasteryChestInfo()
    var championSelectInfo = ChampionSelectInfo()
    var championInfo = mapOf<Int, ChampionInfo>()
    var challengeInfo = mapOf<ChallengeCategory, MutableList<ChallengeInfo>>()
    var challengesUpdatedInfo = mutableListOf<Pair<ChallengeInfo, ChallengeInfo>>()
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
                val obj = GSON.toJson(it)
                GSON.fromJson(obj, ChallengeInfo::class.java)
            })
        },
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

        gameId = -1L
        gameMode = GameMode.NONE
        role = Role.ANY
        summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
        championSelectInfo = ChampionSelectInfo()
        championInfo = mapOf()

        clientState = LolGameflowGameflowPhase.NONE

        summonerInfo = SummonerInfo()
        masteryChestInfo = MasteryChestInfo()
        championSelectInfo = ChampionSelectInfo()
        championInfo = mapOf()
        challengeInfo = mapOf()
        challengesUpdatedInfo = mutableListOf()
        challengeInfoSummary = ChallengeSummary()
        eternalsValidQueues = setOf()

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

    private fun runLootCleanup() {
        if (isSmurf) return
        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        val loot = clientApi!!.executeGet("/lol-loot/v1/player-loot", Array<LolLootPlayerLoot>::class.java).responseObject ?: return
        Logging.log(loot, LogType.VERBOSE)

        val recipesToCraft = mutableListOf<LolLootRecipeWithMilestones>()

        val shards = loot.filter { it.type == "CHAMPION_RENTAL" }
        val tokens = loot.filter { it.type == "CHAMPION_TOKEN" }
        mapOf(2 to 5, 3 to 6).map { (k, v) ->
            tokens.filter { it.count == k && championInfo[it.refId.toInt()]?.level == v }
                .forEach {
                    val recipes = clientApi!!.executeGet("/lol-loot/v1/recipes/initial-item/${it.lootId}", Array<LolLootRecipeWithMilestones>::class.java)
                        .responseObject ?: return

                    if (shards.any { shard -> shard.storeItemId == it.refId.toInt() && shard.count >= 1 }) {
                        val recipeShard = recipes.first { recipe -> recipe.recipeName.contains("shard") }
                        recipesToCraft.add(recipeShard)
                        return
                    }

                    val recipeEssence = recipes.first { recipe -> recipe.recipeName.contains("essence") }
                    recipesToCraft.add(recipeEssence)
                }
        }

        val unneededShards = shards.filter { championInfo[it.storeItemId]?.level == 7 }
            .forEach {
                val recipes = clientApi!!.executeGet("/lol-loot/v1/recipes/initial-item/${it.lootId}", Array<LolLootRecipeWithMilestones>::class.java)
                    .responseObject ?: return
                Logging.log(recipes, LogType.VERBOSE)
            }

        recipesToCraft.forEach { recipe ->
            val postRequest = clientApi!!.executePost(recipe.recipeName, recipe.lootMilestoneIds, LolLootPlayerLootUpdate::class.java).responseObject
        }
    }

    fun updateChampionMasteryInfo() {
        val champions = clientApi!!.executeGet("/lol-champions/v1/inventories/${summonerInfo.summonerId}/champions",
            Array<LolChampionsCollectionsChampion>::class.java).responseObject ?: return
        Logging.log(champions, LogType.VERBOSE)

        val championMasteryList = clientApi!!.executeGet("/lol-collections/v1/inventories/${summonerInfo.summonerId}/champion-mastery",
            Array<LolCollectionsCollectionsChampionMastery>::class.java).responseObject ?: return
        Logging.log(championMasteryList, LogType.VERBOSE)

        val eternalSummary = clientApi!!.executeGet("/lol-statstones/v2/player-summary-self", Array<LolStatstonesChampionStatstoneSummary>::class.java)
                .responseObject
                .associate { it.championId to (it.sets.first { set -> set.name != "Starter Series" }.stonesOwned > 0) }
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
                        .first { set -> set.name != "Starter Series" && set.stonesOwned > 0 }
                }

                ChampionInfo(it.id, it.name, championOwnershipStatus, championPoints, currentMasteryPoints, nextLevelMasteryPoints, championLevel, tokens,
                    eternal=eternal)
            }

        championInfo = masteryPairing.associateBy({ it.id }, { it })
    }

    private fun updateMasteryChestInfo(force: Boolean = false) {
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
                runLootCleanup()

                socket = clientApi?.openWebSocket()
                socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                    override fun onEvent(event: ClientWebSocket.Event?) {
                        if (event == null || event.uri == null || event.data == null) return

                        val mappedRegex = eventListenerMapping.keys.firstOrNull { event.uri.matches(it) }
                        val bad = listOf("/lol-hovercard", "/lol-chat", "/lol-game-client-chat")
                        if (mappedRegex == null && !bad.any { event.uri.contains(it) }) {
                            Logging.log("", LogType.VERBOSE, "ClientAPI WebSocket: " + event.uri + " - " + event.eventType)
                            return
                        }

                        eventListenerMapping[mappedRegex]?.invoke(event.data)
                    }

                    override fun onClose(code: Int, reason: String?) {
                        println("ClientAPI WebSocket: Closed - $code")

                        summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
                    }
                })

                updateChallengesInfo()
                loggedIn()

                preloadChallengesCache()
            }

            override fun onClientDisconnected() {
                summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)

                socket?.close()
            }
        }

        clientApi?.addClientConnectionListener(clientApiListener)
    }

    private fun handleChallengesChange(challengeInfoList: List<ChallengeInfo>) {
        challengesUpdatedInfo.clear()

        challengeInfoList.forEach {
            val index = challengeInfo[it.category]!!.indexOfFirst { old -> old.id == it.id }
            it.init()

            challengesUpdatedInfo.add(Pair(challengeInfo[it.category]!![index], it))

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
        DatabaseImpl.setMasteryInfo(summonerInfo, masteryChestInfo)

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

        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        gameMode = when (gameFlowPhase) {
            LolGameflowGameflowPhase.CHAMPSELECT -> {
                val gameFlow = clientApi!!.executeGet("/lol-gameflow/v1/session", LolGameflowGameflowSession::class.java).responseObject ?: return
                Logging.log(gameFlow, LogType.DEBUG)

                GameMode.fromGameMode(gameFlow.gameData.queue.gameMode, gameFlow.gameData.queue.id)
            }
            LolGameflowGameflowPhase.INPROGRESS -> {
                val gameFlow = clientApi!!.executeGet("/lol-gameflow/v1/session", LolGameflowGameflowSession::class.java).responseObject ?: return
                Logging.log(gameFlow, LogType.DEBUG)

                if (championSelectInfo.teamChampions.isEmpty()) {
                    val players = gameFlow.gameData.teamOne + gameFlow.gameData.teamTwo
                    val me = players.map { it as LinkedTreeMap<*, *> }.first { it["summonerName"] as String == summonerInfo.displayName }
                    val championJson = gameFlow.gameData.playerChampionSelections.map { it as LinkedTreeMap<*, *> }.
                        first { it["summonerInternalName"] == me["summonerInternalName"] }
                    val champion = championInfo[(championJson["championId"] as Double).toInt()].apply { this!!.isSummonerSelectedChamp = true }

                    championSelectInfo = ChampionSelectInfo(listOf(champion), listOf(), Role.ANY)
                }

                gameId = gameFlow.gameData.gameId
                GameMode.fromGameMode(gameFlow.gameData.queue.gameMode, gameFlow.gameData.queue.id)
            }
            else -> {
                GameMode.NONE
            }
        }

        if (clientState == LolGameflowGameflowPhase.ENDOFGAME) {
            updateChampionMasteryInfo()
            runLootCleanup()
        }

        if (!isSmurf) {
            role = championSelectInfo.assignedRole
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

        val selectedChamp = champSelectSession.myTeam.find { it.summonerId == summonerInfo.summonerId }!!

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

                getClientVersion()
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

        getClientVersion()
        val summoner = clientApi!!.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner::class.java).responseObject
        Logging.log(summoner, LogType.DEBUG)

        summonerInfo = SummonerInfo(
            SummonerStatus.LOGGED_IN_AUTHORIZED, summoner.accountId, summoner.summonerId, summoner.displayName, summoner.internalName,
            summoner.percentCompleteForNextLevel, summoner.summonerLevel, summoner.xpUntilNextLevel)
        summonerChanged()

        updateMasteryChestInfo()
        updateChampionMasteryInfo()
        updateClientState()
        getEternalsQueueIds()

        return true
    }

    private fun preloadChallengesCache() {
        thread {
            val elements = challengeInfo.values
                .flatMap { challengeInfos -> challengeInfos.flatMap { challengeInfo -> challengeInfo.thresholds!!.keys.map { rank -> Pair(challengeInfo.id, rank) } } }
                .toList()

            val maxCount = elements.count()
            val fileWalk = Files.walk(LeagueCommunityDragonApi.getPath(CacheType.CHALLENGE)).count()
            if (fileWalk < maxCount) {
                thread {
                    Logging.log("Challenges - Starting Cache Download...", LogType.INFO)

                    val num = AtomicInteger(0)
                    elements.parallelStream()
                        .forEach {
                            LeagueCommunityDragonApi.getImagePath(CacheType.CHALLENGE, it.first.toString().lowercase(), it.second)

                            num.incrementAndGet()
                        }

                    while (num.get() != maxCount) {
                        Thread.sleep(1000)
                    }

                    Logging.log("Challenges - Finished Cache Download.", LogType.INFO)
                }
            }
        }
    }

    private fun getClientVersion() {
        val versionInfo = clientApi!!.executeGet("/system/v1/builds", BuildInfo::class.java).responseObject
        LeagueCommunityDragonApi.VERSION = versionInfo.version.split(".").subList(0, 2).joinToString(".")
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
