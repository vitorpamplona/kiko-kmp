# kiko-kmp

[![Last Version](https://img.shields.io/github/release/vitorpamplona/kiko-kmp.svg?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/vitorpamplona/kiko-kmp/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/com.vitorpamplona.kiko/kiko?label=Maven%20Central&labelColor=27303D&color=0877d2)](https://central.sonatype.com/artifact/com.vitorpamplona.kiko/kiko)
[![JitPack snapshots](https://img.shields.io/badge/snapshots-JitPack-27303D?labelColor=27303D&color=0877d2)](https://jitpack.io/#vitorpamplona/kiko-kmp)
[![CI](https://img.shields.io/github/actions/workflow/status/vitorpamplona/kiko-kmp/build.yml?labelColor=27303D)](https://github.com/vitorpamplona/kiko-kmp/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/github/license/vitorpamplona/kiko-kmp?labelColor=27303D&color=0877d2)](LICENSE)

A runtime for 128x128, 16-color fantasy-console cartridges (`.p8`, `.p8.png`, `.p8.rom`) in
Kotlin Multiplatform: its own Lua interpreter with 16:16 fixed-point numbers, the console's memory
map, graphics, text, sound, input, pause menu and cart formats. It depends on nothing but the
Kotlin stdlib and runs on the JVM, Android, iOS, macOS, Linux, JavaScript and Wasm.

It comes in three artifacts, so an app takes only what it needs:

| Artifact | Depends on | What it does |
|---|---|---|
| `kiko` | the Kotlin stdlib | Runs one cart: your app calls `step()` once per frame, draws the pixels and plays the samples. |
| `kiko-player` | `kiko`, kotlinx.coroutines | Plays it in real time without UI: the game thread, the frame clock, a hang watchdog, saves and the device's sound. |
| `kiko-compose` | `kiko-player`, Compose Multiplatform | One composable, `KikoGame`, with the screen, touch controls, keyboard and gamepad, pausing with the app. |

[Amethyst](https://github.com/vitorpamplona/amethyst) uses them to play carts posted on Nostr
inside the app.

Written from scratch from the public manual and community documentation of the formats. It is not
affiliated with the makers of any console, and contains none of their code or assets.

## Install

```kotlin
dependencies {
    implementation("com.vitorpamplona.kiko:kiko-compose:<version>") // or kiko-player, or kiko
}
```

| Requirement | |
|---|---|
| JVM | Java 17 |
| Android | minSdk 26 |
| Apple | iosArm64, iosSimulatorArm64, macosArm64 (`kiko-compose`: iOS only) |
| Linux | linuxX64 (not `kiko-compose`) |
| Web | js and wasmJs, in browsers and Node.js (`kiko-compose`: browsers) |

## Usage

### In Compose

```kotlin
import com.vitorpamplona.kiko.compose.KikoGame

// Loads the cart, plays it with touch controls, keys and sound, and pauses in the background.
KikoGame(
    key = url,
    source = { httpClient.get(url) }, // the cart's bytes, or null; kiko never downloads anything
    modifier = Modifier.fillMaxSize(),
)
```

An app that keeps the game across an Android configuration change holds a `KikoPlayer` in its
ViewModel and passes it to `KikoGame(player)`. `KikoLabels` carries the app's translations, and a
`KikoSaveStore` keeps what games save. `pauseInBackground` and `requestFocus` turn off the two
things `KikoGame` does on its own.

For a layout of your own, `KikoGame` is made of pieces you can use directly:

```kotlin
val player = rememberKikoPlayer(key = url) { httpClient.get(url) }
val input = rememberKikoInput(player) // one player's buttons, from every control
Box(Modifier.kikoKeys(input, onBack = { … }).focusable()) {
    KikoScreen(player, Modifier.fillMaxSize())
    KikoStatus(player, Modifier.fillMaxSize())
}
KikoDirectionPad(input, Modifier.size(160.dp))
KikoActionButtons(input, Modifier.size(160.dp))
```

### Without UI

```kotlin
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.runtime.Buttons

val player = KikoPlayer(source = { File(path).readBytes() }, saves = mySaveStore)
player.resume() // loads, then runs on its own thread at the game's frame rate
val pixels = IntArray(128 * 128)
player.frameNumber.collect { player.copyFrame(pixels); draw(pixels) } // ARGB, your own buffer
player.setButtons(Buttons.LEFT or Buttons.O) // player 1; setButtons(mask, playerIndex = 1) for 2
player.pause() // when the app goes to the background; close() when done
```

### The runtime alone

```kotlin
import com.vitorpamplona.kiko.cart.CartLoader
import com.vitorpamplona.kiko.runtime.ConsoleHost
import com.vitorpamplona.kiko.runtime.FantasyConsole

// .p8.png, .p8 or .p8.rom: the format is read from the bytes, not the file name.
val cart = CartLoader.load(bytes)

val console =
    FantasyConsole(
        cart,
        object : ConsoleHost {
            override fun log(message: String) = println(message) // printh
            override fun loadCartData(id: String): IntArray? = saves[id] // cartdata, 64 numbers
            override fun saveCartData(id: String, values: IntArray) { saves[id] = values }
        },
    )

// On one thread with a FantasyConsole.RECOMMENDED_STACK_BYTES stack, every 1/console.fps s:
console.setButtons(0, held) // Buttons bits: LEFT, RIGHT, UP, DOWN, O, X, PAUSE
console.step()
console.render(pixels) // IntArray(128 * 128), ARGB, with the pause menu over it when open
play(console.audioBuffer, console.audioSamples) // FantasyConsole.SAMPLE_RATE Hz mono floats

// console.state and console.error say when the cart stops.
// console.abort() stops it from any thread, even in the middle of a frame.
```

Everything a cart can reach outside itself goes through `ConsoleHost`, and each method has a
default: logging, saves (`cartdata`), the other carts of a game in several parts (`load`,
`reload`), the local time zone (`stat(90..95)`) and memory pressure.

Carts come from strangers, so loading and running treat them as hostile: a malformed or malicious
cart fails to load or stops with an error, without crashing the host, filling its memory or keeping
a core busy after `abort()`.

## What's inside

- **Lua** with the console's dialect: compound assignment, `!=`, one-line `if`/`while`, `?` for
  print, integer division `\`, the bit operators, `_ENV`, `goto`, metatables and coroutines.
  Numbers are 16:16 fixed point with the original's overflow and rounding.
- **Graphics**: every drawing call, palettes and the secondary palette, fill patterns with colors
  and transparency, clipping, camera, sprites and maps with flips and stretching, `tline`, memory
  remapping of the screen, sprite sheet and map, and the screen modes.
- **Text**: the P8SCII character set and its control codes (colors, cursor, wide, tall, inverted
  and outlined text, custom fonts in memory, one-off glyphs).
- **Sound**: the eight waveforms, effects, custom instruments, music with fades and channel masks.
- **System**: `cartdata` saves, the pause menu with `menuitem`, mouse and keyboard, the clock,
  `run(param)`, `stop`, and games in several carts.

## Status

Played against 164 carts from the public forum: 151 run and render correctly through 300 frames of
random input; 11 are the first part of a game in several carts and stop where the next cart would
load (the host decides how to find it); 2 have their own bug when a button is held through a
screen change. Known gaps: the sound filters (noise, buzz, detune, reverb, dampen) are not applied,
the kana glyphs draw blank, and there is no CPU budget (a heavy cart runs as fast as the host
allows instead of slowing down). Design notes: [`plans/`](plans).

## Try it

```bash
./gradlew :demo:run                          # a small game written for the demo
./gradlew :demo:run --args="/path/to/game.p8.png"
./gradlew :demo:wasmJsBrowserDevelopmentRun  # the same demo in a browser
```

The demo is a desktop window over `kiko-compose` (File > Open plays another cart), and a web page
that plays its own cart; the `pages.yml` workflow publishes the page to GitHub Pages from `main`.
It is not published to Maven.

## Building

```bash
./gradlew :kiko:jvmTest :kiko-player:jvmTest :kiko-compose:jvmTest
# Play a folder of carts headless and keep a screenshot of each (carts are not in this repo):
./gradlew :kiko:jvmTest --tests '*CartSmokeTest*' -Dkiko.carts=/path/to/carts -Dkiko.out=/tmp/screens
```

See [`AGENTS.md`](AGENTS.md) for the layout, the rules and the release process.

## License

MIT. Carts are not part of this project: each keeps its author's license.
