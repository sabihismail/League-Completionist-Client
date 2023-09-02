package ui.views

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import okhttp3.Headers
import tornadofx.*
import util.ProcessUtil
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


class FriendsView : View("Friend List") {
    override val configPath: Path = Paths.get("friends.properties")

    val tag = SimpleStringProperty(this, TAG, config.string(TAG))

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                val output = ProcessUtil.runCommand("wmic PROCESS WHERE name='LeagueClientUx.exe' GET commandline")

                val portRegex = ".*--app-port=([0-9]*).*".toRegex()
                val tokenRegex = ".*--remoting-auth-token=([a-zA-Z0-9-_]+).*".toRegex()
                for (line in output.split("\n")) {
                    val port = portRegex.find(line)?.groups?.get(1)
                    val token = tokenRegex.find(line)?.groups?.get(1)

                    val headers = Headers.Builder().add("Authorization", "Basic $token").build()
                    //val response = HttpUtil.makeGetRequestJson<Array<LolChatFriendResource>>("https://127.0.0.1:${port}/lol-chat/v1/friends", headers = headers)
                }
            }
        }, 0, 60 * 1000)
    }

    override val root = borderpane {
        center = borderpane {

        }

        bottom = borderpane {
            paddingBottom = 24.0
            paddingRight = 24.0

            hbox {
                alignment = Pos.BOTTOM_RIGHT
                spacing = 6.0

                hbox {
                    alignment = Pos.CENTER_LEFT

                    label("Tag: ")
                    textfield("") {
                        textProperty().addListener { _, _, new ->
                            config[TAG] = new
                        }
                    }
                }
            }
        }
    }

    companion object {
        val timer = Timer()

        const val TAG = "tag"
    }
}

