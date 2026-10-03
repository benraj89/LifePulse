package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.vibecheck.lifepulse.core.*
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.components.toComposeColor
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.format.DateTimeFormatter

internal fun LazyListScope.spendingSection(
    state: ExpenseUiState, filter: String, onFilter: (String) -> Unit,
    onCalendar: () -> Unit, onBreakdown: () -> Unit,
    onAccount: (Long?) -> Unit, onEdit: (Expense) -> Unit
) {
    item { SpendingSummary(state, onCalendar, onBreakdown) }
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your entries", style = NeoTypography.titleLarge)
            EntryFilterMenu(state, filter, onFilter, onAccount)
        }
        val accountName = state.accounts.firstOrNull { it.id == state.selectedAccountId }?.name
        if (filter != "All" || accountName != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(if (filter != "All") filter else null, accountName).joinToString(" · "), modifier = Modifier.weight(1f), style = NeoTypography.bodySmall)
                TextButton(onClick = { onFilter("All"); onAccount(null) }) { Text("Clear", color = NeoColors.OnSurface) }
            }
        } else Text("Tap an entry to edit", style = NeoTypography.bodySmall)
    }
    val groups = state.expensesByDay.map { group -> group.copy(expenses = group.expenses.filter {
        (it.type.needsCategory || it.type == TransactionType.TRANSFER) &&
            (filter == "All" || (filter == "Expenses" && it.type == TransactionType.EXPENSE) ||
            (filter == "Income" && it.type == TransactionType.INCOME) || (filter == "Transfers" && it.type == TransactionType.TRANSFER))
    }) }.filter { it.expenses.isNotEmpty() }
    if (state.isLoading) item { Text("Loading your entries…") }
    else if (groups.isEmpty()) item { FinanceEmpty("A fresh start", "No entries here yet. Tap Add entry to get started.") }
    groups.forEach { group ->
        item(key = "date_${group.date}") { Text(group.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")), style = NeoTypography.labelLarge) }
        items(group.expenses, key = { it.id }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }) }
    }
}

@Composable
private fun SpendingSummary(state: ExpenseUiState, onCalendar: () -> Unit, onBreakdown: () -> Unit) {
    FinanceSummaryCard("MONTHLY SPENDING", NeoColors.Yellow, NeoColors.Lime) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { SummaryAmount(state.spentMinor) }
                Box(Modifier.background(NeoColors.Cyan).border(2.dp, NeoColors.Border).clickable(onClick = onCalendar)
                    .heightIn(min = 48.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(state.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")) + " ▾",
                        style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
                }
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                SummaryStat("SPENT TODAY", state.totalToday, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Surface))
                Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
                SummaryStat("MONTH'S INCOME", state.incomeMinor, Modifier.weight(1f).fillMaxHeight().background(NeoColors.MintGreen))
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Row(Modifier.fillMaxWidth().background(NeoColors.Surface).clickable(onClick = onBreakdown)
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("See category breakdown", style = NeoTypography.labelLarge, fontWeight = FontWeight.Black, color = NeoColors.OnSurface,
                    modifier = Modifier.weight(1f))
                Text("→", style = NeoTypography.titleLarge, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
            }
    }
}

@Composable
private fun SummaryStat(label: String, amount: Long, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
        Text(Money.format(amount), style = NeoTypography.titleMedium, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
    }
}

internal fun LazyListScope.accountsSection(state: ExpenseUiState, onAccount: (Account?) -> Unit,
                                          onCurrency: () -> Unit, onEdit: (Expense) -> Unit) {
    item {
        FinanceSummaryCard("YOUR TOTAL BALANCE", NeoColors.MintGreen, NeoColors.Cyan) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                SummaryAmount(state.balances.values.sum())
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Text("Based on the entries you've recorded", style = NeoTypography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(NeoColors.Surface).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your accounts", style = NeoTypography.titleLarge)
            NeoIconButton(Icons.Default.Add, "Add account", { onAccount(null) }, size = 40.dp, shadowOffset = 3.dp)
        }
    }
    items(state.accounts, key = { "account_${it.id}" }) { account ->
        NeoCard(Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 3.dp).clickable { onAccount(account) },
            backgroundColor = NeoColors.Surface, shape = RoundedCornerShape(4.dp), borderWidth = 3.dp,
            shadowOffsetX = 6.dp, shadowOffsetY = 6.dp, contentPadding = 3.dp) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(10.dp).fillMaxHeight().background(NeoColors.Cyan))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(account.name, style = NeoTypography.titleMedium, fontWeight = FontWeight.Black)
                    Text(Money.format(state.balances[account.id] ?: account.openingMinor), style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Default.Edit, "Edit account", modifier = Modifier.padding(end = 16.dp).size(16.dp), tint = NeoColors.OnSurface)
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Currency · ${Money.currencyCode}", style = NeoTypography.bodyMedium)
            TextButton(onClick = onCurrency) { Text("Change", color = NeoColors.OnSurface) }
        }
        Text("Transfers", style = NeoTypography.titleLarge)
    }
    val transfers = state.transactions.filter { it.type == TransactionType.TRANSFER }.sortedByDescending { it.dateTimestamp }
    if (transfers.isEmpty()) item { Text("Transfers will appear here. They don't count as spending.", style = NeoTypography.bodyMedium) }
    items(transfers, key = { "transfer_${it.id}" }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }, showDate = true) }
}

