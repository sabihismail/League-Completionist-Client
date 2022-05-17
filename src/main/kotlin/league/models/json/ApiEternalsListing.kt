package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiEternalsListing(val name: String, val contentId: String, val boundChampion: ApiEternalsChampion, val milestones: List<Int>) {
    override fun toString(): String {
        return "ApiEternalsListing(name='$name', contentId=$contentId, boundChampion=$boundChampion, milestones=$milestones)"
    }
}