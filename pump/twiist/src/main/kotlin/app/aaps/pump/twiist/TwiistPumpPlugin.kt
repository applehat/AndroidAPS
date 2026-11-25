package app.aaps.pump.twiist

import app.aaps.core.data.plugin.PluginType
import app.aaps.core.data.pump.defs.ManufacturerType
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.di.PumpDriver
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpPluginBase
import app.aaps.core.interfaces.pump.PumpProfile
import app.aaps.core.interfaces.pump.PumpRate
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.Twiist
import app.aaps.core.interfaces.pump.defs.fillFor
import app.aaps.core.interfaces.pump.mapState
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.core.ui.compose.icons.IcPluginVirtualPump
import app.aaps.pump.twiist.compose.TwiistComposeContent
import app.aaps.pump.twiist.protocol.TwiistBleTransport
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow
import dev.zacsweers.metro.IntKey as MetroIntKey

/**
 * Sequel twiist pump driver - SKELETON.
 *
 * SAFETY (fail closed): until [TwiistSafety.DELIVERY_ENABLED] is true, every call that could change
 * insulin delivery returns success=false / enacted=false, and [isConfigured] is false so the command
 * queue never tries to reach a pump. The real protocol does not exist yet.
 *
 * Structure follows Carelevo (driver / protocol / emulator modules) with Medtrum-style Pump overrides.
 */
// Pump plugin registration - @MetroIntKey range 1000-1200, see AppRootGraph for the overview
@ContributesIntoMap(AppScope::class, binding = binding<PluginBase>())
@PumpDriver
@MetroIntKey(1140)
@SingleIn(AppScope::class)
@Inject
class TwiistPumpPlugin(
    aapsLogger: AAPSLogger,
    override val rh: ResourceHelper,
    preferences: Preferences,
    commandQueue: CommandQueue,
    private val twiistPump: TwiistPump,
    private val transport: TwiistBleTransport,
    private val pumpEnactResultProvider: () -> PumpEnactResult,
    notificationManager: NotificationManager
) : PumpPluginBase(
    pluginDescription = PluginDescription()
        .mainType(PluginType.PUMP)
        .icon(IcPluginVirtualPump)
        .pluginName(TextRef.AndroidRes(R.string.twiist))
        .description(TextRef.AndroidRes(R.string.twiist_pump_description))
        .composeContent { _ ->
            TwiistComposeContent(
                pluginName = rh.gs(R.string.twiist),
                transportIsEmulator = transport.isOperational
            )
        },
    ownPreferences = emptyList(),
    aapsLogger, rh, preferences, commandQueue, notificationManager
), Pump, Twiist {

    override val isDeliveryLocked: Boolean get() = !TwiistSafety.DELIVERY_ENABLED

    private fun locked(what: String): PumpEnactResult {
        aapsLogger.warn(LTag.PUMP, "twiist: refusing $what - driver skeleton, delivery locked")
        return pumpEnactResultProvider().success(false).enacted(false).comment(rh.gs(R.string.twiist_delivery_locked))
    }

    // ---- state --------------------------------------------------------------------------------
    // SAFETY: not configured -> PumpPluginBase skips the initial status read and the queue stays idle.
    override fun isConfigured(): Boolean = false
    override fun isInitialized(): Boolean = false
    override fun isSuspended(): Boolean = true
    override fun isBusy(): Boolean = false
    override fun isConnected(): Boolean = false
    override fun isConnecting(): Boolean = false
    override fun isHandshakeInProgress(): Boolean = false

    override fun connect(reason: String) {
        aapsLogger.debug(LTag.PUMP, "twiist connect($reason) ignored - skeleton")
    }

    override fun disconnect(reason: String) {}
    override fun stopConnecting() {}

    override suspend fun getPumpStatus(reason: String) {
        aapsLogger.debug(LTag.PUMP, "twiist getPumpStatus($reason) - not implemented")
    }

    // ---- delivery: ALL fail closed ------------------------------------------------------------
    // TODO(safety): real implementations must read delivery back from the pump, handle "unknown
    // outcome" after a lost link by querying status before any retry, and report via PumpSync with
    // pump-unique ids. Validate each with the emulator fault cases, then saline-only on hardware.
    override suspend fun setNewBasalProfile(profile: PumpProfile): PumpEnactResult = locked("setNewBasalProfile")

    // Report "set" so AAPS does not loop on profile pushes to an unconfigured pump. Must compare against
    // the PUMP-REPORTED profile once implemented.
    override fun isThisProfileSet(profile: PumpProfile): Boolean = true

    override suspend fun deliverTreatment(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult = locked("bolus ${detailedBolusInfo.insulin}U")

    override fun stopBolusDelivering() {
        // Nothing can be in progress while locked. Once delivery exists this MUST be implemented first.
    }

    override suspend fun setTempBasalAbsolute(absoluteRate: Double, durationInMinutes: Int, enforceNew: Boolean, tbrType: PumpSync.TemporaryBasalType): PumpEnactResult =
        locked("TBR $absoluteRate U/h ${durationInMinutes}m")

    override suspend fun setTempBasalPercent(percent: Int, durationInMinutes: Int, enforceNew: Boolean, tbrType: PumpSync.TemporaryBasalType): PumpEnactResult =
        locked("TBR $percent% ${durationInMinutes}m")

    override suspend fun cancelTempBasal(enforceNew: Boolean): PumpEnactResult = locked("cancelTempBasal")
    override suspend fun setExtendedBolus(insulin: Double, durationInMinutes: Int): PumpEnactResult = locked("extended bolus")
    override suspend fun cancelExtendedBolus(): PumpEnactResult = locked("cancelExtendedBolus")
    override suspend fun loadTDDs(): PumpEnactResult = locked("loadTDDs")

    // ---- reported values (from pump status reads only) ---------------------------------------
    override val lastDataTime: StateFlow<Long> = twiistPump.lastDataTime
    override val lastBolusTime: StateFlow<Long?> = twiistPump.lastBolusTime
    override val lastBolusAmount: StateFlow<PumpInsulin?> = twiistPump.lastBolusAmount.mapState { it?.let(::PumpInsulin) }
    override val baseBasalRate: PumpRate get() = PumpRate(twiistPump.baseBasalRate)
    override val reservoirLevel: StateFlow<PumpInsulin> = twiistPump.reservoir.mapState(::PumpInsulin)
    override val batteryLevel: StateFlow<Int?> = twiistPump.battery

    override fun manufacturer(): ManufacturerType = ManufacturerType.Sequel
    override fun model(): PumpType = twiistPump.pumpType
    override fun serialNumber(): String = twiistPump.serialNumber
    override val pumpDescription: PumpDescription = PumpDescription().fillFor(PumpType.SEQUEL_TWIIST)
    override val isFakingTempsByExtendedBoluses: Boolean = false
    override fun canHandleDST(): Boolean = false // TODO verify pump clock/timezone behaviour
    override fun pumpSpecificShortStatus(veryShort: Boolean): String = "twiist: delivery locked (skeleton)"
}
