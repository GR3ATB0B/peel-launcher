# Peel Launcher Phase 1.5 — Visual Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the saturated Phase 1 palette and Material3-default surfaces of the Peel Launcher with the refined-flat design system specified in `docs/superpowers/specs/2026-05-04-peel-phase-1-5-visual-refinement-design.md` — locked color palette, custom tile backgrounds, custom stroke icons, intentional Roboto type scale, custom Control Center sliders/chips/panel, and spring press feedback.

**Architecture:** Pure resource + view refactor. No new modules, no new dependencies. Token files (`colors.xml`, `dimens.xml`) drive every measurable value. Per-tile shape drawables replace `MaterialCardView`. Custom `<shape>` and `<layer-list>` drawables replace `Widget.Material3.Button.OutlinedButton` and the stock `SeekBar`'s default progress drawable. `MainActivity` and `ControlCenterActivity` keep their existing structure — only view types and animators change.

**Tech Stack:** Kotlin · Android Gradle Plugin 8.7.3 · Material 3 · ConstraintLayout · RecyclerView · Robolectric (SDK 34 pin) · Espresso · JUnit4. AVD `Peel_Test_API35`.

---

## Reference

- **Design spec:** `docs/superpowers/specs/2026-05-04-peel-phase-1-5-visual-refinement-design.md` — single source of truth for every dp/sp/hex value used below.
- **Build command:** `./gradlew assembleDebug` (compile only) or `./gradlew installDebug` (build + push to running emulator).
- **Test commands:** `./gradlew test` (Robolectric/JUnit, no emulator) and `./gradlew connectedAndroidTest` (Espresso, requires emulator running).

## File Structure

| Path | Status | Responsibility |
|---|---|---|
| `app/src/main/res/values/colors.xml` | Rewrite | Color tokens — surface, panel, chip, four tile colors, foreground tints |
| `app/src/main/res/values/dimens.xml` | **Create** | All sp/dp tokens used by themes and layouts |
| `app/src/main/res/values/themes.xml` | Modify | `Theme.Peel`, plus new `Widget.Peel.Chip`, `Widget.Peel.Slider`, and text-appearance styles |
| `app/src/main/res/values-night/themes.xml` | **Delete** | Phase-1 duplicate of the default theme; removed to avoid drift |
| `app/src/main/res/values/strings.xml` | Modify | "SMS" → "Messages" |
| `app/src/main/res/drawable/tile_phone_bg.xml` | **Create** | Rounded-rect shape, fill `tile_phone` |
| `app/src/main/res/drawable/tile_messages_bg.xml` | **Create** | Rounded-rect shape, fill `tile_messages` |
| `app/src/main/res/drawable/tile_camera_bg.xml` | **Create** | Rounded-rect shape, fill `tile_camera` |
| `app/src/main/res/drawable/tile_claude_bg.xml` | **Create** | Rounded-rect shape, fill `tile_claude` |
| `app/src/main/res/drawable/ic_phone.xml` | Rewrite | Stroke handset, 1.6 weight, white |
| `app/src/main/res/drawable/ic_messages.xml` | **Create** (rename from `ic_sms.xml`) | Stroke speech bubble |
| `app/src/main/res/drawable/ic_sms.xml` | **Delete** | Replaced by `ic_messages.xml` |
| `app/src/main/res/drawable/ic_camera.xml` | Rewrite | Stroke camera + circular lens |
| `app/src/main/res/drawable/ic_claude.xml` | Rewrite | Wikimedia Commons Claude AI symbol path, fill white |
| `app/src/main/res/drawable/chip_pill.xml` | **Create** | Pill shape, fill `peel_chip` |
| `app/src/main/res/drawable/slider_track.xml` | **Create** | Layer-list track (`peel_chip`) + scaled progress (`peel_text_primary`) |
| `app/src/main/res/drawable/slider_thumb.xml` | **Create** | Oval thumb, fill `peel_text_primary` |
| `app/src/main/res/drawable/cc_panel_bg.xml` | **Create** | Bottom-rounded rect, fill `peel_panel` |
| `app/src/main/res/drawable/cc_drag_handle.xml` | **Create** | Small rounded pill, `#333333` |
| `app/src/main/res/layout/activity_main.xml` | Modify | Max-width 480dp, vertical bias 0.5 |
| `app/src/main/res/layout/item_app_tile.xml` | Rewrite | `FrameLayout` + per-tile background drawable, no `MaterialCardView` |
| `app/src/main/res/layout/activity_control_center.xml` | Rewrite | Drag handle, restructured slider rows, pill TextView chips, silent-mode row |
| `app/src/main/kotlin/com/peel/launcher/AppTile.kt` | Modify | "SMS" → "Messages", swap `tile_sms`/`ic_sms` → `tile_messages`/`ic_messages`, add `backgroundRes: Int` field for per-tile drawable |
| `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt` | Modify | Drop `MaterialCardView`, use per-tile background drawable, add spring press animation |
| `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt` | Modify | Replace `MaterialButton` lookups with `TextView`, replace `Theme.Peel.ControlCenter` window animation with explicit panel + scrim animators, add chip press animation |
| `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt` | Modify | Test expects "Messages" label |
| `app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt` | Modify | Test no longer relies on `MaterialCardView` lookup |
| `app/src/test/kotlin/com/peel/launcher/ColorPaletteTest.kt` | **Create** | Robolectric test pinning palette hex values |
| `app/src/test/kotlin/com/peel/launcher/TilePressAnimationTest.kt` | **Create** | Robolectric test pinning press scale/alpha animation |

---

## Task 1: Color palette rewrite

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/test/kotlin/com/peel/launcher/ColorPaletteTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/peel/launcher/ColorPaletteTest.kt`:

```kotlin
package com.peel.launcher

