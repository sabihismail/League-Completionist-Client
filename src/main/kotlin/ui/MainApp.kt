package ui

import league.LeagueConnection
import league.SummonerStatus
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import tornadofx.*
import java.util.*

class MainViewController : Controller() {
    private val view: MainView by inject()
    private val leagueConnection = LeagueConnection()

    init {
        leagueConnection.start()

        leagueConnection.onSummonerChange {
            val str = when(it.status) {
                SummonerStatus.NOT_LOGGED_IN -> "Not logged in."
                SummonerStatus.LOGGED_IN_UNAUTHORIZED -> "Unauthorized Login."
                SummonerStatus.LOGGED_IN_AUTHORIZED -> "Logged in as: ${it.displayName} (Level: ${it.summonerLevel})"
            }

            runLater { view.summonerProperty.set(str) }

            if (it.status != SummonerStatus.LOGGED_IN_AUTHORIZED) return@onSummonerChange

            leagueConnection.updateMasteryChestInfo()
        }

        leagueConnection.onMasteryChestChange {
            if (it.nextChestDate == null) return@onMasteryChestChange

            val remaining = (it.nextChestDate!!.time - Calendar.getInstance().timeInMillis) / (1000.0 * 60 * 60 * 24)
            val remainingStr = String.format("%.2f", remaining)

            runLater { view.chestProperty.set("Available chests: ${it.chestCount} (next one in $remainingStr days)") }
        }
    }

    fun updateChestInfo() {
        view.chestProperty.set("Querying...")

        leagueConnection.updateMasteryChestInfo()
    }

    fun updateChampionMasteryInfo() {
        leagueConnection.updateChampionMasteryInfo()
    }

    fun stopOrStart() {

    }
}

class MainView: View() {
    val summonerProperty = SimpleStringProperty()
    val chestProperty = SimpleStringProperty()

    private val controller = find(MainViewController::class)

    override val root = vbox {
        alignment = Pos.TOP_CENTER
        prefWidth = 800.0
        prefHeight = 600.0

        label(summonerProperty)
        label(chestProperty)
        hbox {
            alignment = Pos.CENTER
            spacing = 6.0

            button("Refresh Chest Data") {
                action { controller.updateChestInfo() }
            }
            button("Refresh Champion Mastery Data") {
                action { controller.updateChampionMasteryInfo() }
            }
            button("Stop") {
                action { controller.stopOrStart() }
            }
        }
    }
}

class MainApp: App(MainView::class)
