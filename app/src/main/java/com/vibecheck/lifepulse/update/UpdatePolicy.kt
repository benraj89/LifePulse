package com.vibecheck.lifepulse.update

internal enum class UpdateAction { NONE, OFFER_DOWNLOAD, OFFER_INSTALL, OFFER_IMMEDIATE, RESUME_IMMEDIATE }

/** Download recovery is independent of the cooldown for asking users to accept a new update. */
internal object UpdatePolicy {
    const val PROMPT_INTERVAL_MS = 24L * 60 * 60 * 1000
    const val IMMEDIATE_PRIORITY = 4

    fun action(
        availableVersion: Int,
        updateAvailable: Boolean,
        flexibleAllowed: Boolean,
        downloaded: Boolean,
        downloadInProgress: Boolean,
        lastPromptVersion: Int,
        lastPromptAt: Long,
        now: Long,
        priority: Int = 0,
        immediateAllowed: Boolean = false,
        immediateInProgress: Boolean = false,
        lastPromptWasImmediate: Boolean = false
    ): UpdateAction {
        // An already accepted immediate flow must resume, rather than switch to flexible UI.
        if (immediateInProgress) return if (immediateAllowed) UpdateAction.RESUME_IMMEDIATE else UpdateAction.NONE
        if (downloaded) return UpdateAction.OFFER_INSTALL
        if (downloadInProgress || !updateAvailable) return UpdateAction.NONE

        val immediate = priority >= IMMEDIATE_PRIORITY && immediateAllowed
        if (!immediate && !flexibleAllowed) return UpdateAction.NONE
        val canPrompt = availableVersion != lastPromptVersion || lastPromptAt <= 0L || now < lastPromptAt ||
            now - lastPromptAt >= PROMPT_INTERVAL_MS || (immediate && !lastPromptWasImmediate)
        if (!canPrompt) return UpdateAction.NONE
        return if (immediate) UpdateAction.OFFER_IMMEDIATE else UpdateAction.OFFER_DOWNLOAD
    }
}
