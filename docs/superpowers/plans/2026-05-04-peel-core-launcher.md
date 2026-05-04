# Peel Core Launcher (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Peel launcher's core: a 2x2 home grid that launches four allowed apps, with a swipe-down control center for system toggles. Targets the Android emulator (API 35); no Clicks hardware dependencies.

**Architecture:** Single-Activity launcher (`MainActivity`) registered as `category.HOME` so Android treats it as a home app. A separate `ControlCenterActivity` is launched by a swipe-down gesture from the home screen and styled as a translucent overlay. App tiles are simple `MaterialCardView` instances bound from a list of `AppTile` data objects; tap intents resolve via `PackageManager.getLaunchIntentForPackage`. System controls in the control center use stock Android APIs (`AudioManager`, brightness via `Settings.System`, settings deep-link); WiFi/Bluetooth/cellular toggles are intentionally read-only on Android 10+ and route the user to the relevant settings panel — this matches platform reality and is documented in the plan.

**Tech Stack:**
- Kotlin 1.9+, Gradle (Kotlin DSL)
- minSdk 34 (Android 14), targetSdk 35 (Android 15 — Android 16 not yet stable in AGP at time of writing; bump when available)
- AndroidX (core-ktx, appcompat, constraintlayout), Material Components
- JUnit 4 + Robolectric for JVM unit tests, Espresso for instrumented UI tests
- No third-party dependencies for Phase 1

**Out of scope (later phases):**
- LED ring control (Phase 2 — Clicks SDK gated)
- SMS/call monitoring + priority contacts + LED triggers (Phase 2)
- Wispr Flow + Clicks key mapping (Phase 3 — Clicks SDK gated)
- Photo sharing flow restrictions (relies on having installed apps)
- Status-bar custom rendering (Android system bars retained for Phase 1)

**Repo conventions:**
- Conventional commits (`feat:`, `fix:`, `chore:`, `test:`, `docs:`)
- Commit after every passing test cycle
- Branches: work directly on `main` for Phase 1 since this is greenfield

---

## File Structure

After all tasks complete, the repo layout will be:

```
peel-launcher/
├── README.md
├── LICENSE
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts                          # root, plugin versions only
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml                    # version catalog
│   └── wrapper/
├── gradlew, gradlew.bat
├── app/
│   ├── build.gradle.kts                      # module build config
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/peel/launcher/
│       │   │   ├── MainActivity.kt           # home screen
│       │   │   ├── ControlCenterActivity.kt  # swipe-down overlay
│       │   │   ├── AppTile.kt                # data model
│       │   │   ├── AppTileAdapter.kt         # RecyclerView adapter for tiles
│       │   │   ├── AppLauncher.kt            # package -> intent resolver
│       │   │   └── SwipeDownDetector.kt      # gesture helper
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_main.xml
│       │       │   ├── activity_control_center.xml
│       │       │   └── item_app_tile.xml
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       ├── values-night/themes.xml
│       │       └── drawable/
│       │           ├── ic_phone.xml
│       │           ├── ic_sms.xml
│       │           ├── ic_camera.xml
│       │           └── ic_claude.xml
│       ├── test/                             # JVM unit tests (Robolectric)
│       │   └── kotlin/com/peel/launcher/
│       │       ├── AppTileTest.kt
│       │       ├── AppLauncherTest.kt
│       │       └── AppTileAdapterTest.kt
│       └── androidTest/                      # instrumented Espresso tests
│           └── kotlin/com/peel/launcher/
│               ├── MainActivityTest.kt
│               └── ControlCenterActivityTest.kt
└── docs/
    └── superpowers/plans/
        └── 2026-05-04-peel-core-launcher.md  # this file
```

---

## Task 0: Toolchain Setup (Android Studio + AVD + JDK)

**Why a manual task:** Android Studio's first-run setup is interactive (license accepts, SDK download). Don't try to script it.

**Files:** none (environment only)

**Goal:** End up with `adb`, `gradle`, `emulator`, and a booted AVD reachable from the shell. Purely CLI — no Android Studio GUI.

- [ ] **Step 1: Install Android command-line tools**

```bash
brew install --cask android-commandlinetools
```

This installs the SDK at `/opt/homebrew/share/android-commandlinetools` (Apple Silicon). Skip if `which sdkmanager` already resolves.

- [ ] **Step 2: Set environment variables**

Append to `~/.zshrc`:

```bash
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Then `source ~/.zshrc`.

- [ ] **Step 3: Accept all SDK licenses**

```bash
yes | sdkmanager --licenses
```

Expected: prints "All SDK package licenses accepted."

- [ ] **Step 4: Install required SDK packages**

```bash
sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "emulator" \
  "system-images;android-35;google_apis;arm64-v8a"
