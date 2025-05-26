package league

import com.google.gson.JsonObject
import com.stirante.lolclient.ClientApi
import com.stirante.lolclient.ClientConnectionListener
import com.stirante.lolclient.ClientWebSocket
import com.stirante.lolclient.PowershellProcessWatcher
import db.DatabaseImpl
import generated.*
import league.api.LeagueCommunityDragonApi
import league.models.*
import league.models.enums.*
import league.models.enums.Role
import league.models.json.*
import league.models.league.LolChampSelectChampSelectSessionImpl
import league.models.league.LolChampionsCollectionsChampionImpl
import league.util.LeagueConnectionUtil
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.hc.client5.http.HttpHostConnectException
import org.apache.hc.core5.http.HttpException
import util.*
import util.KotlinExtensionUtil.containsLong
import util.constants.GenericConstants.GSON
import util.constants.GenericConstants.GSON_PRETTY
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.ConnectException
import java.util.*
import kotlin.concurrent.thread


class LeagueConnection {
    var clientApi: ClientApi? = null
    var socket: ClientWebSocket? = null

    var gameMode = GameMode.NONE
    var role = Role.ANY
    var isDisenchantmentUser = false
    var isDeveloper = false

    var championSelectInfo = ChampionSelectInfo()
    var championInfo = mapOf<Int, ChampionInfo>()
    var challengeInfo = mapOf<ChallengeCategory, MutableList<Challenge>>()
    var completableChallenges = listOf<Challenge>()
    var challengesUpdatedInfo = mutableListOf<Pair<Challenge, Challenge>>()
    var challengeInfoSummary = ChallengeSummary()

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
            val dataJson = FieldUtils.readField(event, "dataJson", true) as JsonObject
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
            val dataJson = FieldUtils.readField(event, "dataJson", true) as JsonObject
            val jsonStr = GSON.toJson(dataJson)
            val json = StringUtil.extractJsonMapFromString<Challenge>(jsonStr)

