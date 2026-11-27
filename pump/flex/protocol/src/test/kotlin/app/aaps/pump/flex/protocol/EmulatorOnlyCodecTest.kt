package app.aaps.pump.flex.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EmulatorOnlyCodecTest {

    private val codec = EmulatorOnlyCodec

    @Test
    fun commandsRoundTrip() {
        val commands = listOf(
            FlexCommand.ReadStatus,
            FlexCommand.Bolus(1.25),
            FlexCommand.CancelBolus,
            FlexCommand.SetTempBasal(0.85, 30),
            FlexCommand.CancelTempBasal,
            FlexCommand.SetBasalProfile(List(48) { 0.05 * (it % 10 + 1) }),
            FlexCommand.Suspend,
            FlexCommand.Resume,
            FlexCommand.FillCannula(0.3),
            FlexCommand.FillTubing(1.0),
            FlexCommand.SetTempTarget(110, 1),
            FlexCommand.CancelTempTarget
        )
        commands.forEach { cmd ->
            val decoded = codec.decodeCommand(codec.encode(cmd))
            if (cmd is FlexCommand.SetBasalProfile) {
                assertThat((decoded as FlexCommand.SetBasalProfile).halfHourRates.zip(cmd.halfHourRates).all { (a, b) -> kotlin.math.abs(a - b) < 1e-9 }).isTrue()
            } else assertThat(decoded).isEqualTo(cmd)
        }
    }

    @Test
    fun statusRoundTrip() {
        val status = FlexResponse.Status(123.45, 80, false, true, 0.5, 1.2, 25, FlexAlarm.LOW_RESERVOIR)
        assertThat(codec.decode(codec.encodeResponse(status))).isEqualTo(status)
    }

    @Test
    fun foreignBytesAreUnknownNeverSuccess() {
        assertThat(codec.decode(byteArrayOf(0x01, 0x02, 0x03))).isInstanceOf(FlexResponse.Unknown::class.java)
        assertThat(codec.decode(byteArrayOf())).isInstanceOf(FlexResponse.Unknown::class.java)
        assertThat(codec.decodeCommand(byteArrayOf(0x55))).isNull()
    }
}
