package ru.starline.bluz

/**
 * Завершённый и проверенный BLE-фрейм от прибора BluZ.
 *
 * Собирается из MTU-чанков в [BluetoothInterface.processIncomingPacket], эмитится
 * в [BluetoothInterface.deviceFrames]. Подписчик ([MainActivity]) копирует поля в [globalObj]
 * через `applyFrameToState` и обновляет UI через `applyFrameToUi`.
 *
 * **Размер фрейма** зависит от [frameType] и разрешения спектра. Header — 58 байт,
 * далее dosimeterData (512 байт), log (250 байт), spectrum/history (resolution × 2 байт), CRC (2 байта).
 *
 * @property frameType 0..6 — тип фрейма (см. таблицу в DEVELOPER_GUIDE.md раздел 5.2):
 *  - 0 = только дозиметр + лог
 *  - 1..3 = + spectrum 1024/2048/4096
 *  - 4..6 = + history 1024/2048/4096
 * @property totalPulses Всего импульсов с начала измерения (uint32).
 * @property pulsesPerSec Импульсов в последнюю секунду (uint32) — то, что подписчик кладёт в `GO.pulsePerSec`.
 * @property avgCps Скользящее среднее CPS (float, считается прошивкой).
 * @property measurementTime Время измерения, секунды.
 * @property temperature Температура SiPM, °C.
 * @property batteryVoltage Напряжение АКБ, В.
 * @property dosimeterData 512 значений гистограммы дозиметра (CPS по интервалам).
 * @property logEntries До 50 [LogEntry] с прибора (timestamp + код действия).
 * @property spectrumData Спектр (1024/2048/4096 каналов) для [frameType] 1..3, иначе null.
 * @property historyData Историчный спектр для [frameType] 4..6, иначе null.
 * @property hw Распарсенная [HardwareConfig] из шапки фрейма.
 * @property overload Флаг перегрузки прибора (превышен макс. предел измерения).
 */
data class DeviceFrame(
    val frameType: Int,
    val totalPulses: UInt,
    val pulsesPerSec: UInt,
    val avgCps: Float,
    val measurementTime: UInt,
    val temperature: Float,
    val batteryVoltage: Float,
    val dosimeterData: DoubleArray,
    val logEntries: List<LogEntry>,
    val spectrumData: DoubleArray?,
    val historyData: DoubleArray?,
    val hw: HardwareConfig,
    val overload: Boolean
)

/**
 * Одна запись из аппаратного лога прибора.
 *
 * @property timestamp Относительное время (секунды от какого-то момента старта прибора).
 *  При отображении в UI переводится в абсолютное через `unixNow - (messTm - tm) * 1000`.
 * @property action Код действия (1=Turn on, 2..5=Level changes, 6=Dosimeter reset, ...).
 *  Полная таблица в `drawLogs.updateLogs`.
 */
data class LogEntry(val timestamp: UInt, val action: UByte)

/**
 * Аппаратная конфигурация прибора, распакованная из шапки [DeviceFrame].
 *
 * Подписчик копирует все поля в `GO.HW*` ([globalObj]) — это то, что прибор сейчас имеет.
 * Поля `GO.prop*` — то, что хочет иметь пользователь (UI настройки), записываются в прибор
 * при нажатии Write в Settings.
 *
 * @property ledKvant Подсветка каждого зарегистрированного кванта (импульса).
 * @property soundKvant Озвучка каждого кванта.
 * @property soundLevel1 Включён звук при превышении порога L1 ([level1]).
 * @property soundLevel2 То же для L2.
 * @property soundLevel3 То же для L3.
 * @property vibroLevel1 Вибросигнал на L1.
 * @property vibroLevel2 На L2.
 * @property vibroLevel3 На L3.
 * @property autoStartSpectrometer Автозапуск спектрометра при включении прибора.
 * @property click10 Делитель «звук кванта /10» — звучит каждый 10-й.
 * @property led10 Делитель «LED кванта /10».
 * @property level1 Порог тревоги L1, CPS.
 * @property level2 Порог тревоги L2, CPS.
 * @property level3 Порог тревоги L3, CPS.
 * @property cps2ur Коэффициент перевода CPS → мкР/ч (зависит от детектора).
 * @property hVoltage Высокое напряжение SiPM (10-битное значение АЦП, 0..1023).
 * @property comparator Порог компаратора АЦП (10-битное, 0..1023). Используется для
 *  расчёта первого «живого» канала в [globalObj.updateSpecSubtitle]:
 *  `firstCh = comparator / bitsChannel`.
 * @property coef1024A Коэф полинома `A·ch² + B·ch + C` (энергия в кэВ) для разрешения 1024.
 * @property coef1024B B-коэф для 1024.
 * @property coef1024C C-коэф для 1024.
 * @property coef2048A A-коэф для 2048.
 * @property coef2048B B-коэф для 2048.
 * @property coef2048C C-коэф для 2048.
 * @property coef4096A A-коэф для 4096.
 * @property coef4096B B-коэф для 4096.
 * @property coef4096C C-коэф для 4096.
 * @property acquireValue Точность измерения дозиметра (unsigned 16-bit, 0..65535).
 * @property bitsChan Бит/отсчётов АЦП на 1 канал спектра. Дефолт 20, диапазон 16..32.
 * @property sampleTime Время выборки АЦП (3 бита).
 * @property spectrometerTime Время работы спектрометра, секунд.
 * @property spectrometerPulse Импульсов, набранных спектрометром.
 */
data class HardwareConfig(
    val ledKvant: Boolean,
    val soundKvant: Boolean,
    val soundLevel1: Boolean,
    val soundLevel2: Boolean,
    val soundLevel3: Boolean,
    val vibroLevel1: Boolean,
    val vibroLevel2: Boolean,
    val vibroLevel3: Boolean,
    val autoStartSpectrometer: Boolean,
    val click10: Boolean,
    val led10: Boolean,
    val level1: Int,
    val level2: Int,
    val level3: Int,
    val cps2ur: Float,
    val hVoltage: UShort,
    val comparator: UShort,
    //val coef1024A: Float, val coef1024B: Float, val coef1024C: Float,
    //val coef2048A: Float, val coef2048B: Float, val coef2048C: Float,
    val coef4096A: Float, val coef4096B: Float, val coef4096C: Float, val coef4096D: Float, val coef4096E: Float,
    val acquireValue: UShort,
    val bitsChan: UByte,
    val sampleTime: UByte,
    val spectrometerTime: UInt,
    val spectrometerPulse: UInt
)

/**
 * Состояние BLE-соединения с прибором.
 *
 * Транслируется через [BluetoothInterface.status] как [kotlinx.coroutines.flow.StateFlow].
 * UI-слой подписывается и обновляет иконку BT в StatusStrip.
 */
sealed class BleStatus {
    /** Идёт попытка подключения (после [BluetoothInterface.initLeDevice]). */
    object Connecting : BleStatus()
    /** Соединение установлено, MTU согласован, сервисы дискаверены, notify подписан. */
    object Connected : BleStatus()
    /** Соединения нет. Стартовое состояние и состояние после destroyDevice/disconnect. */
    object Disconnected : BleStatus()
    /** Ошибка с описанием. Не используется как «нормальное» состояние. */
    data class Error(val message: String) : BleStatus()
}
