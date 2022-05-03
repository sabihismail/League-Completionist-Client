package ui.views

import db.DatabaseImpl
import db.models.MasteryChestTable
import javafx.geometry.Orientation
import org.jetbrains.exposed.sql.ResultRow
import tornadofx.*
import ui.controllers.MainViewController
import java.time.LocalDateTime
import java.time.ZoneId


class MasteryAccountView: View() {
    override var root = hbox {
        paddingBottom = 8.0
    }

    fun run() {
        root.children.removeAll()

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
        val currentChestCount = (MainViewController.CHEST_MAX_COUNT - (diff / MainViewController.CHEST_WAIT_TIME)).toInt()
        val nextChestDays = diff % MainViewController.CHEST_WAIT_TIME

        return "${entry[MasteryChestTable.name]} - $currentChestCount (next in ${String.format("%.2f", nextChestDays)} days)"
    }
}