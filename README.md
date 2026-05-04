# Peel

A minimalist Android launcher for the [Clicks Communicator](https://www.clicks.tech/) that turns the phone back into a tool, not a distraction.

Peel restricts the device to exactly four apps — **Phone, SMS, Camera, Claude** — and uses the Clicks Communicator's hardware LED ring for ambient notifications. No Play Store, no browser, no app drawer.

## Status

Early development. Phase 1 (core launcher, emulator-targeted) in progress. Hardware integration (LED ring, Clicks key → Wispr Flow) gated on Clicks SDK access.

## The Four Apps

| App    | Source                  | Purpose                  |
|--------|-------------------------|--------------------------|
| Phone  | Fossify Simple Dialer   | Calls                    |
| SMS    | Fossify Simple SMS      | Text messaging           |
| Camera | Open Camera             | Photos + built-in viewer |
| Claude | Anthropic Claude        | AI assistant             |

Plus **Wispr Flow** as a background voice-input service, triggered by the Clicks hardware key.

## Roadmap

- **Phase 1 — Core launcher (current).** 2x2 home grid, control center, default-home behavior, emulator-tested.
- **Phase 2 — LED + notifications.** Clicks LED ring control, SMS/call monitoring, priority contacts. Requires Clicks SDK.
- **Phase 3 — Wispr Flow + Clicks key.** Hardware key mapping for push-to-talk transcription. Requires Clicks SDK.

## Build

Phase 1 plan: [`docs/superpowers/plans/2026-05-04-peel-core-launcher.md`](docs/superpowers/plans/2026-05-04-peel-core-launcher.md)

## License

MIT — see [LICENSE](LICENSE).
