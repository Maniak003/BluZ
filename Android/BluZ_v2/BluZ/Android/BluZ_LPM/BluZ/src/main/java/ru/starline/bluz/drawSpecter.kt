package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Html
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.emptyLongSet
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import ru.starline.bluz.MainActivity
import java.lang.Math.log
import java.lang.Math.round
import java.lang.Math.sqrt
import java.util.Locale
import kotlin.math.ln
import androidx.core.graphics.createBitmap


/**
 * Отрисовка живого спектра излучения через [Canvas]/[Bitmap].
 *
 * Один экземпляр живёт в `App.onCreate` (`GO.drawSPECTER`) — переживает пересоздание Activity
 * при повороте экрана, чтобы не терять накопленный спектр [spectrData] (4096 значений).
 *
 * **Цикл отрисовки** ([redrawSpecter]):
 *  1. SMA-фильтр (окно `GO.windowSMA`) если [flagSMA]
 *  2. Медианный фильтр (3 точки) если [flagMEDIAN]
 *  3. Энергокомпенсация MLEM (через [DoseCalculator.calculateH10DoseSafe]) → `GO.compMED`,
 *     если [flagMLEM] и есть калибровка
 *  4. Поиск максимума → коэффициенты [koefLin] / [koefLog]
 *  5. Цикл по каналам → `canvas.drawLine` (style=line) или `drawLine` сверху-вниз (histogram)
 *
 * **Реинициализация bitmap** ([init]) — `if (GO.drawObjectInit || !specBitmap.isInitialized)`.
 * Флаг `GO.drawObjectInit` ставится в true фрагментом перед первым redraw (после layout).
 *
 * @see drawHistory — то же, но для накопленного исторического спектра
 * @see drawCursor — overlay поверх специтра (вертикальная линия, информация об изотопе)
 */
class drawSpecter {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    public lateinit var imgView : ImageView
    public var mlemBuffer = DoubleArray(4096)
    public var spectrData: DoubleArray = DoubleArray(4096)
    public var tmpSpecterData: DoubleArray = DoubleArray(4096)
    public var flagSMA: Boolean = false
    public var flagMEDIAN: Boolean = false
    public var flagMLEM: Boolean = false
    public var ResolutionSpectr: Int = 1024;
    public var koefLog: Double = 1.0
    public var koefLin: Double = 1.0
    public var xSize: Double = 1.0                          // Размер для одного канала.
    private var MF: DoubleArray = DoubleArray(3)
    private var indexMF: Int = 0

