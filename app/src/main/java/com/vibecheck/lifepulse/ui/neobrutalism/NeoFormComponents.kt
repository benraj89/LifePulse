package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecheck.lifepulse.R

val NeoFormSheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

@Composable
fun NeoSheetGrip() {
    Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp, 6.dp).background(NeoColors.Yellow, RoundedCornerShape(2.dp))
            .border(1.5.dp, NeoColors.Ink, RoundedCornerShape(2.dp)))
    }
}

/** Label overlaps the outline like a printed sticker, without covering editable text. */
@Composable
fun NeoFormTextField(value: String, onValueChange: (String) -> Unit, label: String,
                     modifier: Modifier = Modifier, placeholder: String = "",
                     color: Color = NeoColors.Surface, stickerColor: Color = NeoColors.Yellow,
                     singleLine: Boolean = true, textStyle: TextStyle = NeoTypography.bodyLarge,
                     keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)) {
    val labelClearance = with(LocalDensity.current) { 16.sp.toDp() } + 4.dp
    Box(modifier.padding(top = 10.dp, end = 3.dp, bottom = 3.dp)) {
        NeoTextField(value, onValueChange, Modifier.fillMaxWidth(), placeholder = placeholder, label = label,
            singleLine = singleLine, backgroundColor = color, focusedShadowColor = NeoColors.Ink,
            keyboardOptions = keyboardOptions, textStyle = textStyle,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = labelClearance, bottom = 10.dp),
            restingShadow = 2.dp, focusedShadow = 3.dp)
        NeoSticker(label, Modifier.offset(x = 8.dp, y = (-10).dp), color = stickerColor)
    }
}

/** Separate printed tickets give each form choice its own color and clear hit area. */
@Composable
fun NeoFormActionRow(label: String, value: String, onClick: () -> Unit,
                     modifier: Modifier = Modifier, color: Color = NeoColors.Habit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(5.dp)
    Row(modifier.fillMaxWidth().padding(end = 2.dp, bottom = 2.dp)
        .neoHardShadow(shape, offsetX = 2.dp, offsetY = 2.dp)
        .background(if (enabled) color else color.copy(alpha = .45f).compositeOver(NeoColors.Surface), shape)
        .border(2.dp, NeoColors.Ink, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = NeoTypography.labelMedium, color = NeoColors.Ink, modifier = Modifier.weight(.9f))
        Text(value, style = NeoTypography.titleSmall, color = NeoColors.Ink,
            textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
        if (enabled) Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp), tint = NeoColors.Ink)
    }
}

@Composable
fun NeoFormChoice(label: String, options: List<Pair<String, String>>, selectedKey: String?, onSelect: (String) -> Unit,
                  modifier: Modifier = Modifier, color: Color = NeoColors.Habit, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(5.dp)
    Box(modifier) {
        NeoFormActionRow(label, options.firstOrNull { it.first == selectedKey }?.second ?: stringResource(R.string.choice_placeholder_short),
            { focus.clearFocus(); keyboard?.hide(); expanded = true }, color = color, enabled = enabled && options.isNotEmpty())
        DropdownMenu(expanded, { expanded = false }, shape = shape, containerColor = NeoColors.Surface,
            tonalElevation = 0.dp, shadowElevation = 0.dp,
            modifier = Modifier.widthIn(min = 200.dp, max = 320.dp).border(2.dp, NeoColors.Ink, shape)) {
            options.forEach { (key, name) ->
                DropdownMenuItem(text = { Text(name, style = NeoTypography.bodyMedium, color = NeoColors.Ink) },
                    leadingIcon = { if (key == selectedKey) Icon(Icons.Default.Check, null, tint = NeoColors.Ink)
                        else Spacer(Modifier.size(24.dp)) },
                    modifier = Modifier.padding(horizontal = 6.dp).background(if (key == selectedKey) NeoColors.Yellow else NeoColors.Surface, shape),
                    onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

/** Only form selectors use the full pop treatment; screen navigation keeps its own styling. */
@Composable
fun NeoFormTabs(options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit,
                modifier: Modifier = Modifier, enabled: Boolean = true) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fills = listOf(NeoColors.Habit, NeoColors.LightViolet, NeoColors.BubblegumPink)
    val shape = RoundedCornerShape(4.dp)
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, (key, label) ->
            val selected = selectedKey == key
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                .selectable(selected, enabled, Role.Tab) { focus.clearFocus(); keyboard?.hide(); onSelect(key) }
                .padding(top = 3.dp, bottom = 5.dp, end = 2.dp)) {
                Box(Modifier.fillMaxSize().neoHardShadow(shape, offsetX = if (selected) 3.dp else 0.dp, offsetY = if (selected) 3.dp else 0.dp)
                    .background(if (!enabled) NeoColors.Muted else if (selected) NeoColors.Yellow else fills[index % fills.size], shape)
                    .border(if (selected) 2.5.dp else 1.5.dp, NeoColors.Ink, shape), contentAlignment = Alignment.Center) {
                    Text(label, color = NeoColors.Ink, textAlign = TextAlign.Center,
                        style = NeoTypography.labelLarge.copy(fontFamily = NeoDisplayFont, fontSize = 11.sp, lineHeight = 16.sp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp))
                }
            }
        }
    }
}

@Composable
fun NeoReminderToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val progress = animateFloatAsState(if (checked) 1f else 0f, tween(110), label = "reminderSwitch")
    val shape = RoundedCornerShape(5.dp)
    Row(Modifier.fillMaxWidth().background(if (checked) NeoColors.Lime else NeoColors.Lavender, shape)
        .border(2.dp, NeoColors.Ink, shape).toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
        .semantics { contentDescription = label }.heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.NotificationsActive, null, Modifier.size(20.dp), tint = NeoColors.Ink)
        Text(label, style = NeoTypography.titleSmall, color = NeoColors.Ink, modifier = Modifier.weight(1f))
        Canvas(Modifier.size(52.dp, 28.dp)) {
            val box = Size(48.dp.toPx(), 24.dp.toPx())
            val radius = CornerRadius(2.dp.toPx())
            drawRoundRect(NeoColors.Ink, Offset(2.dp.toPx(), 2.dp.toPx()), box, radius)
            drawRoundRect(if (checked) NeoColors.Yellow else NeoColors.Surface, size = box, cornerRadius = radius)
            drawRoundRect(NeoColors.Ink, size = box, cornerRadius = radius, style = Stroke(2.dp.toPx()))
            drawRoundRect(NeoColors.Ink, Offset((3.dp.toPx() + 24.dp.toPx() * progress.value), 3.dp.toPx()),
                Size(18.dp.toPx(), 18.dp.toPx()), radius)
        }
    }
}

@Preview(name = "Comic form controls", widthDp = 360, showBackground = true)
@Preview(name = "Comic form controls large text", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun FormComponentsPreview() {
    NeoBrutalismTheme {
        Column(Modifier.background(NeoColors.Background).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoFormTextField("Read a few pages", {}, "Habit name", color = NeoColors.PaleCyan)
            NeoFormTabs(listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly"), "daily", {})
            NeoReminderToggle("Remind me", true, {})
            NeoFormActionRow("Category", "Food & coffee", {}, color = NeoColors.LightViolet)
        }
    }
}
