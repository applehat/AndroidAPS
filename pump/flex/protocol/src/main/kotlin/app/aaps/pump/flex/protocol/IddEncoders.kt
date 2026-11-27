package app.aaps.pump.flex.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Plaintext IDD PDU builders from MiniMed Instinct US 1.3.1 encoder layouts
 * (`FINDINGS-opcodes.md`). Little-endian. Insulin units packed as uint32
 * (`UnitsPacking.packInsulinUnits` = packUint32) — the exact pump-native scale factor
 * (1 U = ? raw) is TODO against hardware; this stub packs milli-units (U * 1000) so tests
 * have a deterministic mapping. Replace with the measured scale before any live use.
 *
 * These bytes are the *inner* IDD PDUs. A live pump requires the TLS 1.3 / MTS wrapper,
 * Android attestation, cloud enrollment and post-TLS passkey before it will accept them.
 * Do NOT send these on a naked GATT write to a real pump.
 */
object IddEncoders {

    private fun buf(size: Int) = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    /** Pack insulin units for the stub. TODO: replace with measured pump-native scale. */
    fun packInsulinUnits(units: Double): Int = Math.round(units * 1000.0).toInt()

    /**
     * `setCommandParameters` request: `[0]=0x06 [1]=CommandType [2..]=parameter-data`.
     */
    fun setCommandParameters(type: CommandType, parameterData: ByteArray = byteArrayOf()): ByteArray =
        buf(2 + parameterData.size)
            .put(CommandPointOpCode.SET_COMMAND_PARAMETERS.wire.toByte())
            .put(type.wire.toByte())
            .put(parameterData)
            .array()

    fun initiateTransaction(type: CommandType): ByteArray =
        byteArrayOf(CommandPointOpCode.REQUEST_TRANSACTION.wire.toByte(), type.wire.toByte())

    fun confirmCommand(type: CommandType): ByteArray =
        byteArrayOf(CommandPointOpCode.CONFIRM_COMMAND.wire.toByte(), type.wire.toByte())

    fun cancelTransaction(reason: Byte = 0): ByteArray =
        byteArrayOf(CommandPointOpCode.CANCEL_TRANSACTION.wire.toByte(), reason)

    /**
     * Manual bolus parameter data (CommandType.MANUAL_BOLUS):
     * `[0] bolusType u8 | [1..4] amountFast u32 | [5..8] amountSquare u32 | [9..10] duration u16 | [11] trailing=0x02`
     */
    fun bolusParameterData(
        bolusType: Int,
        amountFast: Double,
        amountSquare: Double = 0.0,
        durationMinutes: Int = 0
    ): ByteArray = buf(12)
        .put(bolusType.toByte())
        .putInt(packInsulinUnits(amountFast))
        .putInt(packInsulinUnits(amountSquare))
        .putShort(durationMinutes.toShort())
        .put(0x02)
        .array()

    fun fillCannulaParameterData(units: Double): ByteArray =
        buf(4).putInt(packInsulinUnits(units)).array()

    fun fillTubingParameterData(units: Double): ByteArray =
        buf(4).putInt(packInsulinUnits(units)).array()

    /**
     * Temp target parameter data (CommandType.SET_TEMP_TARGET):
     * `[0] flag u8 | [1..2] target u16`
     */
    fun tempTargetParameterData(flag: Int, target: Int): ByteArray =
        buf(3).put(flag.toByte()).putShort(target.toShort()).array()

    /**
     * SRCP request: `[0..1] opcode u16 | [2..3] operand u16`
     */
    fun srcpRequest(op: IddSrcpOpCode, operand: Int = 0): ByteArray =
        buf(4).putShort(op.wire.toShort()).putShort(operand.toShort()).array()

    /**
     * RACP request:
     * `[0] opcode u8 | [1] operator u8 | [2] filter u8 | [3..6] min u32 | [7..10] max u32`
     */
    fun racpRequest(
        op: IdsRacpOpCode,
        operator: IdsRacpOperator,
        filter: Int = 0,
        rangeMin: Long = 0,
        rangeMax: Long = 0
    ): ByteArray = buf(11)
        .put(op.wire.toByte())
        .put(operator.wire.toByte())
        .put(filter.toByte())
        .putInt(rangeMin.toInt())
        .putInt(rangeMax.toInt())
        .array()
}
