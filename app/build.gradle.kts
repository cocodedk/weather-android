import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // No kotlin.android plugin: AGP 9 provides Kotlin support itself and rejects it.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The release workflow computes the version from the latest git tag and passes it
// in; a local build with no env var just gets 0.0.0 and never pretends otherwise.
val appVersionName: String = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.0.0"
val semver = appVersionName.split(".")
val vMajor = semver.getOrNull(0)?.toIntOrNull() ?: 0
val vMinor = semver.getOrNull(1)?.toIntOrNull() ?: 0
val vPatch = semver.getOrNull(2)?.toIntOrNull() ?: 0
// Ceiling: 999 minor and 999 patch releases per level before this overflows.
val appVersionCode: Int = vMajor * 1_000_000 + vMinor * 1_000 + vPatch + 1

// Signing material only ever arrives through the environment. A missing keystore
// is not an error — it just means this is a local build, which stays unsigned.
val keystorePath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keyAliasEnv = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
val keyPasswordEnv = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val keystoreFile = keystorePath?.let { rootProject.file(it).absoluteFile }?.takeIf { it.isFile }
val hasSigningConfig = keystoreFile != null && keystorePassword != null &&
    keyAliasEnv != null && keyPasswordEnv != null

android {
    namespace = "dk.cocode.weather"
    // 37 is a floor, not a preference: androidx.core 1.19.0 and lifecycle 2.11.0 both
    // declare minCompileSdk=37 in their AAR metadata and the build will not resolve
    // below it. targetSdk deliberately stays at 35 — compiling against a newer API is
    // safe, opting into its runtime behaviour changes is a separate decision.
    compileSdk = 37

    defaultConfig {
        applicationId = "dk.cocode.weather"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    lint {
        // The build must fail on a real lint error, but a warning should not block
        // a merge — CI runs this on every PR.
        abortOnError = true
        warningsAsErrors = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// AGP 9 removed android.kotlinOptions; the Kotlin plugin's own block replaces it.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// No JSON, HTTP or location library here on purpose: org.json, HttpURLConnection
// and LocationManager all ship with Android, which keeps the dependency surface
// (and the APK) small enough that the whole build resolves from the local cache.
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
