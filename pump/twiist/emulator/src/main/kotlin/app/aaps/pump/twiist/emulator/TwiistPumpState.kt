package app.aaps.pump.twiist.emulator

import app.aaps.pump.twiist.protocol.TwiistAlarm

/** Mutable state of the emulated pump. Tests poke at it directly to set up scenarios. */
class TwiistPumpState(
    var serialNumber: String = "EMU00001",
    var macAddress: ByteArray = byteArrayOf(0x02, 0x7E, 0x57, 0x00, 0x00, 0x01),
    var reservoirUnits: Double = 200.0,
    var batteryPercent: Int = 100,
    var suspended: Boolean = false,
    var bolusInProgress: Boolean = false,
    var lastBolusDelivered: Double = 0.0,
    var tempBasalRate: Double? = null,
    var tempBasalRemainingMinutes: Int = 0,
    var basalProfile: List<Double> = List(48) { 0.0 },
    var activeAlarm: TwiistAlarm? = null,
    var clockEpochMillis: Long = 0L,
    var utcOffsetMinutes: Int = 0,

    // ---- fault injection -------------------------------------------------------------------
    /** Deliver only this fraction of each bolus (simulates an interrupted/occluded bolus). */
    var bolusDeliveryFraction: Double = 1.0,
    /** Drop every reply (simulates a BLE link lost after the command was sent). */
    var silent: Boolean = false,
    /** Reply with garbage instead of a valid frame. */
    var garbleReplies: Boolean = false
)
