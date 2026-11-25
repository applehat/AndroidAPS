package app.aaps.core.interfaces.pump

/**
 * Functionality supported by Sequel twiist pumps only. Grows as the driver does.
 */
interface Twiist {

    /** True while the driver is a skeleton: no insulin-delivery command can succeed. */
    val isDeliveryLocked: Boolean
}
