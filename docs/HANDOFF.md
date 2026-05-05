# Peel Launcher — Session Handoff

**Last updated:** 2026-05-05
**Repo:** https://github.com/GR3ATB0B/peel-launcher (public, on `main`)
**Last commit:** `90bc48d` — *feat: refined Control Center surface, sliders, chips, and motion*

## Where things stand

**Phase 1.5 (Visual Refinement) is feature-complete, tested, and pushed.**

11 tasks landed on `main` between `0c23d57..90bc48d`. The launcher now ships the refined-flat editorial design system:

- 2×2 home grid centered on the screen, capped at 480dp max-width with 14dp inter-tile spacing
- Deep desaturated solid tiles: forest `#1F3D2B` Phone, indigo navy `#243349` Messages, warm graphite `#2A2723` Camera, terracotta `#3D2418` Claude
- Custom stroke vector icons (Phone / Messages / Camera) at 1.6dp weight; the Claude tile uses the real Wikimedia Commons Claude AI symbol
- Spring press animation on every tile (0.96 scale + 0.85 alpha, 120ms down / 180ms release) — replaces Material ripple
- Control Center on `#0E0E0E` panel with custom drag handle, custom-thumb sliders for brightness + ring volume, pill-chip Wi-Fi / Bluetooth / Settings deep-links, full-width Silent-mode row
- Slide-down enter (220ms decel) + slide-up exit (180ms accel) animations with parallel scrim fade
- Roboto-based type scale tokenized in `dimens.xml` (display 48 / title 20 / body 13 / chip 12 / caption 11 sp) with intentional tracking
- 23/23 unit tests green (`./gradlew testDebugUnitTest`); previously-passing Espresso tests (`gridShowsFourTiles`, `gridIsVisible`) still green; new tests pin color hexes (`ColorPaletteTest`) and tile press feedback (`TilePressAnimationTest`)
- Verified end-to-end on `Peel_Test_API35` emulator; screenshots saved at `~/peel_home.png`, `~/peel_grid_bright.png`, `~/peel_cc.png`

**Phase 2 (LED ring + SMS/call notifications + priority contacts)** and **Phase 3 (Wispr Flow + Clicks key mapping)** remain blocked on Clicks Communicator SDK access.

## Open follow-ups (parked, not blocking)

1. **Lock-screen / ambient view.** Nash wants a "front page" that uses the same Roboto display type but no tiles — likely Phase 2 once we know what the system lock screen will let us replace versus theme. Today the device unlocks straight to the home grid.
2. **Clicks Communicator emulator dimensions.** Make a second AVD that matches the actual Clicks screen so the design can be sanity-checked at the device aspect ratio. Current verification is on Pixel 7 (1080×2400, 9:19.5).
3. **Adult content blocking.** Cloudflare Family DNS plan in HANDOFF history — not implemented yet. Wire Private DNS via `WRITE_SECURE_SETTINGS` on first launch as a Phase 2 task.
4. **`overridePendingTransition` deprecation.** Both `MainActivity` and `ControlCenterActivity` still call it. API-34+ replacement is `overrideActivityTransition`. Cosmetic; not breaking anything.

## Technical environment (don't reinstall)

All set up on this Mac, persistent across sessions:
- JDK 17 (Temurin) at `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
- Android command-line SDK at `/opt/homebrew/share/android-commandlinetools`
- `~/.zshrc` exports `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and prepends `platform-tools` + `emulator` to PATH
- AVD `Peel_Test_API35` (Pixel 7 profile, API 35, arm64-v8a)
- Gradle 9.5 system + 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21
- Robolectric pinned to SDK 34 in `app/src/test/resources/robolectric.properties` (it doesn't yet support API 35)

Background `bash`/`zsh` invocations don't source `.zshrc` — when launching the emulator non-interactively, use the absolute binary path `/opt/homebrew/share/android-commandlinetools/emulator/emulator`.

## To resume after a fresh `claude` restart

```bash
cd ~/peel-launcher

# If emulator is not running:
nohup /opt/homebrew/share/android-commandlinetools/emulator/emulator \
  -avd Peel_Test_API35 -no-snapshot-load -no-boot-anim > /tmp/emulator.log 2>&1 &

# Wait for boot, then:
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 3; done

./gradlew installDebug
adb shell cmd package set-home-activity com.peel.launcher/com.peel.launcher.MainActivity
adb shell input keyevent KEYCODE_HOME
```

## Source map (for the next session)

| Path | Role |
|---|---|
| `docs/superpowers/specs/2026-05-04-peel-phase-1-5-visual-refinement-design.md` | Locked design system spec |
| `docs/superpowers/plans/2026-05-04-peel-phase-1-5-visual-refinement.md` | Implementation plan (executed) |
| `docs/superpowers/plans/2026-05-04-peel-core-launcher.md` | Phase 1 plan (executed) |
| `app/src/main/res/values/colors.xml` | Refined-flat palette tokens |
| `app/src/main/res/values/dimens.xml` | Type scale + spacing tokens |
| `app/src/main/res/values/themes.xml` | `Theme.Peel`, `Widget.Peel.Chip`, `Widget.Peel.Slider`, type appearances |
| `app/src/main/res/drawable/tile_*_bg.xml` | Per-tile rounded background drawables |
| `app/src/main/res/drawable/ic_*.xml` | Stroke icons (Phone/Messages/Camera) + Claude AI symbol |
| `app/src/main/res/drawable/chip_pill.xml`, `slider_track.xml`, `slider_thumb.xml`, `cc_panel_bg.xml`, `cc_drag_handle.xml` | Control Center surface drawables |
| `app/src/main/kotlin/com/peel/launcher/MainActivity.kt` | Home grid + spacing decoration + swipe-down detector |
| `app/src/main/kotlin/com/peel/launcher/AppTileAdapter.kt` | FrameLayout-based tile bind + spring press animation |
| `app/src/main/kotlin/com/peel/launcher/ControlCenterActivity.kt` | Custom panel + scrim animators, chip press feedback |

## Caveman plugin

Active. If a future session loses it, re-enable with `claude plugin install caveman@caveman` then restart.
