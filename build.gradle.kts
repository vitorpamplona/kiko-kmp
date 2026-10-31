import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.diffplugSpotless)
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("src/**/*.kt")

            // Same ktlint and header as Amethyst, where this code started, and the other
            // libraries, so a file moved between the repos formats identically on every side.
            ktlint("1.7.1")
            licenseHeaderFile(
                rootProject.file("spotless/copyright.kt"),
                "@file:|package|import|class|object|sealed|open|interface|abstract ",
            )
        }
    }
}

subprojects {
    // What every published artifact shares; each module names and describes itself with
    // POM_ARTIFACT_ID, POM_NAME and POM_DESCRIPTION in its own gradle.properties.
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            configure(KotlinMultiplatform(sourcesJar = SourcesJar.Sources()))

            // groupId, artifactId, and version come from gradle.properties (GROUP,
            // POM_ARTIFACT_ID, VERSION_NAME) so CI can override VERSION_NAME via
            // ORG_GRADLE_PROJECT_VERSION_NAME without conflicting with a hardcoded value.
            publishToMavenCentral(automaticRelease = true)

            // Sign only when a key is configured: the release workflow passes one, while JitPack and
            // local publishToMavenLocal builds have none and would otherwise fail on the signing task.
            // Maven Central still rejects an unsigned upload, so a release can't slip out unsigned.
            if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                signAllPublications()
            }

            pom {
                inceptionYear = "2026"
                url = "https://github.com/vitorpamplona/kiko-kmp/"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://github.com/vitorpamplona/kiko-kmp/blob/main/LICENSE"
                    }
                }
                developers {
                    developer {
                        id = "vitorpamplona"
                        name = "Vitor Pamplona"
                        url = "http://vitorpamplona.com"
                        email = "vitor@vitorpamplona.com"
                    }
                }
                scm {
                    url = "https://github.com/vitorpamplona/kiko-kmp"
                    connection = "https://github.com/vitorpamplona/kiko-kmp.git"
                }
            }
        }
    }

    afterEvaluate {
        // Kotlin Multiplatform registers no plain `test` task, so `./gradlew test` would skip
        // :kiko silently. Alias it onto jvmTest.
        if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") &&
            tasks.findByName("test") == null &&
            tasks.findByName("jvmTest") != null
        ) {
            tasks.register("test") {
                group = "verification"
                description = "Runs the JVM unit tests for this Kotlin Multiplatform module."
                dependsOn("jvmTest")
            }
        }
    }
}

tasks.register<Copy>("installGitHook") {
    from("git-hooks/pre-commit")
    from("git-hooks/pre-push")
    into(".git/hooks")
    filePermissions { unix("rwxrwxrwx") }
}

// The checks, listed once: the pre-push hook, CI (build.yml) and the release workflow
// (create-release.yml) all run these tasks instead of keeping their own lists.
val published = listOf(":kiko", ":kiko-player", ":kiko-compose")

tasks.register("checkLinux") {
    group = "verification"
    description = "Formatting, the public APIs, and every test that runs on a Linux machine except the web's."
    dependsOn(allprojects.map { if (it == rootProject) ":spotlessCheck" else "${it.path}:spotlessCheck" })
    dependsOn(published.map { "$it:checkKotlinAbi" })
    dependsOn(":kiko:jvmTest", ":kiko-player:jvmTest", ":kiko-compose:jvmTest", ":demo:jvmTest")
    dependsOn(":kiko:testAndroidHostTest", ":kiko-player:testAndroidHostTest")
    dependsOn(":kiko:linuxX64Test", ":kiko-player:linuxX64Test")
    // Apple code compiles on Linux as metadata, so a mistake fails here in minutes instead of in
    // the macOS job.
    dependsOn(published.map { "$it:compileCommonMainKotlinMetadata" })
    dependsOn(":kiko-compose:compileSkikoMainKotlinMetadata")
}

tasks.register("checkWeb") {
    group = "verification"
    description = "The common tests on Kotlin/JS and Kotlin/Wasm, in Node.js and headless Chrome."
    dependsOn(":kiko:jsTest", ":kiko:wasmJsTest", ":kiko-player:jsTest", ":kiko-player:wasmJsTest")
}

tasks.register("checkWebNode") {
    group = "verification"
    description = "The common tests on Kotlin/JS and Kotlin/Wasm, in Node.js only (no browser needed)."
    dependsOn(":kiko:jsNodeTest", ":kiko:wasmJsNodeTest", ":kiko-player:jsNodeTest", ":kiko-player:wasmJsNodeTest")
}

tasks.register("checkApple") {
    group = "verification"
    description = "The iOS and macOS tests, and the iOS builds of kiko-compose and the demo. macOS only."
    dependsOn(":kiko:iosSimulatorArm64Test", ":kiko:macosArm64Test", ":kiko:compileTestKotlinIosArm64")
    dependsOn(":kiko-player:iosSimulatorArm64Test", ":kiko-player:macosArm64Test")
    dependsOn(":kiko-compose:compileKotlinIosArm64", ":kiko-compose:compileKotlinIosSimulatorArm64")
    dependsOn(":demo:linkDebugFrameworkIosSimulatorArm64")
}
