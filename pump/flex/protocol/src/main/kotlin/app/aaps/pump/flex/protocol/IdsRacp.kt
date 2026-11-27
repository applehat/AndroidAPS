package app.aaps.pump.flex.protocol

/**
 * Record Access Control Point (history) opcodes / operators.
 *
 * Recovered from MiniMed Instinct US 1.3.1. Rides the SIG RACP characteristic `0x2A52`.
 * Many opcode/operator values are constant-weight (balanced) byte codes.
 */
enum class IdsRacpOpCode(val wire: Int) {
    REPORT_STORED_RECORDS(0x33),
    RESPONSE(0x0F);

    companion object {
        fun fromWire(value: Int): IdsRacpOpCode? = entries.firstOrNull { it.wire == value }
    }
}

enum class IdsRacpOperator(val wire: Int) {
    NONE(0x0F),
    RANGE(0x5A),
    FIRST(0x66),
    LAST(0x69);

    companion object {
        fun fromWire(value: Int): IdsRacpOperator? = entries.firstOrNull { it.wire == value }
    }
}
