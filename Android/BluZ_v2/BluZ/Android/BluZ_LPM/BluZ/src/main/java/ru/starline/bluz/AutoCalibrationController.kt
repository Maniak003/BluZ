package ru.starline.bluz

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.starline.bluz.GO
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Корутинная машина состояний автокалибровки спектрометра по источнику Ra-226.
 *
 * Оркестрирует фазы взаимодействия с прибором поверх чистой расчётной логики
 * [AutoCalibrator]. Не имеет UI: публикует состояние через [state] (StateFlow), а UI
 * (диалоги в `SettingsFragment`) подписывается и отображает прогресс. Отмена через
 * [cancel] прерывает процесс в любой точке.
 *
 * ## Режимы
 *
 * | Режим                       | Компаратор | ВВ        | Полином    | Имп. всего |
 * |-----------------------------|------------|-----------|------------|-----------:|
 * | [Mode.PRIMARY]              | подбор + финальная подгонка | bracket + refine | пересчёт   | ~2.5 M  |
 * | [Mode.HV_AND_COMPARATOR]    | подбор + финальная подгонка | bracket          | без правок | ~1.0 M  |
 * | [Mode.POLYNOMIAL_ONLY]      | без правок | без правок | пересчёт   | ~1.5 M  |
 *
 * Дополнительно есть отдельный режим [startComparatorOnly] для standalone-подбора
 * компаратора (вызывается отдельной кнопкой, не через диалог режимов).
 *
 * ## Поток выполнения PRIMARY
 *
 * ```
 *  start(PRIMARY)
 *    │
 *    ├─► Спектрометр запущен? Если нет — cmd_startup_spectrometer
 *    ├─► Сброс полинома к 1:1
 *    │
 *    ├─► requestUserAction(RemoveSourceForCompTuning)
 *    ├─► Phase 1: tuneComparatorByIteration (V=100, шум-критерии)
 *    │
 *    ├─► requestUserAction(PlaceSourceForCalibration)
 *    ├─► Phase 2: tuneHvByBracket500and100 (V=500/100 × 500к имп., линейная интерполяция)
 *    │
 *    ├─► Phase 3 ×REFINE_ITERATIONS:
 *    │     ├─ applyHv → accumulateByCounts(500к)
 *    │     ├─ AutoCalibrator.analyze → полином
 *    │     └─ корректировка V через локальную sensitivity и target_ch с учётом C
 *    │
 *    ├─► requestUserAction(RemoveSourceForCompTuning)
 *    ├─► Phase 4: tuneComparatorAtCurrentHv при финальном V (без forced V=100)
 *    │
 *    └─► Phase.AWAITING_APPLY: pendingResult готов
 *          │
 *          └─ applyPending() → sendFullConfigToDevice → Phase.DONE
 * ```
 *
 * [Mode.HV_AND_COMPARATOR] идентичен PRIMARY кроме пропуска Phase 3 (полином не пересчитывается,
 * в pendingResult кладутся исходные коэффициенты). [Mode.POLYNOMIAL_ONLY] пропускает
 * Phase 1, 2, 4 (только Phase 3 после запроса источника).
 *
 * ## Управление прибором (BLE-команды)
 *
 *  - `cmd_setup` (0u) — отправляет весь конфиг (propHVoltage, propComparator, propCoef*).
 *    Прошивка применяет ВВ и компаратор сразу через `LTC1662.setLevelOnPort` без
 *    перезагрузки. Используется через [sendFullConfigToDevice].
 *  - `cmd_clear_specter` (1u) — очистить буфер спектра в приборе. Очистка имеет
 *    задержку ~1 сек до прихода обновлённого фрейма; см. [clearAndAccumulateByTime]
 *    и [accumulateByCounts] которые ждут реального опустошения.
 *  - `cmd_startup_spectrometer` (2u) — toggle старт/стоп. Шлём только если
 *    `!GO.specterRunning`.
 *  - `cmd_find_device` (6u) — звук/вибро на приборе. Шлём перед каждым
 *    [UserPrompt] чтобы привлечь внимание пользователя.
 *
 * Подтверждение применения настроек делается через read-back: после `cmd_setup`
 * ждём фрейм с `frame.hw.hVoltage == target` (или `frame.hw.comparator == target`)
 * с retry (5 сек → повторный `cmd_setup` → 10 сек). См. [writeAndConfirmHv],
 * [writeAndConfirmComp].
 *
 * ## ВВ работает «наоборот»
 *
 * Чем **меньше** значение `propHVoltage` (DAC), тем **выше** реальное напряжение на SiPM
 * и тем больше gain. Чтобы сдвинуть пик правее (увеличить усиление) — нужно **уменьшить**
 * `propHVoltage`. Нижняя граница [MIN_HV_DAC] = 100, верхняя [MAX_HV_DAC] = 1023.
 *
 * ## Threading и cancellation
 *
 *  - Процедура запускается в `scope.launch`, поэтому отменяется по `scope.cancel()` или
 *    [cancel]. Все `suspend` функции в коде кооперативны (delay, withTimeoutOrNull, first()).
 *  - [state] — `MutableStateFlow`, безопасен для чтения с любого потока.
 *  - Внутри обрабатываем `CancellationException` отдельно от прочих — чтобы
 *    переход в Phase.IDLE с message «Отменено» произошёл до проброса исключения.
 *
 * ## Stabilization SiPM
 *
 * После смены ВВ держим паузу [HV_STABILIZE_MS] = 5 сек. SiPM (~30 В) стабилизируется
 * быстрее PMT, но детектору всё равно нужно время на термализацию.
 */