```

Expected: ~3-5 GB download. `arm64-v8a` is required on Apple Silicon — do NOT use x86_64 images.

- [ ] **Step 5: Create AVD**

```bash
echo "no" | avdmanager create avd \
  -n Peel_Test_API35 \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d pixel_7
```

Expected: `Created AVD 'Peel_Test_API35'`.

- [ ] **Step 6: Boot emulator in background and wait for it**

```bash
emulator -avd Peel_Test_API35 -no-snapshot-load &
adb wait-for-device
adb shell getprop sys.boot_completed | tr -d '\r'
```

Wait until `getprop sys.boot_completed` returns `1` (poll every 5s; usually 60-120s).

- [ ] **Step 7: Smoke-test ADB connectivity**

```bash
adb devices
```

Expected: `emulator-5554   device` listed. JDK is already 17 (verified separately).

---

## Task 1: Bootstrap Android project (CLI, no IDE)

**Goal:** Generate every Gradle and Android manifest file by hand, build a minimal Hello-World app, and verify it installs on the running emulator. After this task, `./gradlew assembleDebug` and `./gradlew installDebug` work from the command line.

**Files to create:**
- `gradle/wrapper/gradle-wrapper.properties` (generated by `gradle wrapper`)
- `gradle/wrapper/gradle-wrapper.jar` (generated by `gradle wrapper`)
- `gradlew`, `gradlew.bat` (generated by `gradle wrapper`)
- `settings.gradle.kts`
- `build.gradle.kts` (root)
- `gradle.properties`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/peel/launcher/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`

**Versions used in this task:** AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9, compileSdk 35, minSdk 34, targetSdk 35.

- [ ] **Step 1: Generate Gradle wrapper**

```bash
cd ~/peel-launcher
gradle wrapper --gradle-version 8.9 --distribution-type bin
```

Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 2: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Peel"
include(":app")
```

- [ ] **Step 3: Write root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
```

- [ ] **Step 4: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 5: Write `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.peel.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.peel.launcher"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
    sourceSets["test"].java.srcDirs("src/test/kotlin")
    sourceSets["androidTest"].java.srcDirs("src/androidTest/kotlin")

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
```

- [ ] **Step 6: Write `app/proguard-rules.pro`** (empty stub)

```
# Add project-specific ProGuard rules here.
```

- [ ] **Step 7: Write `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Peel"
        tools:targetApi="35">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:screenOrientation="portrait"
            android:stateNotNeeded="true"
            android:theme="@style/Theme.Peel">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

(HOME intent-filter is added in Task 8 — not yet, so initial install behaves as a normal app.)

- [ ] **Step 8: Write `app/src/main/res/xml/data_extraction_rules.xml`** (referenced by manifest)

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" />
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 9: Write `app/src/main/kotlin/com/peel/launcher/MainActivity.kt`**

```kotlin
package com.peel.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

- [ ] **Step 10: Write `app/src/main/res/layout/activity_main.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/peel_background">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/hello_peel"
        android:textColor="@android:color/white"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 11: Write `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Peel</string>
    <string name="hello_peel">Peel</string>
</resources>
```

- [ ] **Step 12: Write `app/src/main/res/values/colors.xml`** (minimal — Task 2 expands this)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="peel_background">#000000</color>
</resources>
```

- [ ] **Step 13: Write `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.Peel" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/peel_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
    </style>
</resources>
```

- [ ] **Step 14: Build the debug APK**

```bash
cd ~/peel-launcher
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

If the build fails with an SDK location error, write `local.properties`:

```
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

