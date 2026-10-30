import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

// A desktop window that plays a cart with kiko-compose, to try the library without an app of
// your own: `./gradlew :demo:run`, or `./gradlew :demo:run --args="<cart>"`. Not published.
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":kiko-compose"))
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

compose.desktop {
    application {
        mainClass = "com.vitorpamplona.kiko.demo.MainKt"
    }
}
