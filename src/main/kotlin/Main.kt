import com.stirante.lolclient.ClientApi
import com.stirante.lolclient.ClientConnectionListener
import com.stirante.lolclient.ClientWebSocket
import generated.LolChampionsCollectionsChampion
import generated.LolCollectionsCollectionsChampionMastery
import generated.LolSummonerSummoner


const val loggingEndpoints = false

val enabled = true

data class SummonerChampionInfo(val name: String, val masteryStatus: MasteryStatus, val masteryPoints: Int)

enum class MasteryStatus(i: Int) {
    NONE(0),
    FREE_TO_PLAY(1),
    BOX_NOT_ATTAINED(2),
    BOX_ATTAINED(3)
}

fun main(args: Array<String>) {
    val clientAPI = ClientApi()
    var socket: ClientWebSocket? = null

    clientAPI.addClientConnectionListener(object : ClientConnectionListener {
        override fun onClientConnected() {
            if (!clientAPI.isAuthorized) return

            socket = clientAPI.openWebSocket()
            socket?.setSocketListener(object : ClientWebSocket.SocketListener {
                override fun onEvent(event: ClientWebSocket.Event?) {
                    if (event?.uri != null && event.uri.startsWith("/lol-champ-select")) {
                        println(event.eventType + " - " + event.uri)
                        println(event.data)
                    } else {
                        if (!loggingEndpoints) return

                        println("RE")
                        println(event?.eventType + " - " + event?.uri)
                        println(event?.data)
                    }
                }

                override fun onClose(code: Int, reason: String?) {

                }
            })

            val summoner = clientAPI.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner::class.java).responseObject
            val champions = clientAPI.executeGet("/lol-champions/v1/inventories/${summoner.summonerId}/champions",
                Array<LolChampionsCollectionsChampion>::class.java).responseObject

            val championMasteryList = clientAPI.executeGet("/lol-collections/v1/inventories/${summoner.summonerId}/champion-mastery",
                Array<LolCollectionsCollectionsChampionMastery>::class.java).responseObject

            val masteryPairing = champions.map {
                val status = if (!it.ownership.owned) {
                    if (it.freeToPlay) MasteryStatus.FREE_TO_PLAY else MasteryStatus.NONE
                } else {
                    val championMastery = championMasteryList.firstOrNull { championMastery -> championMastery.championId == it.id }

                    if (championMastery == null) println(it.name)

                    if (championMastery != null && championMastery.chestGranted) MasteryStatus.BOX_ATTAINED else MasteryStatus.BOX_NOT_ATTAINED
                }

                Pair(it.name, status)
            }

            for (pairing in masteryPairing) {
                println("${pairing.first} - ${pairing.second}")
            }
        }

        override fun onClientDisconnected() {
            socket?.close()
        }
    })

    while (enabled) {
        Thread.sleep(1000)
    }
}
