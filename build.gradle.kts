// Versions are pinned to what the local Gradle cache already holds, so the build
// resolves offline-fast and cannot drift under us mid-session.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// One command for CI and the pre-push hook, so what blocks a push is exactly what
// blocks a merge.
tasks.register("buildSmoke") {
    group = "verification"
    description = "Build debug, run unit tests, and lint."
    dependsOn(":app:assembleDebug", ":app:testDebugUnitTest", ":app:lintDebug")
}
