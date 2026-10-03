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

## Money tracker

The money tracker has three sections in one flat tab strip: Spending, Accounts, and Money owed. `ExpenseScreen` coordinates editors and dialogs; `FinanceSections` renders each section. One compact Add entry action opens `TransactionSheet` with Expense, Income, and Transfer choices, or Lent/Borrowed choices in Money owed. Changing type retains common fields, chooses a matching category, and saves only applicable fields. Spending lists all three entry types; a single Filter menu contains account/type choices and the month label opens the calendar. Its spending/income totals exclude transfers. Accounts contains balances and editable transfers; tap an account to edit. Money owed contains loans and their editable repayments. Forms group category, account, and date into compact rows, with optional notes and one prominent Save action. Category management is inside the category menu. `NeoTextTabs`, `NeoInlineChoice`, `NeoColumnCard`, and `NeoConfirmDialog` reuse the application's neobrutalism borders, fills, shadows, and typography.

`ExpenseRepositoryImpl` validates and saves a transaction inside a Room transaction. The existing `expenses` table and `Expense` model now represent typed money movements; dashboard expense queries still include only `EXPENSE` records.

- Store amounts as `Long` hundredths (`amountMinor`, `openingMinor`). Use `Money` for parsing and formatting; do not calculate balances with `Double`.
- Income adds to an account; expenses subtract from it. Transfers are a single record that debits one account and credits another, so they cannot be half-saved.
- Lending subtracts from an account; borrowing adds to it. Neither counts as spending or income. Repayments reference the original loan and update its outstanding amount. Validation prevents overpayment, invalid dates, and mismatched repayment directions.
- Derive account balances and outstanding amounts from records using `Ledger`. Editing or deleting a record immediately recalculates them. A loan with repayments cannot be deleted until its linked repayments are removed.
- Categories belong to income or expenses. Soft deletion preserves historical category names. Account names and opening balances can be edited; accounts are not deleted.
- All accounts use one explicitly persisted currency. Changing the currency changes the unit label, with confirmation; it does not perform exchange conversion. Supported currencies use two decimal places.
- Accepted writes finish after navigation. Forms close only after successful saving and show validation/save errors inline.

Database version 5 migrates existing expenses into the Cash account and rounds legacy decimal amounts once to hundredths. It retains IDs, notes, dates, categories, habits, and completion history. There is no destructive migration fallback.

## Verification

Run `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline` on Windows when dependencies are cached. Omit `--offline` when dependencies need downloading.

Unit tests cover recurrence dates, period boundaries, streaks, date rollover, money precision, and ledger arithmetic. `FinanceDatabaseTest` validates the real Room migration and finance rules; `FinanceUiTest` exercises account creation, transfers, editing, and partial repayments on an emulator. Build their APK with `:app:assembleDebugAndroidTest` and run them on an explicitly selected test device. Screen-off reminder delivery, reboot restoration, and permission changes require separate device tests.
