package app.aaps.pump.twiist.protocol

/**
 * Commands the driver can ask of a twiist pump - the *semantic* model, independent of wire format.
 *
 * Amounts are in pump units (cU) as AAPS hands them to the driver; any scaling to pump-native
 * resolution (pulses, 0.0x U steps) belongs to the codec and MUST be validated against the real pump.
 */
sealed interface TwiistCommand {

    data object ReadStatus : TwiistCommand
    data class Bolus(val units: Double) : TwiistCommand
    data object CancelBolus : TwiistCommand
    data class SetTempBasal(val unitsPerHour: Double, val durationMinutes: Int) : TwiistCommand
    data object CancelTempBasal : TwiistCommand

    /** 48 half-hour rates (U/h), starting at local midnight. */
    data class SetBasalProfile(val halfHourRates: List<Double>) : TwiistCommand {

        init {
            require(halfHourRates.size == 48) { "basal profile must have 48 half-hour entries, got ${halfHourRates.size}" }
        }
    }

    data object Suspend : TwiistCommand
    data object Resume : TwiistCommand
    data class SetTime(val epochMillis: Long, val utcOffsetMinutes: Int) : TwiistCommand
}

/** What a pump replies. */
sealed interface TwiistResponse {

    data class Status(
        val reservoirUnits: Double,
        val batteryPercent: Int,
        val suspended: Boolean,
        val bolusInProgress: Boolean,
        /** Units delivered by the current/last bolus as reported by the pump. */
        val lastBolusDelivered: Double,
        val tempBasalRate: Double?,
        val tempBasalRemainingMinutes: Int,
        val activeAlarm: TwiistAlarm?
    ) : TwiistResponse

    data class Ack(val command: String) : TwiistResponse
    data class Nack(val command: String, val reason: NackReason) : TwiistResponse

    /** Bytes the codec could not interpret. The driver MUST treat this as failure. */
    class Unknown(val raw: ByteArray) : TwiistResponse
}

enum class NackReason { SUSPENDED, BUSY, OUT_OF_RANGE, RESERVOIR_EMPTY, ALARM_ACTIVE, NOT_SUPPORTED, UNKNOWN }

enum class TwiistAlarm { OCCLUSION, RESERVOIR_EMPTY, LOW_RESERVOIR, LOW_BATTERY, PUMP_ERROR }
