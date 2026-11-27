package app.aaps.pump.flex.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * INVENTED framing for emulator-only testing. NOT the Flex / Instinct wire protocol.
 *
 * Frame: [MAGIC][opcode][payload...]. Doubles are sent as milli-units (Int, little endian).
 * Anything that does not start with [MAGIC] decodes to [FlexResponse.Unknown] so a real pump
 * (which would never send MAGIC-prefixed frames) can't be mistaken for success.
 *
 * Real IDD plaintext builders live in [IddEncoders]; they are deliberately NOT bound to the
 * emulator or to any live transport in this skeleton.
 */
object EmulatorOnlyCodec : FlexCodec {

    const val MAGIC: Byte = 0x7F.toByte()

    private object Op {
        const val READ_STATUS: Byte = 0x01
        const val BOLUS: Byte = 0x02
        const val CANCEL_BOLUS: Byte = 0x03
        const val SET_TBR: Byte = 0x04
        const val CANCEL_TBR: Byte = 0x05
        const val SET_PROFILE: Byte = 0x06
        const val SUSPEND: Byte = 0x07
        const val RESUME: Byte = 0x08
        const val FILL_CANNULA: Byte = 0x09
        const val FILL_TUBING: Byte = 0x0A
        const val SET_TEMP_TARGET: Byte = 0x0B
        const val CANCEL_TEMP_TARGET: Byte = 0x0C
        const val R_STATUS: Byte = 0x41
        const val R_ACK: Byte = 0x42
        const val R_NACK: Byte = 0x43
    }

    private fun buf(size: Int) = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    private fun mU(u: Double): Int = Math.round(u * 1000.0).toInt()
    private fun u(mU: Int): Double = mU / 1000.0

    override fun encode(command: FlexCommand): ByteArray = when (command) {
        FlexCommand.ReadStatus      -> byteArrayOf(MAGIC, Op.READ_STATUS)
        is FlexCommand.Bolus        -> buf(6).put(MAGIC).put(Op.BOLUS).putInt(mU(command.unitsFast)).array()
        FlexCommand.CancelBolus     -> byteArrayOf(MAGIC, Op.CANCEL_BOLUS)
        is FlexCommand.SetTempBasal -> buf(10).put(MAGIC).put(Op.SET_TBR).putInt(mU(command.unitsPerHour)).putInt(command.durationMinutes).array()
        FlexCommand.CancelTempBasal -> byteArrayOf(MAGIC, Op.CANCEL_TBR)
        is FlexCommand.SetBasalProfile -> buf(2 + 48 * 4).put(MAGIC).put(Op.SET_PROFILE).also { b -> command.halfHourRates.forEach { b.putInt(mU(it)) } }.array()
        FlexCommand.Suspend         -> byteArrayOf(MAGIC, Op.SUSPEND)
        FlexCommand.Resume          -> byteArrayOf(MAGIC, Op.RESUME)
        is FlexCommand.FillCannula  -> buf(6).put(MAGIC).put(Op.FILL_CANNULA).putInt(mU(command.units)).array()
        is FlexCommand.FillTubing   -> buf(6).put(MAGIC).put(Op.FILL_TUBING).putInt(mU(command.units)).array()
        is FlexCommand.SetTempTarget -> buf(7).put(MAGIC).put(Op.SET_TEMP_TARGET).putInt(command.target).put(command.flag.toByte()).array()
        FlexCommand.CancelTempTarget -> byteArrayOf(MAGIC, Op.CANCEL_TEMP_TARGET)
    }

    override fun decodeCommand(data: ByteArray): FlexCommand? {
        if (data.size < 2 || data[0] != MAGIC) return null
        val b = ByteBuffer.wrap(data, 2, data.size - 2).order(ByteOrder.LITTLE_ENDIAN)
        return try {
            when (data[1]) {
                Op.READ_STATUS        -> FlexCommand.ReadStatus
                Op.BOLUS              -> FlexCommand.Bolus(u(b.int))
                Op.CANCEL_BOLUS       -> FlexCommand.CancelBolus
                Op.SET_TBR            -> FlexCommand.SetTempBasal(u(b.int), b.int)
                Op.CANCEL_TBR         -> FlexCommand.CancelTempBasal
                Op.SET_PROFILE        -> FlexCommand.SetBasalProfile(List(48) { u(b.int) })
                Op.SUSPEND            -> FlexCommand.Suspend
                Op.RESUME             -> FlexCommand.Resume
                Op.FILL_CANNULA       -> FlexCommand.FillCannula(u(b.int))
                Op.FILL_TUBING        -> FlexCommand.FillTubing(u(b.int))
                Op.SET_TEMP_TARGET    -> FlexCommand.SetTempTarget(b.int, b.get().toInt())
                Op.CANCEL_TEMP_TARGET -> FlexCommand.CancelTempTarget
                else                  -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun encodeResponse(response: FlexResponse): ByteArray = when (response) {
        is FlexResponse.Status  -> buf(2 + 4 + 4 + 1 + 1 + 4 + 4 + 4 + 1).put(MAGIC).put(Op.R_STATUS)
            .putInt(mU(response.reservoirUnits)).putInt(response.batteryPercent)
            .put(if (response.suspended) 1 else 0).put(if (response.bolusInProgress) 1 else 0)
            .putInt(mU(response.lastBolusDelivered))
            .putInt(response.tempBasalRate?.let(::mU) ?: -1).putInt(response.tempBasalRemainingMinutes)
            .put((response.activeAlarm?.ordinal ?: -1).toByte()).array()

        is FlexResponse.Ack     -> byteArrayOf(MAGIC, Op.R_ACK) + response.command.encodeToByteArray()
        is FlexResponse.Nack    -> byteArrayOf(MAGIC, Op.R_NACK, response.reason.ordinal.toByte()) + response.command.encodeToByteArray()
        is FlexResponse.Unknown -> response.raw
    }

    override fun decode(data: ByteArray): FlexResponse {
        if (data.size < 2 || data[0] != MAGIC) return FlexResponse.Unknown(data)
        return try {
            val b = ByteBuffer.wrap(data, 2, data.size - 2).order(ByteOrder.LITTLE_ENDIAN)
            when (data[1]) {
                Op.R_STATUS -> {
                    val reservoir = u(b.int)
                    val battery = b.int
                    val suspended = b.get() == 1.toByte()
                    val bolusing = b.get() == 1.toByte()
                    val lastBolus = u(b.int)
                    val tbr = b.int.let { if (it < 0) null else u(it) }
                    val tbrRemaining = b.int
                    val alarm = b.get().toInt().let { if (it < 0) null else FlexAlarm.entries.getOrNull(it) }
                    FlexResponse.Status(reservoir, battery, suspended, bolusing, lastBolus, tbr, tbrRemaining, alarm)
                }

                Op.R_ACK    -> FlexResponse.Ack(data.copyOfRange(2, data.size).decodeToString())
                Op.R_NACK   -> FlexResponse.Nack(
                    data.copyOfRange(3, data.size).decodeToString(),
                    NackReason.entries.getOrElse(data[2].toInt()) { NackReason.UNKNOWN }
                )

                else        -> FlexResponse.Unknown(data)
            }
        } catch (_: Exception) {
            FlexResponse.Unknown(data)
        }
    }
}
