# Peel Launcher — Phase 1.5 Visual Refinement Design

**Date:** 2026-05-04
**Status:** Design approved, ready for implementation plan
**Replaces:** Phase 1 saturated palette and Material3 default surfaces shipped at commit `78c59f7`

## Goal

Bring the visible surfaces of Peel Launcher to an Apple-grade aesthetic that matches the reference at whatthenash.com — minimalism with personality, generous whitespace, restrained color, intentional typography. The refined-flat foundation chosen here lives or dies on color sophistication and type quality, not on glass or shadow.

This spec covers two surfaces only:

1. The Home grid (`MainActivity` / `activity_main.xml` / `item_app_tile.xml`).
2. The Control Center (`ControlCenterActivity` / `activity_control_center.xml`).

Out of scope:

- Lock-screen / ambient view. Android lock screen is system-controlled. A future ambient surface using the same type system is deferred to Phase 2.
- Clicks Communicator–specific emulator dimensions. The current `Peel_Test_API35` AVD (Pixel 7 profile) is the implementation target. A separate AVD matching Clicks dimensions is a follow-up.
- Phase 2 features: LED ring, SMS/call notifications, priority contacts, Wispr Flow, Clicks key mapping, content filtering.

## Foundation

**Refined flat — "editorial."** Deep desaturated solid tiles. No gradient, no glass, no elevation, no border. Premium because restrained. The Control Center adopts a single shade lighter (`#0E0E0E`) than the home background (`#000000`) to signal hierarchy without breaking the flat language.

## 1. Color system

| Token | Hex | Usage |
|---|---|---|
| `peel_background` | `#000000` | Home background, status bar fill |
| `peel_panel` | `#0E0E0E` | Control Center surface |
| `peel_chip` | `#1A1A1A` | Pill chip fill, slider track |
| `tile_phone` | `#1F3D2B` | Phone tile (forest) — replaces `#00FF00` |
| `tile_messages` | `#243349` | Messages tile (indigo navy) — replaces `#0000FF` |
| `tile_camera` | `#2A2723` | Camera tile (warm graphite) — replaces `#808080` |
| `tile_claude` | `#3D2418` | Claude tile (terracotta) — replaces `#FF6600` |
| `peel_text_primary` | `#FFFFFF` | Headings, body, chip labels |
| `peel_text_muted` | `#888888` | Section labels, secondary readouts |
| `peel_text_faint` | `#444444` | Reserved for future low-emphasis chrome |
| `peel_icon` | `#FFFFFF` | All tile glyphs and Control Center icons |

`colors.xml` is rewritten in full. `themes.xml` keeps `Theme.Peel` parented to `Theme.Material3.DayNight.NoActionBar` but updates `windowBackground` to `peel_background` and removes any unused legacy color references.

## 2. Tile component

| Property | Value |
|---|---|
| Aspect | 1 : 1 (square) |
| Corner radius | `24dp` |
| Icon size | `44dp` |
| Icon stroke width | `1.6dp` |
| Icon color | `#FFFFFF` |
| Elevation | `0dp` |
| Border | none |
| Inter-tile gap | `14dp` (horizontal and vertical) |
| Pressed scale | `0.96` |
| Pressed opacity | `0.85` |
| Press-down duration | `120ms` |
| Press-release duration | `180ms` |
| Press interpolator | spring (Android `SpringForce.STIFFNESS_LOW`, `DAMPING_RATIO_NO_BOUNCY`) |
| Material ripple | removed |

**Layout change.** `MaterialCardView` is replaced with a plain `FrameLayout` whose `android:background` points to a per-tile rounded-rect drawable (`tile_phone_bg.xml`, `tile_messages_bg.xml`, `tile_camera_bg.xml`, `tile_claude_bg.xml`), each a `<shape>` with corners `24dp` and a solid color from the palette above. This removes the residual elevation/ripple behavior of `MaterialCardView` and gives total control over press animation.