import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorPaletteTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun resolve(name: String): Int {
        val id = ctx.resources.getIdentifier(name, "color", ctx.packageName)
        return ContextCompat.getColor(ctx, id)
    }

    @Test fun `peel_background is pure black`() = assertEquals(0xFF000000.toInt(), resolve("peel_background"))
    @Test fun `peel_panel is 0E0E0E`() = assertEquals(0xFF0E0E0E.toInt(), resolve("peel_panel"))
    @Test fun `peel_chip is 1A1A1A`() = assertEquals(0xFF1A1A1A.toInt(), resolve("peel_chip"))
    @Test fun `tile_phone is forest 1F3D2B`() = assertEquals(0xFF1F3D2B.toInt(), resolve("tile_phone"))
    @Test fun `tile_messages is indigo navy 243349`() = assertEquals(0xFF243349.toInt(), resolve("tile_messages"))
    @Test fun `tile_camera is warm graphite 2A2723`() = assertEquals(0xFF2A2723.toInt(), resolve("tile_camera"))
    @Test fun `tile_claude is terracotta 3D2418`() = assertEquals(0xFF3D2418.toInt(), resolve("tile_claude"))
    @Test fun `peel_text_primary is white`() = assertEquals(0xFFFFFFFF.toInt(), resolve("peel_text_primary"))
    @Test fun `peel_text_muted is 888888`() = assertEquals(0xFF888888.toInt(), resolve("peel_text_muted"))
    @Test fun `peel_text_faint is 444444`() = assertEquals(0xFF444444.toInt(), resolve("peel_text_faint"))
    @Test fun `peel_icon is white`() = assertEquals(0xFFFFFFFF.toInt(), resolve("peel_icon"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.ColorPaletteTest`
Expected: FAIL — most assertions fail because `peel_panel`, `peel_chip`, `tile_messages`, `peel_text_primary`, `peel_text_muted`, `peel_text_faint` don't exist yet, and existing colors are saturated (`tile_phone = #00FF00`, etc.).

- [ ] **Step 3: Rewrite `app/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Surface -->
    <color name="peel_background">#000000</color>
    <color name="peel_panel">#0E0E0E</color>
    <color name="peel_chip">#1A1A1A</color>

    <!-- Tile palette -->
    <color name="tile_phone">#1F3D2B</color>
    <color name="tile_messages">#243349</color>
    <color name="tile_camera">#2A2723</color>
    <color name="tile_claude">#3D2418</color>

    <!-- Foreground -->
    <color name="peel_text_primary">#FFFFFF</color>
    <color name="peel_text_muted">#888888</color>
    <color name="peel_text_faint">#444444</color>
    <color name="peel_icon">#FFFFFF</color>

    <!-- Material defaults retained for system surfaces -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

Note: `tile_sms` and `tile_icon` from the previous palette are intentionally removed. Task 3 renames the remaining references; this task may leave the project in a state where `AppTile.kt` references `R.color.tile_sms` (which no longer exists). Run the next step before assuming compilation works.

- [ ] **Step 4: Run color-palette test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.ColorPaletteTest`
Expected: PASS for all 11 assertions.

The full build is broken at this point (the rest of the codebase still references `tile_sms` / `tile_icon`). That is fixed in Task 3.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/test/kotlin/com/peel/launcher/ColorPaletteTest.kt
git commit -m "feat: refined-flat color palette tokens"
```

---

## Task 2: Dimens + text-appearance styles

**Files:**
- Create: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Delete: `app/src/main/res/values-night/themes.xml`

The night-qualified `themes.xml` is a Phase-1 duplicate of the default. The launcher is intentionally dark in both day and night, so removing the duplicate prevents drift between the two files when the styles in this task change.

- [ ] **Step 1: Create `app/src/main/res/values/dimens.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Type scale -->
    <dimen name="text_display">48sp</dimen>
    <dimen name="text_title">20sp</dimen>
    <dimen name="text_body">13sp</dimen>
    <dimen name="text_chip">12sp</dimen>
    <dimen name="text_caption">11sp</dimen>

    <!-- Tile -->
    <dimen name="tile_corner_radius">24dp</dimen>
    <dimen name="tile_icon_size">44dp</dimen>
    <dimen name="tile_gap">14dp</dimen>

    <!-- Home grid -->
    <dimen name="home_horizontal_padding">24dp</dimen>
    <dimen name="home_grid_max_width">480dp</dimen>

    <!-- Control Center -->
    <dimen name="cc_panel_radius">24dp</dimen>
    <dimen name="cc_panel_padding_top">16dp</dimen>
    <dimen name="cc_panel_padding_horizontal">24dp</dimen>
    <dimen name="cc_panel_padding_bottom">28dp</dimen>
    <dimen name="cc_drag_handle_width">36dp</dimen>
    <dimen name="cc_drag_handle_height">4dp</dimen>
    <dimen name="cc_drag_handle_margin_bottom">18dp</dimen>

    <!-- Slider -->
    <dimen name="slider_track_height">4dp</dimen>
    <dimen name="slider_thumb_size">20dp</dimen>

    <!-- Pill chip -->
    <dimen name="chip_padding_vertical">10dp</dimen>
    <dimen name="chip_padding_horizontal">18dp</dimen>
    <dimen name="chip_gap">6dp</dimen>
</resources>
```

- [ ] **Step 2: Replace `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">

    <!-- Type appearances -->
    <style name="TextAppearance.Peel.Display" parent="TextAppearance.Material3.HeadlineLarge">
        <item name="android:textSize">@dimen/text_display</item>
        <item name="android:fontFamily">sans-serif-light</item>
        <item name="android:letterSpacing">-0.04</item>
        <item name="android:textColor">@color/peel_text_primary</item>
    </style>

    <style name="TextAppearance.Peel.Title" parent="TextAppearance.Material3.TitleMedium">
        <item name="android:textSize">@dimen/text_title</item>
        <item name="android:fontFamily">sans-serif</item>
        <item name="android:letterSpacing">-0.02</item>
        <item name="android:textColor">@color/peel_text_primary</item>
    </style>

    <style name="TextAppearance.Peel.Body" parent="TextAppearance.Material3.BodyMedium">
        <item name="android:textSize">@dimen/text_body</item>
        <item name="android:fontFamily">sans-serif</item>
        <item name="android:letterSpacing">-0.01</item>
        <item name="android:textColor">@color/peel_text_primary</item>
    </style>

    <style name="TextAppearance.Peel.Chip" parent="TextAppearance.Material3.LabelMedium">
        <item name="android:textSize">@dimen/text_chip</item>
        <item name="android:fontFamily">sans-serif</item>
        <item name="android:letterSpacing">0</item>
        <item name="android:textColor">@color/peel_text_primary</item>
        <item name="android:textAllCaps">false</item>
    </style>

    <style name="TextAppearance.Peel.Caption" parent="TextAppearance.Material3.LabelSmall">
        <item name="android:textSize">@dimen/text_caption</item>
        <item name="android:fontFamily">sans-serif</item>
        <item name="android:letterSpacing">0.05</item>
        <item name="android:textColor">@color/peel_text_muted</item>
        <item name="android:textAllCaps">true</item>
    </style>

    <!-- Pill chip -->
    <style name="Widget.Peel.Chip" parent="android:Widget">
        <item name="android:background">@drawable/chip_pill</item>
        <item name="android:gravity">center</item>
        <item name="android:paddingTop">@dimen/chip_padding_vertical</item>
        <item name="android:paddingBottom">@dimen/chip_padding_vertical</item>
        <item name="android:paddingStart">@dimen/chip_padding_horizontal</item>
        <item name="android:paddingEnd">@dimen/chip_padding_horizontal</item>
        <item name="android:textAppearance">@style/TextAppearance.Peel.Chip</item>
        <item name="android:clickable">true</item>
        <item name="android:focusable">true</item>
    </style>

    <!-- Slider -->
    <style name="Widget.Peel.Slider" parent="Widget.AppCompat.SeekBar">
        <item name="android:progressDrawable">@drawable/slider_track</item>
        <item name="android:thumb">@drawable/slider_thumb</item>
        <item name="android:splitTrack">false</item>
        <item name="android:minHeight">@dimen/slider_track_height</item>
        <item name="android:maxHeight">@dimen/slider_track_height</item>
    </style>

    <!-- Themes -->
    <style name="Theme.Peel" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/peel_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
    </style>

    <style name="Theme.Peel.ControlCenter" parent="Theme.Peel">
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowAnimationStyle">@null</item>
    </style>
</resources>
```

The Control Center window animation style is set to `@null` because Task 11 supplies explicit `ViewPropertyAnimator` calls instead.

- [ ] **Step 3: Compile to confirm resources resolve**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL — but only after Task 3 fixes the dangling `tile_sms` references. Skip this verification for now and proceed to Task 3.

- [ ] **Step 4: Delete `app/src/main/res/values-night/themes.xml`**

```bash
git rm app/src/main/res/values-night/themes.xml
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/dimens.xml app/src/main/res/values/themes.xml
git commit -m "feat: type tokens, chip and slider styles"
```

---

## Task 3: Rename SMS → Messages

**Files:**
- Modify: `app/src/main/kotlin/com/peel/launcher/AppTile.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt`
- Move: `app/src/main/res/drawable/ic_sms.xml` → `app/src/main/res/drawable/ic_messages.xml`

- [ ] **Step 1: Update `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt`** to expect `Messages`

Replace the four-tile assertion block:

```kotlin
@Test
fun `AppTile_defaultTiles returns the four core apps in grid order`() {
    val tiles = AppTile.defaultTiles()

    assertEquals(4, tiles.size)
    assertEquals("Phone",    tiles[0].label)
    assertEquals("Messages", tiles[1].label)
    assertEquals("Camera",   tiles[2].label)
    assertEquals("Claude",   tiles[3].label)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.AppTileTest`
Expected: FAIL — current label is "SMS".

- [ ] **Step 3: Rename the drawable file**

```bash
git mv app/src/main/res/drawable/ic_sms.xml app/src/main/res/drawable/ic_messages.xml
```

(The file's contents stay for now; Task 5 rewrites the path data.)

- [ ] **Step 4: Update `app/src/main/res/values/strings.xml`**

Replace any string resource currently named for SMS. Open the file and:
- If a `<string name="tile_sms">SMS</string>` entry exists, rename it to `<string name="tile_messages">Messages</string>`.
- If `tile_sms` is not present (label is hard-coded in `AppTile.kt`), no change here.

- [ ] **Step 5: Update `app/src/main/kotlin/com/peel/launcher/AppTile.kt`**

Replace the second tile entry:

```kotlin
AppTile(
    label = "Messages",
    packageName = "com.simplemobiletools.smsmessenger",
    colorRes = R.color.tile_messages,
    iconRes = R.drawable.ic_messages,
),
```

- [ ] **Step 6: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — including the updated `AppTileTest`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/AppTile.kt \
        app/src/main/res/values/strings.xml \
        app/src/main/res/drawable/ic_messages.xml \
        app/src/test/kotlin/com/peel/launcher/AppTileTest.kt
git commit -m "refactor: rename SMS tile to Messages"
```

(`git mv` already staged the deletion of `ic_sms.xml`.)

---

## Task 4: Tile background drawables

**Files:**
- Create: `app/src/main/res/drawable/tile_phone_bg.xml`
- Create: `app/src/main/res/drawable/tile_messages_bg.xml`
- Create: `app/src/main/res/drawable/tile_camera_bg.xml`
- Create: `app/src/main/res/drawable/tile_claude_bg.xml`

- [ ] **Step 1: Create `tile_phone_bg.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/tile_phone" />
    <corners android:radius="@dimen/tile_corner_radius" />
</shape>
```

- [ ] **Step 2: Create `tile_messages_bg.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/tile_messages" />
    <corners android:radius="@dimen/tile_corner_radius" />
</shape>
```

- [ ] **Step 3: Create `tile_camera_bg.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/tile_camera" />
    <corners android:radius="@dimen/tile_corner_radius" />
</shape>
```

- [ ] **Step 4: Create `tile_claude_bg.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/tile_claude" />
    <corners android:radius="@dimen/tile_corner_radius" />
</shape>
```

- [ ] **Step 5: Compile to confirm drawables parse**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/tile_phone_bg.xml \
        app/src/main/res/drawable/tile_messages_bg.xml \
        app/src/main/res/drawable/tile_camera_bg.xml \
        app/src/main/res/drawable/tile_claude_bg.xml
git commit -m "feat: per-tile rounded background drawables"
```

---

## Task 5: Stroke icon vectors (Phone, Messages, Camera, Claude)

**Files:**
- Rewrite: `app/src/main/res/drawable/ic_phone.xml`
- Rewrite: `app/src/main/res/drawable/ic_messages.xml`
- Rewrite: `app/src/main/res/drawable/ic_camera.xml`
- Rewrite: `app/src/main/res/drawable/ic_claude.xml`

These are stroke-based vector drawables matching the design mockups. ViewBox `0 0 24 24` for the three Feather-style icons; `0 0 100 100` for the Wikimedia Claude path.

- [ ] **Step 1: Rewrite `ic_phone.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="@color/peel_icon"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent"
        android:pathData="M22,16.92v3a2,2 0 0 1 -2.18,2 19.79,19.79 0 0 1 -8.63,-3.07 19.5,19.5 0 0 1 -6,-6 19.79,19.79 0 0 1 -3.07,-8.67A2,2 0 0 1 4.11,2h3a2,2 0 0 1 2,1.72 12.84,12.84 0 0 0 0.7,2.81 2,2 0 0 1 -0.45,2.11L8.09,9.91a16,16 0 0 0 6,6l1.27,-1.27a2,2 0 0 1 2.11,-0.45 12.84,12.84 0 0 0 2.81,0.7A2,2 0 0 1 22,16.92z" />
</vector>
```

- [ ] **Step 2: Rewrite `ic_messages.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="@color/peel_icon"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent"
        android:pathData="M21,15a2,2 0 0 1 -2,2H7l-4,4V5a2,2 0 0 1 2,-2h14a2,2 0 0 1 2,2z" />
</vector>
```

- [ ] **Step 3: Rewrite `ic_camera.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="@color/peel_icon"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent"
        android:pathData="M23,19a2,2 0 0 1 -2,2H3a2,2 0 0 1 -2,-2V8a2,2 0 0 1 2,-2h4l2,-3h6l2,3h4a2,2 0 0 1 2,2z" />
    <path
        android:strokeColor="@color/peel_icon"
        android:strokeWidth="1.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent"
        android:pathData="M16,13 a4,4 0 1 1 -8,0 a4,4 0 1 1 8,0" />
</vector>
```

- [ ] **Step 4: Rewrite `ic_claude.xml`**

Source: `https://upload.wikimedia.org/wikipedia/commons/b/b0/Claude_AI_symbol.svg` (via `File:Claude_AI_symbol.svg`, Wikimedia Commons). Path data preserved verbatim; fill changed to white.

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:fillColor="@color/peel_icon"
        android:pathData="m19.6,66.5 19.7,-11 0.3,-1 -0.3,-0.5h-1l-3.3,-0.2 -11.2,-0.3L14,53l-9.5,-0.5 -2.4,-0.5L0,49l0.2,-1.5 2,-1.3 2.9,0.2 6.3,0.5 9.5,0.6 6.9,0.4L38,49.1h1.6l0.2,-0.7 -0.5,-0.4 -0.4,-0.4L29,41 18.4,34l-5.6,-4.1 -3,-2 -1.5,-2 -0.6,-4.2 2.7,-3 3.7,0.3 0.9,0.2 3.7,2.9 8,6.1L37,36l1.5,1.2 0.6,-0.4 0.1,-0.3 -0.7,-1.1L33,25 27,14.6 24.3,10.3 23.6,7.7c-0.3,-1 -0.4,-2 -0.4,-3l3,-4.2L28,0l4.2,0.6L33.8,2l2.6,6 4.1,9.3L47,29.9l2,3.8 1,3.4 0.3,1h0.7v-0.5l0.5,-7.2 1,-8.7 1,-11.2 0.3,-3.2 1.6,-3.8 3,-2L61,2.6l2,2.9 -0.3,1.8 -1.1,7.7L59,27.1l-1.5,8.2h0.9l1,-1.1 4.1,-5.4 6.9,-8.6 3,-3.5L77,13l2.3,-1.8h4.3l3.1,4.7 -1.4,4.9 -4.4,5.6 -3.7,4.7 -5.3,7.1 -3.2,5.7 0.3,0.4h0.7l12,-2.6 6.4,-1.1 7.6,-1.3 3.5,1.6 0.4,1.6 -1.4,3.4 -8.2,2 -9.6,2 -14.3,3.3 -0.2,0.1 0.2,0.3 6.4,0.6 2.8,0.2h6.8l12.6,1 3.3,2 1.9,2.7 -0.3,2 -5.1,2.6 -6.8,-1.6 -16,-3.8 -5.4,-1.3h-0.8v0.4l4.6,4.5 8.3,7.5L89,80.1l0.5,2.4 -1.3,2 -1.4,-0.2 -9.2,-7 -3.6,-3 -8,-6.8h-0.5v0.7l1.8,2.7 9.8,14.7 0.5,4.5 -0.7,1.4 -2.6,1 -2.7,-0.6 -5.8,-8 -6,-9 -4.7,-8.2 -0.5,0.4 -2.9,30.2 -1.3,1.5 -3,1.2 -2.5,-2 -1.4,-3 1.4,-6.2 1.6,-8 1.3,-6.4 1.2,-7.9 0.7,-2.6v-0.2H49L43,72l-9,12.3 -7.2,7.6 -1.7,0.7 -3,-1.5 0.3,-2.8L24,86l10,-12.8 6,-7.9 4,-4.6 -0.1,-0.5h-0.3L17.2,77.4l-4.7,0.6 -2,-2 0.2,-3 1,-1z" />
</vector>
```

- [ ] **Step 5: Compile + run unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (`AppTileTest`, `AppTileAdapterTest`, `AppLauncherTest`, `SwipeDownDetectorTest`, `ColorPaletteTest` all green).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/ic_phone.xml \
        app/src/main/res/drawable/ic_messages.xml \
        app/src/main/res/drawable/ic_camera.xml \
        app/src/main/res/drawable/ic_claude.xml
git commit -m "feat: stroke icon vectors and Wikimedia Claude mark"
```

---

## Task 6: Tile layout refactor (FrameLayout, no MaterialCardView)

**Files:**
- Rewrite: `app/src/main/res/layout/item_app_tile.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/AppTile.kt` (add `backgroundRes`)
- Modify: `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt`
- Modify: `app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt`
- Modify: `app/src/test/kotlin/com/peel/launcher/AppTileTest.kt`

- [ ] **Step 1: Update `AppTileTest.kt`** to assert per-tile background drawable wired

Add a third test:

```kotlin
@Test
fun `AppTile_defaultTiles wires per-tile background drawables`() {
    val tiles = AppTile.defaultTiles()

    assertEquals(R.drawable.tile_phone_bg,    tiles[0].backgroundRes)
    assertEquals(R.drawable.tile_messages_bg, tiles[1].backgroundRes)
    assertEquals(R.drawable.tile_camera_bg,   tiles[2].backgroundRes)
    assertEquals(R.drawable.tile_claude_bg,   tiles[3].backgroundRes)
}
```

Also update the constructor invocation in the existing first test to include `backgroundRes`:

```kotlin
val tile = AppTile(
    label = "Phone",
    packageName = "com.simplemobiletools.dialer",
    colorRes = android.R.color.holo_green_light,
    iconRes = android.R.drawable.sym_call_outgoing,
    backgroundRes = android.R.drawable.btn_default,
)
```

- [ ] **Step 2: Update `AppTileAdapterTest.kt`** to no longer reference `MaterialCardView`

Replace the click test's holder body — it should not require `holder.card` to exist:

```kotlin
@Test
fun `clicking a tile invokes the onTileClick callback with that tile`() {
    val tiles = AppTile.defaultTiles()
    var clicked: AppTile? = null
    val adapter = AppTileAdapter(tiles) { clicked = it }

    val app: android.app.Application = ApplicationProvider.getApplicationContext()
    app.setTheme(R.style.Theme_Peel)
    val holder = adapter.onCreateViewHolder(
        android.widget.FrameLayout(app),
        0,
    )
    adapter.onBindViewHolder(holder, 2)
    holder.itemView.performClick()

    assertEquals(tiles[2], clicked)
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.AppTileTest --tests com.peel.launcher.AppTileAdapterTest`
Expected: FAIL — `backgroundRes` does not exist on `AppTile`, and the adapter still references `MaterialCardView`.

- [ ] **Step 4: Add `backgroundRes` to `AppTile.kt`**

```kotlin
package com.peel.launcher

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AppTile(
    val label: String,
    val packageName: String,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
) {
    companion object {
        fun defaultTiles(): List<AppTile> = listOf(
            AppTile(
                label = "Phone",
                packageName = "com.simplemobiletools.dialer",
                colorRes = R.color.tile_phone,
                iconRes = R.drawable.ic_phone,
                backgroundRes = R.drawable.tile_phone_bg,
            ),
            AppTile(
                label = "Messages",
                packageName = "com.simplemobiletools.smsmessenger",
                colorRes = R.color.tile_messages,
                iconRes = R.drawable.ic_messages,
                backgroundRes = R.drawable.tile_messages_bg,
            ),
            AppTile(
                label = "Camera",
                packageName = "net.sourceforge.opencamera",
                colorRes = R.color.tile_camera,
                iconRes = R.drawable.ic_camera,
                backgroundRes = R.drawable.tile_camera_bg,
            ),
            AppTile(
                label = "Claude",
                packageName = "com.anthropic.claude",
                colorRes = R.color.tile_claude,
                iconRes = R.drawable.ic_claude,
                backgroundRes = R.drawable.tile_claude_bg,
            ),
        )
    }
}
```

- [ ] **Step 5: Rewrite `app/src/main/res/layout/item_app_tile.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <FrameLayout
        android:id="@+id/tile_root"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_margin="@dimen/tile_gap"
        android:background="@drawable/tile_phone_bg"
        android:clickable="true"
        android:focusable="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintDimensionRatio="1:1"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <ImageView
            android:id="@+id/tile_icon"
            android:layout_width="@dimen/tile_icon_size"
            android:layout_height="@dimen/tile_icon_size"
            android:layout_gravity="center"
            android:contentDescription="@null"
            android:src="@drawable/ic_phone" />
    </FrameLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

The `android:background` placeholder is overwritten in the adapter's bind step. The margin is `tile_gap / 2` worth of breathing room on each side; combined with `tile_gap` from the neighbour, total inter-tile spacing equals `tile_gap × 2 = 28dp`. To match the spec's 14dp inter-tile spacing, change `android:layout_margin` to `7dp` here, **OR** move spacing into the grid itself in Task 7. The plan uses Task 7's grid-level spacing, so set this margin to `0dp` in the next step.

- [ ] **Step 6: Set tile margin to 0dp**

In `item_app_tile.xml`, change `android:layout_margin="@dimen/tile_gap"` to `android:layout_margin="0dp"`. Spacing is set per-item by `RecyclerView` decoration in Task 7.

- [ ] **Step 7: Update `AppTileAdapter.kt`**

```kotlin
package com.peel.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AppTileAdapter(
    private val tiles: List<AppTile>,
    private val onTileClick: (AppTile) -> Unit,
) : RecyclerView.Adapter<AppTileAdapter.TileViewHolder>() {

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tile: FrameLayout = view.findViewById(R.id.tile_root)
        val icon: ImageView = view.findViewById(R.id.tile_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        holder.tile.setBackgroundResource(tile.backgroundRes)
        holder.icon.setImageResource(tile.iconRes)
        holder.icon.contentDescription = tile.label
        holder.tile.setOnClickListener { onTileClick(tile) }
    }

    override fun getItemCount(): Int = tiles.size
}
```

- [ ] **Step 8: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — `AppTileTest`, `AppTileAdapterTest`, `AppLauncherTest`, `SwipeDownDetectorTest`, `ColorPaletteTest` all green.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/AppTile.kt \
        app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt \
        app/src/main/res/layout/item_app_tile.xml \
        app/src/test/kotlin/com/peel/launcher/AppTileTest.kt \
        app/src/test/kotlin/com/peel/launcher/AppTileAdapterTest.kt
git commit -m "refactor: tile uses FrameLayout + per-tile background"
```

---

## Task 7: Home grid layout (max width + center bias + spacing)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/MainActivity.kt`

- [ ] **Step 1: Rewrite `activity_main.xml`**

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
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="@dimen/home_horizontal_padding"
        app:layout_constraintWidth_max="@dimen/home_grid_max_width"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.5" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Add a 14dp item-decoration to `MainActivity.kt`**

Replace `MainActivity.kt` with:

```kotlin
package com.peel.launcher

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var launcher: AppLauncher
    private lateinit var swipeDetector: SwipeDownDetector

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
        val gapPx = resources.getDimensionPixelSize(R.dimen.tile_gap)
        grid.addItemDecoration(GridSpacingDecoration(gapPx))

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        swipeDetector = SwipeDownDetector(slopPx = touchSlop) {
            startActivity(Intent(this, ControlCenterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, 0)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouch(null, event)
        return super.dispatchTouchEvent(event)
    }

    private class GridSpacingDecoration(private val gapPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % 2
            outRect.left = if (column == 0) 0 else gapPx / 2
            outRect.right = if (column == 0) gapPx / 2 else 0
            outRect.top = if (position < 2) 0 else gapPx
        }
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run instrumented test (emulator must be running)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — `gridShowsFourTiles`, `gridIsVisible` still green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml \
        app/src/main/kotlin/com/peel/launcher/MainActivity.kt
git commit -m "feat: centered home grid with 480dp max width and 14dp spacing"
```

---

## Task 8: Tile press animation (spring scale + alpha)

**Files:**
- Modify: `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt`
- Create: `app/src/test/kotlin/com/peel/launcher/TilePressAnimationTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/peel/launcher/TilePressAnimationTest.kt`:

```kotlin
package com.peel.launcher

import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TilePressAnimationTest {

    @Test
    fun `tile press scales and dims, release returns to default`() {
        val tiles = AppTile.defaultTiles()
        val adapter = AppTileAdapter(tiles) { /* no-op */ }
        val app: android.app.Application = ApplicationProvider.getApplicationContext()
        app.setTheme(R.style.Theme_Peel)

        val holder = adapter.onCreateViewHolder(android.widget.FrameLayout(app), 0)
        adapter.onBindViewHolder(holder, 0)

        val view = holder.tile

        // Default
        assertEquals(1f, view.scaleX, 0.0001f)
        assertEquals(1f, view.alpha, 0.0001f)

        // ACTION_DOWN
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        view.dispatchTouchEvent(down)
        // Animation runs on main looper; advance Robolectric's clock to settle.
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue("scale should drop on press", view.scaleX < 1f)
        assertTrue("alpha should drop on press", view.alpha < 1f)
        down.recycle()

        // ACTION_UP
        val up = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
        view.dispatchTouchEvent(up)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals("scale returns to 1", 1f, view.scaleX, 0.0001f)
        assertEquals("alpha returns to 1", 1f, view.alpha, 0.0001f)
        up.recycle()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.TilePressAnimationTest`
Expected: FAIL — no press animation wired yet.

- [ ] **Step 3: Add press animation to `AppTileAdapter.kt`**

Replace `onBindViewHolder` and add a private helper:

```kotlin
override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
    val tile = tiles[position]
    holder.tile.setBackgroundResource(tile.backgroundRes)
    holder.icon.setImageResource(tile.iconRes)
    holder.icon.contentDescription = tile.label
    holder.tile.setOnClickListener { onTileClick(tile) }
    attachPressAnimation(holder.tile)
}

@android.annotation.SuppressLint("ClickableViewAccessibility")
private fun attachPressAnimation(view: android.view.View) {
    view.setOnTouchListener { v, event ->
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.85f)
                    .setDuration(120L).start()
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(180L).start()
            }
        }
        false
    }
}
```

The `false` return keeps the existing `OnClickListener` working — the touch is not consumed.

- [ ] **Step 4: Run press-animation test**

Run: `./gradlew :app:testDebugUnitTest --tests com.peel.launcher.TilePressAnimationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt \
        app/src/test/kotlin/com/peel/launcher/TilePressAnimationTest.kt
git commit -m "feat: tile spring press animation"
```

---

## Task 9: Control Center drawables

**Files:**
- Create: `app/src/main/res/drawable/chip_pill.xml`
- Create: `app/src/main/res/drawable/slider_track.xml`
- Create: `app/src/main/res/drawable/slider_thumb.xml`
- Create: `app/src/main/res/drawable/cc_panel_bg.xml`
- Create: `app/src/main/res/drawable/cc_drag_handle.xml`

- [ ] **Step 1: Create `chip_pill.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/peel_chip" />
    <corners android:radius="999dp" />
</shape>
```

- [ ] **Step 2: Create `slider_thumb.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/peel_text_primary" />
    <size
        android:width="@dimen/slider_thumb_size"
        android:height="@dimen/slider_thumb_size" />
</shape>
```

- [ ] **Step 3: Create `slider_track.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background">
        <shape android:shape="rectangle">
            <solid android:color="@color/peel_chip" />
            <corners android:radius="2dp" />
            <size android:height="@dimen/slider_track_height" />
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <clip>
            <shape android:shape="rectangle">
                <solid android:color="@color/peel_text_primary" />
                <corners android:radius="2dp" />
                <size android:height="@dimen/slider_track_height" />
            </shape>
        </clip>
    </item>
</layer-list>
```

- [ ] **Step 4: Create `cc_panel_bg.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/peel_panel" />
    <corners
        android:topLeftRadius="0dp"
        android:topRightRadius="0dp"
        android:bottomLeftRadius="@dimen/cc_panel_radius"
        android:bottomRightRadius="@dimen/cc_panel_radius" />
</shape>
```

- [ ] **Step 5: Create `cc_drag_handle.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#333333" />
    <corners android:radius="2dp" />
</shape>
```

- [ ] **Step 6: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/drawable/chip_pill.xml \
        app/src/main/res/drawable/slider_track.xml \
        app/src/main/res/drawable/slider_thumb.xml \
        app/src/main/res/drawable/cc_panel_bg.xml \
        app/src/main/res/drawable/cc_drag_handle.xml
git commit -m "feat: Control Center surface, slider, chip drawables"
```

---

## Task 10: Control Center layout + view bindings

**Files:**
- Rewrite: `app/src/main/res/layout/activity_control_center.xml`
- Modify: `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt`
- Add: a string resource `silent_mode` set to `"Silent mode"` (if not already present)

- [ ] **Step 1: Confirm strings**

Open `app/src/main/res/values/strings.xml`. Ensure these names exist (add any missing):

```xml
<string name="brightness">Brightness</string>
<string name="volume">Volume</string>
<string name="wifi">Wi-Fi</string>
<string name="bluetooth">Bluetooth</string>
<string name="settings">Settings</string>
<string name="silent_mode">Silent mode</string>
<string name="silent_mode_off">Off</string>
<string name="silent_mode_on">On</string>
```

The existing layout already uses several of these; the only required addition is `silent_mode` if absent. Do not delete other strings.

- [ ] **Step 2: Rewrite `activity_control_center.xml`**

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
        android:background="@drawable/cc_panel_bg"
        android:orientation="vertical"
        android:paddingTop="@dimen/cc_panel_padding_top"
        android:paddingHorizontal="@dimen/cc_panel_padding_horizontal"
        android:paddingBottom="@dimen/cc_panel_padding_bottom"
        app:layout_constraintTop_toTopOf="parent">

        <View
            android:layout_width="@dimen/cc_drag_handle_width"
            android:layout_height="@dimen/cc_drag_handle_height"
            android:layout_gravity="center_horizontal"
            android:layout_marginBottom="@dimen/cc_drag_handle_margin_bottom"
            android:background="@drawable/cc_drag_handle" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="10dp"
            android:text="@string/brightness"
            android:textAppearance="@style/TextAppearance.Peel.Caption" />

        <SeekBar
            android:id="@+id/brightness_seek"
            style="@style/Widget.Peel.Slider"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:max="255" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="22dp"
            android:layout_marginBottom="10dp"
            android:text="@string/volume"
            android:textAppearance="@style/TextAppearance.Peel.Caption" />

        <SeekBar
            android:id="@+id/volume_seek"
            style="@style/Widget.Peel.Slider"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:orientation="horizontal"
            android:weightSum="3">

            <TextView
                android:id="@+id/wifi_btn"
                style="@style/Widget.Peel.Chip"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginEnd="@dimen/chip_gap"
                android:layout_weight="1"
                android:text="@string/wifi" />

            <TextView
                android:id="@+id/bluetooth_btn"
                style="@style/Widget.Peel.Chip"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginEnd="@dimen/chip_gap"
                android:layout_weight="1"
                android:text="@string/bluetooth" />

            <TextView
                android:id="@+id/settings_btn"
                style="@style/Widget.Peel.Chip"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/settings" />
        </LinearLayout>

        <FrameLayout
            android:id="@+id/silent_toggle"
            style="@style/Widget.Peel.Chip"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="start|center_vertical"
                android:text="@string/silent_mode"
                android:textAppearance="@style/TextAppearance.Peel.Chip" />

            <TextView
                android:id="@+id/silent_toggle_state"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end|center_vertical"
                android:text="@string/silent_mode_off"
                android:textAppearance="@style/TextAppearance.Peel.Body"
                android:textColor="@color/peel_text_muted" />
        </FrameLayout>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Replace `ControlCenterActivity.kt`**

```kotlin
package com.peel.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ControlCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)

        val scrim = findViewById<View>(R.id.control_scrim)
        val panel = findViewById<View>(R.id.control_panel)

        animatePanelIn(scrim, panel)

        scrim.setOnClickListener { dismiss(scrim, panel) }
        panel.setOnClickListener { /* consume */ }

        val slop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        scrim.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> downY = event.y
                    MotionEvent.ACTION_UP -> {
                        if (downY - event.y > slop && abs(downY - event.y) > slop) {
                            dismiss(scrim, panel)
                            return true
                        }
                        v?.performClick()
                    }
                }
                return false
            }
        })

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

        val silentRow = findViewById<View>(R.id.silent_toggle)
        val silentState = findViewById<TextView>(R.id.silent_toggle_state)
        fun refreshSilent() {
            silentState.setText(
                if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) R.string.silent_mode_on
                else R.string.silent_mode_off
            )
        }
        refreshSilent()
        silentRow.setOnClickListener {
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
        attachChipPress(silentRow)

        val wifi = findViewById<View>(R.id.wifi_btn)
        wifi.setOnClickListener { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        attachChipPress(wifi)

        val bt = findViewById<View>(R.id.bluetooth_btn)
        bt.setOnClickListener { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        attachChipPress(bt)

        val settings = findViewById<View>(R.id.settings_btn)
        settings.setOnClickListener { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        attachChipPress(settings)
    }

    private fun animatePanelIn(scrim: View, panel: View) {
        scrim.alpha = 0f
        panel.post {
            panel.translationY = -panel.height.toFloat()
            panel.animate()
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            scrim.animate()
                .alpha(1f)
                .setDuration(220L)
                .start()
        }
    }

    private fun dismiss(scrim: View, panel: View) {
        panel.animate()
            .translationY(-panel.height.toFloat())
            .setDuration(180L)
            .setInterpolator(AccelerateInterpolator())
            .start()
        scrim.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction { finish(); overridePendingTransition(0, 0) }
            .start()
    }

    private fun attachChipPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.85f).setDuration(120L).start()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180L).start()
            }
            false
        }
    }

    private fun currentSystemBrightness(): Int =
        try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
        catch (e: Settings.SettingNotFoundException) { 128 }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Run instrumented tests**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — `gridShowsFourTiles` and `gridIsVisible` still green.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/activity_control_center.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt
