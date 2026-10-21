package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.core.CurrencyCatalog
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import com.vibecheck.lifepulse.ui.neobrutalism.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencySetupViewModel @Inject constructor(private val repository: ExpenseRepository) : ViewModel() {
    val settings = repository.observeCurrencySettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    var saving by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    init { initialize() }
    fun initialize() = viewModelScope.launch {
        failed = false
        try { repository.initializeCurrency() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    fun select(code: String) = viewModelScope.launch {
        if (saving) return@launch
        saving = true; failed = false
        try { repository.setCurrency(code) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
        finally { saving = false }
    }
}

@Composable
fun CurrencyGate(viewModel: CurrencySetupViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings
    // Publish before rendering screens; a restored ledger can carry a different currency.
    SideEffect { current?.code?.let { Money.currencyCode = it } }
    if (current?.confirmed == true && current.code != null && Money.currencyCode == current.code) {
        content()
    } else Surface(Modifier.fillMaxSize(), color = NeoColors.Background) {
        if (current != null && !current.confirmed) CurrencyPicker(current.code, viewModel::select, Modifier.fillMaxSize().statusBarsPadding(),
            saving = viewModel.saving, error = if (viewModel.failed) stringResource(R.string.currency_save_error) else null,
            allowedCodes = if (current.requiresOriginalCurrency) CurrencyCatalog.LEGACY_CODES else null, legacyLedger = current.requiresOriginalCurrency)
        else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (viewModel.failed) {
                Text(stringResource(R.string.currency_load_error), style = NeoTypography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                NeoButton(stringResource(R.string.action_retry), { viewModel.initialize() })
            } else CircularProgressIndicator(color = NeoColors.Ink)
        }
    }
}
