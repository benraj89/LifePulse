package com.vibecheck.lifepulse.ui.neobrutalism

import com.vibecheck.lifepulse.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.shape.RoundedCornerShape

/** Compact comic tabs keep a full 48dp target around the smaller printed tab. */
@Composable
fun NeoTextTabs(options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit,
                modifier: Modifier = Modifier, enabled: Boolean = true) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, (key, label) ->
            val selected = selectedKey == key
            val shape = RoundedCornerShape(4.dp)
            val fill = listOf(NeoColors.Yellow, NeoColors.Habit, NeoColors.BubblegumPink)[index % 3]
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).selectable(selected, enabled = enabled, role = Role.Tab,
                onClick = { focus.clearFocus(); keyboard?.hide(); onSelect(key) }).padding(top = 4.dp, bottom = 6.dp, end = 2.dp)) {
                Box(Modifier.fillMaxSize()
                    .neoHardShadow(shape, offsetX = if (selected) 2.dp else 0.dp, offsetY = if (selected) 2.dp else 0.dp)
                    .background(if (selected) fill else NeoColors.Surface, shape)
                    .border(if (selected) 2.dp else 1.dp, NeoColors.Ink, shape), contentAlignment = Alignment.Center) {
                    Text(label, modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                        textAlign = TextAlign.Center, style = NeoTypography.labelLarge,
                        fontWeight = FontWeight.Bold, color = NeoColors.OnSurface)
                }
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
            enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, style = NeoTypography.bodyMedium, color = NeoColors.OnSurface, modifier = Modifier.weight(1f))
                Text((options.firstOrNull { it.first == selectedKey }?.second ?: stringResource(R.string.choice_placeholder_short)) + if (enabled) " ▾" else "",
                    style = NeoTypography.labelLarge, color = NeoColors.OnSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
            options.forEach { (key, name) -> DropdownMenuItem(text = { Text(name, color = NeoColors.OnSurface) }, onClick = { onSelect(key); expanded = false }) }
        }
    }
}
