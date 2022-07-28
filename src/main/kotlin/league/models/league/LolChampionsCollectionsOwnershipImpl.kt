package league.models.league

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class LolChampionsCollectionsOwnershipImpl {
    var freeToPlayReward: Boolean = false
    var owned: Boolean = false
    var rental: LolChampionsCollectionsRentalImpl? = null
}