# POCKDO privacy and Play Data safety

Publisher: **Ben Raj**. Public support/privacy email: **benraj8887@gmail.com**.
Applies to the first release of `com.pockdo.daily` with only Firebase Crashlytics.

## Policy URL: required before submission

The complete policy is `app/src/main/assets/privacy-policy.html`. This is the
single source used by the native Privacy & support screen. The same file is a
standalone web page: upload it unchanged to a static HTTPS host, such as GitHub
Pages, and put its actual public URL in Play Console's Privacy policy field.
No domain purchase is needed. A local file path is not a public policy URL.
See [GitHub's Pages setup instructions](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site).

For GitHub Pages, create a public repository for the policy, upload this file as
`index.html` at its root, and configure Pages to deploy the root of that branch.
Use the URL that GitHub actually provides. Open it while signed out, including on
a phone, and confirm the whole policy loads without a login or regional restriction.
Do not upload signing keys, keystore.properties, or the Android project just to
host this page. If you change the policy, update this asset and the hosted copy
together. Review the date and description against the app you actually submit.

Nothing has been published by this change. No placeholder URL is shipped. The
in-app policy works offline through Dashboard > Privacy & support.

Google requires a public, active, non-geofenced policy URL and a policy link or
text in the app. It must identify the app/developer and describe data handling,
security, retention, deletion and contact details; a PDF is not accepted.
[Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).

## SDK configuration and choices

- Google Analytics and Firebase Cloud Messaging dependencies are removed.
- Crashlytics remains, with automatic collection disabled in the manifest.
- The in-app Share crash reports switch persists the user's choice through
  Crashlytics. Off takes full effect on the next process start. Pending reports
  are discarded at startup when sharing is off. The UI explains this delay.
- Do not add financial values, habit names, person names, user IDs or free-form
  notes to SDK logs/custom keys. Check exception messages for unintended content.
- Firebase Installations and Sessions are transitive dependencies. Removing
  Analytics does not remove Crashlytics' app-quality session telemetry.
- Android automatic backup/device transfer remains enabled and is disclosed.
  User-selected .mbak/CSV exports can go to local or cloud file providers.

[Firebase collection controls](https://firebase.google.com/docs/reference/android/com/google/firebase/crashlytics/FirebaseCrashlytics).

## Data safety answers to review in Play Console

These answers describe the app configuration, not a completed Console submission.
Declare optional collection even though users can keep crash uploads off.

| Console item | Answer for this release |
| --- | --- |
| Does the app collect user data? | Yes: optional diagnostics and identifiers through Crashlytics and its dependencies. |
| App info and performance > Crash logs | Collected, optional; Analytics purpose (diagnosing crashes), not advertising. |
| App info and performance > Diagnostics | Collected, optional; Analytics purpose (app reliability). |
| Device or other IDs | Collected, optional; Analytics purpose (installation/session correlation for app quality). |
| App activity > App interactions | Collected, optional; Analytics purpose, to account for Sessions' foreground/session-start metrics. No Google Analytics event SDK is installed. |
| Is collection ephemeral? | No: diagnostics persist on Firebase beyond the request. |
| Is collected user data encrypted in transit? | Yes for Firebase diagnostic uploads (HTTPS); this does not mean exports or the local database have added encryption. |
| Is data shared? | Sending to Firebase acting solely as a contracted service provider can fall under Play's service-provider exception. Review actual Firebase integrations and sharing settings before marking these types as not shared. |
| Financial info, names, habit records | Stored locally, not intentionally uploaded to our servers or attached to diagnostics. Local processing alone does not count as collection. Do not add these types just because the app stores them locally. Reassess if any new remote feature or diagnostic payload sends them. |
| Account creation | No POCKDO sign-in accounts. Ledger accounts are local finance records. |
| Deletion requests | Users can email the published address. Only select a Console deletion-request option after confirming you can operate the stated process; do not promise that an email address identifies Crashlytics reports. |

Treat the App interactions mapping as a conservative classification of Sessions'
foreground metrics. Review the current form wording, resolved SDK versions and
any Firebase project integrations at submission; the app owner submits the form.
The verified release graph currently resolves Crashlytics 19.3.0, Firebase
Installations 18.0.0 and Sessions 2.0.7. Firebase's disclosure page describes its
latest SDK versions; recheck behavior when upgrading this project's SDKs.
Support email voluntarily sent outside the app and user-directed exports have
different disclosure rules from automatic SDK uploads. Do not describe the app as
collecting no data or assume all Firebase transfers require a shared-data answer.

Sources:

- [Firebase Android SDK disclosures](https://firebase.google.com/docs/android/play-data-disclosure)
- [Google Play Data safety definitions and exceptions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Firebase privacy, security and retention](https://firebase.google.com/support/privacy)

## Remaining release checks

1. Host the policy, verify the public URL, and enter it in Play Console.
2. Review Firebase project access/integrations; do not enable Analytics linking or
   other data exports without reassessing the policy and form.
3. Submit accurate Data safety, target audience, ads and content-rating answers.
   The policy does not replace these separate declarations. Do not choose a child
   target audience without reviewing the additional Families requirements.
4. Confirm in an internal Play test that a fresh install leaves report sharing
   off, opting in persists, and opting out remains off after a process restart.
   Validate a controlled crash only on test installations, never real user data.
5. Review any new SDK/remote feature and update the form and policy before release.

## Local verification

The signed release bundle, debug APK, test APK, all 69 JVM tests and Android lint
passed. Six Android 15 UI tests passed, including the dashboard privacy entry and
the complete offline policy, report-choice interaction and Back action at 150%
text size. Release dependencies and the merged release manifest have no Analytics,
Messaging or advertising-ID permissions; the upload-signing certificate is unchanged.
These checks do not submit a Data safety form or publish the policy.
Manual checks on the Android 17 phone emulator also verified both enabled and
disabled report-sharing choices after force-stop/relaunch, plus readable normal
and 150% text layouts. Reporting was left off and the original text size restored.
