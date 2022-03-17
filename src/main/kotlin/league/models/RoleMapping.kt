package league.models

import kotlinx.serialization.SerialName
import java.util.*
import kotlin.collections.HashMap

@kotlinx.serialization.Serializable
data class RoleMapping(@SerialName("TOP") val top: HashMap<Int, Float>,
                       @SerialName("JUNGLE") val jungle: HashMap<Int, Float>,
                       @SerialName("MIDDLE") val middle: HashMap<Int, Float>,
                       @SerialName("BOTTOM") val bottom: HashMap<Int, Float>,
                       @SerialName("SUPPORT") val support: HashMap<Int, Float>)

data class SummonerInfo(
    var status: SummonerStatus = SummonerStatus.NOT_CHECKED,
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

data class ChampionSelectInfo(val gameMode: GameMode = GameMode.NONE, val teamChampions: List<ChampionInfo?> = listOf(),
                              val benchedChampions: List<ChampionInfo> = listOf(), val assignedRole: Role = Role.ANY)
