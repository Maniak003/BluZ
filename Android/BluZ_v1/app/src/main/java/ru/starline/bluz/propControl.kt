package ru.starline.bluz

import android.content.Context
import android.content.SharedPreferences

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

    fun getPropStr(key: String): String {
        return prop.getString(key, "").toString()
    }

    fun getPropFloat(key: String): Float {
        return prop.getFloat(key, 0.0f)
    }

    constructor() {
        prop = mainContext.getSharedPreferences("device.properties", Context.MODE_PRIVATE)
    }
}