            handleChallengesChange(json.values.toList())
        },
        "/lol-event-shop/v1/info".toRegex() to { event ->
            val dataJson = FieldUtils.readField(event, "dataJson", true) as JsonObject
            val data = GSON.fromJson(dataJson, LolEventShopInfo::class.java)

            handleEventShop(data, " orb")
        },
    )

    fun start() {
        thread {
            lateinit var console: ProcessExecutor

            while (true) {
                console = ProcessExecutor(PowershellProcessWatcher.EXECUTABLE, "--end-marker--")
                console.addFinalOutputListener { s: String ->
                    val isAlive = StringUtils.countMatches(s,"LeagueClientUx.exe") > 1

                    if (isConnected == isAlive) return@addFinalOutputListener
                    isConnected = isAlive

                    Logging.log(if (isConnected) "Client Process Detected" else "Client Process Vanished", LogType.INFO)
                    if (isConnected) {
                        startClientAPI()
                    } else {
                        stopClientAPI()
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
                    .thenByDescending { it.eternalInfo.any() }
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

    private fun craftLoot(recipe: LolLootRecipeWithMilestones?, extraText: String? = null): Boolean {
        if (recipe == null) return false

        val path = "/lol-loot/v1/recipes/${recipe.recipeName}/craft"
        var lootIds = recipe.slots.flatMap { it.lootIds }
        val postRequest = if (recipe.recipeName.contains("STATSTONE_SHARD_")) {
            recipe.slots.flatMap { it.lootIds }.firstNotNullOf {
                lootIds = listOf(recipe.slots.flatMap { it.lootIds }.first())
                val postRequest = clientApi!!.executePost(path, lootIds, LolLootPlayerLootUpdate::class.java)

                if (postRequest.isOk) {
                    postRequest
                } else {
                    null
                }
            }
        } else {
            clientApi!!.executePost(path, lootIds, LolLootPlayerLootUpdate::class.java)
        }

        val response = postRequest.responseObject
        return if (postRequest.isOk && (response.added.isNotEmpty() || response.removed.isNotEmpty())) {
            val localizedName = listOf(
                { LeagueCommunityDragonApi.getLootEntity("loot_name_" + recipe.recipeName.lowercase().replace("_open", "")) },
                { recipe.description },
                { championInfo[lootIds.first { it.contains("CHAMPION_RENTAL") }.split('_').last().toIntOrNull() ?: 1]?.name }
            ).firstOrNull { !it().isNullOrEmpty() } ?: { "" }

            var nameText = localizedName()
            if (extraText != null) {
                nameText = "$extraText - $nameText"
            }
            Logging.log("Crafted '${nameText} (${recipe.recipeName})' ($path) with params [${lootIds.joinToString(", ")}]", LogType.INFO)
            true
        } else {
            var nameText = recipe.recipeName
            if (extraText != null) {
                nameText = "$extraText - $nameText"
            }
            Logging.log("Failed Craft: '$nameText' ($path) with params [${lootIds.joinToString(", ")}]", LogType.INFO)
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

    private fun craftLoot(lootElement: LolLootPlayerLoot?, recipeName: String?): Boolean {
        if (lootElement == null) return false

        val recipes = getRecipes(lootElement.lootId).toList()
        val recipe = if (!recipeName.isNullOrEmpty()) {
            recipes.firstOrNull { it.recipeName == recipeName }
        } else {
            recipes.firstOrNull()
        }

        return craftLoot(recipe)
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
            .map { craftLoot(it, recipeName = null) }
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

    private fun upgradeChampionPermanents(loot: List<LolLootPlayerLoot>, blueEssence: LolLootPlayerLoot): Boolean {
        return loot.map {
            val recipes = getRecipes(it.lootId)

            val recipe = if (recipes.size == 1) {
                recipes.first { recipe -> recipe.recipeName.lowercase().contains("disenchant") }
            } else {
                val upgradeRecipe = recipes.first { recipe -> recipe.recipeName.contains("upgrade") }
                val cost = upgradeRecipe.slots.first { slot -> slot.lootIds.contains(blueEssence.lootId) }.quantity

                if (blueEssence.count > cost) {
                    upgradeRecipe
                } else {
                    null
                }
            }

            if (recipe == null) {
                return@map false
            }

            craftLoot(recipe, extraText = it.localizedName)
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

            val info = it.localizedName.split(' ')
            val champion = info.first()
            val series = info.last().toInt()
            val recipe = if (championInfo.values.first { championInfo -> championInfo.name == champion }.eternalInfo[series] == false) {
                recipes.first { recipe -> recipe.recipeName.lowercase().contains("upgrade") }
            } else {
                recipes.first { recipe -> recipe.recipeName.lowercase().contains("disenchant") }
            }

            craftLoot(recipe, extraText = it.localizedName)
        }.any { it }
    }

    fun runEventShopCleanup() {
        val data = clientApi?.executeGet("/lol-event-shop/v1/info", LolEventShopInfo::class.java)?.responseObject ?: LolEventShopInfo(0)
        handleEventShop(data, " orb")
    }

    fun runLootCleanup() {
        val loot = clientApi!!.executeGet("/lol-loot/v1/player-loot", Array<LolLootPlayerLoot>::class.java).responseObject ?: return
        Logging.log(loot, LogType.VERBOSE)

        val ignoredIds = listOf("CURRENCY_champion", "CHEST_champion_mastery", "MATERIAL_key_fragment", "CURRENCY_champion", "CURRENCY_RP")
        val ignoredCategories = listOf("SKIN", "WARDSKIN")
        @Suppress("UNUSED_VARIABLE", "unused") val singleLoot = loot.filter { ignoredIds.all { id -> it.lootId != id } }
            .filter { ignoredCategories.all { id -> it.displayCategories != id } }
            .filter { !it.localizedName.contains(" Token") }
            .filter { it.type != "CHAMPION_RENTAL" }
            .filter { it.type != "CHAMPION_TOKEN" }

        val blueEssence = loot.first { it.lootId == "CURRENCY_champion" } // CURRENCY_RP

        val shards = loot.filter { it.type == "CHAMPION_RENTAL" }
        val permanents = loot.filter { it.type == "CHAMPION" }
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

        if (isDisenchantmentUser) {
            functions.addAll(mutableListOf(
                { upgradeMasteryTokens(loot) },

                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 7 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { championInfo[it.storeItemId]?.level == 6 && it.count == 2 } },
                { craftLoot(shards, "CHAMPION_RENTAL_disenchant") { it.count >= 1 &&
                        !ChampionOwnershipStatus.UNOWNED_SET.contains(championInfo[it.storeItemId]?.ownershipStatus) } },

                { upgradeChampionShard(shards, blueEssence) { ChampionOwnershipStatus.UNOWNED_SET.contains(championInfo[it.storeItemId]?.ownershipStatus) } },
                { upgradeChampionPermanents(permanents, blueEssence) },

//                { disenchantTokenItem(loot, "Unlock new and classic content exclusively for Mythic Essence", "Random Skin Shard") },
            ))

            // If we have any unowned champions
            val unownedChampion = championInfo.values.firstOrNull { ChampionOwnershipStatus.UNOWNED_SET.contains(it.ownershipStatus) }
            if (unownedChampion != null) {
                functions.addAll(mutableListOf(
                    { disenchantByText(loot, "Random Champion Permanent") },
                    { disenchantByText(loot, "Champion Capsule") },
                    { disenchantByText(loot, "Random Champion Shard") },
                ))
            }
        }

        if (functions.any { it.invoke() }) {
            runLootCleanup()
        }
    }

    private fun updateGlobalVariables() {
        isDisenchantmentUser = Settings.INSTANCE.disenchantIds.containsLong(summonerInfo.uniqueId)
        isDeveloper = Settings.INSTANCE.developerIds.containsLong(summonerInfo.uniqueId)
    }

    private fun updateChampionMasteryInfo() {
        if (challengeInfo.isEmpty()) {
            updateChallengesInfo()
        }

        val champions = clientApi?.executeGet("/lol-champions/v1/inventories/${summonerInfo.summonerId}/champions",
            Array<LolChampionsCollectionsChampionImpl>::class.java)?.responseObject?.filter { it.active == true } ?: return

        val championMasteryList = clientApi!!.executeGet("/lol-champion-mastery/v1/local-player/champion-mastery",
            Array<ChampionMastery>::class.java).responseObject ?: return
        Logging.log(championMasteryList, LogType.VERBOSE)

        val eternalSummary = clientApi!!.executeGet("/lol-statstones/v2/player-summary-self", Array<LolStatstonesChampionStatstoneSummary>::class.java)
                .responseObject
        Logging.log(eternalSummary, LogType.VERBOSE)

        val championIdToHasEternal = eternalSummary.associate {
            it.championId to it.sets.filter { set -> set.name != "Starter Series" }
                .filter { set -> set.stonesOwned > 0 }
                .associate { setSummary -> setSummary.name.split(" ").last().toInt() to (setSummary.stonesOwned > 0) }
        }
        Logging.log(championIdToHasEternal, LogType.VERBOSE)

        val masteryPairing = champions.filter { it.id != -1 }
            .map {
                var championPoints = 0
                var currentMasteryPoints = 0
                var nextLevelMasteryPoints = 0
                var championLevel = 0
                var tokens = 0
                var masteryBoxRewards = ""

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
                    championOwnershipStatus = ChampionOwnershipStatus.OWNED

                    val championMastery = championMasteryList.firstOrNull { championMastery -> championMastery.championId == it.id }

                    if (championMastery != null) {
                        masteryBoxRewards = championMastery.masteryBoxRewards

                        championPoints = championMastery.championPoints
                        championLevel = championMastery.championLevel
                        currentMasteryPoints = championMastery.championPointsSinceLastLevel
                        nextLevelMasteryPoints = championMastery.championPointsUntilNextLevel
                        tokens = championMastery.tokensEarned
                    }
                }

                ChampionInfo(it.id, it.name, championOwnershipStatus, championPoints, currentMasteryPoints, nextLevelMasteryPoints, championLevel, tokens,
                    eternalInfo=championIdToHasEternal.getOrDefault(it.id, mapOf()), roles=it.roles.map { role -> ChampionRole.fromString(role) }.toSet(),
                    clientApi=clientApi, masteryBoxRewards=masteryBoxRewards)
            }

        completableChallenges = challengeInfo.values.flatten().filter { it.isListingCompletedChampions }
        val challengesCompleted = hashMapOf<Int, MutableSet<Int>>()
        val challengesAvailable = hashMapOf<Int, MutableSet<Int>>()
        completableChallenges.forEach { challenge ->
            challenge.completedIdsInt.forEach { champId ->
                if (!challengesCompleted.containsKey(champId)) {
                    challengesCompleted[champId] = mutableSetOf()
                }

                challengesCompleted[champId]?.add(challenge.id?.toInt()!!)
            }

            challenge.availableIdsInt.forEach { champId ->
                if (!challengesAvailable.containsKey(champId)) {
                    challengesAvailable[champId] = mutableSetOf()
                }

                challengesAvailable[champId]?.add(challenge.id?.toInt()!!)
            }
        }

        masteryPairing.forEach { champ ->
            champ.completedChallenges = challengesCompleted[champ.id] ?: mutableSetOf()
            champ.availableChallenges = challengesAvailable[champ.id] ?: mutableSetOf()
        }

        championInfo = masteryPairing.associateBy({ it.id }, { it })
    }

    @Suppress("unused")
    private fun checkTftBattlepassRewardsAvailable() {
        val battlepass = clientApi!!.executeGet("/lol-tft/v2/tft/battlepass", LolMissionsTftPaidBattlepass::class.java).responseObject
        val missions = battlepass.milestones.filter { !it.isPaid && it.state == "REWARDABLE" }

        missions.forEach {
            val rewardGroups = LolMissionsRewardGroupsSelection().apply { rewardGroups = it.rewards.map { reward -> reward.rewardGroup } }
            val missionPut = clientApi?.executePut("/lol-missions/v1/player/" + it.missionId, rewardGroups)

            if (missionPut?.isOk != true) {
                println(missionPut)
            }
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
        val challengesResult = clientApi!!.executeGet("/lol-challenges/v1/challenges/local-player", Map::class.java).responseObject
        val jsonStr = GSON.toJson(challengesResult)
        val json = StringUtil.extractJsonMapFromString<Challenge>(jsonStr)

        val sections = json.values.groupBy { it.category!! }
            .map { entry ->
                Pair(entry.key, entry.value.sortedWith(
                    compareBy<Challenge> { it.isComplete }
                        .thenByDescending { it.currentLevel }
                        .thenByDescending { it.hasRewardTitle }
                        .thenBy { !it.rewardObtained }
                        .thenByDescending { it.nextLevelPoints }
                        .thenByDescending { it.percentage }
                ).toMutableList())
            }
            .toMap()

        challengeInfo = sections.filter { it.key != ChallengeCategory.NONE }
        challengeInfoSummary = clientApi!!.executeGet("/lol-challenges/v1/summary-player-data/local-player", ChallengeSummary::class.java).responseObject
    }

    fun executeCommand(path: String) {
        val obj = clientApi?.executeGet(path, Any::class.java)?.responseObject

        if (obj != null) {
            val jsonStr = GSON_PRETTY.toJson(obj)

            Logging.log(jsonStr, LogType.INFO)
            val selection = StringSelection(jsonStr)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
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

                runLootCleanup()
                checkEventShopRewardsAvailable()
                runEventShopCleanup()
                // checkTftBattlepassRewardsAvailable()

                socket = clientApi?.openWebSocket()
                socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                    override fun onEvent(event: ClientWebSocket.Event?) {
                        if (event == null || event.uri == null) return

                        val mappedRegex = eventListenerMapping.keys.firstOrNull { event.uri.matches(it) }
                        val bad = listOf("/lol-hovercard", "/lol-game-client-chat", "/riot-messaging-service", "/lol-patch/v1/products/league_of_legends",
                            "/patcher/v1/products/league_of_legends", "/lol-settings", "/data-store", "/lol-premade-voice", "/lol-matchmaking/v1/search",
                            "/lol-loadouts/v1/loadouts/scope/champion", "/lol-suggested-players/v1/suggested-player", "/lol-matchmaking/v1/ready-check",
                            "/lol-clash", "/lol-champ-select/v1/sfx-notifications", "/lol-regalia/v2/summoners/", "/lol-league-session/v1/league-session-token",
                            "/lol-loadouts/v4/loadouts/scope/champion/", "/lol-challenges/v1/updated-challenges/")
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
            val claimSelectAll = clientApi?.executePost("/lol-event-shop/v1/claim-select-all")
            val claimSelectBonusIteration = clientApi?.executePost("/lol-event-shop/v1/claim-select-bonus-iteration")

            if (claimSelectAll?.statusCode == 204 && claimSelectBonusIteration?.statusCode == 204) {
                Logging.log("Claimed all tokens (${data.rewardsCount})", LogType.INFO,  messageType = LogMessageType.EVENT_SHOP)
            } else {
                println("Failed endpoint.")
            }
        }
    }

    private fun getEventShop(): List<LolEventShopCategoriesOfferItem> {
        val response = clientApi?.executeGet("/lol-event-shop/v1/categories-offers", Array<LolEventShopCategoriesOffer>::class.java)?.responseObject ?: arrayOf()
        return response.flatMap { it.offers }
    }

    private fun handleEventShop(event: LolEventShopInfo, @Suppress("SameParameterValue") itemText: String) {
        val shop = getEventShop()

        // Shop is not on right now
        if (shop.isEmpty()) return

        val canPurchase: Boolean
        if (isDisenchantmentUser) {
            val item = shop.first { it.localizedTitle.lowercase().contains(itemText) }

            canPurchase = event.currentTokenBalance >= item.price
            if (!canPurchase) return

            val finalBalance = event.currentTokenBalance - item.price

            val purchase = clientApi?.executePost("/lol-event-shop/v1/purchase-offer", LolEventShopPurchaseOfferRequest(item.id))
            Logging.log("Purchased ${item.localizedTitle} for ${item.price}, remaining balance: $finalBalance", LogType.INFO, messageType = LogMessageType.EVENT_SHOP)

            if (purchase?.statusCode == 200) {
                val newEvent = LolEventShopInfo(finalBalance)
                handleEventShop(newEvent, " orb")
            }
        } else if (isMain) {
            Logging.log("Main not supported", LogType.INFO, messageType = LogMessageType.EVENT_SHOP)
        }

        runLootCleanup()
    }

    private fun handleChallengesChange(challengeInfoList: List<Challenge>) {
        challengesUpdatedInfo.clear()

        challengeInfoList.forEach {
            val index = challengeInfo[it.category]!!.indexOfFirst { old -> old.id == it.id }

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
                val gameFlow = clientApi!!.executeGet("/lol-gameflow/v1/session", GameflowSession::class.java).responseObject ?: return
                Logging.log(gameFlow, LogType.DEBUG)

                GameMode.fromGameMode(gameFlow.gameData.queue.gameMode, gameFlow.gameData.queue.id)
            }
            LolGameflowGameflowPhase.INPROGRESS -> {
                val gameFlow = clientApi!!.executeGet("/lol-gameflow/v1/session", GameflowSession::class.java).responseObject ?: return
                Logging.log(gameFlow, LogType.DEBUG)

                if (championSelectInfo.teamChampions.isEmpty()) {
                    val championId = gameFlow.gameData.getCurrentChampionId(summonerInfo.summonerId)
                    val championsTeam1 = gameFlow.gameData.teamOne.map { championInfo[it.championId] }
                        .mapNotNull { it.apply { it?.isSummonerSelectedChamp = it?.id == championId } }
                    val championsTeam2 = gameFlow.gameData.teamTwo.map { championInfo[it.championId] }
                        .mapNotNull { it.apply { it?.isSummonerSelectedChamp = it?.id == championId } }

                    val currentTeam = if (championsTeam1.any { it.isSummonerSelectedChamp }) championsTeam1 else championsTeam2
                    val otherTeam = if (currentTeam == championsTeam1) championsTeam2 else championsTeam1

                    championSelectInfo = ChampionSelectInfo(currentTeam, otherTeam, Role.ANY)
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
            updateChampionMasteryInfo()
            // checkTftBattlepassRewardsAvailable()

            checkEventShopRewardsAvailable()
            runEventShopCleanup()
        }

        if (!isDisenchantmentUser) {
            role = championSelectInfo.assignedRole
        }

        clientState = gameFlowPhase
        clientStateChanged()
    }

    fun ensureChampionsAndChallengesSetup() {
        if (challengeInfo.isEmpty()) {
            updateChallengesInfo()
        }

        if (championInfo.isEmpty()) {
            updateChampionMasteryInfo()
        }
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
                if (it.championId <= 0 && it.championPickIntent > 0) {
                    championInfo[it.championPickIntent]
                } else if (championInfo.contains(it.championId)) {
                    championInfo[it.championId]
                } else {
                    null
                }
            }

        // Set ideal champion to master based on mastery points and role size
        if (gameMode == GameMode.ARAM) {
            val idealChampions = listOf(teamChampions, benchedChampions)
                .flatMap { lst -> lst?.filter { it?.level!! < 5 }?.map { it } ?: listOf() }
                .sortedWith(
                    compareByDescending<ChampionInfo?> { it?.masteryPoints }
                        .thenByDescending { it?.roles?.size }
                )
                .mapIndexed { index, championInfo -> championInfo?.id to (index + 1) }
                .toMap()

            teamChampions.filterNotNull().forEach {
                it.isSummonerSelectedChamp = it.id == selectedChamp.championId || it.id == selectedChamp.championPickIntent
                it.idealChampionToMasterEntry = idealChampions.getOrDefault(it.id, -1)
            }

            benchedChampions?.forEach {
                it.idealChampionToMasterEntry = idealChampions.getOrDefault(it.id, -1)
            }
        }

        val assignedRole = Role.fromString(selectedChamp.assignedPosition)

        championSelectInfo = ChampionSelectInfo(teamChampions.filterNotNull(), benchedChampions ?: listOf(), assignedRole)
        championSelectChanged()
    }

    private fun handleClientConnection(): Boolean {
        if (!clientApi!!.isConnected) {
            Logging.log("Login - " + SummonerStatus.NOT_LOGGED_IN, LogType.INFO, ignorableDuplicate = true)
            return false
        }

        try {
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

        updateGlobalVariables()
        updateChampionMasteryInfo()
        updateMasteryChestInfo()
        updateClientState()
        getEternalsQueueIds()

        return true
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

    private fun clientEventChanged(event: ClientWebSocket.Event?) {
        if (event == null || event.uri.isNullOrEmpty()) return

        onLcuEventList.forEach { it(event) }
    }

    companion object {
        var summonerInfo = SummonerInfo()
    }
}
