import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `kiko.library`: a published kiko module with no UI, on every target the Kotlin stdlib reaches:
 * the JVM, Android, iOS, macOS, Linux, and the web (browsers and Node.js). Its common tests use
 * kotlin-test and also run on the Android host.
 */
class KikoLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            configureKikoLibrary(withHostTest = true)

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val kotlinTest = libs.findLibrary("kotlin-test").get()

            extensions.configure<KotlinMultiplatformExtension> {
                macosArm64()
                linuxX64()

                js {
                    browser()
                    nodejs()
                }

                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser()
                    nodejs()
                }

                applyDefaultHierarchyTemplate()

                sourceSets.getByName("commonTest").dependencies { implementation(kotlinTest) }
                sourceSets.getByName("androidHostTest").dependencies { implementation(kotlinTest) }
            }
        }
}
