import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

// Plays a cart in Compose with one call, the way an image loader shows an image: kiko-player
// plus Compose UI and lifecycle. Every target Compose Multiplatform draws on, so no linuxX64
// or macOS.
kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "com.vitorpamplona.kiko.compose"
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
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                // In the API: KikoGame takes a KikoPlayer.
                api(project(":kiko-player"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.lifecycle.runtime.compose)
            }
        }

        // Everything Compose draws with Skia: the desktop JVM, iOS and the web.
        val skikoMain =
            create("skikoMain") {
                dependsOn(commonMain.get())
            }
        jvmMain.get().dependsOn(skikoMain)
        iosMain.get().dependsOn(skikoMain)
        webMain.get().dependsOn(skikoMain)

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
