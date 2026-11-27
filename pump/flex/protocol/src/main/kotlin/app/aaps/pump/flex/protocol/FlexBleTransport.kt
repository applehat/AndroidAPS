package app.aaps.pump.flex.protocol

import app.aaps.core.interfaces.pump.ble.BleTransport

/**
 * Flex-specific extension of the shared fleet [BleTransport].
 *
 * Implemented by the in-tree emulator (`:pump:flex:emulator`) and, once the TLS-over-BLE channel
 * is understood on hardware, by a real Android BLE implementation in the driver. Until then the
 * driver binds a radio-less transport that refuses every connection (fail closed).
 */
interface FlexBleTransport : BleTransport {

    /** MAC address of the paired pump, null until paired. */
    var targetAddress: String?

    /** True if this transport can reach a pump at all (false for the not-yet-implemented real radio). */
    val isOperational: Boolean
}
