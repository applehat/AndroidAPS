package app.aaps.pump.flex.protocol

/**
 * IDD command sub-opcode (byte 1 of setCommandParameters / initiate / confirm).
 *
 * Recovered from MiniMed Instinct US 1.3.1 (`CommandType`, 25 members, jump table at
 * `findCommandTypeByValue`). High-bit values (0x81/0x82/0x83) are used for temp-target /
 * meal-wizard commands.
 */
enum class CommandType(val wire: Int) {
    THERAPY_CONTROL(0x01),
    CLEAR_ACTIVE_INSULIN(0x02),
    SET_TEMP_TARGET(0x81),
    CANCEL_TEMP_TARGET(0x82),
    CANCEL_BOLUS(0x03),
    MANUAL_BOLUS(0x06),
    CONFIGURE_BASAL_PATTERN(0x07),
    CONFIGURE_AND_ACTIVATE_BASAL_PATTERN(0x08),
    DELETE_BACKUP_SETTING(0x0A),
    SET_MAX_BOLUS(0x0B),
    SET_MAX_BASAL(0x0C),
    STOP_TEMP_BASAL(0x0D),
    TEMP_BASAL(0x0E),
    BOLUS_WIZARD(0x0F),
    MEAL_WIZARD(0x83),
    SET_BG_TARGET(0x10),
    SET_ISF(0x11),
    SET_CARB_RATIO(0x12),
    SET_ACTIVE_INSULIN_TIME(0x13),
    TOGGLE_BOLUS_WIZARD(0x14),
    MANUAL_SUSPEND(0x17),
    SET_NOTIFICATION_SETTINGS(0x1C),
    FILL_TUBING(0x19),
    FILL_CANNULA(0x1A),
    SET_SMART_GUARD_SETTING(0x1E);

    companion object {
        fun fromWire(value: Int): CommandType? = entries.firstOrNull { it.wire == value }
    }
}