**Press animation.** Implemented in the tile's click listener via `View.animate()` with two `SpringAnimation` instances on `SCALE_X`/`SCALE_Y` and one `ObjectAnimator` on `alpha`. No XML state list animator — the spring keeps the response physical.

## 3. Iconography

All four icons are stroke-based at `1.6dp` weight, white. Phase 1 currently ships Material symbol drawables; these are replaced with custom `<vector>` drawables matching the Feather-style stroke vocabulary used in the design mockups.

- `ic_phone.xml` — handset, single path, stroke
- `ic_messages.xml` — speech-bubble rectangle with tail, single path, stroke
- `ic_camera.xml` — camera body + circular lens, two paths, stroke
- `ic_claude.xml` — official Claude AI symbol, **filled** path, sourced from Wikimedia Commons (`File:Claude_AI_symbol.svg`, viewBox `0 0 100 100`, `fill="#FFFFFF"`).

The Claude vector drawable embeds this exact path data:

```
m19.6 66.5 19.7-11 .3-1-.3-.5h-1l-3.3-.2-11.2-.3L14 53l-9.5-.5-2.4-.5L0 49l.2-1.5 2-1.3 2.9.2 6.3.5 9.5.6 6.9.4L38 49.1h1.6l.2-.7-.5-.4-.4-.4L29 41l-10.6-7-5.6-4.1-3-2-1.5-2-.6-4.2 2.7-3 3.7.3.9.2 3.7 2.9 8 6.1L37 36l1.5 1.2.6-.4.1-.3-.7-1.1L33 25l-6-10.4-2.7-4.3-.7-2.6c-.3-1-.4-2-.4-3l3-4.2L28 0l4.2.6L33.8 2l2.6 6 4.1 9.3L47 29.9l2 3.8 1 3.4.3 1h.7v-.5l.5-7.2 1-8.7 1-11.2.3-3.2 1.6-3.8 3-2L61 2.6l2 2.9-.3 1.8-1.1 7.7L59 27.1l-1.5 8.2h.9l1-1.1 4.1-5.4 6.9-8.6 3-3.5L77 13l2.3-1.8h4.3l3.1 4.7-1.4 4.9-4.4 5.6-3.7 4.7-5.3 7.1-3.2 5.7.3.4h.7l12-2.6 6.4-1.1 7.6-1.3 3.5 1.6.4 1.6-1.4 3.4-8.2 2-9.6 2-14.3 3.3-.2.1.2.3 6.4.6 2.8.2h6.8l12.6 1 3.3 2 1.9 2.7-.3 2-5.1 2.6-6.8-1.6-16-3.8-5.4-1.3h-.8v.4l4.6 4.5 8.3 7.5L89 80.1l.5 2.4-1.3 2-1.4-.2-9.2-7-3.6-3-8-6.8h-.5v.7l1.8 2.7 9.8 14.7.5 4.5-.7 1.4-2.6 1-2.7-.6-5.8-8-6-9-4.7-8.2-.5.4-2.9 30.2-1.3 1.5-3 1.2-2.5-2-1.4-3 1.4-6.2 1.6-8 1.3-6.4 1.2-7.9.7-2.6v-.2H49L43 72l-9 12.3-7.2 7.6-1.7.7-3-1.5.3-2.8L24 86l10-12.8 6-7.9 4-4.6-.1-.5h-.3L17.2 77.4l-4.7.6-2-2 .2-3 1-1 8-5.5Z
```

## 4. Typography

Family: **Roboto** (Android system default, no font file shipped in APK).

Tokens are declared once in `dimens.xml` and applied via styles in `themes.xml`. No raw `android:textSize` literals in layouts.