    /**
     * Создаёт (или пересоздаёт) [specBitmap] и [specCanvas] под текущие размеры [imgView].
     *
     * Пересоздание срабатывает если `GO.drawObjectInit == true` (фрагмент ставит флаг при
     * повороте) или если bitmap ещё не создан. Если `imgView.width / height == 0` (layout
     * ещё не отработал) — просто логирует ошибку и выходит.
     */
    fun init() {
        if (! this::imgView.isInitialized) {
            Log.e("BluZ-BT", "imgView not init.")
            return
        }
        if (GO.drawObjectInit or !this::specBitmap.isInitialized) {
            HSize = imgView.width
            VSize = imgView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize, Res: $ResolutionSpectr")
            } else {
                /* Подготавливаем bitmap для рисования */
                specBitmap = createBitmap(HSize, VSize)
                specCanvas = Canvas(specBitmap)
                GO.drawObjectInit = false
            }
        } else {
            imgView.setImageBitmap(specBitmap)
        }
        //txtStat1.text = saveStat1
        //txtStat2.text = saveStat2
        //txtStat3.text = saveStat3
    }

    /**
     * Полный цикл отрисовки спектра в [specCanvas]:
     *  1. Применение SMA-фильтра ([flagSMA]) с окном `GO.windowSMA`
     *  2. Медианного фильтра ([flagMEDIAN]) — 3-точечного
     *  3. MLEM-компенсации ([flagMLEM]) — расчёт `GO.compMED` через [DoseCalculator.calculateH10DoseSafe]
     *  4. Масштабирование по X ([xSize]) с учётом [GO.xZoom]
     *  5. Цикл по каналам с двумя `drawLine` — линейная (lin) и логарифмическая (log) шкала
     *  6. Для каждого канала индекс смещается на `GO.xPosition` (pan)
     *
     * @param spType Тип спектра (0=1024, 1=2048, 2=4096) — не используется напрямую, но
     *  логируется для отладки. Реальное разрешение — [ResolutionSpectr].
     * @param offSetX Резерв на будущее, не используется в текущей реализации.
     */
    fun redrawSpecter(spType: Int, offSetX: Float) {
        if (!this::specBitmap.isInitialized) {
            Log.w("BluZ-BT", "specBitmap not initialized, call init() first")
            return
        }
        Log.d("BluZ-BT", "Type: $spType HSize: $HSize VSize: $VSize, Res: $ResolutionSpectr")
        //Log.d("BluZ-BT", "Draw specter.")
        val paintLin: Paint = Paint()
        val paintLog: Paint = Paint()
        val paintFoneLin = Paint()
        val paintFoneLog = Paint()

        /*
         *  Подготовка массива для отрисовки данных
         *  Функии фильтрации
         */

        var tmpSM: Double
        for (idx: Int in 0 until 3) {
            MF[idx] = 0.0
        }
        indexMF = 0
        for (ttt: Int in 0 until ResolutionSpectr - GO.windowSMA) {
            if (ttt > GO.rejectChann) {
                /* SMA фильтр */
                if (flagSMA) {
                    /* SMA */
                    tmpSM = 0.0
                    for (k in 0 until GO.windowSMA) {
                        tmpSM += spectrData[ttt + k]
                    }
                    tmpSpecterData[ttt] = tmpSM / GO.windowSMA
                } else {
                    /* Без преобразования */
                    tmpSpecterData[ttt] = spectrData[ttt]
                }
                /* Медианный фильтр */
                if (flagMEDIAN) {
                    MF[indexMF] = tmpSpecterData[ttt]
                    tmpSpecterData[ttt] = MF.sorted()[1]
                    if (++indexMF >= 3) {
                        indexMF = 0
                    }
                }
            } else {
                tmpSpecterData[ttt] = 0.0       // Удалим данные из не используемых каналов
            }
        }

        xSize = HSize.toDouble() / ResolutionSpectr * GO.xZoom
        when (ResolutionSpectr) {
            1024 -> {
                paintLin.strokeWidth = 3.0f //xSize.toFloat()
                paintLog.strokeWidth = 3.0f // xSize.toFloat()
                paintFoneLin.strokeWidth = 4.0f //xSize.toFloat() * 2
                paintFoneLog.strokeWidth = 4.0f //xSize.toFloat() * 2
            }

            2048 -> {
                paintLin.strokeWidth = 2.0f //xSize.toFloat()
                paintLog.strokeWidth = 2.0f // xSize.toFloat()
                paintFoneLin.strokeWidth = 3.0f //xSize.toFloat() * 2
                paintFoneLog.strokeWidth = 3.0f //xSize.toFloat() * 2
            }

            4096 -> {
                paintLin.strokeWidth = 1.0f //xSize.toFloat()
                paintLog . strokeWidth = 1.0f // xSize.toFloat()
                paintFoneLin.strokeWidth = 2.0f //xSize.toFloat() * 2
                paintFoneLog.strokeWidth = 2.0f //xSize.toFloat() * 2
            }
            else -> {
                paintLin.strokeWidth = 3.0f //xSize.toFloat()
                paintLog.strokeWidth = 3.0f // xSize.toFloat()
                paintFoneLin.strokeWidth = 4.0f //xSize.toFloat() * 2
                paintFoneLog.strokeWidth = 4.0f //xSize.toFloat() * 2
            }
        }

        if (GO.specterGraphType == 0) {         // Линейный стиль графика
            paintLin.color = GO.ColorLin        // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLog        // Цвет для отображения логарифмического спектра
            paintFoneLin.color = GO.ColorFone
            paintFoneLog.color = GO.ColorFoneLg
        } else {                                // Стиль гистограмма
            paintLin.color = GO.ColorLinGisto   // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLogGisto   // Цвет для отображения логарифмического спектра
            paintFoneLin.color = GO.ColorFoneGisto
            paintFoneLog.color = GO.ColorFoneLgGisto
        }
        var oldYlin: Double = VSize.toDouble()
        var oldYlog: Double = VSize.toDouble()
        var oldX: Double = 0.0
        var maxYlin: Double = 0.0
        var maxYlog: Double = 0.0
        //var tmpLog: Double

        /* Поиск максимального значения массива MLEM */
        if (flagMLEM) { // Массив готов и можно прорисовывать
            /* Очистим младшие каналы, там всегда шум. */
            for (idxm in 0 until 20) {
                mlemBuffer[idxm] = 0.0
            }
        }
        /* Поиск максимального значения для масштабирования */
        for (idx in GO.rejectChann .. ResolutionSpectr - 1) {
            if (maxYlin < tmpSpecterData[idx]) {
                maxYlin = tmpSpecterData[idx]
            }
        }
        maxYlog = ln(maxYlin);
        //Log.d("BluZ-BT", "MAX: : $maxYlin")
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        var Ylin: Float
        var Ylog: Float
        for (idx in 0 until ResolutionSpectr) {
            var idx1 = (idx + GO.xPosition).toInt()
            if (idx1 >= ResolutionSpectr) {
                idx1 = ResolutionSpectr - 1
            }
            /* Подготовка данных спектра */
            Ylin = (VSize - tmpSpecterData[idx1] * koefLin).toFloat()
            Ylog = if (tmpSpecterData[idx1] != 0.0) {
                (VSize - ln(tmpSpecterData[idx1]) * koefLog).toFloat()
            } else {
                VSize.toFloat()
            }
            if (GO.specterGraphType == 0) {         // Стиль графика - линия
                /* Прорисовка линейного графика */

                if ( ! (oldYlin == VSize.toDouble() && tmpSpecterData[idx1] == 0.0)) {
                    specCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlin.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylin,                       // Конечный Y
                        paintLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldYlog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ && Ylog == VSize.toFloat())) {
                    specCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlog.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylog,                       // Конечный Y
                        paintLog
                    )
                }
            } else {                                // Стиль графика - гистограмма
                /* Линейный график */
                specCanvas.drawLine((idx * xSize).toFloat(), Ylin, (idx * xSize).toFloat(),VSize.toFloat(), paintLin )
                /* Логарифмический график */
                specCanvas.drawLine((idx * xSize).toFloat(), Ylog, (idx * xSize).toFloat(),VSize.toFloat(), paintLog )
            }
            //if ((Ylin.toDouble() < VSize) && (GO.specterGraphType == 0)) {
                oldYlin = Ylin.toDouble()
                oldYlog = Ylog.toDouble()
                oldX = idx.toDouble()
            //}
        }
        /* Тестовая линия */
        //specCanvas.drawLine(0.0f, VSize.toFloat(), HSize.toFloat(),VSize.toFloat(), paintLin )

        imgView.setImageBitmap(specBitmap)

        //saveStat1 = txtStat1.text.toString()
        //saveStat2 = txtStat2.text.toString()
        //saveStat3 = txtStat3.text.toString()
        //analyzeChiVector(DoseCalculator.chiVectorOrg)
        //debugDoseCalculation(DoseCalculator.chiVectorOrg, spectrData, GO.spectrometerTime.toDouble())

        /* Проверим наличие калибровки */
        val needCalcH10D = ((GO.propCoef4096A != 0f) || (GO.propCoef4096B != 0f) || (GO.propCoef4096C != 0f))

        /* Определим максимальную отображаемую энергию */
        val maxEnergy = GO.enrgCalc.channelToEnergy(4095)
        /* Расчет энергокомпенсированный МЕД */
        if (needCalcH10D && maxEnergy > 0) {
            GO.compMED = DoseCalculator.calculateH10DoseSafe(
                DoseCalculator.chiVectorOrg,
                spectrData,
                false,
                GO.spectrometerTime.toDouble(),
                maxEnergy.toFloat()
            ).toFloat()
        } else {
            GO.compMED = 0f
        }
    }

    fun analyzeChiVector(chi: DoubleArray) {
        val positive = chi.count { it > 0 }
        val negative = chi.count { it < 0 }
        val zero = chi.count { it == 0.0 }

        Log.d("CHI_ANALYSIS", """
        Размер: ${chi.size}
        Положительные: $positive (${100*positive/chi.size}%)
        Отрицательные: $negative (${100*negative/chi.size}%)
        Нули: $zero
        Сумма: ${"%.3e".format(chi.sum())}
        Среднее: ${"%.3e".format(chi.average())}
        Медиана: ${"%.3e".format(chi.sorted()[chi.size/2])}
    """.trimIndent())
    }

    fun debugDoseCalculation(
        chi: DoubleArray,
        spectrum: DoubleArray,
        acquisitionTimeSec: Double
    ) {
        Log.d("DOSE_DEBUG", "=== ОТЛАДКА РАСЧЁТА ДОЗЫ ===")

        // 1. Параметры χ
        Log.d("DOSE_DEBUG", "χ: size=${chi.size}, min=${"%.3e".format(chi.minOrNull() ?: 0.0)}, max=${"%.3e".format(chi.maxOrNull() ?: 0.0)}")

        // 2. Параметры спектра
        val specSum = spectrum.sum()
        val specMax = spectrum.maxOrNull() ?: 0.0
        Log.d("DOSE_DEBUG", "Спектр: size=${spectrum.size}, sum=$specSum, max=$specMax")

        // 3. Время
        Log.d("DOSE_DEBUG", "Время: $acquisitionTimeSec с")

        // 4. Расчёт полной дозы (без нормировки и времени)
        val rawDose = DoseCalculator.calculateH10DoseSafe(
            chi = chi,
            spectrum = spectrum,
            normalize = false,
            acquisitionTimeSec = 0.0  // Полная доза
        )
        Log.d("DOSE_DEBUG", "Полная доза (сырая): %.4e".format(rawDose))

        // 5. Расчёт мощности дозы
        val doseRate = if (acquisitionTimeSec > 0) {
            rawDose / acquisitionTimeSec * 3600.0
        } else {
            rawDose
        }
        Log.d("DOSE_DEBUG", "Мощность дозы: %.4f μR/ч (или пЗв/ч)".format(doseRate))

        // 6. Ожидаемая оценка по счётчику
        val cps = specSum / acquisitionTimeSec
        val estimatedDoseRate = cps * 0.1  // Грубая оценка: ~0.1 μR/ч на 1 cps для малого NaI
        Log.d("DOSE_DEBUG", "Оценка по cps: $cps cps → ~%.2f μR/ч".format(estimatedDoseRate))

        Log.d("DOSE_DEBUG", "===============================")
    }

    /** Заливает [specCanvas] чёрным, **не** сбрасывает [spectrData]. Используется
     *  перед перерисовкой, чтобы убрать старые штрихи. */
    fun clearSpecter() {
        //Log.d("BluZ-BT", "Clear specter.")
        if ((HSize > 0) and (VSize > 0)) {
            specCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
    /** **Сбрасывает данные спектра** ([spectrData], [tmpSpecterData] — обнуляет все 4096
     *  значений) и заливает canvas чёрным. Вызывается из обработчика `buttonClearSpectr` в
     *  SpectrumFragment перед `sendCommand(1u)` (очистка в приборе). */
    fun resetSpecter() {
        //Log.d("BluZ-BT", "Clear specter.")
        for (ttt: Int in 0 until 4096) {
            tmpSpecterData[ttt] = 0.0
            spectrData[ttt] = 0.0
        }
        if ((HSize > 0) and (VSize > 0)) {
            specCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
}