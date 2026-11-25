plugins {
    alias(libs.plugins.android.library)
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

android {
    namespace = "app.aaps.pump.twiist.protocol"
}

// The twiist command/response model and the transport contract it is sent over. No Android and no
// driver internals, so both the real driver and the emulator can sit on it without depending on each other.
// The REAL wire encoding is unknown until BLE capture analysis is done - see TwiistCodec.
dependencies {
    implementation(project(":core:interfaces"))

    testImplementation(project(":shared:tests"))
}
