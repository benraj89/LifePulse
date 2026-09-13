# LifePulse — Architecture & Interview Walkthrough

A single reference doc explaining **how the app works end-to-end**, so you can
confidently talk through it in an interview: architecture, data flow, DI,
navigation, and the tricky bugs you fixed (great "tell me about a bug you
solved" material).

---

## 1. One-Line Pitch

> "LifePulse is an offline-first Android app that merges a Habit Tracker and
> an Expense Tracker into one Dashboard. It's built with Kotlin, Jetpack
> Compose (Material 3), MVVM + Clean Architecture, Room for persistence,
> Hilt for DI, Kotlin Flow/StateFlow for reactive state, and WorkManager for
> a daily local reminder notification."

---

## 2. High-Level Architecture

```
UI (Compose Screens)  ──observes──►  ViewModel (StateFlow)
        │                                   │
        │ user actions (click/toggle)       │ calls suspend fns
        ▼                                   ▼
   NavHost (routes)                   Repository (interface = domain, impl = data)
                                              │
                                              ▼
                                        Room DAO (Flow<T> queries)
                                              │
                                              ▼
                                          SQLite (Room DB)
```

This is **MVVM + Clean Architecture** split into 3 layers:

| Layer | Package | Responsibility |
|---|---|---|
| **UI** | `ui/*` | Compose screens, ViewModels, navigation. Knows nothing about Room. |
| **Domain** | `domain/*` | Pure Kotlin: `model` (data classes), `repository` (interfaces), `usecase` (business logic like streak calc). No Android/Room imports. |
| **Data** | `data/*` | Room entities, DAOs, `LifePulseDatabase`, and `*RepositoryImpl` classes that implement the domain interfaces. |

**Why this split matters (interview answer):** the domain layer defines
*contracts* (`HabitRepository`, `ExpenseRepository`) that the UI depends on.
The data layer is swappable (e.g., you could add a remote API later) without
touching ViewModels or Composables — classic dependency inversion.

---

## 3. Dependency Injection (Hilt)

- `LifePulseApp : Application()` is annotated `@HiltAndroidApp` — the root of
  the DI graph.
- `di/DatabaseModule.kt` — provides the singleton `LifePulseDatabase` (Room)
  and each DAO (`HabitDao`, `CategoryDao`, `ExpenseDao`).
- `di/RepositoryModule.kt` — binds `HabitRepositoryImpl → HabitRepository`
  and `ExpenseRepositoryImpl → ExpenseRepository` (typically via `@Binds`).
- ViewModels are annotated `@HiltViewModel` and injected with repositories
  via constructor injection.
- Composables fetch the ViewModel with `hiltViewModel()` — Hilt+Navigation
  Compose automatically scopes the ViewModel to the current back-stack entry
  (nav-graph scoped), so it survives configuration changes but is recreated
  when you navigate away and back (this matters for the "why did my state
  reset" type questions).
- `HabitReminderWorker` is a `@HiltWorker` — Hilt provides a custom
  `HiltWorkerFactory` wired in `LifePulseApp` via
  `Configuration.Provider.workManagerConfiguration`, so the Worker itself can
  receive `HabitRepository` via `@AssistedInject`.

---

## 4. Database Layer (Room)

### Entities
| Entity | Table | Key fields |
|---|---|---|
| `HabitEntity` | `habits` | `id` (PK, autoGenerate), `title`, `frequency`, `createdAt` |
| `HabitLogEntity` | `habit_logs` | `id`, `habitId` (FK → habits.id, `CASCADE` delete), `completedDate` ("yyyy-MM-dd" string) |
| `CategoryEntity` | `categories` | `id`, `name`, `colorHex` |
| `ExpenseEntity` | `expenses` | `id`, `categoryId` (FK → categories.id, `CASCADE`), `amount`, `dateTimestamp` (epoch millis), `note` |

Storing `completedDate` as a **string key** (`DateUtils.toKey()` = ISO
`yyyy-MM-dd`) instead of a timestamp makes "did I complete this habit today"
queries trivial (`WHERE completedDate = :date`) and timezone-safe.

### DAOs return `Flow<T>`
Room + Kotlin coroutines: any `@Query` that returns `Flow<T>` is
**automatically re-run and re-emitted** whenever a table it touches changes
(Room tracks this via an `InvalidationTracker` registered on the underlying
tables). This is *the* mechanism that makes the whole app "reactive" without
manual refresh calls:

```kotlin
@Query("""
    SELECT h.*, (SELECT COUNT(*) FROM habit_logs l
                 WHERE l.habitId = h.id AND l.completedDate = :date) > 0 AS completedToday
    FROM habits h ORDER BY h.createdAt DESC
""")
fun observeHabitsWithStatus(date: String): Flow<List<HabitWithTodayStatus>>
```

Toggling a habit inserts/deletes a row in `habit_logs` → Room notices
`habit_logs` changed → re-runs this query → emits new list → ViewModel
`.map{}` → `StateFlow` → Compose recomposes. **No manual "refresh" anywhere.**

### Repository layer
`HabitRepositoryImpl` / `ExpenseRepositoryImpl` implement the domain
interfaces, map Room entities → domain models (`Habit`, `Expense`, ...), and
inject the `CalculateStreakUseCase` to compute the streak per habit.

### Seeding default data
`DatabaseModule` uses `Room.databaseBuilder(...).addCallback(...)`, where
`onCreate()` runs **once**, the very first time the DB file is created, to
insert default expense categories (Food, Transport, ...) using
`INSERT OR IGNORE`.

---

## 5. State Management (StateFlow + Combine)

Each screen has exactly one `ViewModel` exposing a single
`StateFlow<UiState>` — a **single source of truth** pattern.

Example — `ExpenseViewModel` combines 5 independent flows into one UI state:

```kotlin
val uiState: StateFlow<ExpenseUiState> = combine(
    expenseRepository.observeCategories(),
    expenseRepository.observeRecentExpenses(limit = 100),
    expenseRepository.observeTotalInRange(startOfDay, endOfDay),
    expenseRepository.observeTotalInRange(startOfMonth, endOfMonth),
    expenseRepository.observeTotalsByCategory(startOfMonth, endOfMonth)
) { categories, recent, totalToday, totalMonth, byCategory ->
    ExpenseUiState(categories, recent, totalToday, totalMonth, byCategory, isLoading = false)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())
```

Key interview talking points:
- **`combine`** — merges N flows, re-emits whenever *any* upstream emits.
- **`stateIn` + `SharingStarted.WhileSubscribed(5_000)`** — turns a cold
  `Flow` into a hot `StateFlow` that's shared across recompositions/rotation,
  but stops collecting from Room 5s after the last observer disappears (saves
  battery/CPU when the screen isn't visible), and restarts instantly if the
  user comes back within that window.
- The Dashboard's `DashboardViewModel` follows the exact same pattern,
  combining habit-progress flows + expense flows into one
  `DashboardUiState` — this is *why* completing a habit or adding an expense
  on other screens instantly updates the Dashboard: they all read from the
  same Room tables, and Flow propagates the change automatically.
- UI layer collects with `collectAsStateWithLifecycle()` (not the plain
  `collectAsState()`) — this pauses collection when the app is backgrounded,
  which is the recommended, lifecycle-aware way to bridge Flow → Compose
  `State`.

---

## 6. Navigation (Compose Navigation)

`ui/navigation/LifePulseNavHost.kt`:

- 3 top-level tabs modeled as a sealed class `Destination` (`Dashboard`,
  `Habits`, `Expenses`), each with a route string, label resource, and icon.
- A single `NavHost` + Material 3 `NavigationBar` (bottom bar) wraps all
  three `composable(route) { Screen() }` destinations.
- **`navigateToTab()`** helper encapsulates the standard "bottom nav" pattern:

```kotlin
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

  - `popUpTo(startDestination) { saveState = true }` — clears everything
    above the start destination but **saves** its UI state before removing.
  - `launchSingleTop = true` — don't push a second copy of the same
    destination if it's already on top.
  - `restoreState = true` — when navigating back to a tab, restore its saved
    scroll/UI state instead of recreating from scratch.
  - Used both by the bottom bar items **and** by in-screen "See all →"
    buttons (Dashboard → Habits / Dashboard → Expenses), so the back stack
    behavior is identical no matter which UI triggers the navigation.

---

## 7. Screens

### `DashboardScreen`
- Top `SummaryCard`: habit progress fraction (`LinearProgressIndicator`) +
  today's / this month's spend, sourced from `DashboardViewModel`'s combined
  `StateFlow`.
- Quick actions: "Add Expense" (opens `AddExpenseSheet` bottom sheet) and
  "All Habits" (navigates to the Habits tab).
- Two `LazyColumn` sections (today's habits preview, recent expenses
  preview), each with an `EmptyState` fallback and a "See all" header button.

### `HabitScreen`
- `LazyColumn` checklist, one `HabitRow` per habit with a `Checkbox` bound to
  `habit.completedToday`.
- Streak label built from `CalculateStreakUseCase` (walks backward from
  today/yesterday counting consecutive `completedDate`s).
- FAB opens `AddHabitDialog` (title text field + frequency chips:
  Daily/Weekly/Monthly).

### `ExpenseScreen`
- Summary row (today / this month totals).
- `LazyColumn` of recent transactions (category color dot, note, formatted
  timestamp, amount, delete icon).
- FAB opens `AddExpenseSheet` (amount, category dropdown, optional note).

### Shared components (`ui/components`, `ui/neobrutalism`)
- `AddExpenseSheet`, `EmptyState`, `ColorDot` — reusable across Dashboard and
  ExpenseScreen.
- A custom "neo-brutalist" design system (`NeoCard`, `NeoButton`, `NeoChip`,
  `NeoColors`, `NeoTypography`) — a thin Material 3 wrapper giving bold
  borders/offset shadows instead of standard elevation, kept in its own
  package so it can be swapped without touching business logic.

---

## 8. Background Work — Daily Reminder (WorkManager + Hilt)

- `worker/ReminderScheduler.kt` schedules a **unique periodic** work request
  (`PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)`) via
  `WorkManager.enqueueUniquePeriodicWork(WORK_NAME, KEEP, request)` —
  `ExistingPeriodicWorkPolicy.KEEP` makes scheduling **idempotent**: calling
  it again (e.g., every `Application.onCreate()`) won't duplicate or reset
  the schedule.
- `worker/HabitReminderWorker.kt` (`@HiltWorker`, `CoroutineWorker`):
  1. Reads `total` habit count and `completed` count for today directly from
     the repository (`.first()` on the Flow — one-shot read).
  2. If not all habits are done, builds and shows a notification (channel
     created lazily, permission-checked for API 33+).
  3. Returns `Result.success()` or `Result.retry()` on failure.
- Wired in `LifePulseApp.onCreate()`:
  ```kotlin
  override val workManagerConfiguration = Configuration.Builder()
      .setWorkerFactory(workerFactory) // Hilt-provided
      .build()
  reminderScheduler.scheduleDailyReminder(hour = 20, minute = 0)
  ```
- `MainActivity` requests `POST_NOTIFICATIONS` permission at runtime for
  Android 13+ before the worker ever tries to notify.

### 8.1 When exactly does the notification fire?

**Scheduled for 8:00 PM (20:00) local device time, every day** —
set via `reminderScheduler.scheduleDailyReminder(hour = 20, minute = 0)` in
`LifePulseApp.onCreate()`.

**How the first-run delay is computed** (`ReminderScheduler.initialDelay`):
```kotlin
private fun initialDelay(hour: Int, minute: Int): Duration {
    val now = LocalDateTime.now()
    var target = now.with(LocalTime.of(hour, minute))     // today at 20:00
    if (!target.isAfter(now)) target = target.plusDays(1) // already past -> tomorrow
    return Duration.between(now, target)
}
```
- Before 8 PM today → first fire is **today at 8 PM**.
- After/at 8 PM today → first fire is **tomorrow at 8 PM**.
- After the first fire, `repeatInterval = Duration.ofDays(1)` repeats it
  roughly every 24h from then on.
- `enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)`
  is called on **every app cold start**, so each launch re-derives the delay
  against "now" and updates the existing unique work — self-healing if the
  schedule ever drifts, without creating duplicate periodic jobs.

**Whether it actually shows a notification** (`HabitReminderWorker.doWork()`):
- Reads `total` habit count and `completed` count for **today** in one shot
  (`.first()` on the repository Flows).
- If `total > 0 && completed < total` → shows the reminder notification
  ("you haven't completed all your habits today").
- If all habits are already done, or there are zero habits → **skips**
  silently and returns `Result.success()`.
- On any thrown error → returns `Result.retry()` (WorkManager backs off and
  retries automatically).
- Requires `POST_NOTIFICATIONS` permission granted on API 33+, otherwise the
  notification call is a no-op (checked in `showNotification()`).

**Caveats worth knowing (interview-relevant):**
- WorkManager periodic work is **best-effort, not exact-alarm**. Android
  (especially under Doze/battery optimization) can delay the fire by
  several minutes to longer — this is *not* the same guarantee as
  `AlarmManager.setExactAndAllowWhileIdle`.
- No flex interval is set, so each subsequent run is ~24h after the
  *previous actual execution time* — if one day's run drifts late, that
  drift can compound slightly day over day. Re-deriving the delay on every
  app open (as done here) mitigates this in practice.
- Changing the time only requires editing the
  `scheduleDailyReminder(hour = X, minute = Y)` call — a Settings screen
  could later expose this as a user preference (e.g., stored in
  DataStore/SharedPreferences and re-read before scheduling).

---

## 9. Common Interview Q&A Cheat-Sheet

**Q: Why Room + Flow instead of LiveData?**
A: Flow is Kotlin-native, composes well with `combine`/`map`/`stateIn`, and
integrates directly with coroutines used everywhere else (ViewModel,
Worker). `StateFlow` also has a **current value** (`.value`), unlike
LiveData, which simplifies testing and one-shot reads.

**Q: How do habit completion and expense entry update the Dashboard
instantly?**
A: Because `DashboardViewModel` combines the *same* Room-backed Flows
(`observeHabitsForDate`, `observeTotalInRange`, etc.) that `HabitScreen` and
`ExpenseScreen` use. Room's invalidation tracker detects the table write
(INSERT/DELETE) and reruns every active Flow query bound to that table —
there is no manual "notify dashboard" step required.

**Q: How is offline-first achieved?**
A: There's no remote backend at all — Room/SQLite is the single source of
truth on-device. All reads/writes are local; there's nothing to "sync",
which is the simplest form of offline-first.

**Q: How would you add multi-day/weekly habit support properly?**
A: `HabitFrequency` already exists as an enum; the streak use case currently
assumes daily cadence. You'd extend `CalculateStreakUseCase` to accept the
frequency and adjust the "is broken" window (e.g., allow a 7-day gap for
weekly habits) and adjust `observeHabitsWithStatus`'s "did you do it in the
current period" check accordingly.

**Q: How would you test this?**
A: Domain layer (`CalculateStreakUseCase`) is pure Kotlin — trivial JUnit
tests with fake date lists. Repositories can be tested with an in-memory
Room DB (`Room.inMemoryDatabaseBuilder`). ViewModels can be tested by
injecting fake repository implementations and asserting on
`uiState.value` after advancing a `TestDispatcher`.

**Q: Why split strings into `strings.xml`?**
A: Testability (Espresso/Compose UI tests can assert against resource IDs
instead of brittle hardcoded text), localization support, and
single-source-of-truth for copy changes without touching Kotlin files.

---

## 10. Known Pitfalls Hit During Development (great debugging stories)

### A. `IllegalArgumentException: Key "X" was already used` in `LazyColumn`
- **Symptom:** crash after adding a habit/expense and navigating, or after
  app restart.
- **Root cause:** `LazyColumn`'s `key = { it.id }` requires every id in the
  *currently composed list* to be unique **at every point in time**,
  including mid-recomposition. If a Room `Flow` briefly emits a stale list
  (old item still present) overlapping with a newly emitted list containing
  an item with a **reused primary key** (SQLite reuses `rowid`s after a row
  with the max id is deleted, since `HabitEntity`/`ExpenseEntity` don't
  declare `@PrimaryKey(autoGenerate = true)` with the `AUTOINCREMENT` SQLite
  keyword — Room's `autoGenerate = true` maps to plain `INTEGER PRIMARY KEY`,
  which **does** reuse ids), you can transiently get two entries in the
  composed list sharing a key.
- **Fix approach:**
  1. Prefer composite/prefixed keys in shared lists to avoid cross-section
     collisions (already done on Dashboard: `"habit_${it.id}"` /
     `"expense_${it.id}"`).
  2. Guarantee monotonically increasing ids by forcing real SQLite
     `AUTOINCREMENT` (`@Entity(... )` + explicit `INTEGER PRIMARY KEY
     AUTOINCREMENT` via `@ColumnInfo`/raw SQL, or simply never hard-delete +
     reuse ids in tests).
  3. As a defensive fix in Compose, fall back to `key = { index -> ... }` or
     `key(habit.id, habit.hashCode())` only if true uniqueness can't be
     guaranteed — but the correct long-term fix is uniqueness at the DB
     level, not papering over it in the UI.

### B. Navigating back to Dashboard sometimes "did nothing"
- **Root cause:** clicking a bottom-bar item that's already conceptually
  "current" relies on `currentBackStackEntryAsState().value?.destination?.route`
  matching exactly. If a dialog/bottom sheet was still on screen and
  intercepting input, or if `currentRoute` momentarily lagged one frame
  behind an in-flight navigation, the `selected` state and the click did not
  line up.
- **Fix approach:** ensure sheets/dialogs are dismissed (`showSheet = false`)
  synchronously in the `onSave`/`onDismiss` callback *before* triggering
  navigation, and rely on `launchSingleTop` + `restoreState` (already
  applied) so re-tapping a tab is always safe/idempotent.

### C. Hardcoded strings
- Every user-facing string was extracted into `res/values/strings.xml` and
  referenced via `stringResource(R.string.xxx)` — improves testability,
  localization-readiness, and lets designers/PMs tweak copy without a code
  change.

---

## 11. File / Package Map (quick reference)

```
app/src/main/java/com/example/lifepulse/
├── LifePulseApp.kt              # @HiltAndroidApp, schedules WorkManager
├── MainActivity.kt              # hosts Compose content + notif permission
├── core/
│   └── DateUtils.kt             # date <-> "yyyy-MM-dd" key helpers
├── data/
│   ├── local/
│   │   ├── LifePulseDatabase.kt
│   │   ├── dao/ (HabitDao, CategoryDao, ExpenseDao)
│   │   ├── entity/ (HabitEntity, HabitLogEntity, CategoryEntity, ExpenseEntity)
│   │   └── relation/ (HabitWithTodayStatus, ExpenseWithCategory, CategoryTotal)
│   └── repository/ (HabitRepositoryImpl, ExpenseRepositoryImpl)
├── domain/
│   ├── model/Models.kt          # Habit, Category, Expense, CategorySpending
│   ├── repository/              # HabitRepository, ExpenseRepository (interfaces)
│   └── usecase/CalculateStreakUseCase.kt
├── di/
│   ├── DatabaseModule.kt        # Room + DAO providers, default category seed
│   └── RepositoryModule.kt      # binds impl -> interface
├── ui/
│   ├── navigation/LifePulseNavHost.kt
│   ├── dashboard/ (DashboardScreen, DashboardViewModel)
│   ├── habits/ (HabitScreen, HabitViewModel)
│   ├── expenses/ (ExpenseScreen, ExpenseViewModel)
│   ├── components/ (AddExpenseSheet, EmptyState, ColorDot)
│   ├── neobrutalism/ (NeoCard, NeoButton, NeoChip, NeoColors, NeoTypography)
│   └── theme/
└── worker/
    ├── HabitReminderWorker.kt   # @HiltWorker, CoroutineWorker
    └── ReminderScheduler.kt     # enqueueUniquePeriodicWork
```

---

## 12. 30-Second Elevator Summary (memorize this)

> "LifePulse follows MVVM with a Clean-Architecture split into UI, domain,
> and data layers. Room DAOs expose Kotlin `Flow`s that auto-invalidate on
> writes; each screen's ViewModel `combine`s the flows it needs into one
> `StateFlow<UiState>` via `stateIn(WhileSubscribed(5000))`, and Compose
> collects it with `collectAsStateWithLifecycle()`. Hilt wires the whole
> graph — database, repositories, ViewModels, and even the WorkManager
> worker that fires a daily habit reminder. Because the Dashboard and the
> individual Habit/Expense screens all read from the same Room tables,
> completing a habit or logging an expense anywhere in the app is reflected
> everywhere else instantly, with zero manual refresh code — that's the core
> architectural win of this app."

