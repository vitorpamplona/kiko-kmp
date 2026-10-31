# kiko-player and kiko-compose — 2026-10-30

Status: written and tested here; Amethyst moved onto them (its `CartPlayerViewModel` holds a
`KikoPlayer`, its `CartPlayer` shows it with `KikoGame`). First released in 0.2.0.

## Goal

Make any Compose app able to play a cart with one call, the way an image loader shows an image:

```kotlin
KikoGame(key = url, source = { http.get(url) })
```

Until now every embedder rebuilt the same things around `FantasyConsole`: a thread with a deep
stack, a frame clock, a watchdog for frames that never end, debounced saves, a sound output, and
in Compose the screen, touch controls and key map. Amethyst had all of them; they moved here.

## Why three artifacts

- **`kiko` stays stdlib-only.** It is where hostile carts are handled, and "no dependencies, no
  threads, no I/O" is a rule that can be stated and checked. Hosts that run their own loop (a
  web page, a game engine, tests, a validator) keep using `step()` and `render()` and pull in
  nothing else.
- **`kiko-player` is headless.** It adds kotlinx.coroutines and is the only module that starts
  threads or opens a sound device. Amethyst's `commons` may not depend on Compose UI, and its
  ViewModel must hold the player, so the player cannot live in the Compose artifact.
- **`kiko-compose`** adds Compose runtime/ui/foundation and lifecycle-runtime-compose, no
  Material: the spinner is drawn by hand, the words come in `KikoLabels`, and there are no
  icons or app chrome. The app adds its own pause/close buttons and routes the system Back to
  `KikoPlayer.openPauseMenu()`.

The player API (state shapes, audio backends) will change faster than cart semantics; separate
artifacts keep that churn out of `kiko`'s ABI.

## What moved from Amethyst

| From (Amethyst) | To |
|---|---|
| `CartPlayerViewModel`: frame loop, pacing, backlog drop, watchdog, triple buffer, input hops | `KikoPlayer` |
| `CartPlatform`: game thread, heap check, `CartAudioSink` and its actuals | `kiko-player` platform code |
| `CartSaves` and the cap on new save ids | internal `SaveBuffer`; `KikoSaveStore` is the store |
| `CartPlayer`: layout, `DirectionPad`, `ActionButtons`, key map, typed text, status overlay | `KikoGame` |
| `CartScreenBitmap`, `claimSystemGestures` | `kiko-compose` platform code |
| `Buttons` bits, the screen size | `kiko`'s `Buttons`, `FantasyConsole.SCREEN_SIZE` |

One change on the way: buttons held while the cart loads used to be dropped; the player now
hands them to the console when it starts.

Amethyst keeps cart detection in notes, the Tor-routed download and sha256 check (the `source`),
the DataStore save store and its string format, its strings, the pause/close buttons, Back and
Android's window mode.

## API choices (settled before 0.2.0)

- `KikoPlayer` takes a `KikoPlayerHost` (log, time zone, other carts), not kiko's
  `ConsoleHost`: the player answers saves and memory pressure itself, and a `ConsoleHost`
  would have offered methods it silently ignored.
- `Status.Failed(reason)` tells a missing cart (`NO_CART`: the source failed) from bytes that
  are not one (`NOT_A_CART`), so an app can say "check your connection" or "not a game".
- Frames are pulled, not handed out: `frameNumber` says a new one is ready and `copyFrame`
  copies it into the caller's buffer. A handed-out array would have to be reused (a frame is
  64 KiB, 60 times a second) and change under a reader that kept it. The copy checks the
  frame counter afterwards and retries if the game reused that buffer meanwhile.
- `setButtons(mask, playerIndex)`: the console has eight players, and two gamepads should work.
- `KikoGame` is built from public pieces (`KikoScreen`, `KikoStatus`, `KikoDirectionPad`,
  `KikoActionButtons`, `Modifier.kikoKeys`, `KikoInput`), so an app can lay a player out its
  own way. `pauseInBackground` and `requestFocus` turn off what it does on its own.
- An internal constructor takes the game dispatcher, clock and save scope: `KikoPlayerTest`
  plays on virtual time on every target, and exact frame rates are testable.

## Limits

- On the web there is one thread: the game shares it with the page, and the watchdog only runs
  between frames, so a frame that never ends freezes the page.
- No sound output on iOS, macOS, Linux or the web yet (`openAudio` returns null there; an app
  may pass its own `KikoAudioSink`).
- `kiko-compose` has no macOS or Linux native targets: Compose Multiplatform draws on the JVM,
  Android, iOS and the web.

## Demo

`demo/` is unpublished: a desktop window over `kiko-compose` (`./gradlew :demo:run`, or
`--args="<cart>"`) and a browser page (`wasmJsMain`), which `pages.yml` publishes to GitHub Pages
from `main`. Without a file it plays `DemoCart.kt`, a small cart written for it, or one of two
permissively licensed games by other people (`DEMO_GAMES`, credited in the app and in
`demo/CREDITS.md`, which also records the games considered and why they were left out).
