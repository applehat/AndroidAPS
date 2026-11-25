package app.aaps.pump.twiist

/**
 * Single switch that keeps the skeleton driver inert.
 *
 * SAFETY: flipping this requires, at minimum: a validated codec built from real BLE captures,
 * delivery confirmation read back from the pump (never assumed from an ACK), emulator tests for every
 * fault in TwiistPumpEmulator, and saline-only bench testing on real hardware. Tests assert it is false.
 */
object TwiistSafety {

    const val DELIVERY_ENABLED: Boolean = false
}
