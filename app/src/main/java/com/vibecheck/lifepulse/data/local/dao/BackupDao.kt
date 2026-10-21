package com.vibecheck.lifepulse.data.local.dao

import androidx.room.*
import com.vibecheck.lifepulse.data.local.entity.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Keyset pages keep snapshots and restores bounded regardless of ledger size. */
@Dao
abstract class BackupDao {
    @Query("SELECT * FROM accounts WHERE id > :after ORDER BY id LIMIT :size")
    abstract suspend fun accounts(after: Long, size: Int): List<AccountEntity>

    @Query("SELECT * FROM categories WHERE id > :after ORDER BY id LIMIT :size")
    abstract suspend fun categories(after: Long, size: Int): List<CategoryEntity>

    @Query("SELECT * FROM expenses WHERE id > :after AND (loanId IS NOT NULL) = :repayments ORDER BY id LIMIT :size")
    abstract suspend fun transactions(after: Long, repayments: Boolean, size: Int): List<ExpenseEntity>

    @Query("SELECT DISTINCT person FROM expenses WHERE person != '' AND person > :after COLLATE BINARY ORDER BY person COLLATE BINARY LIMIT :size")
    abstract suspend fun contacts(after: String, size: Int): List<String>

    @Query("""SELECT e.id, e.dateTimestamp, e.type, e.amountMinor, COALESCE(c.name, '') AS category,
        a.name AS account, COALESCE(t.name, '') AS toAccount, e.person AS contact, e.note
        FROM expenses e JOIN accounts a ON a.id = e.accountId
        LEFT JOIN accounts t ON t.id = e.toAccountId LEFT JOIN categories c ON c.id = e.categoryId
        WHERE e.id > :after ORDER BY e.id LIMIT :size""")
    abstract suspend fun csvRows(after: Long, size: Int): List<CsvRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertAccounts(rows: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertCategories(rows: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertTransactions(rows: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE loanId IS NOT NULL")
    abstract suspend fun deleteRepayments()
    @Query("DELETE FROM expenses")
    abstract suspend fun deleteTransactions()
    @Query("DELETE FROM categories")
    abstract suspend fun deleteCategories()
    @Query("DELETE FROM accounts")
    abstract suspend fun deleteAccounts()

    @Query("""SELECT COUNT(*) FROM expenses e LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN expenses l ON l.id = e.loanId WHERE
        (e.type IN ('EXPENSE', 'INCOME') AND (c.id IS NULL OR c.kind != e.type)) OR
        (e.type = 'REPAYMENT_RECEIVED' AND (l.type != 'LEND' OR l.id IS NULL)) OR
        (e.type = 'REPAYMENT_PAID' AND (l.type != 'BORROW' OR l.id IS NULL)) OR
        (e.loanId IS NOT NULL AND e.dateTimestamp < l.dateTimestamp)""")
    abstract suspend fun invalidRelations(): Long

    // SQLite throws on integer SUM overflow; this also safely rejects malicious totals.
    @Query("""SELECT COUNT(*) FROM (SELECT l.id FROM expenses l JOIN expenses r ON r.loanId = l.id
        GROUP BY l.id HAVING SUM(r.amountMinor) > l.amountMinor)""")
    abstract suspend fun overpaidLoans(): Long

    @Transaction
    open suspend fun readSnapshot(consume: suspend (BackupDao) -> Unit) = consume(this)

    /** A failed insert, I/O error or coroutine cancellation rolls back every deletion. */
    @Transaction
    open suspend fun replaceFinance(source: BackupDao) {
        deleteRepayments()
        deleteTransactions()
        deleteCategories()
        deleteAccounts()
        source.forEachAccountPage { insertAccounts(it) }
        source.forEachCategoryPage { insertCategories(it) }
        source.forEachTransactionPage { insertTransactions(it) }
        currentCoroutineContext().ensureActive()
    }
}

data class CsvRow(
    val id: Long, val dateTimestamp: Long, val type: String, val amountMinor: Long,
    val category: String, val account: String, val toAccount: String, val contact: String, val note: String
)

const val BACKUP_PAGE_SIZE = 500

suspend fun BackupDao.forEachAccountPage(block: suspend (List<AccountEntity>) -> Unit) {
    var after = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val rows = accounts(after, BACKUP_PAGE_SIZE)
        if (rows.isEmpty()) return
        block(rows)
        after = rows.last().id
    }
}

suspend fun BackupDao.forEachCategoryPage(block: suspend (List<CategoryEntity>) -> Unit) {
    var after = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val rows = categories(after, BACKUP_PAGE_SIZE)
        if (rows.isEmpty()) return
        block(rows)
        after = rows.last().id
    }
}

suspend fun BackupDao.forEachTransactionPage(block: suspend (List<ExpenseEntity>) -> Unit) {
    // Loans always precede repayments, including when IDs were restored from another device.
    for (repayments in listOf(false, true)) {
        var after = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val rows = transactions(after, repayments, BACKUP_PAGE_SIZE)
            if (rows.isEmpty()) break
            block(rows)
            after = rows.last().id
        }
    }
}
