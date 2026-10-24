package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.CurrencyCatalog
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.ui.neobrutalism.*

@Composable
internal fun CurrencyPicker(initialCode: String?, onSave: (String) -> Unit,
                            modifier: Modifier = Modifier, locked: Boolean = false,
                            saving: Boolean = false, error: String? = null, onDismiss: (() -> Unit)? = null,
                            allowedCodes: Set<String>? = null, legacyBackup: Boolean = false, legacyLedger: Boolean = false) {
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCode by rememberSaveable(initialCode) { mutableStateOf(initialCode) }
    val currencies = remember(locale.toLanguageTag(), allowedCodes) {
        CurrencyCatalog.entries.filter { allowedCodes == null || it.code in allowedCodes }
            .sortedWith(compareBy<com.vibecheck.lifepulse.core.CurrencyInfo> { it.code != initialCode }.thenBy { it.name(locale) })
    }
    val matches = remember(query, currencies, locale) {
        currencies.filter { it.code.contains(query.trim(), true) || it.name(locale).contains(query.trim(), true) || it.symbol(locale).contains(query.trim(), true) }
    }
    Column(modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(if (legacyLedger) R.string.currency_recover_title else if (legacyBackup) R.string.currency_legacy_title else if (locked) R.string.currency_title else R.string.currency_choose_title), style = NeoTypography.headlineMedium)
        Text(stringResource(if (legacyLedger) R.string.currency_recover_hint else if (legacyBackup) R.string.currency_legacy_hint else if (locked) R.string.currency_locked_hint else R.string.currency_setup_hint), style = NeoTypography.bodyMedium)
        selectedCode?.let { code ->
            Text(stringResource(R.string.currency_selection_preview, code, Money.format(123456, code, locale)), style = NeoTypography.labelLarge)
        }
        NeoTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().testTag("currency_search"),
            placeholder = stringResource(R.string.currency_search_hint), backgroundColor = NeoColors.Surface)
        LazyColumn(Modifier.weight(1f).testTag("currency_list"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (matches.isEmpty()) item { Text(stringResource(R.string.currency_no_results), style = NeoTypography.bodyMedium) }
            items(matches, key = { it.code }) { currency ->
                val selected = currency.code == selectedCode
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("currency_${currency.code}")
                    .background(if (selected) NeoColors.Primary else NeoColors.Surface, NeoTheme.shapes.small)
                    .border(if (selected) 2.dp else 0.dp, if (selected) NeoColors.Ink else NeoColors.Surface, NeoTheme.shapes.small)
                    .selectable(selected, enabled = !locked && !saving, role = Role.RadioButton,
                        onClick = { selectedCode = currency.code }).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected, onClick = null, enabled = !locked && !saving,
                        colors = RadioButtonDefaults.colors(selectedColor = NeoColors.Ink))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(currency.name(locale), style = NeoTypography.bodyMedium)
                        Text(currency.code, style = NeoTypography.bodySmall)
                    }
                    Text(currency.symbol(locale), style = NeoTypography.labelLarge)
                }
            }
        }
        error?.let { Text(it, color = NeoColors.Danger, style = NeoTypography.bodyMedium) }
        if (!locked) NeoButton(stringResource(if (saving) R.string.action_saving else R.string.currency_save),
            { selectedCode?.let(onSave) }, Modifier.fillMaxWidth(), enabled = selectedCode != null && !saving,
            backgroundColor = NeoColors.Primary)
        onDismiss?.let { dismiss ->
            TextButton(onClick = dismiss, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done), color = NeoColors.OnSurface) }
        }
    }
}
