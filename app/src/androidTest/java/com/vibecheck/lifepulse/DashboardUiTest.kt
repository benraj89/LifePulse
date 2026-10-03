package com.vibecheck.lifepulse

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.dashboard.*
import com.vibecheck.lifepulse.ui.neobrutalism.NeoBrutalismTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

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
        compose.onNodeWithText("YOUR MONEY · OCT").performClick()
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
}
