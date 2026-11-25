package app.aaps.pump.twiist.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * INVENTED framing for emulator-only testing. NOT the twiist wire protocol.
 *
 * Frame: [MAGIC][opcode][payload...]. Doubles are sent as milli-units (Int, little endian).
 * Anything that does not start with [MAGIC] decodes to [TwiistResponse.Unknown] so a real pump
 * (which would never send MAGIC-prefixed frames) can't be mistaken for success.
 */
object EmulatorOnlyCodec : TwiistCodec {

    const val MAGIC: Byte = 0x7E

    private object Op {

        const val READ_STATUS: Byte = 0x01
        const val BOLUS: Byte = 0x02
        const val CANCEL_BOLUS: Byte = 0x03
        const val SET_TBR: Byte = 0x04
        const val CANCEL_TBR: Byte = 0x05
        const val SET_PROFILE: Byte = 0x06
        const val SUSPEND: Byte = 0x07
        const val RESUME: Byte = 0x08
        const val SET_TIME: Byte = 0x09
        const val R_STATUS: Byte = 0x41
        const val R_ACK: Byte = 0x42
        const val R_NACK: Byte = 0x43
    }

    private fun buf(size: Int) = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    private fun mU(u: Double): Int = Math.round(u * 1000.0).toInt()
    private fun u(mU: Int): Double = mU / 1000.0

    override fun encode(command: TwiistCommand): ByteArray = when (command) {
        TwiistCommand.ReadStatus         -> byteArrayOf(MAGIC, Op.READ_STATUS)
        is TwiistCommand.Bolus           -> buf(6).put(MAGIC).put(Op.BOLUS).putInt(mU(command.units)).array()
        TwiistCommand.CancelBolus        -> byteArrayOf(MAGIC, Op.CANCEL_BOLUS)
        is TwiistCommand.SetTempBasal    -> buf(10).put(MAGIC).put(Op.SET_TBR).putInt(mU(command.unitsPerHour)).putInt(command.durationMinutes).array()
        TwiistCommand.CancelTempBasal    -> byteArrayOf(MAGIC, Op.CANCEL_TBR)
        is TwiistCommand.SetBasalProfile -> buf(2 + 48 * 4).put(MAGIC).put(Op.SET_PROFILE).also { b -> command.halfHourRates.forEach { b.putInt(mU(it)) } }.array()
        TwiistCommand.Suspend            -> byteArrayOf(MAGIC, Op.SUSPEND)
        TwiistCommand.Resume             -> byteArrayOf(MAGIC, Op.RESUME)
        is TwiistCommand.SetTime         -> buf(14).put(MAGIC).put(Op.SET_TIME).putLong(command.epochMillis).putInt(command.utcOffsetMinutes).array()
    }

    override fun decodeCommand(data: ByteArray): TwiistCommand? {
        if (data.size < 2 || data[0] != MAGIC) return null
        val b = ByteBuffer.wrap(data, 2, data.size - 2).order(ByteOrder.LITTLE_ENDIAN)
        return try {
            when (data[1]) {
                Op.READ_STATUS  -> TwiistCommand.ReadStatus
                Op.BOLUS        -> TwiistCommand.Bolus(u(b.int))
                Op.CANCEL_BOLUS -> TwiistCommand.CancelBolus
                Op.SET_TBR      -> TwiistCommand.SetTempBasal(u(b.int), b.int)
                Op.CANCEL_TBR   -> TwiistCommand.CancelTempBasal
                Op.SET_PROFILE  -> TwiistCommand.SetBasalProfile(List(48) { u(b.int) })
                Op.SUSPEND      -> TwiistCommand.Suspend
                Op.RESUME       -> TwiistCommand.Resume
                Op.SET_TIME     -> TwiistCommand.SetTime(b.long, b.int)
                else            -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun encodeResponse(response: TwiistResponse): ByteArray = when (response) {
        is TwiistResponse.Status  -> buf(2 + 4 + 4 + 1 + 1 + 4 + 4 + 4 + 1).put(MAGIC).put(Op.R_STATUS)
            .putInt(mU(response.reservoirUnits)).putInt(response.batteryPercent)
            .put(if (response.suspended) 1 else 0).put(if (response.bolusInProgress) 1 else 0)
            .putInt(mU(response.lastBolusDelivered))
            .putInt(response.tempBasalRate?.let(::mU) ?: -1).putInt(response.tempBasalRemainingMinutes)
            .put((response.activeAlarm?.ordinal ?: -1).toByte()).array()

        is TwiistResponse.Ack     -> byteArrayOf(MAGIC, Op.R_ACK) + response.command.encodeToByteArray()
        is TwiistResponse.Nack    -> byteArrayOf(MAGIC, Op.R_NACK, response.reason.ordinal.toByte()) + response.command.encodeToByteArray()
        is TwiistResponse.Unknown -> response.raw
    }

    override fun decode(data: ByteArray): TwiistResponse {
        if (data.size < 2 || data[0] != MAGIC) return TwiistResponse.Unknown(data)
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
                    val alarm = b.get().toInt().let { if (it < 0) null else TwiistAlarm.entries.getOrNull(it) }
                    TwiistResponse.Status(reservoir, battery, suspended, bolusing, lastBolus, tbr, tbrRemaining, alarm)
                }

                Op.R_ACK    -> TwiistResponse.Ack(data.copyOfRange(2, data.size).decodeToString())
                Op.R_NACK   -> TwiistResponse.Nack(
                    data.copyOfRange(3, data.size).decodeToString(),
                    NackReason.entries.getOrElse(data[2].toInt()) { NackReason.UNKNOWN }
                )

                else        -> TwiistResponse.Unknown(data)
            }
        } catch (_: Exception) {
            TwiistResponse.Unknown(data)
        }
    }
}
