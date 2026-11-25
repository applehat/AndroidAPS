package app.aaps.pump.twiist.di

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.configuration.ExternalOptions
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.twiist.ble.TwiistNoRadioBleTransport
import app.aaps.pump.twiist.emulator.TwiistEmulatorBleTransport
import app.aaps.pump.twiist.emulator.TwiistPumpEmulator
import app.aaps.pump.twiist.protocol.TwiistBleTransport
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Picks the twiist transport: the in-tree emulator when the `emulate_twiist` marker file is present,
 * otherwise the radio-less placeholder (real BLE is not implemented yet).
 */
@ContributesTo(AppScope::class)
@BindingContainer
object TwiistBleBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideTwiistBleTransport(config: Config, aapsLogger: AAPSLogger): TwiistBleTransport =
        if (config.isEnabled(ExternalOptions.EMULATE_TWIIST)) {
            aapsLogger.debug(LTag.PUMPEMULATOR, "twiist emulator active - real Bluetooth is not used")
            TwiistEmulatorBleTransport(TwiistPumpEmulator(aapsLogger = aapsLogger), aapsLogger)
        } else TwiistNoRadioBleTransport()
}
