package com.vibecheck.lifepulse.ui.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.core.*
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.neobrutalism.*

private enum class MoneyPage(val label: String) { SPENDING("Spending"), ACCOUNTS("Accounts"), OWED("Money owed") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(modifier: Modifier = Modifier, viewModel: ExpenseViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val accountEditor by viewModel.accountEditor.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var pageName by rememberSaveable { mutableStateOf(MoneyPage.SPENDING.name) }
    val page = MoneyPage.valueOf(pageName)
    var activityFilter by rememberSaveable { mutableStateOf("All") }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var repaying by remember { mutableStateOf<Expense?>(null) }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showTransaction by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showAccount by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showInsights by rememberSaveable { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSettled by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var currencyTarget by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    LaunchedEffect(editor.saved) { if (editor.saved) activityFilter = "All" }
    fun selectPage(next: MoneyPage) { pageName = next.name; viewModel.selectAccount(null) }
    BackHandler(page != MoneyPage.SPENDING) { selectPage(MoneyPage.SPENDING) }
    fun openTransaction(type: TransactionType, transaction: Expense? = null, loan: Expense? = null) {
        newType = type; editing = transaction; repaying = loan
        viewModel.resetEditor(); showTransaction = true
    }
    fun openAccount(account: Account? = null) {
        editingAccount = account; viewModel.resetAccountEditor(); showAccount = true
    }

    if (showInsights) {
        ExpenseInsightsScreen(state, viewModel::selectMonth, viewModel::selectAccount, { showInsights = false })
        return
    }

    Scaffold(modifier, containerColor = NeoColors.Background, contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Your money", style = NeoTypography.headlineMedium) },
            actions = { NeoButton("Add entry", { openTransaction(if (page == MoneyPage.OWED) TransactionType.LEND else TransactionType.EXPENSE) },
                Modifier.padding(end = 16.dp), backgroundColor = NeoColors.Coral, contentColor = NeoColors.OnSurface,
                horizontalPadding = 12.dp, verticalPadding = 8.dp, shadowOffset = 3.dp,
                leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }) },
            windowInsets = WindowInsets(0), colors = TopAppBarDefaults.topAppBarColors(containerColor = NeoColors.Background)) },
        snackbarHost = { SnackbarHost(snackbar) { data ->
            NeoColumnCard(Modifier.padding(16.dp).fillMaxWidth(), backgroundColor = NeoColors.PastelYellow) {
                Text(data.visuals.message, style = NeoTypography.bodyMedium)
            }
        } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NeoTextTabs(MoneyPage.entries.map { it.name to it.label }, page.name,
                { selectPage(MoneyPage.valueOf(it)) }, Modifier.padding(horizontal = 16.dp))
            key(page) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (page) {
                        MoneyPage.SPENDING -> spendingSection(state, activityFilter, { activityFilter = it },
                            { showCalendar = true }, { showInsights = true },
                            viewModel::selectAccount, { openTransaction(it.type, transaction = it) })
                        MoneyPage.ACCOUNTS -> accountsSection(state, { openAccount(it) },
                            { showSettings = true },
                            { openTransaction(it.type, transaction = it) })
                        MoneyPage.OWED -> owedSection(state, showSettled, { showSettled = it },
                            { openTransaction(it.type, transaction = it) },
                            { openTransaction(it.type, loan = it) })
                    }
                }
            }
        }
    }
    if (showTransaction) key(editing?.id, repaying?.id, newType) {
        TransactionSheet(state.accounts, state.categories, editor, editing, repaying,
            repaying?.let { Ledger.outstanding(it, state.transactions) } ?: 0,
            onDismiss = { showTransaction = false }, onSave = viewModel::saveTransaction,
            onAddCategory = { name, color, type -> viewModel.addCategory(name, color, type) }, onDeleteCategory = { viewModel.deleteCategory(it) },
            defaultAccountId = state.selectedAccountId, defaultType = newType,
            onDelete = editing?.let { target -> { showTransaction = false; deleteTarget = target } })
    }
    if (showAccount) key(editingAccount?.id) { AccountSheet(editingAccount, accountEditor, { showAccount = false }, viewModel::saveAccount) }
    if (showCalendar) NeoCalendarDialog(state.selectedMonth, viewModel::selectMonth, { showCalendar = false })
    if (showSettings) ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = NeoColors.PaleCyan) {
        Column(Modifier.navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Currency", style = NeoTypography.headlineMedium)
            NeoChoiceField("Used for all accounts", Money.supportedCurrencies.map { it to it }, Money.currencyCode,
                { if (it != Money.currencyCode) currencyTarget = it })
            Text("Changing currency changes the labels. Amounts are not converted.", style = NeoTypography.bodyMedium)
            NeoButton("Done", { showSettings = false }, Modifier.fillMaxWidth())
        }
    }
    deleteTarget?.let { target -> NeoConfirmDialog("Delete entry?", "Delete ${Money.format(target.amountMinor)}? This will update your account balance.",
        "Delete", { deleteTarget = null }, { viewModel.deleteExpense(target.id); deleteTarget = null }) }
    currencyTarget?.let { code -> NeoConfirmDialog("Use $code?", "Existing amounts will not be converted.",
        "Change", { currencyTarget = null }, { viewModel.setCurrency(code); currencyTarget = null }) }
}
