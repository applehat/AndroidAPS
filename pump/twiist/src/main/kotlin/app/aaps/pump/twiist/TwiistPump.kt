package app.aaps.pump.twiist

import app.aaps.core.data.pump.defs.PumpType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Driver-side view of the pump. Every value here must come from the pump itself (status reads),
 * never from what the driver *asked* for. Nothing is persisted yet.
 */
@SingleIn(AppScope::class)
class TwiistPump @Inject constructor() {

    val pumpType: PumpType = PumpType.SEQUEL_TWIIST

    var serialNumber: String = ""
    var baseBasalRate: Double = 0.0
    var suspended: Boolean = true

    private val _lastDataTime = MutableStateFlow(0L)
    val lastDataTime: StateFlow<Long> = _lastDataTime.asStateFlow()

    private val _lastBolusTime = MutableStateFlow<Long?>(null)
    val lastBolusTime: StateFlow<Long?> = _lastBolusTime.asStateFlow()

    private val _lastBolusAmount = MutableStateFlow<Double?>(null)
    val lastBolusAmount: StateFlow<Double?> = _lastBolusAmount.asStateFlow()

    private val _reservoir = MutableStateFlow(0.0)
    val reservoir: StateFlow<Double> = _reservoir.asStateFlow()

    private val _battery = MutableStateFlow<Int?>(null)
    val battery: StateFlow<Int?> = _battery.asStateFlow()

    fun updateFromStatus(time: Long, reservoirUnits: Double, batteryPercent: Int, isSuspended: Boolean) {
        _reservoir.value = reservoirUnits
        _battery.value = batteryPercent
        suspended = isSuspended
        _lastDataTime.value = time
    }
}
