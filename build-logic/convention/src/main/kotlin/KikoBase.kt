import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * What every published kiko module shares: an explicit, checked public API; JVM 17 (a library
 * can't need a newer JVM than the apps that use it); Android at the catalog's SDK levels, with
 * the namespace taken from the module's name (kiko-player is com.vitorpamplona.kiko.player); and
 * Maven Central publishing, configured in the root build.
 */
internal fun Project.configureKikoLibrary(withHostTest: Boolean) {
    pluginManager.apply("com.vanniktech.maven.publish")

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    fun sdk(name: String) =
        libs
            .findVersion(name)
            .get()
            .requiredVersion
            .toInt()

    extensions.configure<KotlinMultiplatformExtension> {
        explicitApi()

        @OptIn(ExperimentalAbiValidation::class)
        abiValidation()

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
            namespace = "com.vitorpamplona." + project.name.replace('-', '.')
            compileSdk = sdk("android-compileSdk")
            minSdk = sdk("android-minSdk")
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            if (withHostTest) withHostTest {}
        }

        iosArm64()
        iosSimulatorArm64()
    }
}
