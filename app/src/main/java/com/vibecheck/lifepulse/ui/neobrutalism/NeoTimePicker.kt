package com.vibecheck.lifepulse.ui.neobrutalism

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vibecheck.lifepulse.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTimePickerDialog(initialHour: Int, initialMinute: Int,
    title: String = stringResource(R.string.habit_time_picker_title),
    confirmText: String = stringResource(R.string.action_ok),
    cancelText: String = stringResource(R.string.add_expense_date_cancel),
    onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour.coerceIn(0, 23), initialMinute.coerceIn(0, 59),
        is24Hour = DateFormat.is24HourFormat(LocalContext.current))
    Dialog(onDismissRequest = onDismiss) {
        NeoColumnCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), contentPadding = 20.dp) {
            Text(title, style = NeoTypography.headlineSmall)
            TimeInput(state, modifier = Modifier.fillMaxWidth())
            NeoButton(confirmText, { onConfirm(state.hour, state.minute) }, Modifier.fillMaxWidth())
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(cancelText) }
        }
    }
}
