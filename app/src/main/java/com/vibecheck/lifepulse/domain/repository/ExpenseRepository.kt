package com.vibecheck.lifepulse.domain.repository

import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.CategorySpending
import com.vibecheck.lifepulse.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    fun observeTotalInRange(start: Long, end: Long): Flow<Double>

    fun observeExpensesInRange(start: Long, end: Long): Flow<List<Expense>>

    fun observeRecentExpenses(limit: Int = 50): Flow<List<Expense>>

    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategorySpending>>

    fun observeCategories(): Flow<List<Category>>

    suspend fun addCategory(name: String, colorHex: String): Long

    suspend fun deleteCategory(id: Long)

    /**
     * Idempotently ensures the built-in default categories exist and are flagged as
     * non-deletable. Safe to call on every app start: existing rows are left untouched
     * (besides re-flagging isDefault), missing ones are inserted.
     */
    suspend fun ensureDefaultCategories()

    suspend fun addExpense(categoryId: Long, amount: Double, note: String, timestamp: Long): Long

    suspend fun deleteExpense(id: Long)
}

