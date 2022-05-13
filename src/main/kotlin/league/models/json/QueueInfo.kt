package league.models.json

@kotlinx.serialization.Serializable
data class QueueInfo(val name: String, val shortName: String, val description: String, val detailedDescription: String)