internal fun LazyListScope.owedSection(state: ExpenseUiState, showSettled: Boolean, onSettled: (Boolean) -> Unit,
                                      onEdit: (Expense) -> Unit, onRepay: (Expense) -> Unit) {
    item {
        FinanceSummaryCard("MONEY OWED", NeoColors.Cyan, NeoColors.Yellow) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                SummaryStat("PEOPLE OWE YOU", state.outstandingLoans.filter { it.type == TransactionType.LEND }
                    .sumOf { Ledger.outstanding(it, state.transactions) }, Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
                SummaryStat("YOU OWE PEOPLE", state.outstandingLoans.filter { it.type == TransactionType.BORROW }
                    .sumOf { Ledger.outstanding(it, state.transactions) }, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Yellow))
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show repaid loans", style = NeoTypography.bodyMedium)
            Checkbox(showSettled, onSettled, colors = CheckboxDefaults.colors(checkedColor = NeoColors.OnSurface, checkmarkColor = NeoColors.Surface, uncheckedColor = NeoColors.Border))
        }
    }
    val loans = (if (showSettled) state.transactions.filter { it.type.isLoan } else state.outstandingLoans).sortedByDescending { it.dateTimestamp }
    if (loans.isEmpty()) item { FinanceEmpty("All clear", "Record money you lend or borrow, then track repayments here.") }
    items(loans, key = { "loan_${it.id}" }) { loan ->
        val remaining = Ledger.outstanding(loan, state.transactions)
        var showRepayments by remember { mutableStateOf(false) }
        NeoColumnCard(Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 3.dp), backgroundColor = NeoColors.Surface,
            shape = RoundedCornerShape(4.dp), contentPadding = 10.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(loan.person, style = NeoTypography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(if (remaining == 0L) "REPAID" else if (loan.type == TransactionType.LEND) "LENT" else "BORROWED",
                    style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(if (remaining == 0L) NeoColors.MintGreen else if (loan.type == TransactionType.LEND) NeoColors.Cyan else NeoColors.Yellow)
                        .border(2.dp, NeoColors.Border).padding(horizontal = 8.dp, vertical = 6.dp))
            }
            Text(if (remaining == 0L) "Fully repaid" else if (loan.type == TransactionType.LEND) "Owes you ${Money.format(remaining)}" else "You owe ${Money.format(remaining)}", style = NeoTypography.titleMedium)
            Text("${Money.format(loan.amountMinor)} ${if (loan.type == TransactionType.LEND) "lent" else "borrowed"} · ${DateUtils.formatTimestamp(loan.dateTimestamp, "dd MMM yyyy")}", style = NeoTypography.bodySmall)
            if (loan.note.isNotBlank()) Text(loan.note, style = NeoTypography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEdit(loan) }) { Text("Edit", color = NeoColors.OnSurface) }
                if (remaining > 0) TextButton(onClick = { onRepay(loan) }) { Text("Record repayment", color = NeoColors.OnSurface) }
            }
            val repayments = state.transactions.filter { it.loanId == loan.id }.sortedByDescending { it.dateTimestamp }
            if (repayments.isNotEmpty()) TextButton(onClick = { showRepayments = !showRepayments }) {
                Text(if (showRepayments) "Hide repayments" else "Repayments (${repayments.size})", color = NeoColors.OnSurface)
            }
            if (showRepayments) repayments.forEach { payment ->
                TextButton(onClick = { onEdit(payment) }) {
                    Text("${Money.format(payment.amountMinor)} repaid · ${DateUtils.formatTimestamp(payment.dateTimestamp, "dd MMM")} · Edit", color = NeoColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun EntryFilterMenu(state: ExpenseUiState, filter: String, onFilter: (String) -> Unit, onAccount: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val active = filter != "All" || state.selectedAccountId != null
    val shape = RoundedCornerShape(4.dp)
    Box {
        NeoButton("Filter", { expanded = true }, backgroundColor = if (active) NeoColors.Cyan else NeoColors.Surface,
            contentColor = NeoColors.OnSurface, shape = shape, shadowOffset = 3.dp,
            horizontalPadding = 12.dp, verticalPadding = 8.dp,
            leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) })
        DropdownMenu(expanded, { expanded = false }, shape = shape, containerColor = NeoColors.Surface,
            tonalElevation = 0.dp, shadowElevation = 0.dp,
            modifier = Modifier.widthIn(min = 224.dp, max = 300.dp)
                .neoHardShadow(shape = shape, offsetX = 5.dp, offsetY = 5.dp)
                .background(NeoColors.Surface, shape)
                .border(3.dp, NeoColors.Border, shape)) {
            Text("ENTRY TYPE", style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            listOf("All", "Expenses", "Income", "Transfers").forEach { option ->
                FilterMenuOption(if (option == "All") "All entries" else option, filter == option,
                    { onFilter(option); expanded = false })
            }
            if (state.accounts.size > 1) {
                HorizontalDivider(thickness = 3.dp, color = NeoColors.Border, modifier = Modifier.padding(vertical = 8.dp))
                Text("ACCOUNT", style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                FilterMenuOption("All accounts", state.selectedAccountId == null, { onAccount(null); expanded = false })
                state.accounts.forEach { account -> FilterMenuOption(account.name, state.selectedAccountId == account.id,
                    { onAccount(account.id); expanded = false }) }
            }
        }
    }
}

@Composable
private fun FilterMenuOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = NeoTypography.bodyMedium, color = NeoColors.OnSurface,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium) },
        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, tint = NeoColors.OnSurface, modifier = Modifier.size(16.dp))
            else Spacer(Modifier.size(16.dp)) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            .background(if (isSelected) NeoColors.Cyan else NeoColors.Surface)
            .then(if (isSelected) Modifier.border(2.dp, NeoColors.Border) else Modifier)
            .semantics { selected = isSelected }
    )
}

