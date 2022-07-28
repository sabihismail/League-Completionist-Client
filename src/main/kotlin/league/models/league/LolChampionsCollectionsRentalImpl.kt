package league.models.league

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class LolChampionsCollectionsRentalImpl {
    var endDate: Long? = null
    var purchaseDate: Double? = null
    var rented: Boolean? = null
    var winCountRemaining: Int? = null
}