package com.vibecheck.lifepulse.ui.habits

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.worker.ReminderSyncWorker

/** Shown only while configuring a reminder; special access is optional. */
@Composable
internal fun ExactAlarmPermissionHint(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val alarms = remember(context) { context.getSystemService(AlarmManager::class.java) }
    fun hasAccess() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        alarms?.canScheduleExactAlarms() == true

    var allowed by remember { mutableStateOf(hasAccess()) }
    var settingsUnavailable by remember { mutableStateOf(false) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        allowed = hasAccess()
        ReminderSyncWorker.enqueueOneTimeSync(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = hasAccess() }

    if (!allowed) ExactAlarmPermissionNotice(onOpenSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            settingsUnavailable = runCatching {
                settingsLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}")))
            }.isFailure
        }
    }, settingsUnavailable = settingsUnavailable, modifier = modifier)
}

@Composable
private fun ExactAlarmPermissionNotice(
    onOpenSettings: () -> Unit,
    settingsUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier.testTag("exact_alarm_permission_hint")) {
        Text(stringResource(R.string.habit_exact_alarm_explanation),
            style = NeoTypography.bodyMedium, color = NeoColors.Ink)
        if (settingsUnavailable) Text(stringResource(R.string.habit_exact_alarm_settings_unavailable),
            style = NeoTypography.bodyMedium, color = NeoColors.Danger)
        TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.habit_exact_alarm_settings), color = NeoColors.Ink)
        }
    }
}

@Preview(name = "Optional precise reminders", widthDp = 320, fontScale = 1.5f)
@Composable
private fun ExactAlarmPermissionPreview() {
    NeoBrutalismTheme { ExactAlarmPermissionNotice({}, settingsUnavailable = false) }
}
