package league

import com.stirante.lolclient.*
import com.stirante.lolclient.libs.com.google.gson.internal.LinkedTreeMap
import com.stirante.lolclient.libs.org.apache.http.HttpException
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException
import db.DatabaseImpl
import generated.*
import league.api.CacheUtil
import league.api.LeagueApi
import league.api.LeagueCommunityDragonApi
import league.models.*
import league.models.enums.*
import league.models.enums.Role
import league.models.json.*
import league.models.league.LolChampSelectChampSelectSessionImpl
import league.models.league.LolChampionsCollectionsChampionImpl
import league.util.LeagueConnectionUtil
import org.apache.commons.lang3.reflect.FieldUtils
import tornadofx.*
import util.LogType
import util.Logging
import util.ProcessExecutor
import util.Settings
import util.constants.GenericConstants.GSON
import util.constants.GenericConstants.GSON_PRETTY
import java.io.*
import java.net.ConnectException
import java.util.*
import kotlin.concurrent.thread

class LeagueConnection {
    var clientApi: ClientApi? = null
    var socket: ClientWebSocket? = null

    var gameMode = GameMode.NONE
    var role = Role.ANY
    val isSmurf get() = summonerInfo.uniqueId == Settings.INSTANCE.smurfId

    var championSelectInfo = ChampionSelectInfo()
    var championInfo = mapOf<Int, ChampionInfo>()
    var challengeInfo = mapOf<ChallengeCategory, MutableList<ChallengeInfo>>()
    var challengesUpdatedInfo = mutableListOf<Pair<ChallengeInfo, ChallengeInfo>>()
    var challengeInfoSummary = ChallengeSummary()
    var currentChampion = championSelectInfo.teamChampions.firstOrNull { it?.isSummonerSelectedChamp == true }

    private var clientApiListener: ClientConnectionListener? = null

    private var clientState = LolGameflowGameflowPhase.NONE
    private var gameId = -1L
    private val isMain get() = summonerInfo.uniqueId == Settings.INSTANCE.mainId

    private var masteryChestInfo = MasteryChestInfo()
    private var eternalsValidQueues = setOf<Int>()

    private val onSummonerChangeList = ArrayList<(SummonerInfo) -> Unit>()
    private val onMasteryChestChangeList = ArrayList<(MasteryChestInfo) -> Unit>()
    private val onChampionSelectChangeList = ArrayList<(ChampionSelectInfo) -> Unit>()
    private val onClientStateChangeList = ArrayList<(LolGameflowGameflowPhase) -> Unit>()
    private val onChallengesChangedList = ArrayList<() -> Unit>()
    private val onLoggedInList = ArrayList<() -> Unit>()
    private val onLcuEventList = ArrayList<(ClientWebSocket.Event) -> Unit>()

    private var isConnected = false

