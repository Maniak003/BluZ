package ru.starline.bluz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceUiState(
    val bleStatus: BleStatus = BleStatus.Disconnected,
    val totalPulses: UInt = 0u,
    val pulsesPerSec: UInt = 0u,
    val avgCps: Float = 0f,
    val measurementTime: UInt = 0u,
    val temperature: Float = 0f,
    val batteryVoltage: Float = 0f,
    val overload: Boolean = false,
    val frameType: Int = 0,
    val hw: HardwareConfig? = null,
    val dosimeterData: DoubleArray = DoubleArray(512),
    val logEntries: List<LogEntry> = emptyList(),
    val spectrumData: DoubleArray? = null,
    val historyData: DoubleArray? = null,
) {
    // DoubleArray doesn't participate in data class equals/hashCode —
    // override to prevent spurious recompositions if we ever use Compose.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceUiState) return false
        return bleStatus == other.bleStatus &&
                totalPulses == other.totalPulses &&
                pulsesPerSec == other.pulsesPerSec &&
                avgCps == other.avgCps &&
                measurementTime == other.measurementTime &&
                temperature == other.temperature &&
                batteryVoltage == other.batteryVoltage &&
                overload == other.overload &&
                frameType == other.frameType &&
                hw == other.hw &&
                dosimeterData.contentEquals(other.dosimeterData) &&
                logEntries == other.logEntries &&
                (spectrumData == null) == (other.spectrumData == null) &&
                (historyData == null) == (other.historyData == null)
    }

    override fun hashCode(): Int = bleStatus.hashCode()
}

@OptIn(ExperimentalUnsignedTypes::class)
class DeviceViewModel : ViewModel() {

    private val _state = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = _state.asStateFlow()

    fun observeBle(btt: BluetoothInterface) {
        viewModelScope.launch {
            btt.status.collect { status ->
                _state.value = _state.value.copy(bleStatus = status)
            }
        }
        viewModelScope.launch {
            btt.deviceFrames.collect { frame ->
                _state.value = DeviceUiState(
                    bleStatus     = _state.value.bleStatus,
                    totalPulses   = frame.totalPulses,
                    pulsesPerSec  = frame.pulsesPerSec,
                    avgCps        = frame.avgCps,
                    measurementTime = frame.measurementTime,
                    temperature   = frame.temperature,
                    batteryVoltage = frame.batteryVoltage,
                    overload      = frame.overload,
                    frameType     = frame.frameType,
                    hw            = frame.hw,
                    dosimeterData = frame.dosimeterData,
                    logEntries    = frame.logEntries,
                    spectrumData  = frame.spectrumData,
                    historyData   = frame.historyData,
                )
            }
        }
    }
}
