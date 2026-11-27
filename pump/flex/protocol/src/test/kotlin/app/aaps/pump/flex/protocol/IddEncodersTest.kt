package app.aaps.pump.flex.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Golden-byte checks against layouts recovered from MiniMed Instinct 1.3.1 (FINDINGS-opcodes.md).
 * Insulin packing uses the stub milli-unit scale (U*1000) until hardware measures the real factor.
 */
class IddEncodersTest {

    @Test
    fun commandPointOpcodesMatchRecoveredTable() {
        assertThat(CommandPointOpCode.REQUEST_TRANSACTION.wire).isEqualTo(0x01)
        assertThat(CommandPointOpCode.SET_COMMAND_PARAMETERS.wire).isEqualTo(0x06)
        assertThat(CommandPointOpCode.CONFIRM_COMMAND.wire).isEqualTo(0x08)
        assertThat(CommandPointOpCode.PROTOCOL_ERROR.wire).isEqualTo(0xFF)
        assertThat(CommandPointOpCode.fromWire(0x06)).isEqualTo(CommandPointOpCode.SET_COMMAND_PARAMETERS)
    }

    @Test
    fun commandTypesMatchRecoveredTable() {
        assertThat(CommandType.MANUAL_BOLUS.wire).isEqualTo(0x06)
        assertThat(CommandType.TEMP_BASAL.wire).isEqualTo(0x0E)
        assertThat(CommandType.STOP_TEMP_BASAL.wire).isEqualTo(0x0D)
        assertThat(CommandType.FILL_CANNULA.wire).isEqualTo(0x1A)
        assertThat(CommandType.SET_TEMP_TARGET.wire).isEqualTo(0x81)
        assertThat(CommandType.SET_SMART_GUARD_SETTING.wire).isEqualTo(0x1E)
        assertThat(CommandType.fromWire(0x06)).isEqualTo(CommandType.MANUAL_BOLUS)
    }

    @Test
    fun bolusParameterLayout() {
        // 1.250 U normal bolus => milli-units 1250 = 0x000004E2 LE
        val params = IddEncoders.bolusParameterData(BolusDeliveryType.NORMAL.wire, 1.25)
        assertThat(params.size).isEqualTo(12)
        assertThat(params[0]).isEqualTo(0x00.toByte())
        // amountFast LE
        assertThat(params[1]).isEqualTo(0xE2.toByte())
        assertThat(params[2]).isEqualTo(0x04.toByte())
        assertThat(params[3]).isEqualTo(0x00.toByte())
        assertThat(params[4]).isEqualTo(0x00.toByte())
        // trailing flag
        assertThat(params[11]).isEqualTo(0x02.toByte())

        val frame = IddEncoders.setCommandParameters(CommandType.MANUAL_BOLUS, params)
        assertThat(frame[0]).isEqualTo(0x06.toByte())
        assertThat(frame[1]).isEqualTo(0x06.toByte())
        assertThat(frame.size).isEqualTo(14)
    }

    @Test
    fun transactionFraming() {
        assertThat(IddEncoders.initiateTransaction(CommandType.MANUAL_BOLUS).toList())
            .isEqualTo(listOf(0x01.toByte(), 0x06.toByte()))
        assertThat(IddEncoders.confirmCommand(CommandType.TEMP_BASAL).toList())
            .isEqualTo(listOf(0x08.toByte(), 0x0E.toByte()))
    }

    @Test
    fun srcpAndRacpLayouts() {
        val srcp = IddEncoders.srcpRequest(IddSrcpOpCode.INSULIN_ON_BOARD)
        assertThat(srcp.size).isEqualTo(4)
        // 0x3F3 LE
        assertThat(srcp[0]).isEqualTo(0xF3.toByte())
        assertThat(srcp[1]).isEqualTo(0x03.toByte())

        val racp = IddEncoders.racpRequest(IdsRacpOpCode.REPORT_STORED_RECORDS, IdsRacpOperator.RANGE)
        assertThat(racp[0]).isEqualTo(0x33.toByte())
        assertThat(racp[1]).isEqualTo(0x5A.toByte())
    }

    @Test
    fun uuidBase() {
        assertThat(FlexBleUuids.IDD_SERVICE).startsWith("00000100-")
        assertThat(FlexBleUuids.IDD_SERVICE).endsWith("009132591325")
        assertThat(FlexBleUuids.RACP).contains("2A52")
        assertThat(FlexBleUuids.IDD_CHARACTERISTICS).hasSize(8)
    }
}
