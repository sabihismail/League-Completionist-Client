package league.models.json

import kotlinx.serialization.Serializable

@Serializable
data class ApiQueueInfoResponse(val name: String, val shortName: String, val description: String, val detailedDescription: String) {
    override fun toString(): String {
        return "QueueInfo(name='$name', shortName='$shortName', description='$description', detailedDescription='$detailedDescription')"
    }
}