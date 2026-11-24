plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "de.gymminimal.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "de.gymminimal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "1.5.0"
    }
    // Signing credentials are injected by GitHub Actions from encrypted Secrets.
    // Debug builds and pull-request validation require no signing Secrets.
    val releaseKeyPath = providers.environmentVariable("REPAVIO_KEYSTORE_PATH").orNull
    if (!releaseKeyPath.isNullOrBlank()) {
        signingConfigs {
            create("repavioRelease") {
                storeFile = file(releaseKeyPath)
                storePassword = providers.environmentVariable("REPAVIO_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("REPAVIO_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("REPAVIO_KEY_PASSWORD").orNull
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("repavioRelease")
                isMinifyEnabled = false
            }
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime:2.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
