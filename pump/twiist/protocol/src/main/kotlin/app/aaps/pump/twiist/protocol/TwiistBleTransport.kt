package app.aaps.pump.twiist.protocol

import app.aaps.core.interfaces.pump.ble.BleTransport

/**
 * twiist-specific extension of the shared fleet [BleTransport].
 *
 * Implemented by the in-tree emulator (`:pump:twiist:emulator`) and, once the protocol is understood,
 * by a real Android BLE implementation in the driver. Until then the driver binds a radio-less
 * transport that refuses every connection (fail closed).
 */
interface TwiistBleTransport : BleTransport {

    /** MAC address of the paired pump, null until paired. */
    var targetAddress: String?

    /** True if this transport can reach a pump at all (false for the not-yet-implemented real radio). */
    val isOperational: Boolean
}
