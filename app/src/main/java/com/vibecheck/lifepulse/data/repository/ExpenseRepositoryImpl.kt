package com.vibecheck.lifepulse.data.repository

import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.AccountDao
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.AccountEntity
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.TransactionDraft
import com.vibecheck.lifepulse.domain.model.TransactionType
import androidx.room.withTransaction
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
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val database: LifePulseDatabase
) : ExpenseRepository {

    override fun observeTransactions(): Flow<List<Expense>> =
        expenseDao.observeAllTransactions().map { rows -> rows.map(ExpenseWithCategory::toDomain) }

    override fun observeAccounts(): Flow<List<Account>> = accountDao.observeAccounts().map { rows ->
        rows.map { Account(it.id, it.name, it.openingMinor) }
    }

    override suspend fun saveAccount(id: Long, name: String, openingMinor: Long): Long {
        require(name.isNotBlank()) { "Enter an account name." }
        require(name.trim().length <= 50) { "Use an account name under 50 characters." }
        val entity = AccountEntity(id, name.trim(), openingMinor)
        return if (id == 0L) accountDao.insert(entity) else {
            require(accountDao.get(id) != null) { "This account no longer exists." }
            accountDao.update(entity)
            id
        }
    }

    override suspend fun saveTransaction(draft: TransactionDraft): Long = database.withTransaction {
        require(draft.amountMinor in 1..100_000_000_000_000L) { "Enter a valid amount greater than zero." }
        require(accountDao.get(draft.accountId) != null) { "Choose an account." }
        val existing = if (draft.id != 0L) expenseDao.get(draft.id) else null
        require(draft.id == 0L || existing != null) { "This transaction no longer exists." }
        if (draft.type == TransactionType.TRANSFER) {
            require(draft.toAccountId != null && draft.toAccountId != draft.accountId &&
                accountDao.get(draft.toAccountId) != null) { "Choose two different accounts." }
        }
        if (draft.type.needsCategory) {
            val category = draft.categoryId?.let { categoryDao.getCategory(it) }
            require(category != null && category.kind == draft.type.name &&
                (!category.isDeleted || existing?.categoryId == category.id)) { "Choose a valid category." }
        }
        if (draft.type.isLoan) require(draft.person.isNotBlank()) { "Enter the person's name." }
        if (existing != null && expenseDao.repaid(existing.id) > 0) {
            require(existing.type == draft.type.name && existing.accountId == draft.accountId) {
                "A loan with repayments must keep its type and original account."
            }
            require(draft.amountMinor >= expenseDao.repaid(existing.id)) { "Amount cannot be below repayments already recorded." }
            require(draft.dateTimestamp <= (expenseDao.firstRepaymentDate(existing.id) ?: Long.MAX_VALUE)) {
                "The loan date must be on or before its first repayment."
            }
        }
        if (draft.type.isRepayment) {
            val loan = draft.loanId?.let { expenseDao.get(it) }
            val expected = if (draft.type == TransactionType.REPAYMENT_RECEIVED) "LEND" else "BORROW"
            require(loan != null && loan.type == expected && loan.id != draft.id) { "Choose the original lending or borrowing record." }
            require(draft.dateTimestamp >= loan.dateTimestamp) { "Repayment date must be on or after the loan date." }
            require(draft.amountMinor <= loan.amountMinor - expenseDao.repaid(loan.id, draft.id)) {
                "Repayment is higher than the outstanding amount."
            }
        }
        val entity = ExpenseEntity(
            id = draft.id, categoryId = draft.categoryId.takeIf { draft.type.needsCategory },
            amountMinor = draft.amountMinor, dateTimestamp = draft.dateTimestamp, note = draft.note.trim(),
            type = draft.type.name, accountId = draft.accountId,
            toAccountId = draft.toAccountId.takeIf { draft.type == TransactionType.TRANSFER },
            person = draft.person.trim().takeIf { draft.type.isLoan || draft.type.isRepayment } ?: "",
            loanId = draft.loanId.takeIf { draft.type.isRepayment }
        )
        if (draft.id == 0L) expenseDao.insertExpense(entity) else {
            expenseDao.updateExpense(entity)
            draft.id
        }
    }

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
            list.map { Category(it.id, it.name, it.colorHex, it.isDefault, TransactionType.valueOf(it.kind)) }
        }

    override suspend fun addCategory(name: String, colorHex: String, kind: TransactionType): Long {
        require(name.isNotBlank()) { "Enter a category name." }
        require(kind.needsCategory) { "Categories are for income and expenses." }
        val id = categoryDao.insertCategory(CategoryEntity(name = name.trim(), colorHex = colorHex, kind = kind.name))
        require(id != -1L) { "That category name already exists. Choose a different name." }
        return id
    }

    override suspend fun deleteCategory(id: Long) {
        require(categoryDao.getCategory(id)?.isDefault == false) { "Built-in categories cannot be deleted." }
        categoryDao.deleteCategory(id)
    }

    override suspend fun ensureDefaultCategories() {
        DefaultCategories.ALL.forEach { (name, color) ->
            categoryDao.insertCategory(CategoryEntity(name = name, colorHex = color, isDefault = true))
            categoryDao.markAsDefault(name)
        }
        listOf("Salary", "Freelance", "Interest", "Gifts", "Other income").forEach { name ->
            categoryDao.insertCategory(CategoryEntity(name = name, colorHex = "#66BB6A", isDefault = true, kind = "INCOME"))
        }
    }

    override suspend fun deleteExpense(id: Long) = database.withTransaction {
        require(expenseDao.repaid(id) == 0L) { "Delete the linked repayments before deleting this loan." }
        expenseDao.deleteExpense(id)
    }
}

private fun ExpenseWithCategory.toDomain() = Expense(
    id = expense.id,
    categoryId = expense.categoryId,
    categoryName = categoryName,
    categoryColorHex = categoryColorHex,
    amountMinor = expense.amountMinor,
    dateTimestamp = expense.dateTimestamp,
    note = expense.note,
    type = TransactionType.valueOf(expense.type), accountId = expense.accountId,
    toAccountId = expense.toAccountId, person = expense.person, loanId = expense.loanId
)

