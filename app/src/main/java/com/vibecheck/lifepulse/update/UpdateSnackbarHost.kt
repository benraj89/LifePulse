package com.vibecheck.lifepulse.update

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.R

/** Hosted by the navigation scaffold so the action clears the bottom navigation and system bars. */
@Composable
internal fun UpdateSnackbarHost(notice: UpdateNotice?, onRestart: () -> Unit, onDismiss: (Long) -> Unit) {
    val host = remember { SnackbarHostState() }
    val message = stringResource(if (notice?.installFailed == true) R.string.update_install_failed else R.string.update_downloaded)
    val restart = stringResource(R.string.update_restart)
    LaunchedEffect(notice?.id) {
        val current = notice ?: return@LaunchedEffect
        val result = host.showSnackbar(message, restart, withDismissAction = true, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) onRestart() else onDismiss(current.id)
    }
    SnackbarHost(host, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
}
