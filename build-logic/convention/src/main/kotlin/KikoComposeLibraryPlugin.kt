import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `kiko.compose-library`: a published kiko module with Compose UI, on every target Compose
 * Multiplatform draws on: the JVM, Android, iOS and browsers. `skikoMain` holds what the targets
 * drawing with Skia (all but Android) share.
 */
class KikoComposeLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")
            configureKikoLibrary(withHostTest = false)

            extensions.configure<KotlinMultiplatformExtension> {
                js {
                    browser()
                }

                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser()
                }

                applyDefaultHierarchyTemplate()

                sourceSets.apply {
                    val skikoMain = create("skikoMain") { dependsOn(getByName("commonMain")) }
                    getByName("jvmMain").dependsOn(skikoMain)
                    getByName("iosMain").dependsOn(skikoMain)
                    getByName("webMain").dependsOn(skikoMain)
                }
            }

            // The Compose plugin fails `check` when a browser target's tests could load Skiko
            // but no executable bundles them (CMP-4906). A library has no executable and these
            // modules run no browser tests, so the check guards nothing here.
            tasks.matching { it.name.startsWith("checkComposeUiTestConfigurationFor") }.configureEach {
                enabled = false
            }
        }
}
