package ru.starline.bluz

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import ru.starline.bluz.data.entity.DetectorType

@OptIn(ExperimentalUnsignedTypes::class)
class SettingsFragment : Fragment() {

    private lateinit var rbLine: RadioButton
    private lateinit var rbLg: RadioButton
    private lateinit var rbFoneLin: RadioButton
    private lateinit var rbFoneLg: RadioButton
    private lateinit var selA: SeekBar
    private lateinit var selR: SeekBar
    private lateinit var selG: SeekBar
    private lateinit var selB: SeekBar
    private lateinit var rgTypeSpec: RadioGroup
    private lateinit var rbResolution: RadioGroup
    private var noChange: Boolean = true
    private lateinit var paddingTextLeft: EditText
    private lateinit var paddingTextRight: EditText
    private lateinit var rgUnit: RadioGroup
    private lateinit var rbuRh: RadioButton
    private lateinit var rbuSvh: RadioButton
    private lateinit var textDetectName: TextView

    private val chiFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleChiFileSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.setup_layout, container, false)

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GO.rbGistogramSpectr = view.findViewById(R.id.rbGistogram)
        GO.rbLineSpectr = view.findViewById(R.id.rbLine)
        GO.rbClickNone = view.findViewById(R.id.rbClickNone)
        GO.rbClick1 = view.findViewById(R.id.rbClick1)
        GO.rbClick10 = view.findViewById(R.id.rbClick10)
        GO.rbLedNone = view.findViewById(R.id.rbLedNone)
        GO.rbLed1 = view.findViewById(R.id.rbLed1)
        GO.rbLed10 = view.findViewById(R.id.rbLed10)
        GO.editPolinomA = view.findViewById(R.id.editPolA)
        GO.editPolinomB = view.findViewById(R.id.editPolB)
        GO.editPolinomC = view.findViewById(R.id.editPolC)
        GO.editPolinomD = view.findViewById(R.id.editPolD)
        GO.editPolinomE = view.findViewById(R.id.editPolE)
        GO.editLevel1 = view.findViewById(R.id.editLevel1)
        GO.editLevel2 = view.findViewById(R.id.editLevel2)
        GO.editLevel3 = view.findViewById(R.id.editLevel3)
        GO.cbSoundLevel1 = view.findViewById(R.id.CBSoudLevel1)
        GO.cbSoundLevel2 = view.findViewById(R.id.CBSoudLevel2)
        GO.cbSoundLevel3 = view.findViewById(R.id.CBSoudLevel3)
        GO.cbVibroLevel1 = view.findViewById(R.id.CBVibroLevel1)
        GO.cbVibroLevel2 = view.findViewById(R.id.CBVibroLevel2)
        GO.cbVibroLevel3 = view.findViewById(R.id.CBVibroLevel3)
        GO.editCPS2Rh = view.findViewById(R.id.editCPS2RH)
        GO.rbResolution1024 = view.findViewById(R.id.RBResol1024)
        GO.rbResolution2048 = view.findViewById(R.id.RBResol2048)
        GO.rbResolution4096 = view.findViewById(R.id.RBResol4096)
        GO.editHVoltage = view.findViewById(R.id.editHvoltage)
        GO.editComparator = view.findViewById(R.id.editComparator)
        GO.cbSpectrometr = view.findViewById(R.id.CBSpectrometer)
        GO.editSMA = view.findViewById(R.id.editTextSMAWindow)
        GO.editRejectChann = view.findViewById(R.id.editTextRejectCann)
        GO.rbSpctTypeBq = view.findViewById(R.id.rbBqMon)
        GO.rbSpctTypeSPE = view.findViewById(R.id.rbSPE)
        GO.rbSpctType = view.findViewById(R.id.rbSpctType)
        GO.textMACADR = view.findViewById(R.id.textMACADDR)
        GO.aqureEdit = view.findViewById(R.id.editAquracy)
        GO.bitsChannelEdit = view.findViewById(R.id.editBitsChannel)
        GO.cbFullScrn = view.findViewById(R.id.CBFullScreen)
        GO.rbGPXType = view.findViewById(R.id.rbGPX)
        GO.rbKMLType = view.findViewById(R.id.rbKML)
        GO.rbTrackFmt = view.findViewById(R.id.RGTrackFormat)
        GO.sampleTimeEdit = view.findViewById(R.id.editSampleTime)
        GO.textAppLogLevel = view.findViewById(R.id.editTextApplucationLog)
        GO.textXZoom = view.findViewById(R.id.editTextXZoom)

        paddingTextLeft = view.findViewById(R.id.editTextPaddingLeft)
        paddingTextRight = view.findViewById(R.id.editTextPaddingRight)

        val buttonNewDetect: Button = view.findViewById(R.id.buttonNewDetector)
        val buttomLoadDetect: Button = view.findViewById(R.id.buttonLoadDetector)
        val buttonSelectDetect: Button = view.findViewById(R.id.buttonSelectDetector)
        textDetectName = view.findViewById(R.id.textDetectorName)

        buttomLoadDetect.setOnClickListener {
            val bluZDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "BluZ"
            )
            if (!bluZDir.exists()) {
                bluZDir.mkdirs()
            }
            if (bluZDir.canRead()) {
                chiFileLauncher.launch("application/octet-stream")
            } else {
                Toast.makeText(context, "Access deny ${bluZDir.name}.", Toast.LENGTH_SHORT).show()
            }
        }

        /* Диалог для редактирования детектора */
        textDetectName.setOnClickListener {
            if (textDetectName.text.isNotEmpty()) {
                val context = requireContext()
                val input = EditText(context)
                input.setText(textDetectName.text)
                AlertDialog.Builder(context)
                    .setTitle("Edit detector")
                    .setView(input)
                    .setPositiveButton("Save") { dialog, _ ->
                        val name = input.text.toString().trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        lifecycleScope.launch {
                            try {
                                GO.dao.editDetector(GO.currentDetector, name)
                                GO.curretnDetectorName = name
                                textDetectName.text = name
                            } catch (e: Exception) {
                                Log.e("DetectorDialog", "Error edit detector", e)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }
                    .setNeutralButton("Delete") { _, _ ->
                        if (GO.currentDetector > 1) {
                            lifecycleScope.launch {
                                try {
                                    GO.dao.deleteDetector(GO.currentDetector)
                                    GO.currentDetector = 0
                                    GO.curretnDetectorName = ""
                                    textDetectName.text = ""
                                } catch (e: Exception) {
                                    Log.e("DetectorDialog", "Error delete detector", e)
                                }
                            }
                        }
                    }
                    .show()
            }
        }

        /* Выбор детектора из базы */
        buttonSelectDetect.setOnClickListener {
            lifecycleScope.launch {
                val detect = GO.dao.getAllDetector()
                val names = detect.map { it.name }.toTypedArray()
                val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.item_track_dialog, names) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val v = super.getView(position, convertView, parent)
                        val textView = v.findViewById<TextView>(R.id.textView)
                        val fullText = names[position]
                        val dateLength = 20
                        if (fullText.length >= dateLength) {
                            val spannable = SpannableString(fullText)
                            val dateColor = ContextCompat.getColor(requireContext(), R.color.labelTextColor2)
                            spannable.setSpan(ForegroundColorSpan(dateColor), 0, dateLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            textView.text = spannable
                        } else {
                            textView.text = fullText
                        }
                        return v
                    }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Select detector")
                    .setAdapter(adapter) { _, which ->
                        val selectedDet = detect[which]
                        GO.curretnDetectorName = selectedDet.name
                        textDetectName.text = selectedDet.name
                        GO.currentDetector = selectedDet.id
                        lifecycleScope.launch {
                            GO.dao.activateDetector(selectedDet.id)
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }

        /* Создание нового детектора */
        buttonNewDetect.setOnClickListener {
            val context = requireContext()
            val input = EditText(context)
            input.hint = "Enter Detector name"
            AlertDialog.Builder(context)
                .setTitle("New detector")
                .setView(input)
                .setPositiveButton("Create") { dialog, _ ->
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Name is empty.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    lifecycleScope.launch {
                        try {
                            val newDetector = DetectorType(
                                id = 0,
                                name = name,
                                changeAt = System.currentTimeMillis() / 1000,
                                chiVector = DoubleArray(1024),
                                curActive = false
                            )
                            GO.currentDetector = GO.dao.insertDetector(newDetector)
                            GO.curretnDetectorName = name
                            textDetectName.text = name
                            GO.dao.activateDetector(GO.currentDetector)
                        } catch (e: Exception) {
                            Log.e("DetectorDialog", "Error create detector", e)
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }
                .show()
        }

        rgUnit = view.findViewById(R.id.RGUnits)
        rbuRh = view.findViewById(R.id.rbURH)
        rbuSvh = view.findViewById(R.id.rbUSVH)

        reloadConfigParameters()

        GO.cbFullScrn.setOnCheckedChangeListener { _, isChecked ->
            GO.fullScrn = isChecked
            val window = requireActivity().window
            WindowCompat.setDecorFitsSystemWindows(window, !isChecked)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    if (isChecked) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }

            // Status/nav bar colors are set by the theme (Theme.BluZ → bz_bg).
            // Don't override them here — that breaks day/night theme switching.
        }

        GO.editPolinomA.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (noChange) {
                    if (GO.editPolinomA.text.isNotEmpty()) {
                        try {
                            //if (GO.rbResolution1024.isChecked) {
                                //GO.propCoef1024A = GO.editPolinomA.text.toString().toFloat()
                            //} else if (GO.rbResolution2048.isChecked) {
                                //GO.propCoef2048A = GO.editPolinomA.text.toString().toFloat()
                            //} else if (GO.rbResolution4096.isChecked) {
                                GO.propCoef4096A = GO.editPolinomA.text.toString().toFloat()
                            //}
                        } catch (e: NumberFormatException) {}
                    }
                }
            }
        })

        GO.editPolinomB.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (noChange) {
                    if (GO.editPolinomB.text.isNotEmpty()) {
                        try {
                            //if (GO.rbResolution1024.isChecked) {
                                //GO.propCoef1024B = GO.editPolinomB.text.toString().toFloat()
                            //} else if (GO.rbResolution2048.isChecked) {
                                //GO.propCoef2048B = GO.editPolinomB.text.toString().toFloat()
                            //} else if (GO.rbResolution4096.isChecked) {
                                GO.propCoef4096B = GO.editPolinomB.text.toString().toFloat()
                            //}
                        } catch (e: NumberFormatException) {}
                    }
                }
            }
        })

        GO.editPolinomC.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (noChange) {
                    if (GO.editPolinomC.text.isNotEmpty()) {
                        try {
                            //if (GO.rbResolution1024.isChecked) {
                                //GO.propCoef1024C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution2048.isChecked) {
                                //GO.propCoef2048C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution4096.isChecked) {
                                GO.propCoef4096C = GO.editPolinomC.text.toString().toFloat()
                            //}
                        } catch (e: NumberFormatException) {}
                    }
                }
            }
        })

        GO.editPolinomD.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (noChange) {
                    if (GO.editPolinomD.text.isNotEmpty()) {
                        try {
                            //if (GO.rbResolution1024.isChecked) {
                                //GO.propCoef1024C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution2048.isChecked) {
                                //GO.propCoef2048C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution4096.isChecked) {
                                GO.propCoef4096D = GO.editPolinomD.text.toString().toFloat()
                            //}
                        } catch (e: NumberFormatException) {}
                    }
                }
            }
        })

        GO.editPolinomE.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (noChange) {
                    if (GO.editPolinomE.text.isNotEmpty()) {
                        try {
                            //if (GO.rbResolution1024.isChecked) {
                                //GO.propCoef1024C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution2048.isChecked) {
                                //GO.propCoef2048C = GO.editPolinomC.text.toString().toFloat()
                            //} else if (GO.rbResolution4096.isChecked) {
                                GO.propCoef4096E = GO.editPolinomE.text.toString().toFloat()
                            //}
                        } catch (e: NumberFormatException) {}
                    }
                }
            }
        })

        var btnRestoreSetup: Button = view.findViewById(R.id.buttonRestoreSetup)
        btnRestoreSetup.setOnClickListener {
            GO.readConfigParameters()
            reloadConfigParameters()
        }

        val btnSaveSetup: Button = view.findViewById(R.id.buttonSaveSetup)
        btnSaveSetup.setOnClickListener {
            val pdTmpL = paddingTextLeft.text.toString().toIntOrNull() ?: 0
            val pdTmpR = paddingTextRight.text.toString().toIntOrNull() ?: 0
            val activity = requireActivity()
            val mainLayout = activity.findViewById<View>(R.id.main)
            if ((pdTmpL != GO.paddingLeft) or (pdTmpR != GO.paddingRight)) {
                GO.paddingLeft = pdTmpL
                GO.paddingRight = pdTmpR
                mainLayout.setPadding(GO.paddingLeft, mainLayout.paddingTop, GO.paddingRight, mainLayout.paddingBottom)
            }
            GO.xZoom = GO.textXZoom.text.toString().toFloatOrNull() ?: 1.0f
            GO.appLogLevel = GO.textAppLogLevel.text.toString().toInt()
            if (rbuRh.isChecked) {
                GO.unitsMess = 0
            } else {
                GO.unitsMess = 1
            }
            if (GO.rbSpctTypeBq.isChecked) {
                GO.saveSpecterType = 0
            } else {
                GO.saveSpecterType = 1
            }
            GO.rejectChann = GO.editRejectChann.text.toString().toInt()
            if (GO.rbLineSpectr.isChecked) {
                GO.specterGraphType = 0
            } else {
                GO.specterGraphType = 1
            }
            if (GO.rbClickNone.isChecked) {
                GO.propSoundKvant = 0
            } else if (GO.rbClick1.isChecked) {
                GO.propSoundKvant = 1
            } else if (GO.rbClick10.isChecked) {
                GO.propSoundKvant = 2
            }
            if (GO.rbLedNone.isChecked) {
                GO.propLedKvant = 0
            } else {
                if (GO.rbLed1.isChecked) {
                    GO.propLedKvant = 1
                } else {
                    GO.propLedKvant = 2
                }
            }
            GO.propAutoStartSpectrometr = GO.cbSpectrometr.isChecked
            // Безопасный парсинг: пустые поля → 0 вместо NumberFormatException.
            GO.propLevel1 = GO.editLevel1.text.toString().trim().toIntOrNull() ?: 0
            GO.propLevel2 = GO.editLevel2.text.toString().trim().toIntOrNull() ?: 0
            GO.propLevel3 = GO.editLevel3.text.toString().trim().toIntOrNull() ?: 0
            GO.propSoundLevel1 = GO.cbSoundLevel1.isChecked
            GO.propSoundLevel2 = GO.cbSoundLevel2.isChecked
            GO.propSoundLevel3 = GO.cbSoundLevel3.isChecked
            GO.propVibroLevel1 = GO.cbVibroLevel1.isChecked
            GO.propVibroLevel2 = GO.cbVibroLevel2.isChecked
            GO.propVibroLevel3 = GO.cbVibroLevel3.isChecked
            GO.propCPS2UR = GO.editCPS2Rh.text.toString().trim().replace(',', '.').toFloatOrNull() ?: 0f
            val smaRaw = GO.editSMA.text.toString().trim().toIntOrNull() ?: 0
            GO.windowSMA = (smaRaw / 2) * 2 + 1
            GO.propHVoltage = (GO.editHVoltage.text.toString().trim().toIntOrNull() ?: 0).toUShort()
            GO.propComparator = (GO.editComparator.text.toString().trim().toIntOrNull() ?: 0).toUShort()
            if (GO.rbResolution1024.isChecked) {
                GO.spectrResolution = 0
            } else if (GO.rbResolution2048.isChecked) {
                GO.spectrResolution = 1
            } else if (GO.rbResolution4096.isChecked) {
                GO.spectrResolution = 2
            } else {
                GO.spectrResolution = 0
            }
            GO.enrgCalc.rS = GO.spectrResolution        // Данные для пересчета в энергию.
            GO.aqureValue = GO.aqureEdit.text.toString().trim().toIntOrNull() ?: 0
            GO.bitsChannel = GO.bitsChannelEdit.text.toString().trim().toIntOrNull() ?: 20
            if (GO.bitsChannel < 16 || GO.bitsChannel > 32) {
                GO.bitsChannel = 20
            }
            GO.sampleTime = GO.sampleTimeEdit.text.toString().trim().toIntOrNull() ?: 0
            if (GO.sampleTime < 0 || GO.sampleTime > 7) {
                GO.sampleTime = 0
            }
            Log.d("BluZ-BT", "mac addr: " + GO.LEMAC + " Resolution: " + GO.spectrResolution.toString())
            GO.writeConfigParameters()
            GO.updateSpecSubtitle()
            Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_SHORT).show()
            if (GO.LEMAC.length == 17 && GO.LEMAC[0] != 'X') {
                GO.tmFull.startTimer()
            } else {
                GO.oneShotBLETimer = false
                GO.tmFull.stopTimer()
            }
        }

        GO.scanButton = view.findViewById(R.id.buttonScanBT)
        GO.scanButton.setOnClickListener { startDeviceDiscovery() }

        val switchAutoCfg = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchAutoLoadDeviceCfg)
        switchAutoCfg.isChecked = GO.autoLoadDeviceCfg
        switchAutoCfg.setOnCheckedChangeListener { _, checked ->
            GO.autoLoadDeviceCfg = checked
            GO.PP.setPropBoolean(GO.propAutoLoadDeviceCfg, checked)
        }

        GO.btnReadFromDevice = view.findViewById(R.id.buttonReadFromDevice)
        GO.btnReadFromDevice.setOnClickListener {
            if (GO.configDataReady) {
                GO.readConfigFormDevice()
                reloadConfigParameters()
            }
        }

        GO.btnWriteToDevice = view.findViewById(R.id.buttonWriteToDevice)
        GO.btnWriteToDevice.setOnClickListener {
            if (GO.rbClickNone.isChecked) {
                GO.propSoundKvant = 0
            } else if (GO.rbClick1.isChecked) {
                GO.propSoundKvant = 1
            } else if (GO.rbClick10.isChecked) {
                GO.propSoundKvant = 2
            }
            if (GO.rbLedNone.isChecked) {
                GO.propLedKvant = 0
            } else {
                if (GO.rbLed1.isChecked) {
                    GO.propLedKvant = 1
                } else {
                    GO.propLedKvant = 2
                }
            }
            GO.propAutoStartSpectrometr = GO.cbSpectrometr.isChecked
            // Безопасный парсинг — пустые поля → 0 (а не NumberFormatException).
            GO.propLevel1 = GO.editLevel1.text.toString().trim().toIntOrNull() ?: 0
            GO.propLevel2 = GO.editLevel2.text.toString().trim().toIntOrNull() ?: 0
            GO.propLevel3 = GO.editLevel3.text.toString().trim().toIntOrNull() ?: 0
            GO.propSoundLevel1 = GO.cbSoundLevel1.isChecked
            GO.propSoundLevel2 = GO.cbSoundLevel2.isChecked
            GO.propSoundLevel3 = GO.cbSoundLevel3.isChecked
            GO.propVibroLevel1 = GO.cbVibroLevel1.isChecked
            GO.propVibroLevel2 = GO.cbVibroLevel2.isChecked
            GO.propVibroLevel3 = GO.cbVibroLevel3.isChecked
            GO.propCPS2UR = GO.editCPS2Rh.text.toString().trim().replace(',', '.').toFloatOrNull() ?: 0f
            GO.propHVoltage = (GO.editHVoltage.text.toString().trim().toIntOrNull() ?: 0).toUShort()
            GO.propComparator = (GO.editComparator.text.toString().trim().toIntOrNull() ?: 0).toUShort()
            if (GO.rbResolution1024.isChecked) {
                GO.spectrResolution = 0
            } else if (GO.rbResolution2048.isChecked) {
                GO.spectrResolution = 1
            } else if (GO.rbResolution4096.isChecked) {
                GO.spectrResolution = 2
            } else {
                GO.spectrResolution = 0
            }

            var convVal = ByteBuffer.allocate(4).putInt(GO.propLevel1).array()
            GO.BTT.sendBuffer[4] = convVal[0].toUByte()
            GO.BTT.sendBuffer[5] = convVal[1].toUByte()
            GO.BTT.sendBuffer[6] = convVal[2].toUByte()
            GO.BTT.sendBuffer[7] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putInt(GO.propLevel2).array()
            GO.BTT.sendBuffer[8] = convVal[0].toUByte()
            GO.BTT.sendBuffer[9] = convVal[1].toUByte()
            GO.BTT.sendBuffer[10] = convVal[1].toUByte()
            GO.BTT.sendBuffer[11] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putInt(GO.propLevel3).array()
            GO.BTT.sendBuffer[12] = convVal[0].toUByte()
            GO.BTT.sendBuffer[13] = convVal[1].toUByte()
            GO.BTT.sendBuffer[14] = convVal[2].toUByte()
            GO.BTT.sendBuffer[15] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCPS2UR).array()
            GO.BTT.sendBuffer[16] = convVal[0].toUByte()
            GO.BTT.sendBuffer[17] = convVal[1].toUByte()
            GO.BTT.sendBuffer[18] = convVal[2].toUByte()
            GO.BTT.sendBuffer[19] = convVal[3].toUByte()

            GO.BTT.sendBuffer[20] = 0u
            if (GO.rbLed1.isChecked || GO.rbLed10.isChecked) {
                GO.BTT.sendBuffer[20] = 1u
            }
            if (GO.rbClick1.isChecked || GO.rbClick10.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000010u
            }
            if (GO.cbSoundLevel1.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00000100u
            }
            if (GO.cbSoundLevel2.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00001000u
            }
            if (GO.cbSoundLevel3.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00010000u
            }
            if (GO.cbVibroLevel1.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b00100000u
            }
            if (GO.cbVibroLevel2.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b01000000u
            }
            if (GO.cbVibroLevel3.isChecked) {
                GO.BTT.sendBuffer[20] = GO.BTT.sendBuffer[20] or 0b10000000u
            }
            /*
            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024A).array()
            GO.BTT.sendBuffer[21] = convVal[0].toUByte()
            GO.BTT.sendBuffer[22] = convVal[1].toUByte()
            GO.BTT.sendBuffer[23] = convVal[2].toUByte()
            GO.BTT.sendBuffer[24] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024B).array()
            GO.BTT.sendBuffer[25] = convVal[0].toUByte()
            GO.BTT.sendBuffer[26] = convVal[1].toUByte()
            GO.BTT.sendBuffer[27] = convVal[2].toUByte()
            GO.BTT.sendBuffer[28] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef1024C).array()
            GO.BTT.sendBuffer[29] = convVal[0].toUByte()
            GO.BTT.sendBuffer[30] = convVal[1].toUByte()
            GO.BTT.sendBuffer[31] = convVal[2].toUByte()
            GO.BTT.sendBuffer[32] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048A).array()
            GO.BTT.sendBuffer[39] = convVal[0].toUByte()
            GO.BTT.sendBuffer[40] = convVal[1].toUByte()
            GO.BTT.sendBuffer[41] = convVal[2].toUByte()
            GO.BTT.sendBuffer[42] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048B).array()
            GO.BTT.sendBuffer[43] = convVal[0].toUByte()
            GO.BTT.sendBuffer[44] = convVal[1].toUByte()
            GO.BTT.sendBuffer[45] = convVal[2].toUByte()
            GO.BTT.sendBuffer[46] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef2048C).array()
            GO.BTT.sendBuffer[47] = convVal[0].toUByte()
            GO.BTT.sendBuffer[48] = convVal[1].toUByte()
            GO.BTT.sendBuffer[49] = convVal[2].toUByte()
            GO.BTT.sendBuffer[50] = convVal[3].toUByte()
*/
            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096A).array()
            GO.BTT.sendBuffer[51] = convVal[0].toUByte()
            GO.BTT.sendBuffer[52] = convVal[1].toUByte()
            GO.BTT.sendBuffer[53] = convVal[2].toUByte()
            GO.BTT.sendBuffer[54] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096B).array()
            GO.BTT.sendBuffer[55] = convVal[0].toUByte()
            GO.BTT.sendBuffer[56] = convVal[1].toUByte()
            GO.BTT.sendBuffer[57] = convVal[2].toUByte()
            GO.BTT.sendBuffer[58] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096C).array()
            GO.BTT.sendBuffer[59] = convVal[0].toUByte()
            GO.BTT.sendBuffer[60] = convVal[1].toUByte()
            GO.BTT.sendBuffer[61] = convVal[2].toUByte()
            GO.BTT.sendBuffer[62] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096D).array()
            GO.BTT.sendBuffer[43] = convVal[0].toUByte()
            GO.BTT.sendBuffer[44] = convVal[1].toUByte()
            GO.BTT.sendBuffer[45] = convVal[2].toUByte()
            GO.BTT.sendBuffer[46] = convVal[3].toUByte()

            convVal = ByteBuffer.allocate(4).putFloat(GO.propCoef4096E).array()
            GO.BTT.sendBuffer[47] = convVal[0].toUByte()
            GO.BTT.sendBuffer[48] = convVal[1].toUByte()
            GO.BTT.sendBuffer[49] = convVal[2].toUByte()
            GO.BTT.sendBuffer[50] = convVal[3].toUByte()

            // aqureEdit может содержать значения 0..65535 (unsigned 16-bit). Парсим как Int
            // и затем .toShort() — это truncate без NumberFormatException (Short.parseShort падает на >32767).
            val aqureRaw = GO.aqureEdit.text.toString().trim().toIntOrNull() ?: 0
            val convVal2 = ByteBuffer.allocate(2).putShort(aqureRaw.toShort()).array()
            GO.BTT.sendBuffer[64] = convVal2[0].toUByte()
            GO.BTT.sendBuffer[63] = convVal2[1].toUByte()

            GO.BTT.sendBuffer[65] = GO.bitsChannelEdit.text.toString().toUByte()

            GO.BTT.sendBuffer[33] = (GO.propHVoltage and 255u).toUByte()
            GO.BTT.sendBuffer[34] = ((GO.propHVoltage.toUInt() shr 8) and 255u).toUByte()

            GO.BTT.sendBuffer[35] = (GO.propComparator and 255u).toUByte()
            GO.BTT.sendBuffer[36] = ((GO.propComparator.toUInt() shr 8) and 255u).toUByte()

            if (GO.rbResolution1024.isChecked) {
                GO.BTT.sendBuffer[37] = 0u
            } else if (GO.rbResolution2048.isChecked) {
                GO.BTT.sendBuffer[37] = 1u
            } else if (GO.rbResolution4096.isChecked) {
                GO.BTT.sendBuffer[37] = 2u
            } else {
                GO.BTT.sendBuffer[37] = 0u
            }

            if (GO.cbSpectrometr.isChecked) {
                GO.BTT.sendBuffer[38] = 1u
            } else {
                GO.BTT.sendBuffer[38] = 0u
            }

            val tmpSampleTime = ((GO.sampleTimeEdit.text.toString().toUInt() and 7u) shl 1).toUByte()
            GO.BTT.sendBuffer[38] = GO.BTT.sendBuffer[38] or tmpSampleTime

            if (GO.rbClick10.isChecked) {
                GO.BTT.sendBuffer[38] = GO.BTT.sendBuffer[38] or 16u
            }

            if (GO.rbLed10.isChecked) {
                GO.BTT.sendBuffer[38] = GO.BTT.sendBuffer[38] or 32u
            }

            GO.BTT.sendCommand(0u)
        }

        if (GO.ColorDosimeter == 0) {
            GO.ColorDosimeter = resources.getColor(R.color.ColorDosimeter, GO.mainContext.theme)
        }
        if (GO.ColorDosimeterSMA == 0) {
            GO.ColorDosimeterSMA = resources.getColor(R.color.ColorDosimeterSMA, GO.mainContext.theme)
        }
        if (GO.ColorLin == 0) {
            GO.ColorLin = resources.getColor(R.color.specterColorLin, GO.mainContext.theme)
        }
        if (GO.ColorLog == 0) {
            GO.ColorLog = resources.getColor(R.color.specterColorLog, GO.mainContext.theme)
        }
        if (GO.ColorFone == 0) {
            GO.ColorFone = resources.getColor(R.color.specterColorFone, GO.mainContext.theme)
        }
        if (GO.ColorFoneLg == 0) {
            GO.ColorFoneLg = resources.getColor(R.color.specterColorFoneLg, GO.mainContext.theme)
        }
        if (GO.ColorLinGisto == 0) {
            GO.ColorLinGisto = resources.getColor(R.color.specterColorLinGisto, GO.mainContext.theme)
        }
        if (GO.ColorLogGisto == 0) {
            GO.ColorLogGisto = resources.getColor(R.color.specterColorLogGisto, GO.mainContext.theme)
        }
        if (GO.ColorFoneGisto == 0) {
            GO.ColorFoneGisto = resources.getColor(R.color.specterColorFoneGisto, GO.mainContext.theme)
        }
        if (GO.ColorFoneLgGisto == 0) {
            GO.ColorFoneLgGisto = resources.getColor(R.color.specterColorFoneLgGisto, GO.mainContext.theme)
        }

        rbLine = view.findViewById(R.id.RBLin)
        rbLg = view.findViewById(R.id.RBLg)
        rbFoneLin = view.findViewById(R.id.RBFoneLin)
        rbFoneLg = view.findViewById(R.id.RBFoneLg)
        rgTypeSpec = view.findViewById(R.id.rgTypeSpectr)
        rbResolution = view.findViewById(R.id.RGResolution)

        GO.rbTrackFmt.setOnCheckedChangeListener { _, checkedId ->
            view.findViewById<RadioButton>(checkedId)?.apply {
                noChange = false
                when (checkedId) {
                    GO.rbKMLType.id -> {
                        GO.saveTrackType = 0
                        if (GO.isButtonSaveTrackInitialized) {
                            GO.buttonSaveTrack.text = resources.getString(R.string.textKML)
                        }
                    }
                    GO.rbGPXType.id -> {
                        GO.saveTrackType = 1
                        if (GO.isButtonSaveTrackInitialized) {
                            GO.buttonSaveTrack.text = resources.getString(R.string.textGPX)
                        }
                    }
                }
                noChange = true
            }
        }

        GO.rbSpctType.setOnCheckedChangeListener { _, checkedId ->
            view.findViewById<RadioButton>(checkedId)?.apply {
                noChange = false
                when (checkedId) {
                    GO.rbSpctTypeBq.id -> GO.saveSpecterType = 0
                    GO.rbSpctTypeSPE.id -> GO.saveSpecterType = 1
                }
                noChange = true
            }
        }

        rbResolution.setOnCheckedChangeListener { _, checkedId ->
            view.findViewById<RadioButton>(checkedId)?.apply {
                noChange = false
                val df = DecimalFormat(GO.acuricyPatern, DecimalFormatSymbols(Locale.US))
                GO.editPolinomA.setText(df.format(GO.propCoef4096A))
                GO.editPolinomB.setText(df.format(GO.propCoef4096B))
                GO.editPolinomC.setText(df.format(GO.propCoef4096C))
                GO.editPolinomD.setText(df.format(GO.propCoef4096D))
                GO.editPolinomE.setText(df.format(GO.propCoef4096E))
                noChange = true
            }
        }

        rgUnit.setOnCheckedChangeListener { _, checkedId ->
            view.findViewById<RadioButton>(checkedId)?.apply {
                noChange = false
                GO.unitsMess = when (checkedId) {
                    rbuSvh.id -> 1
                    else -> 0
                }
                noChange = true
            }
        }

        rgTypeSpec.setOnCheckedChangeListener { _, checkedId ->
            view.findViewById<RadioButton>(checkedId)?.apply {
                noChange = false
                if (checkedId == GO.rbLineSpectr.id) {
                    GO.specterGraphType = 0
                    if (rbLine.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorLin), false)
                        selR.setProgress(Color.red(GO.ColorLin), false)
                        selG.setProgress(Color.green(GO.ColorLin), false)
                        selB.setProgress(Color.blue(GO.ColorLin), false)
                    } else if (rbLg.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorLog), false)
                        selR.setProgress(Color.red(GO.ColorLog), false)
                        selG.setProgress(Color.green(GO.ColorLog), false)
                        selB.setProgress(Color.blue(GO.ColorLog), false)
                    } else if (rbFoneLin.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorFone), false)
                        selR.setProgress(Color.red(GO.ColorFone), false)
                        selG.setProgress(Color.green(GO.ColorFone), false)
                        selB.setProgress(Color.blue(GO.ColorFone), false)
                    } else if (rbFoneLg.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                        selR.setProgress(Color.red(GO.ColorFoneLg), false)
                        selG.setProgress(Color.green(GO.ColorFoneLg), false)
                        selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                    }
                } else if (checkedId == GO.rbGistogramSpectr.id) {
                    GO.specterGraphType = 1
                    if (rbLine.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                        selR.setProgress(Color.red(GO.ColorLinGisto), false)
                        selG.setProgress(Color.green(GO.ColorLinGisto), false)
                        selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                    } else if (rbLg.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                        selR.setProgress(Color.red(GO.ColorLogGisto), false)
                        selG.setProgress(Color.green(GO.ColorLogGisto), false)
                        selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                    } else if (rbFoneLin.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                        selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                        selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                        selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                    } else if (rbFoneLg.isChecked) {
                        selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                        selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                        selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                        selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                    }
                }
                GO.drawExamp.exampRedraw()
                noChange = true
            }
        }

        val radioButtons = listOf(rbLine, rbLg, rbFoneLin, rbFoneLg)
        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                radioButtons.filter { it != radioButton }.forEach { it.isChecked = false }
                changeSpectrType(radioButton.id)
            }
        }

        selA = view.findViewById(R.id.seekBarA)
        selA.setProgress(Color.alpha(GO.ColorLin), false)
        selA.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (noChange) {
                    if (GO.rbLineSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLin = GO.bColor.setSpecterColor(0, progress, GO.ColorLin)
                        else if (rbLg.isChecked) GO.ColorLog = GO.bColor.setSpecterColor(0, progress, GO.ColorLog)
                        else if (rbFoneLin.isChecked) GO.ColorFone = GO.bColor.setSpecterColor(0, progress, GO.ColorFone)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLg = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLg)
                    } else if (GO.rbGistogramSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLinGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLinGisto)
                        else if (rbLg.isChecked) GO.ColorLogGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorLogGisto)
                        else if (rbFoneLin.isChecked) GO.ColorFoneGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneGisto)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(0, progress, GO.ColorFoneLgGisto)
                    }
                    GO.drawExamp.exampRedraw()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        selR = view.findViewById(R.id.seekBarR)
        selR.setProgress(Color.red(GO.ColorLin), false)
        selR.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (noChange) {
                    if (GO.rbLineSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLin = GO.bColor.setSpecterColor(1, progress, GO.ColorLin)
                        else if (rbLg.isChecked) GO.ColorLog = GO.bColor.setSpecterColor(1, progress, GO.ColorLog)
                        else if (rbFoneLin.isChecked) GO.ColorFone = GO.bColor.setSpecterColor(1, progress, GO.ColorFone)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLg = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLg)
                    } else if (GO.rbGistogramSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLinGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLinGisto)
                        else if (rbLg.isChecked) GO.ColorLogGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorLogGisto)
                        else if (rbFoneLin.isChecked) GO.ColorFoneGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneGisto)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(1, progress, GO.ColorFoneLgGisto)
                    }
                    GO.drawExamp.exampRedraw()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        selG = view.findViewById(R.id.seekBarG)
        selG.setProgress(Color.green(GO.ColorLin), false)
        selG.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (noChange) {
                    if (GO.rbLineSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLin = GO.bColor.setSpecterColor(2, progress, GO.ColorLin)
                        else if (rbLg.isChecked) GO.ColorLog = GO.bColor.setSpecterColor(2, progress, GO.ColorLog)
                        else if (rbFoneLin.isChecked) GO.ColorFone = GO.bColor.setSpecterColor(2, progress, GO.ColorFone)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLg = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLg)
                    } else if (GO.rbGistogramSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLinGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLinGisto)
                        else if (rbLg.isChecked) GO.ColorLogGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorLogGisto)
                        else if (rbFoneLin.isChecked) GO.ColorFoneGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneGisto)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(2, progress, GO.ColorFoneLgGisto)
                    }
                    GO.drawExamp.exampRedraw()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        selB = view.findViewById(R.id.seekBarB)
        selB.setProgress(Color.blue(GO.ColorLin), false)
        selB.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (noChange) {
                    if (GO.rbLineSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLin = GO.bColor.setSpecterColor(3, progress, GO.ColorLin)
                        else if (rbLg.isChecked) GO.ColorLog = GO.bColor.setSpecterColor(3, progress, GO.ColorLog)
                        else if (rbFoneLin.isChecked) GO.ColorFone = GO.bColor.setSpecterColor(3, progress, GO.ColorFone)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLg = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneLg)
                    } else if (GO.rbGistogramSpectr.isChecked) {
                        if (rbLine.isChecked) GO.ColorLinGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorLinGisto)
                        else if (rbLg.isChecked) GO.ColorLogGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorLogGisto)
                        else if (rbFoneLin.isChecked) GO.ColorFoneGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneGisto)
                        else if (rbFoneLg.isChecked) GO.ColorFoneLgGisto = GO.bColor.setSpecterColor(3, progress, GO.ColorFoneLgGisto)
                    }
                    GO.drawExamp.exampRedraw()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        GO.drawExamp.exampleImgView = view.findViewById(R.id.tvColor)
        GO.drawExamp.exampleImgView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawExamp.exampleImgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawExamp.init()
                    GO.drawExamp.exampRedraw()
                }
            }
        )

        val buttonFnd = view.findViewById<Button>(R.id.buttonFind)
        buttonFnd.setOnClickListener {
            GO.BTT.sendCommand(6u)
        }

        val buttonLogs = view.findViewById<Button>(R.id.buttonLogs)
        buttonLogs.setOnClickListener {
            startActivity(Intent(requireContext(), LogActivity::class.java))
        }

        val buttonCalibrateBatt = view.findViewById<Button>(R.id.buttonCalibrateBatt)
        buttonCalibrateBatt.setOnClickListener {
            showBatteryCalibrationDialog()
        }

        val buttonAutoCal = view.findViewById<Button>(R.id.buttonAutoCalibrate)
        buttonAutoCal.setOnClickListener {
            showAutoCalibrationModeDialog()
        }

        val buttonTuneComp = view.findViewById<Button>(R.id.buttonTuneComparator)
        buttonTuneComp.setOnClickListener {
            showStandaloneComparatorTuningDialog()
        }

        val switchDayTheme = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.bzSwitchDayTheme)
        switchDayTheme.isChecked = ThemePrefs.isDayTheme(requireContext())
        switchDayTheme.setOnCheckedChangeListener { _, isChecked ->
            Log.d("BluZ-Theme", "Switching dayTheme=$isChecked")
            ThemePrefs.setDayTheme(requireContext(), isChecked)
            view.post {
                Log.d("BluZ-Theme", "Recreating activity")
                requireActivity().recreate()
            }
        }

        val switchBatteryPercent = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.bzSwitchBatteryPercent)
        switchBatteryPercent.isChecked = GO.showBatteryPercent
        switchBatteryPercent.setOnCheckedChangeListener { _, isChecked ->
            GO.showBatteryPercent = isChecked
            GO.PP.setPropBoolean(GO.propShowBatteryPercent, isChecked)
            // Сразу перерисуем StatusStrip из закэшированного напряжения, чтобы переключение
            // отображалось мгновенно, не дожидаясь следующего BLE-фрейма.
            (activity as? MainActivity)?.applyBatteryDisplay(GO.battLevel)
        }
    }

    private val scanDurationMs: Long = 15000L
    private var scanJob: kotlinx.coroutines.Job? = null
    private var scanCountdownJob: kotlinx.coroutines.Job? = null
    private var scanDialog: AlertDialog? = null

    /** Открывает диалог "Поиск BluZ" и сразу запускает сканирование на 8 сек. */
    /**
     * Открывает диалог «Поиск BluZ» (`dialog_bz_scan.xml`) с обратным отсчётом 15 секунд
     * и динамическим списком найденных приборов.
     *
     * **Layout элементы:**
     *  - `bzScanCountdown` — таймер «(N с)» сверху справа
     *  - `bzScanHint` — статус строки «Найдено: N» или «Устройства BluZ не найдены»
     *  - `bzScanList` — `LinearLayout`, в который [addDeviceRow] динамически добавляет строки
     *  - `bzScanClose` / `bzScanRestart` — кнопки внизу
     *
     * `setOnDismissListener` отменяет `scanJob` и `scanCountdownJob` при закрытии.
     * Первый цикл стартует сразу через [launchScanCycle].
     */
    private fun startDeviceDiscovery() {
        val ctx = requireContext()
        val content = LayoutInflater.from(ctx).inflate(R.layout.dialog_bz_scan, null)
        val countdownTv = content.findViewById<TextView>(R.id.bzScanCountdown)
        val hintTv = content.findViewById<TextView>(R.id.bzScanHint)
        val listContainer = content.findViewById<android.widget.LinearLayout>(R.id.bzScanList)
        val restartBtn = content.findViewById<Button>(R.id.bzScanRestart)
        val closeBtn = content.findViewById<Button>(R.id.bzScanClose)

        val dialog = AlertDialog.Builder(ctx)
            .setView(content)
            .create()
        scanDialog = dialog
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            scanJob?.cancel(); scanJob = null
            scanCountdownJob?.cancel(); scanCountdownJob = null
            scanDialog = null
        }

        restartBtn.setOnClickListener {
            listContainer.removeAllViews()
            hintTv.text = "Ожидание устройств…"
            launchScanCycle(countdownTv, hintTv, listContainer)
        }

        dialog.show()
        launchScanCycle(countdownTv, hintTv, listContainer)
    }

    /** Запускает один цикл сканирования: countdown в заголовке + динамическое добавление найденных. */
    /**
     * Запускает один цикл сканирования (15 сек) + параллельный обратный отсчёт.
     *
     *  - `scanCountdownJob` — корутина с `delay(1000)` для обновления `countdownTv` раз в секунду
     *  - `scanJob` — вызов [BluetoothInterface.scanForDevices] с callback'ами `onFound`
     *    (добавляет строку через [addDeviceRow]) и `onComplete` (скрывает countdown,
     *    обновляет hint)
     *
     * При повторном вызове (кнопка «Повторить поиск») — обе корутины отменяются и
     * стартуют заново.
     */
    private fun launchScanCycle(
        countdownTv: TextView,
        hintTv: TextView,
        listContainer: android.widget.LinearLayout
    ) {
        scanJob?.cancel()
        scanCountdownJob?.cancel()

        val totalSec = (scanDurationMs / 1000).toInt()
        countdownTv.visibility = View.VISIBLE
        countdownTv.text = "(${totalSec}с)"

        // Обратный отсчёт обновляет заголовок раз в секунду
        scanCountdownJob = viewLifecycleOwner.lifecycleScope.launch {
            var remaining = totalSec
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000L)
                remaining--
                if (isAdded) countdownTv.text = "(${remaining}с)"
            }
        }

        scanJob = viewLifecycleOwner.lifecycleScope.launch {
            // BluetoothInterface.scanForDevices сам уже возвращает Job, но удобнее обернуть.
            val innerJob = GO.BTT.scanForDevices(scanDurationMs,
                onFound = { mac ->
                    if (!isAdded) return@scanForDevices
                    addDeviceRow(listContainer, mac)
                    hintTv.text = "Найдено: ${listContainer.childCount}"
                },
                onComplete = {
                    if (!isAdded) return@scanForDevices
                    countdownTv.visibility = View.GONE
                    if (listContainer.childCount == 0) {
                        hintTv.text = "Устройства BluZ не найдены"
                    } else {
                        hintTv.text = "Найдено: ${listContainer.childCount} · поиск завершён"
                    }
                }
            )
            try { innerJob.join() } catch (_: Exception) {}
        }
    }

    /** Создаёт строку списка устройств с MAC и колокольчиком. */
    /**
     * Инфлейтит `item_bz_scan_device.xml` (TextView с MAC + ImageButton с колокольчиком)
     * и добавляет в контейнер.
     *
     * **Обработчики:**
     *  - Клик по всей строке → закрывает диалог + [applySelectedMac] (применить MAC)
     *  - Клик по колокольчику → [pingDevice] (опознать прибор сигналом)
     */
    private fun addDeviceRow(container: android.widget.LinearLayout, mac: String) {
        val row = LayoutInflater.from(container.context)
            .inflate(R.layout.item_bz_scan_device, container, false)
        row.findViewById<TextView>(R.id.bzScanItemMac).text = mac
        row.setOnClickListener {
            scanDialog?.dismiss()
            applySelectedMac(mac)
        }
        row.findViewById<ImageButton>(R.id.bzScanItemBell).setOnClickListener {
            pingDevice(mac)
        }
        container.addView(row)
    }

    /** Подключается к указанному MAC и шлёт команду `find` (звук/вибро) — чтобы пользователь
     *  смог опознать нужный прибор среди нескольких. Команду физически нельзя «бросить в воздух» —
     *  BLE-связь приложение→прибор всегда через GATT-write. Если мы уже подключены к этому MAC,
     *  повторное нажатие шлёт команду мгновенно, без переподключения. */
    private fun pingDevice(mac: String) {
        // Быстрый путь: уже подключены к нужному прибору — просто шлём команду.
        if (GO.BTT.connected && GO.LEMAC == mac) {
            GO.BTT.sendCommand(6u)
            Toast.makeText(requireContext(), "Сигнал на $mac", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Сигнал на $mac…", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            // Меняем MAC — рвём текущее GATT-соединение (если есть) и автореконнект-таймер,
            // чтобы потом гарантированно подключиться к новому MAC.
            GO.tmFull.stopTimer()
            GO.BTT.destroyDevice()
            GO.LEMAC = mac
            GO.PP.setPropStr(GO.propCfgADDRESS, mac)

            // Ждём, пока StateFlow покинет состояние Connected (последствие destroyDevice).
            // Этот шаг исключает ложное срабатывание `first { Connected }` ниже от
            // предыдущего, ещё не разорванного соединения.
            kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                GO.BTT.status.first { it !is BleStatus.Connected }
            }

            GO.BTT.initLeDevice()
            GO.tmFull.startTimer()

            val ok = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                GO.BTT.status.first { it is BleStatus.Connected }
            }
            if (ok != null) {
                GO.BTT.sendCommand(6u)
            } else if (isAdded) {
                Toast.makeText(requireContext(), "Не удалось подключиться к $mac", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Применяет выбранный MAC: подставляет в поле, сохраняет в SharedPreferences и (опционально)
     *  подключается к прибору для автозагрузки конфигурации. */
    /**
     * Применяет выбранный MAC: сохраняет в `GO.LEMAC` + SharedPreferences (ключ `propCfgADDRESS`),
     * подставляет в EditText, запускает таймер автоподключения.
     *
     * **Если `autoLoadDeviceCfg` включён** — после первого пришедшего фрейма от прибора:
     *  1. [BluetoothInterface.deviceFrames]`.first()` с таймаутом 15 сек (suspend)
     *  2. [globalObj.readConfigFormDevice] — `HW*` → `prop*`
     *  3. [globalObj.writeConfigParameters] — сохранить в SharedPreferences
     *  4. [reloadConfigParameters] — обновить UI настроек
     *  5. Toast «Настройки загружены с прибора»
     *
     * Иначе — просто Toast «MAC сохранён» без подключения.
     */
    private fun applySelectedMac(mac: String) {
        GO.LEMAC = mac
        GO.textMACADR.setText(mac)
        GO.PP.setPropStr(GO.propCfgADDRESS, mac)
        if (!GO.autoLoadDeviceCfg) {
            Toast.makeText(requireContext(), "MAC сохранён: $mac", Toast.LENGTH_SHORT).show()
            // Стартуем таймер автоподключения как в обычном flow Save
            GO.tmFull.startTimer()
            return
        }

        Toast.makeText(requireContext(), "Подключение к $mac…", Toast.LENGTH_SHORT).show()
        // Запускаем подключение и ждём первый фрейм с HardwareConfig
        GO.tmFull.startTimer()
        GO.BTT.initLeDevice()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val frame = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                    GO.BTT.deviceFrames.first()
                }
                if (frame == null) {
                    Toast.makeText(requireContext(), "Не удалось прочитать настройки (таймаут)", Toast.LENGTH_LONG).show()
                    return@launch
                }
                // Прибор уже прислал свой фрейм с HardwareConfig — applyFrameToState в MainActivity
                // успел положить hw-значения в GO.HW*-поля. Переносим их в prop*-поля и сохраняем.
                GO.readConfigFormDevice()
                GO.writeConfigParameters()
                reloadConfigParameters()
                Toast.makeText(requireContext(), "Настройки загружены с прибора", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("BluZ-BT", "Auto-load device cfg failed", e)
                Toast.makeText(requireContext(), "Ошибка чтения настроек: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Диалог калибровки АКБ. Показывает текущее измеренное напряжение, принимает реальное
     * (через мультиметр) от пользователя и шлёт его в прибор (`sendCommand(7u)` + float
     * в `sendBuffer[4..7]`).
     *
     * **UX:**
     *  - Заголовок «Калибровка АКБ»
     *  - Текст «Напряжение: %.2f В» (текущее `GO.battLevel`)
     *  - EditText с hint «введите измеренное», `DigitsKeyListener("0123456789.,")` — принимает оба разделителя
     *  - Диапазон 2.00..5.00 В; вне — Toast с ошибкой
     *
     * Не открывается если прибор не подключён.
     */
    private fun showBatteryCalibrationDialog() {
        if (!GO.BTT.connected) {
            Toast.makeText(requireContext(), "Прибор не подключён", Toast.LENGTH_SHORT).show()
            return
        }

        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val currentVoltageText = TextView(ctx).apply {
            text = "Напряжение: %.2f В".format(GO.battLevel)
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.bz_text))
        }
        container.addView(currentVoltageText)

        val input = EditText(ctx).apply {
            hint = "введите измеренное"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.,")
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }
        container.addView(input)

        AlertDialog.Builder(ctx)
            .setTitle("Калибровка АКБ")
            .setView(container)
            .setPositiveButton("Отправить") { _, _ ->
                val raw = input.text.toString().trim().replace(',', '.')
                val value = raw.toFloatOrNull()
                if (value == null || value < 2.0f || value > 5.0f) {
                    Toast.makeText(
                        ctx,
                        "Некорректное значение. Допустимо 2.00–5.00 В",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }
                /* Передача напряжения в прибор (команда 7) */
                val convVal = ByteBuffer.allocate(4).putFloat(value).array()
                GO.BTT.sendBuffer[4] = convVal[0].toUByte()
                GO.BTT.sendBuffer[5] = convVal[1].toUByte()
                GO.BTT.sendBuffer[6] = convVal[2].toUByte()
                GO.BTT.sendBuffer[7] = convVal[3].toUByte()
                GO.BTT.sendCommand(7u)
                GO.battLevel = value
                GO.showStatistics()
                Toast.makeText(ctx, "Калибровка отправлена: %.2f В".format(value), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
            .show()
    }

    // ───────────────────── Автокалибровка по Ra-226 ─────────────────────

    /**
     * Создаёт `AlertDialog` с надутым layout-файлом из `res/layout` и убирает системный
     * фон окна (заменяет на прозрачный `ColorDrawable`).
     *
     * **Зачем убирать фон.** Стандартный `AlertDialog` рисует свой светлый фон под
     * контентом. Наши XML-разметки в `dialog_bz_*.xml` сами устанавливают `bz_bg`
     * на корневой `LinearLayout`. Без прозрачного фона диалога получалась бы «двойная
     * рамка»: светлая системная подложка вокруг тёмного `bz_bg`.
     *
     * @param layoutRes ID разметки `R.layout.dialog_bz_*` со своим фоном/паддингами.
     * @param cancelable `false` — нельзя закрыть кнопкой Back и тапом снаружи; нужно
     *   для прогресс-диалогов и UserPrompt'ов, чтобы пользователь не оборвал процедуру
     *   случайным тапом.
     * @return пара (диалог, корневой View разметки) — вызывающий код находит свои `findViewById`.
     */
    private fun makeStyledBzDialog(layoutRes: Int, cancelable: Boolean = true): Pair<AlertDialog, View> {
        val view = LayoutInflater.from(requireContext()).inflate(layoutRes, null, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(cancelable)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        return dialog to view
    }

    /**
     * Диалог выбора режима автокалибровки по Ra-226. Три варианта (см. [AutoCalibrationController.Mode]):
     *  - **Полная** ([Mode.PRIMARY][AutoCalibrationController.Mode.PRIMARY]) — ВВ + компаратор
     *    + полином, суммарный набор ≈2.5М импульсов.
     *  - **Высокое и компаратор** ([Mode.HV_AND_COMPARATOR][AutoCalibrationController.Mode.HV_AND_COMPARATOR])
     *    — ВВ + компаратор без пересчёта полинома, ≈1М импульсов.
     *  - **Только полином** ([Mode.POLYNOMIAL_ONLY][AutoCalibrationController.Mode.POLYNOMIAL_ONLY])
     *    — ВВ/компаратор не трогаем, ≈1.5М импульсов.
     *
     * Если прибор не подключён — Toast и выход: алгоритм требует двусторонней связи
     * (читать спектр + писать настройки), без онлайна не имеет смысла. После выбора
     * режима показывается универсальный инструктаж ([showAutoCalibrationInstructionAndStart]).
     */
    private fun showAutoCalibrationModeDialog() {
        if (!GO.BTT.connected) {
            Toast.makeText(requireContext(), "Прибор не подключён", Toast.LENGTH_SHORT).show()
            return
        }
        val (dialog, view) = makeStyledBzDialog(R.layout.dialog_bz_autocalib_mode)
        val select = { mode: AutoCalibrationController.Mode ->
            dialog.dismiss()
            showAutoCalibrationInstructionAndStart(mode)
        }
        view.findViewById<Button>(R.id.bzCalibModeFull).setOnClickListener {
            select(AutoCalibrationController.Mode.PRIMARY)
        }
        view.findViewById<Button>(R.id.bzCalibModeHvComp).setOnClickListener {
            select(AutoCalibrationController.Mode.HV_AND_COMPARATOR)
        }
        view.findViewById<Button>(R.id.bzCalibModePolyOnly).setOnClickListener {
            select(AutoCalibrationController.Mode.POLYNOMIAL_ONLY)
        }
        view.findViewById<Button>(R.id.bzCalibModeCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Универсальная инструкция перед запуском любого режима. Время накопления не
     * указываем — оно зависит от активности источника; информативнее количество
     * импульсов в названии режима. Действия с источником сопровождаются звуковым
     * сигналом прибора (`cmd_find_device`).
     */
    private fun showAutoCalibrationInstructionAndStart(mode: AutoCalibrationController.Mode) {
        showStyledMessage(
            title = "Подготовка",
            body = "Вам потребуется источник Ra-226. Не размещайте прибор и источник рядом " +
                "без указания приложения. Во время калибровки экран не будет гаснуть. Когда " +
                "потребуется действие с источником и прибором, прибор издаст сигнал.\n\n" +
                "Автокалибровка может ошибаться. В некоторых случаях вам придётся " +
                "устанавливать настройки вручную.",
            positive = "Начать",
            onPositive = { launchAutoCalibration(mode) },
        )
    }

    /**
     * Показывает универсальный 2-кнопочный стилизованный диалог поверх `dialog_bz_message.xml`.
     *
     * Используется как для подтверждений («Начать?»), так и для финальных сообщений
     * («Прервано: …») — в последнем случае вызывающий код может скрыть кнопку
     * `bzMsgNegative` через `findViewById(...)?.visibility = View.GONE`.
     *
     * @param title заголовок диалога (`bzMsgTitle`).
     * @param body основной текст (`bzMsgBody`), может быть многострочным.
     * @param positive подпись правой кнопки (зелёная подсветка `bz_accent`).
     * @param onPositive колбэк по «положительной» кнопке. Диалог закрывается ДО вызова.
     * @param negative подпись левой кнопки, по умолчанию «Отмена».
     * @param onNegative колбэк по «отрицательной» кнопке, опциональный.
     * @return `AlertDialog` — на случай если внешний код хочет программно отменить
     *   через `dismiss()` или модифицировать `findViewById(...)` уже показанного диалога.
     */
    private fun showStyledMessage(
        title: String,
        body: String,
        positive: String,
        onPositive: () -> Unit,
        negative: String = "Отмена",
        onNegative: (() -> Unit)? = null,
    ): AlertDialog {
        val (dialog, view) = makeStyledBzDialog(R.layout.dialog_bz_message)
        view.findViewById<TextView>(R.id.bzMsgTitle).text = title
        view.findViewById<TextView>(R.id.bzMsgBody).text = body
        val posBtn = view.findViewById<Button>(R.id.bzMsgPositive)
        posBtn.text = positive
        posBtn.setOnClickListener { dialog.dismiss(); onPositive() }
        val negBtn = view.findViewById<Button>(R.id.bzMsgNegative)
        negBtn.text = negative
        negBtn.setOnClickListener { dialog.dismiss(); onNegative?.invoke() }
        dialog.show()
        return dialog
    }

    /**
     * Запускает процедуру и подписывается на [AutoCalibrationController.state] —
     * обновляет прогресс-диалог, ловит финальный AWAITING_APPLY и переключается
     * на диалог результата. При failure показывает диалог ошибки.
     */
    private fun launchAutoCalibration(mode: AutoCalibrationController.Mode) {
        launchControllerProcedure(dialogTitle = "Автокалибровка") { ctrl -> ctrl.start(mode) }
    }

    /** Standalone подбор только компаратора при текущих ВВ и полиноме. */
    private fun showStandaloneComparatorTuningDialog() {
        if (!GO.BTT.connected) {
            Toast.makeText(requireContext(), "Прибор не подключён", Toast.LENGTH_SHORT).show()
            return
        }
        showStyledMessage(
            title = "Подбор компаратора",
            body = "Подбор выполняется при естественном фоне. Не используйте во время " +
                "измерения и/или при воздействии повышенного излучения.",
            positive = "Начать",
            onPositive = {
                launchControllerProcedure(dialogTitle = "Подбор компаратора") { ctrl ->
                    ctrl.startComparatorOnly()
                }
            },
        )
    }

    /**
     * Универсальная UI-обёртка для запуска любой процедуры [AutoCalibrationController].
     *
     * Объединяет общую механику двух точек входа — полной автокалибровки
     * ([launchAutoCalibration]) и standalone-подбора компаратора ([showStandaloneComparatorTuningDialog]):
     *
     *  1. Создаёт свежий [AutoCalibrationController] на `viewLifecycleOwner.lifecycleScope`
     *     (живёт ровно столько же, сколько фрагмент в фоновом состоянии).
     *  2. Включает `FLAG_KEEP_SCREEN_ON` — за длительную процедуру (5–15 мин) экран бы успел
     *     потухнуть, корутины бы не остановились но сценарий с UserPrompt'ом сломался бы.
     *  3. Показывает стилизованный прогресс-диалог `dialog_bz_autocalib_progress.xml`
     *     с заголовком [dialogTitle], кнопкой «Отмена» (вызывает `controller.cancel()`).
     *  4. Подписывается на `controller.state` — на каждый эмит обновляет заголовок фазы,
     *     текущее сообщение, прогресс-бар, строку параметров, и реагирует на:
     *      - `userPrompt != null` — показывает дочерний диалог-prompt с кнопкой «Готово».
     *        Каждый из 4 типов `UserPrompt` имеет свои заголовок и текст, заданные
     *        здесь жёстко (не в контроллере) — позволяет UI оставаться единственным
     *        источником правды для пользователя.
     *      - `Phase.AWAITING_APPLY` + `pendingResult != null` — закрывает прогресс и
     *        открывает [showAutoCalibrationResultDialog].
     *      - `Phase.DONE` — Toast «Завершена», перезагрузка значений в форме настроек.
     *      - `failure != null` — диалог ошибки.
     *  5. Запускает фактическое действие через [startAction] (typically `ctrl.start(mode)` или
     *     `ctrl.startComparatorOnly()`).
     *
     * @param dialogTitle отображается в шапке прогресс-диалога («Автокалибровка» / «Подбор компаратора»).
     * @param startAction вызывается ПОСЛЕ настройки UI и подписки на state. Получает свежий
     *   контроллер и решает что именно с ним делать.
     */
    private fun launchControllerProcedure(
        dialogTitle: String,
        startAction: (AutoCalibrationController) -> Unit,
    ) {
        val ctx = requireContext()
        val controller = AutoCalibrationController(viewLifecycleOwner.lifecycleScope)

        // Не даём экрану гаснуть на время процедуры.
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val releaseKeepScreenOn = {
            if (isAdded) requireActivity().window.clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val (progressDialog, progView) = makeStyledBzDialog(
            R.layout.dialog_bz_autocalib_progress, cancelable = false)
        progView.findViewById<TextView>(R.id.bzCalibProgTitle).text = dialogTitle
        val phaseTv = progView.findViewById<TextView>(R.id.bzCalibProgPhase)
        val msgTv = progView.findViewById<TextView>(R.id.bzCalibProgMsg)
        val progressBar = progView.findViewById<android.widget.ProgressBar>(R.id.bzCalibProgBar)
        val paramsTv = progView.findViewById<TextView>(R.id.bzCalibProgParams)
        progView.findViewById<Button>(R.id.bzCalibProgCancel).setOnClickListener {
            controller.cancel()
            progressDialog.dismiss()
            releaseKeepScreenOn()
        }
        progressDialog.show()

        var promptDialog: AlertDialog? = null

        // Подписка на StateFlow. Корутина живёт вместе с view-lifecycle.
        viewLifecycleOwner.lifecycleScope.launch {
            controller.state.collect { st ->
                if (!isAdded) return@collect

                phaseTv.text = phaseTitle(st.phase)
                msgTv.text = st.message
                progressBar.isIndeterminate = (st.progressPercent == null && st.phase != AutoCalibrationController.Phase.IDLE)
                if (st.progressPercent != null) progressBar.progress = st.progressPercent
                paramsTv.text = paramsLine(st)

                // Запросы пользователю — отдельный стилизованный диалог поверх прогресса.
                val prompt = st.userPrompt
                if (prompt != null && promptDialog == null) {
                    val (title, msg) = when (prompt) {
                        AutoCalibrationController.UserPrompt.RemoveSourceForCompTuning ->
                            "Уберите источник" to "Уберите источник Ra-226 от прибора. Будет подобран уровень компаратора по чистому фону."
                        AutoCalibrationController.UserPrompt.PlaceSourceForCalibration ->
                            "Верните источник" to "Поднесите источник Ra-226 к прибору и зафиксируйте. Будет подобрано высокое напряжение и накоплен спектр."
                        AutoCalibrationController.UserPrompt.RemoveSourceForBackgroundCheck ->
                            "Уберите источник" to "Уберите источник Ra-226 от прибора. После этого начнётся проверка собственного шума прибора."
                        AutoCalibrationController.UserPrompt.PlaceSourceForPolynomOnly ->
                            "Поднесите источник" to "Поднесите источник Ra-226 к прибору и зафиксируйте до конца процедуры. Будет накоплен спектр и пересчитан полином."
                    }
                    val (pd, pView) = makeStyledBzDialog(R.layout.dialog_bz_message, cancelable = false)
                    pView.findViewById<TextView>(R.id.bzMsgTitle).text = title
                    pView.findViewById<TextView>(R.id.bzMsgBody).text = msg
                    pView.findViewById<Button>(R.id.bzMsgNegative).visibility = View.GONE
                    val posBtn = pView.findViewById<Button>(R.id.bzMsgPositive)
                    posBtn.text = "Готово"
                    posBtn.setOnClickListener {
                        controller.resolveUserPrompt()
                        pd.dismiss()
                        promptDialog = null
                    }
                    promptDialog = pd
                    pd.show()
                }

                // Готов финальный пакет — закрываем прогресс, открываем результат.
                if (st.phase == AutoCalibrationController.Phase.AWAITING_APPLY && st.pendingResult != null) {
                    progressDialog.dismiss()
                    showAutoCalibrationResultDialog(controller, st.pendingResult, releaseKeepScreenOn)
                    return@collect
                }
                if (st.phase == AutoCalibrationController.Phase.DONE) {
                    progressDialog.dismiss()
                    releaseKeepScreenOn()
                    Toast.makeText(ctx, "Автокалибровка завершена", Toast.LENGTH_LONG).show()
                    reloadConfigParameters()
                    return@collect
                }
                if (st.failure != null) {
                    progressDialog.dismiss()
                    releaseKeepScreenOn()
                    showStyledMessage(
                        title = "Автокалибровка прервана",
                        body = st.failure,
                        positive = "OK",
                        onPositive = {},
                    ).also { d ->
                        // У этого диалога не нужна Отмена — скрываем.
                        d.findViewById<Button>(R.id.bzMsgNegative)?.visibility = View.GONE
                    }
                    return@collect
                }
            }
        }

        startAction(controller)
    }

    /** Локализованный заголовок текущей фазы. */
    private fun phaseTitle(phase: AutoCalibrationController.Phase): String = when (phase) {
        AutoCalibrationController.Phase.IDLE -> "Подготовка"
        AutoCalibrationController.Phase.HV_TUNING -> "Подбор высокого напряжения"
        AutoCalibrationController.Phase.COMPARATOR_TUNING -> "Подбор уровня компаратора"
        AutoCalibrationController.Phase.ACCUMULATING -> "Накопление спектра"
        AutoCalibrationController.Phase.ANALYZING -> "Анализ"
        AutoCalibrationController.Phase.BACKGROUND_CHECK -> "Проверка фона"
        AutoCalibrationController.Phase.AWAITING_APPLY -> "Готово"
        AutoCalibrationController.Phase.APPLYING -> "Запись в прибор"
        AutoCalibrationController.Phase.DONE -> "Готово"
    }

    /** Однострочная сводка текущих параметров для нижней строки прогресс-диалога. */
    private fun paramsLine(st: AutoCalibrationController.State): String {
        val parts = mutableListOf<String>()
        if (st.currentHv > 0) parts += "ВВ ${st.currentHv}"
        if (st.currentComparator > 0) parts += "Комп ${st.currentComparator}"
        if (st.iteration > 0) parts += "итерация ${st.iteration}"
        return parts.joinToString(" · ")
    }

    /**
     * Финальный диалог результата с найденными пиками, невязкой и сравнением «было → стало».
     * По «Применить» — отправляет все параметры в прибор через [AutoCalibrationController.applyPending].
     */
    private fun showAutoCalibrationResultDialog(
        controller: AutoCalibrationController,
        result: AutoCalibrationController.PendingResult,
        releaseKeepScreenOn: () -> Unit,
    ) {
        val sb = StringBuilder()
        sb.append("ВВ:        ${result.oldHv} → ${result.newHv}\n")
        sb.append("Компаратор: ${result.oldComparator} → ${result.newComparator}\n\n")
        sb.append("Полином:\n")
        sb.append("  A = %.7f\n".format(result.cA))
        sb.append("  B = %.5f\n".format(result.cB))
        sb.append("  C = %.2f\n".format(result.cC))
        sb.append("Верх шкалы: %.0f кэВ (цель 3500)\n".format(result.eAtLastChannel))
        if (result.peaks.isNotEmpty()) {
            sb.append("\nНайденные пики Ra-226 (невязка %.2f кэВ):\n".format(result.residualKev))
            for (p in result.peaks) {
                val predicted = result.cA * p.channel * p.channel + result.cB * p.channel + result.cC
                val diff = predicted - p.expectedEnergyKev
                sb.append("  %d кэВ → канал %.1f (Δ %+.2f кэВ, %d счётов)\n"
                    .format(p.expectedEnergyKev, p.channel, diff, p.countsInWindow))
            }
        }

        val (dialog, view) = makeStyledBzDialog(R.layout.dialog_bz_autocalib_result, cancelable = false)
        view.findViewById<TextView>(R.id.bzCalibResultBody).text = sb.toString()
        view.findViewById<Button>(R.id.bzCalibResultApply).setOnClickListener {
            dialog.dismiss()
            controller.applyPending()
        }
        view.findViewById<Button>(R.id.bzCalibResultCancel).setOnClickListener {
            dialog.dismiss()
            releaseKeepScreenOn()
        }
        dialog.show()
    }

    /**
     * Обработчик переключения стиля графика спектра (линия / гистограмма) в SeekBar-ах
     * настройки цветов. Подменяет соответствующий цвет в `GO.Color*` и перерисовывает
     * превью через [drawExmple.exampRedraw].
     *
     * @param checkedId ID выбранного RadioButton (rbLine / rbHistogram).
     */
    private fun changeSpectrType(checkedId: Int) {
        noChange = false
        if (GO.rbLineSpectr.isChecked) {
            when (checkedId) {
                rbLine.id -> {
                    selA.setProgress(Color.alpha(GO.ColorLin), false)
                    selR.setProgress(Color.red(GO.ColorLin), false)
                    selG.setProgress(Color.green(GO.ColorLin), false)
                    selB.setProgress(Color.blue(GO.ColorLin), false)
                }
                rbLg.id -> {
                    selA.setProgress(Color.alpha(GO.ColorLog), false)
                    selR.setProgress(Color.red(GO.ColorLog), false)
                    selG.setProgress(Color.green(GO.ColorLog), false)
                    selB.setProgress(Color.blue(GO.ColorLog), false)
                }
                rbFoneLin.id -> {
                    selA.setProgress(Color.alpha(GO.ColorFone), false)
                    selR.setProgress(Color.red(GO.ColorFone), false)
                    selG.setProgress(Color.green(GO.ColorFone), false)
                    selB.setProgress(Color.blue(GO.ColorFone), false)
                }
                rbFoneLg.id -> {
                    selA.setProgress(Color.alpha(GO.ColorFoneLg), false)
                    selR.setProgress(Color.red(GO.ColorFoneLg), false)
                    selG.setProgress(Color.green(GO.ColorFoneLg), false)
                    selB.setProgress(Color.blue(GO.ColorFoneLg), false)
                }
            }
        } else if (GO.rbGistogramSpectr.isChecked) {
            when (checkedId) {
                rbLine.id -> {
                    selA.setProgress(Color.alpha(GO.ColorLinGisto), false)
                    selR.setProgress(Color.red(GO.ColorLinGisto), false)
                    selG.setProgress(Color.green(GO.ColorLinGisto), false)
                    selB.setProgress(Color.blue(GO.ColorLinGisto), false)
                }
                rbLg.id -> {
                    selA.setProgress(Color.alpha(GO.ColorLogGisto), false)
                    selR.setProgress(Color.red(GO.ColorLogGisto), false)
                    selG.setProgress(Color.green(GO.ColorLogGisto), false)
                    selB.setProgress(Color.blue(GO.ColorLogGisto), false)
                }
                rbFoneLin.id -> {
                    selA.setProgress(Color.alpha(GO.ColorFoneGisto), false)
                    selR.setProgress(Color.red(GO.ColorFoneGisto), false)
                    selG.setProgress(Color.green(GO.ColorFoneGisto), false)
                    selB.setProgress(Color.blue(GO.ColorFoneGisto), false)
                }
                rbFoneLg.id -> {
                    selA.setProgress(Color.alpha(GO.ColorFoneLgGisto), false)
                    selR.setProgress(Color.red(GO.ColorFoneLgGisto), false)
                    selG.setProgress(Color.green(GO.ColorFoneLgGisto), false)
                    selB.setProgress(Color.blue(GO.ColorFoneLgGisto), false)
                }
            }
        }
        GO.drawExamp.exampRedraw()
        noChange = true
    }

    /**
     * Обновляет все UI-элементы настроек (EditText, CheckBox, RadioButton) значениями
     * из `GO.prop*` и `GO.HW*` полей.
     *
     * Вызывается:
     *  - В [onViewCreated] для начального заполнения
     *  - После [readConfigFormDevice][globalObj.readConfigFormDevice] (кнопка Read)
     *  - После [applySelectedMac] с включённым auto-load switch
     *
     * Также дёргает [globalObj.updateSpecSubtitle] — коэффициенты могли измениться.
     */
    fun reloadConfigParameters() {
        GO.propButtonInit = false
        if (GO.LEMAC.isNotEmpty()) {
            GO.textMACADR.setText(GO.LEMAC)
        }
        // Коэффициенты калибровки и разрешение могли измениться — обновляем subtitle на Spectrum tab.
        GO.updateSpecSubtitle()
        when (GO.saveTrackType) {
            0 -> GO.rbKMLType.isChecked = true
            1 -> GO.rbGPXType.isChecked = true
        }
        when (GO.saveSpecterType) {
            0 -> GO.rbSpctTypeBq.isChecked = true
            1 -> GO.rbSpctTypeSPE.isChecked = true
        }
        when (GO.specterGraphType) {
            0 -> GO.rbLineSpectr.isChecked = true
            1 -> GO.rbGistogramSpectr.isChecked = true
        }
        when (GO.propSoundKvant) {
            0 -> GO.rbClickNone.isChecked = true
            1 -> GO.rbClick1.isChecked = true
            2 -> GO.rbClick10.isChecked = true
            else -> GO.rbClickNone.isChecked = true
        }
        when (GO.propLedKvant) {
            0 -> GO.rbLedNone.isChecked = true
            1 -> GO.rbLed1.isChecked = true
            2 -> GO.rbLed10.isChecked = true
            else -> GO.rbLedNone.isChecked = true
        }
        GO.cbSpectrometr.isChecked = GO.propAutoStartSpectrometr
        GO.editRejectChann.setText(GO.rejectChann.toString())
        when (GO.spectrResolution) {
            0 -> GO.rbResolution1024.isChecked = true
            1 -> GO.rbResolution2048.isChecked = true
            2 -> GO.rbResolution4096.isChecked = true
            else -> GO.rbResolution1024.isChecked = true
        }
        GO.enrgCalc.rS = GO.spectrResolution
        GO.editLevel1.setText(GO.propLevel1.toString())
        GO.editLevel2.setText(GO.propLevel2.toString())
        GO.editLevel3.setText(GO.propLevel3.toString())
        GO.cbSoundLevel1.isChecked = GO.propSoundLevel1
        GO.cbSoundLevel2.isChecked = GO.propSoundLevel2
        GO.cbSoundLevel3.isChecked = GO.propSoundLevel3
        GO.cbVibroLevel1.isChecked = GO.propVibroLevel1
        GO.cbVibroLevel2.isChecked = GO.propVibroLevel2
        GO.cbVibroLevel3.isChecked = GO.propVibroLevel3
        val df = DecimalFormat(GO.acuricyPatern, DecimalFormatSymbols(Locale.US))
        GO.editPolinomA.setText(df.format(GO.propCoef4096A))
        GO.editPolinomB.setText(df.format(GO.propCoef4096B))
        GO.editPolinomC.setText(df.format(GO.propCoef4096C))
        GO.editPolinomD.setText(df.format(GO.propCoef4096D))
        GO.editPolinomE.setText(df.format(GO.propCoef4096E))
        GO.editCPS2Rh.setText(GO.propCPS2UR.toString())
        GO.editHVoltage.setText(GO.propHVoltage.toString())
        GO.editComparator.setText(GO.propComparator.toString())
        GO.editSMA.setText(GO.windowSMA.toString())
        GO.propButtonInit = true
        GO.aqureEdit.setText(GO.aqureValue.toString())
        GO.bitsChannelEdit.setText(GO.bitsChannel.toString())
        GO.cbFullScrn.isChecked = GO.fullScrn
        GO.sampleTimeEdit.setText(GO.sampleTime.toString())
        GO.textAppLogLevel.setText(GO.appLogLevel.toString())
        paddingTextLeft.setText(GO.paddingLeft.toString())
        paddingTextRight.setText(GO.paddingRight.toString())
        GO.textXZoom.setText(GO.xZoom.toString())
        when (GO.unitsMess) {
            1 -> rbuSvh.isChecked = true
            else -> rbuRh.isChecked = true
        }
        textDetectName.text = GO.curretnDetectorName
    }

    /**
     * Конвертирует массив байт (little-endian double, 8 байт на элемент) в [DoubleArray].
     * Используется в [loadChiFile] для парсинга бинарного формата χ-вектора.
     */
    private fun bytesToDoubleArray(bytes: ByteArray): DoubleArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return DoubleArray(bytes.size / 8) { buffer.getDouble() }
    }

    /**
     * Загружает χ-вектор (1024 значения Double, little-endian, 8192 байта) из файла по
     * [Uri], создаёт новую запись [data.entity.DetectorType] в БД и помечает её активной.
     *
     * Используется в импорте детектора из файла. После успешной загрузки [DoseCalculator.chiVectorOrg]
     * обновляется и MLEM-расчёт начнёт использовать новый χ.
     */
    private fun loadChiFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val chiVector = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        if (bytes.size % 8 != 0) {
                            throw IllegalArgumentException("Illegal file size: size not multiple 8")
                        }
                        bytesToDoubleArray(bytes)
                    } ?: throw IOException("File read error")
                }
                if (chiVector.size != 1024) {
                    Toast.makeText(requireContext(), "Illigal load CHI, size is ${chiVector.size}.", Toast.LENGTH_LONG).show()
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val buffBlob = ByteBuffer.allocate(chiVector.size * 8).order(ByteOrder.LITTLE_ENDIAN)
                            chiVector.forEach { buffBlob.putDouble(it) }
                            val chiBlob = buffBlob.array()
                            val rowsAffect = GO.dao.editDetectorCHI(GO.currentDetector, chiBlob)
                            if (rowsAffect == 1) {
                                DoseCalculator.chiVectorOrg = chiVector
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Vector saved success.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Update error CHI.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                Log.e("BluZ-BT", "Error: ${e.message}", e)
                                Toast.makeText(requireContext(), "Save error CHI: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluZ-BT", "Error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Возвращает отображаемое имя файла из [Uri] через ContentResolver / OpenableColumns. */
    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
        return name ?: uri.lastPathSegment
    }

    /** Обработчик результата выбора файла через `ActivityResultContracts.OpenDocument`.
     *  Проверяет, что имя файла соответствует ожидаемому формату, и запускает [loadChiFile]. */
    private fun handleChiFileSelection(uri: Uri) {
        val fileName = getFileNameFromUri(uri) ?: "unknown"
        if (!fileName.endsWith(".chi", ignoreCase = true)) {
            AlertDialog.Builder(requireContext())
                .setTitle("️Format incorrect.")
                .setMessage("Selected: $fileName\nExpect extension .chi\nContinue ?")
                .setPositiveButton("Yes") { _, _ -> loadChiFile(uri) }
                .setNegativeButton("No", null)
                .show()
        } else {
            loadChiFile(uri)
        }
    }
}
