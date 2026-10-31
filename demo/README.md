# Demo

One app on every platform, built on `kiko-compose`: a list of games, each credited on screen, and
on all but the browser a button to open a cart of your own. Not published to Maven.

| Where | How |
|---|---|
| Browser | [vitorpamplona.com/kiko-kmp](https://vitorpamplona.com/kiko-kmp/), or `./gradlew :demo:wasmJsBrowserDevelopmentRun` |
| Desktop | `./gradlew :demo:run`, or `./gradlew :demo:run --args="/path/to/game.p8.png"` |
| Android | `./gradlew :demo-android:installDebug`, with a phone or emulator connected |
| iOS | Open `ios/KikoDemo.xcodeproj` in Xcode, pick a simulator or your phone, Run |

The iOS build runs Gradle from Xcode, so Xcode needs a JDK 17+ it can find (`JAVA_HOME`, or
`org.gradle.java.home` in `~/.gradle/gradle.properties`) and an Android SDK (`ANDROID_HOME` or
`local.properties`), since every module also builds for Android. On a phone, pick your team under
Signing & Capabilities. Games are silent on iOS for now.

## The games

The demo's own cart, *Snowman* by aquova (CC0) and *1492* by A.C. Wright Design (MIT). Thanks to
them for sharing their games; [`CREDITS.md`](CREDITS.md) has the sources and licenses.

## The code

- [`shared/.../examples/`](shared/src/commonMain/kotlin/com/vitorpamplona/kiko/demo/examples):
  short, tested examples of the API, linked from the main README.
- [`shared/.../DemoScreen.kt`](shared/src/commonMain/kotlin/com/vitorpamplona/kiko/demo/DemoScreen.kt):
  the app itself, one `KikoGame` under the game list.
- `shared/src/{jvm,wasmJs,ios}Main`, [`android/`](android) and [`ios/`](ios): each platform's
  entry point.
