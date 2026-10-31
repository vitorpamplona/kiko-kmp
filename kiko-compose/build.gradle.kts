// Plays a cart in Compose with one call, the way an image loader shows an image: kiko-player plus
// Compose UI and lifecycle. Targets (every one Compose Multiplatform draws on), JVM level, API
// checks and publishing come from build-logic's kiko.compose-library.
plugins {
    id("kiko.compose-library")
}

kotlin {
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

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
