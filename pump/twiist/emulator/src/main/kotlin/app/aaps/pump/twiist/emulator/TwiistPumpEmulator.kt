package app.aaps.pump.twiist.emulator

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.twiist.protocol.EmulatorOnlyCodec
import app.aaps.pump.twiist.protocol.NackReason
import app.aaps.pump.twiist.protocol.TwiistAlarm
import app.aaps.pump.twiist.protocol.TwiistCodec
import app.aaps.pump.twiist.protocol.TwiistCommand
import app.aaps.pump.twiist.protocol.TwiistResponse

/**
 * Behavioural model of a twiist pump, speaking [EmulatorOnlyCodec].
 *
 * Its job is to let the driver's safety logic be exercised (rejections, partial delivery, lost
 * replies, alarms) without hardware. Its behaviour is a GUESS until checked against the real pump;
 * update it as BLE captures reveal how the pump actually responds.
 */
class TwiistPumpEmulator(
    val state: TwiistPumpState = TwiistPumpState(),
    private val aapsLogger: AAPSLogger? = null,
    private val codec: TwiistCodec = EmulatorOnlyCodec
) {

    /** Every command the emulator understood, in order. */
    val received: MutableList<TwiistCommand> = mutableListOf()

    fun handle(frame: ByteArray): List<ByteArray> {
        val command = codec.decodeCommand(frame)
        aapsLogger?.debug(LTag.PUMPEMULATOR, "twiist emulator <- $command")
        if (command != null) received += command
        val response = if (command == null) TwiistResponse.Nack("?", NackReason.NOT_SUPPORTED) else apply(command)
        if (state.silent) return emptyList()
        if (state.garbleReplies) return listOf(byteArrayOf(0x00, 0x13, 0x37))
        return listOf(codec.encodeResponse(response))
    }

    private fun nack(cmd: TwiistCommand, reason: NackReason) = TwiistResponse.Nack(cmd::class.simpleName ?: "?", reason)
    private fun ack(cmd: TwiistCommand) = TwiistResponse.Ack(cmd::class.simpleName ?: "?")

    private fun apply(cmd: TwiistCommand): TwiistResponse = with(state) {
        when (cmd) {
            TwiistCommand.ReadStatus         -> status()

            is TwiistCommand.Bolus           -> when {
                activeAlarm == TwiistAlarm.OCCLUSION || activeAlarm == TwiistAlarm.PUMP_ERROR -> nack(cmd, NackReason.ALARM_ACTIVE)
                suspended                                                                    -> nack(cmd, NackReason.SUSPENDED)
                bolusInProgress                                                              -> nack(cmd, NackReason.BUSY)
                cmd.units <= 0.0 || cmd.units > MAX_BOLUS                                    -> nack(cmd, NackReason.OUT_OF_RANGE)
                cmd.units > reservoirUnits                                                   -> nack(cmd, NackReason.RESERVOIR_EMPTY)
                else                                                                         -> {
                    val delivered = cmd.units * bolusDeliveryFraction
                    reservoirUnits -= delivered
                    lastBolusDelivered = delivered
                    ack(cmd)
                }
            }

            TwiistCommand.CancelBolus        -> {
                bolusInProgress = false; ack(cmd)
            }

            is TwiistCommand.SetTempBasal    -> when {
                suspended                                                                   -> nack(cmd, NackReason.SUSPENDED)
                cmd.unitsPerHour < 0.0 || cmd.unitsPerHour > MAX_BASAL || cmd.durationMinutes !in 30..(12 * 60) -> nack(cmd, NackReason.OUT_OF_RANGE)
                else                                                                        -> {
                    tempBasalRate = cmd.unitsPerHour; tempBasalRemainingMinutes = cmd.durationMinutes; ack(cmd)
                }
            }

            TwiistCommand.CancelTempBasal    -> {
                tempBasalRate = null; tempBasalRemainingMinutes = 0; ack(cmd)
            }

            is TwiistCommand.SetBasalProfile -> if (cmd.halfHourRates.any { it < 0.0 || it > MAX_BASAL }) nack(cmd, NackReason.OUT_OF_RANGE)
            else {
                basalProfile = cmd.halfHourRates; ack(cmd)
            }

            TwiistCommand.Suspend            -> {
                suspended = true; tempBasalRate = null; tempBasalRemainingMinutes = 0; ack(cmd)
            }

            TwiistCommand.Resume             -> {
                suspended = false; ack(cmd)
            }

            is TwiistCommand.SetTime         -> {
                clockEpochMillis = cmd.epochMillis; utcOffsetMinutes = cmd.utcOffsetMinutes; ack(cmd)
            }
        }
    }

    private fun TwiistPumpState.status() = TwiistResponse.Status(
        reservoirUnits = reservoirUnits,
        batteryPercent = batteryPercent,
        suspended = suspended,
        bolusInProgress = bolusInProgress,
        lastBolusDelivered = lastBolusDelivered,
        tempBasalRate = tempBasalRate,
        tempBasalRemainingMinutes = tempBasalRemainingMinutes,
        activeAlarm = activeAlarm
    )

    companion object {

        /** Emulator-side hard caps (placeholders mirroring the conservative PumpType limits). */
        const val MAX_BOLUS = 25.0
        const val MAX_BASAL = 5.0
    }
}
