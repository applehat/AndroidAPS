package app.aaps.pump.flex.protocol

/**
 * Turns [FlexCommand]s into bytes and bytes into [FlexResponse]s.
 *
 * [PlaintextIddCodec] encodes the recovered Instinct/Flex IDD plaintext PDUs (opcodes + layouts
 * from reversing-flex). A live pump will not accept these bytes on a naked GATT write — they must
 * ride inside the TLS 1.3 / MTS tunnel after attestation + enrollment + passkey. The emulator
 * speaks the same plaintext codec so the driver can be exercised without hardware or crypto.
 */
interface FlexCodec {

    fun encode(command: FlexCommand): ByteArray
    fun decode(data: ByteArray): FlexResponse

    /** Emulator-side helpers (the emulator speaks the same codec in reverse). */
    fun decodeCommand(data: ByteArray): FlexCommand?
    fun encodeResponse(response: FlexResponse): ByteArray
}
