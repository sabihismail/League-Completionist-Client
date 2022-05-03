package db.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object MasteryChestTable : IntIdTable() {
    val accountId = long("account_id").uniqueIndex()
    val name = text("account_name")
    val lastBoxDate = datetime("last_box_date")
}