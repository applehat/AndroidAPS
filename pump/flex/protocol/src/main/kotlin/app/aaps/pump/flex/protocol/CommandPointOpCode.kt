package app.aaps.pump.flex.protocol

/**
 * IDD Command Control Point framing opcode (byte 0 of the control-point PDU).
 *
 * Recovered from MiniMed Instinct US 1.3.1 `libapp.so` (`CommandPointOpCode` /
 * `findCommandPointOpCodeByValue`). Two-phase transaction:
 * requestTransaction → setCommandParameters → confirmCommand (cancel to abort).
 *
 * These are plaintext IDD values. On a live pump they ride inside the TLS 1.3 / MTS tunnel —
 * integrity (seq/nonce/CRC) is applied by that transport, not by these encoders.
 */
enum class CommandPointOpCode(val wire: Int) {
    REQUEST_TRANSACTION(0x01),
    ACCEPT_TRANSACTION(0x02),
    REJECT_TRANSACTION(0x03),
    CANCEL_TRANSACTION(0x04),
    CANCELLATION_RESULT(0x05),
    SET_COMMAND_PARAMETERS(0x06),
    PARAMETER_RESULT(0x07),
    CONFIRM_COMMAND(0x08),
    COMMAND_RESULT(0x09),
    PROTOCOL_ERROR(0xFF);

    companion object {
        fun fromWire(value: Int): CommandPointOpCode? = entries.firstOrNull { it.wire == value }
    }
}
