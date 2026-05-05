# Peel Launcher — Session Handoff

**Last updated:** 2026-05-04 evening
**Repo:** https://github.com/GR3ATB0B/peel-launcher (public, on `main`)
**Last commit:** `78c59f7` — *docs: document Phase 1 build + test commands*

## Where things stand

**Phase 1 is feature-complete and pushed.** The launcher works on the emulator end-to-end:
- 2x2 home grid (Phone / SMS / Camera / Claude) with tap-to-launch + missing-app toast fallback
- Registered as Android `category.HOME` so it's a real launcher, not just an app
- Swipe-down opens Control Center (translucent overlay with brightness slider, volume slider, silent-mode toggle, Wi-Fi/Bluetooth/Settings deep-link buttons)
- Tap-outside-panel and swipe-up dismiss the Control Center
- 8 unit tests (Robolectric + JUnit) and 2 Espresso instrumented tests, all green

**Phase 2 (LED ring + SMS/call notifications + priority contacts) and Phase 3 (Wispr Flow + Clicks key mapping)** are written up in the plan but blocked on getting Clicks Communicator SDK access.

## Open design direction (start here next session)

Nash wants Peel's visual style to feel **Apple-grade, refined, luxurious** — like whatthenash.com. The current Phase 1 UI ships colored tiles using saturated primaries (`#00FF00` / `#0000FF` / `#808080` / `#FF6600`) and that reads as "AI slop." It needs a design pass before this is something he'd want on a real Clicks Communicator.

Specific aesthetic problems with what's currently committed:
1. **Tile colors are too saturated and primary.** Pure RGB greens/blues feel like a 2010 Material Design demo, not a premium device. Move to a more sophisticated palette (think Apple SF Symbols defaults, deeper greens, indigo/navy instead of pure blue, warm terracotta instead of pure orange).
2. **Tiles are flat with no depth.** No shadow, no gradient, no glass. Apple-aesthetic launchers either commit hard to flat with perfect typography, or layer subtle surface treatment (frosted glass, tasteful elevation).
3. **Icons are basic Material symbols at full white opacity.** SF-Symbol-inspired iconography with consistent stroke weight and a subtle inner highlight would land better.
4. **Typography is missing entirely** (no labels yet). Whether or not labels return, the type system needs intentionality (Inter, SF Pro Display, or similar — variable weight, tight letter-spacing, generous leading).
5. **Spacing reads as too tight.** The grid sits in the upper portion with a lot of empty bottom — should feel composed, not arbitrary.
6. **Control Center is purely functional** — Material outlined buttons + raw SeekBars. Needs a proper visual treatment: rounded surfaces, well-considered slider thumbs, proper iconography on the toggle row.

Recommended next-session approach:
- Treat this as a **Phase 1.5: Visual Refinement** task before touching Phase 2.
- Use the `frontend-design` skill for the design pass — it's tuned to avoid generic-AI aesthetics.
- Consider the **ui-ux-pro-max** skill for layout/spacing guidance.
- Reference whatthenash.com directly: minimalism with personality, generous whitespace, thoughtful typography.

## Technical environment (don't reinstall)

All set up on this Mac, persistent across sessions:
- JDK 17 (Temurin) at `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
- Android command-line SDK at `/opt/homebrew/share/android-commandlinetools`
- `~/.zshrc` exports `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and prepends `platform-tools` + `emulator` to PATH
- AVD `Peel_Test_API35` (Pixel 7 profile, API 35, arm64-v8a)
- Gradle 9.5 system + 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21
- Robolectric pinned to SDK 34 in `app/src/test/resources/robolectric.properties` (it doesn't yet support API 35)

To resume after a fresh `claude` restart:
```bash
cd ~/peel-launcher
./gradlew installDebug
adb shell cmd package set-home-activity com.peel.launcher/com.peel.launcher.MainActivity
adb shell input keyevent KEYCODE_HOME
```

## Open product concerns

- **No browser by design.** The four shipped apps don't include one; SMS links are intentionally not clickable; "Install unknown apps" should be blocked in Phase 2.
- **Belt-and-suspenders content filtering.** Cloudflare Family DNS (`family.cloudflare-dns.com`) blocks adult/malware/phishing at the DNS layer. Worth wiring as a Phase 2 task: have Peel set Private DNS on first launch (needs `WRITE_SECURE_SETTINGS`, granted via adb on the factory image).

## Files worth reading first in the next session

1. This file (`docs/HANDOFF.md`)
2. `docs/superpowers/plans/2026-05-04-peel-core-launcher.md` — full Phase 1 plan (already executed)
3. `app/src/main/res/values/colors.xml` — current saturated palette to redesign
4. `app/src/main/res/values/themes.xml` — current `Theme.Peel` definition
5. `app/src/main/res/layout/item_app_tile.xml` — the tile that needs the most love
6. `app/src/main/res/layout/activity_control_center.xml` — Control Center surface to refine

## Caveman plugin

Installed in the previous session via `claude plugin install caveman@caveman`. It activates on the next session start — output should compress significantly. If it doesn't kick in, run `claude plugin list` to confirm it's still enabled.
