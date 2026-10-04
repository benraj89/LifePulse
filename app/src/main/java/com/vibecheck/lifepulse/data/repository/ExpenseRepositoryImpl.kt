package com.vibecheck.lifepulse.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.data.local.DefaultCategories
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.dao.AccountDao
import com.vibecheck.lifepulse.data.local.dao.CategoryDao
import com.vibecheck.lifepulse.data.local.dao.ExpenseDao
import com.vibecheck.lifepulse.data.local.entity.AccountEntity
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import com.vibecheck.lifepulse.data.local.entity.ExpenseEntity
import com.vibecheck.lifepulse.data.local.relation.ExpenseWithCategory
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.domain.model.CategorySpending
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.TransactionDraft
import com.vibecheck.lifepulse.domain.model.TransactionType
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val database: LifePulseDatabase,
    @ApplicationContext private val context: Context
) : ExpenseRepository {

    override fun observeTransactions(): Flow<List<Expense>> =
        expenseDao.observeAllTransactions().map { rows -> rows.map(ExpenseWithCategory::toDomain) }

    override fun observeAccounts(): Flow<List<Account>> = accountDao.observeAccounts().map { rows ->
        rows.map { Account(it.id, it.name, it.openingMinor) }
    }

    override suspend fun saveAccount(id: Long, name: String, openingMinor: Long): Long {
        require(name.isNotBlank()) { context.getString(R.string.error_account_name_required) }
        require(name.trim().length <= 50) { context.getString(R.string.error_account_name_length) }
        val entity = AccountEntity(id, name.trim(), openingMinor)
        return if (id == 0L) accountDao.insert(entity) else {
            require(accountDao.get(id) != null) { context.getString(R.string.error_account_missing) }
            accountDao.update(entity)
            id
        }
    }

    override suspend fun saveTransaction(draft: TransactionDraft): Long = database.withTransaction {
        require(draft.amountMinor in 1..100_000_000_000_000L) { context.getString(R.string.error_amount_invalid) }
        require(accountDao.get(draft.accountId) != null) { context.getString(R.string.error_account_required) }
        val existing = if (draft.id != 0L) expenseDao.get(draft.id) else null
        require(draft.id == 0L || existing != null) { context.getString(R.string.error_transaction_missing) }
        if (draft.type == TransactionType.TRANSFER) {
            require(draft.toAccountId != null && draft.toAccountId != draft.accountId &&
                accountDao.get(draft.toAccountId) != null) { context.getString(R.string.error_transfer_accounts) }
        }
        if (draft.type.needsCategory) {
            val category = draft.categoryId?.let { categoryDao.getCategory(it) }
            require(category != null && category.kind == draft.type.name &&
                (!category.isDeleted || existing?.categoryId == category.id)) { context.getString(R.string.error_category_invalid) }
        }
        if (draft.type.isLoan) require(draft.person.isNotBlank()) { context.getString(R.string.error_person_required) }
        if (existing != null && expenseDao.repaid(existing.id) > 0) {
            require(existing.type == draft.type.name && existing.accountId == draft.accountId) {
                context.getString(R.string.error_loan_type_account_locked)
            }
            require(draft.amountMinor >= expenseDao.repaid(existing.id)) { context.getString(R.string.error_loan_amount_below_repayments) }
            require(draft.dateTimestamp <= (expenseDao.firstRepaymentDate(existing.id) ?: Long.MAX_VALUE)) {
                context.getString(R.string.error_loan_date_after_repayment)
            }
        }
        if (draft.type.isRepayment) {
            val loan = draft.loanId?.let { expenseDao.get(it) }
            val expected = if (draft.type == TransactionType.REPAYMENT_RECEIVED) "LEND" else "BORROW"
            require(loan != null && loan.type == expected && loan.id != draft.id) { context.getString(R.string.error_repayment_loan_required) }
            require(draft.dateTimestamp >= loan.dateTimestamp) { context.getString(R.string.error_repayment_date_before_loan) }
            require(draft.amountMinor <= loan.amountMinor - expenseDao.repaid(loan.id, draft.id)) {
                context.getString(R.string.error_repayment_exceeds_outstanding)
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
        require(name.isNotBlank()) { context.getString(R.string.error_category_name_required) }
        require(kind.needsCategory) { context.getString(R.string.error_category_type) }
        val id = categoryDao.insertCategory(CategoryEntity(name = name.trim(), colorHex = colorHex, kind = kind.name))
        require(id != -1L) { context.getString(R.string.error_category_name_duplicate) }
        return id
    }

    override suspend fun deleteCategory(id: Long) {
        require(categoryDao.getCategory(id)?.isDefault == false) { context.getString(R.string.error_category_default_delete) }
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
        require(expenseDao.repaid(id) == 0L) { context.getString(R.string.error_loan_has_repayments) }
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

