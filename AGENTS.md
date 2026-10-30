# kiko-kmp

A from-scratch Kotlin Multiplatform runtime for 128x128, 16-color fantasy-console cartridges
(`.p8`, `.p8.png`, `.p8.rom`): a Lua dialect with 16:16 fixed-point numbers, memory, graphics,
sound, input and cart loading, on the Kotlin stdlib alone. Published to Maven Central as
`com.vitorpamplona.kiko:kiko`. It started in [Amethyst](https://github.com/vitorpamplona/amethyst),
which plays carts posted on Nostr with it, and moved here so any app can embed it.

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
- `plans/` — design notes, `YYYY-MM-DD-<slug>.md`.
- `tools/agent-hooks/` — the checks Claude Code and Codex run before a push (see below).

Tests: `commonTest` runs on every target (language, fixed point, graphics, console features);
`jvmTest` holds what needs the JVM: `PngCartTest` (a cart written by the JDK's PNG encoder),
`HostileCartTest` (crafted input that must fail cleanly) and `CartSmokeTest`, which plays a folder
of real carts given with `-Dkiko.carts=<dir>` and skips itself otherwise.

## Rules

- **No dependencies** beyond the Kotlin stdlib (and `kotlin-test` for tests). Depending on kiko
  must never bring anything else onto a classpath. A new dependency needs its license checked
  against its actual `LICENSE` file first, and a very good reason.
- **Clean room.** The original console's runtime is proprietary. Behaviour comes from its public
  manual and from community documentation of the file formats; other implementations may be read
  to learn what a feature does, never copied, and never run on this machine. The font is our own
  drawing. Never put the original console's name in the API, a package, an artifact or a class.
- **No app concerns.** No Nostr, no networking, no file system, no logging, no threads of its
  own: everything outside the cart goes through `ConsoleHost`, and the host owns the thread that
  calls `step()`.
- **Every cart is hostile.** Carts come from strangers. A malformed or malicious one may fail to
  load or stop with an error, but must never crash the host, exhaust its memory or stack, or keep
  a core busy after `abort()`. Keep the limits that enforce this (chunk lengths and inflate output
  in `png`, parser nesting, `Interpreter.MAX_DEPTH` and `MAX_STRING`, abort checks on loop back
  edges and calls, drawing clipped before it loops, `ConsoleHost.memoryLow`), and give each new
  one a test in `HostileCartTest` that fails without it.
- **Carts are never committed.** Most published carts are CC BY-NC-SA. Tests build their carts in
  code; `CartSmokeTest` reads real ones from a folder outside the repository.
- **Match the original's semantics**, observable ones included: 16:16 overflow and rounding,
  `btnp` repeat timing, draw state in RAM at its documented address, line numbers in errors.
  When the manual is silent, say so in a comment rather than guess silently.
- `commonMain` and `commonTest` stay free of JVM-only APIs: Linux, Apple and the web compile them
  as common code. For `@Volatile` import `kotlin.concurrent.Volatile`; in tests use
  `assertTrue`, not `assert`. `HostileCartTest`, `PngCartTest` and `CartSmokeTest` are JVM-only
  on purpose.
- **Thread model.** One host thread calls `step()`, `render()` and the input setters; only
  `abort()` may come from another thread. Run it with `FantasyConsole.RECOMMENDED_STACK_BYTES` of
  stack: each Lua call takes several native frames.
- **Explicit API mode** is on: every public declaration says `public`, and `kiko/api/` records
  the public ABI. A deliberate API change runs `./gradlew :kiko:updateKotlinAbi` and commits the
  new dump; anything that should not be API is `internal`. Keep the public surface to `cart` and
  `FantasyConsole`/`ConsoleHost`.
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
./gradlew :kiko:checkKotlinAbi                  # :kiko:updateKotlinAbi after a deliberate change
./gradlew :kiko:testAndroidHostTest :kiko:linuxX64Test :kiko:jsNodeTest :kiko:wasmJsNodeTest
./gradlew :kiko:iosSimulatorArm64Test :kiko:macosArm64Test   # macOS only
# Real carts, with a screenshot of each in <out>:
./gradlew :kiko:jvmTest --tests '*CartSmokeTest*' -Dkiko.carts=<dir> -Dkiko.out=<out>
```

The pre-push hook runs `spotlessCheck`, the ABI check, the JVM, Android host, Linux, JS and Wasm
(Node.js) tests, the common metadata compile and the agent-config check: CI's `build` job, minus
headless Chrome. The Apple tests run in CI's `test-apple` job.

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
  `.agents/skills/<name>`. Never copy a skill into `.agents/`. There are none yet.
- **A skill's `description` is how both agents decide to load it.** Keep its concrete class
  names, paths and commands; 120–300 characters, strongest triggers first.
- **Shared checks live in `tools/agent-hooks/`.** `.claude/settings.json` and `.codex/hooks.json`
  run the same pre-push scripts (spotless, agent config), and CI and the git pre-push hook run
  `agent_config_check.py`.

## Versioning

[Semantic versioning](https://semver.org/) over the public API: every `public` declaration in
`kiko/`, in source and binary form. Before 1.0.0 a minor version may break the API; from 1.0.0 on
a breaking change needs a major version, and goes out first as a deprecation
(`@Deprecated("Use X instead. Removed in 2.0.0.", ReplaceWith("X"))`) in a minor. Amethyst pins
a version, so a breaking change needs a matching change there.

## Releasing

Bump `VERSION_NAME` in `gradle.properties`, then push a `v<version>` tag. The
`create-release.yml` workflow runs the Apple tests on macOS, then tests, publishes to Maven
Central and opens the GitHub release. It needs `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`,
`SIGNING_PRIVATE_KEY` and `SIGNING_PASSWORD`, the same secrets as crypto-kmp and Quartz.
JitPack builds snapshots of any commit (`jitpack.yml`): the common metadata, JVM and Android.
