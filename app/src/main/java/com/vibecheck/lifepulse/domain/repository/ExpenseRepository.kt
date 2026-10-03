package com.vibecheck.lifepulse.domain.repository

import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.CategorySpending
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.TransactionDraft
import com.vibecheck.lifepulse.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeTransactions(): Flow<List<Expense>>
    fun observeAccounts(): Flow<List<Account>>
    suspend fun saveTransaction(draft: TransactionDraft): Long
    suspend fun saveAccount(id: Long, name: String, openingMinor: Long): Long

    fun observeTotalInRange(start: Long, end: Long): Flow<Double>

    fun observeExpensesInRange(start: Long, end: Long): Flow<List<Expense>>

    fun observeRecentExpenses(limit: Int = 50): Flow<List<Expense>>

    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategorySpending>>

    fun observeCategories(): Flow<List<Category>>

    suspend fun addCategory(name: String, colorHex: String, kind: TransactionType = TransactionType.EXPENSE): Long

    suspend fun deleteCategory(id: Long)

    /**
     * Idempotently ensures the built-in default categories exist and are flagged as
     * non-deletable. Safe to call on every app start: existing rows are left untouched
     * (besides re-flagging isDefault), missing ones are inserted.
     */
    suspend fun ensureDefaultCategories()

    suspend fun deleteExpense(id: Long)
}

