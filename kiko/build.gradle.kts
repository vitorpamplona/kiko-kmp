// The runtime: the Kotlin stdlib alone, on every target (build-logic's kiko.library).
plugins {
    id("kiko.library")
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
