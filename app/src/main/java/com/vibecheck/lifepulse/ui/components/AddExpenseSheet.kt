package com.vibecheck.lifepulse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.Category
import com.vibecheck.lifepulse.ui.neobrutalism.NeoButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoDatePickerDialog
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTextField
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reusable "Add Expense" modal bottom sheet, fully state-hoisted:
 * the caller owns visibility and receives the validated payload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amount: Double, note: String, timestamp: Long) -> Unit,
    onAddCategory: ((name: String, colorHex: String) -> Unit)? = null,
    onDeleteCategory: ((Category) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategory == null) selectedCategory = categories.firstOrNull()
    }

    val amount = amountText.toDoubleOrNull()
    val isValid = amount != null && amount > 0.0 && selectedCategory != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeoColors.PastelYellow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.add_expense_sheet_title),
                style = NeoTypography.headlineMedium,
                color = NeoColors.OnSurface
            )

            NeoTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
                        amountText = input
                    }
                },
                placeholder = stringResource(R.string.add_expense_amount_label),
                backgroundColor = NeoColors.PaleCyan,
                focusedShadowColor = NeoColors.HotPink,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
                backgroundColor = NeoColors.SkyBlue,
                shadowColor = NeoColors.DeepPurple,
                onAddCategory = onAddCategory,
                onDeleteCategory = onDeleteCategory
            )

            // Custom Neobrutalism-styled date picker trigger row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.add_expense_date_label),
                    style = NeoTypography.labelLarge,
                    color = NeoColors.OnSurface
                )
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    backgroundColor = NeoColors.BubblegumPink,
                    shape = RoundedCornerShape(10.dp),
                    borderWidth = 2.dp,
                    shadowOffsetX = 4.dp,
                    shadowOffsetY = 4.dp,
                    contentPadding = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = NeoColors.OnSurface
                        )
                        Text(
                            text = DateUtils.formatTimestamp(
                                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                pattern = "EEE, dd MMM yyyy"
                            ),
                            style = NeoTypography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeoColors.OnSurface
                        )
                    }
                }
            }

            NeoTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = stringResource(R.string.add_expense_note_label),
                backgroundColor = NeoColors.MintGreen,
                focusedShadowColor = NeoColors.DeepPurple,
                modifier = Modifier.fillMaxWidth()
            )

            NeoButton(
                text = stringResource(R.string.add_expense_save),
                onClick = {
                    val category = selectedCategory ?: return@NeoButton
                    val timestamp = selectedDate.atTime(java.time.LocalTime.now())
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onSave(category.id, amount ?: return@NeoButton, note, timestamp)
                    onDismiss()
                },
                enabled = isValid,
                backgroundColor = NeoColors.Coral,
                contentColor = NeoColors.OnSurface,
                shadowColor = NeoColors.DeepPurple,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }

    if (showDatePicker) {
        NeoDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
