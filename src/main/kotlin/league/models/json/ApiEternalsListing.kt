package league.models.json

import kotlinx.serialization.Serializable
import league.models.enums.EternalTrackingType
import util.KotlinExtensionUtil.toReadableNumber
import util.StringUtil

@Serializable
data class ApiEternalsListing(val name: String, val contentId: String, val boundChampion: ApiEternalsChampion, val milestones: List<Int>, val trackingType: Int) {
    private val trackingTypeValue get() = EternalTrackingType.values()[trackingType]

    fun getMilestoneValues(): List<Pair<Int, String>> {
        val elements = milestones.foldIndexed<Int, List<Int>>(listOf()) { i, acc, e -> acc + ((if (i >= 1) acc[i - 1] else 0) + e) }
        val displayedElements = when(trackingTypeValue) {
            EternalTrackingType.COUNT -> {
                elements.map { it.toReadableNumber() }
            }
            EternalTrackingType.TIME -> {
                elements.map { StringUtil.parseSecondsToHMS(it) }
            }
            EternalTrackingType.DISTANCE -> {
                elements.map {
                    if (it < 1000) {
                        it.toString()
                    } else {
                        "%.1f".format(it / 1000.0)
                    }
                }
            }
        }

        return elements.zip(displayedElements)
    }

    override fun toString(): String {
        return "ApiEternalsListing(name='$name', contentId='$contentId', boundChampion=$boundChampion, milestones=$milestones, trackingType=$trackingType, " +
                "trackingTypeValue=$trackingTypeValue)"
    }
}
