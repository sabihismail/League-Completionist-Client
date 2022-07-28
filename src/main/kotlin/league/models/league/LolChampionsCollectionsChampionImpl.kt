package league.models.league

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class LolChampionsCollectionsChampionImpl {
    var active: Boolean? = null
    var alias: String? = null
    var banVoPath: String? = null
    var baseLoadScreenPath: String? = null
    var baseSplashPath: String? = null
    var botEnabled: Boolean? = null
    var chooseVoPath: String? = null
    var disabledQueues: List<String>? = null
    var freeToPlay: Boolean = false
    var id: Int = -1
    var name: String = ""
    var ownership: LolChampionsCollectionsOwnershipImpl? = null
    // var passive: LolChampionsCollectionsChampionSpell? = null
    var purchased: Double? = null
    var rankedPlayEnabled: Boolean? = null
    var roles: List<String> = listOf()
    // var skins: List<LolChampionsCollectionsChampionSkin>? = null
    // var spells: List<LolChampionsCollectionsChampionSpell>? = null
    var squarePortraitPath: String? = null
    var stingerSfxPath: String? = null
    // var tacticalInfo: LolChampionsCollectionsChampionTacticalInfo? = null
    var title: String? = null
}