(Do NOT commit `local.properties` — it's already in `.gitignore`.)

- [ ] **Step 15: Install on the running emulator and launch**

```bash
adb devices                          # confirm emulator-5554 device
./gradlew installDebug
adb shell am start -n com.peel.launcher/.MainActivity
```

Expected: emulator screen shows a black background with "Peel" centered.

- [ ] **Step 16: Capture a screenshot for verification**

```bash
adb exec-out screencap -p > /tmp/peel-task1-smoke.png
ls -lh /tmp/peel-task1-smoke.png
```

Expected: PNG file >5KB exists. (Optional: open it to eyeball.)

- [ ] **Step 17: Initial commit**

```bash
git add -A
git commit -m "feat: bootstrap Android project skeleton (CLI, no IDE)"
```

---

## Task 2: Add the brand color palette

**Files:**
- Modify: `app/src/main/res/values/colors.xml`

- [ ] **Step 1: Replace contents of colors.xml**

Overwrite `app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Background -->
    <color name="peel_background">#000000</color>
    <color name="peel_surface">#121212</color>

    <!-- App tile colors -->
    <color name="tile_phone">#00FF00</color>
    <color name="tile_sms">#0000FF</color>
    <color name="tile_camera">#808080</color>
    <color name="tile_claude">#FF6600</color>

    <!-- On-tile foreground -->
    <color name="tile_icon">#FFFFFF</color>

    <!-- Material defaults retained for system surfaces -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/colors.xml
git commit -m "feat: add Peel brand color palette"
```

---

## Task 3: Dark, edge-to-edge launcher theme

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values-night/themes.xml`

- [ ] **Step 1: Replace values/themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.Peel" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/peel_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
    </style>

    <style name="Theme.Peel.ControlCenter" parent="Theme.Peel">
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowAnimationStyle">@android:style/Animation.Translucent</item>
    </style>
</resources>
```

- [ ] **Step 2: Create values-night/themes.xml**

Same contents — Peel is always dark, so no behavior change between modes.

```bash
mkdir -p app/src/main/res/values-night
cp app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml
```

- [ ] **Step 3: Reference theme in manifest**

Edit `app/src/main/AndroidManifest.xml`. The `<application>` tag should set:

```xml
android:theme="@style/Theme.Peel"
```

(Already set by Android Studio bootstrap — verify it points to `Theme.Peel`, rename if needed.)

- [ ] **Step 4: Run on emulator**

Click Run. Expected: black screen instead of light gray, no action bar.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res
git commit -m "feat: dark edge-to-edge Peel theme"
```

---

## Task 4: AppTile data model + unit test

**Files:**
- Create: `app/src/main/kotlin/com/peel/launcher/AppTile.kt`
- Create: `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt`
- Modify: `app/build.gradle.kts` (add JUnit dep — likely already present)

- [ ] **Step 1: Verify JUnit 4 + Robolectric in app/build.gradle.kts**

`dependencies { }` should include (add any missing):

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.robolectric:robolectric:4.13")
testImplementation("androidx.test:core:1.6.1")
```

After editing, click "Sync Now" in the yellow banner.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt`:

```kotlin
package com.peel.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTileTest {

    @Test
    fun `AppTile carries package, color, icon, and label`() {
        val tile = AppTile(
            label = "Phone",
            packageName = "com.simplemobiletools.dialer",
            colorRes = android.R.color.holo_green_light,
            iconRes = android.R.drawable.sym_call_outgoing,
        )

        assertEquals("Phone", tile.label)
        assertEquals("com.simplemobiletools.dialer", tile.packageName)
        assertEquals(android.R.color.holo_green_light, tile.colorRes)
        assertEquals(android.R.drawable.sym_call_outgoing, tile.iconRes)
    }

    @Test
    fun `AppTile_defaultTiles returns the four core apps in grid order`() {
        val tiles = AppTile.defaultTiles()

        assertEquals(4, tiles.size)
        assertEquals("Phone",  tiles[0].label)
        assertEquals("SMS",    tiles[1].label)
        assertEquals("Camera", tiles[2].label)
        assertEquals("Claude", tiles[3].label)
    }
}
```

- [ ] **Step 3: Run the test to confirm it fails**

In Android Studio: right-click `AppTileTest` → Run. Expected: compile error — `AppTile` does not exist.

- [ ] **Step 4: Implement AppTile**

Create `app/src/main/kotlin/com/peel/launcher/AppTile.kt`:

```kotlin
package com.peel.launcher

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AppTile(
    val label: String,
    val packageName: String,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int,
) {
    companion object {
        fun defaultTiles(): List<AppTile> = listOf(
            AppTile(
                label = "Phone",
                packageName = "com.simplemobiletools.dialer",
                colorRes = R.color.tile_phone,
                iconRes = R.drawable.ic_phone,
            ),
            AppTile(
                label = "SMS",
                packageName = "com.simplemobiletools.smsmessenger",
                colorRes = R.color.tile_sms,
                iconRes = R.drawable.ic_sms,
            ),
            AppTile(
                label = "Camera",
                packageName = "net.sourceforge.opencamera",
                colorRes = R.color.tile_camera,
                iconRes = R.drawable.ic_camera,
            ),
            AppTile(
                label = "Claude",
                packageName = "com.anthropic.claude",
                colorRes = R.color.tile_claude,
                iconRes = R.drawable.ic_claude,
            ),
        )
    }
}
```

- [ ] **Step 5: Add the four icon drawables**

The icons referenced (`ic_phone`, `ic_sms`, `ic_camera`, `ic_claude`) need to exist. Use Android Studio's Vector Asset wizard:

File → New → Vector Asset → Clip Art. For each:
- `ic_phone` → "phone" → color `#FFFFFF`, save as `ic_phone.xml`
- `ic_sms` → "message" → color `#FFFFFF`, save as `ic_sms.xml`
- `ic_camera` → "camera alt" → color `#FFFFFF`, save as `ic_camera.xml`
- `ic_claude` → "auto awesome" (sparkle) → color `#FFFFFF`, save as `ic_claude.xml` (placeholder until brand asset is available)

- [ ] **Step 6: Re-run tests**

Run `AppTileTest`. Expected: both tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/AppTile.kt \
        app/src/main/res/drawable/ic_phone.xml \
        app/src/main/res/drawable/ic_sms.xml \
        app/src/main/res/drawable/ic_camera.xml \
        app/src/main/res/drawable/ic_claude.xml \
        app/src/test/kotlin/com/peel/launcher/AppTileTest.kt \
        app/build.gradle.kts
git commit -m "feat: AppTile data model with four-app default config"
```

---

## Task 5: AppLauncher (package → intent resolver) with tests

**Files:**
- Create: `app/src/main/kotlin/com/peel/launcher/AppLauncher.kt`
- Create: `app/src/test/kotlin/com/peel/launcher/AppLauncherTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/peel/launcher/AppLauncherTest.kt`:

```kotlin
package com.peel.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppLauncherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val launcher = AppLauncher(context)

    @Test
    fun `launch returns false when package is not installed`() {
        val launched = launcher.launch("com.does.not.exist")

        assertFalse(launched)
    }

    @Test
    fun `launch returns true and starts an Intent when package is installed`() {
        // Robolectric ships with the host package installed; reuse it
        val installedPkg = context.packageName

        val launched = launcher.launch(installedPkg)

        assertTrue(launched)
        val started = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull(started)
        assertTrue(
            "Started intent should resolve to $installedPkg",
            started.`package` == installedPkg || started.component?.packageName == installedPkg
        )
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run `AppLauncherTest`. Expected: compile error — `AppLauncher` does not exist.

- [ ] **Step 3: Implement AppLauncher**

Create `app/src/main/kotlin/com/peel/launcher/AppLauncher.kt`:

```kotlin
package com.peel.launcher

import android.content.Context
import android.content.Intent

class AppLauncher(private val context: Context) {

    /** Returns true if the launch intent was found and started. */
    fun launch(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
```

- [ ] **Step 4: Run tests**

Expected: both tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/AppLauncher.kt \
        app/src/test/kotlin/com/peel/launcher/AppLauncherTest.kt
git commit -m "feat: AppLauncher resolves launch intent by package name"
```

---

## Task 6: 2x2 home grid layout

**Files:**
- Create: `app/src/main/res/layout/item_app_tile.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/build.gradle.kts` (ensure RecyclerView dep)

- [ ] **Step 1: Add RecyclerView dependency if missing**

In `app/build.gradle.kts` `dependencies { }`:

```kotlin
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("com.google.android.material:material:1.12.0")
```

Sync Gradle.

- [ ] **Step 2: Create item_app_tile.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/tile_card"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_margin="12dp"
    app:cardBackgroundColor="@color/tile_phone"
    app:cardCornerRadius="20dp"
    app:cardElevation="0dp">

    <ImageView
        android:id="@+id/tile_icon"
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:layout_gravity="center"
        android:contentDescription="@null"
        android:src="@drawable/ic_phone"
        app:tint="@color/tile_icon" />
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Replace activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/peel_background">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/tile_grid"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginHorizontal="24dp"
        android:layout_marginVertical="64dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/ app/build.gradle.kts
git commit -m "feat: 2x2 home grid scaffold layout"
```

---

## Task 7: AppTileAdapter + bind to MainActivity

**Files:**
- Create: `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt`
- Create: `app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt`
- Modify: `app/src/main/kotlin/com/peel/launcher/MainActivity.kt`

- [ ] **Step 1: Write the failing adapter test**

Create `app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt`:

```kotlin
package com.peel.launcher

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTileAdapterTest {

    @Test
    fun `adapter exposes one item per tile`() {
        val tiles = AppTile.defaultTiles()
        val adapter = AppTileAdapter(tiles) { /* no-op */ }

        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun `clicking a tile invokes the onTileClick callback with that tile`() {
        val tiles = AppTile.defaultTiles()
        var clicked: AppTile? = null
        val adapter = AppTileAdapter(tiles) { clicked = it }

        // Bind position 2 (Camera)
        val holder = adapter.onCreateViewHolder(
            android.widget.FrameLayout(ApplicationProvider.getApplicationContext()),
            0,
        )
        adapter.onBindViewHolder(holder, 2)
        holder.itemView.performClick()

        assertEquals(tiles[2], clicked)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Expected: compile error — `AppTileAdapter` not defined.

- [ ] **Step 3: Implement AppTileAdapter**

Create `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt`:

```kotlin
package com.peel.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AppTileAdapter(
    private val tiles: List<AppTile>,
    private val onTileClick: (AppTile) -> Unit,
) : RecyclerView.Adapter<AppTileAdapter.TileViewHolder>() {

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.tile_card)
        val icon: ImageView = view.findViewById(R.id.tile_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        val ctx = holder.itemView.context
        holder.card.setCardBackgroundColor(ContextCompat.getColor(ctx, tile.colorRes))
        holder.icon.setImageResource(tile.iconRes)
        holder.icon.contentDescription = tile.label
        holder.itemView.setOnClickListener { onTileClick(tile) }
    }

    override fun getItemCount(): Int = tiles.size
}
```

- [ ] **Step 4: Run adapter tests**

Expected: PASS.

- [ ] **Step 5: Wire adapter into MainActivity**

Replace `MainActivity.kt`:

```kotlin
package com.peel.launcher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var launcher: AppLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launcher = AppLauncher(this)

        val grid = findViewById<RecyclerView>(R.id.tile_grid)
        grid.layoutManager = GridLayoutManager(this, 2)
        grid.adapter = AppTileAdapter(AppTile.defaultTiles()) { tile ->
            if (!launcher.launch(tile.packageName)) {
                Toast.makeText(
                    this,
                    "${tile.label} is not installed yet",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}
```

- [ ] **Step 6: Run on emulator**

Click Run. Expected: black screen with 2x2 colored tiles (green / blue / gray / orange) showing white icons. Tapping any tile shows "X is not installed yet" toast.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt
git commit -m "feat: render four-tile grid with tap-to-launch and missing-app fallback"
```

---

## Task 8: Register MainActivity as a HOME launcher

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add HOME intent filter to MainActivity**

In `AndroidManifest.xml`, replace the MainActivity `<activity>` block:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:screenOrientation="portrait"
    android:stateNotNeeded="true"
    android:theme="@style/Theme.Peel">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- [ ] **Step 2: Run on emulator and set as default home**

Run app. Then on the emulator, press the home button (circle icon). Android prompts "Use Peel as Home?" — pick "Always".

Expected: home button now returns to the Peel grid.

- [ ] **Step 3: Verify recents behavior**

Open the Settings app from the emulator's app drawer (drag-up gesture still works on the system UI). Press home. Press home again. Should return to Peel cleanly without flicker.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: register Peel as a HOME launcher"
```

---

## Task 9: Instrumented test — home grid is rendered

**Files:**
- Create: `app/src/androidTest/kotlin/com/peel/launcher/MainActivityTest.kt`
- Modify: `app/build.gradle.kts` (Espresso deps if missing)

- [ ] **Step 1: Verify Espresso dependencies**

`app/build.gradle.kts`:

```kotlin
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation("androidx.test:rules:1.6.1")
```

- [ ] **Step 2: Write the test**

```kotlin
package com.peel.launcher

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun gridIsVisible() {
        onView(withId(R.id.tile_grid)).check(matches(isDisplayed()))
    }

    @Test
    fun gridShowsFourTiles() {
        rule.scenario.onActivity { activity ->
            val grid = activity.findViewById<RecyclerView>(R.id.tile_grid)
            assertEquals(4, grid.adapter?.itemCount)
        }
    }
}
```

- [ ] **Step 3: Run on emulator**

Right-click the test class → Run. Expected: both tests PASS on emulator.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/ app/build.gradle.kts
git commit -m "test: instrumented tests verify home grid renders four tiles"
```

---

## Task 10: SwipeDownDetector helper + unit test

**Files:**
- Create: `app/src/main/kotlin/com/peel/launcher/SwipeDownDetector.kt`
- Create: `app/src/test/kotlin/com/peel/launcher/SwipeDownDetectorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/peel/launcher/SwipeDownDetectorTest.kt`:

```kotlin
package com.peel.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDownDetectorTest {

    @Test
    fun `triggers when fling exceeds threshold and is mostly downward`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 50f, upX = 110f, upY = 800f)

        assertEquals(1, calls)
    }

    @Test
    fun `does not trigger when vertical delta is below threshold`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 50f, upX = 110f, upY = 70f)

        assertEquals(0, calls)
    }

    @Test
    fun `does not trigger when motion is mostly horizontal`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 50f, downY = 50f, upX = 800f, upY = 200f)

        assertEquals(0, calls)
    }

    @Test
    fun `does not trigger when motion is upward`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 800f, upX = 110f, upY = 50f)

        assertEquals(0, calls)
    }
}
```

- [ ] **Step 2: Run tests — fails**

Expected: compile error.

- [ ] **Step 3: Implement SwipeDownDetector**

Create `app/src/main/kotlin/com/peel/launcher/SwipeDownDetector.kt`:

```kotlin
package com.peel.launcher

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SwipeDownDetector(
    private val slopPx: Float,
    private val onSwipeDown: () -> Unit,
) : View.OnTouchListener {

    private var downX: Float = 0f
    private var downY: Float = 0f

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                onTouch(downX, downY, event.x, event.y)
            }
        }
        return false
    }

    /** Pure-Kotlin entry point used by unit tests and ACTION_UP. */
    fun onTouch(downX: Float, downY: Float, upX: Float, upY: Float) {
        val dx = upX - downX
        val dy = upY - downY
        val verticalDominant = abs(dy) > abs(dx)
        val downward = dy > slopPx
        if (verticalDominant && downward) {
            onSwipeDown()
        }
    }
}
```

- [ ] **Step 4: Run tests**

Expected: 4/4 PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/SwipeDownDetector.kt \
        app/src/test/kotlin/com/peel/launcher/SwipeDownDetectorTest.kt
git commit -m "feat: SwipeDownDetector recognizes top-edge downward fling"
```

---

## Task 11: Wire swipe-down on MainActivity to open ControlCenterActivity

**Files:**
- Modify: `app/src/main/kotlin/com/peel/launcher/MainActivity.kt`
- Create: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`
- Create: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Stub ControlCenterActivity**

Create `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`:

```kotlin
package com.peel.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ControlCenterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)
    }
}
```

- [ ] **Step 2: Stub the layout**

Create `app/src/main/res/layout/activity_control_center.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/control_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#CC000000"
    android:padding="24dp">

    <TextView
        android:id="@+id/control_placeholder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/control_center_title"
        android:textColor="@android:color/white"
        android:textSize="20sp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

Add to `strings.xml`:

```xml
<string name="control_center_title">Control Center</string>
```

- [ ] **Step 3: Register in manifest**

In `AndroidManifest.xml`, inside `<application>`:

```xml
<activity
    android:name=".ControlCenterActivity"
    android:exported="false"
    android:theme="@style/Theme.Peel.ControlCenter" />
```

- [ ] **Step 4: Attach swipe listener in MainActivity**

In `MainActivity.onCreate`, after `grid.adapter = ...`:

```kotlin
val root = findViewById<View>(R.id.root)
val touchSlop = ViewConfiguration.get(this).scaledTouchSlop * 4f
root.setOnTouchListener(SwipeDownDetector(slopPx = touchSlop) {
    startActivity(Intent(this, ControlCenterActivity::class.java))
    overridePendingTransition(android.R.anim.fade_in, 0)
})
```

Add imports as needed (`android.content.Intent`, `android.view.View`, `android.view.ViewConfiguration`).

- [ ] **Step 5: Run on emulator**

Swipe down anywhere on the home screen. Expected: translucent overlay appears with "Control Center" placeholder text.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ \
        app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/AndroidManifest.xml
git commit -m "feat: swipe down on home opens Control Center overlay"
```

---

## Task 12: Tap-outside-to-dismiss + swipe-up-to-dismiss for Control Center

**Files:**
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`
- Modify: `app/src/main/res/layout/activity_control_center.xml`

- [ ] **Step 1: Add a foreground panel container to the layout**

Replace `activity_control_center.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/control_scrim"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#99000000">

    <LinearLayout
        android:id="@+id/control_panel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/peel_surface"
        android:orientation="vertical"
        android:padding="24dp"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:id="@+id/control_placeholder"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/control_center_title"
            android:textColor="@android:color/white"
            android:textSize="20sp" />
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Wire scrim tap + swipe-up to finish**

Update `ControlCenterActivity.kt`:

```kotlin
package com.peel.launcher

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class ControlCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)

        val scrim = findViewById<View>(R.id.control_scrim)
        val panel = findViewById<View>(R.id.control_panel)

        // Tap on scrim (outside panel) -> dismiss
        scrim.setOnClickListener { finishWithFade() }
        // Block taps on the panel itself from bubbling to scrim
        panel.setOnClickListener { /* consume */ }

        // Swipe up anywhere -> dismiss
        val slop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        scrim.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> downY = event.y
                    MotionEvent.ACTION_UP -> {
                        if (downY - event.y > slop && abs(downY - event.y) > slop) {
                            finishWithFade()
                            return true
                        }
                        // Treat as tap on scrim
                        v?.performClick()
                    }
                }
                return false
            }
        })
    }

    private fun finishWithFade() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
```

- [ ] **Step 3: Run on emulator**

Open the control center, then:
- Tap above/below the panel → dismisses
- Swipe up → dismisses
- Tap on the panel → does NOT dismiss

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt \
        app/src/main/res/layout/activity_control_center.xml
git commit -m "feat: tap-outside and swipe-up dismiss for Control Center"
```

---

## Task 13: Brightness slider in Control Center

**Files:**
- Modify: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Note on permissions:** Writing system brightness needs the special `WRITE_SETTINGS` permission, which on Android 6+ requires the user to grant it via `ACTION_MANAGE_WRITE_SETTINGS`. We'll detect this and route to settings.

- [ ] **Step 1: Add the permission**

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_SETTINGS"
    tools:ignore="ProtectedPermissions" />
```

Add `xmlns:tools="http://schemas.android.com/tools"` to the root `<manifest>` if missing.

- [ ] **Step 2: Add brightness UI**

In `activity_control_center.xml`, inside `LinearLayout#control_panel`, after the title:

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="24dp"
    android:text="@string/brightness"
    android:textColor="@android:color/white" />

<SeekBar
    android:id="@+id/brightness_seek"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:max="255" />
```

Add to `strings.xml`:

```xml
<string name="brightness">Brightness</string>
```

- [ ] **Step 3: Wire it up**

In `ControlCenterActivity.onCreate` (after the existing setup):

```kotlin
val brightness = findViewById<SeekBar>(R.id.brightness_seek)
brightness.progress = currentSystemBrightness()
brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        if (!Settings.System.canWrite(this@ControlCenterActivity)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
            )
            return
        }
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            progress.coerceIn(0, 255),
        )
    }
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
})
```

Helper:

```kotlin
private fun currentSystemBrightness(): Int =
    try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
    catch (e: Settings.SettingNotFoundException) { 128 }
