package com.vibecheck.lifepulse.ui.neobrutalism

import com.vibecheck.lifepulse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** A compact choice field that follows the same border, color, and shadow as our buttons. */
@Composable
fun NeoChoiceField(
    label: String, options: List<Pair<String, String>>, selectedKey: String?,
    onSelect: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = NeoTypography.labelLarge, color = NeoColors.OnSurface)
        Box {
            NeoButton(
                text = (options.firstOrNull { it.first == selectedKey }?.second ?: stringResource(R.string.choice_placeholder)) + "  ▾",
                onClick = { focusManager.clearFocus(); keyboard?.hide(); expanded = true }, modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeoColors.Surface, contentColor = NeoColors.OnSurface,
                enabled = enabled && options.isNotEmpty()
            )
            DropdownMenu(expanded, { expanded = false }, modifier = Modifier
                .background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
                options.forEach { (key, name) ->
                    DropdownMenuItem(text = { Text(name, color = NeoColors.OnSurface) }, onClick = {
                        onSelect(key); expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun NeoConfirmDialog(title: String, message: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        NeoStackedDialogCard(backLayerColor = NeoColors.Cyan, tapeText = stringResource(R.string.confirmation_tape)) {
            Text(title, style = NeoTypography.titleLarge, color = NeoColors.OnSurface)
            Text(message, style = NeoTypography.bodyMedium, color = NeoColors.OnSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeoButton(stringResource(R.string.add_expense_date_cancel), onDismiss, Modifier.weight(1f), backgroundColor = NeoColors.Surface,
                    contentColor = NeoColors.OnSurface, horizontalPadding = 8.dp)
                NeoButton(confirmText, onConfirm, Modifier.weight(1f), backgroundColor = NeoColors.Coral,
                    contentColor = NeoColors.OnSurface, horizontalPadding = 8.dp)
            }
        }
    }
}
