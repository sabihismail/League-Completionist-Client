package db.models

import org.jetbrains.exposed.dao.id.IntIdTable

object ChallengeMappingTable : IntIdTable() {
    val name = text("name")
    val championId = integer("champion_id")
    val summonerUniqueId = long("summoner_unique_id")
    val isComplete = bool("is_complete")
}