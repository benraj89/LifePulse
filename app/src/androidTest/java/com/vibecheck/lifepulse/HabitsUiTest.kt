package com.vibecheck.lifepulse

import android.app.AlarmManager
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.habits.*
import com.vibecheck.lifepulse.ui.neobrutalism.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactHabitListKeepsFiveHabitsVisibleAndToggleable() {
        var toggledId: Long? = null
        val habits = listOf("Read a few pages", "Take a walk", "Drink water", "Stretch", "Write a journal")
            .mapIndexed { index, title -> Habit((index + 1).toLong(), title, HabitFrequency.DAILY, 0,
                completedToday = index == 0, currentStreak = if (index == 0) 5 else 0) }
        compose.setContent { NeoBrutalismTheme {
            Box(Modifier.width(360.dp).height(640.dp)) {
                HabitContent(HabitUiState(habits = habits, isLoading = false), {}, { toggledId = it }, {})
            }
        } }
        compose.onNodeWithText("Write a journal").assertIsDisplayed()
        val target = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "comic-habits.png")
        target.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("habit_5"))).performClick()
        compose.runOnIdle { assertEquals(5L, toggledId) }
    }

    @Test fun deletingAHabitRequiresConfirmationAndCanBeCancelled() {
        var deleted = false
        var toggled = false
        compose.setContent { NeoBrutalismTheme { Surface {
            Box(Modifier.width(360.dp).padding(16.dp)) {
                HabitRow(Habit(1, "Read a few pages", HabitFrequency.DAILY, 0), onToggle = { toggled = true }, onDelete = { deleted = true })
            }
        } } }
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle { assertTrue(toggled) }
        compose.onNodeWithContentDescription("Actions for Read a few pages").performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.onNodeWithText("Delete habit?").assertIsDisplayed()
        compose.runOnIdle { assertFalse(deleted) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(deleted) }
        compose.onNodeWithContentDescription("Actions for Read a few pages").performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }

    @Test fun weeklyReminderPreservesDayAndTimeWhenCreatingHabit() {
        var captured: Habit? = null
        compose.setContent { NeoBrutalismTheme {
            AddHabitDialog({}) { title, frequency, hour, minute, day, monthDay ->
                captured = Habit(1, title, frequency, 0, reminderHour = hour, reminderMinute = minute,
                    reminderDayOfWeek = day, reminderDayOfMonth = monthDay)
            }
        } }
        compose.onNode(hasSetTextAction()).performTextInput("  Plan the week  ")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Weekly").performClick()
        compose.onNodeWithContentDescription("Remind me").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Wednesday").performScrollTo().performClick()
        compose.onNodeWithText("Add habit").performClick()
        compose.runOnIdle {
            assertEquals("Plan the week", captured?.title)
            assertEquals(HabitFrequency.WEEKLY, captured?.frequency)
            assertEquals(3, captured?.reminderDayOfWeek)
            assertEquals(9, captured?.reminderHour)
            assertEquals(0, captured?.reminderMinute)
            assertNull(captured?.reminderDayOfMonth)
        }
    }

    @Test fun reminderCanBeSavedWithoutExactAlarmAccessAtLargeFontSize() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeFalse(context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())
        var captured: Habit? = null
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                NeoBrutalismTheme {
                    AddHabitDialog({}) { title, frequency, hour, minute, day, monthDay ->
                        captured = Habit(1, title, frequency, 0, reminderHour = hour, reminderMinute = minute,
                            reminderDayOfWeek = day, reminderDayOfMonth = monthDay)
                    }
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Optional alarm access")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithContentDescription("Remind me").performScrollTo().performClick()
        compose.onNodeWithText("Open alarm settings").performScrollTo().assertIsDisplayed()
        val capture = File(context.cacheDir, "pockdo-exact-alarm-hint.png")
        capture.outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Add habit").performClick()
        compose.runOnIdle {
            assertEquals("Optional alarm access", captured?.title)
            assertEquals(9, captured?.reminderHour)
            assertEquals(0, captured?.reminderMinute)
        }
    }

    @Test fun loadingHabitsDoesNotShowEmptyStateOrEnableCreation() {
        compose.setContent { NeoBrutalismTheme { HabitContent(HabitUiState(), {}, {}, {}) } }
        compose.onNodeWithText("Loading your habits…").assertIsDisplayed()
        compose.onNodeWithText("No habits yet").assertDoesNotExist()
        compose.onNodeWithText("Add habit").assertIsNotEnabled()
    }
}
