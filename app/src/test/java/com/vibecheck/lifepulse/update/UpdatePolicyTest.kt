package com.vibecheck.lifepulse.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {
    private val promptedAt = 1_000_000L

    private fun action(
        version: Int = 2, available: Boolean = true, allowed: Boolean = true,
        downloaded: Boolean = false, inProgress: Boolean = false,
        previousVersion: Int = 2, lastPrompt: Long = promptedAt, now: Long = promptedAt + 1,
        priority: Int = 0, immediateAllowed: Boolean = true, immediateInProgress: Boolean = false,
        lastPromptWasImmediate: Boolean = false
    ) = UpdatePolicy.action(version, available, allowed, downloaded, inProgress, previousVersion, lastPrompt, now,
        priority, immediateAllowed, immediateInProgress, lastPromptWasImmediate)

    @Test fun firstUpdateAndNewerVersionsCanBeOffered() {
        assertEquals(UpdateAction.OFFER_DOWNLOAD, action(previousVersion = 0, lastPrompt = 0))
        assertEquals(UpdateAction.OFFER_DOWNLOAD, action(version = 3))
    }

    @Test fun sameVersionWaitsTwentyFourHoursAfterConsentWasShown() {
        assertEquals(UpdateAction.NONE, action())
        assertEquals(UpdateAction.NONE, action(now = promptedAt + UpdatePolicy.PROMPT_INTERVAL_MS - 1))
        assertEquals(UpdateAction.OFFER_DOWNLOAD, action(now = promptedAt + UpdatePolicy.PROMPT_INTERVAL_MS))
    }

    @Test fun downloadedUpdateCanBeInstalledEvenDuringCooldown() {
        assertEquals(UpdateAction.OFFER_INSTALL, action(downloaded = true, allowed = false, available = false))
    }

    @Test fun activeDownloadsCannotStartAnotherConsentFlow() {
        assertEquals(UpdateAction.NONE, action(inProgress = true, previousVersion = 0, lastPrompt = 0))
    }

    @Test fun missingOrDisallowedUpdatesDoNotPrompt() {
        assertEquals(UpdateAction.NONE, action(available = false, previousVersion = 0))
        assertEquals(UpdateAction.NONE, action(allowed = false, previousVersion = 0))
    }

    @Test fun clockMovingBackwardsCannotSuppressUpdatesIndefinitely() {
        assertEquals(UpdateAction.OFFER_DOWNLOAD, action(now = promptedAt - 1))
    }

    @Test fun onlyHighPriorityReleasesUseImmediateUpdates() {
        (0..3).forEach { priority ->
            assertEquals(UpdateAction.OFFER_DOWNLOAD, action(priority = priority, previousVersion = 0))
        }
        listOf(4, 5).forEach { priority ->
            assertEquals(UpdateAction.OFFER_IMMEDIATE, action(priority = priority, previousVersion = 0))
        }
    }

    @Test fun criticalImmediateOfferCanEscalateAnEarlierFlexiblePrompt() {
        assertEquals(UpdateAction.OFFER_IMMEDIATE, action(priority = 4))
    }

    @Test fun declinedImmediateOfferUsesTheSameTwentyFourHourCooldown() {
        assertEquals(UpdateAction.NONE, action(priority = 5, lastPromptWasImmediate = true))
        assertEquals(UpdateAction.OFFER_IMMEDIATE, action(priority = 5, lastPromptWasImmediate = true,
            now = promptedAt + UpdatePolicy.PROMPT_INTERVAL_MS))
    }

    @Test fun unavailableImmediateFlowFallsBackToFlexibleWithoutBypassingCooldown() {
        assertEquals(UpdateAction.OFFER_DOWNLOAD, action(priority = 5, immediateAllowed = false, previousVersion = 0))
        assertEquals(UpdateAction.NONE, action(priority = 5, immediateAllowed = false))
    }

    @Test fun acceptedImmediateFlowResumesDespiteCooldownOrDownloadedState() {
        assertEquals(UpdateAction.RESUME_IMMEDIATE, action(available = false, allowed = false, downloaded = true,
            inProgress = true, immediateInProgress = true, lastPromptWasImmediate = true))
    }

    @Test fun unsupportedImmediateRecoveryDoesNotSwitchToFlexible() {
        assertEquals(UpdateAction.NONE, action(immediateInProgress = true, immediateAllowed = false, previousVersion = 0))
    }

    @Test fun normalReleasesCannotForceImmediateWhenFlexibleIsUnavailable() {
        assertEquals(UpdateAction.NONE, action(allowed = false, previousVersion = 0))
        assertEquals(UpdateAction.OFFER_IMMEDIATE, action(allowed = false, priority = 5, previousVersion = 0))
    }

    @Test fun existingFlexibleDownloadsAreNotInterruptedByPriorityEscalation() {
        assertEquals(UpdateAction.NONE, action(priority = 5, inProgress = true, previousVersion = 0))
        assertEquals(UpdateAction.OFFER_INSTALL, action(priority = 5, downloaded = true, previousVersion = 0))
    }
}
