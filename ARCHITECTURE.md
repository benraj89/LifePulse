# LifePulse developer guide

LifePulse uses Compose, ViewModels, Room, and Hilt. Keep business rules in small Kotlin helpers; add a layer only when it owns behavior that an existing class cannot clearly own.

## Where to make changes

| Change | Location |
| --- | --- |
| Screen layout and interactions | `ui/habits`, `ui/dashboard`, `ui/expenses` |
| Shared visual components | `ui/neobrutalism`, `ui/components` |
| Screen state and user actions | The screen's `ViewModel` |
| Database reads, writes, and model mapping | `data/repository` |
| SQL and transactional completion toggles | `data/local/dao` |
| Daily, weekly, and monthly period boundaries | `core/HabitPeriod.kt` |
| Streak calculation | `domain/usecase/CalculateStreakUseCase.kt` |
| Reminder date calculation | `core/ReminderTimeCalculator.kt` |
| Android alarm registration | `worker/ReminderScheduler.kt` |
| Notification channel, delivery, and deduplication | `notification/ReminderNotifier.kt` |

## Habit flow

Screens collect their ViewModel's state. The ViewModel observes a repository flow; Room updates it after database writes. `currentDateFlow` refreshes date-dependent queries at midnight and checks for clock or timezone changes once a minute while collected. Completion actions read the current date when executed.

Daily habits use one calendar day. Weekly habits use Monday–Sunday. Monthly habits use a calendar month. Completion remains checked for that period; streaks count consecutive completed periods. An unfinished current period does not break a streak. Existing completion dates are preserved and multiple logs in one period count once.

`HabitActions` owns an application-lifetime coroutine scope for adding habits: insert the habit, then schedule its reminder. This survives screen navigation, but not process termination. Foreground and periodic reminder sync repair reminders for saved habits.

## Reminder flow

1. `ReminderScheduler` registers the next occurrence for each habit.
2. `ReminderAlarmReceiver` schedules the following occurrence before delivering the current one.
3. `ReminderNotifier` serializes alarm and catch-up delivery and records handled occurrences.
4. `ReminderBootReceiver` restores alarms after reboot, updates, or clock changes.
5. `ReminderSyncWorker` repairs scheduling and catches up missed occurrences within 30 minutes.

Both receivers use `runReminderWork` to hold a short wake lock, bound execution time, finish the broadcast, and queue recovery on failure. Completion checks use a small database lookup rather than loading UI state and calculating streaks.

Keep the scheduler, notifier, receivers, and sync worker separate: each has a different Android responsibility. Exact scheduling requires permission; the fallback is inexact. Completed habits suppress reminders for their current period. Delivery cannot be guaranteed while powered off or force-stopped.

## Verification

Run `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline` on Windows when dependencies are cached. Omit `--offline` when dependencies need downloading.

Unit tests cover recurrence dates, period boundaries, streaks, and date rollover. Screen-off delivery, reboot restoration, and permission changes require device tests.
