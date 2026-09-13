package com.vibecheck.lifepulse.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Returns a callback that reliably hides the soft keyboard.
 *
 * Why this exists: on several OEM skins (notably Vivo/FunTouch on Android 11) and inside
 * Compose [androidx.compose.ui.window.Dialog] windows, `LocalSoftwareKeyboardController.hide()`
 * alone is not honoured. The IME is attached to the *dialog* window, so the hide request must be
 * routed to that window's token/insets controller, not the Activity's.
 *
 * This helper applies every available mechanism, in order:
 *  1. Clears Compose focus (ends the text input session).
 *  2. Compose's software keyboard controller.
 *  3. The owning window's `WindowInsetsController` (IME type).
 *  4. A direct [InputMethodManager] hide using the owning window's decor view token.
 *
 * IMPORTANT: call this from a composable scope *inside* the dialog so [LocalView] resolves to
 * the dialog's view hierarchy.
 */
@Composable
fun rememberKeyboardDismisser(): () -> Unit {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    return remember(view, focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()

            val window = view.owningWindow()
            val decorView = window?.decorView ?: view

            window?.let { WindowCompat.getInsetsController(it, decorView) }
                ?.hide(WindowInsetsCompat.Type.ime())

            val imm = view.context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val token = decorView.windowToken ?: view.windowToken
            imm?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)

            // Some OEM IMEs re-post a show request in the same frame; hide again next frame.
            view.post {
                imm?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }
}

/** Resolves the [Window] that actually owns this view (dialog window when inside a Dialog). */
private fun View.owningWindow(): Window? =
    (parent as? DialogWindowProvider)?.window ?: context.findActivity()?.window

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

