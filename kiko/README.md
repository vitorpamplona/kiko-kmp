# kiko

The runtime: one cartridge, one frame at a time, on the Kotlin stdlib alone. Use it directly when
you run your own frame loop; otherwise [`kiko-player`](../kiko-player) and
[`kiko-compose`](../kiko-compose) do it for you (see the [main README](../README.md)).

```kotlin
implementation("com.vitorpamplona.kiko:kiko:0.2.0")
```

Runs on the JVM, Android, iOS, macOS, Linux, and JS and Wasm (browsers and Node.js).

## Usage

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
screen change.

Known gaps:

- The sound filters (noise, buzz, detune, reverb, dampen) are not applied, and the kana glyphs
  draw blank.
- There is no CPU budget: a heavy cart runs as fast as the host allows instead of slowing down.
- `kiko-player` plays sound on Android and the JVM only; on iOS, macOS, Linux and the web games
  are silent for now (an app may pass its own `KikoAudioSink`).
- On the web there is one thread: a frame that never ends freezes the page, since nothing else
  can run to stop it.

To play a folder of carts headless and keep a screenshot of each (carts are not in this
repository):

```bash
./gradlew :kiko:jvmTest --tests '*CartSmokeTest*' -Dkiko.carts=/path/to/carts -Dkiko.out=/tmp/screens
```

Design notes: [`plans/`](../plans).
