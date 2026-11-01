# POCKDO first-release review

Reviewed 1 November 2026. Technical checks support an **internal-testing upload**.
Production release still depends on Play Console setup, required testing and
the store/device checks below. No Play Console upload or submission was performed.
The owner confirmed an **older personal developer account**. The special closed-test
requirement for personal accounts created after 13 November 2023 does not normally
apply to this account category; verify the account's actual eligibility in Console.

## First public database baseline

- Room schema version **1**, file **pockdo.db**, all six current tables retained.
- Development migration code and old schema snapshots removed; the newly generated
  `app/schemas/com.vibecheck.lifepulse.data.local.LifePulseDatabase/1.json` is the
  version 1 baseline to commit and preserve.
- Existing development installs start with empty records because the old file
  `lifepulse.db` is no longer opened. It is not automatically deleted. Old currency
  preferences do not confirm the fresh database's currency.
- The fresh database seeds its Cash account and all seven built-in expense
  categories. The seed SQL now includes the required `isDeleted` column explicitly.
- Backup file format stays at **version 2**, independently of the Room schema
  version. Existing supported finance backups remain importable; they do not
  include habit history. No destructive-migration fallback is enabled.
- After distributing this baseline, preserve `pockdo.db` and schema 1. Future
  table changes need an incremented schema version and tested migration paths.
  Do not reset the baseline or rename the database again for normal updates.

[Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions).

## Verified locally

| Check | Result |
| --- | --- |
| App identity | POCKDO, `com.pockdo.daily`, versionName 1.0, versionCode 1 |
| Android compatibility | minSdk 26, compile/target SDK 36 |
| JVM tests | 69 passed |
| Android database/backup tests | 35 passed on the Android 17 emulator, including production database creation/seeding/reopening, currency locks and backup round trips/invalid-data rollback |
| Android lint | 0 errors; 168 warnings, mostly unused resources and dependency/tooling update suggestions |
| Build outputs | Debug APK, test APK, signed release APK and signed release AAB built successfully |
| Release launch | Signed APK cold launch succeeded; currency confirmation appeared, empty dashboard loaded, policy was readable and crash sharing was off |
| Signing | AAB JAR signature and APK v2 signature verified; both match the existing upload certificate |
| Packaged data | No database, keystore or signing-properties files in the AAB; offline privacy-policy asset present |
| Release manifest | Not debuggable/test-only; no Analytics/Messaging components, advertising-ID/ad-services permissions or USE_EXACT_ALARM; SCHEDULE_EXACT_ALARM present |
| Native compatibility | All four packaged graphics-path libraries have 16 KB ELF LOAD alignment; release APK passed `zipalign -c -P 16 -v 4` |
| Git secrets | keystore.properties is ignored; no private keystore/key files tracked |

The initial combined build/lint invocation hit a generated-Hilt-source analysis
race. Building first, then running `:app:lintDebug` separately passed without
disabling checks. The test-framework incompatibility with Android 17 affects
Espresso UI tests; the 35 pure database tests ran successfully there. The six UI
tests passed previously on Android 15; no interface behavior changed in this reset.

Upload certificate SHA-256:
`92:39:65:AD:53:FA:9D:EA:4B:50:D5:92:8C:D4:87:F9:DA:7A:AB:A5:65:E0:49:05:D6:8A:17:A0:D4:7B:89:B7`.

Artifact: `app/build/outputs/bundle/release/app-release.aab`.
Rebuild it after any further app changes. Once versionCode 1 has been used in
Play Console, increment the code for later distinct uploads. Do not regenerate
the upload key. Keep a secure separate backup of the existing key and credentials.

[Target API requirements](https://developer.android.com/google/play/requirements/target-sdk),
[16 KB compatibility guidance](https://developer.android.com/guide/practices/page-sizes).

## Complete in Play Console

1. Create/select POCKDO and enroll in Play App Signing with the intended signing
   strategy. Upload the AAB to **Internal testing** first, and inspect processing
   errors, SDK warnings, device compatibility and the pre-launch report.
2. Save the public policy URL:
   https://sites.google.com/view/pockdo-privacy-benra
   It was fetched without signing in and all policy headings/paragraphs matched
   the in-app asset. Keep the Google Sites copy and asset synchronized.
3. Complete Data safety using `privacy-and-data-safety.md`. Optional Crashlytics
   diagnostics, identifiers and Sessions metrics still need disclosure.
4. Complete app access, ads, target audience, content rating, Financial features,
   Health apps and any other declarations shown by Console. Current code has no
   sign-in or ad SDK. Choose the actual intended audience and match the listing.
5. Describe finance features as **manual record-keeping**. Recording lending,
   borrowing or transfers does not actually originate loans, connect bank accounts
   or move money. Review the Financial features form's Other option for this
   functionality; do not misrepresent POCKDO as a lender or loan facilitator.
6. The current app is a general habit tracker with no medical guidance, health
   sensors or Health Connect access. Complete the Health apps declaration based
   on actual functionality and store claims; revisit it if health features are added.
7. Add the store title, descriptions, support email, screenshots, icon/feature
   graphic, countries and pricing. All Play Console declarations and production
   access remain unverified from this workspace.

[Financial declaration](https://support.google.com/googleplay/android-developer/answer/13849271?hl=en-en),
[Health declaration](https://support.google.com/googleplay/android-developer/answer/14738291?hl=en).

## Before production

- On Play-installed test builds, verify reminders with notification permission
  allowed/denied, exact-alarm access allowed/denied, screen off/Doze, reboot and
  time-zone changes on representative real devices. Exact access improves timing
  but is not a guarantee against every device restriction.
- Verify Crashlytics receipt in the correct Firebase release app using a controlled
  test crash after opting in. Switch persistence is tested locally; Firebase
  Console receipt has not been verified here.
- Test flexible/immediate updates through Google Play with two compatible version
  codes. A first release and sideloaded APK alone cannot prove Play update delivery.
- Install through the internal track, create sample habits/finance entries, export
  and restore a finance backup, and review the pre-launch report. Static 16 KB
  checks passed; a dedicated 16 KB device/emulator run was not performed.
- For this older personal account, follow Console's production eligibility and
  verification requirements. The 12-testers/14-day closed-test rule applies to
  personal accounts created after 13 November 2023. Internal testing is still
  recommended for this first release before production review.

[Testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).
