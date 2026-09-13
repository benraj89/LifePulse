package com.vibecheck.lifepulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vibecheck.lifepulse.data.local.entity.ExpenseEntity
import com.vibecheck.lifepulse.data.local.relation.CategoryTotal
import com.vibecheck.lifepulse.data.local.relation.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /** Total spend inside an arbitrary range (e.g. start-of-today .. end-of-today). */
    @Query(
        """
        SELECT IFNULL(SUM(amount), 0.0) FROM expenses 
        WHERE dateTimestamp BETWEEN :start AND :end
        """
    )
    fun observeTotalInRange(start: Long, end: Long): Flow<Double>

    @Transaction
    @Query(
        """
        SELECT e.*, c.name AS categoryName, c.colorHex AS categoryColorHex
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.dateTimestamp BETWEEN :start AND :end
        ORDER BY e.dateTimestamp DESC
        """
    )
    fun observeExpensesInRange(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query(
        """
        SELECT e.*, c.name AS categoryName, c.colorHex AS categoryColorHex
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY e.dateTimestamp DESC
        LIMIT :limit
        """
    )
    fun observeRecentExpenses(limit: Int = 50): Flow<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, c.colorHex AS colorHex,
               SUM(e.amount) AS total
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.dateTimestamp BETWEEN :start AND :end
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)
}

