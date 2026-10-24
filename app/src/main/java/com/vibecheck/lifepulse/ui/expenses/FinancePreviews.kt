package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.domain.model.Account
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.domain.model.TransactionType
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.LocalDate
import java.time.YearMonth

@Preview(name = "Expenses • spending", widthDp = 360, heightDp = 800)
@Preview(name = "Expenses • large text", widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun SpendingPreview() {
    NeoBrutalismTheme {
        Surface(color = NeoColors.Background) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                spendingSection(ExpenseUiState(accounts = listOf(Account(1, "Everyday account", 0)),
                    spentMinor = 8959, totalToday = 1460, incomeMinor = 250000, isLoading = false),
                    EntryFilter.ALL, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(name = "Expenses • accounts", widthDp = 360, heightDp = 800)
@Composable
private fun AccountsPreview() {
    NeoBrutalismTheme { Surface(color = NeoColors.Background) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            accountsSection(ExpenseUiState(accounts = listOf(Account(1, "Everyday account", 0), Account(2, "Savings", 0)),
                balances = mapOf(1L to 291041L, 2L to 125000L), isLoading = false), {}, {}, {})
        }
    } }
}

@Preview(name = "Expenses • money owed", widthDp = 360, heightDp = 800)
@Composable
private fun OwedPreview() {
    val loan = Expense(1, null, "", "#FF7A5C", 5000, 0, "Dinner together", TransactionType.LEND, person = "Alex")
    NeoBrutalismTheme { Surface(color = NeoColors.Background) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            owedSection(ExpenseUiState(transactions = listOf(loan), outstandingLoans = listOf(loan), isLoading = false), false, {}, {}, {})
        }
    } }
}

@Preview(name = "Expenses • empty insights", widthDp = 360, heightDp = 800)
@Composable
private fun InsightsPreview() { NeoBrutalismTheme {
    ExpenseInsightsScreen(ExpenseUiState(isLoading = false), {}, {}, {})
} }

@Preview(name = "Currency", widthDp = 360, heightDp = 800)
@Composable
private fun CurrencyPreview() { NeoBrutalismTheme { Surface(color = NeoColors.Surface) {
    CurrencyPicker("USD", {}, Modifier.fillMaxSize())
} } }