| Token | Size | Weight | Tracking | Transform | Usage |
|---|---|---|---|---|---|
| `text_display` | `48sp` | 300 (Light) | `-0.04em` | none | Reserved for future time/date display (lock-screen / ambient) |
| `text_title` | `20sp` | 400 (Regular) | `-0.02em` | none | Control Center title (currently unused, reserved) |
| `text_body` | `13sp` | 400 (Regular) | `-0.01em` | none | Chip secondary state text, future date strings |
| `text_chip` | `12sp` | 400 (Regular) | `0` | none | Wi-Fi / Bluetooth / Settings / Silent mode label |
| `text_caption` | `11sp` | 400 (Regular) | `0.05em` | UPPERCASE | Section headings above sliders ("Brightness", "Volume") |

Phase 1.5 actually renders `text_caption`, `text_chip`, and `text_body`. The display, title, and body-date tokens are declared in `dimens.xml` so Phase 2 lock-screen / ambient work uses the same scale without retroactive changes.

## 5. Home grid layout

| Property | Value |
|---|---|
| Background | `#000000` (window background, full bleed) |
| Status bar | transparent, light icons disabled (white over black) |
| Navigation bar | transparent |
| Container | `ConstraintLayout` |
| Horizontal padding | `24dp` left and right |
| Max grid width | `480dp` (`app:layout_constraintWidth_max="480dp"`) |
| Vertical alignment | `app:layout_constraintVertical_bias="0.5"` (centered) |
| Tile gap | `14dp` horizontal and vertical |
| Columns × rows | 2 × 2 |
| Tile order | row 1: Phone, Messages · row 2: Camera, Claude |
| Wallpaper | solid `#000000` via window background; no `WallpaperManager` calls |
| Chrome (clock, date, wordmark) | none in Phase 1.5 |

The current `RecyclerView` + `GridLayoutManager` stays — only the tile view holder layout changes.

## 6. Control Center

### Panel

| Property | Value |
|---|---|
| Background | `#0E0E0E` |
| Bottom corner radius | `24dp` (top edges flush to status bar) |
| Padding | `16dp` top · `24dp` left/right · `28dp` bottom |
| Width | match parent |
| Vertical position | top of screen |
| Drag handle | `36dp × 4dp` rounded pill, `#333333`, centered, `18dp` below top padding |
| Scrim | `rgba(0,0,0,0.6)` covering the rest of the screen — taps dismiss |

### Slider (custom — replaces stock `SeekBar`)

| Property | Value |
|---|---|
| Track height | `4dp` |
| Track radius | `2dp` |
| Track background | `#1A1A1A` |
| Filled (progress) color | `#FFFFFF` |
| Thumb diameter | `20dp` |
| Thumb color | `#FFFFFF` |
| Thumb shadow | `4dp` blur, 40% black |
| Section label above | `text_caption` style |

Implemented via three drawables: `slider_track.xml` (layer-list with track + scale-progress), `slider_thumb.xml` (oval shape with `<solid>` and elevation shadow), and a single `<style>` applying them to a stock `SeekBar` (`android:progressDrawable`, `android:thumb`).

### Pill chip (Wi-Fi / Bluetooth / Settings)

| Property | Value |
|---|---|
| Background | `#1A1A1A` |
| Corner radius | `999dp` (full pill) |
| Vertical padding | `10dp` |
| Horizontal padding | `18dp` |
| Label style | `text_chip` |
| Inter-chip gap | `6dp` |
| Pressed scale | `0.96` |
| Pressed opacity | `0.85` |

`Widget.Material3.Button.OutlinedButton` is removed. New `chip_pill.xml` shape drawable + a `Widget.Peel.Chip` style applied to plain `TextView`s with `OnClickListener`s.

### Silent toggle row

| Property | Value |
|---|---|
| Layout | full-width pill (same drawable as chip) |
| Padding | `10dp × 18dp` |
| Left content | label `"Silent mode"`, `text_chip` style |
| Right content | state text `"Off"` / `"On"`, `text_body`, color `#888888` |

### Motion

