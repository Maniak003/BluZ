package ru.starline.bluz

import android.content.ClipData.Item
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.loader.content.Loader.ForceLoadContentObserver
import androidx.viewpager2.widget.ViewPager2
import java.sql.Array
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

    public var receiveData: UByteArray = UByteArray(9760)
    public var LEMAC: String = ""
    public lateinit var mainContext: Context
    public lateinit var drawSPECTER: drawSpecter
    public lateinit var drawDOZIMETER: drawDozimeter
    public lateinit var drawCURSOR: drawCursor
    public lateinit var drawLOG: drawLogs
    public var drawObjectInit: Boolean = true
    public var drawDozObjectInit: Boolean = true
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
    public var configDataReady: Boolean = false         // Флаг готовности параметров из прибора
    public var propButtonInit: Boolean = false          // Флаг активности изменения состояния переключателей
    private var saveStat1: String = ""
    private var saveStat2: String = ""
    private var saveStat3: String = ""

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
    /*
    *   Цвета для курсора
    */
    public var ColorEraseCursor: Int = 0
    public var ColorActiveCursor: Int = 0

    /*
    *   Цвета для графика дозиметра
    */
    public var ColorDosimeter: Int = 0
    public var ColorDosimeterSMA: Int = 0
    /*
     *  Цвета для графика спектра типа линия
     */
    public var ColorLin: Int = 0
    public var ColorLog: Int = 0
    public var ColorFone: Int = 0
    public var ColorFoneLg: Int = 0
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
    public var sendCS: UShort = 0u
    public var PCounter: UInt = 0u                  // Всего принято частиц
    public var cps: Float = 0.0f                    // Среднее cps
    public var messTm:UInt = 0u                     // Время измерения
    public var battLevel: Float = 0.0f              // Уровень батареии
    public var tempMC: Float = 0.0f                 // Температура МК
    public var pulsePerSec: UInt = 0u               // CPS за короткий интервал.
    public var rejectChann: Int = 10                // Количество каналов от начала, не отображаемых на гистограмме

    /* Параметры для хранения в приборе */
    public var propSoundKvant: Boolean = false      // Озвучка прихода частицы
    public var propLedKvant: Boolean = true         // Подсветка прихода частицы
    public var propLevel1: Int = 0
    public var propLevel2: Int = 0
    public var propLevel3: Int = 0
    public var propSoundLevel1: Boolean = true
    public var propSoundLevel2: Boolean = true
    public var propSoundLevel3: Boolean = true
    public var propVibroLevel1: Boolean = true
    public var propVibroLevel2: Boolean = true
    public var propVibroLevel3: Boolean = true
    public var propAutoStartSpectrometr: Boolean = false
    public var propCPS2UR: Float = 0.0f
    public var propCoef1024A: Float = 0.0f
    public var propCoef1024B: Float = 0.0f
    public var propCoef1024C: Float = 0.0f
    public var propCoef2048A: Float = 0.0f
    public var propCoef2048B: Float = 0.0f
    public var propCoef2048C: Float = 0.0f
    public var propCoef4096A: Float = 0.0f
    public var propCoef4096B: Float = 0.0f
    public var propCoef4096C: Float = 0.0f
    public var propComparator: UShort = 0u
    public var propHVoltage: UShort = 0u
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

    /*
    *   Справочник изотопов
    */
    public data class IsotopsCls (
        var Energy: Int,
        var Name: String,
        var Activity: Int,
        var Channel: Int
    )
    public var isotopSize: Int = 47
    public var isotopDelta: Int = 3
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
        isotopList[0].Energy = 26
        isotopList[0].Name = "Am-241"
        isotopList[1].Energy = 30
        isotopList[1].Name = "I-131"
        isotopList[2].Energy = 32
        isotopList[2].Name = "Cs-137, Ba-137"
        isotopList[3].Energy = 35
        isotopList[3].Name = "I-125"
        isotopList[4].Energy = 55
        isotopList[4].Name = "Lu-176"
        isotopList[5].Energy = 59
        isotopList[5].Name = "Am-241"
        isotopList[6].Energy = 75
        isotopList[6].Name = "Pa-234m, U-238"
        isotopList[7].Energy = 81
        isotopList[7].Name = "Xe-133"
        isotopList[8].Energy = 141
        isotopList[8].Name = "Tc-99"
        isotopList[9].Energy = 160
        isotopList[9].Name = "I-123"
        isotopList[10].Energy = 171
        isotopList[10].Name = "In-111"
        isotopList[11].Energy = 186
        isotopList[11].Name = "Ra-226, Bi-214, Pb-214"
        isotopList[12].Energy = 190
        isotopList[12].Name = "U-235, U-238, Pa-234m"
        isotopList[13].Energy = 202
        isotopList[13].Name = "Lu-176"
        isotopList[14].Energy = 208
        isotopList[14].Name = "Lu-177"
        isotopList[15].Energy = 238
        isotopList[15].Name = "Th-232, Ac-228, Tl-208"
        isotopList[16].Energy = 242
        isotopList[16].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[17].Energy = 245
        isotopList[17].Name = "In-111"
        isotopList[18].Energy = 295
        isotopList[18].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[19].Energy = 296
        isotopList[19].Name = "Ir-192"
        isotopList[20].Energy = 307
        isotopList[20].Name = "Lu-176"
        isotopList[21].Energy = 308
        isotopList[21].Name = "Ir-192"
        isotopList[22].Energy = 317
        isotopList[22].Name = "Ir-192"
        isotopList[23].Energy = 338
        isotopList[23].Name = "Pb-212, Th-232, Ac-228, Tl-208"
        isotopList[24].Energy = 351
        isotopList[24].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[25].Energy = 364
        isotopList[25].Name = "I-131"
        isotopList[26].Energy = 392
        isotopList[26].Name = "In-113m"
        isotopList[27].Energy = 412
        isotopList[27].Name = "Au-198"
        isotopList[28].Energy = 468
        isotopList[28].Name = "Ir-192"
        isotopList[29].Energy = 511
        isotopList[29].Name = "Annihilation"
        isotopList[30].Energy = 538
        isotopList[30].Name = "Pb-212,Th-232,Ac-228"
        isotopList[31].Energy = 583
        isotopList[31].Name = "Tl-208, Th-232, Ac-228"
        isotopList[32].Energy = 609
        isotopList[32].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[33].Energy = 662
        isotopList[33].Name = "Ba-137, Cs-137"
        isotopList[33].Activity = 838
        isotopList[34].Energy = 750
        isotopList[34].Name = "U-238, U-235, Pa-234m"
        isotopList[35].Energy = 911
        isotopList[35].Name = "Th-232, Pb-212, Ac-228, Tl-208"
        isotopList[36].Energy = 920
        isotopList[36].Name = "Tl-20"
        isotopList[37].Energy = 1001
        isotopList[37].Name = "U-238, U-235, Pa-234m"
        isotopList[38].Energy = 1120
        isotopList[38].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[39].Energy = 1173
        isotopList[39].Name = "Co-60"
        isotopList[40].Energy = 1332
        isotopList[40].Name = "Co-60"
        isotopList[41].Energy = 1460
        isotopList[41].Name = "K-40"
        isotopList[42].Energy = 1588
        isotopList[42].Name = "Th-232, Ac-228"
        isotopList[43].Energy = 1600
        isotopList[43].Name = "Th-232, Pb-212, Ac-228, Tl-208"
        isotopList[44].Energy = 1760
        isotopList[44].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[45].Energy = 2200
        isotopList[45].Name = "Ra-226, Pb-214, Bi-214"
        isotopList[46].Energy = 2614
        isotopList[46].Name = "Th-232, Pb-212, Ac-228, Tl-208"

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
        cbLedKvant.isChecked = GO.HWpropLedKvant
        cbSoundKvant.isChecked = GO.HWpropSoundKvant
        if (rbResolution1024.isChecked) {
            editPolinomA.setText(GO.HWCoef1024A.toString())
            editPolinomB.setText(GO.HWCoef1024B.toString())
            editPolinomC.setText(GO.HWCoef1024C.toString())
        } else if (rbResolution2048.isChecked) {
            editPolinomA.setText(GO.HWCoef2048A.toString())
            editPolinomB.setText(GO.HWCoef2048B.toString())
            editPolinomC.setText(GO.HWCoef2048C.toString())
        } else if (rbResolution4096.isChecked) {
            editPolinomA.setText(GO.HWCoef4096A.toString())
            editPolinomB.setText(GO.HWCoef4096B.toString())
            editPolinomC.setText(GO.HWCoef4096C.toString())
        }
        editLevel1.setText(GO.HWpropLevel1.toString())
        editLevel2.setText(GO.HWpropLevel2.toString())
        editLevel3.setText(GO.HWpropLevel3.toString())
        cbSoundLevel1.isChecked = GO.HWpropSoundLevel1
        cbSoundLevel2.isChecked = GO.HWpropSoundLevel2
        cbSoundLevel3.isChecked = GO.HWpropSoundLevel3
        cbVibroLevel1.isChecked = GO.HWpropVibroLevel1
        cbVibroLevel2.isChecked = GO.HWpropVibroLevel2
        cbVibroLevel3.isChecked = GO.HWpropVibroLevel3
        editCPS2Rh.setText(HWpropCPS2UR.toString())
        //rbResolution1024
        //rbResolution2048
        //rbResolution4096
        editHVoltage.setText(GO.HWpropHVoltage.toString())
        editComparator.setText(GO.HWpropComparator.toString())
        cbSpectrometr.isChecked = GO.HWpropAutoStartSpectrometr

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
        GO.BTT = BluetoothInterface(GO.indicatorBT)
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
        *   1 - SPE
        */
        GO.saveSpecterType = GO.PP.getPropInt(propCfgSaveSpecterType)

        GO.needTerminate = false
        Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())

        if (GO.LEMAC.length == 17 &&  GO.LEMAC[0] != 'X') { // MAC адрес настроен, продолжаем работу.
            GO.tmFull.startTimer();
        } else {                                            // MAC адрес не настроен, переходим к настройкам
            //GO.viewPager.setCurrentItem(0, false)
            //GO.viewPager.setCurrentItem(4, false)
            GO.bColor.resetToDefault()
            GO.bColor.setToActive(GO.btnSetup)
            Toast.makeText(GO.mainContext, "MAC address not set.\nScan your device.", Toast.LENGTH_LONG ).show()
            GO.viewPager.setCurrentItem(4, false)
        }
    }
}