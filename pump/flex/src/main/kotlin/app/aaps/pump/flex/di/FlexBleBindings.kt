package app.aaps.pump.flex.di

import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.configuration.ExternalOptions
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.flex.ble.FlexNoRadioBleTransport
import app.aaps.pump.flex.emulator.FlexEmulatorBleTransport
import app.aaps.pump.flex.emulator.FlexPumpEmulator
import app.aaps.pump.flex.protocol.FlexBleTransport
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Picks the Flex transport: the in-tree emulator when the `emulate_flex` marker file is present,
 * otherwise the radio-less placeholder (real BLE/TLS is not implemented yet).
 */
@ContributesTo(AppScope::class)
@BindingContainer
object FlexBleBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFlexBleTransport(config: Config, aapsLogger: AAPSLogger): FlexBleTransport =
        if (config.isEnabled(ExternalOptions.EMULATE_FLEX)) {
            aapsLogger.debug(LTag.PUMPEMULATOR, "flex emulator active - real Bluetooth is not used")
            FlexEmulatorBleTransport(FlexPumpEmulator(aapsLogger = aapsLogger), aapsLogger)
        } else FlexNoRadioBleTransport()
}
