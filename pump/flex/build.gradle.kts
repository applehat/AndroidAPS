plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.metro)
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

android {
    namespace = "app.aaps.pump.flex"

    buildFeatures {
        compose = true
    }
}

// Medtronic Flex / Instinct driver. SKELETON: fails closed on every delivery path until the real
// TLS-over-BLE transport and hardware validation exist. Protocol opcodes recovered from the
// official Instinct APK (see reversing-flex / FINDINGS-opcodes.md) live in :pump:flex:protocol.
dependencies {
    api(project(":pump:flex:protocol"))
    implementation(project(":pump:flex:emulator"))
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:keys"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(project(":shared:tests"))
}
