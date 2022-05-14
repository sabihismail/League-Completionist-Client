package league.models

import java.util.*

data class MasteryChestInfo(var nextChestDate: Date? = null, var chestCount: Int = 0) {
    override fun toString(): String {
        return "MasteryChestInfo(nextChestDate=$nextChestDate, chestCount=$chestCount)"
    }
}