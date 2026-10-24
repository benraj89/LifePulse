package com.vibecheck.lifepulse.ui.expenses

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** SAF grants access only to the documents the user chooses; no storage permission is needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupActions(snackbar: SnackbarHostState, onRestored: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val pendingLegacyRestore by viewModel.pendingLegacyRestore.collectAsStateWithLifecycle()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let { uri -> viewModel.run(BackupOperation.BACKUP, uri) }
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let { uri -> viewModel.run(BackupOperation.CSV, uri) }
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.selectRestore(it) }
    val currentOnRestored by rememberUpdatedState(onRestored)
    LaunchedEffect(state.message) {
        state.message?.let {
            if (state.restored) currentOnRestored()
            snackbar.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessage()
        }
    }
    fun pick(action: () -> Unit) {
        showMenu = false
        try { action() } catch (_: ActivityNotFoundException) { viewModel.pickerFailed() }
        catch (_: SecurityException) { viewModel.pickerFailed() }
    }
    fun filename(extension: String): String = "LifePulse-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.$extension"

    val backupLabel = stringResource(R.string.backup_menu_title)
    NeoIconButton(Icons.Default.FolderOpen, backupLabel, { showMenu = true },
        enabled = state.operation == null, backgroundColor = NeoColors.LightViolet, shadowOffset = 2.dp)

    if (showMenu) ModalBottomSheet(onDismissRequest = { showMenu = false }, containerColor = NeoColors.Background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.backup_menu_title), style = NeoTypography.headlineMedium)
            BackupActionCard(R.string.backup_create_title, R.string.backup_create_hint, R.string.backup_create_button,
                NeoColors.Primary) { pick { backupPicker.launch(filename("mbak")) } }
            BackupActionCard(R.string.backup_restore_title, R.string.backup_restore_hint, R.string.backup_restore_button,
                NeoColors.PastelYellow) { pick { restorePicker.launch(arrayOf("*/*")) } }
            BackupActionCard(R.string.backup_csv_title, R.string.backup_csv_hint, R.string.backup_csv_button,
                NeoColors.PaleCyan) { pick { csvPicker.launch(filename("csv")) } }
            Text(stringResource(R.string.backup_privacy_hint), style = NeoTypography.bodySmall)
        }
    }
    if (pendingRestore != null) NeoConfirmDialog(
        stringResource(R.string.backup_restore_confirm_title), stringResource(R.string.backup_restore_confirm_message),
        stringResource(R.string.backup_restore_confirm_button), { viewModel.selectRestore(null) }, viewModel::confirmRestore)

    if (pendingLegacyRestore != null) ModalBottomSheet(onDismissRequest = viewModel::clearLegacyRestore,
        containerColor = NeoColors.Surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        CurrencyPicker(null, viewModel::restoreLegacy, Modifier.fillMaxHeight(0.9f),
            onDismiss = viewModel::clearLegacyRestore, allowedCodes = com.vibecheck.lifepulse.data.backup.BackupFormat.LEGACY_CURRENCIES,
            legacyBackup = true)
    }

    state.operation?.let { operation ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.PastelYellow) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = NeoColors.Ink)
                    Text(stringResource(when (operation) {
                        BackupOperation.BACKUP -> R.string.backup_progress_save
                        BackupOperation.RESTORE -> R.string.backup_progress_restore
                        BackupOperation.CSV -> R.string.backup_progress_csv
                    }), style = NeoTypography.titleMedium)
                }
                Text(stringResource(R.string.backup_progress_hint), style = NeoTypography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BackupActionCard(title: Int, hint: Int, button: Int, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface) {
        Text(stringResource(title), style = NeoTypography.titleMedium)
        Text(stringResource(hint), style = NeoTypography.bodyMedium)
        NeoButton(stringResource(button), onClick, Modifier.fillMaxWidth(),
            backgroundColor = if (color == NeoColors.Primary) NeoColors.Primary else NeoColors.Surface,
            shadowOffset = if (color == NeoColors.Primary) 4.dp else 0.dp)
    }
}
