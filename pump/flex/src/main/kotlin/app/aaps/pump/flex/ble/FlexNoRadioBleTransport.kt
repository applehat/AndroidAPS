package app.aaps.pump.flex.ble

import app.aaps.core.interfaces.pump.ble.BleAdapter
import app.aaps.core.interfaces.pump.ble.BleGatt
import app.aaps.core.interfaces.pump.ble.BleScanner
import app.aaps.core.interfaces.pump.ble.BleTransportListener
import app.aaps.core.interfaces.pump.ble.PairingState
import app.aaps.core.interfaces.pump.ble.ScannedDevice
import app.aaps.pump.flex.protocol.FlexBleTransport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Placeholder for the real Android BLE + TLS-over-BLE transport. Refuses every connection and
 * write, so nothing can reach a physical pump until the authenticated channel has been built and
 * validated. Replace with FlexBleTransportImpl once hardware bring-up starts.
 */
class FlexNoRadioBleTransport : FlexBleTransport {

    override var targetAddress: String? = null
    override val isOperational: Boolean = false

    override val adapter: BleAdapter = object : BleAdapter {
        override fun enable() {}
        override fun getDeviceName(address: String): String? = null
        override fun isDeviceBonded(address: String): Boolean = false
        override fun createBond(address: String): Boolean = false
        override fun removeBond(address: String) {}
    }

    override val scanner: BleScanner = object : BleScanner {
        override val scannedDevices: SharedFlow<ScannedDevice> = MutableSharedFlow()
        override fun startScan() {}
        override fun stopScan() {}
    }

    override val gatt: BleGatt = object : BleGatt {
        override fun connect(address: String): Boolean = false
        override fun disconnect() {}
        override fun close() {}
        override fun discoverServices() {}
        override fun findCharacteristics(): Boolean = false
        override fun enableNotifications() {}
        override fun writeCharacteristic(data: ByteArray) {
            error("flex: real BLE/TLS transport not implemented - refusing write")
        }
    }

    private val _pairingState = MutableStateFlow(PairingState())
    override val pairingState: StateFlow<PairingState> = _pairingState
    override fun updatePairingState(state: PairingState) {
        _pairingState.value = state
    }

    override fun setListener(listener: BleTransportListener?) {}
}
