import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    // A library: every public declaration is a choice, and the ABI dump in api/ records it,
    // so a breaking change shows up in review (and in a major version) instead of by accident.
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    // JVM 17: a dependency can't need a newer JVM than the apps and libraries that use it.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "com.vitorpamplona.kiko"
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

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()

    // The web: a cart player in a page. Kotlin/JS emulates Long, which the runtime barely uses
    // (numbers are 16:16 fixed point in an Int).
    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

// Forwards the opt-in cart smoke test's settings to the test JVM (see CartSmokeTest): a folder of
// real carts, which stay out of the repository since most are CC BY-NC-SA.
tasks.withType<Test>().configureEach {
    listOf("kiko.carts", "kiko.out", "kiko.frames", "kiko.only").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    outputs.upToDateWhen { System.getProperty("kiko.carts") == null }
    testLogging.showStandardStreams = System.getProperty("kiko.carts") != null
    System.getProperty("kiko.jfr")?.let { jvmArgs("-XX:StartFlightRecording=filename=$it,settings=profile") }
}
