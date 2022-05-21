package db.models

import org.jetbrains.exposed.dao.id.IntIdTable

object HextechTable : IntIdTable()  {
    val accountId = long("account_id").uniqueIndex()
    val isAutoEnchant = bool("is_auto_enchant").default(false)
}