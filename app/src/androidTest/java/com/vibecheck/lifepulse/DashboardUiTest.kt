package com.vibecheck.lifepulse

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.dashboard.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.io.File

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun habitPeriodIsClearAndCompletionCanBeToggledFromDashboard() {
        var state by mutableStateOf(DashboardUiState(date = LocalDate.of(2026, 10, 3),
            habits = listOf(Habit(1, "Weekly review", HabitFrequency.WEEKLY, 0)),
            totalHabits = 1, accounts = listOf(Account(1, "Cash", 0)), isLoading = false))
        compose.setContent {
            NeoBrutalismTheme {
                DashboardContent(state, onHabits = {}, onMoney = {}, onAddEntry = {}, onToggleHabit = { id ->
                    state = state.copy(habits = state.habits.map {
                        if (it.id == id) it.copy(completedToday = !it.completedToday) else it
                    }, completedHabits = 1)
                })
            }
        }
        compose.onNodeWithText("This week · To do").performScrollTo().assertIsDisplayed()
        compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("dashboard_habit_1")))
            .performClick().assertIsOn()
        compose.onNodeWithText("This week · Done").assertExists()
    }

    @Test fun overviewNavigationAndSingleEntryActionWork() {
        var moneyClicks = 0
        var habitClicks = 0
        var entryClicks = 0
        val state = DashboardUiState(date = LocalDate.of(2026, 10, 3),
            accounts = listOf(Account(1, "Cash", 0)), owedToYou = 500, isLoading = false)
        compose.setContent {
            NeoBrutalismTheme {
                DashboardContent(state, onHabits = { habitClicks++ }, onMoney = { moneyClicks++ },
                    onAddEntry = { entryClicks++ }, onToggleHabit = {})
            }
        }
        compose.onNodeWithText("Add entry").performClick()
        compose.onNodeWithText("Your money · Oct").performScrollTo().performClick()
        compose.onNodeWithText("People owe you").assertExists()
        compose.onNodeWithText("Start a habit").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, entryClicks)
            assertEquals(1, moneyClicks)
            assertEquals(1, habitClicks)
        }
    }

    @Test fun loadingDoesNotShowFalseEmptyStatesOrEnableEntry() {
        compose.setContent {
            NeoBrutalismTheme {
                DashboardContent(DashboardUiState(), onHabits = {}, onMoney = {}, onAddEntry = {}, onToggleHabit = {})
            }
        }
        compose.onNodeWithText("Loading your overview…").assertIsDisplayed()
        compose.onNodeWithText("Start a habit").assertDoesNotExist()
        compose.onNodeWithText("Add entry").assertIsNotEnabled()
    }

    @Test fun largeTextAndLongCurrencyRemainReadableAndHabitRowIsToggleable() {
        var toggledId: Long? = null
        val state = dashboardState(
            LocalDate.of(2026, 10, 21),
            listOf(
                Habit(1, "Make time for a walk around the neighbourhood", HabitFrequency.DAILY, 0),
                Habit(2, "Plan the week", HabitFrequency.WEEKLY, 0, completedToday = true)
            ),
            emptyList(), emptyList(), listOf(Account(1, "Everyday account", 12345678901234))
        ).copy(spentThisMonth = 98765432101234)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                NeoBrutalismTheme {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        DashboardContent(state, onHabits = {}, onMoney = {}, onAddEntry = {},
                            onToggleHabit = { toggledId = it })
                    }
                }
            }
        }
        compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("dashboard_habit_1")))
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1L, toggledId) }
        val monthLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(Money.format(state.spentThisMonth)).performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(monthLayouts) }
        compose.runOnIdle { assertFalse("The large monthly amount must wrap without clipping", monthLayouts.single().hasVisualOverflow) }
        val amount = compose.onNodeWithText(Money.format(state.accountBalance))
        amount.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        amount.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        captureDashboard("dashboard-large-text.png")
        compose.runOnIdle {
            val layout = layouts.single()
            assertFalse("Currency overflow: size=${layout.size}, lines=${layout.lineCount}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
        }
    }

    @Test fun populatedPhoneDashboardKeepsHabitActionBeforeMoney() {
        compose.setContent {
            NeoBrutalismTheme {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    DashboardContent(dashboardPreviewState(), onHabits = {}, onMoney = {}, onAddEntry = {}, onToggleHabit = {})
                }
            }
        }
        compose.onNodeWithTag("dashboard_habit_1").assertIsDisplayed()
        captureDashboard("dashboard-populated.png")
        compose.onNodeWithText("Groceries").performScrollTo().assertIsDisplayed()
        captureDashboard("dashboard-recent-spending.png")
    }

    private fun captureDashboard(filename: String) {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        File(cache, filename).outputStream().use { output ->
            compose.onNodeWithTag("dashboard").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
