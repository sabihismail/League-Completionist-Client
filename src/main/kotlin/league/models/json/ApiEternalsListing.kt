package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.EternalTrackingType
import util.KotlinExtensionUtil.toCommaSeparatedNumber
import util.StringUtil
import util.constants.GenericConstants.DISTANCE_CONSTANT

@Serializable
data class ApiEternalsListing(val name: String, val contentId: String, val boundChampion: ApiEternalsChampion, val milestones: List<Int>, val trackingType: Int) {
    private val trackingTypeValue get() = EternalTrackingType.values()[trackingType]

    fun getMilestoneValues(): List<Pair<Int, String>> {
        val elements = milestones.foldIndexed<Int, List<Int>>(listOf()) { i, acc, e -> acc + ((if (i >= 1) acc[i - 1] else 0) + e) }
        return when(trackingTypeValue) {
            EternalTrackingType.COUNT -> {
                elements.zip(elements.map { it.toCommaSeparatedNumber() })
            }
            EternalTrackingType.TIME -> {
                elements.zip(elements.map { StringUtil.toTimeStyleString(it, listOf(1 to "s", 60 to "m", 60 to "h")) })
            }
            EternalTrackingType.DISTANCE -> {
                elements.map { it / DISTANCE_CONSTANT to StringUtil.toDistanceString(it / DISTANCE_CONSTANT, listOf(1 to "m", 1000 to "km")) }
            }
        }
    }

    override fun toString(): String {
        return "ApiEternalsListing(name='$name', contentId='$contentId', boundChampion=$boundChampion, milestones=$milestones, trackingType=$trackingType, " +
                "trackingTypeValue=$trackingTypeValue)"
    }
}
