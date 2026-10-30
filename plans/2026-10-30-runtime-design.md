# Fantasy-console cart runtime — 2026-10-30

Status: written and tested; moved from Amethyst's `fantasyConsole` module into this repository
as `com.vitorpamplona.kiko`. Amethyst's player (detection in notes, the touch controls, saves)
stays in Amethyst; this note covers the runtime.

## Goal

Play 128x128 fantasy-console cartridges (`.p8`, `.p8.png`) natively, first in Amethyst (carts
posted on Nostr, for example as a `blossom:<sha256>.png` URI) instead of in a WebView. The runtime
is written from scratch in Kotlin Multiplatform so every platform shares it.

## Why from scratch

- The original runtime is proprietary; it cannot ship inside an MIT library or app.
- LuaJ (MIT, Lua 5.2) is JVM-only and counts in doubles. Carts depend on 16:16 fixed-point
  overflow and rounding, and on the console's syntax, so a Lua engine needed changes anyway.
- Nothing here is copied: the API, syntax and memory map come from the public manual, and the
  file formats from community documentation. The 3x5 font is our own drawing (the original font's
  license is unconfirmed). The library never uses the console's name in its API.

## Layout (`com.vitorpamplona.kiko`, stdlib only)

| Package | What |
|---|---|
| `lua` | `Fix` (16:16 math), `Lexer`, `Parser` → executable AST (`Ast.kt`), `Interpreter` (metatables, coroutines), `LuaTable` |
| `cart` | `CartLoader` (`.p8.png`, `.p8`, `.p8.rom`, detected by content), `CodeCompression` (`:c:` and `pxa`), `P8scii` |
| `png` | `PngDecoder` + `Inflate`, so `commonMain` needs no `java.util.zip` |
| `runtime` | `FantasyConsole` (frame loop, host API), `Ram`, `Graphics`, `Printer` (P8SCII text), `Font`, `Audio`, `Input`, `PauseMenu`, `Clock`, `Api` (the globals) |

Design points:

- **Numbers** are the raw 32-bit pattern in an `Int`; `+`/`-` wrap for free.
- **Locals** resolve to frame slots at parse time; only captured locals are boxed in a `Cell`.
- **Coroutines** are Kotlin coroutines. `coresume`/`yield`/`flip` post to a one-slot
  `Scheduler` and suspend, so the native stack never grows with resumes (20,000 resumes in one
  frame is a test).
- **Draw state lives in RAM** at its documented address, so carts that `poke` it work.
- **The cart's frame loop is Lua.** Like the original, the console copies 60 built-ins into
  top-level locals on the cart's first line (so a function whose `_ENV` is a plain table still
  reaches `circfill`) and appends the `_init`/`_update`/`_draw` loop after the cart's code (so it
  sees a cart's `local function _draw()`). Line numbers are unchanged.
- **Busy waits end the frame.** Loop back-edges call a hook every 1024 iterations; outside
  `_draw`, a cart past its frame time yields there, so `while time() < 3 do end` advances time.
- The host calls `step()` once per frame, then reads `render()` and `audioBuffer`
  (22,050 Hz mono). Run it on a thread with `RECOMMENDED_STACK_BYTES` (64 MiB of reserved
  stack): some carts run an interpreter of their own that recurses over 10,000 calls deep. A
  deeper chain ends the cart with "stack overflow" (`Interpreter.MAX_DEPTH`, 50,000), not a crash.
- **Games in several carts** ask the host for the next one (`ConsoleHost.loadCart`) on
  `load(name, breadcrumb, param)` and `reload(…, name)`; `extcmd("breadcrumb")` goes back.

## Verified

- 56 unit tests: fixed-point edge cases, every syntax shorthand, closures per iteration, varargs,
  metatables, goto, coroutines, `all()` with deletion, `_ENV`, frame loop, `flip()` loops, busy
  waits, errors with line numbers, cart data, graphics primitives, palettes, clip, camera, fill
  patterns with colors and inversion, out-of-bounds values, map and screen remapping, print modes,
  control codes and custom fonts, the pause menu and `menuitem`, mouse and keyboard, `run(param)`,
  multi-cart `load`/`reload`, both code compressions, P8SCII round trip, and a `.p8.png` written by
  the JDK's PNG encoder.
- 164 carts from the public cart forum (kept out of the repo, they are CC BY-NC-SA): its featured
  list plus recent posts, 300 frames each with buttons pressed at random after frame 15. 151 run
  and render recognizably (platformers, raycasters, a music tracker, card games, a fighting game,
  minified and `_ENV`-heavy code, custom fonts). 11 are the first cart of a multi-cart game and
  stop with a clear message, since the test host supplies no next cart. 2 hit their own bug
  (a nil read) when a button is held through a screen change, which the original console does
  too. Reproduce with `./gradlew :kiko:jvmTest --tests '*CartSmokeTest*'
  -Dkiko.carts=<dir> -Dkiko.out=<dir>`.
- Semantics were cross-checked against the public manual and two open-source tools' readings of
  it, without running or copying their code.
- Speed on the desktop JVM, steady state: 1.3-5.3 ms per frame for the heaviest of those carts
  (budget: 33 ms at 30 fps). Profile: table lookups ~15%, continuation allocation ~13%.
- The common tests pass on the JVM, Android (host), Linux native and Kotlin/JS; Wasm and the
  Apple targets compile, and CI runs their tests.
- Hostile input (`HostileCartTest`): oversized PNG chunks, decompression bombs, deeply nested
  code, endless loops in `_draw`, recursion without loops and huge draws all fail cleanly.

## Known gaps

- Sound: waveforms and effects are approximations, and the per-sfx filters (noiz, buzz, detune,
  reverb, dampen) are not applied. Custom instruments, fades, music masks and `\a` in `print`
  work.
- Games in several carts need a host that answers `loadCart`; Amethyst doesn't yet, since a
  Nostr post has no way to name its other carts.
- Kana glyphs (154-253) draw blank. `serial`, `export`, `cstore` and `printh` to a file do
  nothing.
- No CPU budget: a heavy cart runs as fast as the host thread allows instead of slowing down.
  `stat(1)` is wall-clock time over the frame, capped at 2.
- Exactness of `rnd` sequences, `atan2(0, 0)` and number printing is from community docs, not
  checked against the original.

## Next steps

1. Sound filters.
2. Kana glyphs.
3. A CPU budget, so a heavy cart slows down like on the original instead of running flat out.
4. Performance on low-end Android devices, measured on a phone.
