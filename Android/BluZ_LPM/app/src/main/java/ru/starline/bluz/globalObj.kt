package ru.starline.bluz

import android.content.ClipData.Item
import android.content.Context
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.text.HtmlCompat
import androidx.loader.content.Loader.ForceLoadContentObserver
import androidx.viewpager2.widget.ViewPager2
import ru.starline.bluz.GO
import java.sql.Array
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by ed on 27,июнь,2024
 */
class globalObj {
    public val propCfgADDRESS: String = "Address"
    public val propCfgBLEDeviceName: String = "BluZ"
    public val propCfgSpectrGraphType: String = "SpecterGraphType"
    public val propCfgColorDozimeter: String = "ColorDozimeter"
    public val propCfgColorDozimeterSMA: String = "ColorDozimeterSMA"
    public val propCfgColorSpecterLin: String = "ColorLin"
    public val propCfgColorSpecterLog: String = "ColorLog"
    public val propCfgColorSpecterFone: String = "ColorFone"
    public val propCfgColorSpecterFoneLg: String = "ColorFoneLg"
    public val propCfgColorSpecterLinGisto: String = "ColorLinGisto"
    public val propCfgColorSpecterLogGisto: String = "ColorLogGisto"
    public val propCfgColorSpecterFoneGisto: String = "ColorFoneGisto"
    public val propCfgColorSpecterFoneLgGisto: String = "ColorFoneLgGisto"
    public val propCfgLevel1: String = "Level1"
    public val propCfgLevel2: String = "Level2"
    public val propCfgLevel3: String = "Level3"
    public val propCfgSoundLevel1: String = "soundLevel1"
    public val propCfgSoundLevel2: String = "soundLevel2"
    public val propCfgSoundLevel3: String = "soundLevel3"
    public val propCfgVibroLevel1: String = "vibroLevel1"
    public val propCfgVibroLevel2: String = "vibroLevel2"
    public val propCfgVibroLevel3: String = "vibroLevel3"
    public val propCfgCPS2UR: String = "CPS2UR"
    public val propCfgCoef1024A: String = "CoeffA"
    public val propCfgCoef1024B: String = "CoeffB"
    public val propCfgCoef1024C: String = "CoeffC"
    public val propCfgCoef2048A: String = "Coeff2048A"
    public val propCfgCoef2048B: String = "Coeff2048B"
    public val propCfgCoef2048C: String = "Coeff2048C"
    public val propCfgCoef4096A: String = "Coeff4096A"
    public val propCfgCoef4096B: String = "Coeff4096B"
    public val propCfgCoef4096C: String = "Coeff4096C"
    public val propCfgHV: String = "HVoltage"
    public val propCfgComparator: String = "Comparator"
    public val propCfgSoundKvant: String = "SoundKvant"
    public val propCfgLedKvant: String = "LedKvant"
    public val propCfgResolution: String = "Resolution"
    public val propCfgStartSpectrometr: String = "AutoStartSpectrometr"
    public val propCfgSMAWindow: String = "SMAWindow"
    public val propCfgRejectCann: String = "RejectConn"
    public val propCfgSaveSpecterType: String = "saveSpecterType"
    public val acuricyPatern : String = "###0.#######"
    public val propAquracy : String = "AquracyDozimeter"
    public val propBitsChan: String = "BitsOfChannel"

    public var receiveData: UByteArray = UByteArray(9760)
    public var LEMAC: String = ""
    public lateinit var mainContext: Context
    public lateinit var drawSPECTER: drawSpecter
    public lateinit var drawHISTORY: drawHistory
    public lateinit var drawDOZIMETER: drawDozimeter
    public lateinit var drawCURSOR: drawCursor
    public lateinit var drawLOG: drawLogs
    public lateinit var drawExamp: drawExmple
    public var drawObjectInit: Boolean = true
    public var drawObjectInitHistory: Boolean = true
    public var drawDozObjectInit: Boolean = true
    public var exampleObjectInit: Boolean = true
    public var pagerFrame: Int = 1
    public lateinit var BTT:  BluetoothInterface
    public var initBT: Boolean = false
    public var initDOZ: Boolean = false
    public lateinit var adapter: NumberAdapter
    public lateinit var textMACADR: EditText
    public lateinit var bColor: buttonColor
    public lateinit var indicatorBT: TextView
    public lateinit var viewPager: ViewPager2
    public lateinit var PP: propControl
    public lateinit var scanButton: Button
    public lateinit var btnSpecterSS: Button
    public var btnSpecterSSisInit: Boolean = false
    public lateinit var btnReadFromDevice: Button
    public lateinit var btnWriteToDevice: Button
    public lateinit var btnSetup: ImageButton
    public var needTerminate: Boolean = false
    public val tmFull = intervalTimer()
    public lateinit var txtStat1: TextView
    public lateinit var txtStat2: TextView
    public lateinit var txtStat3: TextView
    public lateinit var txtIsotopInfo: TextView         // Текст для вывода данных об изотопе
    public var configDataReady: Boolean = false         // Флаг готовности параметров из прибора
    public var propButtonInit: Boolean = false          // Флаг активности изменения состояния переключателей
    public var bluetoothRunning: Boolean = false        // Флаг активности таймера
    //private var saveStat1: String = ""
    //private var saveStat2: String = ""
    //private var saveStat3: String = ""

