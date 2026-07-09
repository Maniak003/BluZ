package ru.starline.bluz

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {

    private var currentScaleX = 1f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.history_layout, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Phase D: register hero refs (nullable; null when fragment not visible)
        val CBSMA: CheckBox = view.findViewById(R.id.cbHistorySMA)
        GO.bzHistDuration = view.findViewById(R.id.bzHistDuration)
        GO.bzHistIntegralValue = view.findViewById(R.id.bzHistIntegralValue)
        GO.bzHistAvgCpsValue = view.findViewById(R.id.bzHistAvgCpsValue)
        GO.bzHistDuration?.setOnClickListener {
            GO.clockShowSecondsHist = !GO.clockShowSecondsHist
            GO.showStatistics()
        }

        GO.drawHISTORY.imgView = view.findViewById(R.id.historyView)
        GO.drawHistoryCURSOR.cursorView = view.findViewById(R.id.historyCursorView)
        GO.txtHistoryIsotop = view.findViewById(R.id.textHistoryIsotop)

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

        /* Курсор для гистограммы истории */
        GO.drawHistoryCURSOR.cursorView.apply {
            isClickable = true
            isFocusable = true
        }

        GO.drawHISTORY.imgView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawHISTORY.imgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    GO.drawHistoryCursorObjectInit = true
                    GO.drawHISTORY.init()
                    GO.drawHISTORY.clearHistory()
                    GO.drawHISTORY.redrawSpecter(GO.specterType)
                }
            }
        )


        // Пересоздание bitmap курсора под текущие размеры cursorView (важно после поворота:
        // у нового view другие width/height, старый bitmap не подходит — курсор рисуется криво).
        GO.drawHistoryCURSOR.cursorView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    GO.drawHistoryCURSOR.cursorView.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    )
                    GO.drawHistoryCursorObjectInit = true
                    GO.drawHistoryCURSOR.init()
                }
            }
        )

        CBSMA.isChecked = GO.drawHISTORY.flagSMA
        CBSMA.setOnCheckedChangeListener { _, isChecked ->
            if (GO.propButtonInit) {
                GO.drawHISTORY.flagSMA = isChecked
                if (GO.drawHISTORY.VSize > 0 && GO.drawHISTORY.HSize > 0) {
                    GO.drawHISTORY.clearHistory()
                    GO.drawHISTORY.redrawSpecter(GO.specterType)
                } else {
                    Log.e("BluZ-BT", "drawSPEC is null")
                }
            }
        }


        var xPos = 0.0f
        var lastDist = 0.0f
        var resizeKf = 0.0f

        GO.drawHistoryCURSOR.cursorView.setOnTouchListener { _, event ->
            when {
                event.pointerCount == 2 -> {
                    val x1 = event.getX(0)
                    val x2 = event.getX(1)
                    val dist = kotlin.math.hypot(x1 - x2, x2 - x1)
                    if (xPos > 0) {
                        GO.xPositionHistory += (xPos - x1) * 0.5f
                        if (GO.xPositionHistory < 0) {
                            GO.xPositionHistory = 0.0f
                        } else if (0 > GO.drawHISTORY.ResolutionHistory * GO.drawHISTORY.xSize - GO.drawHISTORY.HSize - GO.xPositionHistory) {
                            GO.xPositionHistory =
                                (GO.drawHISTORY.ResolutionHistory * GO.drawHISTORY.xSize - GO.drawHISTORY.HSize).toFloat()
                        }
                    }
                    if (lastDist > 0) {
                        resizeKf = kotlin.math.abs(dist / lastDist)
                        GO.xZoomHistory = GO.xZoomHistory * resizeKf
                        if (GO.xZoomHistory < 1) {
                            GO.xZoomHistory = 1.0f
                        } else if (GO.xZoomHistory > 5) {
                            GO.xZoomHistory = 5.0f
                        }
                        GO.drawHISTORY.clearHistory()
                        //GO.drawHISTORY.redrawSpecter(GO.specterType, GO.xPosition)
                        GO.drawHISTORY.redrawSpecter(GO.specterType)
                    }
                    lastDist = dist
                    xPos = x1
                    //Log.i("BluZ-BT","X1: $x1, Resol: ${GO.drawHISTORY.ResolutionHistory}, HSize: ${GO.drawHISTORY.HSize}, xSize: ${GO.drawHISTORY.xSize}, Xpos: ${GO.xPositionHistory}, Scale: ${GO.xZoomHistory}")
                    true
                }

                event.pointerCount == 1 -> {
                    lastDist = 0.0f
                    xPos = 0.0f
                    val rawX = event.x
                    val rawY = event.y
                    if (event.action == MotionEvent.ACTION_DOWN ||
                        event.action == MotionEvent.ACTION_MOVE
                    ) {
                        val correctedX = rawX / currentScaleX.coerceAtLeast(0.001f)
                        if (GO.drawHistoryCURSOR.oldX != correctedX) {
                            GO.drawHistoryCURSOR.showCorsor(correctedX, rawY)
                        }
                    }
                    true
                }

                else -> false
            }
        }
    }


    override fun onDestroyView() {
        GO.bzHistDuration = null
        GO.bzHistIntegralValue = null
        GO.bzHistAvgCpsValue = null
        GO.txtHistoryIsotop = null
        super.onDestroyView()
    }
}
