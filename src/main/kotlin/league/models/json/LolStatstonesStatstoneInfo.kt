package league.models.json

import league.api.LeagueCommunityDragonApi
import java.text.NumberFormat

class LolStatstonesStatstoneInfo {
    private var statstoneId: String = ""
    var description: String = ""
    var formattedMilestoneLevel: String = ""
    var formattedValue: String? = null
    var name: String? = null
    var nextMilestone: String = ""
    /*
    var boundChampionItemId: Int? = null
    var category: String? = null
    var completionValue: Double? = null
    var formattedPersonalBest: String? = null
    var imageUrl: String? = null
    var isComplete: Boolean? = null
    var isEpic: Boolean? = null
    var isFeatured: Boolean? = null
    var isRetired: Boolean? = null
    var playerRecord: LolStatstonesStatstonePlayerRecord? = null
    */

    var summaryThreshold = lazy {
        LeagueCommunityDragonApi.getEternal(statstoneId)
            .dropLast(1)
            .filter { it.first > parseNextMilestone(it.second, nextMilestone) }
            .joinToString(" > ") { it.second }
    }

    private fun parseNextMilestone(parsed: String, nextMilestone: String): Int {
        val timeMapping = hashMapOf('h' to 60 * 60, 'm' to 60, 's' to 1)
        val distanceMapping = hashMapOf("km" to 1000, "m" to 1)

        return if (timeMapping.keys.any { parsed.contains(it) } && !parsed.contains(".")) {
            nextMilestone.split(" ").sumOf { it.substring(0, it.length - 1).toInt() * timeMapping[it.last()]!! }
        } else if (distanceMapping.keys.any { parsed.contains(it) } && parsed.contains(".")) {
            var numberOfStrOnly = nextMilestone
            var key = ""

            while (numberOfStrOnly.last().isLetter()) {
                key = numberOfStrOnly.last() + key
                numberOfStrOnly = numberOfStrOnly.substring(0, numberOfStrOnly.length - 1)
            }

            return (numberOfStrOnly.toDouble() * distanceMapping[key]!!).toInt()
        } else {
            return NumberFormat.getNumberInstance().parse(nextMilestone).toInt()
        }
    }
}
