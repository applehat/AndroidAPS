package app.aaps.pump.flex.emulator

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.flex.protocol.EmulatorOnlyCodec
import app.aaps.pump.flex.protocol.FlexAlarm
import app.aaps.pump.flex.protocol.FlexCodec
import app.aaps.pump.flex.protocol.FlexCommand
import app.aaps.pump.flex.protocol.FlexResponse
import app.aaps.pump.flex.protocol.NackReason

/**
 * Behavioural model of a Flex pump, speaking [EmulatorOnlyCodec].
 *
 * Lets the driver's safety logic be exercised (rejections, partial delivery, lost replies, alarms)
 * without hardware. Behaviour is a GUESS until checked against the real pump.
 */
class FlexPumpEmulator(
    val state: FlexPumpState = FlexPumpState(),
    private val aapsLogger: AAPSLogger? = null,
    private val codec: FlexCodec = EmulatorOnlyCodec
) {

    val received: MutableList<FlexCommand> = mutableListOf()

    fun handle(frame: ByteArray): List<ByteArray> {
        val command = codec.decodeCommand(frame)
        aapsLogger?.debug(LTag.PUMPEMULATOR, "flex emulator <- $command")
        if (command != null) received += command
        val response = if (command == null) FlexResponse.Nack("?", NackReason.NOT_SUPPORTED) else apply(command)
        if (state.silent) return emptyList()
        if (state.garbleReplies) return listOf(byteArrayOf(0x00, 0x13, 0x37))
        return listOf(codec.encodeResponse(response))
    }

    private fun nack(cmd: FlexCommand, reason: NackReason) = FlexResponse.Nack(cmd::class.simpleName ?: "?", reason)
    private fun ack(cmd: FlexCommand) = FlexResponse.Ack(cmd::class.simpleName ?: "?")

    private fun apply(cmd: FlexCommand): FlexResponse = with(state) {
        when (cmd) {
            FlexCommand.ReadStatus      -> status()

            is FlexCommand.Bolus        -> when {
                activeAlarm == FlexAlarm.OCCLUSION || activeAlarm == FlexAlarm.PUMP_ERROR -> nack(cmd, NackReason.ALARM_ACTIVE)
                suspended                                                                 -> nack(cmd, NackReason.SUSPENDED)
                bolusInProgress                                                           -> nack(cmd, NackReason.BUSY)
                cmd.unitsFast <= 0.0 || cmd.unitsFast > MAX_BOLUS                         -> nack(cmd, NackReason.OUT_OF_RANGE)
                cmd.unitsFast > reservoirUnits                                            -> nack(cmd, NackReason.RESERVOIR_EMPTY)
                else                                                                      -> {
                    val delivered = cmd.unitsFast * bolusDeliveryFraction
                    reservoirUnits -= delivered
                    lastBolusDelivered = delivered
                    ack(cmd)
                }
            }

            FlexCommand.CancelBolus     -> {
                bolusInProgress = false; ack(cmd)
            }

            is FlexCommand.SetTempBasal -> when {
                suspended -> nack(cmd, NackReason.SUSPENDED)
                cmd.unitsPerHour < 0.0 || cmd.unitsPerHour > MAX_BASAL || cmd.durationMinutes !in 30..(12 * 60) ->
                    nack(cmd, NackReason.OUT_OF_RANGE)
                else -> {
                    tempBasalRate = cmd.unitsPerHour; tempBasalRemainingMinutes = cmd.durationMinutes; ack(cmd)
                }
            }

            FlexCommand.CancelTempBasal -> {
                tempBasalRate = null; tempBasalRemainingMinutes = 0; ack(cmd)
            }

            is FlexCommand.SetBasalProfile -> if (cmd.halfHourRates.any { it < 0.0 || it > MAX_BASAL }) nack(cmd, NackReason.OUT_OF_RANGE)
            else {
                basalProfile = cmd.halfHourRates; ack(cmd)
            }

            FlexCommand.Suspend -> {
                suspended = true; tempBasalRate = null; tempBasalRemainingMinutes = 0; ack(cmd)
            }

            FlexCommand.Resume -> {
                suspended = false; ack(cmd)
            }

            is FlexCommand.FillCannula, is FlexCommand.FillTubing -> ack(cmd)
            is FlexCommand.SetTempTarget, FlexCommand.CancelTempTarget -> ack(cmd)
        }
    }

    private fun FlexPumpState.status() = FlexResponse.Status(
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
        const val MAX_BOLUS = 25.0
        const val MAX_BASAL = 5.0
    }
}
