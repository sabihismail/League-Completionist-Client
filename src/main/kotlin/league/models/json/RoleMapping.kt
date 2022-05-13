package league.models.json

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class RoleMapping(@SerialName("TOP") val top: HashMap<Int, Float>,
                       @SerialName("JUNGLE") val jungle: HashMap<Int, Float>,
                       @SerialName("MIDDLE") val middle: HashMap<Int, Float>,
                       @SerialName("BOTTOM") val bottom: HashMap<Int, Float>,
                       @SerialName("SUPPORT") val support: HashMap<Int, Float>? = null,
                       @SerialName("UTILITY") val utility: HashMap<Int, Float>? = null)
