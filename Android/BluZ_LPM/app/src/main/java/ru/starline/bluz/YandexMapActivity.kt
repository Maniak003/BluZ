package ru.starline.bluz

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.starline.bluz.BuildConfig

class YandexMapActivity : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
    }
}