package ru.starline.bluz

import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.doOnLayout // <-- Не забудьте этот импорт
import androidx.fragment.app.Fragment

/**
 * Created by ed on 02,ноябрь,2026
 */
class SpectrogrammaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.sgram_layout, container, false)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GO.sGramView = view.findViewById(R.id.sGrammaView)

        // doOnLayout выполнится, когда у sGramView появятся реальные размеры
        GO.sGramView.doOnLayout { imageView ->
            val hSize = imageView.width
            val vSize = imageView.height

            if (hSize == 0 || vSize == 0) {
                Log.e("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize")
                return@doOnLayout
            }

            if (! GO.drawSGinit) {
                GO.drawSG = drawSGramm()
                GO.drawSG.init(hSize, vSize)
            }
            //GO.drawSG.redraw()

            // Принудительно обновляем ImageView, чтобы отрисовка сразу отобразилась
            imageView.invalidate()
        }

    }
}