# POCKDO in-app updates

Release builds use Google's Play In-App Update library (`app-update:2.1.0`).
No custom version endpoint, APK download permission, or background worker is used.

## Behavior

- Check when MainActivity resumes and currency setup is complete. Wait for the
  notification permission result before opening Play consent.
- Normal releases (Play priority 0-3) use flexible updates; accepted downloads
  run in the background. Critical releases (priority 4-5) use the full-screen
  immediate flow when Play allows it, falling back to flexible if necessary.
  After immediate consent, Play handles download, installation, and restart.
- Resume an already accepted immediate update on returning to the app, including
  after process recreation. Recovery bypasses the new-consent cooldown and
  never switches an immediate flow to the flexible Restart snackbar.
- Persist when consent is shown. Offer the same version at most once every 24
  hours, including across process restarts. A different available version or an
  escalation from flexible to critical immediate can be offered sooner. Users
  can cancel either flow and continue; this is not a mandatory minimum-version
  gate. Failed/offline checks do not interrupt the app.
- Detect completed downloads using both install status events and foreground
  checks. Show a dismissible Restart snackbar above the bottom navigation.
- For flexible updates, install only after Restart is tapped. Rediscover pending downloads after
  returning to the app or restarting the process. Show a retry action if the
  install request fails and the download is still ready.
- Debuggable builds (including `com.pockdo.daily.dev`) skip Play requests.
  Listeners follow the activity lifecycle. No changes to finance/habit storage.

## Release and testing

### Marking a release critical

Set `inAppUpdatePriority` to 4 or 5 using the Google Play Developer API when
publishing a critical fix. Default priority 0 keeps normal releases flexible.
This setting is release metadata, not a Gradle setting or a client-side switch.
No release or priority was published by this code change.

Google computes priority across intermediate version codes and release tracks.
A high-priority test release can therefore affect older production users when
they later update past it; use critical priority deliberately.

### Verify delivery

1. Configure proper release/upload signing and Play App Signing before release;
   the current release Gradle configuration still uses debug signing.
2. Upload a release using `com.pockdo.daily` with this feature enabled. Install
   it using Google Play's internal testing or internal app sharing workflow.
3. Upload a second compatible build with a higher `versionCode`, using the same
   application ID and appropriate signing identity.
4. Verify flexible consent, cancellation, background download, Restart/dismissal,
   and offline operation. Internal app sharing requires opening the newer sharing
   link without installing it before returning to the older app.
5. For the priority-triggered immediate path, use a Play testing track with a
   release carrying priority 4 or 5 via the Developer API. Internal app sharing
   does not support `inAppUpdatePriority`. Check immediate cancellation and
   recovery after backgrounding/process recreation, followed by Play's install
   and restart. Do not mark routine releases critical just to show this UI.

The first release has no newer Play version to offer. A locally sideloaded APK
alone cannot establish that Play delivery works. Unit tests cover the prompt
policy; actual delivery must be verified through Play before relying on it.

## Local validation

Debug APK builds and all 69 JVM tests passed, including 14 update policy tests
covering both flows, cooldowns, critical escalation, fallback, and interrupted
immediate recovery. Android lint passed with existing project warnings.
Play consent, download delivery, and installation still need the
two-version store test above; no live Play release was performed here.

References:

- [Google implementation guide](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java)
- [Google testing guide](https://developer.android.com/guide/playcore/in-app-updates/test)
