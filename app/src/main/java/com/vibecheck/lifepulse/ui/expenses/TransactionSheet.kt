package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.components.CategoryDropdown
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheet(
    accounts: List<Account>, categories: List<Category>, state: SaveState,
    initial: Expense? = null, repayLoan: Expense? = null, remainingMinor: Long = 0,
    onDismiss: () -> Unit, onSave: (TransactionDraft) -> Unit,
    onAddCategory: (String, String, TransactionType) -> Unit, onDeleteCategory: (Long) -> Unit,
    defaultAccountId: Long? = null, defaultType: TransactionType = TransactionType.EXPENSE,
    onDelete: (() -> Unit)? = null
) {
    val startingType = initial?.type ?: repayLoan?.let {
        if (it.type == TransactionType.LEND) TransactionType.REPAYMENT_RECEIVED else TransactionType.REPAYMENT_PAID
    } ?: defaultType
    var typeName by rememberSaveable { mutableStateOf(startingType.name) }
    val type = TransactionType.valueOf(typeName)
    val entryTypes = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
    val canChooseType = initial == null && repayLoan == null && startingType in entryTypes
    val canChooseLoan = initial == null && repayLoan == null && startingType.isLoan
    var amountText by rememberSaveable { mutableStateOf(
        initial?.let { Money.input(it.amountMinor) } ?: if (repayLoan != null) Money.input(remainingMinor) else ""
    ) }
    var accountId by rememberSaveable { mutableStateOf(initial?.accountId ?: repayLoan?.accountId ?: defaultAccountId ?: accounts.firstOrNull()?.id) }
    var destinationId by rememberSaveable { mutableStateOf(initial?.toAccountId) }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var person by rememberSaveable { mutableStateOf(initial?.person ?: repayLoan?.person ?: "") }
    var dateText by rememberSaveable { mutableStateOf(
        initial?.let { Instant.ofEpochMilli(it.dateTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            ?: LocalDate.now().toString()
    ) }
    var showDate by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var manageCategories by remember { mutableStateOf(false) }
    val availableCategories = categories.filter { it.kind == type }.let { list ->
        if (initial?.type == type && initial.categoryId != null && list.none { it.id == initial.categoryId }) {
            list + Category(initial.categoryId, initial.categoryName, initial.categoryColorHex, kind = type)
        } else list
    }
    LaunchedEffect(availableCategories, type) {
        if (availableCategories.none { it.id == categoryId }) categoryId =
            availableCategories.firstOrNull { it.name.equals("Other", ignoreCase = true) }?.id ?: availableCategories.firstOrNull()?.id
    }
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == accountId }) accountId = accounts.firstOrNull()?.id
    }
    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }
    val amount = Money.parse(amountText)
    val valid = amount != null && amount > 0 && accountId != null &&
        (!type.needsCategory || categoryId != null) && (!type.isLoan || person.isNotBlank()) &&
        (type != TransactionType.TRANSFER || (destinationId != null && destinationId != accountId))

    ModalBottomSheet(
        modifier = if (canChooseType || canChooseLoan) Modifier.fillMaxHeight(0.9f) else Modifier,
        onDismissRequest = { if (!state.saving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeoColors.PastelYellow
    ) {
        Column(Modifier.fillMaxWidth().testTag("transaction_editor").weight(1f, fill = false).verticalScroll(rememberScrollState())
            .navigationBarsPadding().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(if (initial != null) "Edit ${type.label.lowercase()}" else if (repayLoan != null) "Record repayment" else if (canChooseType) "Add entry" else when (type) {
                    TransactionType.TRANSFER -> "Transfer money"
                    TransactionType.LEND -> "Lend money"
                    TransactionType.BORROW -> "Borrow money"
                    else -> "Add entry"
                }, style = NeoTypography.headlineMedium, color = NeoColors.OnSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, enabled = !state.saving) { Icon(Icons.Default.Close, "Close entry", modifier = Modifier.size(18.dp)) }
            }
            if (canChooseType || canChooseLoan) {
                val choices = if (canChooseLoan) listOf(TransactionType.LEND to "Lent", TransactionType.BORROW to "Borrowed")
                    else entryTypes.map { it to it.label }
                NeoTextTabs(choices.map { it.first.name to it.second }, type.name, {
                    typeName = it; localError = null; manageCategories = false
                }, enabled = !state.saving)
            }
            if (type.isRepayment) {
                Text("${type.label} · $person", style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
                if (repayLoan != null) Text("Outstanding: ${Money.format(remainingMinor)}. You can record a partial repayment.",
                    color = NeoColors.OnSurface, style = NeoTypography.bodyMedium)
            }
            Text("Amount (${Money.currencyCode})", style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
            NeoTextField(amountText, { if (!state.saving && it.matches(Regex("^\\d{0,12}(\\.\\d{0,2})?$"))) amountText = it },
                placeholder = "0.00", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                backgroundColor = NeoColors.PaleCyan, modifier = Modifier.fillMaxWidth(), textStyle = NeoTypography.headlineMedium)
            if (type.isLoan) {
                Text(if (type == TransactionType.LEND) "Who are you lending to?" else "Who are you borrowing from?",
                    style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
                NeoTextField(person, { if (!state.saving) person = it }, placeholder = "Person's name",
                    modifier = Modifier.fillMaxWidth(), backgroundColor = NeoColors.MintGreen)
                Text("Track repayments in Money owed. This won't count as income or spending.",
                    style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            }
            NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
                if (type.needsCategory) NeoInlineChoice("Category",
                    availableCategories.map { it.id.toString() to it.name } + ("manage" to "Manage categories…"),
                    categoryId?.toString(), { if (it == "manage") manageCategories = true else categoryId = it.toLong() },
                    Modifier.testTag("category_choice"), enabled = !state.saving)
                NeoInlineChoice(if (type == TransactionType.TRANSFER) "From account" else if (type.moneyIn) "Receive into" else "Paid with",
                    accounts.map { it.id.toString() to it.name }, accountId?.toString(), { accountId = it.toLong() },
                    Modifier.testTag("account_choice"), enabled = !state.saving && accounts.size > 1)
                if (type == TransactionType.TRANSFER) NeoInlineChoice("To account",
                    accounts.filter { it.id != accountId }.map { it.id.toString() to it.name }, destinationId?.toString(),
                    { destinationId = it.toLong() }, Modifier.testTag("destination_choice"), enabled = !state.saving)
                TextButton(onClick = { showDate = true }, enabled = !state.saving, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date", style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
                        Text((if (dateText == LocalDate.now().toString()) "Today" else LocalDate.parse(dateText).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))) + " ▾",
                            style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
                    }
                }
            }
            if (type.needsCategory && (manageCategories || availableCategories.isEmpty())) {
                CategoryDropdown(availableCategories, availableCategories.firstOrNull { it.id == categoryId },
                    onSelect = { if (!state.saving) categoryId = it.id },
                    onAddCategory = { name, color -> onAddCategory(name, color, type) }, onDeleteCategory = { onDeleteCategory(it.id) })
                TextButton(onClick = { manageCategories = false }) { Text("Done managing", color = NeoColors.OnSurface) }
            }
            if (type == TransactionType.TRANSFER) {
                Text("Transfers don't count as spending or income.", style = NeoTypography.bodySmall, color = NeoColors.OnSurface)
                if (accounts.size < 2) Text("Add a second account in Accounts before recording a transfer.", style = NeoTypography.bodyMedium, color = NeoColors.Danger)
            }
            NeoTextField(note, { if (!state.saving) note = it }, placeholder = "Note (optional)", singleLine = false,
                modifier = Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface)
            (localError ?: state.error)?.let { Text(it, color = NeoColors.Danger, style = NeoTypography.bodyMedium) }
            NeoButton(if (state.saving) "Saving…" else if (type.isRepayment) "Save repayment" else "Save ${type.label.lowercase()}", {
                val date = LocalDate.parse(dateText)
                if (date.isAfter(LocalDate.now())) { localError = "Choose today or an earlier date."; return@NeoButton }
                val timestamp = if (initial != null && date == Instant.ofEpochMilli(initial.dateTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()) initial.dateTimestamp
                else date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onSave(TransactionDraft(
                    id = initial?.id ?: 0, type = type, amountMinor = amount!!, accountId = accountId!!,
                    toAccountId = if (type == TransactionType.TRANSFER) destinationId else null,
                    categoryId = if (type.needsCategory) categoryId else null, dateTimestamp = timestamp,
                    note = note, person = person, loanId = initial?.loanId ?: repayLoan?.id
                ))
            }, Modifier.fillMaxWidth(), enabled = valid && !state.saving,
                backgroundColor = NeoColors.Coral, contentColor = NeoColors.OnSurface)
            if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(), enabled = !state.saving) {
                Text("Delete ${type.label.lowercase()}", color = NeoColors.Danger)
            }
        }
    }
    if (showDate) NeoDatePickerDialog(LocalDate.parse(dateText), { dateText = it.toString(); localError = null }, { showDate = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(account: Account?, state: SaveState, onDismiss: () -> Unit, onSave: (Long, String, Long) -> Unit) {
    var name by rememberSaveable { mutableStateOf(account?.name ?: "") }
    var opening by rememberSaveable { mutableStateOf(account?.let { Money.input(it.openingMinor) } ?: "0.00") }
    val openingMinor = Money.parse(opening.removePrefix("-"))?.let { if (opening.startsWith("-")) -it else it }
    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }
    ModalBottomSheet(onDismissRequest = { if (!state.saving) onDismiss() }, containerColor = NeoColors.PaleCyan,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (account == null) "Add account" else "Edit account", style = NeoTypography.headlineMedium, color = NeoColors.OnSurface)
            Text("Use a name such as Cash, Bank, or Savings.", style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            NeoTextField(name, { if (!state.saving) name = it }, placeholder = "Account name", modifier = Modifier.fillMaxWidth())
            Text("Opening balance (${Money.currencyCode})", style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
            NeoTextField(opening, { if (!state.saving && it.matches(Regex("^-?\\d{0,12}(\\.\\d{0,2})?$"))) opening = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
            Text("Balance before the transactions you've recorded here. A negative balance is allowed.",
                style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            state.error?.let { Text(it, color = NeoColors.Danger) }
            NeoButton(if (state.saving) "Saving…" else "Save account", { onSave(account?.id ?: 0, name, openingMinor!!) },
                Modifier.fillMaxWidth(), enabled = name.isNotBlank() && openingMinor != null && !state.saving,
                backgroundColor = NeoColors.MintGreen, contentColor = NeoColors.OnSurface)
        }
    }
}
