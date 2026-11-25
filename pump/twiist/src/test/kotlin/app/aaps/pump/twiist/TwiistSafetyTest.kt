package app.aaps.pump.twiist

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Guard rail: the skeleton must stay inert. Deliberately fails the build if someone flips the switch
 * without also replacing this test with real delivery-validation tests.
 */
class TwiistSafetyTest {

    @Test
    fun deliveryIsLockedInSkeleton() {
        assertThat(TwiistSafety.DELIVERY_ENABLED).isFalse()
    }
}
