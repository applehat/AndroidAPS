plugins {
    alias(libs.plugins.android.library)
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

android {
    namespace = "app.aaps.pump.flex.protocol"
}

// Flex command/response model and the transport contract it is sent over. No Android and no
// driver internals, so both the real driver and the emulator can sit on it without depending on each other.
// Plaintext IDD opcodes/layouts recovered from MiniMed Instinct 1.3.1 (blutter). Live pumps wrap
// these PDUs inside TLS 1.3 over BLE + MTS; that channel is NOT implemented here.
dependencies {
    implementation(project(":core:interfaces"))

    testImplementation(project(":shared:tests"))
}
