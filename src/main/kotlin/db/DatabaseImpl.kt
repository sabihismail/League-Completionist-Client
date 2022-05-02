package db

import db.models.MasteryBoxTable
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ui.controllers.MainViewController.Companion.CHEST_MAX_COUNT
import ui.controllers.MainViewController.Companion.CHEST_WAIT_TIME
import java.sql.Connection
import java.time.LocalDateTime

object DatabaseImpl {
    init {
        Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.createMissingTablesAndColumns(MasteryBoxTable)
        }
    }

    fun setMasteryInfo(summonerInfo: SummonerInfo, masteryChestInfo: MasteryChestInfo, extra: Double) {
        val finalDate = if (CHEST_MAX_COUNT == masteryChestInfo.chestCount) {
            LocalDateTime.now().plusMinutes((CHEST_WAIT_TIME * CHEST_MAX_COUNT * 24 * 60).toLong())
        } else {
            val count = ((CHEST_MAX_COUNT - masteryChestInfo.chestCount - 1) * CHEST_WAIT_TIME + extra) * 24 * 60

            LocalDateTime.now().plusMinutes(count.toLong())
        }

        transaction {
            val uniqueAccountId = summonerInfo.accountID.xor(summonerInfo.summonerID)

            if (MasteryBoxTable.select { MasteryBoxTable.accountId eq uniqueAccountId }.count() >= 1) {
                MasteryBoxTable.update({ MasteryBoxTable.accountId eq uniqueAccountId }) { update ->
                    update[lastBoxDate] = finalDate
                }
            } else {
                MasteryBoxTable.insert { insert ->
                    insert[accountId] = uniqueAccountId
                    insert[lastBoxDate] = finalDate
                }
            }
        }
    }
}