    private val eventListenerMapping = mapOf(
        "/lol-champ-select/v1/session.*".toRegex() to { event: ClientWebSocket.Event ->
            val dataJson = FieldUtils.readField(event, "dataJson", true) as com.stirante.lolclient.libs.com.google.gson.JsonObject
            val data = GSON.fromJson(dataJson, LolChampSelectChampSelectSessionImpl::class.java)

            handleChampionSelectChange(data)
        },
        "/lol-gameflow/v1/gameflow-phase.*".toRegex() to { event ->
            handleClientStateChange(event.data as LolGameflowGameflowPhase)
        },
        "/lol-login/v1/session.*".toRegex() to { event ->
            handleSignOnStateChange(event.data as LolLoginLoginSession)
        },
        "/lol-collections/v1/inventories/chest-eligibility.*".toRegex() to { event ->
            handleMasteryChestChange(event.data as LolCollectionsCollectionsChestEligibility)
        },
        "/lol-loot/v1/loot-grants.*".toRegex() to {
            runLootCleanup()
        },
        "/lol-challenges/v1/my-updated-challenges/.*".toRegex() to { event ->
            handleChallengesChange((event.data as Array<*>).map {
                val obj = GSON.toJson(it)
                GSON.fromJson(obj, ChallengeInfo::class.java)
            })
        },
        "/lol-event-shop/v1/info".toRegex() to { event ->
            val dataJson = FieldUtils.readField(event, "dataJson", true) as com.stirante.lolclient.libs.com.google.gson.JsonObject
            val data = GSON.fromJson(dataJson, LolEventShopInfo::class.java)

            handleEventShop(data)
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

    private fun updateClientState() {
        val newClientState = clientApi!!.executeGet("/lol-gameflow/v1/gameflow-phase", LolGameflowGameflowPhase::class.java).responseObject
        if (newClientState == clientState) return

        clientState = newClientState
        Logging.log(clientState, LogType.DEBUG)

        handleClientStateChange(clientState)

        when (clientState) {
            LolGameflowGameflowPhase.CHAMPSELECT -> {
                val championSelectSession = clientApi!!.executeGet("/lol-champ-select/v1/session", LolChampSelectChampSelectSessionImpl::class.java).responseObject
                handleChampionSelectChange(championSelectSession)
            }
            else -> return
        }
    }

    private fun getRecipes(lootId: String): Array<LolLootRecipeWithMilestones> {
        return clientApi!!.executeGet("/lol-loot/v1/recipes/initial-item/$lootId", Array<LolLootRecipeWithMilestones>::class.java)
            .responseObject!!
    }

    private fun craftLoot(recipe: LolLootRecipeWithMilestones?): Boolean {
        if (recipe == null) return false

        val path = "/lol-loot/v1/recipes/${recipe.recipeName}/craft"
        val lootIds = recipe.slots.flatMap { it.lootIds }
        val postRequest = clientApi!!.executePost(path, lootIds, LolLootPlayerLootUpdate::class.java)
        val response = postRequest.responseObject

        return if (postRequest.isOk && (response.added.isNotEmpty() || response.removed.isNotEmpty() || response.removed.isNotEmpty())) {
            val localizedName = listOf(
                { LeagueCommunityDragonApi.getLootEntity("loot_name_" + recipe.recipeName.lowercase().replace("_open", "")) },
                { recipe.description },
                { championInfo[lootIds.first { it.contains("CHAMPION_RENTAL") }.split('_').last().toIntOrNull() ?: 1]?.name }
            ).firstOrNull { !it().isNullOrEmpty() } ?: { "" }

            Logging.log("Crafted '${localizedName()} (${recipe.recipeName})' ($path) with params [${lootIds.joinToString(", ")}]", LogType.INFO)
            true
        } else {
            Logging.log("Failed Craft: '${recipe.recipeName}' ($path) with params [${lootIds.joinToString(", ")}]", LogType.INFO)
            false
        }
    }

    @Suppress("SameParameterValue", "unused")
    private fun disenchantTokenItem(loot: Array<LolLootPlayerLoot>, primaryElement: String, element: String): Boolean {
        val tokens = loot.firstOrNull { it.localizedRecipeSubtitle.contains(primaryElement) } ?: return false

        val recipes = getRecipes(tokens.lootId)

        val recipe = recipes.first { it.description.contains(element) }
        val numberOfTimes = tokens.count / recipe.slots.first { it.lootIds.contains(tokens.lootId) }.quantity

        val toRun = Collections.nCopies(numberOfTimes, recipe)
        val mapped = toRun.map { craftLoot(it) }

        return mapped.any()
    }

    private fun craftLoot(lootElement: LolLootPlayerLoot?, recipeName: String?) {
        if (lootElement == null) return

        val recipes = getRecipes(lootElement.lootId).toList()
        val recipe = if (!recipeName.isNullOrEmpty()) {
            recipes.firstOrNull { it.recipeName == recipeName }
        } else {
            recipes.firstOrNull()
        }

        craftLoot(recipe)
    }

    @Suppress("SameParameterValue")
    private fun craftLoot(loot: List<LolLootPlayerLoot>, recipeName: String? = null, filter: (LolLootPlayerLoot) -> Boolean): Boolean {
        return loot.filter { filter(it) }.map {
            craftLoot(it, recipeName = recipeName)
            return@map true
        }.any()
    }

    @Suppress("SameParameterValue")
    private fun craftLoot(loot: Array<LolLootPlayerLoot>, lootId: String, count: Int): Boolean {
        return loot.filter { it.lootId == lootId }
            .filter { it.count >= count }
            .map {
                craftLoot(it, recipeName = null)
                true
            }
            .any()
    }

    private fun disenchantByText(loot: Array<LolLootPlayerLoot>, element: String): Boolean {
        val specificLoot = loot.firstOrNull {
            val localizedName = if (it.localizedName.isNullOrEmpty()) {
                LeagueCommunityDragonApi.getLootEntity("loot_name_" + it.lootId.lowercase()) ?: ""
            } else {
                it.localizedName
            }

            localizedName.contains(element)
        } ?: return false
        val recipes = getRecipes(specificLoot.lootId).firstOrNull() ?: return false

        val toRun = Collections.nCopies(specificLoot.count, recipes)
        val mapped = toRun.map { craftLoot(it) }

        return mapped.any()
    }

    private fun upgradeChampionShard(loot: List<LolLootPlayerLoot>, blueEssence: LolLootPlayerLoot, filter: (LolLootPlayerLoot) -> Boolean): Boolean {
        return loot.filter { filter(it) }.map {
            val recipes = getRecipes(it.lootId)
            val upgradeRecipe = recipes.first { recipe -> recipe.recipeName.contains("upgrade") }

            val cost = upgradeRecipe.slots.first { slot -> slot.lootIds.contains(blueEssence.lootId) }.quantity

            if (blueEssence.count > cost) {
                craftLoot(upgradeRecipe)
                blueEssence.count -= cost
                return@map true
            }

            false
        }.any { it }
    }

    private fun upgradeMasteryTokens(loot: Array<LolLootPlayerLoot>, onlyShard: Boolean = false): Boolean {
        val shards = loot.filter { it.type == "CHAMPION_RENTAL" }
        val tokens = loot.filter { it.type == "CHAMPION_TOKEN" }

        return mapOf(2 to 5, 3 to 6).flatMap { (k, v) ->
            tokens.filter { it.count == k && championInfo[it.refId.toInt()]?.level == v }
                .map {
                    val txt = if (shards.any { shard -> shard.storeItemId == it.refId.toInt() && shard.count >= 1 }) "shard" else "essence"
                    if (txt == "essence" && onlyShard) return@map null

                    getRecipes(it.lootId).first { recipe -> recipe.recipeName.contains(txt) }
                }
            }
            .filterNotNull()
            .map { craftLoot(it) }
            .any()
    }

    private fun upgradeOrDisenchantEternals(loot: Array<LolLootPlayerLoot>): Boolean {
        return loot.filter { it.type == "STATSTONE_SHARD" }.map {
            val recipes = getRecipes(it.lootId)

            val recipe = if (championInfo.values.first { championInfo -> championInfo.name == it.localizedName.split(' ').first() }.eternal == null) {
                recipes.first { recipe -> recipe.recipeName.lowercase().contains("upgrade") }
            } else {
                recipes.first { recipe -> recipe.recipeName.lowercase().contains("disenchant") }
            }

            craftLoot(recipe)
            true
        }.any { it }
    }

    fun runLootCleanup() {
        val loot = clientApi!!.executeGet("/lol-loot/v1/player-loot", Array<LolLootPlayerLoot>::class.java).responseObject ?: return
        Logging.log(loot, LogType.VERBOSE)

        val ignoredIds = listOf("CURRENCY_champion", "CHEST_champion_mastery", "MATERIAL_key_fragment", "CURRENCY_champion", "CURRENCY_RP")
        val ignoredCategories = listOf("SKIN", "WARDSKIN")
        @Suppress("UNUSED_VARIABLE") val singleLoot = loot.filter { ignoredIds.all { id -> it.lootId != id } }
            .filter { ignoredCategories.all { id -> it.displayCategories != id } }
            .filter { !it.localizedName.contains(" Token") }
            .filter { it.type != "CHAMPION_RENTAL" }
            .filter { it.type != "CHAMPION_TOKEN" }

        val blueEssence = loot.first { it.lootId == "CURRENCY_champion" } // CURRENCY_RP

        val shards = loot.filter { it.type == "CHAMPION_RENTAL" }
        val functions = mutableListOf(
            { craftLoot(loot, "MATERIAL_key_fragment", 3) },
            { disenchantByText(loot, "Little Legends") },
            { disenchantByText(loot, "Mystery Emote") },
            { upgradeOrDisenchantEternals(loot) }
        )

        if (isMain) {
            functions.addAll(mutableListOf(
                { upgradeMasteryTokens(loot) },

                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 7 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 6 && it.count == 2 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { it.count == 3 } },
                { upgradeChampionShard(shards, blueEssence) { ChampionOwnershipStatus.UNOWNED_SET.contains(championInfo[it.storeItemId]?.ownershipStatus) } },

                // { disenchantTokenItem(loot, "Tokens expire", "Mystery Emote") }, // Orb
            ))
        }

        if (isSmurf) {
            functions.addAll(mutableListOf(
                { upgradeMasteryTokens(loot) },

                // { disenchantByText(loot, "Champion Capsule") },
                // { disenchantByText(loot, "Random Champion Shard") },

                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 7 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 6 && it.count == 2 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { it.count == 3 &&
                        !ChampionOwnershipStatus.UNOWNED_SET.contains(championInfo[it.storeItemId]?.ownershipStatus) } },

                { upgradeChampionShard(shards, blueEssence) { ChampionOwnershipStatus.UNOWNED_SET.contains(championInfo[it.storeItemId]?.ownershipStatus) } },

                // { disenchantTokenItem(loot, "Tokens expire", "Random Champion Shard") },
                // { disenchantTokenItem(loot, "Unlock new and classic content exclusively for Mythic Essence", "150 Blue Essence") },
            ))
        }

        if (functions.any { it.invoke() }) {
            runLootCleanup()
        }
    }

