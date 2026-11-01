# POCKDO release signing

Release builds read a dedicated upload key from the local `keystore.properties`
file. Gradle validates the credentials when signing; debug builds still work
without an upload key.
No production key or passwords are shipped with this repository.

## Create your first upload key locally

1. In Android Studio, select **Build > Generate Signed Bundle / APK > Android App
   Bundle**, select the app module, and choose **Create new** beside Key store path.
2. Save the keystore outside the repository, for example
   `C:/Users/YOUR_USER/PockdoSigning/pockdo-upload.jks`. Create its parent folder if
   needed. Use an alias such as `key0`, a strong password saved in your password
   manager, the same password for the key and store, and validity of at least
   25 years. Enter your own certificate details.
3. After creating the key, cancel the wizard and finish the local configuration
   below to build from Gradle.
4. Back up the keystore to a separate encrypted location and securely retain the
   alias/password. An ignored file is not a backup. Do not share passwords in chat,
   commit keys, or rely on a single computer for recovery.

If an upload key is already registered for this app, use that key. Do not generate
a different key for each release.

## Configure this checkout

Create `keystore.properties` at the repository root with these four fields, or
keep your existing configured file:

```properties
storeFile=C:/Users/YOUR_USER/PockdoSigning/pockdo-upload
storePassword=YOUR_PRIVATE_STORE_PASSWORD
keyAlias=key0
keyPassword=YOUR_PRIVATE_KEY_PASSWORD
```

Replace the example values with your actual path, alias, and passwords. A keystore
does not need a filename extension; use its exact saved filename. `key0` is a valid
alias if that is the one you created. Windows paths must use forward
slashes, or escaped backslashes, because the file uses Java properties syntax.
Do not add quotes around values. Passwords containing backslashes must escape
them as `\\`; leading spaces must also be escaped. A password manager-generated
password without backslashes or leading spaces avoids those properties escapes.

Do not put passwords in shell command arguments, tracked Gradle files, or build logs.

Confirm the local file is ignored before committing other changes:

```powershell
git check-ignore keystore.properties
.\gradlew.bat :app:bundleRelease
```

The standard bundle output is `app/build/outputs/bundle/release/app-release.aab`.
Successful bundle signing verifies the keystore password and key alias. Configure
your upload key rather than the Android debug keystore.
Only use a fresh successful build for upload; an older output can remain after a
failed build. The bundle must identify `com.pockdo.daily`, version code `1`, target
API `36`, and use your upload certificate. Increment the version code for later
uploads once a version has been used in Play Console.

## Google Play App Signing

For the first Google Play release, enroll in Play App Signing and let Google
generate/manage the app signing key if Google Play is your only intended store.
Your local upload key signs the AAB; Google verifies it and signs delivered APKs
with the separate app signing key. The app ID remains `com.pockdo.daily`.

If distribution through other stores with the same signing identity is planned,
decide the app signing key strategy before enrollment; see Google's documentation.
In Firebase or other APIs that require certificate fingerprints, register the
**Play app signing certificate** for Play-installed builds, and the upload
certificate as appropriate for locally signed release testing. These certificates
have different fingerprints; debug uses a third certificate.

Keep a recovery record of the Play developer account, package ID, upload alias,
backup location, and upload/app signing SHA-256 fingerprints. Store passwords
separately and protect account recovery. Play supports an upload-key reset when
needed; it is different from replacing the app signing key.

See [Google's official signing guide](https://developer.android.com/studio/publish/app-signing).
Final production-key signing and Play App Signing enrollment require your local
key setup and Play Console account; they are not completed by editing Gradle.
