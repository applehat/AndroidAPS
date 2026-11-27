package app.aaps.pump.flex.emulator

import app.aaps.pump.flex.protocol.EmulatorOnlyCodec
import app.aaps.pump.flex.protocol.FlexAlarm
import app.aaps.pump.flex.protocol.FlexCommand
import app.aaps.pump.flex.protocol.FlexResponse
import app.aaps.pump.flex.protocol.NackReason
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class FlexPumpEmulatorTest {

    private val emulator = FlexPumpEmulator()
    private fun send(cmd: FlexCommand): FlexResponse = EmulatorOnlyCodec.decode(emulator.handle(EmulatorOnlyCodec.encode(cmd)).single())

    @Test
    fun bolusDeductsReservoir() {
        assertThat(send(FlexCommand.Bolus(2.0))).isInstanceOf(FlexResponse.Ack::class.java)
        val status = send(FlexCommand.ReadStatus) as FlexResponse.Status
        assertThat(status.reservoirUnits).isWithin(1e-9).of(198.0)
        assertThat(status.lastBolusDelivered).isWithin(1e-9).of(2.0)
    }

    @Test
    fun bolusRefusedWhenSuspendedOccludedOrOutOfRange() {
        send(FlexCommand.Suspend)
        assertThat((send(FlexCommand.Bolus(1.0)) as FlexResponse.Nack).reason).isEqualTo(NackReason.SUSPENDED)
        send(FlexCommand.Resume)
        emulator.state.activeAlarm = FlexAlarm.OCCLUSION
        assertThat((send(FlexCommand.Bolus(1.0)) as FlexResponse.Nack).reason).isEqualTo(NackReason.ALARM_ACTIVE)
        emulator.state.activeAlarm = null
        assertThat((send(FlexCommand.Bolus(100.0)) as FlexResponse.Nack).reason).isEqualTo(NackReason.OUT_OF_RANGE)
        emulator.state.reservoirUnits = 0.5
        assertThat((send(FlexCommand.Bolus(1.0)) as FlexResponse.Nack).reason).isEqualTo(NackReason.RESERVOIR_EMPTY)
    }

    @Test
    fun partialDeliveryIsReportedNotHidden() {
        emulator.state.bolusDeliveryFraction = 0.4
        send(FlexCommand.Bolus(5.0))
        val status = send(FlexCommand.ReadStatus) as FlexResponse.Status
        assertThat(status.lastBolusDelivered).isWithin(1e-9).of(2.0)
    }

    @Test
    fun suspendCancelsTempBasal() {
        send(FlexCommand.SetTempBasal(1.0, 60))
        send(FlexCommand.Suspend)
        val status = send(FlexCommand.ReadStatus) as FlexResponse.Status
        assertThat(status.tempBasalRate).isNull()
        assertThat(status.suspended).isTrue()
    }

    @Test
    fun silentAndGarbledFaults() {
        emulator.state.silent = true
        assertThat(emulator.handle(EmulatorOnlyCodec.encode(FlexCommand.ReadStatus))).isEmpty()
        emulator.state.silent = false
        emulator.state.garbleReplies = true
        assertThat(send(FlexCommand.ReadStatus)).isInstanceOf(FlexResponse.Unknown::class.java)
    }
}
