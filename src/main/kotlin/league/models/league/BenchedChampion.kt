package league.models.league

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
data class BenchedChampion(val championId: Int, val isPriority: Boolean)