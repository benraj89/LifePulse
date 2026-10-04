package com.vibecheck.lifepulse

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.*
import com.vibecheck.lifepulse.data.repository.ExpenseRepositoryImpl
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.expenses.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: LifePulseDatabase
    private lateinit var repository: ExpenseRepositoryImpl
    private lateinit var viewModel: ExpenseViewModel
    private val store = ViewModelStore()
    private var expenseId = 0L

    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build()
        repository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao(), database.accountDao(), database, context)
        database.accountDao().insert(AccountEntity(1, "Cash", 10000))
        database.accountDao().insert(AccountEntity(2, "Bank", 0))
        val category = database.categoryDao().insertCategory(CategoryEntity(name = "Food", colorHex = "#FF7043"))
        database.categoryDao().insertCategory(CategoryEntity(name = "Salary", colorHex = "#00CCA0", kind = TransactionType.INCOME.name))
        expenseId = repository.saveTransaction(TransactionDraft(amountMinor = 1234, accountId = 1, categoryId = category, note = "Lunch"))
        viewModel = ExpenseViewModel(repository, context)
        store.put("finance", viewModel)
        compose.setContent { NeoBrutalismTheme { ExpenseScreen(viewModel = viewModel) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Your entries").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun cleanup() { store.clear(); database.close() }

    @Test fun addAccountThroughForm() {
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithContentDescription("Add account").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Savings")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("50.00")
        compose.onAllNodes(hasSetTextAction())[1].performImeAction()
        compose.waitForIdle()
        compose.onNodeWithText("Save account").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeAccounts().first().any { it.name == "Savings" && it.openingMinor == 5000L } } }
        compose.onNodeWithText("Account saved").assertExists()
    }

    @Test fun transferUsesTwoAccountsAndSavesExactAmount() {
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onNode(hasText("Transfer") and hasAnyAncestor(hasTestTag("transaction_editor"))).performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("10.25")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.waitForIdle()
        compose.onNodeWithTag("destination_choice").performScrollTo().performClick()
        compose.onNode(hasText("Bank") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Save transfer").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.TRANSFER && it.amountMinor == 1025L && it.accountId == 1L && it.toAccountId == 2L
        } } }
        compose.onNodeWithText("Transaction saved").assertExists()
    }

    @Test fun tapTransactionAndEditAmount() {
        compose.onNodeWithTag("transaction_$expenseId").performScrollTo().performClick()
        compose.onNodeWithText("Edit expense").assertExists()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("20.00")
        compose.onNodeWithText("Save expense").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().single().amountMinor == 2000L } }
        compose.onNodeWithText("Transaction updated").assertExists()
    }

    @Test fun partialRepaymentThroughLoanSection() {
        val loanId = runBlocking { repository.saveTransaction(TransactionDraft(type = TransactionType.LEND,
            amountMinor = 10000, accountId = 1, person = "Alex", dateTimestamp = 1000)) }
        compose.onNodeWithText("Money owed").performClick()
        compose.onNodeWithText("Record repayment").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("25.00")
        compose.onNodeWithText("Save repayment").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.loanId == loanId && it.type == TransactionType.REPAYMENT_RECEIVED && it.amountMinor == 2500L
        } } }
        compose.onNodeWithText("Transaction saved").assertExists()
    }

    @Test fun addExpenseWithVisibleCategoryAndAccount() {
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onNodeWithText("Paid with").assertExists()
        compose.onNodeWithText("Note (optional)").assertExists()
        compose.onNode(hasText("Food", substring = true) and hasAnyAncestor(hasTestTag("category_choice"))).assertExists()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("8.50")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.onNodeWithText("Save expense").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.EXPENSE && it.amountMinor == 850L && it.accountId == 1L
        } } }
        compose.onNodeWithText("Transaction saved").assertExists()
    }

    @Test fun compactAccountFieldSavesIntoSelectedAccount() {
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("6.25")
        compose.onNodeWithTag("account_choice").performScrollTo().performClick()
        compose.onNode(hasText("Bank") and hasAnyAncestor(isPopup())).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Bank", substring = true) and hasAnyAncestor(hasTestTag("account_choice"))).assertExists()
        compose.onNodeWithText("Save expense").assertIsEnabled().performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.EXPENSE && it.amountMinor == 625L && it.accountId == 2L
        } } }
    }

    @Test fun incomeIsSeparateFromSpendingAndCanBeFiltered() {
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onNode(hasText("Income") and hasAnyAncestor(hasTestTag("transaction_editor"))).performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("50.00")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.onNodeWithText("Save income").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.INCOME && it.amountMinor == 5000L
        } } }
        chooseEntryFilter("Income")
        compose.onNodeWithTag("transaction_$expenseId").assertDoesNotExist()
        compose.onNodeWithText("Salary").assertExists()
        chooseEntryFilter("Expenses")
        compose.onNodeWithTag("transaction_$expenseId").assertExists()
        compose.onNodeWithText("Salary").assertDoesNotExist()
    }

    @Test fun deleteEntryRequiresConfirmation() {
        compose.onNodeWithTag("transaction_$expenseId").performScrollTo().performClick()
        compose.onNodeWithText("Delete expense").performScrollTo().performClick()
        compose.onNodeWithText("Delete entry?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        Assert.assertEquals(1, runBlocking { repository.observeTransactions().first().size })
        compose.onNodeWithTag("transaction_$expenseId").performScrollTo().performClick()
        compose.onNodeWithText("Delete expense").performScrollTo().performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().isEmpty() } }
    }

    @Test fun savingReturnsToTheEntryMonthAndClearsAnIncompatibleFilter() {
        val previousMonth = java.time.YearMonth.now().minusMonths(1)
        compose.runOnIdle { viewModel.selectMonth(previousMonth) }
        compose.waitUntil(5000) { viewModel.uiState.value.selectedMonth == previousMonth }
        chooseEntryFilter("Income")
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("9.75")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.onNodeWithText("Save expense").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any { it.amountMinor == 975L } } }
        val savedId = runBlocking { repository.observeTransactions().first().single { it.amountMinor == 975L }.id }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("transaction_$savedId").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("transaction_$savedId").assertExists()
    }

    @Test fun switchingEntryTypesKeepsAmountAndAccountAndUsesTheRightCategory() {
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onNodeWithTag("account_choice").performScrollTo().performClick()
        compose.onNode(hasText("Bank") and hasAnyAncestor(isPopup())).performClick()
        compose.onNode(hasText("Bank", substring = true) and hasAnyAncestor(hasTestTag("account_choice"))).assertExists()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("15.75")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.waitForIdle()
        compose.onNode(hasText("Transfer") and hasAnyAncestor(hasTestTag("transaction_editor"))).performScrollTo().performClick()
        compose.onNodeWithTag("destination_choice").performScrollTo().performClick()
        compose.onNode(hasText("Cash") and hasAnyAncestor(isPopup())).performClick()
        compose.onNode(hasText("Income") and hasAnyAncestor(hasTestTag("transaction_editor"))).performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("15.75")
        compose.onNodeWithText("Save income").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.INCOME && it.amountMinor == 1575L && it.accountId == 2L &&
                it.categoryName == "Salary" && it.toAccountId == null
        } } }
    }

    private fun chooseEntryFilter(label: String) {
        compose.onNodeWithText("Filter").performClick()
        compose.onNode(hasText(label, substring = true) and hasAnyAncestor(isPopup())).performClick()
    }

    @Test fun addingBorrowingUsesTheSameAddActionAndLoanSheet() {
        compose.onNodeWithText("Money owed").performClick()
        compose.onNodeWithContentDescription("Add entry").performClick()
        compose.onNode(hasText("Borrowed") and hasAnyAncestor(hasTestTag("transaction_editor"))).performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("25.00")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Casey")
        compose.onAllNodes(hasSetTextAction())[1].performImeAction()
        compose.onNodeWithText("Save borrow").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repository.observeTransactions().first().any {
            it.type == TransactionType.BORROW && it.amountMinor == 2500L && it.person == "Casey"
        } } }
    }

    @Test fun singleFilterMenuCanFilterByAccountAndClear() {
        chooseEntryFilter("Bank")
        compose.waitUntil(5000) { viewModel.uiState.value.selectedAccountId == 2L }
        compose.onNodeWithTag("transaction_$expenseId").assertDoesNotExist()
        compose.onNodeWithText("Clear").performClick()
        compose.waitUntil(5000) { viewModel.uiState.value.selectedAccountId == null }
        compose.onNodeWithTag("transaction_$expenseId").assertExists()
    }
}
