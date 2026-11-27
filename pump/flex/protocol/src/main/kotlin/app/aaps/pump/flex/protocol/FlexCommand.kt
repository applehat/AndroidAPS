package app.aaps.pump.flex.protocol

/**
 * Commands the driver can ask of a Flex / Instinct pump — the *semantic* model, independent of
 * the TLS/MTS transport that will eventually carry the plaintext IDD PDUs.
 *
 * Amounts are in insulin units as AAPS hands them to the driver; scaling to the pump-native
 * packed uint32 representation belongs to the codec ([PlaintextIddCodec]) and MUST be validated
 * against the real pump before any insulin use.
 */
sealed interface FlexCommand {

    data object ReadStatus : FlexCommand

    /**
     * Manual bolus. [bolusType] wire codes: normal=0x00, square=0x33, dualNormal=0x3C, dualSquare=0x55.
     * Only normal is used by the AAPS skeleton today.
     */
    data class Bolus(
        val unitsFast: Double,
        val unitsSquare: Double = 0.0,
        val durationMinutes: Int = 0,
        val bolusType: Int = BolusDeliveryType.NORMAL.wire
    ) : FlexCommand

    data object CancelBolus : FlexCommand

    /** Temp basal. [tempBasalType]: rate=0x00, percent=0x01. */
    data class SetTempBasal(
        val unitsPerHour: Double,
        val durationMinutes: Int,
        val tempBasalType: Int = TempBasalType.RATE.wire
    ) : FlexCommand

    data object CancelTempBasal : FlexCommand

    /** Basal pattern. Segment count / schedule shape still TODO against hardware. */
    data class SetBasalProfile(val halfHourRates: List<Double>) : FlexCommand {
        init {
            require(halfHourRates.size == 48) { "basal profile must have 48 half-hour entries, got ${halfHourRates.size}" }
        }
    }

    data object Suspend : FlexCommand
    data object Resume : FlexCommand

    data class FillCannula(val units: Double) : FlexCommand
    data class FillTubing(val units: Double) : FlexCommand

    data class SetTempTarget(val target: Int, val flag: Int = 0) : FlexCommand
    data object CancelTempTarget : FlexCommand
}

enum class BolusDeliveryType(val wire: Int) {
    NORMAL(0x00),
    SQUARE(0x33),
    DUAL_NORMAL(0x3C),
    DUAL_SQUARE(0x55)
}

enum class TempBasalType(val wire: Int) {
    RATE(0x00),
    PERCENT(0x01)
}

/** What a pump (or emulator) replies. */
sealed interface FlexResponse {

    data class Status(
        val reservoirUnits: Double,
        val batteryPercent: Int,
        val suspended: Boolean,
        val bolusInProgress: Boolean,
        val lastBolusDelivered: Double,
        val tempBasalRate: Double?,
        val tempBasalRemainingMinutes: Int,
        val activeAlarm: FlexAlarm?
    ) : FlexResponse

    data class Ack(val command: String) : FlexResponse
    data class Nack(val command: String, val reason: NackReason) : FlexResponse

    /** Bytes the codec could not interpret. The driver MUST treat this as failure. */
    class Unknown(val raw: ByteArray) : FlexResponse
}

enum class NackReason { SUSPENDED, BUSY, OUT_OF_RANGE, RESERVOIR_EMPTY, ALARM_ACTIVE, NOT_SUPPORTED, UNKNOWN }

enum class FlexAlarm { OCCLUSION, RESERVOIR_EMPTY, LOW_RESERVOIR, LOW_BATTERY, PUMP_ERROR }
