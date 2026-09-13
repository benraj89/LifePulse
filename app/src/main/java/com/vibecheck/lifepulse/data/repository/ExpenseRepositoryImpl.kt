package com.vibecheck.lifepulse.data.repository

import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.ExpenseDao
import com.vibecheck.lifepulse.data.local.DefaultCategories
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.local.entity.ExpenseEntity
import com.vibecheck.lifepulse.data.local.relation.ExpenseWithCategory
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.CategorySpending
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : ExpenseRepository {

    override fun observeTotalInRange(start: Long, end: Long): Flow<Double> =
        expenseDao.observeTotalInRange(start, end)

    override fun observeExpensesInRange(start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.observeExpensesInRange(start, end).map { it.map(ExpenseWithCategory::toDomain) }

    override fun observeRecentExpenses(limit: Int): Flow<List<Expense>> =
        expenseDao.observeRecentExpenses(limit).map { it.map(ExpenseWithCategory::toDomain) }

    override fun observeTotalsByCategory(start: Long, end: Long): Flow<List<CategorySpending>> =
        expenseDao.observeTotalsByCategory(start, end).map { rows ->
            rows.map { CategorySpending(it.categoryId, it.categoryName, it.colorHex, it.total) }
        }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategories().map { list ->
            list.map { Category(it.id, it.name, it.colorHex, it.isDefault) }
        }

    override suspend fun addCategory(name: String, colorHex: String): Long =
        categoryDao.insertCategory(CategoryEntity(name = name.trim(), colorHex = colorHex))

    override suspend fun deleteCategory(id: Long) = categoryDao.deleteCategory(id)

    override suspend fun ensureDefaultCategories() {
        DefaultCategories.ALL.forEach { (name, color) ->
            categoryDao.insertCategory(CategoryEntity(name = name, colorHex = color, isDefault = true))
            categoryDao.markAsDefault(name)
        }
    }

    override suspend fun addExpense(
        categoryId: Long,
        amount: Double,
        note: String,
        timestamp: Long
    ): Long = expenseDao.insertExpense(
        ExpenseEntity(
            categoryId = categoryId,
            amount = amount,
            dateTimestamp = timestamp,
            note = note.trim()
        )
    )

    override suspend fun deleteExpense(id: Long) = expenseDao.deleteExpense(id)
}

private fun ExpenseWithCategory.toDomain() = Expense(
    id = expense.id,
    categoryId = expense.categoryId,
    categoryName = categoryName,
    categoryColorHex = categoryColorHex,
    amount = expense.amount,
    dateTimestamp = expense.dateTimestamp,
    note = expense.note
)

