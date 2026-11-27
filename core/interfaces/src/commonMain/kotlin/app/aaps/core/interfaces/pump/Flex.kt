package app.aaps.core.interfaces.pump

/**
 * Functionality supported by Medtronic Flex / Instinct pumps only. Grows as the driver does.
 */
interface Flex {

    /** True while the driver is a skeleton: no insulin-delivery command can succeed. */
    val isDeliveryLocked: Boolean
}