```

Imports: `android.content.Intent`, `android.net.Uri`, `android.provider.Settings`, `android.widget.SeekBar`.

- [ ] **Step 4: Test on emulator**

Open Control Center, drag the slider. First time: it routes to system settings to grant `WRITE_SETTINGS`. Toggle on, navigate back, retry — slider now changes screen brightness.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt \
        app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/AndroidManifest.xml
git commit -m "feat: brightness slider with WRITE_SETTINGS handoff"
```

---

## Task 14: Volume slider (ringer stream)

**Files:**
- Modify: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`

- [ ] **Step 1: Add UI**

Append in `control_panel`:

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="@string/volume"
    android:textColor="@android:color/white" />

<SeekBar
    android:id="@+id/volume_seek"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

`strings.xml`:

```xml
<string name="volume">Volume</string>
```

- [ ] **Step 2: Wire AudioManager**

In `onCreate`:

```kotlin
val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val volume = findViewById<SeekBar>(R.id.volume_seek)
volume.max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
volume.progress = audio.getStreamVolume(AudioManager.STREAM_RING)
volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) audio.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
    }
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
})
```

Imports: `android.content.Context`, `android.media.AudioManager`.

- [ ] **Step 3: Test on emulator**

Drag volume slider — emulator volume indicator should reflect changes.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt \
        app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat: ringer volume slider in Control Center"
```

