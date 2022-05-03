package ui.views

import db.DatabaseImpl
import db.models.MasteryChestTable
import javafx.geometry.Orientation
import org.jetbrains.exposed.sql.ResultRow
import tornadofx.*
import ui.controllers.MainViewController.Companion.CHEST_MAX_COUNT
import ui.controllers.MainViewController.Companion.CHEST_WAIT_TIME
import java.time.LocalDateTime
import java.time.ZoneId


class MasteryAccountView: View() {
    override var root = hbox {
        paddingBottom = 8.0
    }

    init {
        run()
    }

    fun run() {
        root.children.clear()

        val lst = DatabaseImpl.getMasteryChestEntryCount()
        lst.forEachIndexed { i, entry ->
            val label = label(getMasteryString(entry)) {
                paddingHorizontal = 8.0
            }

            root.add(label)

            if (i == lst.size - 1) return

            val separator = separator(Orientation.VERTICAL)
            root.add(separator)
        }
    }

    private fun getMasteryString(entry: ResultRow): String {
        val zoneId = ZoneId.systemDefault()
        val diff = (entry[MasteryChestTable.lastBoxDate].atZone(zoneId).toEpochSecond() - LocalDateTime.now().atZone(zoneId).toEpochSecond()) / (24 * 60 * 60.0)
        val currentChestCount = (CHEST_MAX_COUNT - (diff / CHEST_WAIT_TIME)).toInt()

        var s = "${entry[MasteryChestTable.name]} - $currentChestCount"

        if (diff > 0) {
            s += " (next in ${String.format("%.2f", diff % CHEST_WAIT_TIME)} days)"
        }

        return s
    }
}