| Phase | Animation |
|---|---|
| Enter | translate Y from `-panelHeight` to `0`, `220ms`, `DecelerateInterpolator` (ease-out) |
| Enter (scrim) | alpha `0` → `1`, `220ms`, parallel |
| Exit | translate Y `0` → `-panelHeight`, `180ms`, `AccelerateInterpolator` (ease-in) |
| Exit (scrim) | alpha `1` → `0`, `180ms`, parallel |

The existing `Animation.Translucent` window animation in `Theme.Peel.ControlCenter` is replaced by these explicit `ViewPropertyAnimator` calls in `ControlCenterActivity.onCreate` / `onBackPressedDispatcher`, so the panel and scrim animate independently.

## File-level change summary

| File | Change |
|---|---|
| `app/src/main/res/values/colors.xml` | Rewrite to the palette above |
| `app/src/main/res/values/themes.xml` | Keep `Theme.Peel` parent; update window background reference; add `Widget.Peel.Chip` and slider style |
| `app/src/main/res/values/dimens.xml` | New file — declare all `text_*` tokens, all radii, paddings used in this spec |
| `app/src/main/res/drawable/tile_phone_bg.xml` | New — rounded-rect shape, `tile_phone` |
| `app/src/main/res/drawable/tile_messages_bg.xml` | New — `tile_messages` |
| `app/src/main/res/drawable/tile_camera_bg.xml` | New — `tile_camera` |
| `app/src/main/res/drawable/tile_claude_bg.xml` | New — `tile_claude` |
| `app/src/main/res/drawable/ic_phone.xml` | Replace with stroke vector |
| `app/src/main/res/drawable/ic_messages.xml` | Replace with stroke vector |
| `app/src/main/res/drawable/ic_camera.xml` | Replace with stroke vector |
| `app/src/main/res/drawable/ic_claude.xml` | Replace with the Wikimedia Commons Claude AI symbol path |
| `app/src/main/res/drawable/chip_pill.xml` | New — pill shape, `peel_chip` |
| `app/src/main/res/drawable/slider_track.xml` | New — layer-list track + progress |
| `app/src/main/res/drawable/slider_thumb.xml` | New — oval thumb with shadow |
| `app/src/main/res/drawable/cc_panel_bg.xml` | New — bottom-rounded rect, `peel_panel` |
| `app/src/main/res/drawable/cc_drag_handle.xml` | New — small pill |
| `app/src/main/res/layout/item_app_tile.xml` | Replace `MaterialCardView` with `FrameLayout` + per-tile background drawable |
| `app/src/main/res/layout/activity_main.xml` | Add `app:layout_constraintWidth_max` and vertical bias to grid container |
| `app/src/main/res/layout/activity_control_center.xml` | Restructure for new chip/slider/silent-row markup, drag handle, panel background drawable |
| `app/src/main/java/com/peel/launcher/MainActivity.kt` | Wire spring press-animation on tile click |
| `app/src/main/java/com/peel/launcher/ControlCenterActivity.kt` | Replace stock window animation with explicit panel + scrim `ViewPropertyAnimator`; same press feedback on chips |

## Verification (post-implementation)

- Visual: install on `Peel_Test_API35`, capture screenshot of home grid and Control Center, compare against the locked mockups.
- Existing test suites stay green: 8 Robolectric/JUnit unit tests, 2 Espresso instrumented tests (`gridShowsFourTiles`, `gridIsVisible`).
- New: an Espresso assertion that the Control Center panel `View` has `peel_panel` background and the brightness slider's progress drawable resolves to `slider_track`. (Concrete test names handed off to the writing-plans pass.)

## Sources

- Claude AI symbol SVG — [File:Claude_AI_symbol.svg, Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Claude_AI_symbol.svg) ([raw asset](https://upload.wikimedia.org/wikipedia/commons/b/b0/Claude_AI_symbol.svg))
- Brainstorm session mockups — `.superpowers/brainstorm/88692-1777942121/content/` (gitignored)
