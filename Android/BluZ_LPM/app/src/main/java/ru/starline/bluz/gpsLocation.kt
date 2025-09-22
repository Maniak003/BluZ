package ru.starline.bluz

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.math.toRadians
import com.google.android.gms.location.*
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import kotlin.math.ln
import kotlin.math.sqrt

class ContinuousLocationManager(
    private val context: Bundle,
    private val onLocationUpdate: (Location) -> Unit) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(GO.mainContext)
    private lateinit var locationCallback: LocationCallback

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }
        //setupLocationCallback()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(GO.mainContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Разрешение не получено — нужно запросить
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // каждые 5 сек
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(1000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /* Для расчета дистанции с учетом того, что земля круглая, а не плоская */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Радиус Земли в метрах
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2) *
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return R * c
    }

    /* Расчет zoom по диагонали */
    fun calculateZoomFromDiagonal(mapView: MapView, diagonalMeters: Double): Float {
        val mapWidth = mapView.width.toDouble()
        val mapHeight = mapView.height.toDouble()
        val screenDiagonal = sqrt(mapWidth * mapWidth + mapHeight * mapHeight)

        // Метров на пиксель при зуме
        val metersPerPixelAtZoom0 = 156543.0    // Необходимое разрешение (м/пиксель)
        val requiredMetersPerPixel = diagonalMeters / (screenDiagonal * 0.8) // 80% экрана

        // Уровень зума
        val zoom = log2(metersPerPixelAtZoom0 / requiredMetersPerPixel).toFloat() * 0.8f

        return maxOf(1f, minOf(zoom, 21f))
    }


    fun calculateZoomLevel(mapView: MapView, latDiff: Double, lonDiff: Double, centerLat: Double): Float {
        val mapWidth = mapView.width.toDouble()
        val mapHeight = mapView.height.toDouble()

        // Константа: метров на пиксель при зуме
        val metersPerPixelAtZoom0 = 156543.0
        // Учёт искажения Меркатора: ширина в метрах зависит от широты
        val earthCircumference = 40075000.0 // метров
        val latitudeMeters = latDiff * earthCircumference / 360.0    // Ключевая поправка: используем среднюю широту для долготы
        val avgLatitude = Math.toRadians(centerLat)
        val longitudeMeters = lonDiff * (earthCircumference * kotlin.math.cos(avgLatitude)) / 360.0    // Рассчитываем zoom по ширине и высоте
        val zoomByWidth = if (longitudeMeters > 0) {
            log2(mapWidth * metersPerPixelAtZoom0 / longitudeMeters).toFloat()
        } else {
            21f
        }

        val zoomByHeight = if (latitudeMeters > 0) {
            log2(mapHeight * metersPerPixelAtZoom0 / latitudeMeters).toFloat()
        } else {
            21f
        }

        var zoom = minOf(zoomByWidth, zoomByHeight)
        zoom *= .8f

        return maxOf(1f, minOf(zoom, 21f))
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

}