    fun updateChampionMasteryInfo() {
        val champions = clientApi?.executeGet("/lol-champions/v1/inventories/${summonerInfo.summonerId}/champions",
            Array<LolChampionsCollectionsChampionImpl>::class.java)?.responseObject ?: return
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
                if (it.ownership?.owned == false) {
                    championOwnershipStatus = if (it.ownership?.rental?.rented == true) {
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
                    eternal=eternal, roles=it.roles.map { role -> ChampionRole.fromString(role) }.toSet())
            }

        championInfo = masteryPairing.associateBy({ it.id }, { it })
    }

    private fun checkTftBattlepassRewardsAvailable() {
        val battlepass = clientApi!!.executeGet("/lol-tft/v2/tft/battlepass", LolMissionsTftPaidBattlepass::class.java).responseObject
        val missions = battlepass.milestones.filter { !it.isPaid && it.state == "REWARDABLE" }
        val completedMissions = missions.map {
            //val missionPut = clientApi?.executePut("/lol-missions/v1/player/" + it.missionId, null)?.responseObject

            //println(missionPut)
            false
        }

        if (completedMissions.any()) {
            runLootCleanup()
        }
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

    fun executeCommand(path: String) {
        val obj = clientApi?.executeGet(path, Any::class.java)?.responseObject

        if (obj != null) {
            Logging.log(GSON_PRETTY.toJson(obj), LogType.INFO)
        }
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

    fun onLcuEvent(callable: (ClientWebSocket.Event) -> Unit) {
        onLcuEventList.add(callable)
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
                checkTftBattlepassRewardsAvailable()

                socket = clientApi?.openWebSocket()
                socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                    override fun onEvent(event: ClientWebSocket.Event?) {
                        if (event == null || event.uri == null) return

                        val mappedRegex = eventListenerMapping.keys.firstOrNull { event.uri.matches(it) }
                        val bad = listOf("/lol-hovercard", "/lol-chat", "/lol-game-client-chat", "/riot-messaging-service", "/lol-patch/v1/products/league_of_legends",
                            "/patcher/v1/products/league_of_legends", "/lol-settings", "/data-store", "/lol-premade-voice", "/lol-matchmaking/v1/search",
                            "/lol-loadouts/v1/loadouts/scope/champion", "/lol-suggested-players/v1/suggested-player", "/lol-matchmaking/v1/ready-check",
                            "/lol-clash", "/lol-champ-select/v1/sfx-notifications", "/lol-regalia/v2/summoners/", "/lol-league-session/v1/league-session-token")
                        if (mappedRegex == null && !bad.any { event.uri.contains(it) }) {
                            Logging.log("", LogType.VERBOSE, "ClientAPI WebSocket: " + event.uri + " - " + event.eventType)
                            clientEventChanged(event)
                            return
                        }

                        eventListenerMapping[mappedRegex]?.invoke(event)
                    }

                    override fun onClose(code: Int, reason: String?) {
                        println("ClientAPI WebSocket: Closed - $code")

                        summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)
                    }
                })

