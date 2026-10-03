package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A single quiet tab strip; cards and primary actions carry the bold styling. */
@Composable
fun NeoTextTabs(options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit,
                modifier: Modifier = Modifier, enabled: Boolean = true) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Row(modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { (key, label) ->
            Column(Modifier.weight(1f).selectable(selectedKey == key, enabled = enabled, role = Role.Tab,
                onClick = { focus.clearFocus(); keyboard?.hide(); onSelect(key) })) {
                Text(label, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                    textAlign = TextAlign.Center, style = NeoTypography.labelLarge,
                    fontWeight = if (selectedKey == key) FontWeight.Black else FontWeight.Normal, color = NeoColors.OnSurface)
                Box(Modifier.fillMaxWidth().height(if (selectedKey == key) 3.dp else 1.dp).background(NeoColors.Border))
            }
        }
    }
}

/** Label and selected value in one row, with choices shown only when needed. */
@Composable
fun NeoInlineChoice(label: String, options: List<Pair<String, String>>, selectedKey: String?,
                    onSelect: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier) {
        TextButton(onClick = { focus.clearFocus(); keyboard?.hide(); expanded = true },
            enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, style = NeoTypography.bodyMedium, color = NeoColors.OnSurface, modifier = Modifier.weight(1f))
                Text((options.firstOrNull { it.first == selectedKey }?.second ?: "Choose") + if (enabled) " ▾" else "",
                    style = NeoTypography.labelLarge, color = NeoColors.OnSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
            options.forEach { (key, name) -> DropdownMenuItem(text = { Text(name, color = NeoColors.OnSurface) }, onClick = { onSelect(key); expanded = false }) }
        }
    }
}
