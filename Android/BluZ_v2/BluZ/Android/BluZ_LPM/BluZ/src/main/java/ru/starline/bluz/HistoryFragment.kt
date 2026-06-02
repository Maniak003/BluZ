package ru.starline.bluz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.history_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Phase D: register hero refs (nullable; null when fragment not visible)
        GO.bzHistDuration = view.findViewById(R.id.bzHistDuration)
        GO.bzHistIntegralValue = view.findViewById(R.id.bzHistIntegralValue)
        GO.bzHistAvgCpsValue = view.findViewById(R.id.bzHistAvgCpsValue)
        GO.bzHistDuration?.setOnClickListener {
            GO.clockShowSecondsHist = !GO.clockShowSecondsHist
            GO.showStatistics()
        }

        GO.drawHISTORY.imgView = view.findViewById(R.id.historyView)

        GO.drawHISTORY.imgView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawHISTORY.imgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawObjectInitHistory = true
                    GO.drawHISTORY.init()
                    GO.drawHISTORY.clearHistory()
                    GO.drawHISTORY.redrawSpecter(GO.specterType)
                }
            }
        )

        val btnHistoryLoad: Button = view.findViewById(R.id.buttonLoadHistory)
        btnHistoryLoad.setOnClickListener {
            GO.BTT.sendCommand(5u)
            Toast.makeText(GO.mainContext, R.string.historyRequest, Toast.LENGTH_LONG).show()
        }

        val btnHistorySave: Button = view.findViewById(R.id.buttonHistorySave)
        btnHistorySave.setOnClickListener {
            val saveBqMon = SaveBqMon()
            saveBqMon.saveSpecter()
            Toast.makeText(GO.mainContext, R.string.saveComplete, Toast.LENGTH_LONG).show()
        }

        val btnHistoryClear: ImageButton = view.findViewById(R.id.buttonClearHistory)
        btnHistoryClear.setOnClickListener {
            GO.BTT.sendCommand(8u)
            for (iii in 0 until GO.drawHISTORY.ResolutionHistory) {
                GO.drawHISTORY.historyData[iii] = 0.0
            }
            GO.drawHISTORY.clearHistory()
            GO.drawHISTORY.redrawSpecter(GO.specterType)
        }
    }

    override fun onDestroyView() {
        GO.bzHistDuration = null
        GO.bzHistIntegralValue = null
        GO.bzHistAvgCpsValue = null
        super.onDestroyView()
    }
}
