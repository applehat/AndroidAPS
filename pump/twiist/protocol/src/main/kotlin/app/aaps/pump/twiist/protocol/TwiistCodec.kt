package app.aaps.pump.twiist.protocol

/**
 * Turns [TwiistCommand]s into bytes and bytes into [TwiistResponse]s.
 *
 * The real twiist BLE protocol is NOT known yet. The only implementation is [EmulatorOnlyCodec], an
 * invented framing used purely so the driver and emulator can be exercised end to end. It will
 * never work against a real pump and must never be bound to the real transport.
 */
interface TwiistCodec {

    fun encode(command: TwiistCommand): ByteArray
    fun decode(data: ByteArray): TwiistResponse

    /** Emulator-side helpers (the emulator speaks the same codec in reverse). */
    fun decodeCommand(data: ByteArray): TwiistCommand?
    fun encodeResponse(response: TwiistResponse): ByteArray
}
