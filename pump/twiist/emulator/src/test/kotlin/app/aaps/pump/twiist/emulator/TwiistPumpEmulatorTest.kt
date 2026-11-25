package app.aaps.pump.twiist.emulator

import app.aaps.pump.twiist.protocol.EmulatorOnlyCodec
import app.aaps.pump.twiist.protocol.NackReason
import app.aaps.pump.twiist.protocol.TwiistAlarm
import app.aaps.pump.twiist.protocol.TwiistCommand
import app.aaps.pump.twiist.protocol.TwiistResponse
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TwiistPumpEmulatorTest {

    private val emulator = TwiistPumpEmulator()
    private fun send(cmd: TwiistCommand): TwiistResponse = EmulatorOnlyCodec.decode(emulator.handle(EmulatorOnlyCodec.encode(cmd)).single())

    @Test
    fun bolusDeductsReservoir() {
        assertThat(send(TwiistCommand.Bolus(2.0))).isInstanceOf(TwiistResponse.Ack::class.java)
        val status = send(TwiistCommand.ReadStatus) as TwiistResponse.Status
        assertThat(status.reservoirUnits).isWithin(1e-9).of(198.0)
        assertThat(status.lastBolusDelivered).isWithin(1e-9).of(2.0)
    }

    @Test
    fun bolusRefusedWhenSuspendedOccludedOrOutOfRange() {
        send(TwiistCommand.Suspend)
        assertThat((send(TwiistCommand.Bolus(1.0)) as TwiistResponse.Nack).reason).isEqualTo(NackReason.SUSPENDED)
        send(TwiistCommand.Resume)
        emulator.state.activeAlarm = TwiistAlarm.OCCLUSION
        assertThat((send(TwiistCommand.Bolus(1.0)) as TwiistResponse.Nack).reason).isEqualTo(NackReason.ALARM_ACTIVE)
        emulator.state.activeAlarm = null
        assertThat((send(TwiistCommand.Bolus(100.0)) as TwiistResponse.Nack).reason).isEqualTo(NackReason.OUT_OF_RANGE)
        emulator.state.reservoirUnits = 0.5
        assertThat((send(TwiistCommand.Bolus(1.0)) as TwiistResponse.Nack).reason).isEqualTo(NackReason.RESERVOIR_EMPTY)
    }

    @Test
    fun partialDeliveryIsReportedNotHidden() {
        emulator.state.bolusDeliveryFraction = 0.4
        send(TwiistCommand.Bolus(5.0))
        val status = send(TwiistCommand.ReadStatus) as TwiistResponse.Status
        assertThat(status.lastBolusDelivered).isWithin(1e-9).of(2.0)
    }

    @Test
    fun suspendCancelsTempBasal() {
        send(TwiistCommand.SetTempBasal(1.0, 60))
        send(TwiistCommand.Suspend)
        val status = send(TwiistCommand.ReadStatus) as TwiistResponse.Status
        assertThat(status.tempBasalRate).isNull()
        assertThat(status.suspended).isTrue()
    }

    @Test
    fun silentAndGarbledFaults() {
        emulator.state.silent = true
        assertThat(emulator.handle(EmulatorOnlyCodec.encode(TwiistCommand.ReadStatus))).isEmpty()
        emulator.state.silent = false
        emulator.state.garbleReplies = true
        assertThat(send(TwiistCommand.ReadStatus)).isInstanceOf(TwiistResponse.Unknown::class.java)
    }
}
