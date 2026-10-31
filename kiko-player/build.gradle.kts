// Plays a cart in real time, headless: kiko plus kotlinx.coroutines, nothing else. The thread, the
// clock and the sound device live here so kiko itself keeps none. Targets, JVM level, API checks
// and publishing come from build-logic's kiko.library.
plugins {
    id("kiko.library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Both are in the API: KikoPlayer takes a KikoPlayerHost and hands out StateFlows.
                api(project(":kiko"))
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // The thread with a deep stack and the heap check, shared by the JVM and Android.
        val jvmAndroidMain =
            create("jvmAndroidMain") {
                dependsOn(commonMain.get())
            }
        jvmMain.get().dependsOn(jvmAndroidMain)
        androidMain.get().dependsOn(jvmAndroidMain)
    }
}