class AutoCalibrationController(
    private val scope: CoroutineScope,
) {

    /** Режим автокалибровки, выбирается пользователем в диалоге. */
    enum class Mode {
        /** Полная процедура — компаратор + bracket scan ВВ + 3 итерации refine с записью
         *  полинома. Суммарный набор импульсов ~2.5М. */
        PRIMARY,

        /** Только ВВ и компаратор: компаратор + bracket scan ВВ, итераций refine нет,
         *  полином в финале **не перезаписывается** (остаётся прежний). Суммарный
         *  набор ~1М импульсов. */
        HV_AND_COMPARATOR,

        /** ВВ и компаратор не трогаем, только пересчитываем полином через 3 итерации refine.
         *  Суммарный набор ~1.5М импульсов. */
        POLYNOMIAL_ONLY,
    }

    /** Идентификатор фазы — для отображения в UI (в шапке прогресс-диалога). */
    enum class Phase {
        /** Начальное состояние или после ошибки/отмены. */
        IDLE,
        /** Фаза 2: bracket scan ВВ при V=500 и V=100. */
        HV_TUNING,
        /** Фаза 1 или 4: итеративный подбор уровня компаратора без источника. */
        COMPARATOR_TUNING,
        /** Накопление импульсов перед анализом (фаза 3 refine или внутри bracket scan). */
        ACCUMULATING,
        /** Запущен `AutoCalibrator.analyze` — поиск пиков и решение системы для полинома. */
        ANALYZING,
        /** Опциональная проверка фона после применения настроек (резерв на будущее). */
        BACKGROUND_CHECK,
        /** Процедура успешно завершена, [PendingResult] заполнен, ждём `applyPending` от UI. */
        AWAITING_APPLY,
        /** `applyPending` в процессе — пишем настройки в прибор. */
        APPLYING,
        /** Запись завершена, прибор откалиброван. UI может закрыть диалог. */
        DONE,
    }

    /** Состояние процесса для UI. Immutable — публикуется через [state] (StateFlow). */
    data class State(
        val phase: Phase = Phase.IDLE,
        val mode: Mode = Mode.PRIMARY,
        /** Текущее сообщение для пользователя (на русском, краткое). */
        val message: String = "",
        /** Прогресс текущей фазы 0..100, или null если нерелевантно. */
        val progressPercent: Int? = null,
        /** Промежуточные значения для отображения в шапке прогресс-диалога. */
        val currentHv: Int = 0,
        val currentComparator: Int = 0,
        /** Итерация в фазах подбора (1..N). */
        val iteration: Int = 0,
        /** Финальный результат — заполняется на фазе AWAITING_APPLY. */
        val pendingResult: PendingResult? = null,
        /** Описание ошибки если что-то пошло не так. null = всё ок. */
        val failure: String? = null,
        /** Запрос диалога к пользователю (убрать источник). null = не нужно. */
        val userPrompt: UserPrompt? = null,
    )

    /** Финальный пакет настроек, который будет применён по подтверждению пользователя. */
    data class PendingResult(
        val oldHv: Int, val newHv: Int,
        val oldComparator: Int, val newComparator: Int,
        val cA: Float, val cB: Float, val cC: Float, val cD: Float,  val cE: Float,
        val peaks: List<AutoCalibrator.IdentifiedPeak>,
        val residualKev: Float,
        /** Энергия последнего канала спектра (channel = N-1) по итоговому полиному.
         *  Должна быть близка к [AutoCalibrator.TARGET_FULL_RANGE_KEV] (~3500 кэВ). */
        val eAtLastChannel: Float,
    )

    /** Запрос от контроллера к UI — например, попросить пользователя убрать источник. */
    sealed class UserPrompt {
        /** Просьба убрать источник Ra-226 перед подбором компаратора. Без источника
         *  шумовая стенка от компаратора видна гарантированно (естественный фон не
         *  мешает), и её правый край легко найти. */
        object RemoveSourceForCompTuning : UserPrompt()

        /** Просьба вернуть источник Ra-226 на место для дальнейших фаз (подбор ВВ,
         *  длинное накопление, расчёт полинома). */
        object PlaceSourceForCalibration : UserPrompt()

        /** Просьба убрать источник Ra-226 для финальной проверки шума компаратора. */
        object RemoveSourceForBackgroundCheck : UserPrompt()

        /** Первая просьба поднести источник — для режима POLYNOMIAL_ONLY, где
         *  предыдущие фазы (убирание источника для компаратора) пропущены. */
        object PlaceSourceForPolynomOnly : UserPrompt()
    }

    private val _state = MutableStateFlow(State())
    /** Подписка для UI на состояние машины. */
    val state: StateFlow<State> = _state

    private var job: Job? = null

    /** Готовность пользователя продолжить после ответа на [UserPrompt]. */
    private var userPromptResolved: Boolean = false

    // ───────────────────────────── Константы алгоритма ─────────────────────────────

    /**
     * Минимально допустимое значение ВВ DAC.
     *
     * ВВ инвертирован: меньше DAC → выше реальное напряжение на SiPM → больше gain.
     * Значение 100 — нижняя «безопасная» граница для прибора. Дальше идти нельзя
     * (gain детектора уже максимальный по физике).
     */
    val MIN_HV_DAC: Int = 100

    /** Максимально допустимое значение ВВ DAC (по характеристикам `LTC1662`, 10-битный DAC). */
    val MAX_HV_DAC: Int = 1023

    /**
     * Пауза стабилизации SiPM после смены ВВ, мс.
     *
     * SiPM (~30 В) стабилизируется быстрее PMT, но детектору всё равно нужно время
     * на термализацию после изменения опорного напряжения. 5 секунд — компромисс
     * между точностью и общим временем процедуры.
     */
    val HV_STABILIZE_MS: Long = 5_000L

    /** Максимум итераций подбора компаратора (защита от расходимости). */
    val MAX_COMP_ITERATIONS: Int = 20

    /**
     * Количество итераций фазы Refine (накопление → полином → корректировка V).
     *
     * 3 итерации эмпирически дают сходимость даже при значительной нелинейности
     * SiPM-усиления. Каждая итерация ≈3 мин на активном источнике (500к импульсов).
     */
    val REFINE_ITERATIONS: Int = 3

    /**
     * Целевое число импульсов в спектре для накопления в фазах 2 (bracket scan) и 3 (refine).
     *
     * Большое значение нужно для надёжной идентификации пика 609 кэВ — на маленьком
     * сцинтилляторе он в 5–20 раз слабее пика 82 кэВ и в коротких накоплениях тонет
     * в фоне. Активный источник Ra-226 даёт ~2500 cps → 500к импульсов набираются
     * за ~3 мин; слабый бытовой источник может потребовать ~10 мин.
     */
    val ACC_REFINE_IMPULSES: Int = 500_000

    /** Таймаут на одно накопление, мс. Защита от очень слабого источника (10 минут). */
    val ACC_TIMEOUT_MS: Long = 10 * 60_000L

    /**
     * Нижняя точка ВВ для bracket scan фазы 2.
     *
     * 100 — максимум реального усиления; пик 609 кэВ сдвинется максимально вправо
     * по шкале каналов. Используется в паре с [HV_BRACKET_HIGH] для линейной
     * интерполяции к целевому каналу.
     */
    val HV_BRACKET_LOW: Int = 100

    /**
     * Верхняя точка ВВ для bracket scan фазы 2.
     *
     * 500 — комфортный «полу-вверх» диапазона: ниже усиление, спектр сжат влево.
     * Не используем экстремум (MAX_HV_DAC=1023) потому что около 1023 чувствительность
     * пика к ВВ нелинейна и интерполяция к финальному V плохо работает.
     */
    val HV_BRACKET_HIGH: Int = 500

    /**
     * Базовый шаг изменения уровня компаратора в каналах спектра — зависит от разрешения.
     *
     * Значение интерпретируется как «прирост позиции порога компаратора в каналах
     * спектра на одну итерацию подбора». Маленький шаг (3/5/10 для 1024/2048/4096)
     * даёт точное попадание у границы: больший шаг одним прыжком уводит компаратор
     * в зону «отсекает всё» — после чего ВВ перестаёт влиять на масштаб спектра.
     *
     * В алгоритме [tuneComparatorAtCurrentHv] этот шаг масштабируется адаптивно
     * (×4/×2/×1) в зависимости от текущего значения ratio шум/плато — быстро
     * проходим зону массивной иголки и точно попадаем у границы сходимости.
     */
    private fun comparatorStepInSpectrumChannels(channels: Int): Int = when (channels) {
        4096 -> 10
        2048 -> 5
        else -> 3
    }

    /**
     * Шаг компаратора в единицах АЦП (== единицах DAC компаратора `LTC1662`).
     *
     * Связь канала спектра и значения АЦП: `канал = ADC / bitsChannel`, поэтому
     * `ΔADC = Δканал × bitsChannel`. Значение `bitsChannel` берётся из `GO`, при
     * нештатной величине используется дефолт 20.
     */
    private fun comparatorStepInAdc(channels: Int): Int {
        val bitsCh = if (GO.bitsChannel in 1..64) GO.bitsChannel else 20
        return comparatorStepInSpectrumChannels(channels) * bitsCh
    }

    /** Текущее количество каналов спектра по `GO.spectrResolution` (0=1024, 1=2048, 2=4096). */
    private fun currentChannels(): Int = when (GO.spectrResolution) {
        1 -> 2048
        2 -> 4096
        else -> 1024
    }

    // ───────────────────────────── Public API ─────────────────────────────

    /**
     * Запускает полную процедуру автокалибровки в выбранном [mode].
     *
     * Идемпотентна: если процедура уже запущена ([job] активна) — возврат без действий.
     *
     * Жизненный цикл:
     *  1. Сразу публикует `Phase.IDLE` с текущими ВВ/comp в `_state`.
     *  2. Запускает корутину `scope.launch { runProcedure(mode) }`.
     *  3. По исключению — обновляет `_state.failure` (для UI) и пишет в логи.
     *     CancellationException пробрасывается дальше согласно правилам структурированной
     *     многозадачности; ошибки приложения превращаются в человеко-читаемое сообщение.
     *
     * Никак не блокирует вызывающий поток. UI должен подписаться на [state] до или после
     * вызова — `MutableStateFlow` гарантирует доставку последнего значения.
     */
    fun start(mode: Mode) {
        if (job?.isActive == true) return
        _state.value = State(phase = Phase.IDLE, mode = mode,
            currentHv = GO.propHVoltage.toInt(), currentComparator = GO.propComparator.toInt())
        job = scope.launch {
            try {
                runProcedure(mode)
            } catch (ce: CancellationException) {
                _state.value = _state.value.copy(phase = Phase.IDLE,
                    message = "Отменено пользователем", failure = null)
                throw ce
            } catch (t: Throwable) {
                Log.e("BluZ-AutoCalib", "Procedure failed", t)
                _state.value = _state.value.copy(phase = Phase.IDLE,
                    failure = "Ошибка: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    /**
     * Подтверждает что пользователь обработал текущий [UserPrompt] (нажал «Готово»).
     *
     * Используется в паре с [requestUserAction]: корутина процедуры висит в `while (!resolved)
     * delay(200)`, пока эта функция не выставит флаг. UI обязан вызвать её ровно один раз
     * за один prompt; повторные вызовы безвредны (флаг уже true).
     */
    fun resolveUserPrompt() {
        userPromptResolved = true
    }

    /**
     * Применяет финальный [PendingResult] в прибор.
     *
     * Вызывается из UI после нажатия «Применить» в диалоге результата. Не блокирует UI:
     * фактическая запись делается в `scope.launch`.
     *
     * Side effects:
     *  - Перезаписывает `GO.propHVoltage` / `GO.propComparator` / `GO.propCoef{1024|2048|4096}*`
     *    (в зависимости от текущего разрешения)
     *  - Записывает `GO.compNoiseLevel` (для будущих повторных калибровок)
     *  - Сохраняет prop-параметры в SharedPreferences через `GO.writeConfigParameters()`
     *  - Отправляет `cmd_setup` в прибор через `sendFullConfigToDevice()`
     *  - Обновляет `_state` на `Phase.APPLYING` → `Phase.DONE`
     *
     * Если pendingResult == null (фаза AWAITING_APPLY не достигнута) — функция выходит молча.
     */
    fun applyPending() {
        val pending = _state.value.pendingResult ?: return
        scope.launch {
            _state.value = _state.value.copy(phase = Phase.APPLYING, message = "Запись в прибор…")
            GO.propHVoltage = pending.newHv.toUShort()
            GO.propComparator = pending.newComparator.toUShort()
            //writeCurrentResolutionCoefs(pending.cA, pending.cB, pending.cC)
            GO.propCoef4096A = pending.cA
            GO.propCoef4096B = pending.cB
            GO.propCoef4096C = pending.cC
            GO.propCoef4096D = pending.cD
            GO.propCoef4096E = pending.cE
            GO.compNoiseLevel = pending.newComparator
            GO.writeConfigParameters()
            sendFullConfigToDevice()
            _state.value = _state.value.copy(phase = Phase.DONE,
                message = "Готово. Прибор откалиброван.", progressPercent = 100)
        }
    }

    /**
     * Прерывает текущую процедуру.
     *
     * Корутина [job] получает `CancellationException`, обработка в `start()` переводит
     * [state] в Phase.IDLE с message «Отменено». Безопасно при отсутствии активной job —
     * простое `job?.cancel()` без эффекта.
     *
     * Применяется состояние прибора в момент отмены **не откатывается** — что было записано
     * через `sendFullConfigToDevice()` до отмены, в приборе и останется. Пользователь может
     * восстановить старые настройки через ручной ввод или повторный запуск.
     */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /**
     * Standalone-подбор компаратора **при текущих ВВ и полиноме** — без полного цикла
     * автокалибровки. Используется когда пользователь вручную поменял ВВ или хочет
     * уточнить компаратор после изменения условий: старый компаратор может «съесть»
     * полезные первые каналы спектра при новом ВВ.
     *
     * Процедура:
     *  1. Запрос «уберите источник»
     *  2. `tuneComparatorAtCurrentHv` — итеративное повышение comp пока иголка не пропадёт
     *  3. Готов pendingResult с одним только новым компаратором — `applyPending` запишет
     */
    fun startComparatorOnly() {
        if (job?.isActive == true) return
        val initialHv = GO.propHVoltage.toInt()
        val initialComp = GO.propComparator.toInt()
        _state.value = State(phase = Phase.IDLE, mode = Mode.PRIMARY,
            currentHv = initialHv, currentComparator = initialComp)
        job = scope.launch {
            try {
                val channels = currentChannels()
                if (!GO.specterRunning) {
                    GO.BTT.sendCommand(2u)
                    GO.specterRunning = true
                    delay(1_500L)
                }
                // Без UserPrompt + cmd_find_device — пользователь уже подтвердил отсутствие
                // источника в стартовом диалоге standalone-подбора. Сразу начинаем подбор.
                val newComp = tuneComparatorAtCurrentHv(channels)
                // pendingResult с теми же значениями HV и полинома — applyPending запишет
                // только новый компаратор, остальное останется как было.
                val powVal = when (GO.spectrResolution) {
                    1 -> 2.0    // 2048
                    2 -> 1.0    // 4096
                    else -> 4.0 // 1024
                }
                val eLast = (GO.propCoef4096A * (channels * powVal - 1).toDouble().pow(4.0)
                    + GO.propCoef4096B * (channels * powVal - 1).toDouble().pow(3.0)
                    + GO.propCoef4096C *(channels * powVal - 1).toDouble().pow(2.0)
                    + GO.propCoef4096D *(channels * powVal - 1).toDouble()
                    + GO.propCoef4096E).toFloat()
                _state.value = _state.value.copy(
                    phase = Phase.AWAITING_APPLY,
                    message = "Готово. Нажмите «Применить» чтобы записать новый компаратор.",
                    progressPercent = 100,
                    currentHv = initialHv, currentComparator = newComp,
                    pendingResult = PendingResult(
                        oldHv = initialHv, newHv = initialHv,
                        oldComparator = initialComp, newComparator = newComp,
                        cA = GO.propCoef4096A, cB = GO.propCoef4096B, cC = GO.propCoef4096C, cD = GO.propCoef4096D, cE =GO.propCoef4096E,
                        peaks = emptyList(), residualKev = 0f,
                        eAtLastChannel = eLast,
                    ),
                )
            } catch (ce: CancellationException) {
                _state.value = _state.value.copy(phase = Phase.IDLE,
                    message = "Отменено", failure = null)
                throw ce
            } catch (t: Throwable) {
                Log.e("BluZ-AutoCalib", "Comparator-only failed", t)
                _state.value = _state.value.copy(phase = Phase.IDLE,
                    failure = "Ошибка: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    /** Текущие коэффициенты полинома для активного разрешения. */
    /*
    private fun currentResolutionCoefs(): Triple<Float, Float, Float> = when (GO.spectrResolution) {
        1 -> Triple(GO.propCoef2048A, GO.propCoef2048B, GO.propCoef2048C)
        2 -> Triple(GO.propCoef4096A, GO.propCoef4096B, GO.propCoef4096C)
        else -> Triple(GO.propCoef1024A, GO.propCoef1024B, GO.propCoef1024C)
    }*/

    // ───────────────────────────── Procedure ─────────────────────────────

    /**
     * Главная корутина автокалибровки. Выполняет последовательность фаз в зависимости
     * от [mode]; публикует промежуточные состояния через [_state] и блокируется в местах
     * запроса действий пользователя (см. [requestUserAction]).
     *
     * ### Конечный автомат фаз
     *
     * | Фаза                          | PRIMARY | HV_AND_COMPARATOR | POLYNOMIAL_ONLY |
     * |-------------------------------|:-------:|:-----------------:|:---------------:|
     * | 1. Подбор comp при V=100      |   ✓     |        ✓          |       —         |
     * | 2. Bracket scan V=500/100     |   ✓     |        ✓          |       —         |
     * | 3. Refine V + полином (×N)    |   ✓     |        — (N=0)    |       ✓ (1 раз) |
     * | 4. Финальная подгонка comp    |   ✓     |        ✓          |       —         |
     *
     * ### Ключевые инварианты
     *
     *  - В `HV_AND_COMPARATOR` исходные коэффициенты полинома (`initCA/B/C`) сохраняются
     *    в начале и возвращаются в `PendingResult` в конце — `applyPending` запишет
     *    ровно тот же полином, что был до калибровки.
     *  - В `POLYNOMIAL_ONLY` ВВ и компаратор НЕ меняются: `finalHv = initialHv`,
     *    `finalComp = initialComp`; фазы 1, 2 и 4 пропускаются.
     *  - На фазе 3 локальная sensitivity `Δch/ΔV` пересчитывается по двум последним
     *    точкам, а дельта V клампится на `|finalHv - prevV|` — нелинейность кривой
     *    V↔ch вокруг рабочей точки делает глобальную sensitivity ненадёжной.
     *  - Полином 1:1 (`A=0, B=1, C=0`) явно прописывается на старте всех режимов кроме
     *    `HV_AND_COMPARATOR` — иначе старые коэффициенты будут мешать поиску якорных
     *    каналов на шаге `AutoCalibrator.analyze`.
     *
     * ### Завершение
     *
     * Не пишет напрямую в прибор итоговые значения — формирует [PendingResult] и переводит
     * фазу в [Phase.AWAITING_APPLY]. Запись делает [applyPending] после подтверждения
     * пользователем результата в UI.
     *
     * Исключения внутри ([AutoCalibrator.Result.WeakSignal], [AutoCalibrator.Result.NoRa226Pattern],
     * [AutoCalibrator.Result.UnreasonableFit]) пробрасываются наверх в [start] как
     * [IllegalStateException] и попадают в `state.failure`.
     */
    private suspend fun runProcedure(mode: Mode) {
        val channels = currentChannels()
        val initialHv = GO.propHVoltage.toInt()
        val initialComp = GO.propComparator.toInt()
        // Сохраняем исходные коэффициенты полинома — пригодятся для HV_AND_COMPARATOR
        // (там полином не перезаписывается, итоговый pendingResult должен вернуть
        // именно исходные коэффициенты).
        //val (initCA, initCB, initCC) = currentResolutionCoefs()
        val initCA = GO.propCoef4096A
        val initCB = GO.propCoef4096B
        val initCC = GO.propCoef4096C
        val initCD = GO.propCoef4096D
        val initCE = GO.propCoef4096E
        Log.i("BluZ-AutoCalib", "=== START procedure mode=$mode channels=$channels initialHv=$initialHv initialComp=$initialComp ===")

        // Убедиться что спектрометр запущен. cmd_startup_spectrometer (2u) — toggle,
        // поэтому шлём только если флаг говорит «не запущен». Без активного спектрометра
        // буфер spectrData будет нулевой, фаза 1 ВВ не найдёт ни одного пика и упадёт.
        if (!GO.specterRunning) {
            Log.i("BluZ-AutoCalib", "Spectrometer not running, sending cmd_startup_spectrometer (2u)")
            GO.BTT.sendCommand(2u)
            GO.specterRunning = true
            delay(1_500L)  // дать прибору применить старт и начать заполнять буфер
        }

        // Сброс полинома к 1:1 — только для режимов где мы его пересчитываем.
        // В HV_AND_COMPARATOR полином остаётся как был.
        if (mode != Mode.HV_AND_COMPARATOR) {
            //writeCurrentResolutionCoefs(0.0f, 1.0f, 0.0f)
            GO.propCoef4096A = 0.0f
            GO.propCoef4096B = 0.0f
            GO.propCoef4096C = 0.0f
            GO.propCoef4096D = 1.0f
            GO.propCoef4096E = 0.0f

            Log.i("BluZ-AutoCalib", "Polynom reset to 1:1 (A=0 B=1 C=0)")
        }

        var finalHv = initialHv
        var finalComp = initialComp
        var bracketPrevV: Int? = null
        var bracketPrevCh: Double? = null

        if (mode != Mode.POLYNOMIAL_ONLY) {
            // ─── ФАЗА 1: подбор компаратора БЕЗ источника при V=100 ───
            requestUserAction(UserPrompt.RemoveSourceForCompTuning,
                "Уберите источник Ra-226 от прибора. Будет подобран уровень компаратора " +
                "при максимальном усилении (V=100).")
            finalComp = tuneComparatorByIteration(channels)

            // ─── ФАЗА 2: bracket scan ВВ при HV=500 и HV=100 ───
            requestUserAction(UserPrompt.PlaceSourceForCalibration,
                "Поднесите источник Ra-226 к прибору и зафиксируйте до конца процедуры. " +
                "Будет подобрано высокое напряжение по пику 609 кэВ.")
            val bracketRes = tuneHvByBracket500and100(channels)
            finalHv = bracketRes.tunedV
            // Используем ближайшую bracket-точку как «prev» для refine 1 — даёт локальную
            // sensitivity на нелинейном участке V↔ch вокруг финального V.
            bracketPrevV = bracketRes.nearestV
            bracketPrevCh = bracketRes.nearestCh
        }

        // В режиме POLYNOMIAL_ONLY мы попадаем сюда напрямую, минуя фазы компаратора и
        // bracket-сканирования. Нужно явно запросить пользователя поднести источник
        // (отдельный prompt, чтобы заголовок был «Поднесите источник», а не «Верните»).
        if (mode == Mode.POLYNOMIAL_ONLY) {
            requestUserAction(UserPrompt.PlaceSourceForPolynomOnly,
                "Поднесите источник Ra-226 к прибору и зафиксируйте до конца процедуры. " +
                "Будет накоплен спектр и пересчитан полином.")
        }

        // ─── ФАЗА 3 (×N): накопление ACC_REFINE_IMPULSES → полином → корректировка V ───
        // Локальная sensitivity вычисляется на каждой итерации по двум последним точкам
        // (V, ch_609) — глобальная sensitivity из bracket scan может сильно отличаться
        // от реальной около финального V (зависимость V↔ch нелинейная).
        //
        // В режиме HV_AND_COMPARATOR refine пропускается — мы не пересчитываем полином.
        var analysis: AutoCalibrator.Result.Ok? = null
        var prevV: Int? = bracketPrevV
        var prevCh609: Double? = bracketPrevCh
        val refineIters = if (mode == Mode.HV_AND_COMPARATOR) 0 else REFINE_ITERATIONS
        for (i in 1..refineIters) {
            _state.value = _state.value.copy(
                phase = Phase.ACCUMULATING,
                message = "Итерация $i/$REFINE_ITERATIONS: накопление до $ACC_REFINE_IMPULSES имп…",
                iteration = i, progressPercent = 0, currentHv = finalHv,
            )
            applyHvAndClearSpectrum(finalHv)
            val spec = accumulateByCounts(ACC_REFINE_IMPULSES, channels) { pct, counts ->
                _state.value = _state.value.copy(progressPercent = pct,
                    message = "Итерация $i: $counts / $ACC_REFINE_IMPULSES имп")
            }
            _state.value = _state.value.copy(phase = Phase.ANALYZING,
                message = "Итерация $i: анализ спектра…", progressPercent = null)
            val result = AutoCalibrator.analyze(spec, channels)
            when (result) {
                is AutoCalibrator.Result.Ok -> {
                    analysis = result
                    //writeCurrentResolutionCoefs(result.cA, result.cB, result.cC)
                    GO.propCoef4096A = result.cA
                    GO.propCoef4096B = result.cB
                    GO.propCoef4096C = result.cC
                    GO.propCoef4096D = result.cD
                    GO.propCoef4096E = result.cE

                    val eLast = GO.enrgCalc.channelToEnergy(channels - 1, GO.spectrResolution)
                    /*val eLast = result.cA * (channels - 1) * (channels - 1) +
                                result.cB * (channels - 1) + result.cC*/
                    Log.i("BluZ-AutoCalib",
                        "Refine $i: A=${result.cA} B=${result.cB} C=${result.cC} D=${result.cD} E=${result.cE}, E_last=${"%.0f".format(eLast)}")

                    if (mode == Mode.POLYNOMIAL_ONLY) break
                    if (i == refineIters) break

                    // Целевой канал для 609 кэВ С УЧЁТОМ текущего C полинома.
                    // Хотим E(channels-1) = TARGET_FULL_RANGE_KEV и E(ch_609) = 609.
                    // Без учёта C формула 609×N/3500 правильна только когда C=0; при
                    // реальном C=-120 (компаратор отсекает первые каналы) пик 609 должен
                    // оказаться в канале (609-C)×(N-1)/(target-C).
                    val targetCh = if (kotlin.math.abs(result.cE) < 5.0f) {
                        AutoCalibrator.targetChannelFor(609, channels)
                    } else {
                        val raw = (609.0 - result.cE) * (channels - 1) /
                                  (AutoCalibrator.TARGET_FULL_RANGE_KEV - result.cE)
                        raw.toInt().coerceIn(1, channels - 1)
                    }
                    Log.i("BluZ-AutoCalib", "Refine $i: effective target ch for 609 keV with C=${result.cC} → $targetCh")
                    val curCh609 = AutoCalibrator.predictChannel(609, result.cA, result.cB, result.cC, result.cD, result.cE)
                    if (curCh609 == null) {
                        Log.w("BluZ-AutoCalib", "Refine $i: cannot predict ch_609, skipping")
                        prevV = finalHv; prevCh609 = null
                        continue
                    }

                    // **Локальная sensitivity**: если есть предыдущая точка (V, ch), считаем
                    // sens = (ch_curr - ch_prev) / (V_curr - V_prev). На граничных условиях
                    // (нет prev, или V не сменился) используем глобальную из bracket scan.
                    val localSens: Float = if (prevV != null && prevCh609 != null && prevV != finalHv) {
                        val s = ((curCh609 - prevCh609!!) / (finalHv - prevV!!)).toFloat()
                        Log.i("BluZ-AutoCalib", "Refine $i: local sensitivity = $s ch/V (vs global ${GO.hvSensitivity})")
                        s
                    } else {
                        GO.hvSensitivity
                    }
                    if (localSens == 0.0f) {
                        Log.w("BluZ-AutoCalib", "Refine $i: sensitivity=0, stopping")
                        break
                    }

                    val deltaCh = targetCh - curCh609
                    var deltaV = (deltaCh / localSens).toInt()
                    // Clamp дельты на ±|разнос предыдущих V| (если есть). Не позволяем
                    // прыгнуть дальше уже проверенного интервала — за ним sensitivity
                    // может оказаться совсем другой, и шаг уйдёт в мусор.
                    if (prevV != null) {
                        val maxDelta = kotlin.math.abs(finalHv - prevV!!)
                        if (kotlin.math.abs(deltaV) > maxDelta) {
                            Log.i("BluZ-AutoCalib", "Refine $i: clamping deltaV $deltaV → ±$maxDelta")
                            deltaV = deltaV.coerceIn(-maxDelta, maxDelta)
                        }
                    }
                    val newV = (finalHv + deltaV).coerceIn(MIN_HV_DAC, MAX_HV_DAC)
                    Log.i("BluZ-AutoCalib",
                        "Refine $i: curCh609=${"%.1f".format(curCh609)} target=$targetCh deltaCh=${"%.1f".format(deltaCh)} sens=$localSens → V $finalHv→$newV (Δ$deltaV)")
                    if (newV == finalHv) {
                        Log.i("BluZ-AutoCalib", "Refine $i: V converged ($finalHv), stopping early")
                        break
                    }
                    prevV = finalHv
                    prevCh609 = curCh609
                    finalHv = newV
                }
                AutoCalibrator.Result.WeakSignal ->
                    throw IllegalStateException("Итерация $i: сигнал от источника слабый. Поднесите источник ближе.")
                AutoCalibrator.Result.NoRa226Pattern ->
                    throw IllegalStateException("Итерация $i: пики Ra-226 не найдены. Проверьте источник.")
                is AutoCalibrator.Result.UnreasonableFit ->
                    throw IllegalStateException("Итерация $i: неправильная идентификация пиков: ${result.reason}")
            }
        }

        // В HV_AND_COMPARATOR и POLYNOMIAL_ONLY analysis может быть null/из 1 итерации.
        // В PRIMARY обязательно должен быть.
        if (mode == Mode.PRIMARY && analysis == null) {
            throw IllegalStateException("Калибровка не сошлась за $REFINE_ITERATIONS итераций.")
        }

        // ─── ФИНАЛЬНАЯ ПОДГОНКА КОМПАРАТОРА при готовом V ───
        // Компаратор был подобран при V=100 (максимум усиления, иголка максимальна).
        // После refine V обычно больше → иголка меньше → текущий компаратор может
        // обрезать полезные первые каналы спектра. Пересматриваем компаратор на финальном V.
        if (mode != Mode.POLYNOMIAL_ONLY) {
            requestUserAction(UserPrompt.RemoveSourceForCompTuning,
                "Уберите источник Ra-226 ещё раз. Будет финальная подгонка компаратора при " +
                "подобранном V=$finalHv — чтобы он не отрезал первые каналы спектра.")
            // НЕ применяем V=100 — оставляем finalHv. Подбираем comp при нём.
            val refinedComp = tuneComparatorAtCurrentHv(channels)
            Log.i("BluZ-AutoCalib", "Final comparator at V=$finalHv: $finalComp → $refinedComp")
            finalComp = refinedComp
        }

        // Полином для финала: новый из analysis (PRIMARY/POLYNOMIAL_ONLY) или исходный
        // (HV_AND_COMPARATOR — там полином не пересчитывался).
        val finalCA: Float; val finalCB: Float; val finalCC: Float; val finalCD: Float; val finalCE: Float
        val finalPeaks: List<AutoCalibrator.IdentifiedPeak>; val finalResidual: Float
        if (analysis != null) {
            finalCA = analysis.cA; finalCB = analysis.cB; finalCC = analysis.cC; finalCD = analysis.cD; finalCE = analysis.cE
            finalPeaks = analysis.peaks; finalResidual = analysis.residualKev
        } else {
            // HV_AND_COMPARATOR — возвращаем исходные коэффициенты, чтобы applyPending
            // записал ровно тот полином что был в начале.
            finalCA = initCA; finalCB = initCB; finalCC = initCC; finalCD = initCD; finalCE = initCE
            finalPeaks = emptyList(); finalResidual = 0f
        }
        val finalELast = finalCA * (channels - 1) * (channels - 1) + finalCB * (channels - 1) + finalCC
        _state.value = _state.value.copy(
            phase = Phase.AWAITING_APPLY,
            message = "Готово. Нажмите «Применить», чтобы записать в прибор.",
            progressPercent = 100,
            currentHv = finalHv, currentComparator = finalComp,
            pendingResult = PendingResult(
                oldHv = initialHv, newHv = finalHv,
                oldComparator = initialComp, newComparator = finalComp,
                cA = finalCA, cB = finalCB, cC = finalCC, cD = finalCD, cE = finalCE,
                peaks = finalPeaks, residualKev = finalResidual,
                eAtLastChannel = finalELast,
            ),
        )
    }

    // ───────────────────────────── Phase 1: comparator iteration ─────────────────────────────

    /**
     * Подбор компаратора методом **итеративного повышения** при V=100 без источника.
     *
     * При V=100 (максимум реального усиления) шумовая иголка максимальна — что найдём
     * здесь, того хватит для любого V ≥ 100 (большие V → меньшее усиление → иголка
     * меньше). Стартовый comp=100, шаг = step × bitsChannel в АЦП (5/10/20 каналов
     * спектра для 1024/2048/4096). Поднимаем пока max в первых 5% каналов не упадёт
     * ниже 2× max в плато 10..30%.
     */
    private suspend fun tuneComparatorByIteration(channels: Int): Int {
        Log.i("BluZ-AutoCalib", "Comparator tuning: setting V=$MIN_HV_DAC for max gain")
        applyHvAndClearSpectrum(MIN_HV_DAC)
        return tuneComparatorAtCurrentHv(channels)
    }

    /**
     * Подбор компаратора **при текущем V** — без принудительной установки V=100.
     * Используется на финальной фазе после refine, когда полином и V уже подобраны,
     * чтобы компаратор не отсекал полезные первые каналы спектра.
     */
    private suspend fun tuneComparatorAtCurrentHv(channels: Int): Int {

        var comp = 100
        val baseStep = comparatorStepInAdc(channels)  // например 60 АЦП (3 канала × 20)
        val noiseZoneEnd = (channels * 0.05).toInt().coerceAtLeast(20)
        val plateauStart = (channels * 0.10).toInt().coerceAtLeast(noiseZoneEnd * 2)
        val plateauEnd = (channels * 0.30).toInt().coerceAtLeast(plateauStart * 2)
        val NOISE_RATIO_THRESHOLD = 1.5

        // На большом кристалле (CsI 10×10×50, фон ~30 имп/с) метрика ratio = maxNoise/maxPlateau
        // не падает ниже ~2-3 даже когда иголка фактически убита — из-за статистики
        // естественного фона. Дополнительные критерии сходимости:
        //   1) Резкое падение maxNoise (в N раз за одну итерацию) — иголка пробита.
        //   2) Стабилизация maxNoise (3 итерации подряд значение ±30%) — мы на фоне.
        var prevMaxNoise: Double = Double.NaN
        var stableCount = 0
        val DROP_RATIO_FOR_KILL = 3.0
        val STABLE_TOL = 0.30

        for (iter in 1..MAX_COMP_ITERATIONS) {
            _state.value = _state.value.copy(
                phase = Phase.COMPARATOR_TUNING,
                message = "Подбор компаратора (попытка $iter, comp=$comp)…",
                currentComparator = comp, iteration = iter,
                progressPercent = (iter - 1) * 100 / MAX_COMP_ITERATIONS,
            )
            GO.propComparator = comp.toUShort()
            if (!writeAndConfirmComp(comp)) {
                Log.w("BluZ-AutoCalib", "Comp iter $iter: setup not confirmed, продолжаем")
            }
            // Накопление по времени С АКТИВНЫМ ожиданием очистки буфера до старта.
            // Без источника естественный фон даёт единицы счётов/сек — по времени
            // надёжнее чем по импульсам (которых может не набраться за разумное время).
            val spec = clearAndAccumulateByTime(30_000L, channels)
            val maxNoise = (0 until noiseZoneEnd).maxOf { spec[it] }
            val maxPlateau = (plateauStart until plateauEnd).maxOf { spec[it] }.coerceAtLeast(1.0)
            val ratio = maxNoise / maxPlateau
            Log.i("BluZ-AutoCalib", "Comp iter $iter: comp=$comp, maxNoise=$maxNoise, maxPlateau=$maxPlateau, ratio=${"%.2f".format(ratio)}")

            // Сходимость по стандартному порогу (низкий фон, чёткое плато).
            if (ratio < NOISE_RATIO_THRESHOLD) {
                return finalizeCompWithSafetyStep(comp, baseStep, "ratio=$ratio")
            }
            // Сходимость по резкому падению — иголка убита текущим шагом.
            if (!prevMaxNoise.isNaN() && prevMaxNoise / maxNoise >= DROP_RATIO_FOR_KILL && maxNoise < 100) {
                return finalizeCompWithSafetyStep(comp, baseStep,
                    "noise dropped ${"%.1fx".format(prevMaxNoise/maxNoise)} from $prevMaxNoise to $maxNoise")
            }
            // Сходимость по стабилизации maxNoise — мы давно на фоне, иголки нет.
            if (!prevMaxNoise.isNaN() && kotlin.math.abs(maxNoise - prevMaxNoise) / prevMaxNoise < STABLE_TOL) {
                stableCount++
                if (stableCount >= 2 && maxNoise < 100) {
                    return finalizeCompWithSafetyStep(comp, baseStep,
                        "stable at maxNoise≈$maxNoise for ${stableCount+1} iters — on background")
                }
            } else {
                stableCount = 0
            }
            prevMaxNoise = maxNoise

            // Переменный шаг — большой когда ratio огромный, маленький при приближении
            // к порогу. Это позволяет быстро пройти зону массивной иголки (ratio в тысячах)
            // и точно попасть в окрестности threshold без перебора.
            val step = when {
                ratio > 100 -> baseStep * 4   // ~240 АЦП, +12 каналов спектра
                ratio > 10  -> baseStep * 2   // ~120 АЦП, +6 каналов
                else        -> baseStep        // 60 АЦП, +3 канала
            }
            comp = (comp + step).coerceAtMost(1023)
            if (comp >= 1023) {
                Log.w("BluZ-AutoCalib", "Comp reached max DAC, stopping")
                GO.compNoiseLevel = comp
                return comp
            }
        }
        GO.compNoiseLevel = comp
        return comp
    }

    /**
     * Финализация подбора компаратора: добавляет +1 базовый шаг (`baseStep` АЦП ≈ 3
     * канала спектра) к сошедшемуся значению для запаса. На границе сходимости иголка
     * может быть «почти убита» — один шаг сверху гарантирует что остатки не вылезут
     * при изменении ВВ или температуры. baseStep маленький (~60 АЦП) — это безопасно,
     * не отрежет полезные каналы.
     */
    private suspend fun finalizeCompWithSafetyStep(
        converged: Int, baseStep: Int, reason: String,
    ): Int {
        val withSafety = (converged + baseStep).coerceAtMost(1023)
        Log.i("BluZ-AutoCalib", "Comp converged at $converged ($reason), +1 step → $withSafety")
        GO.propComparator = withSafety.toUShort()
        if (!writeAndConfirmComp(withSafety)) {
            Log.w("BluZ-AutoCalib", "Comp safety step: write not confirmed, продолжаем")
        }
        GO.compNoiseLevel = withSafety
        return withSafety
    }

    // ───────────────────────────── Phase 2: HV bracket V=500/100 ─────────────────────────────

    /** Результат bracket scan — финальное V плюс ближайшая измеренная точка (V, ch_609)
     *  для использования как «prev» в первой итерации refine (локальная sensitivity). */
    private data class BracketResult(val tunedV: Int, val nearestV: Int, val nearestCh: Double)

    /**
     * Bracket scan ВВ при V=500 и V=100 с накоплением **по 500000 импульсов** в каждом
     * замере. Линейная интерполяция к каналу `channels × 609/3500`.
     *
     * Запоминает `GO.hvSensitivity = Δch / ΔV` (глобальная) + возвращает ближайшую
     * измеренную точку для refine.
     */
    private suspend fun tuneHvByBracket500and100(channels: Int): BracketResult {
        val targetCh = AutoCalibrator.targetChannelFor(609, channels)
        Log.i("BluZ-AutoCalib", "Bracket scan: target ch for 609 keV = $targetCh")

        _state.value = _state.value.copy(
            phase = Phase.HV_TUNING,
            message = "Замер 1/2: V=$HV_BRACKET_HIGH, накопление…",
            currentHv = HV_BRACKET_HIGH, iteration = 1, progressPercent = 0)
        applyHvAndClearSpectrum(HV_BRACKET_HIGH)
        val spec500 = accumulateByCounts(ACC_REFINE_IMPULSES, channels) { pct, counts ->
            _state.value = _state.value.copy(progressPercent = pct / 2,
                message = "Замер 1/2 (V=$HV_BRACKET_HIGH): $counts / $ACC_REFINE_IMPULSES имп")
        }
        val ch500 = findBrightestPeakCh(spec500, channels)
            ?: throw IllegalStateException("При V=$HV_BRACKET_HIGH пик 609 кэВ не найден.")
        Log.i("BluZ-AutoCalib", "Bracket point 1: V=$HV_BRACKET_HIGH → ch_609=$ch500")

        _state.value = _state.value.copy(
            message = "Замер 2/2: V=$HV_BRACKET_LOW, накопление…",
            currentHv = HV_BRACKET_LOW, iteration = 2, progressPercent = 50)
        applyHvAndClearSpectrum(HV_BRACKET_LOW)
        val spec100 = accumulateByCounts(ACC_REFINE_IMPULSES, channels) { pct, counts ->
            _state.value = _state.value.copy(progressPercent = 50 + pct / 2,
                message = "Замер 2/2 (V=$HV_BRACKET_LOW): $counts / $ACC_REFINE_IMPULSES имп")
        }
        val ch100 = findBrightestPeakCh(spec100, channels)
            ?: throw IllegalStateException("При V=$HV_BRACKET_LOW пик 609 кэВ не найден.")
        Log.i("BluZ-AutoCalib", "Bracket point 2: V=$HV_BRACKET_LOW → ch_609=$ch100")

        val deltaCh = (ch100 - ch500).toFloat()
        val deltaV = (HV_BRACKET_LOW - HV_BRACKET_HIGH).toFloat()  // = -400
        if (abs(deltaCh) < 5f) {
            throw IllegalStateException("ВВ не влияет на масштаб (ch_500=$ch500, ch_100=$ch100).")
        }
        val sensitivity = deltaCh / deltaV  // ch/V
        GO.hvSensitivity = sensitivity
        Log.i("BluZ-AutoCalib", "hvSensitivity=$sensitivity ch/V")
        val tunedV = (HV_BRACKET_HIGH + (targetCh - ch500) / sensitivity)
            .toInt().coerceIn(MIN_HV_DAC, MAX_HV_DAC)
        Log.i("BluZ-AutoCalib", "Bracket interp: V=$tunedV (target ch=$targetCh)")
        // Ближайшая измеренная точка к tunedV — для локальной sensitivity в refine.
        val nearestV: Int; val nearestCh: Double
        if (kotlin.math.abs(tunedV - HV_BRACKET_HIGH) < kotlin.math.abs(tunedV - HV_BRACKET_LOW)) {
            nearestV = HV_BRACKET_HIGH; nearestCh = ch500.toDouble()
        } else {
            nearestV = HV_BRACKET_LOW; nearestCh = ch100.toDouble()
        }
        return BracketResult(tunedV, nearestV, nearestCh)
    }


    // ───────────────────────────── Helpers ─────────────────────────────

    /**
     * Ставит [UserPrompt] в публикуемое [_state] и блокирует процедуру до ответа
     * пользователя.
     *
     * Последовательность:
     *  1. Шлёт прибору `cmd_find_device` (6u) — звук/вибро, чтобы привлечь внимание
     *     пользователя даже если экран не в фокусе.
     *  2. Публикует state с заданными prompt и message.
     *  3. Висит в цикле `while (!userPromptResolved && scope.isActive) delay(200)`.
     *     UI должен вызвать [resolveUserPrompt] чтобы разблокировать.
     *  4. После ответа удерживает паузу 3 сек — пользователь нажимает «Готово»
     *     сразу, но физически переместить источник занимает несколько секунд.
     *  5. Шлёт `cmd_clear_specter` (1u) и ждёт 500 мс — устраняет остаточные счёты,
     *     накопленные во время диалога/перемещения.
     *
     * @param prompt тип запроса; UI использует для подбора заголовка диалога.
     * @param message текст для пользователя (передаётся в state.message и отображается в диалоге).
     */
    private suspend fun requestUserAction(prompt: UserPrompt, message: String) {
        GO.BTT.sendCommand(6u)  // cmd_find_device
        userPromptResolved = false
        _state.value = _state.value.copy(message = message, userPrompt = prompt, progressPercent = null)
        while (!userPromptResolved && scope.isActive) delay(200L)
        _state.value = _state.value.copy(userPrompt = null,
            message = "Подождите 3 сек после изменения положения источника…")
        delay(3_000L)
        GO.BTT.sendCommand(1u)
        delay(500L)
    }

    /**
     * Применяет новое значение ВВ в прибор и подготавливает буфер для следующего измерения.
     *
     * Шаги:
     *  1. Обновляет `GO.propHVoltage`.
     *  2. Вызывает [writeAndConfirmHv] для отправки и подтверждения; при таймауте
     *     процедура **продолжается** — это позволяет пройти случай когда прибор не
     *     отражает новое значение во фрейме (но физически применил).
     *  3. Пауза [HV_STABILIZE_MS] на стабилизацию SiPM.
     *  4. `cmd_clear_specter` + 500 мс — очистка буфера для чистого следующего накопления.
     *
     * @param newHv целевое значение DAC ВВ (0..1023).
     */
    private suspend fun applyHvAndClearSpectrum(newHv: Int) {
        Log.i("BluZ-AutoCalib", "applyHvAndClearSpectrum: writing HV=$newHv")
        GO.propHVoltage = newHv.toUShort()
        if (!writeAndConfirmHv(newHv)) {
            Log.w("BluZ-AutoCalib", "HV=$newHv NOT confirmed after retry — продолжаем без подтверждения")
        }
        delay(HV_STABILIZE_MS)
        GO.BTT.sendCommand(1u)
        delay(500L)
    }

    /**
     * Отправляет полный конфиг (`cmd_setup`) и подтверждает применение ВВ через
     * read-back BLE-фрейма с retry.
     *
     *  1. `sendFullConfigToDevice()` → `cmd_setup` (0u).
     *  2. Ждём до 5 сек фрейм где `frame.hw.hVoltage == target`.
     *  3. Если не дождались — **повторяем** `cmd_setup` и ждём ещё 10 сек.
     *
     * Read-back через сам BLE-фрейм (а не отдельную команду чтения) — прибор сам
     * периодически шлёт состояние своей конфигурации, мы просто его читаем.
     *
     * @return `true` если значение подтверждено прибором (хотя бы со второй попытки),
     *         `false` иначе. Возврат `false` не означает что прибор не применил
     *         значение — возможно прошивка просто не обновила поле во фрейме.
     */
    private suspend fun writeAndConfirmHv(target: Int): Boolean {
        sendFullConfigToDevice()
        if (waitForHvFrame(target, 5_000L)) {
            Log.i("BluZ-AutoCalib", "HV=$target confirmed by device")
            return true
        }
        Log.w("BluZ-AutoCalib", "HV=$target not confirmed within 5s, retrying cmd_setup…")
        sendFullConfigToDevice()
        if (waitForHvFrame(target, 10_000L)) {
            Log.i("BluZ-AutoCalib", "HV=$target confirmed after retry")
            return true
        }
        return false
    }

    /**
     * Подписывается на поток BLE-фреймов и ждёт первого с `hw.hVoltage == target`.
     *
     * @return `true` если за [timeoutMs] миллисекунд пришёл нужный фрейм, `false` иначе.
     *
     * Реализована как `kotlinx.coroutines.withTimeoutOrNull` + бесконечный `while (true)`
     * с `deviceFrames.first()` — корутина кооперативно прерывается по таймауту.
     */
    private suspend fun waitForHvFrame(target: Int, timeoutMs: Long): Boolean =
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            while (true) {
                val frame = GO.BTT.deviceFrames.first()
                if (frame.hw.hVoltage.toInt() == target) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE") false
        } ?: false

    /**
     * Аналог [writeAndConfirmHv] для компаратора.
     *
     * Семантика идентична: `cmd_setup` → ждём 5 сек подтверждение → если нет, повторяем
     * `cmd_setup` → ждём 10 сек. Возврат `false` не блокирует процедуру.
     */
    private suspend fun writeAndConfirmComp(target: Int): Boolean {
        sendFullConfigToDevice()
        if (waitForCompFrame(target, 5_000L)) {
            Log.i("BluZ-AutoCalib", "Comp=$target confirmed by device")
            return true
        }
        Log.w("BluZ-AutoCalib", "Comp=$target not confirmed within 5s, retrying cmd_setup…")
        sendFullConfigToDevice()
        if (waitForCompFrame(target, 10_000L)) {
            Log.i("BluZ-AutoCalib", "Comp=$target confirmed after retry")
            return true
        }
        return false
    }

    /** Аналог [waitForHvFrame] для поля `frame.hw.comparator`. */
    private suspend fun waitForCompFrame(target: Int, timeoutMs: Long): Boolean =
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            while (true) {
                val frame = GO.BTT.deviceFrames.first()
                if (frame.hw.comparator.toInt() == target) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE") false
        } ?: false

    /**
     * Поиск опорного пика 609 кэВ **справа налево** с двойным критерием:
     *  - значение > 10% от globalMax (адаптивный абсолютный порог)
     *  - prominence > 10σ над фоном
     *  - FWHM ≥ 4 каналов (отсев иголок)
     */
    private fun findBrightestPeakCh(spec: DoubleArray, channels: Int): Int? {
        val smaWindow = when (channels) { 4096 -> 12; 2048 -> 6; else -> 3 }
        val edge = (channels * 0.05).toInt().coerceAtLeast(20)
        val fwhmExpected = when (channels) { 4096 -> 64; 2048 -> 32; else -> 16 }
        val MIN_REAL_PEAK_HALF_WIDTH = 4

        val smoothed = DoubleArray(channels)
        for (i in 0 until channels) {
            var sum = 0.0; var cnt = 0
            for (j in (i - smaWindow / 2)..(i + smaWindow / 2)) {
                if (j in 0 until channels) { sum += spec[j]; cnt++ }
            }
            smoothed[i] = if (cnt > 0) sum / cnt else 0.0
        }
        var globalMax = 0.0
        for (i in edge until channels - 20) if (smoothed[i] > globalMax) globalMax = smoothed[i]
        val absolutePeakThreshold = globalMax * 0.10
        Log.i("BluZ-AutoCalib", "findBrightestPeakCh: globalMax=$globalMax, threshold (10%) = $absolutePeakThreshold")

        for (i in (channels - 20) downTo edge) {
            val v = smoothed[i]
            if (v < absolutePeakThreshold) continue
            var isMax = true
            val lo = (i - fwhmExpected).coerceAtLeast(edge)
            val hi = (i + fwhmExpected).coerceAtMost(channels - 1)
            for (j in lo..hi) {
                if (j != i && smoothed[j] > v) { isMax = false; break }
            }
            if (!isMax) continue
            val bgLo = (i - 2 * fwhmExpected).coerceAtLeast(edge)
            val bgHi = (i + 2 * fwhmExpected).coerceAtMost(channels - 1)
            var bg = Double.MAX_VALUE
            for (j in bgLo..bgHi) if (smoothed[j] < bg) bg = smoothed[j]
            val prominence = v - bg
            if (prominence < 10.0 * kotlin.math.sqrt(kotlin.math.max(1.0, bg))) continue
            val halfV = v * 0.5
            val leftCh = (i - MIN_REAL_PEAK_HALF_WIDTH).coerceAtLeast(0)
            val rightCh = (i + MIN_REAL_PEAK_HALF_WIDTH).coerceAtMost(channels - 1)
            if (smoothed[leftCh] < halfV * 0.3 && smoothed[rightCh] < halfV * 0.3) continue

            Log.i("BluZ-AutoCalib", "findBrightestPeakCh (R→L): found ch=$i v=${"%.1f".format(v)} bg=${"%.1f".format(bg)} prom=${"%.1f".format(prominence)}")
            return i
        }
        Log.w("BluZ-AutoCalib", "findBrightestPeakCh (R→L): no significant peak found (globalMax=$globalMax)")
        return null
    }

    /**
     * Очистка буфера + накопление по времени. Перед началом таймера ждёт пока
     * `spectrData.sum()` реально упадёт до фонового уровня (после `cmd_clear_specter`
     * прибор очищает буфер с задержкой ~1 сек, и без ожидания мы видим остаточный
     * спектр от предыдущей итерации — все iter показывали одинаковые значения).
     *
     * Используется в фазе компаратора **без источника**, где `accumulateByCounts`
     * не подходит: естественный фон даёт единицы счётов/сек, и target по импульсам
     * не наберётся за разумное время.
     */
    private suspend fun clearAndAccumulateByTime(durationMs: Long, channels: Int): DoubleArray {
        GO.BTT.sendCommand(1u)  // cmd_clear_specter
        // Ждём пока буфер реально очистится — до 5 сек или пока sum < 100.
        val clearStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - clearStart < 5_000L && scope.isActive) {
            if (GO.drawSPECTER.spectrData.sum() < 100.0) break
            delay(200L)
        }
        val clearedSum = GO.drawSPECTER.spectrData.sum().toInt()
        Log.i("BluZ-AutoCalib", "clearAndAccumulateByTime: cleared in ${System.currentTimeMillis() - clearStart}ms, sum=$clearedSum")
        // Теперь накапливаем по времени.
        val started = System.currentTimeMillis()
        while (scope.isActive && System.currentTimeMillis() - started < durationMs) {
            delay(250L)
        }
        return GO.drawSPECTER.spectrData.copyOf(channels)
    }

    /**
     * Накопление по таймеру без активной очистки буфера.
     *
     * Используется только в коде где известно что предыдущая фаза уже очистила буфер
     * самостоятельно (`clearAndAccumulateByTime`, `accumulateByCounts`). Внутри
     * собственно цикла этой функции `cmd_clear_specter` не отправляется — она просто
     * ждёт `durationMs` миллисекунд, периодически дергая [onProgress].
     *
     * @param durationMs длительность накопления.
     * @param channels количество каналов для копирования итогового спектра.
     * @param onProgress колбэк, получает прошедшие миллисекунды; вызывается каждые ~250 мс.
     */
    private suspend fun accumulateAndCopy(
        durationMs: Long, channels: Int, onProgress: (Long) -> Unit
    ): DoubleArray {
        val started = System.currentTimeMillis()
        while (scope.isActive) {
            val elapsed = System.currentTimeMillis() - started
            if (elapsed >= durationMs) break
            onProgress(elapsed)
            delay(250L)
        }
        return GO.drawSPECTER.spectrData.copyOf(channels)
    }

    /**
     * Накопление спектра до достижения [targetCounts] суммарных импульсов или таймаута
     * [ACC_TIMEOUT_MS]. Адаптивно к активности источника — на активном Ra-226 500к
     * импульсов набираются за ~3 мин, на слабом за ~10 мин.
     *
     * **Активное ожидание очистки буфера.** Перед стартом накопления:
     *  1. Шлёт `cmd_clear_specter` (1u).
     *  2. Ждёт до 5 сек пока `spectrData.sum() < 5%` от target (или `< 100`).
     *     Это критично: `cmd_clear_specter` обрабатывается прибором с задержкой ~1 сек,
     *     обновлённый BLE-фрейм с пустым буфером приходит не сразу. Без активного
     *     ожидания следующая фаза могла видеть остаточный спектр от предыдущей и
     *     ошибочно завершаться по `counts >= target` мгновенно.
     *
     * @param targetCounts целевое количество импульсов суммарно по всему спектру.
     * @param channels количество каналов для копирования итогового спектра.
     * @param onProgress колбэк прогресса `(percent: Int, counts: Int)`. Вызывается
     *                   только при изменении `counts` (~раз в секунду по BLE-фрейму).
     * @return копия спектра на момент достижения цели или таймаута.
     */
    private suspend fun accumulateByCounts(
        targetCounts: Int, channels: Int, onProgress: (percent: Int, counts: Int) -> Unit
    ): DoubleArray {
        GO.BTT.sendCommand(1u)  // cmd_clear_specter
        // Активное ожидание реальной очистки буфера прибора.
        val clearStart = System.currentTimeMillis()
        val clearThreshold = (targetCounts * 0.05).toInt().coerceAtLeast(100)
        while (scope.isActive && System.currentTimeMillis() - clearStart < 5_000L) {
            if (GO.drawSPECTER.spectrData.sum().toInt() < clearThreshold) break
            delay(200L)
        }
        val clearedCounts = GO.drawSPECTER.spectrData.sum().toInt()
        Log.i("BluZ-AutoCalib", "accumulateByCounts: cleared in ${System.currentTimeMillis() - clearStart}ms, sum=$clearedCounts")

        val started = System.currentTimeMillis()
        var lastCounts = 0
        while (scope.isActive) {
            val counts = GO.drawSPECTER.spectrData.sum().toInt()
            val pct = (counts * 100 / targetCounts).coerceIn(0, 100)
            if (counts != lastCounts) {
                onProgress(pct, counts)
                lastCounts = counts
            }
            if (counts >= targetCounts) break
            if (System.currentTimeMillis() - started > ACC_TIMEOUT_MS) {
                Log.w("BluZ-AutoCalib", "accumulateByCounts: timeout at $counts/$targetCounts counts")
                break
            }
            delay(500L)
        }
        return GO.drawSPECTER.spectrData.copyOf(channels)
    }


    /**
     * Записывает коэффициенты полинома в `GO.propCoef{N}*` для активного разрешения.
     *
     * Прошивка хранит три независимых набора коэффициентов (для 1024, 2048, 4096
     * каналов) — при переключении разрешения используется свой полином. Эта функция
     * пишет ТОЛЬКО в активный набор.
     *
     * @param cA коэффициент при ch² (типично малое значение ~1e-5..1e-3).
     * @param cB коэффициент при ch (типично 1..5).
     * @param cC свободный член (типично от -200 до +50 кэВ).
     */
    /*
    private fun writeCurrentResolutionCoefs(cA: Float, cB: Float, cC: Float) {
        when (GO.spectrResolution) {
            0 -> { GO.propCoef1024A = cA; GO.propCoef1024B = cB; GO.propCoef1024C = cC }
            1 -> { GO.propCoef2048A = cA; GO.propCoef2048B = cB; GO.propCoef2048C = cC }
            2 -> { GO.propCoef4096A = cA; GO.propCoef4096B = cB; GO.propCoef4096C = cC }
        }
    }*/

    /**
     * Заполняет [BluetoothInterface.sendBuffer] полным набором конфигурационных полей
     * из текущих `GO.prop*` и шлёт `cmd_setup` (0u) — прошивка применяет всё (ВВ,
     * компаратор, полином, пороги, режимы) сразу, без перезагрузки прибора.
     *
     * **Намеренное дублирование.** Логика заполнения buffer повторяет блок Save
     * (кнопка Write) в `SettingsFragment.kt`. Дубликат сделан осознанно: контроллер
     * пишет из значений `GO.prop*`, а Save-блок читал значения из EditText-полей UI;
     * объединять их через общий хелпер потребовало бы рефакторинга проверенного
     * рабочего кода Save, что выходит за рамки задачи автокалибровки. См. CLAUDE-2.md
     * правило Surgical Changes.
     *
     * **Раскладка buffer'а** соответствует прошивке `bluz_app.c:200..250` (cmd_setup):
     *  - `[4..7]` propLevel1 (int32 LE)
     *  - `[8..11]` propLevel2
     *  - `[12..15]` propLevel3
     *  - `[16..19]` propCPS2UR (float)
     *  - `[20]` бит-маска индикации (LED/Sound/Vibro per level)
     *  - `[21..32]` polynom 1024 (3 float)
     *  - `[33..34]` propHVoltage (uint16 LE)
     *  - `[35..36]` propComparator (uint16 LE)
     *  - `[37]` spectrResolution (0/1/2)
     *  - `[38]` bit-mask: spectrometer-on + sampleTime + click10 + led10
     *  - `[39..50]` polynom 2048
     *  - `[51..62]` polynom 4096
     *  - `[63..64]` aqureValue (uint16 LE)
     *  - `[65]` bitsChannel
     */
    private fun sendFullConfigToDevice() {
        val buf = GO.BTT.sendBuffer

        // Пороги уровней — int32 little-endian
        ByteBuffer.allocate(4).putInt(GO.propLevel1).array().also { putBytes(buf, 4, it) }
        ByteBuffer.allocate(4).putInt(GO.propLevel2).array().also { putBytes(buf, 8, it) }
        ByteBuffer.allocate(4).putInt(GO.propLevel3).array().also { putBytes(buf, 12, it) }

        // CPS→мкР/ч (float)
        ByteBuffer.allocate(4).putFloat(GO.propCPS2UR).array().also { putBytes(buf, 16, it) }

        // Бит-маска индикации (LED/Sound/Vibro per level) — собираем из prop*.
        var bits: UByte = 0u
        if (GO.propLedKvant == 1 || GO.propLedKvant == 2) bits = 1u
        if (GO.propSoundKvant == 1 || GO.propSoundKvant == 2) bits = bits or 0b00000010u
        if (GO.propSoundLevel1) bits = bits or 0b00000100u
        if (GO.propSoundLevel2) bits = bits or 0b00001000u
        if (GO.propSoundLevel3) bits = bits or 0b00010000u
        if (GO.propVibroLevel1) bits = bits or 0b00100000u
        if (GO.propVibroLevel2) bits = bits or 0b01000000u
        if (GO.propVibroLevel3) bits = bits or 0b10000000u
        buf[20] = bits

        // Полиномы 1024/2048/4096 — по 3 float'а каждый.
        //ByteBuffer.allocate(4).putFloat(GO.propCoef1024A).array().also { putBytes(buf, 21, it) }
        //ByteBuffer.allocate(4).putFloat(GO.propCoef1024B).array().also { putBytes(buf, 25, it) }
        //ByteBuffer.allocate(4).putFloat(GO.propCoef1024C).array().also { putBytes(buf, 29, it) }

        // propHVoltage / propComparator — uint16 little-endian
        buf[33] = (GO.propHVoltage and 255u).toUByte()
        buf[34] = ((GO.propHVoltage.toUInt() shr 8) and 255u).toUByte()
        buf[35] = (GO.propComparator and 255u).toUByte()
        buf[36] = ((GO.propComparator.toUInt() shr 8) and 255u).toUByte()

        buf[37] = GO.spectrResolution.toUByte()

        // Бит spectrometer-on + sampleTime + click10 + led10
        var b38: UByte = if (GO.propAutoStartSpectrometr) 1u else 0u
        b38 = b38 or ((GO.sampleTime.toUInt() and 7u) shl 1).toUByte()
        if (GO.propLedKvant == 2) b38 = b38 or 16u  // delitel ×10
        if (GO.propSoundKvant == 2) b38 = b38 or 32u  // delitel ×10
        buf[38] = b38

        //ByteBuffer.allocate(4).putFloat(GO.propCoef2048A).array().also { putBytes(buf, 39, it) }
        //ByteBuffer.allocate(4).putFloat(GO.propCoef2048B).array().also { putBytes(buf, 43, it) }
        //ByteBuffer.allocate(4).putFloat(GO.propCoef2048C).array().also { putBytes(buf, 47, it) }
        ByteBuffer.allocate(4).putFloat(GO.propCoef4096A).array().also { putBytes(buf, 51, it) }
        ByteBuffer.allocate(4).putFloat(GO.propCoef4096B).array().also { putBytes(buf, 55, it) }
        ByteBuffer.allocate(4).putFloat(GO.propCoef4096C).array().also { putBytes(buf, 59, it) }
        ByteBuffer.allocate(4).putFloat(GO.propCoef4096D).array().also { putBytes(buf, 43, it) }
        ByteBuffer.allocate(4).putFloat(GO.propCoef4096E).array().also { putBytes(buf, 47, it) }

        // aqure (uint16 LE через .toShort() truncate — допускает значения >32767)
        val aqureBytes = ByteBuffer.allocate(2).putShort(GO.aqureValue.toInt().toShort()).array()
        buf[63] = aqureBytes[0].toUByte(); buf[64] = aqureBytes[1].toUByte()
        buf[65] = GO.bitsChannel.toUByte()

        GO.BTT.sendCommand(0u)  // cmd_setup
    }

    /**
     * Копирует байты `src[0..src.size]` в `buf[offset..offset+src.size]` с конверсией
     * `Byte → UByte`. Используется для вставки little-endian представления int/float
     * (полученных через `ByteBuffer.allocate(N).putXxx().array()`) в `sendBuffer`.
     */
    private fun putBytes(buf: UByteArray, offset: Int, src: ByteArray) {
        for (i in src.indices) buf[offset + i] = src[i].toUByte()
    }
}
