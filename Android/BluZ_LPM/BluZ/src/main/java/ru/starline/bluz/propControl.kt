package ru.starline.bluz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Created by ed on 21,июнь,2024
 */
class propControl {
    var prop: SharedPreferences

    fun setPropBoolean(key: String, value: Boolean) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putBoolean(key, value)
        ed.commit()
    }

    fun setPropStr(key: String, value: String) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putString(key, value)
        ed.commit()
    }

    fun setPropFloat(key: String, value: Float) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putFloat(key, value)
        ed.commit()
    }

    fun setPropInt(key: String, value: Int) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putInt(key, value)
        ed.commit()
    }

    fun setPropByte(key:String, value:UByte) {
        var ed: SharedPreferences.Editor = prop.edit()
        ed.putInt(key, value.toInt())
        ed.commit()
    }

    fun getPropBoolean(key: String): Boolean {
        return prop.getBoolean(key, false)
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

    fun getPropByte (key: String): UByte {
        return prop.getInt(key, 0).toUByte()
    }
    constructor() {
        prop = GO.mainContext.getSharedPreferences("device.properties", Context.MODE_PRIVATE)
    }
}