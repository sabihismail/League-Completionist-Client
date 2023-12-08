package ui.views

import generated.LolSummonerSummoner
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import league.models.json.LolAvailability
import league.models.json.Friend
import league.models.json.lolChatFriendResourceImplPropertyMap
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.message.BasicHeader
import tornadofx.*
import util.HttpUtil
import util.ProcessUtil
import java.util.*


class FriendsView : View("Friend List") {
    private val friends = SimpleListProperty<Friend>()
    private val searchProperty = SimpleStringProperty(this, SEARCH_FILTER, config.string(SEARCH_FILTER) ?: "")
    private val isOnlineProperty = SimpleBooleanProperty(this, IS_ONLINE, config.boolean(IS_ONLINE) ?: true)
    private val isHideMobileProperty = SimpleBooleanProperty(this, IS_HIDE_MOBILE, config.boolean(IS_HIDE_MOBILE) ?: true)

    private val userIdMapping = hashMapOf<String, String>()
    private val friendsLst = HashMap<String, Array<Friend>>()

    private val booleanFilterMapping = listOf<Pair<SimpleBooleanProperty, (Friend) -> Boolean>>(
        Pair(isOnlineProperty) { it.availability != LolAvailability.OFFLINE },
        Pair(isHideMobileProperty) { it.availability != LolAvailability.MOBILE },
    )

    override val root = borderpane {
        prefWidth = MainView.APP_WIDTH
        prefHeight = MainView.APP_HEIGHT

        center = tableview(friends) {
            column("Account", Friend::ownerFriend) { minWidth = 50.0 }
            column("Friend Name", Friend::gameName) { minWidth = 150.0 }
            column("Note", Friend::note) { minWidth = 300.0 }
            column("Game") {
                minWidth = 150.0

                if (it.value.availability == LolAvailability.MOBILE) {
                    "mobile".toProperty()
                } else if (it.value.product != null) {
                    it.value.product.toString().toProperty()
                } else {
                    "".toProperty()
                }
            }
        }

        bottom = hbox {
            paddingBottom = 12.0
            paddingLeft = 12.0
            alignment = Pos.CENTER_LEFT

            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 10.0

                label("Filter: ")
                textfield(searchProperty) {
                    textProperty().addListener { _, _, new ->
                        config[SEARCH_FILTER] = new
                        refresh()
                    }
                }

                checkbox("Is Online", isOnlineProperty).apply {
                    isOnlineProperty.onChange {
                        config[IS_ONLINE] = it.toString()
                        refresh()
                    }
                }

                checkbox("Is Hide Mobile", isHideMobileProperty).apply {
                    isHideMobileProperty.onChange {
                        config[IS_HIDE_MOBILE] = it.toString()
                        refresh()
                    }
                }
            }
        }
    }

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                val output = ProcessUtil.runCommand("wmic PROCESS WHERE name='LeagueClientUx.exe' GET commandline")

                val portRegex = ".*--app-port=([0-9]*).*".toRegex()
                val tokenRegex = ".*--remoting-auth-token=([a-zA-Z0-9-_]+).*".toRegex()
                for (line in output.split("\n")) {
                    if (!line.contains("--app-port=") || !line.contains("--remoting-auth-token=")) continue

                    val port = portRegex.find(line)?.groups?.get(1)?.value
                    val password = tokenRegex.find(line)?.groups?.get(1)?.value

                    if (port.isNullOrBlank() || password.isNullOrBlank()) {
                        println("[FriendsView] Cmd Failed parsing: $line")
                        continue
                    }

                    val token = String(Base64.getEncoder().encode("riot:$password".toByteArray()))

                    val baseUrl = "https://127.0.0.1:${port}"
                    val headers = listOf(BasicHeader(HttpHeaders.AUTHORIZATION, "Basic $token"), BasicHeader(HttpHeaders.ACCEPT, "*/*"))

                    if (!userIdMapping.containsKey(port)) {
                        val summoner = HttpUtil.makeGetRequestJson<LolSummonerSummoner>("$baseUrl/lol-summoner/v1/current-summoner", headers = headers)

                        if (summoner != null) {
                            userIdMapping[port] = summoner.displayName
                        }
                    }

                    val friendResponse = HttpUtil.makeGetRequestJson<Array<Friend>>("$baseUrl/lol-chat/v1/friends", headers = headers)
                    handleFriendArray(userIdMapping[port], friendResponse)
                }
            }
        }, 0, 60 * 1000)
    }

    private fun handleFriendArray(userName: String?, response: Array<Friend>?) {
        if (response == null || userName.isNullOrBlank()) return

        response.onEach { it.ownerFriend = userName }
        friendsLst[userName] = response

        refresh()
    }

    private fun refresh() {
        runAsync {
            var lst = friendsLst.flatMap { it.value.toList() }

            val booleanFilters = booleanFilterMapping.filter { map -> map.first.get() }.map { it.second }
            booleanFilters.forEach { lst = lst.filter(it) }

            val tagStr = searchProperty.value.toString().lowercase()
            if (tagStr.isNotBlank()) {
                lst = lst.filter { x -> lolChatFriendResourceImplPropertyMap.any { it.getter.call(x).toString().lowercase().contains(tagStr) } }
            }

            lst
        } ui { lst ->
            friends.value = FXCollections.observableList(lst)
        }
    }

    fun onClose() {
        config.save()
    }

    companion object {
        val timer = Timer()

        const val SEARCH_FILTER = "tag"
        const val IS_ONLINE = "is_online"
        const val IS_HIDE_MOBILE = "is_hide_mobile"
    }
}