    /*
    *   Элементы управления закладки Setup
    */
    lateinit var rbGistogramSpectr: RadioButton
    lateinit var rbLineSpectr: RadioButton
    lateinit var cbSoundKvant: CheckBox
    lateinit var cbLedKvant: CheckBox
    lateinit var cbMarker: CheckBox
    lateinit var editPolinomA: EditText
    lateinit var editPolinomB: EditText
    lateinit var editPolinomC: EditText
    lateinit var editLevel1: EditText
    lateinit var editLevel2: EditText
    lateinit var editLevel3: EditText
    lateinit var cbSoundLevel1: CheckBox
    lateinit var cbSoundLevel2: CheckBox
    lateinit var cbSoundLevel3: CheckBox
    lateinit var cbVibroLevel1: CheckBox
    lateinit var cbVibroLevel2: CheckBox
    lateinit var cbVibroLevel3: CheckBox
    lateinit var editCPS2Rh: EditText
    lateinit var rbResolution1024: RadioButton
    lateinit var rbResolution2048: RadioButton
    lateinit var rbResolution4096: RadioButton
    lateinit var editHVoltage: EditText
    lateinit var editComparator: EditText
    lateinit var cbSpectrometr: CheckBox
    lateinit var editSMA: EditText
    lateinit var editRejectChann: EditText
    lateinit var rbSpctTypeBq: RadioButton
    lateinit var rbSpctTypeSPE: RadioButton
    lateinit var rbSpctType : RadioGroup
    lateinit var aqureEdit : EditText
    lateinit var bitsChannelEdit: EditText
    var aqureValue: Int = 100
    var bitsChannel:Int = 20
    /*
    *   Цвета для курсора
    */
    public var ColorEraseCursor: Int = 0
    public var ColorActiveCursor: Int = 0

    /*
    *   Цвета для графика дозиметра
    */
    public var ColorDosimeter: Int = 0              // Цвет графика дозиметра
    public var ColorDosimeterSMA: Int = 0           // Цвет графика SMA дозиметра
    /*
     *  Цвета для графика спектра типа линия
     */
    public var ColorLin: Int = 0                    // Цвет линейного графика
    public var ColorLog: Int = 0                    // Цвет логарифмического графика
    public var ColorFone: Int = 0                   // Цвет линейного графика фона
    public var ColorFoneLg: Int = 0                 // Цвет логарифмического графика фона
    /*
     *  Цвета для графика типа гистограмма
     */
    public var ColorLinGisto: Int = 0
    public var ColorLogGisto: Int = 0
    public var ColorFoneGisto: Int = 0
    public var ColorFoneLgGisto: Int = 0

    public var specterType: Int = 0                 // Разрешение спектра полученное из прибора
    public var specterGraphType: Int = 0            // Тип отображаемого спектра 0 - Линия, 1 - Гистограмма
    public var spectrResolution: Int = 0            // Разрешение спектра из настроек
    public var needCalibrate: Boolean = false       // Флаг для инициализации закладки настроек под текущее разрешение прибора
    public var sendCS: UShort = 0u
    public var PCounter: UInt = 0u                  // Всего принято частиц
    public var cps: Float = 0.0f                    // Среднее cps
    public var messTm:UInt = 0u                     // Время измерения
    public var spectrometerTime:UInt = 0u           // Время работы спектрометра
    public var spectrometerPulse: UInt = 0u         // Количество импульсов от спектрометра
    public var battLevel: Float = 0.0f              // Уровень батареии
    public var tempMC: Float = 0.0f                 // Температура МК
    public var pulsePerSec: UInt = 0u               // CPS за короткий интервал.
    public var rejectChann: Int = 10                // Количество каналов от начала, не отображаемых на гистограмме
    public var realResolution: Int = 30             // Разрешение на линии 662 кЭв в каналах

    /* Параметры для хранения в приборе */
    public var propSoundKvant: Boolean = false      // Озвучка прихода частицы
    public var propLedKvant: Boolean = true         // Подсветка прихода частицы
    public var propLevel1: Int = 0                  // Значение первого уровня
    public var propLevel2: Int = 0                  // Значение второго уровня
    public var propLevel3: Int = 0                  // Значение третьего уровня
    public var propSoundLevel1: Boolean = true      // Разрешение звука первого уровня
    public var propSoundLevel2: Boolean = true      // Разрешение звука второго уровня
    public var propSoundLevel3: Boolean = true      // Разрешение звука третьего уровня
    public var propVibroLevel1: Boolean = true      // Разрешение вибро первого уровня
    public var propVibroLevel2: Boolean = true      // Разрешение вибро второго уровня
    public var propVibroLevel3: Boolean = true      // Разрешение вибро третьего уровня
    public var propAutoStartSpectrometr: Boolean = false
    public var propCPS2UR: Float = 0.0f             // Коэффициент пересчета CPS в uRh
    public var propCoef1024A: Float = 0.0f          // Коэффициенты полинома преобразования канала в энергию
    public var propCoef1024B: Float = 0.0f
    public var propCoef1024C: Float = 0.0f
    public var propCoef2048A: Float = 0.0f
    public var propCoef2048B: Float = 0.0f
    public var propCoef2048C: Float = 0.0f
    public var propCoef4096A: Float = 0.0f
    public var propCoef4096B: Float = 0.0f
    public var propCoef4096C: Float = 0.0f
    public var propComparator: UShort = 0u          // Уровень компаратора
    public var propHVoltage: UShort = 0u            // Уровень высокого напряжения
    public var windowSMA: Int = 5

