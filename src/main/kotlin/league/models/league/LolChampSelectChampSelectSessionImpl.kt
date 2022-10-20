package league.models.league

import generated.LolChampSelectChampSelectPlayerSelection
import generated.LolChampSelectChampSelectTimer
import generated.LolChampSelectChampSelectTradeContract
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class LolChampSelectChampSelectSessionImpl {
    // var actions: List<Any>? = null
    var allowBattleBoost: Boolean? = null
    var allowDuplicatePicks: Boolean? = null
    var allowLockedEvents: Boolean? = null
    var allowRerolling: Boolean? = null
    var allowSkinSelection: Boolean? = null
    // var bans: LolChampSelectChampSelectBannedChampions? = null
    var benchChampions: List<BenchedChampion>? = null
    var benchEnabled: Boolean? = null
    var boostableSkinCount: Int? = null
    // var chatDetails: LolChampSelectChampSelectChatRoomDetails? = null
    var counter: Long? = null
    // var entitledFeatureState: LolChampSelectEntitledFeatureState? = null
    var gameId: Long? = null
    var hasSimultaneousBans: Boolean? = null
    var hasSimultaneousPicks: Boolean? = null
    var isCustomGame: Boolean? = null
    var isSpectating: Boolean? = null
    var localPlayerCellId: Long? = null
    var lockedEventIndex: Int? = null
    var myTeam: List<@Contextual LolChampSelectChampSelectPlayerSelection> = listOf()
    var recoveryCounter: Long? = null
    var rerollsRemaining: Int? = null
    var skipChampionSelect: Boolean? = null
    var theirTeam: List<@Contextual LolChampSelectChampSelectPlayerSelection>? = null
    var timer: @Contextual LolChampSelectChampSelectTimer? = null
    var trades: List<@Contextual LolChampSelectChampSelectTradeContract>? = null
}