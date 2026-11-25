package app.aaps.pump.twiist.emulator

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.ble.BleAdapter
import app.aaps.core.interfaces.pump.ble.BleGatt
import app.aaps.core.interfaces.pump.ble.BleScanner
import app.aaps.core.interfaces.pump.ble.BleTransportListener
import app.aaps.core.interfaces.pump.ble.PairingState
import app.aaps.core.interfaces.pump.ble.ScannedDevice
import app.aaps.pump.twiist.protocol.TwiistBleTransport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [TwiistBleTransport] wired to a [TwiistPumpEmulator] instead of a radio. Modelled on
 * CarelevoEmulatorBleTransport: each write is answered synchronously on the calling thread so tests
 * stay deterministic.
 */
class TwiistEmulatorBleTransport(
    val emulator: TwiistPumpEmulator = TwiistPumpEmulator(),
    private val aapsLogger: AAPSLogger? = null
) : TwiistBleTransport {

    private var listener: BleTransportListener? = null

    @Volatile private var connected = false

    /** Every frame the driver wrote, in order. */
    val writes: MutableList<ByteArray> = mutableListOf()

    var bonded: Boolean = true
    var connectRefused: Boolean = false

    override var targetAddress: String? = null
    override val isOperational: Boolean = true

    override val adapter: BleAdapter = object : BleAdapter {
        override fun enable() {}
        override fun getDeviceName(address: String): String = deviceName()
        override fun isDeviceBonded(address: String): Boolean = bonded
        override fun createBond(address: String): Boolean {
            bonded = true; return true
        }

        override fun removeBond(address: String) {
            bonded = false
        }
    }

    override val scanner: BleScanner = object : BleScanner {
        private val flow = MutableSharedFlow<ScannedDevice>(replay = 1, extraBufferCapacity = 10)
        override val scannedDevices: SharedFlow<ScannedDevice> = flow
        override fun startScan() {
            aapsLogger?.debug(LTag.PUMPEMULATOR, "twiist emulator: scan")
            flow.tryEmit(ScannedDevice(name = deviceName(), address = macAddressString(), rssi = -30))
        }

        override fun stopScan() {}
    }

    override val gatt: BleGatt = object : BleGatt {
        override fun connect(address: String): Boolean {
            if (connectRefused) return false
            connected = true
            listener?.onConnectionStateChanged(true)
            return true
        }

        override fun disconnect() {
            if (connected) {
                connected = false
                listener?.onConnectionStateChanged(false)
            }
        }

        override fun close() {
            connected = false
        }

        override fun discoverServices() {
            listener?.onServicesDiscovered(true)
        }

        override fun findCharacteristics(): Boolean = true
        override fun enableNotifications() {
            listener?.onDescriptorWritten()
        }

        override fun writeCharacteristic(data: ByteArray) {
            writes += data
            listener?.onCharacteristicWritten()
            emulator.handle(data).forEach { listener?.onCharacteristicChanged(it) }
        }
    }

    private val _pairingState = MutableStateFlow(PairingState())
    override val pairingState: StateFlow<PairingState> = _pairingState
    override fun updatePairingState(state: PairingState) {
        _pairingState.value = state
    }

    override fun setListener(listener: BleTransportListener?) {
        this.listener = listener
    }

    private fun deviceName() = "twiist-EMU-${emulator.state.serialNumber}"
    private fun macAddressString() = emulator.state.macAddress.joinToString(":") { "%02X".format(it) }
}
