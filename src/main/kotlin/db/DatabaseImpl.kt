package db

import db.models.MasteryChestTable
import league.models.MasteryChestInfo
import league.models.SummonerInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ui.controllers.MainViewController.Companion.CHEST_MAX_COUNT
import ui.controllers.MainViewController.Companion.CHEST_WAIT_TIME
import java.sql.Connection
import java.time.Duration
import java.time.LocalDateTime

object DatabaseImpl {
    init {
        Database.connect("jdbc:sqlite:database.db", "org.sqlite.JDBC")

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                MasteryChestTable,
            )
        }
    }

    fun setMasteryInfo(summonerInfo: SummonerInfo, masteryChestInfo: MasteryChestInfo) {
        val finalDate = if (CHEST_MAX_COUNT == masteryChestInfo.chestCount) {
            LocalDateTime.now()
        } else {
            val count = ((CHEST_MAX_COUNT - masteryChestInfo.chestCount - 1) * CHEST_WAIT_TIME + masteryChestInfo.remainingTime) * 24 * 60

            LocalDateTime.now().plusMinutes(count.toLong())
        }

        transaction {
            val uniqueAccountId = summonerInfo.uniqueId

            val query: (SqlExpressionBuilder.() -> Op<Boolean>) = { MasteryChestTable.accountId eq uniqueAccountId }
            if (MasteryChestTable.select(query).count() >= 1) {
                MasteryChestTable.update(query) { update ->
                    update[name] = summonerInfo.displayName
                    update[lastBoxDate] = finalDate
                }
            } else {
                MasteryChestTable.insert { insert ->
                    insert[accountId] = uniqueAccountId
                    insert[name] = summonerInfo.displayName
                    insert[lastBoxDate] = finalDate
                }
            }
        }
    }

    fun getMasteryChestInfo(): MutableList<ResultRow> {
        val lst = mutableListOf<ResultRow>()
        transaction {
            val now = LocalDateTime.now()
            val elements = MasteryChestTable.selectAll().sortedWith(
                compareBy<ResultRow> { entry -> Duration.between(now, entry[MasteryChestTable.lastBoxDate]) <= Duration.ZERO }
                    .thenBy { entry -> entry[MasteryChestTable.lastBoxDate] }
                    .thenBy { entry -> entry[MasteryChestTable.name] }
            )

            lst.addAll(elements)
        }

        return lst
    }
}