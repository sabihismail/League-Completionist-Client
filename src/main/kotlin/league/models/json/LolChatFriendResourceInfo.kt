@file:Suppress("unused")

package league.models.json

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
enum class LolProductName {
    @SerializedName("league_of_legends")
    LEAGUE_OF_LEGENDS,

    @SerializedName("valorant")
    VALORANT,
}

@Serializable
enum class LolAvailability {
    @SerializedName("chat")
    CHAT,

    @SerializedName("offline")
    OFFLINE,

    @SerializedName("mobile")
    MOBILE,
}


@Suppress("MemberVisibilityCanBePrivate")
@Serializable
class LolChatFriendResourceImpl {
    var availability: LolAvailability = LolAvailability.OFFLINE
    // var displayGroupId: Int? = null
    // var displayGroupName: String? = null
    var gameName: String? = null
    var gameTag: String? = null
    // var groupId: Int? = null
    // var groupName: String? = null
    // var icon: Int? = null
    // var id: String? = null
    // var isP2PConversationMuted: Boolean? = null
    // var lastSeenOnlineTimestamp: String? = null
    // var lol: Any? = null
    var name: String? = null
    var note: String? = null
    // var patchline: String? = null
    // var pid: String? = null
    // var platformId: String? = null
    var product: LolProductName? = null
    // var productName: String? = null
    // var puuid: String? = null
    var statusMessage: String? = null
    // var summary: String? = null
    var summonerId: Long? = null
    // var time: Long? = null
    var ownerFriend = ""
}
