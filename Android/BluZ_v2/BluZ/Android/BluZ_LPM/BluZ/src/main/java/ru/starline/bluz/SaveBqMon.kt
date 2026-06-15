package ru.starline.bluz

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

/**
 * Экспорт текущего спектра в файл.
 *
 * **Форматы:**
 *  - BqMon XML (`saveSpecterType == 0`) — XML-формат, совместимый со спектральными
 *    программами (BqMon, Theremino, Polimaster). Включает метаданные: время, GPS-координаты,
 *    коэффициенты калибровки, серийный номер устройства (GUID).
 *  - SPE / CSV (`saveSpecterType == 1`) — простой CSV `channel,counts` на строку, читается
 *    большинством анализаторов спектра.
 *
 * **Папка:** `Environment.DIRECTORY_DOCUMENTS/BluZ/` (создаётся при необходимости).
 *
 * **Имя файла:** `yyyyMMdd_HHmmss.{xml|csv}` — текущее время.
 *
 * **GPS** для метаданных берётся напрямую через `LocationManager.GPS_PROVIDER.getLastKnownLocation`.
 * Если недоступно — пишется "Unknown".
 */
class SaveBqMon {

    var tmpVal: Float = 0f

    /*
    *   Сохранение спектра с учетом разрешения.
    */
    /**
     * Диспетчер сохранения спектра: выбирает формат (XML или CSV) по [globalObj.saveSpecterType]
     * и разрешение по [globalObj.HWspectrResolution] → вызывает [saveHistogramXML] или
     * [saveHistogramSPE] с соответствующим количеством каналов.
     */
    fun saveSpecter() {
        when (GO.HWspectrResolution) {
            0 -> {  /* Разрешение 1024 */
                if (GO.saveSpecterType == 0) {
                    saveHistogramXML(GO.mainContext, GO.drawSPECTER.spectrData, 1024)
                } else {
                    saveHistogramSPE(GO.mainContext, GO.drawSPECTER.spectrData,  1024)
                }
            }
            1 -> {  /* Разрешение 2048 */
                if (GO.saveSpecterType == 0) {
                    saveHistogramXML(GO.mainContext, GO.drawSPECTER.spectrData, 2048)
                } else {
                    saveHistogramSPE(GO.mainContext, GO.drawSPECTER.spectrData,  2048)
                }
            }
            2 -> {  /* Разрешение 4096 */
                if (GO.saveSpecterType == 0) {
                    saveHistogramXML(GO.mainContext, GO.drawSPECTER.spectrData, 4096)
                } else {
                    saveHistogramSPE(GO.mainContext, GO.drawSPECTER.spectrData,  4096)
                }
            }
            else -> {
                if (GO.saveSpecterType == 0) {
                    saveHistogramXML(GO.mainContext, GO.drawSPECTER.spectrData, 1024)
                } else {
                    saveHistogramSPE(GO.mainContext, GO.drawSPECTER.spectrData,  1024)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    /**
     * Сохраняет спектр как CSV: одна строка на канал в формате `"channel,counts\n"`.
     * Имя файла: `yyyyMMdd_HHmmss.csv` в `/Documents/BluZ/`.
     *
     * Перед сохранением проверяет, что сумма по каналам > 0 — иначе Toast «The spectrum
     * does not contain data» и сохранение отменяется.
     */
    fun saveHistogramSPE(context: Context, spectrData: DoubleArray, resolution: Int) {
        var sm: Double = 0.0
        for (i in 0 until resolution) {
            sm += spectrData.get(i)
        }
        if (sm == 0.0) {
            Toast.makeText(context, "The spectrum does not contain data. Unloading is not possible.", Toast.LENGTH_SHORT).show()
            return
        }
        var dataStr: String
        val calendar = Calendar.getInstance()
        val now = calendar.time
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd'_'HHmmss", Locale.getDefault())
        val fileName = simpleDateFormat.format(now)
        Toast.makeText(context, "Saved " + GO.saveSpecterType2, Toast.LENGTH_SHORT).show()
        // Check mount devices
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            Log.d("BluZ-BT", "SD-storage not available: ${Environment.getExternalStorageState()}")
            Toast.makeText(context, "SD-storage not available: ${Environment.getExternalStorageState()}", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val SDPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            val direct = File("$SDPath/BluZ")
            if (!direct.exists()) {         // Нужно проверить наличие каталога.
                if (direct.mkdir()) {
                    Log.d("BluZ-BT", "SD Path: $SDPath/BluZ")
                } else {
                    Log.d("BluZ-BT", "Create dir error.")
                    Toast.makeText(context, "Directory create error.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            val myFile = File("$SDPath/BluZ/$fileName.csv")
            if (myFile.createNewFile()) {
                Log.d("BluZ-BT", "File create Ok.")
            } else {
                Toast.makeText(context, "File create error.", Toast.LENGTH_LONG).show()
                Log.d("BluZ-BT", "Create file error.")
                return
            }
            val outputStream = FileOutputStream(myFile)
            /*
            *   Выгрузка данных в файл
            */
            for (i in 0 until resolution) {
                dataStr = java.lang.String.format("%d", i) + "," + java.lang.String.format("%.0f", spectrData.get(i)) + "\n"

                outputStream.write(dataStr.toByteArray()) // Write to file
            }
            outputStream.close()
        } catch (e: Exception) {
            Log.e("BluZ-BT", "Error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    /**
     * Сохраняет спектр как BqMon-совместимый XML с метаданными:
     *  - GPS-координаты (через `LocationManager.GPS_PROVIDER.getLastKnownLocation`)
     *  - StartTime / EndTime (ISO 8601)
     *  - Коэффициенты калибровки полинома (C, B, A — в обратном порядке, как требует BqMon)
     *  - ValidPulseCount, TotalPulseCount, MeasurementTime
     *  - Сам спектр через `<DataPoint>` для каждого канала
     *
     * Имя файла: `yyyyMMdd_HHmmss.xml` в `/Documents/BluZ/`.
     */
    fun saveHistogramXML(context: Context, spectrData: DoubleArray, resolution: Int) {
        var dataStr: String
        val calendar = Calendar.getInstance()
        val now = calendar.time
        var simpleDateFormat = SimpleDateFormat("yyyyMMdd'_'HHmmss", Locale.getDefault())
        val fileName = simpleDateFormat.format(now)
        Toast.makeText(context, "Saved " + GO.saveSpecterType1, Toast.LENGTH_SHORT).show()

        // Check mount devices
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            Log.d("BluZ-BT", "SD-storage not available: ${Environment.getExternalStorageState()}")
            Toast.makeText(context, "SD-storage not available: ${Environment.getExternalStorageState()}", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val SDPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            val direct = File("$SDPath/BluZ")
            Log.d("BluZ-BT", "SD Path: $SDPath/BluZ")

            if (!direct.exists()) {
                if (direct.mkdir()) {
                    Log.d("BluZ-BT", "SD Path: $SDPath/BluZ")
                } else {
                    Log.d("BluZ-BT", "Create dir error.")
                    Toast.makeText(context, "Directory create error.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val myFile = File("$SDPath/BluZ/$fileName.xml")
            if (myFile.createNewFile()) {
                Log.d("BluZ-BT", "File create Ok.")
            } else {
                Toast.makeText(context, "File create error.", Toast.LENGTH_LONG).show()
                Log.d("BluZ-BT", "Create file error.")
                return
            }

            val outputStream = FileOutputStream(myFile)
            var pulseSumm = 0.0
            var locationStr = " Unknown."

            /*
            Get GPS location
            */
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    locationStr = " Lat: ${loc.latitude} Lng: ${loc.longitude} Alt: ${loc.altitude} Speed: ${loc.speed}"
                } else {
                    Toast.makeText(context, "GPS error.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "GPS write error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            simpleDateFormat = SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SZZZZZ");
            val sTime = System.currentTimeMillis() - GO.messTm.toFloat() * 1000.0f
            val startTime = simpleDateFormat.format(sTime)
            val endTime = simpleDateFormat.format(now)
            pulseSumm = GO.PCounter.toDouble()
            val kRes = when (GO.spectrResolution) {
                1 -> {
                    2.0
                }
                2 -> {
                    4.0
                }
                else -> 1.0
            }
            val resolutionStr = """
                <Coefficient>${GO.propCoef4096E * kRes}</Coefficient>
                <Coefficient>${GO.propCoef4096D * kRes}</Coefficient>
                <Coefficient>${GO.propCoef4096C * kRes}</Coefficient>
                <Coefficient>${GO.propCoef4096B * kRes}</Coefficient>
                <Coefficient>${GO.propCoef4096A * kRes}</Coefficient>
                """.trimIndent()

            dataStr = "<?xml version=\"1.0\"?>\n" +
                "<ResultDataFile xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "<FormatVersion>120920</FormatVersion>\n" +
                "<ResultDataList>\n" +
                "<ResultData>\n" +
                "<SampleInfo>\n" +
                "<Name />\n" +
                "<Location>" + locationStr + "</Location>\n" +
                "<Time>" + startTime + "</Time>\n" +
                "<Weight>1</Weight>\n" +
                "<Volume>1</Volume>\n" +
                "<Note />\n" +
                "</SampleInfo>\n" +
                "<DeviceConfigReference>\n" +
                "<Name>BluZ</Name>\n" +
                "<Guid>fb3c0393-034b-495b-8ab1-f3011c558a4d</Guid>\n" +
                "</DeviceConfigReference>\n" +
                "<ROIConfigReference>\n" +
                "<Name>10x40NaI(Tl)_Sensl</Name>\n" +
                "<Guid>63afa7cf-0dc5-44d7-8933-535c84c4c18c</Guid>\n" +
                "</ROIConfigReference>\n" +
                "<BackgroundSpectrumFile />\n" +
                "<StartTime>" + startTime + "</StartTime>\n" +
                "<EndTime>" + endTime + "</EndTime>\n" +
                "<PresetTime>" + GO.spectrometerTime.toString() + "</PresetTime>\n" +
                "<EnergySpectrum>\n" +
                "<NumberOfChannels>" +
                resolution.toString() +
                "</NumberOfChannels>\n" +
                /* Correct by Am6er */
                /*"<ChannelPitch>0.0221</ChannelPitch>\n" +*/
                "<ChannelPitch>1</ChannelPitch>\n" +
                "<EnergyCalibration>\n" +
                "<PolynomialOrder>2</PolynomialOrder>\n" +
                "<Coefficients>\n" +
                resolutionStr +
                "</Coefficients>\n" +
                "</EnergyCalibration>\n" +
                "<ValidPulseCount>" + GO.spectrometerPulse.toInt() + "</ValidPulseCount>\n" +
                "<TotalPulseCount>" + GO.spectrometerPulse.toInt() + "</TotalPulseCount>\n" +
                "<MeasurementTime>" + GO.spectrometerTime.toString() + "</MeasurementTime>\n" +
                "<NumberOfSamples>0</NumberOfSamples>\n" +
                "<Spectrum>\n"
            outputStream.write(dataStr.toByteArray())

            for (i in 0 until resolution) {
                dataStr = "<DataPoint>" + java.lang.String.format("%.0f", spectrData.get(i)) + "</DataPoint>\n"
                outputStream.write(dataStr.toByteArray()) // Write to file
            }
            dataStr = """
                ${"</Spectrum>\n" as String}</EnergySpectrum>
                <Visible>true</Visible>
                <PulseCollection>
                <Format>Base64 encoded binary</Format>
                <Pulses />
                </PulseCollection>
                </ResultData>
                </ResultDataList>
                </ResultDataFile>
                """.trimIndent()
            outputStream.write(dataStr.toByteArray())
            outputStream.close()

        } catch (e: Exception) {
            Log.e("BluZ-BT", "Error: ${e.message}")
        }
    }
}