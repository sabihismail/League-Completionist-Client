package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiEternalsStatstoneData(val name: String, val statstones: List<ApiEternalsListing>) {
    override fun toString(): String {
        return "ApiEternalsInfo(name='$name', statstones=$statstones)"
    }
}