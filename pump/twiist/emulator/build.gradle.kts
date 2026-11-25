plugins {
    alias(libs.plugins.android.library)
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

android {
    namespace = "app.aaps.pump.twiist.emulator"
}

// In-tree twiist emulator: lets the whole driver run, and be unit tested, with no pump hardware.
dependencies {
    implementation(project(":core:interfaces"))
    implementation(project(":pump:twiist:protocol"))

    testImplementation(project(":shared:tests"))
}
