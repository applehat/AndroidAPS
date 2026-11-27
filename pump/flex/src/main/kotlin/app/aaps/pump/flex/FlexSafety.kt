package app.aaps.pump.flex

/**
 * Single switch that keeps the skeleton driver inert.
 *
 * SAFETY: flipping this requires, at minimum: a validated TLS-over-BLE transport + IDD codec
 * checked against real captures, delivery confirmation read back from the pump (never assumed
 * from an ACK), emulator tests for every fault in FlexPumpEmulator, and saline-only bench
 * testing on real hardware. Tests assert it is false.
 */
object FlexSafety {

    const val DELIVERY_ENABLED: Boolean = false
}