git commit -m "feat: refined Control Center surface, sliders, chips, and motion"
```

---

## Task 11: Visual verification on emulator

**Files:** none

This task is a manual checkpoint — no source changes.

- [ ] **Step 1: Confirm emulator is running**

```bash
adb devices
```

Expected: at least one entry showing `emulator-5554  device`. If empty, start the emulator from Android Studio's Device Manager (`Peel_Test_API35`).

- [ ] **Step 2: Install and launch**

```bash
cd /Users/nashvogeltanz/peel-launcher
./gradlew installDebug
adb shell cmd package set-home-activity com.peel.launcher/com.peel.launcher.MainActivity
adb shell input keyevent KEYCODE_HOME
```

- [ ] **Step 3: Capture home screen**

```bash
adb shell screencap -p /sdcard/peel_home.png
adb pull /sdcard/peel_home.png ~/peel_home.png
```

Compare visually against the locked Section 4 mockup (`.superpowers/brainstorm/88692-1777942121/content/design-4-home.html`):
- Tiles are deep desaturated solid colors (forest, indigo navy, warm graphite, terracotta), not saturated primaries
- Tile corner radius reads as `24dp` (rounded but not pill)
- Inter-tile spacing is even
- Grid sits centered vertically; no chrome above or below

- [ ] **Step 4: Capture Control Center**

Open the launcher, swipe down from below the status bar, then:

```bash
adb shell screencap -p /sdcard/peel_cc.png
adb pull /sdcard/peel_cc.png ~/peel_cc.png
```

Compare against Section 5 mockup (`design-5-cc.html`):
- Panel background is one shade lighter than the home black
- Drag handle pill visible at top center
- Sliders have a thin white track to the left of a circular thumb
- Wi-Fi / Bluetooth / Settings render as solid pill chips, not outlined buttons
- Silent-mode row is a full-width pill with state text on the right

- [ ] **Step 5: Verify press feedback**

Tap and hold a tile — it should briefly scale down to ~96% with reduced opacity, then spring back on release. Repeat on a Control Center chip.

- [ ] **Step 6: If anything diverges from the mockups, file follow-up**

Note divergence as a comment on this plan and circle back. Otherwise proceed.

- [ ] **Step 7: Push the branch**

```bash
git log --oneline | head -15
git push origin main
```

---

## Verification summary

After all tasks complete:

- `./gradlew testDebugUnitTest` — all unit tests pass (existing 8 + new `ColorPaletteTest` 11 cases + new `TilePressAnimationTest` 1 case + updated `AppTileTest`/`AppTileAdapterTest`).
- `./gradlew connectedDebugAndroidTest` — `gridShowsFourTiles`, `gridIsVisible` green.
- Visual screenshots match the locked design mockups in `.superpowers/brainstorm/88692-1777942121/content/`.
- The four design issues called out in `docs/HANDOFF.md` (saturated palette, flat-but-cheap tiles, missing typography, raw Material Control Center) are resolved.
