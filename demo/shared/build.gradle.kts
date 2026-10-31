import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

// Plays a cart with kiko-compose, to try the library without an app of your own. Not published.
//   Desktop: ./gradlew :demo:run, or ./gradlew :demo:run --args="<cart>"
//   Browser: ./gradlew :demo:wasmJsBrowserDevelopmentRun (the Pages workflow publishes it)
//   Android: ./gradlew :demo-android:installDebug (the app module around this one)
//   iOS:     open demo/ios/KikoDemo.xcodeproj in Xcode, which builds the DemoKit framework here
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "com.vitorpamplona.kiko.demo.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // The bundled games are Compose resources.
        androidResources.enable = true
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "DemoKit"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "demo.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kiko-compose"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                // The bundled games (composeResources/files/carts; see demo/CREDITS.md).
                implementation(libs.compose.components.resources)
                // For the examples/KeepTheGame example only.
                implementation(libs.lifecycle.viewmodel.compose)
            }
        }

        androidMain {
            dependencies {
                // SystemBack: the Back gesture opens the game's pause menu first.
                implementation(libs.androidx.activity.compose)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.vitorpamplona.kiko.demo.resources"
}

compose.desktop {
    application {
        mainClass = "com.vitorpamplona.kiko.demo.MainKt"
    }
}
