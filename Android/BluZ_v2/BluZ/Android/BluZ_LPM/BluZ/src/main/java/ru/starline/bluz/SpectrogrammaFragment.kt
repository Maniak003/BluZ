package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnLayout // <-- Не забудьте этот импорт
import androidx.fragment.app.Fragment

/**
 * Created by ed on 02,ноябрь,2026
 */
class SpectrogrammaFragment : Fragment() {

    private lateinit var sGramView: ImageView
    private lateinit var sGramCanvas: Canvas
    private lateinit var sGramBitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.sgram_layout, container, false)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sGramView = view.findViewById(R.id.sGrammaView)

        // doOnLayout выполнится, когда у sGramView появятся реальные размеры
        sGramView.doOnLayout { imageView ->
            val hSize = imageView.width
            val vSize = imageView.height

            if (hSize == 0 || vSize == 0) { // В Kotlin лучше использовать || вместо or для Boolean
                Log.e("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize")
                return@doOnLayout
            }

            Log.d("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize")

            // Создаем битмап и канвас только когда размеры известны
            sGramBitmap = createBitmap(hSize, vSize)
            sGramCanvas = Canvas(sGramBitmap)
            sGramView.setImageBitmap(sGramBitmap)

            val drwSG: drawSGramm = drawSGramm()
            drwSG.init()
            drwSG.redraw(sGramCanvas)

            // Принудительно обновляем ImageView, чтобы отрисовка сразу отобразилась
            imageView.invalidate()
        }
    }
}