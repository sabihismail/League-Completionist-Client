package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.EternalTrackingType
import util.KotlinExtensionUtil.toCommaSeparatedNumber
import util.StringUtil

@Serializable
data class ApiEternalsListing(val name: String, val contentId: String, val boundChampion: ApiEternalsChampion, val milestones: List<Int>, val trackingType: Int) {
    private val trackingTypeValue get() = EternalTrackingType.values()[trackingType]

    fun getMilestoneValues(): List<Pair<Int, String>> {
        val elements = milestones.foldIndexed<Int, List<Int>>(listOf()) { i, acc, e -> acc + ((if (i >= 1) acc[i - 1] else 0) + e) }
        val displayedElements = when(trackingTypeValue) {
            EternalTrackingType.COUNT -> {
                elements.map { it.toCommaSeparatedNumber() }
            }
            EternalTrackingType.TIME -> {
                elements.map { StringUtil.toTimeStyleString(it, listOf(1 to "s", 60 to "m", 60 to "h")) }
            }
            EternalTrackingType.DISTANCE -> {
                elements.map { StringUtil.toDistanceString(it, listOf(1 to "m", 1000 to "km")) }
            }
        }

        return elements.zip(displayedElements)
    }

    override fun toString(): String {
        return "ApiEternalsListing(name='$name', contentId='$contentId', boundChampion=$boundChampion, milestones=$milestones, trackingType=$trackingType, " +
                "trackingTypeValue=$trackingTypeValue)"
    }
}