    public lateinit var btnSaveBQ: Button
    /*
    *   Формат сохранения спектра
    *   0 - BqMon
    *   1 - SPE
    */
    public var saveSpecterType: Int = 0
    public var saveSpecterType1: String = ""
    public var saveSpecterType2: String = ""

    /*
    * Формат буфера для передачи
                           * Значения заголовка и формат данных для передачи
                           *
                           * Управляющие данные uint8_t
                           * 0,1,2 - Маркер начала <B>
                           * 3 -  Тип передачи
                           *    0    - Данные дозиметра и лог (6 MTU)
                           *    1    - Данные дозиметра, лог и спектр 1024 (14 MTU)
                           *    2    - Данные дозиметра, лог и спектр 2048 (23 MTU)
                           *    3    - Данные дозиметра, лог и спектр 4096 (39 MTU)
                           *    4	 - Данные дозиметра, лог и исторический спектр 1024
                           *    5	 - Данные дозиметра, лог и исторический спектр 2048
                           *    6	 - Данные дозиметра, лог и исторический спектр 4096
                           * 4,5,6,7 - Зарезервировано
                           *
                           * Статистика и когфигурация uint16_t
                           *
                           * 5, 6   - Общее число импульсов от начала измерения uint32_t
                           * 7, 8   - Число импульсов за последнюю секунду. uint32_t
                           * 9, 10  - Общее время в секундах uint32_t
                           * 11, 12 - Среднее количество имльсов в секунду. float
                           * 13, 14 - Температура в гр. цельсия
                           * 15, 16 - Напряжение батареи в вольтах
                           * 17, 18 - Коэффициент полинома A для 1024 каналов
                           * 19, 20 - Коэффициент полинома B для 1024 каналов
                           * 21, 22 - Коэффициент полинома C для 1024 каналов
                           * 23, 24 - Коэффициент пересчета uRh/cps
                           * 25     - Значение высокого напряжения
                           * 26     - Значение порога компаратора
                           * 27     - Уровень первого порога
                           * 28     - Уровень второго порога
                           * 29     - Уровень третьего порога
                           * 30     - Битовая конфигурация
                           *     0 - Индикация кванта светодиодом
                           *     1 - Индикация кванта звуком
                           *     2 - Звук первого уровня
                           *     3 - Звук второго уровня
                           *     4 - Звук третьего уровня
                           *     5 - Вибро первого уровня
                           *     6 - Вибро второго уровня
                           *     7 - Вибро третьего уровня
                           *     8 - Автозапуск спектрометра при включении
                           * 31, 32 - Коэффициент полинома A для 2048 каналов
                           * 33, 34 - Коэффициент полинома B для 2048 каналов
                           * 35, 36 - Коэффициент полинома C для 2048 каналов
                           * 37, 38 - Коэффициент полинома A для 4096 каналов
                           * 39, 40 - Коэффициент полинома B для 4096 каналов
                           * 41, 42 - Коэффициент полинома C для 4096 каналов
                           * 43, 44 - Время работы спектрометра в секундах
                           * 45, 46 - Количество импульсов от спектрометра
                           *    47  - Погрешность измерения дозиметра
                           *  48(L) - Разрдность канала (младший байт)
                           * 49  - Конец заголовка
                           *
                           * 50  - Данные дозиметра
                           * 572 - Данные лога
                           * 652 - Данные спектра
                           *
                           * 652 или 1676 или 2700 или 4748 - Контрольная сумма.
    */


    public var HWpropSoundKvant: Boolean = false      // Озвучка прихода частицы
    public var HWpropLedKvant: Boolean = true         // Подсветка прихода частицы
    public var HWpropLevel1: Int = 0
    public var HWpropLevel2: Int = 0
    public var HWpropLevel3: Int = 0
    public var HWpropSoundLevel1: Boolean = true
    public var HWpropSoundLevel2: Boolean = true
    public var HWpropSoundLevel3: Boolean = true
    public var HWpropVibroLevel1: Boolean = true
    public var HWpropVibroLevel2: Boolean = true
    public var HWpropVibroLevel3: Boolean = true

    public var HWpropCPS2UR: Float = 0.0f

    public var HWpropHVoltage: UShort = 0u

