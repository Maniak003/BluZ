package ru.starline.bluz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Вкладка дозиметра.
 *
 * **Layout:** `dozimetr_layout.xml`.
 *
 * **Hero readouts** (привязываются здесь, обновляются [MainActivity.applyDoseReadouts]):
 *  - `bzDoseHeroValue` / `bzDoseHeroUnit` — крупная мощность дозы + единица
 *  - `bzDoseAvgLabel` — «Среднее: X.XX мкР/ч»
 *  - `bzDoseStatusPill` — индикатор уровня тревоги (NORMAL / L1 / L2 / L3 / OVERLOAD)
 *  - `bzDoseStatusTitle` — текстовое описание состояния
 *
 * **Canvas дозиметра** ([drawDozimeter.dozView]) — гистограмма 512 значений за последние
 * 512 секунд. Привязка здесь, перерисовка в [MainActivity.applyFrameToUi].
 *
 * **Click handlers:**
 *  - `buttonClearDoze` → `sendCommand(3u)` — сброс дозиметра в приборе
 *  - `bzDoseHeroValue/Unit`, `bzDoseAvgLabel` → [MainActivity.toggleDoseUnits]
 */
class DoseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dozimetr_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GO.drawDOZIMETER.textMax = view.findViewById(R.id.txtMAXDoze)
        GO.drawDOZIMETER.textMin = view.findViewById(R.id.txtMINDoze)
        GO.drawDOZIMETER.dozView = view.findViewById(R.id.dozView)

        // Phase D2: dose hero refs (nullable)
        GO.bzDoseHeroValue = view.findViewById(R.id.bzDoseHeroValue)
        GO.bzDoseHeroUnit = view.findViewById(R.id.bzDoseHeroUnit)
        GO.bzDoseAvgLabel = view.findViewById(R.id.bzDoseAvgLabel)
        GO.bzDoseStatusPill = view.findViewById(R.id.bzDoseStatusPill)
        GO.bzDoseStatusTitle = view.findViewById(R.id.bzDoseStatusTitle)

        val toggleUnits = View.OnClickListener {
            (activity as? MainActivity)?.toggleDoseUnits()
        }
        GO.bzDoseHeroValue?.setOnClickListener(toggleUnits)
        GO.bzDoseHeroUnit?.setOnClickListener(toggleUnits)
        GO.bzDoseAvgLabel?.setOnClickListener(toggleUnits)

        if (!GO.initDOZ) {
            GO.initDOZ = true
            GO.drawDOZIMETER.Init()
        }

        GO.drawDOZIMETER.dozView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawDOZIMETER.dozView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawDozObjectInit = true
                    GO.drawDOZIMETER.Init()
                    GO.drawDOZIMETER.redrawDozimeter()
                }
            }
        )

        val btnClearDose: ImageButton = view.findViewById(R.id.buttonClearDoze)
        btnClearDose.setOnClickListener {
            GO.BTT.sendCommand(3u)
            Toast.makeText(GO.mainContext, R.string.resetDosimeter, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        GO.bzDoseHeroValue = null
        GO.bzDoseHeroUnit = null
        GO.bzDoseAvgLabel = null
        GO.bzDoseStatusPill = null
        GO.bzDoseStatusTitle = null
        super.onDestroyView()
    }
}