---

## Task 15: Silent-mode toggle

**Files:**
- Modify: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`

- [ ] **Step 1: Add toggle UI**

In `control_panel`:

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/silent_toggle"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="24dp"
    android:text="@string/silent_mode_off" />
```

`strings.xml`:

```xml
<string name="silent_mode_off">Silent: Off</string>
<string name="silent_mode_on">Silent: On</string>
```

- [ ] **Step 2: Wire it up**

```kotlin
val silentBtn = findViewById<MaterialButton>(R.id.silent_toggle)
fun refreshSilent() {
    silentBtn.text = getString(
        if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) R.string.silent_mode_on
        else R.string.silent_mode_off
    )
}
refreshSilent()
silentBtn.setOnClickListener {
    audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL)
        AudioManager.RINGER_MODE_SILENT
    else
        AudioManager.RINGER_MODE_NORMAL
    refreshSilent()
}
```

Note: On API 23+, switching to silent may require `DO_NOT_DISTURB_ACCESS`. If `audio.ringerMode = SILENT` throws `SecurityException`, route to settings:

```kotlin
silentBtn.setOnClickListener {
    try {
        audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            AudioManager.RINGER_MODE_SILENT
        else
            AudioManager.RINGER_MODE_NORMAL
        refreshSilent()
    } catch (e: SecurityException) {
        startActivity(Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"))
    }
}
```

