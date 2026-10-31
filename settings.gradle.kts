pluginManagement {
    // kiko.library and kiko.compose-library: what the published modules share.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kiko-kmp"
include(":kiko")
include(":kiko-player")
include(":kiko-compose")
// The demo is one app in demo/: the shared code and the desktop, browser and iOS entry points in
// demo/shared, the Android app around it in demo/android, the Xcode project in demo/ios.
include(":demo")
project(":demo").projectDir = file("demo/shared")
include(":demo-android")
project(":demo-android").projectDir = file("demo/android")
