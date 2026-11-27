package app.aaps.pump.flex.protocol

/**
 * Medtronic custom GATT UUID base and known IDD / security characteristic short IDs.
 *
 * Base: `0000XXXX-0000-1000-0000-009132591325` (company-id tail 00-91-32-59-13-25).
 *
 * Role mapping of the custom 16-bit values (0101/0102/… → ICP vs SRCP vs Status vs History)
 * is **unconfirmed** until an on-device GATT discovery log. Do not assume SIG IDD layout.
 */
object FlexBleUuids {

    const val BASE_SUFFIX = "-0000-1000-0000-009132591325"
    const val BASE_SUFFIX_2000 = "-0000-2000-0000-009132591325"
    const val SIG_BASE_SUFFIX = "-0000-1000-8000-00805F9B34FB"

    /** IDD service parent. */
    const val IDD_SERVICE = "00000100$BASE_SUFFIX"

    /** SIG Record Access Control Point (history). */
    const val RACP = "00002A52$SIG_BASE_SUFFIX"

    /** Encrypted IDD characteristics (role mapping unconfirmed). */
    val IDD_CHARACTERISTICS = listOf(
        "00000101$BASE_SUFFIX",
        "00000102$BASE_SUFFIX",
        "00000103$BASE_SUFFIX",
        "00000105$BASE_SUFFIX",
        "00000108$BASE_SUFFIX",
        "00000110$BASE_SUFFIX",
        "00000112$BASE_SUFFIX",
        "00000113$BASE_SUFFIX",
    )

    fun uuid16(assigned: Int, family2000: Boolean = false): String =
        "0000%04X%s".format(assigned, if (family2000) BASE_SUFFIX_2000 else BASE_SUFFIX)
}