@Composable
private fun FinanceSummaryCard(title: String, background: Color, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    NeoCard(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), backgroundColor = background,
        shape = RoundedCornerShape(4.dp), borderWidth = 3.dp, shadowOffsetX = 6.dp, shadowOffsetY = 6.dp, contentPadding = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().background(NeoColors.Ink).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                    color = NeoColors.White, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { Box(Modifier.size(width = 4.dp, height = 16.dp).rotate(20f).background(accent)) }
                }
            }
            content()
        }
    }
}

@Composable
private fun SummaryAmount(value: Long) {
    val amount = Money.format(value)
    Text(amount, style = NeoTypography.headlineMedium, fontSize = if (amount.length <= 12) 28.sp else 22.sp,
        lineHeight = if (amount.length <= 12) 34.sp else 28.sp, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
}

@Composable
private fun FinanceEmpty(title: String, hint: String) {
    NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
        Text(title, style = NeoTypography.titleMedium)
        Text(hint, style = NeoTypography.bodyMedium)
    }
}

@Composable
internal fun CategorySpendingRow(category: CategoryBreakdown, total: Long) {
    val fraction = if (total > 0) (category.totalMinor.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, style = NeoTypography.titleMedium, modifier = Modifier.weight(1f))
            Text("${Money.format(category.totalMinor)} · ${(fraction * 100).toInt()}%", style = NeoTypography.bodyMedium)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(NeoColors.Concrete).border(2.dp, NeoColors.Border)) {
            if (fraction > 0) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(category.colorHex.toComposeColor()))
        }
    }
}

@Composable
private fun FinanceEntryRow(entry: Expense, accounts: List<Account>, onEdit: () -> Unit, showDate: Boolean = false) {
    val account = accounts.firstOrNull { it.id == entry.accountId }?.name ?: "Account"
    val title = if (entry.type == TransactionType.TRANSFER) "$account → ${accounts.firstOrNull { it.id == entry.toAccountId }?.name ?: "Account"}" else entry.categoryName
    NeoColumnCard(Modifier.fillMaxWidth().testTag("transaction_${entry.id}").clickable(onClick = onEdit), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorDot(entry.categoryColorHex, size = 12)
            Column(Modifier.weight(1f)) {
                Text(title, style = NeoTypography.titleMedium)
                Text(if (showDate) DateUtils.formatTimestamp(entry.dateTimestamp, "dd MMM yyyy") else account, style = NeoTypography.bodySmall)
                if (entry.note.isNotBlank()) Text(entry.note, style = NeoTypography.bodyMedium)
            }
            Text((if (entry.type == TransactionType.INCOME) "+" else if (entry.type == TransactionType.EXPENSE) "−" else "") + Money.format(entry.amountMinor),
                style = NeoTypography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}
