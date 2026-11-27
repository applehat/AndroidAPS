package app.aaps.pump.flex

import app.aaps.core.data.pump.defs.ManufacturerType
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpProfile
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.pump.flex.emulator.FlexEmulatorBleTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * SAFETY: every path that could change insulin delivery must fail closed in the skeleton, even with
 * a fully working (emulated) transport behind it. The fake result starts as success=true so the test
 * proves the plugin actively sets failure rather than inheriting a default.
 */
class FlexPumpPluginFailClosedTest {

    private val rh: ResourceHelper = mock<ResourceHelper>().also { whenever(it.gs(any<Int>())).thenReturn("locked") }
    private val transport = FlexEmulatorBleTransport()
    private lateinit var plugin: FlexPumpPlugin

    @BeforeEach
    fun setup() {
        plugin = FlexPumpPlugin(
            aapsLogger = mock<AAPSLogger>(),
            rh = rh,
            preferences = mock<Preferences>(),
            commandQueue = mock<CommandQueue>(),
            flexPump = FlexPump(),
            transport = transport,
            pumpEnactResultProvider = { OptimisticResult() },
            notificationManager = mock<NotificationManager>()
        )
    }

    private fun PumpEnactResult.assertRefused() {
        assertThat(success).isFalse()
        assertThat(enacted).isFalse()
        assertThat(bolusDelivered).isEqualTo(0.0)
    }

    @Test
    fun everyDeliveryPathIsRefused() = runBlocking {
        plugin.deliverTreatment(DetailedBolusInfo().apply { insulin = 1.0 }).assertRefused()
        plugin.setTempBasalAbsolute(1.0, 30, true, PumpSync.TemporaryBasalType.NORMAL).assertRefused()
        plugin.setTempBasalPercent(150, 30, true, PumpSync.TemporaryBasalType.NORMAL).assertRefused()
        plugin.cancelTempBasal(true).assertRefused()
        plugin.setExtendedBolus(1.0, 30).assertRefused()
        plugin.cancelExtendedBolus().assertRefused()
        plugin.setNewBasalProfile(mock<PumpProfile>()).assertRefused()
        plugin.loadTDDs().assertRefused()
        // Nothing may have been sent to the (emulated) pump.
        assertThat(transport.writes).isEmpty()
    }

    @Test
    fun skeletonIsNeverConfiguredOrConnected() {
        assertThat(plugin.isConfigured()).isFalse()
        assertThat(plugin.isInitialized()).isFalse()
        assertThat(plugin.isConnected()).isFalse()
        assertThat(plugin.isDeliveryLocked).isTrue()
    }

    @Test
    fun identity() {
        assertThat(plugin.manufacturer()).isEqualTo(ManufacturerType.Medtronic)
        assertThat(plugin.model()).isEqualTo(PumpType.MEDTRONIC_FLEX)
    }

    private class OptimisticResult : PumpEnactResult {

        override var success: Boolean = true
        override var enacted: Boolean = true
        override var comment: String = ""
        override var duration: Int = -1
        override var absolute: Double = -1.0
        override var percent: Int = -1
        override var isPercent: Boolean = false
        override var isTempCancel: Boolean = false
        override var bolusDelivered: Double = 0.0
        override var queued: Boolean = false
        override var cancelled: Boolean = false

        override fun success(success: Boolean): PumpEnactResult = apply { this.success = success }
        override fun enacted(enacted: Boolean): PumpEnactResult = apply { this.enacted = enacted }
        override fun comment(comment: String): PumpEnactResult = apply { this.comment = comment }
        override fun comment(ref: TextRef): PumpEnactResult = apply { this.comment = ref.toString() }
        override fun duration(duration: Int): PumpEnactResult = apply { this.duration = duration }
        override fun absolute(absolute: Double): PumpEnactResult = apply { this.absolute = absolute }
        override fun percent(percent: Int): PumpEnactResult = apply { this.percent = percent }
        override fun isPercent(isPercent: Boolean): PumpEnactResult = apply { this.isPercent = isPercent }
        override fun isTempCancel(isTempCancel: Boolean): PumpEnactResult = apply { this.isTempCancel = isTempCancel }
        override fun bolusDelivered(bolusDelivered: Double): PumpEnactResult = apply { this.bolusDelivered = bolusDelivered }
        override fun queued(queued: Boolean): PumpEnactResult = apply { this.queued = queued }
        override fun cancelled(cancelled: Boolean): PumpEnactResult = apply { this.cancelled = cancelled }
    }
}
