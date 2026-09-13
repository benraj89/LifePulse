package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.ui.util.rememberKeyboardDismisser

/**
 * A high-contrast, thick-bordered text field. The border thickens and the shadow grows slightly
 * when focused to give clear, punchy focus feedback (a Neobrutalism staple).
 *
 * Usage:
 * ```
 * var query by remember { mutableStateOf("") }
 * NeoTextField(value = query, onValueChange = { query = it }, placeholder = "Search habits...")
 * ```
 */
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    backgroundColor: Color = NeoColors.Surface,
    borderColor: Color = NeoColors.Border,
    focusedShadowColor: Color = NeoColors.Accent,
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 3.dp,
    focusedBorderWidth: Dp = 4.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val resolvedKeyboardActions = keyboardActions ?: KeyboardActions(
        onDone = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    )

    Box(
        modifier = modifier
            .neoHardShadow(
                shape = shape,
                shadowColor = if (isFocused) focusedShadowColor else NeoColors.Shadow,
                offsetX = if (isFocused) 6.dp else 4.dp,
                offsetY = if (isFocused) 6.dp else 4.dp
            )
            .background(color = backgroundColor, shape = shape)
            .border(
                width = if (isFocused) focusedBorderWidth else borderWidth,
                color = borderColor,
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = NeoTypography.bodyLarge,
                color = NeoColors.Black.copy(alpha = 0.4f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = resolvedKeyboardActions,
            textStyle = NeoTypography.bodyLarge.copy(color = NeoColors.OnSurface),
            interactionSource = interactionSource,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(NeoColors.Black),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}
