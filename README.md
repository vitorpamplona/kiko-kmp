# Kiko: Pico-8 Player in Kotlin Multiplatform

[![Last Version](https://img.shields.io/github/release/vitorpamplona/kiko-kmp.svg?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/vitorpamplona/kiko-kmp/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/com.vitorpamplona.kiko/kiko?label=Maven%20Central&labelColor=27303D&color=0877d2)](https://central.sonatype.com/artifact/com.vitorpamplona.kiko/kiko)
[![JitPack snapshots](https://img.shields.io/badge/snapshots-JitPack-27303D?labelColor=27303D&color=0877d2)](https://jitpack.io/#vitorpamplona/kiko-kmp)
[![CI](https://img.shields.io/github/actions/workflow/status/vitorpamplona/kiko-kmp/build.yml?labelColor=27303D)](https://github.com/vitorpamplona/kiko-kmp/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/github/license/vitorpamplona/kiko-kmp?labelColor=27303D&color=0877d2)](LICENSE)

Plays fantasy-console games (128x128, 16 colors, `.p8` and `.p8.png` cartridges) inside any
Kotlin Multiplatform app: Android, iOS, desktop and the web. Written from scratch, with its own Lua
interpreter; not affiliated with the makers of any console.

## Try it

Play the demo [in your browser](https://vitorpamplona.com/kiko-kmp/), or run it on your desktop:

```bash
./gradlew :demo:run
```

[`demo/`](demo) also runs on Android and iOS.

## Add it to your app

```kotlin
implementation("com.vitorpamplona.kiko:kiko-compose:0.2.0")
```

```kotlin
KikoGame(key = url, source = { download(url) }, modifier = Modifier.fillMaxSize())
```

That is the whole integration: the game, touch controls, keyboard, gamepad and sound, paused while
your app is in the background. kiko never downloads anything; `download` is your own code.

When you need more, each of these is a short, tested example:

- [Keep the game across a rotation, save high scores, use your own words](demo/shared/src/commonMain/kotlin/com/vitorpamplona/kiko/demo/examples/KeepTheGame.kt)
- [Lay out the screen and controls yourself](demo/shared/src/commonMain/kotlin/com/vitorpamplona/kiko/demo/examples/OwnLayout.kt)
- [Play without Compose](demo/shared/src/commonMain/kotlin/com/vitorpamplona/kiko/demo/examples/Headless.kt) (`kiko-player`), or [drive the frame loop yourself](kiko) (`kiko`)

Runs on Android 8+, iOS, the JVM (Java 17+) and browsers, with Kotlin 2.4 and Compose
Multiplatform 1.12.

## Status

151 of 164 games tested from the public forum run and draw through a few seconds of random input.
Sound plays on Android and desktop only for now. Details and known gaps: [`kiko/`](kiko).

## Contributing

[`AGENTS.md`](AGENTS.md) has the layout, the rules, how to build and test, and how to release.

## License

MIT. Games keep their authors' licenses; the ones in the demo are credited in
[`demo/CREDITS.md`](demo/CREDITS.md).
