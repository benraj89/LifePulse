package com.vibecheck.lifepulse.ui.expenses

import com.vibecheck.lifepulse.R
import androidx.compose.ui.res.stringResource
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
    var localError by remember { mutableStateOf<Int?>(null) }
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
                Text(if (initial != null) stringResource(R.string.transaction_edit_format, type.displayLabel().lowercase()) else if (repayLoan != null) stringResource(R.string.repayment_record) else if (canChooseType) stringResource(R.string.transaction_add) else when (type) {
                    TransactionType.TRANSFER -> stringResource(R.string.transaction_transfer_money)
                    TransactionType.LEND -> stringResource(R.string.transaction_lend_money)
                    TransactionType.BORROW -> stringResource(R.string.transaction_borrow_money)
                    else -> stringResource(R.string.transaction_add)
                }, style = NeoTypography.headlineMedium, color = NeoColors.OnSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, enabled = !state.saving) { Icon(Icons.Default.Close, stringResource(R.string.transaction_close), modifier = Modifier.size(18.dp)) }
            }
            if (canChooseType || canChooseLoan) {
                val choices = if (canChooseLoan) listOf(TransactionType.LEND to stringResource(R.string.loan_lent), TransactionType.BORROW to stringResource(R.string.loan_borrowed))
                    else entryTypes.map { it to it.displayLabel() }
                NeoTextTabs(choices.map { it.first.name to it.second }, type.name, {
                    typeName = it; localError = null; manageCategories = false
                }, enabled = !state.saving)
            }
            if (type.isRepayment) {
                Text(stringResource(R.string.repayment_person_format, type.displayLabel(), person), style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
                if (repayLoan != null) Text(stringResource(R.string.repayment_outstanding_hint, Money.format(remainingMinor)),
                    color = NeoColors.OnSurface, style = NeoTypography.bodyMedium)
            }
            Text(stringResource(R.string.transaction_amount_currency, Money.currencyCode), style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
            NeoTextField(amountText, { if (!state.saving && it.matches(Regex("^\\d{0,12}(\\.\\d{0,2})?$"))) amountText = it },
                placeholder = stringResource(R.string.amount_placeholder), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                backgroundColor = NeoColors.PaleCyan, modifier = Modifier.fillMaxWidth(), textStyle = NeoTypography.headlineMedium)
            if (type.isLoan) {
                Text(if (type == TransactionType.LEND) stringResource(R.string.loan_lend_person) else stringResource(R.string.loan_borrow_person),
                    style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
                NeoTextField(person, { if (!state.saving) person = it }, placeholder = stringResource(R.string.loan_person_name),
                    modifier = Modifier.fillMaxWidth(), backgroundColor = NeoColors.MintGreen)
                Text(stringResource(R.string.loan_tracking_hint),
                    style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            }
            NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
                if (type.needsCategory) NeoInlineChoice(stringResource(R.string.add_expense_category_label),
                    availableCategories.map { it.id.toString() to it.name } + ("manage" to stringResource(R.string.category_manage_action)),
                    categoryId?.toString(), { if (it == "manage") manageCategories = true else categoryId = it.toLong() },
                    Modifier.testTag("category_choice"), enabled = !state.saving)
                NeoInlineChoice(if (type == TransactionType.TRANSFER) stringResource(R.string.transaction_from_account) else if (type.moneyIn) stringResource(R.string.transaction_receive_into) else stringResource(R.string.transaction_paid_with),
                    accounts.map { it.id.toString() to it.name }, accountId?.toString(), { accountId = it.toLong() },
                    Modifier.testTag("account_choice"), enabled = !state.saving && accounts.size > 1)
                if (type == TransactionType.TRANSFER) NeoInlineChoice(stringResource(R.string.transaction_to_account),
                    accounts.filter { it.id != accountId }.map { it.id.toString() to it.name }, destinationId?.toString(),
                    { destinationId = it.toLong() }, Modifier.testTag("destination_choice"), enabled = !state.saving)
                TextButton(onClick = { showDate = true }, enabled = !state.saving, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.add_expense_date_label), style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
                        Text((if (dateText == LocalDate.now().toString()) stringResource(R.string.expense_today_label) else LocalDate.parse(dateText).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))) + " ▾",
                            style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
                    }
                }
            }
            if (type.needsCategory && (manageCategories || availableCategories.isEmpty())) {
                CategoryDropdown(availableCategories, availableCategories.firstOrNull { it.id == categoryId },
                    onSelect = { if (!state.saving) categoryId = it.id },
                    onAddCategory = { name, color -> onAddCategory(name, color, type) }, onDeleteCategory = { onDeleteCategory(it.id) })
                TextButton(onClick = { manageCategories = false }) { Text(stringResource(R.string.category_done_managing), color = NeoColors.OnSurface) }
            }
            if (type == TransactionType.TRANSFER) {
                Text(stringResource(R.string.transaction_transfer_hint), style = NeoTypography.bodySmall, color = NeoColors.OnSurface)
                if (accounts.size < 2) Text(stringResource(R.string.transaction_second_account_hint), style = NeoTypography.bodyMedium, color = NeoColors.Danger)
            }
            NeoTextField(note, { if (!state.saving) note = it }, placeholder = stringResource(R.string.add_expense_note_label), singleLine = false,
                modifier = Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface)
            (localError?.let { stringResource(it) } ?: state.error)?.let { Text(it, color = NeoColors.Danger, style = NeoTypography.bodyMedium) }
            NeoButton(if (state.saving) stringResource(R.string.action_saving) else if (type.isRepayment) stringResource(R.string.repayment_save) else stringResource(R.string.transaction_save_format, type.displayLabel().lowercase()), {
                val date = LocalDate.parse(dateText)
                if (date.isAfter(LocalDate.now())) { localError = R.string.error_future_date; return@NeoButton }
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
                Text(stringResource(R.string.transaction_delete_format, type.displayLabel().lowercase()), color = NeoColors.Danger)
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
            Text(if (account == null) stringResource(R.string.account_add) else stringResource(R.string.account_edit), style = NeoTypography.headlineMedium, color = NeoColors.OnSurface)
            Text(stringResource(R.string.account_name_hint), style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            NeoTextField(name, { if (!state.saving) name = it }, placeholder = stringResource(R.string.account_name), modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.account_opening_currency, Money.currencyCode), style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
            NeoTextField(opening, { if (!state.saving && it.matches(Regex("^-?\\d{0,12}(\\.\\d{0,2})?$"))) opening = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.account_opening_hint),
                style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            state.error?.let { Text(it, color = NeoColors.Danger) }
            NeoButton(if (state.saving) stringResource(R.string.action_saving) else stringResource(R.string.account_save), { onSave(account?.id ?: 0, name, openingMinor!!) },
                Modifier.fillMaxWidth(), enabled = name.isNotBlank() && openingMinor != null && !state.saving,
                backgroundColor = NeoColors.MintGreen, contentColor = NeoColors.OnSurface)
        }
    }
}
