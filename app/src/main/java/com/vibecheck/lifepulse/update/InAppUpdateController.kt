package com.vibecheck.lifepulse.update

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class UpdateNotice(val id: Long, val installFailed: Boolean = false)

/** Activity-scoped Play updates. Neither a failed check nor a declined update blocks the app. */
internal class InAppUpdateController(
    private val activity: ComponentActivity,
    enabled: Boolean = activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0,
    private val manager: AppUpdateManager? = if (enabled) AppUpdateManagerFactory.create(activity.applicationContext) else null,
    private val canPrompt: () -> Boolean = { true },
    private val now: () -> Long = System::currentTimeMillis
) : DefaultLifecycleObserver {
    private val preferences = activity.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val _notice = MutableStateFlow<UpdateNotice?>(null)
    val notice = _notice.asStateFlow()
    private var nextNoticeId = 0L
    private var uiReady = false
    private var checking = false
    private var consentOpen = false
    private var activeUpdateType: Int? = null
    private var immediateInProgress = false
    private var completing = false
    private var notifiedThisResume = false
    private var downloadReady = false
    private var queryGeneration = 0L
    private var installRevision = 0L

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        consentOpen = false
        activeUpdateType = null
        immediateInProgress = false
        if (result.resultCode == Activity.RESULT_OK) checkForUpdates()
        // Cancellation/failure keeps the prompt cooldown; the user's current task can continue.
    }

    private val installListener = InstallStateUpdatedListener { state ->
        installRevision++
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                downloadReady = true
                // Query availability before choosing flexible restart UI or immediate recovery.
                checkForUpdates()
            }
            InstallStatus.FAILED, InstallStatus.CANCELED, InstallStatus.INSTALLED -> {
                downloadReady = false
                immediateInProgress = false
                _notice.value = null
            }
            else -> Unit
        }
    }

    init { activity.lifecycle.addObserver(this) }

    override fun onStart(owner: LifecycleOwner) { manager?.registerListener(installListener) }

    override fun onResume(owner: LifecycleOwner) {
        notifiedThisResume = false
        checkForUpdates(resumeImmediate = true)
    }

    override fun onStop(owner: LifecycleOwner) {
        manager?.unregisterListener(installListener)
        queryGeneration++
        checking = false
        _notice.value = null
    }

    fun setUiReady(ready: Boolean) {
        uiReady = ready
        if (ready) checkForUpdates(resumeImmediate = true) else _notice.value = null
    }

    private fun foregroundReady() = uiReady && canPrompt() &&
        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    fun checkForUpdates(resumeImmediate: Boolean = false) {
        val updateManager = manager ?: return
        if (!foregroundReady() || checking || completing ||
            (consentOpen && !(resumeImmediate && activeUpdateType == AppUpdateType.IMMEDIATE))) return
        checking = true
        val generation = queryGeneration
        val revision = installRevision
        updateManager.appUpdateInfo.addOnCompleteListener { task ->
            // A response from a previous foreground visit must not replace a newer check.
            if (generation != queryGeneration) return@addOnCompleteListener
            checking = false
            if (!foregroundReady()) return@addOnCompleteListener
            if (!task.isSuccessful) {
                Log.d(TAG, "Play update check unavailable", task.exception)
                return@addOnCompleteListener
            }
            // A live download event is newer than the snapshot requested before it.
            if (revision != installRevision) {
                if (downloadReady) checkForUpdates(resumeImmediate)
                return@addOnCompleteListener
            }
            handleInfo(task.result)
        }
    }

    private fun handleInfo(info: AppUpdateInfo) {
        downloadReady = info.installStatus() == InstallStatus.DOWNLOADED
        immediateInProgress = info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        when (UpdatePolicy.action(
            availableVersion = info.availableVersionCode(),
            updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            downloaded = downloadReady,
            downloadInProgress = info.installStatus() in setOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING),
            lastPromptVersion = preferences.getInt("last_prompt_version", 0),
            lastPromptAt = preferences.getLong("last_prompt_at", 0),
            now = now(),
            priority = info.updatePriority(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            immediateInProgress = immediateInProgress,
            lastPromptWasImmediate = preferences.getBoolean("last_prompt_immediate", false)
        )) {
            UpdateAction.OFFER_INSTALL -> offerInstall()
            UpdateAction.OFFER_DOWNLOAD -> startFlow(info, AppUpdateType.FLEXIBLE)
            UpdateAction.OFFER_IMMEDIATE -> startFlow(info, AppUpdateType.IMMEDIATE)
            UpdateAction.RESUME_IMMEDIATE -> startFlow(info, AppUpdateType.IMMEDIATE, recordPrompt = false)
            UpdateAction.NONE -> Unit
        }
    }

    private fun startFlow(info: AppUpdateInfo, type: Int, recordPrompt: Boolean = true) {
        val updateManager = manager ?: return
        try {
            consentOpen = true
            activeUpdateType = type
            if (type == AppUpdateType.IMMEDIATE) _notice.value = null
            val started = updateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
            if (started && recordPrompt) preferences.edit()
                .putInt("last_prompt_version", info.availableVersionCode())
                .putLong("last_prompt_at", now())
                .putBoolean("last_prompt_immediate", type == AppUpdateType.IMMEDIATE).apply()
            if (!started) {
                consentOpen = false
                activeUpdateType = null
            }
        } catch (e: Exception) {
            consentOpen = false
            activeUpdateType = null
            Log.d(TAG, "Could not open Play update flow", e)
        }
    }

    private fun offerInstall(installFailed: Boolean = false) {
        if (!foregroundReady() || !downloadReady || immediateInProgress || activeUpdateType == AppUpdateType.IMMEDIATE ||
            completing || (notifiedThisResume && !installFailed)) return
        notifiedThisResume = true
        _notice.value = UpdateNotice(++nextNoticeId, installFailed)
    }

    fun dismissNotice(id: Long) {
        if (_notice.value?.id == id) _notice.value = null
    }

    fun completeUpdate() {
        val updateManager = manager ?: return
        if (!foregroundReady() || !downloadReady || immediateInProgress || activeUpdateType == AppUpdateType.IMMEDIATE || completing) return
        completing = true
        _notice.value = null
        // Flexible installs require Restart. Play owns installation/restart for immediate flows.
        updateManager.completeUpdate().addOnCompleteListener { task ->
            completing = false
            if (!task.isSuccessful) {
                Log.d(TAG, "Could not install downloaded Play update", task.exception)
                if (downloadReady) offerInstall(installFailed = true) else checkForUpdates()
            }
        }
    }

    private companion object { const val TAG = "InAppUpdate" }
}
