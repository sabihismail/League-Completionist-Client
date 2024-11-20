package league.models.json.communitydragon

import kotlinx.serialization.Serializable

@Serializable
data class ApiQueueInfoResponse(val id: Int, val name: String, val shortName: String, val description: String, val detailedDescription: String) {
    override fun toString(): String {
        return "QueueInfo(id='$id', name='$name', shortName='$shortName', description='$description', detailedDescription='$detailedDescription')"
    }
}