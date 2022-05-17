package league.models.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoleMapping(@SerialName("TOP") val top: HashMap<Int, Float>,
                       @SerialName("JUNGLE") val jungle: HashMap<Int, Float>,
                       @SerialName("MIDDLE") val middle: HashMap<Int, Float>,
                       @SerialName("BOTTOM") val bottom: HashMap<Int, Float>,
                       @SerialName("SUPPORT") val support: HashMap<Int, Float>? = null,
                       @SerialName("UTILITY") val utility: HashMap<Int, Float>? = null) {
    override fun toString(): String {
        return "RoleMapping(top=$top, jungle=$jungle, middle=$middle, bottom=$bottom, support=$support, utility=$utility)"
    }
}
