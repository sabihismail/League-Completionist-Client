package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiEternalsResponse(val statstoneData: List<ApiEternalsStatstoneData>) {
    override fun toString(): String {
        return "ApiEternalsResponse(statstoneData=$statstoneData)"
    }
}