Import: `com.google.android.material.button.MaterialButton`.

- [ ] **Step 3: Test on emulator**

Tap toggle. First time may route to DND settings — grant Peel access and retry. Toggle text flips between "Silent: On" and "Silent: Off".

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt \
        app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat: silent mode toggle with DND-permission fallback"
```

---

## Task 16: WiFi / Bluetooth / Settings deep-link buttons

**Note:** Android 10+ removed programmatic WiFi/Bluetooth toggle for non-system apps (`WifiManager.setWifiEnabled` is a no-op for third-party apps targeting API 29+). Peel routes the user to the system panels instead.

**Files:**
- Modify: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`

- [ ] **Step 1: Add three buttons**

In `control_panel`, before the brightness label:

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:orientation="horizontal"
    android:weightSum="3">

    <com.google.android.material.button.MaterialButton
        android:id="@+id/wifi_btn"
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="6dp"
        android:layout_weight="1"
        android:text="@string/wifi" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/bluetooth_btn"
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="3dp"
        android:layout_weight="1"
        android:text="@string/bluetooth" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/settings_btn"
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="6dp"
        android:layout_weight="1"
        android:text="@string/settings" />
</LinearLayout>
```

`strings.xml`:

```xml
<string name="wifi">Wi-Fi</string>
<string name="bluetooth">Bluetooth</string>
<string name="settings">Settings</string>
```

- [ ] **Step 2: Wire intents**

In `onCreate`:

```kotlin
findViewById<MaterialButton>(R.id.wifi_btn).setOnClickListener {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
}
findViewById<MaterialButton>(R.id.bluetooth_btn).setOnClickListener {
    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
}
findViewById<MaterialButton>(R.id.settings_btn).setOnClickListener {
    startActivity(Intent(Settings.ACTION_SETTINGS))
}
```

- [ ] **Step 3: Test on emulator**

Each button should open the corresponding system panel.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt \
        app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat: WiFi/Bluetooth/Settings deep-links in Control Center"
```

