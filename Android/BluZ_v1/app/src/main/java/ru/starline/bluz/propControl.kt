package ru.starline.bluz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Created by ed on 21,июнь,2024
 */
class propControl {
    var prop: SharedPreferences

    fun setPropStr(key: String, value: String) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putString(key, value)
        ed.commit()
    }

    fun setPropInt(key: String, value: Int) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putInt(key, value)
        ed.commit()
    }

    fun getPropStr(key: String): String {
        return prop.getString(key, "").toString()
    }

    fun getPropInt(key: String): Int {
        return prop.getInt(key, 0)
    }

    fun getPropFloat(key: String): Float {
        return prop.getFloat(key, 0.0f)
    }

    constructor() {
        Log.i("BluZ-BT", "Point 15.1")
        prop = GO.mainContext.getSharedPreferences("device.properties", Context.MODE_PRIVATE)
        Log.i("BluZ-BT", "Point 15.2")
    }
}