    public var HWpropComparator: UShort = 0u

    public var HWspectrResolution: Int = 0

    public var HWpropAutoStartSpectrometr: Boolean = false

    public var HWCoef1024A: Float = 0.0f
    public var HWCoef1024B: Float = 0.0f
    public var HWCoef1024C: Float = 0.0f

    public var HWCoef2048A: Float = 0.0f
    public var HWCoef2048B: Float = 0.0f
    public var HWCoef2048C: Float = 0.0f

    public var HWCoef4096A: Float = 0.0f
    public var HWCoef4096B: Float = 0.0f
    public var HWCoef4096C: Float = 0.0f
    public var HWAqureValue: UShort = 0u
    public var HWBitsChan: UByte = 0u

    /*
    *   Вывод статистики для дозиметра и спектрометра
    */
    fun showStatistics() {
        /*
        *  Перевод в дни, часы, минуты, секунды
        */
        var dd: Int = GO.messTm.toInt() / 86400
        var hh: Int = (GO.messTm.toInt() - dd * 86400) /  3600
        var mm: Int = GO.messTm.toInt() / 60 % 60
        var ss: Int = GO.messTm.toInt() / 1 % 60

        var ddS: Int = GO.spectrometerTime.toInt() / 86400
        var hhS: Int = (GO.spectrometerTime.toInt() - ddS * 86400) /  3600
        var mmS: Int = GO.spectrometerTime.toInt() / 60 % 60
        var ssS: Int = GO.spectrometerTime.toInt() / 1 % 60

        var tmpStr: String
        if (GO.viewPager.currentItem == 0) {
            tmpStr = String.format("Time:%02d:%02d:%02d:%02d",  ddS, hhS, mmS, ssS)
        } else {
            tmpStr = String.format("Time:%02d:%02d:%02d:%02d",  dd, hh, mm, ss)
        }

        /*
        *   Вывод первой строки статистики
        */
        if (GO.battLevel < 3.0f) {  // Уровень батареи низкий
            GO.txtStat1.setText(Html.fromHtml("${GO.tempMC.toInt()}&#176C   <font color=#C80000> ${GO.battLevel} v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else if (GO.battLevel < 3.5f) { // Уровнь батареи ниже 50%
            GO.txtStat1.setText(Html.fromHtml("${GO.tempMC.toInt()}&#176C   <font color=#ffff00> ${GO.battLevel} v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else {
            GO.txtStat1.setText(Html.fromHtml("${GO.tempMC.toInt()}&#176C   <font color=#00ff00> ${GO.battLevel} v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        }

        /*
        *   Вывод второй строки статистики
        */
        var aquracy3S: Double
        var cpsS: Float
        var pulseS: Int
        if (GO.viewPager.currentItem == 0) {        // Статистика для спектрометра
            /* Расчет погрешности по трем сигмам для спектрометра */
            aquracy3S = 300.0 / Math.sqrt(GO.spectrometerPulse.toDouble())
            cpsS = GO.spectrometerPulse.toFloat() / GO.spectrometerTime.toFloat()
            pulseS = GO.spectrometerPulse.toInt()
        } else { // Статистика для дозиметра
            /* Расчет погрешности по трем сигмам для дозиметра */
            aquracy3S = 300.0 / Math.sqrt(GO.PCounter.toDouble())
            cpsS = GO.cps
            pulseS = GO.PCounter.toInt()
        }
        GO.txtStat2.setText(String.format("Total:%d(%.2f%%) Avg:%.2f", pulseS, aquracy3S, cpsS))
    }

    /*
    *   Справочник изотопов
    */
    public data class IsotopsCls (
        var Energy: Int,
        var Name: String,
        var Activity: Int,
        var Channel: Int
    )
    public var isotopSize: Int = 47     // Количество записей в справочнике изотопов.
    public var isotopDelta: Int = 3     // Погрешность поиска в справочнике.
    val isotopList = Array(isotopSize) {IsotopsCls(0, "", 0, 0)}

    /*
    *   Функция находит изотоп по энергии в заданном диапазоне точности.
    *   Выдается первый найденный изотоп
    */
    fun findIsotop(Energy: Int): IsotopsCls {
        var fndIsotop: IsotopsCls = IsotopsCls(0, "", 0, 0)
        for (ii in 0 until isotopSize ) {
            if ((isotopList[ii].Energy >  Energy - isotopDelta) and (isotopList[ii].Energy <  Energy + isotopDelta)) {
                fndIsotop.Energy = isotopList[ii].Energy
                fndIsotop.Name = isotopList[ii].Name
                fndIsotop.Activity = isotopList[ii].Activity
                fndIsotop.Channel = isotopList[ii].Channel
                break
            }
        }
        return fndIsotop
    }

    /*
    *   Функция подготавливает справочник изотопов
    */
    fun loadIsotop() {
        var idxIst = 0
        isotopList[idxIst++] = IsotopsCls(26, "Am-241", 0, 0)                           // 0
        isotopList[idxIst++] = IsotopsCls(30, "I-131", 0, 0)                            // 1
        isotopList[idxIst++] = IsotopsCls(32, "Cs-137, Ba-137", 0, 0)                   // 2
        isotopList[idxIst++] = IsotopsCls(35, "I-125", 0, 0)                            // 3
        isotopList[idxIst++] = IsotopsCls(55, "Lu-176", 0, 0)                           // 4
        isotopList[idxIst++] = IsotopsCls(59, "Am-241", 0, 0)                           // 5
        isotopList[idxIst++] = IsotopsCls(75, "Pa-234m, U-238", 0, 0)                   // 6
        isotopList[idxIst++] = IsotopsCls(81, "Xe-133", 0, 0)                           // 7
        isotopList[idxIst++] = IsotopsCls(141, "Tc-99", 0, 0)                           // 8
        isotopList[idxIst++] = IsotopsCls(160, "I-123", 0, 0)                           // 9
        isotopList[idxIst++] = IsotopsCls(171, "In-111", 0, 0)                          // 10
        isotopList[idxIst++] = IsotopsCls(186, "Ra-226, Bi-214, Pb-214", 0, 0)          // 11
        isotopList[idxIst++] = IsotopsCls(190, "U-235, U-238, Pa-234m", 0, 0)           // 12
        isotopList[idxIst++] = IsotopsCls(202, "Lu-176", 0, 0)                          // 13
        isotopList[idxIst++] = IsotopsCls(208, "Lu-177", 0, 0)                          // 14
        isotopList[idxIst++] = IsotopsCls(238, "Th-232, Ac-228, Tl-208", 0, 0)          // 15
        isotopList[idxIst++] = IsotopsCls(242, "Ra-226, Pb-214, Bi-214", 0, 0)          // 16
        isotopList[idxIst++] = IsotopsCls(245, "In-111", 0, 0)                          // 17
        isotopList[idxIst++] = IsotopsCls(295, "Ra-226, Pb-214, Bi-214", 0, 0)          // 18
        isotopList[idxIst++] = IsotopsCls(296, "Ir-192", 0, 0)                          // 19
        isotopList[idxIst++] = IsotopsCls(307, "Lu-176", 0, 0)                          // 20
        isotopList[idxIst++] = IsotopsCls(308, "Ir-192", 0, 0)                          // 21
        isotopList[idxIst++] = IsotopsCls(317, "Ir-192", 0, 0)                          // 22
        isotopList[idxIst++] = IsotopsCls(338, "Pb-212, Th-232, Ac-228, Tl-208", 0, 0)  // 23
        isotopList[idxIst++] = IsotopsCls(351, "Ra-226, Pb-214, Bi-214", 0, 0)          // 24
        isotopList[idxIst++] = IsotopsCls(364, "I-131", 0, 0)                           // 25
        isotopList[idxIst++] = IsotopsCls(392, "In-113m", 0, 0)                         // 26
        isotopList[idxIst++] = IsotopsCls(412, "Au-198", 0, 0)                          // 27
        isotopList[idxIst++] = IsotopsCls(468, "Ir-192", 0, 0)                          // 28
        isotopList[idxIst++] = IsotopsCls(511, "Annihilation", 0, 0)                    // 29
        isotopList[idxIst++] = IsotopsCls(538, "Pb-212,Th-232,Ac-228", 0, 0)            // 30
        isotopList[idxIst++] = IsotopsCls(583, "Tl-208, Th-232, Ac-228", 0, 0)          // 31
        isotopList[idxIst++] = IsotopsCls(609, "Ra-226, Pb-214, Bi-214", 0, 0)          // 32
        isotopList[idxIst++] = IsotopsCls(662, "Ba-137, Cs-137", 1683, 0)               // 33
        isotopList[idxIst++] = IsotopsCls(750, "U-238, U-235, Pa-234m", 0, 0)           // 34
        isotopList[idxIst++] = IsotopsCls(911, "Th-232, Pb-212, Ac-228, Tl-208", 0, 0)  // 35
        isotopList[idxIst++] = IsotopsCls(920, "Tl-20", 0, 0)                           // 36
        isotopList[idxIst++] = IsotopsCls(1001, "U-238, U-235, Pa-234m", 0, 0)          // 37
        isotopList[idxIst++] = IsotopsCls(1120, "Ra-226, Pb-214, Bi-214", 0, 0)         // 38
        isotopList[idxIst++] = IsotopsCls(1173, "Co-60", 0, 0)                          // 39
        isotopList[idxIst++] = IsotopsCls(1332, "Co-60", 0, 0)                          // 40
        isotopList[idxIst++] = IsotopsCls(1460, "K-40", 0, 0)                           // 41
        isotopList[idxIst++] = IsotopsCls(1588, "Th-232, Ac-228", 0, 0)                 // 42
        isotopList[idxIst++] = IsotopsCls(1600, "Th-232, Pb-212, Ac-228, Tl-208", 0, 0) // 43
        isotopList[idxIst++] = IsotopsCls(1760, "Ra-226, Pb-214, Bi-214", 0, 0)         // 44
        isotopList[idxIst++] = IsotopsCls(2200, "Ra-226, Pb-214, Bi-214", 0, 0)         // 45
        isotopList[idxIst++] = IsotopsCls(2614, "Th-232, Pb-212, Ac-228, Tl-208", 0, 0) // 46

        /* Пересчитать канал для изотопов с активностью */
        var cA = 0.0f
        var cB = 0.0f
        var cC = 0.0f
        var DD = 0.0f
        for (ii in 0 until isotopSize) {
            if (isotopList[ii].Activity != 0) {         // Активность не нулевая, требуется получить номер канала для правильного выделения пика
                when (GO.spectrResolution) {
                    0 -> {  // Разрешение 1024
                        cA = GO.propCoef1024A
                        cB = GO.propCoef1024B
                        cC = GO.propCoef1024C
                    }
                    1 -> {  // Разрешение 2048
                        cA = GO.propCoef2048A
                        cB = GO.propCoef2048B
                        cC = GO.propCoef2048C
                    }
                    2 -> {  // Разрешение 4096
                        cA = GO.propCoef4096A
                        cB = GO.propCoef4096B
                        cC = GO.propCoef4096C
                    }
                }
                if ((cA != 0.0f) and (cB != 0.0f)) {
                    /* Дискриминант */
                    DD = cB.pow(2.0f) - 4 * cA * (cC - isotopList[ii].Energy)
                    /* Если имеются реальные корни, выбираем наибольшее значение */
                    if (DD > 0) {
                        var x1 = (-cB + sqrt(DD)) / (2 * cA)
                        var x2 = (-cB - sqrt(DD)) / (2 * cA)
                        isotopList[ii].Channel = max(x1.toInt(), x2.toInt())
                    }
                }
            }
        }
    }

    /*
    *   Чтение параметров прибора в закладку Setup
    */
    fun readConfigFormDevice() {
        GO.propLedKvant = GO.HWpropLedKvant
        //Log.d("BluZ-BT","LedKvant: ${GO.HWpropLedKvant}")
        GO.propSoundKvant = GO.HWpropSoundKvant
        GO.propCoef1024A = GO.HWCoef1024A
        GO.propCoef1024B = GO.HWCoef1024B
        GO.propCoef1024C = GO.HWCoef1024C
        GO.propCoef2048A = GO.HWCoef2048A
        GO.propCoef2048B = GO.HWCoef2048B
        GO.propCoef2048C = GO.HWCoef2048C
        GO.propCoef4096A = GO.HWCoef4096A
        GO.propCoef4096B = GO.HWCoef4096B
        GO.propCoef4096C = GO.HWCoef4096C

        GO.propLevel1 = GO.HWpropLevel1
        GO.propLevel2 = GO.HWpropLevel2
        GO.propLevel3 = GO.HWpropLevel3

        GO.propSoundLevel1 = GO.HWpropSoundLevel1
        GO.propSoundLevel2 = GO.HWpropSoundLevel2
        GO.propSoundLevel3 = GO.HWpropSoundLevel3

        GO.propVibroLevel1 = GO.HWpropVibroLevel1
        GO.propVibroLevel2 = GO.HWpropVibroLevel2
        GO.propVibroLevel3 = GO.HWpropVibroLevel3

        GO.propCPS2UR = GO.HWpropCPS2UR
        GO.propHVoltage = GO.HWpropHVoltage
        GO.propComparator = GO.HWpropComparator
        GO.propAutoStartSpectrometr = GO.HWpropAutoStartSpectrometr
        GO.aqureValue = GO.HWAqureValue.toInt()
        if (GO.HWBitsChan.toInt() < 16 || GO.HWBitsChan.toInt() > 32) {
            GO.bitsChannel = 20
        } else {
            GO.bitsChannel = GO.HWBitsChan.toInt()
        }

        /* Текущее разрешение спектрометра */
        GO.spectrResolution = GO.HWspectrResolution
    }
    /*
    *   Запись всех параметров в конфигурационный файл
    */
    fun writeConfigParameters() {
        /* Сохраняем MAC адрес */
        GO.LEMAC = GO.textMACADR.text.toString()
        //Log.d("BluZ-BT", "Reject chann: " + GO.rejectChann )
        GO.PP.setPropInt(propCfgRejectCann, GO.rejectChann)                    // Сохраним количество не отображаемых каналов
        GO.PP.setPropStr(propCfgADDRESS, GO.LEMAC)                             // Сохраним MAC адрес устройства
        GO.PP.setPropInt(propCfgColorSpecterLin, GO.ColorLin)                  // Сохраним цвет линейного графика
        GO.PP.setPropInt(propCfgColorSpecterLog, GO.ColorLog)                  // Сохраним цвет логарифмического графика
        GO.PP.setPropInt(propCfgColorSpecterFone, GO.ColorFone)                // Сохраним цвет графика фона
        GO.PP.setPropInt(propCfgColorSpecterFoneLg, GO.ColorFoneLg)            // Сохраним цвет логарифмического графика фона
        GO.PP.setPropInt(propCfgColorSpecterLinGisto, GO.ColorLinGisto)        // Сохраним цвет линейного графика гистограммы
        GO.PP.setPropInt(propCfgColorSpecterLogGisto, GO.ColorLogGisto)        // Сохраним цвет логарифмического графика  гистограммы
        GO.PP.setPropInt(propCfgColorSpecterFoneGisto, GO.ColorFoneGisto)      // Сохраним цвет графика фона гистограммы
        GO.PP.setPropInt(propCfgColorSpecterFoneLgGisto, GO.ColorFoneLgGisto)  // Сохраним цвет логарифмического графика фона гистограммы
        GO.PP.setPropInt(propCfgColorDozimeter, GO.ColorDosimeter)             // Сохраним цвет дозиметра
        GO.PP.setPropInt(propCfgColorDozimeterSMA, GO.ColorDosimeterSMA)       // Сохраним цвет дозиметра
        GO.PP.setPropInt(propCfgSaveSpecterType, GO.saveSpecterType)
        GO.PP.setPropInt(propCfgResolution, GO.spectrResolution)
        GO.PP.setPropInt(propCfgComparator, GO.propComparator.toInt())     // Уровень Компаратора
        GO.PP.setPropInt(propCfgHV, GO.propHVoltage.toInt())               // Уровень высокого напряжения
        GO.PP.setPropInt(propCfgSMAWindow, GO.windowSMA)
        GO.PP.setPropFloat(propCfgCoef1024A, GO.propCoef1024A)             // A - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef1024B, GO.propCoef1024B)             // B - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef1024C, GO.propCoef1024C)             // C - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef2048A, GO.propCoef2048A)             // A - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef2048B, GO.propCoef2048B)             // B - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef2048C, GO.propCoef2048C)             // C - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef4096A, GO.propCoef4096A)             // A - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef4096B, GO.propCoef4096B)             // B - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCoef4096C, GO.propCoef4096C)             // C - полинома пересчета канала в энергию
        GO.PP.setPropFloat(propCfgCPS2UR, GO.propCPS2UR)                   // Коэффициент пересчета cps в uRh
        GO.PP.setPropBoolean(propCfgVibroLevel1, GO.propVibroLevel1)       // Вибро первого порога
        GO.PP.setPropBoolean(propCfgVibroLevel2, GO.propVibroLevel2)       // Вибро второго порога
        GO.PP.setPropBoolean(propCfgVibroLevel3, GO.propVibroLevel3)       // Вибро третьего порога
        GO.PP.setPropBoolean(propCfgSoundLevel1, GO.propSoundLevel1)       // Звук первого порога
        GO.PP.setPropBoolean(propCfgSoundLevel2, GO.propSoundLevel2)       // Звук второго порога
        GO.PP.setPropBoolean(propCfgSoundLevel3, GO.propSoundLevel3)       // Звук третьего порога
        GO.PP.setPropInt(propCfgLevel1, GO.propLevel1)                     // Значение первого порога
        GO.PP.setPropInt(propCfgLevel2, GO.propLevel2)                     // Значение второго порога
        GO.PP.setPropInt(propCfgLevel3, GO.propLevel3)                     // Значение третьего порога
        GO.PP.setPropBoolean(propCfgStartSpectrometr, GO.propAutoStartSpectrometr)
        GO.PP.setPropBoolean(propCfgLedKvant, GO.propLedKvant)
        GO.PP.setPropBoolean(propCfgSoundKvant, GO.propSoundKvant)
        GO.PP.setPropInt(propCfgSpectrGraphType, GO.specterGraphType)
        GO.PP.setPropInt(propAquracy, GO.aqureValue)    // Точность усреднения для дозиметра, количество импульсов.
        GO.PP.setPropInt(propBitsChan, GO.bitsChannel)  // Количество  бит в канале
    }

    /*
    *   Чтение всех параметров из конфигурационного файла
    */
    fun readConfigParameters() {
        GO.LEMAC = GO.PP.getPropStr(propCfgADDRESS)
        /* Цвета для дозиметра */
        GO.ColorDosimeter = GO.PP.getPropInt(propCfgColorDozimeter)
        if (GO.ColorDosimeter == 0) {
            GO.ColorDosimeter = mainContext.resources.getColor(R.color.ColorDosimeter, GO.mainContext.theme)
        }
        GO.ColorDosimeterSMA = GO.PP.getPropInt(propCfgColorDozimeterSMA)
        if (GO.ColorDosimeterSMA == 0) {
            GO.ColorDosimeterSMA = mainContext.resources.getColor(R.color.ColorDosimeterSMA, GO.mainContext.theme)
        }

        /* Цвета для линейного графика */
        GO.ColorLin = GO.PP.getPropInt(propCfgColorSpecterLin)
        GO.ColorLog = GO.PP.getPropInt(propCfgColorSpecterLog)
        GO.ColorFone = GO.PP.getPropInt(propCfgColorSpecterFone)
        GO.ColorFoneLg = GO.PP.getPropInt(propCfgColorSpecterFoneLg)
        /* Цвета для гистограммы */
        GO.ColorLinGisto = GO.PP.getPropInt(propCfgColorSpecterLinGisto)
        GO.ColorLogGisto = GO.PP.getPropInt(propCfgColorSpecterLogGisto)
        GO.ColorFoneGisto = GO.PP.getPropInt(propCfgColorSpecterFoneGisto)
        GO.ColorFoneLgGisto = GO.PP.getPropInt(propCfgColorSpecterFoneLgGisto)

        /* Тип графика спектра: линейный, гистограмма */
        GO.specterGraphType = GO.PP.getPropInt(propCfgSpectrGraphType)
        GO.rejectChann = GO.PP.getPropInt(propCfgRejectCann)
        //Log.d("BluZ-BT", "Reject chann: " +GO.rejectCann )
        if (! bluetoothRunning) {
            bluetoothRunning = true
            GO.BTT = BluetoothInterface(GO.indicatorBT)
        }
        /*
        *       Параметры прибора
        */
        GO.propSoundKvant = GO.PP.getPropBoolean(propCfgSoundKvant)
        GO.propLedKvant = GO.PP.getPropBoolean(propCfgLedKvant)
        GO.propAutoStartSpectrometr = GO.PP.getPropBoolean(propCfgStartSpectrometr)
        GO.propSoundLevel1 = GO.PP.getPropBoolean(propCfgSoundLevel1)
        GO.propSoundLevel2 = GO.PP.getPropBoolean(propCfgSoundLevel2)
        GO.propSoundLevel3 = GO.PP.getPropBoolean(propCfgSoundLevel3)
        GO.propVibroLevel1 = GO.PP.getPropBoolean(propCfgVibroLevel1)
        GO.propVibroLevel2 = GO.PP.getPropBoolean(propCfgVibroLevel2)
        GO.propVibroLevel3 = GO.PP.getPropBoolean(propCfgVibroLevel3)
        GO.propLevel1 = GO.PP.getPropInt(propCfgLevel1)
        GO.propLevel2 = GO.PP.getPropInt(propCfgLevel2)
        GO.propLevel3 = GO.PP.getPropInt(propCfgLevel3)
        GO.propCoef1024A = GO.PP.getPropFloat(propCfgCoef1024A)
        GO.propCoef1024B = GO.PP.getPropFloat(propCfgCoef1024B)
        GO.propCoef1024C = GO.PP.getPropFloat(propCfgCoef1024C)
        GO.propCoef2048A = GO.PP.getPropFloat(propCfgCoef2048A)
        GO.propCoef2048B = GO.PP.getPropFloat(propCfgCoef2048B)
        GO.propCoef2048C = GO.PP.getPropFloat(propCfgCoef2048C)
        GO.propCoef4096A = GO.PP.getPropFloat(propCfgCoef4096A)
        GO.propCoef4096B = GO.PP.getPropFloat(propCfgCoef4096B)
        GO.propCoef4096C = GO.PP.getPropFloat(propCfgCoef4096C)
        GO.propCPS2UR = GO.PP.getPropFloat(propCfgCPS2UR)
        GO.propHVoltage = GO.PP.getPropInt(propCfgHV).toUShort()
        GO.propComparator = GO.PP.getPropInt(propCfgComparator).toUShort()
        GO.spectrResolution = GO.PP.getPropInt(propCfgResolution)
        GO.windowSMA = GO.PP.getPropInt(propCfgSMAWindow)
        if (GO.windowSMA < 3) {
            GO.windowSMA = 3
        }

        /*
        *   Формат для сохранения спектра
        *   0 - BqMon
        *   1 - CSV
        */
        GO.saveSpecterType = GO.PP.getPropInt(propCfgSaveSpecterType)
        GO.aqureValue = GO.PP.getPropInt(propAquracy)
        GO.bitsChannel = GO.PP.getPropInt(propBitsChan)
        if (GO.bitsChannel < 16 || GO.bitsChannel > 32) {
            GO.bitsChannel = 20
        }

    }

    /* Запуск таймера для автоматического подключения */
    fun startBluetoothTimer() {
        GO.needTerminate = false
        Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())

        if (GO.LEMAC.length == 17 &&  GO.LEMAC[0] != 'X') { // MAC адрес настроен, продолжаем работу.
            Log.d("BluZ-BT", "Start timer")
            GO.tmFull.startTimer();
        } else {                                            // MAC адрес не настроен, переходим к настройкам
            //GO.viewPager.setCurrentItem(0, false)
            //GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnSetup)
            Toast.makeText(GO.mainContext, "MAC address not set.\nScan your device.", Toast.LENGTH_LONG ).show()
            GO.txtStat1.visibility = View.INVISIBLE
            GO.txtStat2.visibility = View.INVISIBLE
            GO.txtStat3.visibility = View.INVISIBLE
            GO.txtIsotopInfo.visibility = View.INVISIBLE
            GO.viewPager.setCurrentItem(4, false)
        }
    }
}