                updateChallengesInfo()
                loggedIn()

                CacheUtil.preloadChallengesCache(challengeInfo)
            }

            override fun onClientDisconnected() {
                summonerInfo = SummonerInfo(SummonerStatus.NOT_LOGGED_IN)

                socket?.close()
            }
        }

        clientApi?.addClientConnectionListener(clientApiListener)
    }

    private fun checkEventShopRewardsAvailable() {
        val data = clientApi?.executeGet("/lol-event-shop/v1/unclaimed-rewards", LolEventShopUnclaimedRewards::class.java)?.responseObject
            ?: LolEventShopUnclaimedRewards(0, 0)

        if (data.rewardsCount > 0) {
            clientApi?.executePost("/lol-event-shop/v1/claim-select-all")?.responseObject
        }
    }

    private fun getEventShop(): Array<LolEventShopCategoriesOffer> {
        return clientApi?.executeGet("/lol-event-shop/v1/categories-offers", Array<LolEventShopCategoriesOffer>::class.java)?.responseObject ?: arrayOf()
        // /
    }

    private fun handleEventShop(event: LolEventShopInfo) {
        val shop = getEventShop()

        if (isSmurf) {
            val orb = shop.first { it.localizedTitle.lowercase().contains(" orb") }

            if (event.currentTokenBalance > orb.price) {
                return

                val newEvent = LolEventShopInfo(event.currentTokenBalance - orb.price)
                handleEventShop(newEvent)
            }
        } else if (isMain) {
            print("Main")
        }
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
            updateLootTab()
            LeagueApi.updateMatchHistory()
            updateChampionMasteryInfo()
            checkTftBattlepassRewardsAvailable()
            checkEventShopRewardsAvailable()
        }

        runLootCleanup()
        if (!isSmurf) {
            role = championSelectInfo.assignedRole
        }

        clientState = gameFlowPhase
        clientStateChanged()
    }

    private fun updateLootTab() {
        clientApi?.executePost("/lol-loot/v1/refresh")?.responseObject
    }

    private fun handleChampionSelectChange(champSelectSession: LolChampSelectChampSelectSessionImpl) {
        Logging.log(champSelectSession, LogType.DEBUG)

        if (gameMode == GameMode.NONE) return
        if (champSelectSession.myTeam.isEmpty()) return

        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }

        val selectedChamp = champSelectSession.myTeam.find { it.summonerId == summonerInfo.summonerId }!!

        val benchedChampions = champSelectSession.benchChampions?.map { championInfo[it.championId]!! }
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

        championSelectInfo = ChampionSelectInfo(teamChampions, benchedChampions ?: listOf(), assignedRole)
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

        LeagueApi.updateMatchHistory()
        updateMasteryChestInfo()
        updateChampionMasteryInfo()
        updateClientState()
        getEternalsQueueIds()

        return true
    }

    private fun getClientVersion() {
        val versionInfo = clientApi!!.executeGet("/system/v1/builds", BuildInfo::class.java).responseObject
        LeagueCommunityDragonApi.VERSION = versionInfo.version.split(".").subList(0, 2).joinToString(".")
        CacheUtil.CACHE_MAPPING
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

    private fun clientEventChanged(event: ClientWebSocket.Event?) {
        if (event == null || event.uri.isNullOrEmpty()) return

        onLcuEventList.forEach { it(event) }
    }

    companion object {
        var summonerInfo = SummonerInfo()
    }
}
