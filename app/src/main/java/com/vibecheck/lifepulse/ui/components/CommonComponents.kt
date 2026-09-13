package com.vibecheck.lifepulse.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoIconButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTextField
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.ui.neobrutalism.neoHardShadow

/** Safe parsing of "#RRGGBB" strings coming from the database. */
fun String.toComposeColor(fallback: Color = Color.Gray): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

/** Serializes an opaque [Color] back to a "#RRGGBB" hex string for storage. */
fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

/** Curated vibrant swatches offered when creating a new category. */
private val NewCategorySwatches = listOf(
    NeoColors.SkyBlue, NeoColors.TealBlue, NeoColors.MintGreen, NeoColors.LightGreen,
    NeoColors.Mustard, NeoColors.Amber, NeoColors.Coral, NeoColors.Salmon,
    NeoColors.BubblegumPink, NeoColors.HotPink, NeoColors.LightViolet, NeoColors.DeepPurple
)

@Composable
fun ColorDot(colorHex: String, modifier: Modifier = Modifier, size: Int = 12) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(colorHex.toComposeColor(), CircleShape)
            .border(width = 2.dp, color = NeoColors.Border, shape = CircleShape)
    )
}

/**
 * Neobrutalism-styled category picker.
 *
 * Creating a category happens **inline**: the "New Category" menu entry reveals a compact row
 * (name field + color dots + confirm/cancel) right under the picker, so the user never leaves the
 * current screen/sheet and there is no stacked dialog to get lost in. Categories the user creates
 * can be deleted straight from the dropdown list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.add_expense_category_label),
    backgroundColor: Color = NeoColors.Surface,
    shadowColor: Color = NeoColors.Shadow,
    onAddCategory: ((name: String, colorHex: String) -> Unit)? = null,
    onDeleteCategory: ((Category) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedSwatch by remember { mutableStateOf(NewCategorySwatches.first()) }
    var pendingName by remember { mutableStateOf<String?>(null) }
    val shape = RoundedCornerShape(12.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current


    // Auto-select (and close the inline form) as soon as the created category shows up.
    LaunchedEffect(categories, pendingName) {
        val name = pendingName ?: return@LaunchedEffect
        categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let {
            onSelect(it)
            pendingName = null
            creating = false
        }
    }

    fun submit() {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        pendingName = trimmed
        onAddCategory?.invoke(trimmed, selectedSwatch.toHexString())
        newName = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = NeoTypography.labelLarge,
            color = NeoColors.OnSurface
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoHardShadow(shape = shape, shadowColor = shadowColor, offsetX = 4.dp, offsetY = 4.dp)
                    .background(color = backgroundColor, shape = shape)
                    .border(width = 3.dp, color = NeoColors.Border, shape = shape)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                selected?.let { ColorDot(it.colorHex, size = 14) }
                Text(
                    text = selected?.name ?: label,
                    style = NeoTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeoColors.OnSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NeoColors.OnSurface
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = NeoColors.Surface,
                shape = shape,
                modifier = Modifier
                    .border(width = 3.dp, color = NeoColors.Border, shape = shape)
                    .clip(shape)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(category.colorHex)
                                Text(
                                    text = category.name,
                                    fontWeight = if (category == selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (onDeleteCategory != null && !category.isDefault) {
                                NeoIconButton(
                                    icon = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.category_delete),
                                    onClick = {
                                        onDeleteCategory(category)
                                        if (selected?.id == category.id) {
                                            onSelect(categories.first { it.id != category.id })
                                        }
                                    },
                                    backgroundColor = NeoColors.Danger,
                                    iconTint = NeoColors.Surface,
                                    size = 30.dp,
                                    shadowOffset = 2.dp,
                                    borderWidth = 2.dp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelect(category)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(textColor = NeoColors.OnSurface)
                    )
                }

                if (onAddCategory != null) {
                    HorizontalDivider(color = NeoColors.Border, thickness = 2.dp)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = NeoColors.OnSurface
                                )
                                Text(
                                    text = stringResource(R.string.category_new),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            creating = true
                        },
                        colors = MenuDefaults.itemColors(textColor = NeoColors.OnSurface)
                    )
                }
            }
        }

        // Compact inline "create category" row.
        AnimatedVisibility(visible = creating && onAddCategory != null) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NeoTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = stringResource(R.string.category_name_hint),
                        backgroundColor = NeoColors.PaleCyan,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                    NeoIconButton(
                        icon = Icons.Default.Check,
                        contentDescription = stringResource(R.string.category_add),
                        onClick = { submit() },
                        backgroundColor = if (newName.isBlank()) NeoColors.Surface else NeoColors.MintGreen
                    )
                    NeoIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            newName = ""
                            creating = false
                        },
                        backgroundColor = NeoColors.Surface
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NewCategorySwatches) { swatch ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(swatch, CircleShape)
                                .border(
                                    width = if (swatch == selectedSwatch) 3.dp else 2.dp,
                                    color = NeoColors.Border,
                                    shape = CircleShape
                                )
                                .clickable { selectedSwatch = swatch }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    imageSize: androidx.compose.ui.unit.Dp = 180.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(imageSize)
            )
        }
        Text(text = title, style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
        Text(
            text = subtitle,
            style = NeoTypography.bodyMedium,
            color = NeoColors.OnSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ClickableRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) { content() }
}
