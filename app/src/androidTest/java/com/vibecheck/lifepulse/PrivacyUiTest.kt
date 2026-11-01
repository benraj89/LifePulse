package com.vibecheck.lifepulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import com.vibecheck.lifepulse.ui.privacy.PrivacyContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun policyWorksOfflineAndChoiceAndBackRemainUsableWithLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val html = context.assets.open("privacy-policy.html").bufferedReader().use { it.readText() }
            .substringAfter("<main>").substringBefore("</main>")
        var enabled by mutableStateOf(false)
        var backClicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                NeoBrutalismTheme {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        PrivacyContent(html, enabled, onEnabledChange = { enabled = it }, onBack = { backClicks++ })
                    }
                }
            }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.onNode(isToggleable()).performClick().assertIsOff()
        compose.onNodeWithText("POCKDO Privacy Policy", substring = true).performScrollTo().assertExists()
        compose.onNodeWithText("Ben Raj", substring = true).assertExists()
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, backClicks) }
    }
}
