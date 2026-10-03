package com.vibecheck.lifepulse.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.core.*
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class ExpenseDayGroup(val date: LocalDate, val expenses: List<Expense>, val total: Long)
data class CategoryBreakdown(val name: String, val colorHex: String, val totalMinor: Long)
data class SaveState(val saving: Boolean = false, val error: String? = null, val saved: Boolean = false)

data class ExpenseUiState(
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val transactions: List<Expense> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedAccountId: Long? = null,
    val expensesByDay: List<ExpenseDayGroup> = emptyList(),
    val totalToday: Long = 0,
    val spentMinor: Long = 0,
    val incomeMinor: Long = 0,
    val byCategory: List<CategoryBreakdown> = emptyList(),
    val outstandingLoans: List<Expense> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE)
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val selectedAccount = MutableStateFlow<Long?>(null)
    // Only finite accepted writes run here. They finish even if navigation destroys this ViewModel.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _editor = MutableStateFlow(SaveState())
    val editor = _editor.asStateFlow()
    private val _accountEditor = MutableStateFlow(SaveState())
    val accountEditor = _accountEditor.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val uiState: StateFlow<ExpenseUiState> = currentDateFlow().flatMapLatest { today ->
        combine(repository.observeTransactions(), repository.observeAccounts(), repository.observeCategories(),
            selectedMonth, selectedAccount) { transactions, accounts, categories, month, accountId ->
            val zone = ZoneId.systemDefault()
            fun date(transaction: Expense) = Instant.ofEpochMilli(transaction.dateTimestamp).atZone(zone).toLocalDate()
            val filtered = transactions.filter { accountId == null || it.accountId == accountId || it.toAccountId == accountId }
            val monthTransactions = filtered.filter { YearMonth.from(date(it)) == month }
            val expenses = monthTransactions.filter { it.type == TransactionType.EXPENSE }
            val currentTransactions = transactions.filter { !date(it).isAfter(today) }
            ExpenseUiState(
                categories = categories, accounts = accounts,
                balances = accounts.associate { it.id to Ledger.balance(it, currentTransactions) },
                transactions = transactions, selectedMonth = month, selectedAccountId = accountId,
                expensesByDay = monthTransactions.groupBy(::date).toSortedMap(compareByDescending { it })
                    .map { (day, list) -> ExpenseDayGroup(day, list, list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }) },
                totalToday = filtered.filter { date(it) == today && it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
                spentMinor = expenses.sumOf { it.amountMinor },
                incomeMinor = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                byCategory = expenses.groupBy { it.categoryId }.values.map { group ->
                    CategoryBreakdown(group.first().categoryName, group.first().categoryColorHex, group.sumOf { it.amountMinor })
                }.sortedByDescending { it.totalMinor },
                outstandingLoans = currentTransactions.filter {
                    it.type.isLoan && (accountId == null || it.accountId == accountId) && Ledger.outstanding(it, currentTransactions) > 0
                },
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    fun selectMonth(month: YearMonth) { selectedMonth.value = month }
    fun selectAccount(id: Long?) { selectedAccount.value = id }
    fun resetEditor() { _editor.value = SaveState() }
    fun resetAccountEditor() { _accountEditor.value = SaveState() }
    fun clearMessage() { _message.value = null }

    fun saveTransaction(draft: TransactionDraft) {
        if (_editor.value.saving) return
        _editor.value = SaveState(saving = true)
        writeScope.launch {
            try {
                repository.saveTransaction(draft)
                // Keep the saved entry visible even when it belongs to another month/account.
                if (draft.type.needsCategory || draft.type == TransactionType.TRANSFER) {
                    selectedMonth.value = YearMonth.from(Instant.ofEpochMilli(draft.dateTimestamp).atZone(ZoneId.systemDefault()))
                    if (selectedAccount.value != null && selectedAccount.value != draft.accountId && selectedAccount.value != draft.toAccountId)
                        selectedAccount.value = draft.accountId
                }
                _editor.value = SaveState(saved = true)
                _message.value = if (draft.id == 0L) "Transaction saved" else "Transaction updated"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _editor.value = SaveState(error = e.message ?: "Couldn't save. Please try again.") }
        }
    }

    fun saveAccount(id: Long, name: String, openingMinor: Long) {
        if (_accountEditor.value.saving) return
        _accountEditor.value = SaveState(saving = true)
        writeScope.launch {
            try {
                repository.saveAccount(id, name, openingMinor)
                _accountEditor.value = SaveState(saved = true)
                _message.value = "Account saved"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _accountEditor.value = SaveState(error = e.message ?: "Couldn't save account.") }
        }
    }

    fun addCategory(name: String, color: String, kind: TransactionType) = writeScope.launch {
        try { repository.addCategory(name, color, kind); _editor.value = _editor.value.copy(error = null)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            _editor.value = _editor.value.copy(error = e.message ?: "Couldn't add category.")
        }
    }
    fun deleteCategory(id: Long) = writeScope.launch {
        try { repository.deleteCategory(id)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _message.value = e.message ?: "Couldn't delete category." }
    }
    fun deleteExpense(id: Long) = writeScope.launch {
        try { repository.deleteExpense(id); _message.value = "Transaction deleted"
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { _message.value = e.message ?: "Couldn't delete transaction." }
    }
    fun setCurrency(code: String) {
        if (code !in Money.supportedCurrencies) return
        Money.currencyCode = code
        prefs.edit().putString("currency", code).apply()
        _message.value = "Currency set to $code. Existing amounts are not converted."
    }
}
