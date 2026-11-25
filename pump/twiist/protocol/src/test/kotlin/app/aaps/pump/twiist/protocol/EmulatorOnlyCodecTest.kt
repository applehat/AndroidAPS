package app.aaps.pump.twiist.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EmulatorOnlyCodecTest {

    private val codec = EmulatorOnlyCodec

    @Test
    fun commandsRoundTrip() {
        val commands = listOf(
            TwiistCommand.ReadStatus,
            TwiistCommand.Bolus(1.25),
            TwiistCommand.CancelBolus,
            TwiistCommand.SetTempBasal(0.85, 30),
            TwiistCommand.CancelTempBasal,
            TwiistCommand.SetBasalProfile(List(48) { 0.05 * (it % 10 + 1) }),
            TwiistCommand.Suspend,
            TwiistCommand.Resume,
            TwiistCommand.SetTime(1_700_000_000_000L, -360)
        )
        commands.forEach { cmd ->
            val decoded = codec.decodeCommand(codec.encode(cmd))
            if (cmd is TwiistCommand.SetBasalProfile) {
                assertThat((decoded as TwiistCommand.SetBasalProfile).halfHourRates.zip(cmd.halfHourRates).all { (a, b) -> kotlin.math.abs(a - b) < 1e-9 }).isTrue()
            } else assertThat(decoded).isEqualTo(cmd)
        }
    }

    @Test
    fun statusRoundTrip() {
        val status = TwiistResponse.Status(123.45, 80, false, true, 0.5, 1.2, 25, TwiistAlarm.LOW_RESERVOIR)
        assertThat(codec.decode(codec.encodeResponse(status))).isEqualTo(status)
    }

    @Test
    fun foreignBytesAreUnknownNeverSuccess() {
        assertThat(codec.decode(byteArrayOf(0x01, 0x02, 0x03))).isInstanceOf(TwiistResponse.Unknown::class.java)
        assertThat(codec.decode(byteArrayOf())).isInstanceOf(TwiistResponse.Unknown::class.java)
        assertThat(codec.decodeCommand(byteArrayOf(0x55))).isNull()
    }
}
