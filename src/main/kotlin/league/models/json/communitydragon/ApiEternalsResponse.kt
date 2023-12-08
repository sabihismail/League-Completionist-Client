package league.models.json.communitydragon

import kotlinx.serialization.Serializable


@Serializable
data class ApiEternalsStatstoneData(val name: String, val statstones: List<ApiEternalsListing>) {
    override fun toString(): String {
        return "ApiEternalsInfo(name='$name', statstones=$statstones)"
    }
}

@Serializable
data class ApiEternalsResponse(val statstoneData: List<ApiEternalsStatstoneData>) {
    override fun toString(): String {
        return "ApiEternalsResponse(statstoneData=$statstoneData)"
    }
}