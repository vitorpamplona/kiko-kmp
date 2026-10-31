# kiko-kmp

A from-scratch Kotlin Multiplatform runtime for 128x128, 16-color fantasy-console cartridges
(`.p8`, `.p8.png`, `.p8.rom`): a Lua dialect with 16:16 fixed-point numbers, memory, graphics,
sound, input and cart loading, on the Kotlin stdlib alone. Published to Maven Central as
`com.vitorpamplona.kiko:kiko`, with two artifacts on top: `kiko-player` (plays a cart in real
time, headless) and `kiko-compose` (one composable that plays it in Compose). It started in
[Amethyst](https://github.com/vitorpamplona/amethyst), which plays carts posted on Nostr with it,
and moved here so any app can embed it.

## Layout

- `kiko/` — the library, in `com.vitorpamplona.kiko.*`:
  - `cart` — the public loading API: `CartLoader` (`.p8.png`, `.p8`, `.p8.rom`, chosen by content,
    never by file name), `Cart`, `CartFormatException`, `P8scii` (the console's character set),
    and the internal `CodeCompression` (`:c:` and `\0pxa`).
  - `runtime` — the public running API: `FantasyConsole` (the frame loop: `step`, `render`,
    `audioBuffer`, input) and `ConsoleHost` (what the embedding app supplies: logging, saves,
    other carts, time zone, memory pressure). Everything else there is internal: `Ram` and the
    memory map in `Addr`, `Graphics`, `Printer` (P8SCII text), `Font`, `Audio`, `Input`,
    `PauseMenu`, `Clock`, and `Api`, which installs the cart-facing globals.
  - `lua` (internal) — `Fix` (16:16 math), `Lexer`, `Parser` (to an AST that evaluates itself,
    `Ast.kt`), `Interpreter` (metatables, coroutines, limits), `LuaTable`, `LuaValues`.
  - `png` (internal) — `PngDecoder` and `Inflate`, so `commonMain` needs no `java.util.zip`.
- `kiko/api/` — the public ABI dump (`checkKotlinAbi`).
- `kiko-player/` — `com.vitorpamplona.kiko.player`, on kiko and kotlinx.coroutines: `KikoPlayer`
  (loads a cart from a `source` lambda, runs it on a game thread at its frame rate, publishes
  `status` and `frameNumber`, hands frames out with `copyFrame` into the caller's buffer, takes
  input for eight players from any thread, a watchdog for frames that never end), `KikoPlayerHost`
  (log, time zone, other carts), `KikoSaveStore` and the internal `SaveBuffer` (debounced writes, a
  cap on new save ids), `KikoAudioSink`. An internal constructor takes the game dispatcher, clock
  and save scope, so tests run on virtual time. Platform code: the game thread and heap check
  (`jvmAndroidMain`, `nativeMain`, `webMain`), the sound output (`AudioTrack` on Android,
  `javax.sound` on the JVM, none elsewhere).
- `kiko-compose/` — `com.vitorpamplona.kiko.compose`, on kiko-player, Compose ui/foundation and
  lifecycle-runtime-compose: `KikoGame` (two overloads: a `source` lambda, or a `KikoPlayer` the
  app keeps) built from public pieces an app may lay out itself: `KikoScreen`, `KikoStatus`,
  `KikoDirectionPad`, `KikoActionButtons`, `Modifier.kikoKeys`, joined by `KikoInput`
  (`rememberKikoInput`). Also `rememberKikoPlayer` and `KikoLabels`. Platform code: the frame
  bitmap and edge-gesture exclusion (`androidMain`, `skikoMain`). No Material, no string
  resources, no app chrome.
- `demo/` — one demo app on every platform, with a small cart of our own (`DemoCart.kt`) and one
  screen (`DemoScreen`). `demo/shared` (Gradle `:demo`) holds them with the desktop
  (`./gradlew :demo:run [--args=<cart>]`), browser (`wasmJsMain`,
  `:demo:wasmJsBrowserDevelopmentRun`; `pages.yml` publishes it) and iOS (`iosMain`, the `DemoKit`
  framework) entry points; `demo/android` (`:demo-android`, `installDebug`) is the Android app
  around it; `demo/ios` is the Xcode project (SwiftUI shell and file importer), which builds
  `DemoKit` through Gradle. None of it is published to Maven. `demo/shared/.../examples/` holds
  the short, tested examples the README links to (`OneCall`, `KeepTheGame`, `OwnLayout`,
  `Headless`, `RuntimeAlone`): show the API there, in the fewest lines that work, not in the
  README's own code blocks, which nothing compiles.
- `build-logic/` — the build's own plugins: `kiko.library` (a published module with no UI, every
  target) and `kiko.compose-library` (a published Compose module) set the targets, JVM 17, the
  Android SDK and namespace, the explicit API and its check, and publishing. A module's own build
  file holds only its dependencies and source sets.
- `plans/` — design notes, `YYYY-MM-DD-<slug>.md`.
- `tools/agent-hooks/` — the checks Claude Code and Codex run before a push (see below).

Tests: kiko's `commonTest` runs on every target (language, fixed point, graphics, console features);
`jvmTest` holds what needs the JVM: `PngCartTest` (a cart written by the JDK's PNG encoder),
`HostileCartTest` (crafted input that must fail cleanly) and `CartSmokeTest`, which plays a folder
of real carts given with `-Dkiko.carts=<dir>` and skips itself otherwise. kiko-player's
`SaveBufferTest` and `KikoPlayerTest` (virtual time) run everywhere, `KikoPlayerThreadTest` (a real
thread, a frame that never returns) on the JVM; kiko-compose's `ControlsTouchTest` and
`KikoGameTest` draw off screen with Skia on the JVM.

## Rules

- **No dependencies in `kiko`** beyond the Kotlin stdlib (and `kotlin-test` for tests). Depending
  on kiko must never bring anything else onto a classpath. `kiko-player` adds kotlinx.coroutines
  only; `kiko-compose` adds Compose runtime/ui/foundation and lifecycle-runtime-compose only (no
  Material). Keep their versions at or below Amethyst's. A new dependency needs its license
  checked against its actual `LICENSE` file first, and a very good reason.
- **Clean room.** The original console's runtime is proprietary. Behaviour comes from its public
  manual and from community documentation of the file formats; other implementations may be read
  to learn what a feature does, never copied, and never run on this machine. The font is our own
  drawing. Never put the original console's name in the API, a package, an artifact or a class.
- **No app concerns.** No Nostr, no networking, no file system, no logging, in any module. `kiko`
  starts no threads: everything outside the cart goes through `ConsoleHost`, and the host owns
  the thread that calls `step()`. `kiko-player` is that host when an app wants one: it is the only
  module that starts threads or opens a sound device, and it still takes the cart's bytes from a
  `source` lambda and its saves through `KikoSaveStore`.
- **Every cart is hostile.** Carts come from strangers. A malformed or malicious one may fail to
  load or stop with an error, but must never crash the host, exhaust its memory or stack, or keep
  a core busy after `abort()`. Keep the limits that enforce this (chunk lengths and inflate output
  in `png`, parser nesting, `Interpreter.MAX_DEPTH` and `MAX_STRING`, abort checks on loop back
  edges and calls, drawing clipped before it loops, `ConsoleHost.memoryLow`), and give each new
  one a test in `HostileCartTest` that fails without it.
- **Carts are not committed,** with one exception. Most published carts are CC BY-NC-SA or carry
  no license at all. Tests build their carts in code; `CartSmokeTest` reads real ones from a
  folder outside the repository. The exception is the demo's games
  (`demo/shared/src/commonMain/composeResources/files/carts/`): a cart goes there only under a
  permissive license (CC0, MIT, CC BY) that covers all of it, art, sound and any code borrowed
  from someone else included, unchanged, with its full license text beside it, an entry in
  `DEMO_GAMES` that shows its author and license in the app, and a row in `demo/CREDITS.md` naming
  the source commit. When in doubt, leave it out and record why in `demo/CREDITS.md`. Never
  download carts from the Lexaloffle BBS with a script: its terms forbid automated access.
- **Match the original's semantics**, observable ones included: 16:16 overflow and rounding,
  `btnp` repeat timing, draw state in RAM at its documented address, line numbers in errors.
  When the manual is silent, say so in a comment rather than guess silently.
- `commonMain` and `commonTest` stay free of JVM-only APIs: Linux, Apple and the web compile them
  as common code. For `@Volatile` import `kotlin.concurrent.Volatile`; in tests use
  `assertTrue`, not `assert`. `HostileCartTest`, `PngCartTest` and `CartSmokeTest` are JVM-only
  on purpose.
- **Thread model.** One host thread calls `step()`, `render()` and the input setters; only
  `abort()` may come from another thread. Run it with `FantasyConsole.RECOMMENDED_STACK_BYTES` of
  stack: each Lua call takes several native frames. `KikoPlayer` is built on this: its input
  methods hop to the game thread, and only its watchdog and `close()` call `abort()`.
- **Explicit API mode** is on in the three published modules: every public declaration says
  `public`, and each module's `api/` folder records its public ABI. A deliberate API change runs
  `./gradlew updateKotlinAbi` and commits the new dumps; anything that should not be API is
  `internal`. Keep kiko's public surface to `cart` and `FantasyConsole`/`ConsoleHost`/`Buttons`,
  kiko-player's to `KikoPlayer` and its three interfaces, and kiko-compose's to `KikoGame`, its
  pieces (`KikoScreen`, `KikoStatus`, `KikoDirectionPad`, `KikoActionButtons`, `kikoKeys`,
  `KikoInput`), `rememberKikoPlayer` and `KikoLabels`.
- JVM target is 17: a library can't need a newer JVM than the apps that use it.
- Commits: conventional commits (`feat:`, `fix:`, …). Never `--no-verify`.

## Verify, don't guess

Don't assert a diagnosis you haven't reproduced. If a claim is checkable in under a minute, check
it before stating it: write the failing case first (a few lines of Lua in a test), watch it fail,
then explain. For behaviour on real games, run `CartSmokeTest` on a folder of carts and look at
the screenshots it writes.

## Build and test

```bash
./gradlew installGitHook                        # pre-commit (spotless) and pre-push (checks + tests)
./gradlew spotlessApply
./gradlew :kiko:jvmTest                         # also `./gradlew test`
./gradlew :kiko:jvmTest --tests "com.vitorpamplona.kiko.LuaTest"
./gradlew checkKotlinAbi                        # updateKotlinAbi after a deliberate change
./gradlew :kiko-player:jvmTest :kiko-compose:jvmTest :demo:jvmTest
./gradlew :demo:run --args="<cart>"             # try a cart in a window
./gradlew :demo:wasmJsBrowserDevelopmentRun     # ...or in a browser
./gradlew checkLinux checkWebNode               # everything the pre-push hook runs
./gradlew checkWeb                              # JS and Wasm in Node.js and headless Chrome
./gradlew checkApple                            # macOS only
# Real carts, with a screenshot of each in <out>:
./gradlew :kiko:jvmTest --tests '*CartSmokeTest*' -Dkiko.carts=<dir> -Dkiko.out=<out>
```

The check lists live once, as tasks in the root `build.gradle.kts`: `checkLinux` (formatting, the
API checks, the JVM, Android host and Linux tests, the common metadata compile), `checkWeb` /
`checkWebNode` (JS and Wasm) and `checkApple`. The pre-push hook runs `checkLinux checkWebNode`
and the agent-config check; CI's `build` job runs `checkLinux checkWeb` and its `test-apple` job
`checkApple`; the release runs all three. A new module or test task goes in those lists, not in
the workflows.

## Dependency licensing

Before adding a third-party dependency, read its actual `LICENSE`/`COPYING` file or published
POM, not your memory. kiko is MIT and ships inside apps.

- Permissive (MIT, Apache-2.0, BSD, ISC, MPL-2.0, zlib, …): OK.
- LGPL, or GPL/EPL with a linking or Classpath exception: WARN. Acceptable, but say so in your
  summary, and confirm the exception exists in the license text.
- Anything stricter (GPL/AGPL without a linking exception, SSPL, proprietary): STOP. Don't add it;
  surface it and require an explicit call-out in the PR description for a maintainer to decide.

## Kotlin style

- **Hot paths allocate nothing they don't need.** The interpreter runs every cart instruction:
  numbers stay `Int` bit patterns, tables keep an array part, and nodes take their fast path
  before calling into `Interpreter`. Measure with `CartSmokeTest` (`-Dkiko.jfr=<file>` records
  a profile) before and after a change there.
- **Never write fully-qualified class names inline in function bodies**: import the class (use
  `import … as Alias` for a real collision). Comments, KDoc and string literals are exempt.
- **Never put raw invisible or bidirectional Unicode characters in source files**: write them as
  `\uXXXX` escapes (U+202A–U+202E, U+2066–U+2069, U+200E/U+200F, U+061C, U+200B–U+200D, U+FEFF,
  U+2060), even when the character is intentional test data. P8SCII tables are data: escape them
  too.

## Agent configuration (Claude Code + Codex)

Both agents read the same rules, skills and checks, laid out like Amethyst's and Quartz's:

- **This file is the single source of rules.** Codex reads `AGENTS.md`; Claude Code reads
  `.claude/CLAUDE.md`, which imports it with `@../AGENTS.md` and adds only Claude-specific notes.
  Never write a rule in a `CLAUDE.md`.
- **Skills are written once**, in `.claude/skills/<name>/`, and symlinked from
  `.agents/skills/<name>`. Never copy a skill into `.agents/`. Here: `ngit-pr` (PRs on GitHub or as nostr
  proposals).
- **A skill's `description` is how both agents decide to load it.** Keep its concrete class
  names, paths and commands; 120–300 characters, strongest triggers first.
- **Shared checks live in `tools/agent-hooks/`.** `.claude/settings.json` and `.codex/hooks.json`
  run the same pre-push scripts (spotless, agent config), and CI and the git pre-push hook run
  `agent_config_check.py`.

## Versioning

[Semantic versioning](https://semver.org/) over the public API: every `public` declaration in
`kiko/`, `kiko-player/` and `kiko-compose/`, in source and binary form. Before 1.0.0 a minor version
may break the API; from 1.0.0 on a breaking change needs a major version, and goes out first as a
deprecation (`@Deprecated("Use X instead. Removed in 2.0.0.", ReplaceWith("X"))`) in a minor.
Amethyst pins a version, so a breaking change needs a matching change there.

## Remotes and pull requests

The repo is on GitHub, `github.com/vitorpamplona/kiko-kmp`, which holds the canonical `main`, and on
nostr as a NIP-34 repository,
`nostr://npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/kiko-kmp`: a
push to that remote goes to its GRASP git servers and to GitHub. A PR can go out either way: a
GitHub PR (`gh`), or a nostr proposal (`ngit`, a `pr/…` branch, reviewed on
[gitworkshop.dev](https://gitworkshop.dev/npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/kiko-kmp)).
Remote names vary per clone, so identify them by URL. Open a PR only when the human you work for
asks, and before opening, revising or merging one by either path, use the `ngit-pr` skill.

## Releasing

Bump `VERSION_NAME` in `gradle.properties` (all three artifacts share it), then push a
`v<version>` tag. The
`create-release.yml` workflow runs the Apple tests on macOS, then tests, publishes to Maven
Central and opens the GitHub release. It needs `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`,
`SIGNING_PRIVATE_KEY` and `SIGNING_PASSWORD`, the same secrets as crypto-kmp and Quartz.
JitPack builds snapshots of any commit (`jitpack.yml`): the common metadata, JVM and Android.
