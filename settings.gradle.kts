pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kiko-kmp"
include(":kiko")
include(":kiko-player")
include(":kiko-compose")
include(":demo")