---

## Task 17: Lock screen orientation + clean app label

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Confirm portrait lock on MainActivity + ControlCenterActivity**

Both `<activity>` blocks should already include:

```xml
android:screenOrientation="portrait"
```

Add it to ControlCenterActivity if missing.

- [ ] **Step 2: Set the app label**

In `strings.xml`:

```xml
<string name="app_name">Peel</string>
```

(Should already be set — verify.)

- [ ] **Step 3: Run + commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml
git commit -m "chore: lock orientation and confirm app label"
```

---

## Task 18: Document Phase 1 in README + verify the build

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Append Phase 1 build instructions**

Append to README.md:

```markdown

## Building Phase 1 (emulator)

```bash
# Open in Android Studio
open -a "Android Studio" .

# Or build from CLI (requires Android SDK on PATH)
./gradlew assembleDebug
./gradlew test            # JVM unit tests
./gradlew connectedCheck  # Espresso tests on running emulator
```

To set Peel as the default home on the emulator, install the app, press the home button, and choose "Always" when prompted.
```

- [ ] **Step 2: Run all tests one more time**

```bash
./gradlew test connectedCheck
```

Expected: all PASS.

- [ ] **Step 3: Commit + push**

```bash
git add README.md
git commit -m "docs: document Phase 1 build + test commands"
git push -u origin main
```

---

## Self-Review Checklist (run before handing off to executor)

- [ ] Every step that changes code includes the actual code (no "implement appropriately")
- [ ] Method names match across tasks: `AppLauncher.launch`, `AppTile.defaultTiles`, `SwipeDownDetector.onTouch`
- [ ] Resource IDs referenced match across XML and Kotlin: `R.id.tile_grid`, `R.id.tile_card`, `R.id.tile_icon`, `R.id.control_scrim`, `R.id.control_panel`, `R.id.brightness_seek`, `R.id.volume_seek`, `R.id.silent_toggle`, `R.id.wifi_btn`, `R.id.bluetooth_btn`, `R.id.settings_btn`
- [ ] Color resources match: `tile_phone`, `tile_sms`, `tile_camera`, `tile_claude`, `peel_background`, `peel_surface`, `tile_icon`
- [ ] Each task ends in a commit
- [ ] Tests precede implementation for the four logic-bearing units (AppTile, AppLauncher, AppTileAdapter, SwipeDownDetector)
- [ ] No references to Phase 2/3 components (LED, SMS, Wispr) — those are explicitly out of scope
