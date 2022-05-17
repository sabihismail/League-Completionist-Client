package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiEternalsChampion(val itemId: Int) {
    override fun toString(): String {
        return "ApiEternalsChampion(itemId=$itemId)"
    }
}
