package ru.starline.bluz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Тонкая обёртка над [SharedPreferences] (файл `device.properties`).
 *
 * Используется [globalObj.readConfigParameters] / [globalObj.writeConfigParameters] для
 * персистентности всех `prop*`-полей и других пользовательских настроек.
 *
 * Геттеры возвращают дефолты: `0` / `0f` / `""` / `false` / `0.toByte()` если ключа нет.
 * Setter-ы используют `commit()` — синхронно. Это медленнее `apply()`, но безопаснее
 * при работе из обработчиков диалогов (гарантия записи до того как пользователь увидит Toast).
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