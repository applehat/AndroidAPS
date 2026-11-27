package app.aaps.pump.flex.emulator

import app.aaps.pump.flex.protocol.FlexAlarm

/** Mutable state of the emulated Flex pump. Tests poke at it directly to set up scenarios. */
class FlexPumpState(
    var serialNumber: String = "FLEXEMU1",
    var macAddress: ByteArray = byteArrayOf(0x02, 0x7F, 0x1E, 0x00, 0x00, 0x01),
    var reservoirUnits: Double = 200.0,
    var batteryPercent: Int = 100,
    var suspended: Boolean = false,
    var bolusInProgress: Boolean = false,
    var lastBolusDelivered: Double = 0.0,
    var tempBasalRate: Double? = null,
    var tempBasalRemainingMinutes: Int = 0,
    var basalProfile: List<Double> = List(48) { 0.0 },
    var activeAlarm: FlexAlarm? = null,

    // ---- fault injection -------------------------------------------------------------------
    /** Deliver only this fraction of each bolus (simulates an interrupted/occluded bolus). */
    var bolusDeliveryFraction: Double = 1.0,
    /** Drop every reply (simulates a BLE link lost after the command was sent). */
    var silent: Boolean = false,
    /** Reply with garbage instead of a valid frame. */
    var garbleReplies: Boolean = false
)
