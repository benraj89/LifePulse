package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Native switch behavior and checked semantics with the shared brand palette. */
@Composable
fun NeoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
              onColor: Color = NeoColors.Primary, offColor: Color = NeoColors.Muted,
              thumbColor: Color = NeoColors.Surface) {
    Box(modifier.heightIn(min = 48.dp).widthIn(min = 56.dp)
        .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange), contentAlignment = Alignment.Center) {
        Switch(checked, onCheckedChange = null, colors = SwitchDefaults.colors(
            checkedTrackColor = onColor, checkedThumbColor = NeoColors.Ink,
            checkedBorderColor = NeoColors.Ink, uncheckedTrackColor = offColor,
            uncheckedThumbColor = NeoColors.Ink, uncheckedBorderColor = NeoColors.Ink))
    }
}

@Composable
fun NeoSegmentedToggle(selectedIndex: Int, options: List<Pair<String, Color>>,
                       onSelectedIndexChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    NeoTextTabs(options.mapIndexed { index, option -> index.toString() to option.first }, selectedIndex.toString(),
        { onSelectedIndexChange(it.toInt()) }, modifier)
}
