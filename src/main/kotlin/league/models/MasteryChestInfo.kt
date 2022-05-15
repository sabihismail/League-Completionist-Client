package league.models

import java.util.*

data class MasteryChestInfo(var nextChestDate: Date? = null, var chestCount: Int = 0) {
    val remainingTime get() = (nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
    val remainingStr get() = if (nextChestDate == null) "" else String.format("%.2f", remainingTime)

    override fun toString(): String {
        return "MasteryChestInfo(nextChestDate=$nextChestDate, chestCount=$chestCount)"
    }
}